package de.danoeh.antennapod.net.download.service.feed.remote;

import android.os.StatFs;
import androidx.annotation.NonNull;
import android.text.TextUtils;
import android.util.Log;

import de.danoeh.antennapod.net.common.NetworkUtils;
import de.danoeh.antennapod.model.download.DownloadResult;
import de.danoeh.antennapod.model.download.DownloadRequest;
import de.danoeh.antennapod.net.common.AntennapodHttpClient;
import de.danoeh.antennapod.net.common.RedirectChecker;
import de.danoeh.antennapod.net.download.service.R;
import de.danoeh.antennapod.storage.preferences.UserPreferences;
import okhttp3.CacheControl;
import org.apache.commons.io.IOUtils;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.RandomAccessFile;
import java.net.HttpURLConnection;
import java.net.SocketTimeoutException;
import java.net.URI;
import java.net.UnknownHostException;
import java.util.Collections;
import java.util.Date;
import java.util.Locale;

import de.danoeh.antennapod.model.feed.FeedMedia;
import de.danoeh.antennapod.parser.feed.util.DateUtils;
import de.danoeh.antennapod.model.download.DownloadError;
import de.danoeh.antennapod.net.common.UriUtil;
import okhttp3.OkHttpClient;
import okhttp3.Protocol;
import okhttp3.Request;
import okhttp3.Response;
import okhttp3.ResponseBody;

public class HttpDownloader extends Downloader {
    private static final String TAG = "HttpDownloader";
    private static final int BUFFER_SIZE = 8 * 1024;

    public HttpDownloader(@NonNull DownloadRequest request) {
        super(request);
    }

    @Override
    protected void download() {
        File destination = new File(request.getDestination());
        final boolean fileExists = destination.exists();

        RandomAccessFile out = null;
        InputStream connection;
        ResponseBody responseBody = null;

        try {
            final URI uri = UriUtil.getURIFromRequestUrl(request.getSource());
            Request.Builder httpReq = new Request.Builder().url(uri.toURL());
            httpReq.tag(request);
            httpReq.cacheControl(new CacheControl.Builder().noStore().build());

            if (request.getFeedfileType() == FeedMedia.FEEDFILETYPE_FEEDMEDIA) {
                // set header explicitly so that okhttp doesn't do transparent gzip
                Log.d(TAG, "addHeader(\"Accept-Encoding\", \"identity\")");
                httpReq.addHeader("Accept-Encoding", "identity");
                httpReq.cacheControl(new CacheControl.Builder().noCache().build()); // noStore breaks CDNs
            }

            if (uri.getScheme().equals("http")) {
                httpReq.addHeader("Upgrade-Insecure-Requests", "1");
            }

            if (!TextUtils.isEmpty(request.getLastModified())) {
                String lastModified = request.getLastModified();
                Date lastModifiedDate = DateUtils.parse(lastModified);
                if (lastModifiedDate != null) {
                    long threeDaysAgo = System.currentTimeMillis() - 1000 * 60 * 60 * 24 * 3;
                    if (lastModifiedDate.getTime() > threeDaysAgo) {
                        Log.d(TAG, "addHeader(\"If-Modified-Since\", \"" + lastModified + "\")");
                        httpReq.addHeader("If-Modified-Since", lastModified);
                    }
                } else {
                    Log.d(TAG, "addHeader(\"If-None-Match\", \"" + lastModified + "\")");
                    httpReq.addHeader("If-None-Match", lastModified);
                }
            }

            // add range header if necessary
            if (fileExists && destination.length() > 0) {
                request.setSoFar(destination.length());
                httpReq.addHeader("Range", "bytes=" + request.getSoFar() + "-");
                Log.d(TAG, "Adding range header: " + request.getSoFar());
            }

            Response response = newCall(httpReq);
            responseBody = response.body();
            String contentEncodingHeader = response.header("Content-Encoding");
            boolean isGzip = false;
            if (!TextUtils.isEmpty(contentEncodingHeader)) {
                isGzip = TextUtils.equals(contentEncodingHeader.toLowerCase(Locale.US), "gzip");
            }

            Log.d(TAG, "Response code is " + response.code());
            if (!response.isSuccessful() && response.code() == HttpURLConnection.HTTP_NOT_MODIFIED) {
                Log.d(TAG, "Feed '" + request.getSource() + "' not modified since last update, Download canceled");
                onCancelled();
                return;
            } else if (!response.isSuccessful() || response.body() == null) {
                callOnFailByResponseCode(response);
                return;
            } else if (request.getFeedfileType() == FeedMedia.FEEDFILETYPE_FEEDMEDIA
                    && isContentTypeTextAndSmallerThan100kb(response)) {
                onFail(DownloadError.ERROR_FILE_TYPE, null);
                return;
            }
            String redirect = RedirectChecker.getNewUrlIfPermanentRedirect(response);
            if (redirect != null) {
                permanentRedirectUrl = redirect;
            }

            connection = new BufferedInputStream(responseBody.byteStream());

            String contentRangeHeader = (fileExists) ? response.header("Content-Range") : null;
            if (fileExists && response.code() == HttpURLConnection.HTTP_PARTIAL
                    && !TextUtils.isEmpty(contentRangeHeader)) {
                String start = contentRangeHeader.substring("bytes ".length(),
                        contentRangeHeader.indexOf("-"));
                request.setSoFar(Long.parseLong(start));
                Log.d(TAG, "Starting download at position " + request.getSoFar());

                out = new RandomAccessFile(destination, "rw");
                out.seek(request.getSoFar());
            } else {
                boolean success = destination.delete();
                success |= destination.createNewFile();
                if (!success) {
                    throw new IOException("Unable to recreate partially downloaded file");
                }
                out = new RandomAccessFile(destination, "rw");
            }

            byte[] buffer = new byte[BUFFER_SIZE];
            int count;
            request.setStatusMsg(R.string.download_running);
            Log.d(TAG, "Getting size of download");
            request.setSize(responseBody.contentLength() + request.getSoFar());
            Log.d(TAG, "Size is " + request.getSize());
            if (request.getSize() < 0) {
                request.setSize(DownloadResult.SIZE_UNKNOWN);
            }

            long freeSpace = getFreeSpaceAvailable();
            Log.d(TAG, "Free space is " + freeSpace);
            if (request.getSize() != DownloadResult.SIZE_UNKNOWN && request.getSize() > freeSpace) {
                onFail(DownloadError.ERROR_NOT_ENOUGH_SPACE, null);
                return;
            }

            Log.d(TAG, "Starting download");
            try {
                while (!cancelled && (count = connection.read(buffer)) != -1) {
                    out.write(buffer, 0, count);
                    request.setSoFar(request.getSoFar() + count);
                    int progressPercent = (int) (100.0 * request.getSoFar() / request.getSize());
                    request.setProgressPercent(progressPercent);
                }
            } catch (IOException e) {
                Log.e(TAG, Log.getStackTraceString(e));
            }
            if (cancelled) {
                onCancelled();
            } else {
                // check if size specified in the response header is the same as the size of the
                // written file. This check cannot be made if compression was used
                if (!isGzip && request.getSize() != DownloadResult.SIZE_UNKNOWN
                        && request.getSoFar() != request.getSize()) {
                    onFail(DownloadError.ERROR_IO_WRONG_SIZE, "Download completed but size: "
                            + request.getSoFar() + " does not equal expected size " + request.getSize());
                    return;
                } else if (request.getSize() > 0 && request.getSoFar() == 0) {
                    onFail(DownloadError.ERROR_IO_ERROR, "Download completed, but nothing was read");
                    return;
                }
                String lastModified = response.header("Last-Modified");
                if (lastModified != null) {
                    request.setLastModified(lastModified);
                } else {
                    request.setLastModified(response.header("ETag"));
                }
                onSuccess();
            }

        } catch (IllegalArgumentException e) {
            e.printStackTrace();
            onFail(DownloadError.ERROR_MALFORMED_URL, e.getMessage());
        } catch (SocketTimeoutException e) {
            e.printStackTrace();
            onFail(DownloadError.ERROR_CONNECTION_ERROR, e.getMessage());
        } catch (UnknownHostException e) {
            e.printStackTrace();
            onFail(DownloadError.ERROR_UNKNOWN_HOST, e.getMessage());
        } catch (IOException e) {
            e.printStackTrace();
            if (NetworkUtils.wasDownloadBlocked(e)) {
                onFail(DownloadError.ERROR_IO_BLOCKED, e.getMessage());
                return;
            }
            String message = e.getMessage();
            if (message != null && message.contains("Trust anchor for certification path not found")) {
                onFail(DownloadError.ERROR_CERTIFICATE, e.getMessage());
                return;
            }
            onFail(DownloadError.ERROR_IO_ERROR, e.getMessage());
        } catch (NullPointerException e) {
            // might be thrown by connection.getInputStream()
            e.printStackTrace();
            onFail(DownloadError.ERROR_CONNECTION_ERROR, request.getSource());
        } finally {
            IOUtils.closeQuietly(out);
            IOUtils.closeQuietly(responseBody);
        }
    }

    private Response newCall(Request.Builder httpReq) throws IOException {
        OkHttpClient httpClient = AntennapodHttpClient.getHttpClient();
        try {
            return httpClient.newCall(httpReq.build()).execute();
        } catch (IOException e) {
            Log.e(TAG, e.toString());
            if (e.getMessage() != null && e.getMessage().contains("PROTOCOL_ERROR")) {
                // Apparently some servers announce they support SPDY but then actually don't.
                httpClient = httpClient.newBuilder()
                        .protocols(Collections.singletonList(Protocol.HTTP_1_1))
                        .build();
                return httpClient.newCall(httpReq.build()).execute();
            } else {
                throw e;
            }
        }
    }

    private boolean isContentTypeTextAndSmallerThan100kb(Response response) {
        int contentLength = -1;
        String contentLen = response.header("Content-Length");
        if (contentLen != null) {
            try {
                contentLength = Integer.parseInt(contentLen);
            } catch (NumberFormatException e) {
                e.printStackTrace();
            }
        }
        Log.d(TAG, "content length: " + contentLength);
        String contentType = response.header("Content-Type");
        Log.d(TAG, "content type: " + contentType);
        return contentType != null && contentType.startsWith("text/") && contentLength < 100 * 1024;
    }

    private void callOnFailByResponseCode(Response response) {
        final DownloadError error;
        final String details;
        if (response.code() == HttpURLConnection.HTTP_UNAUTHORIZED) {
            error = DownloadError.ERROR_UNAUTHORIZED;
            details = String.valueOf(response.code());
        } else if (response.code() == HttpURLConnection.HTTP_FORBIDDEN) {
            error = DownloadError.ERROR_FORBIDDEN;
            details = String.valueOf(response.code());
        } else if (response.code() == HttpURLConnection.HTTP_NOT_FOUND
                || response.code() == HttpURLConnection.HTTP_GONE) {
            error = DownloadError.ERROR_NOT_FOUND;
            details = String.valueOf(response.code());
        } else {
            error = DownloadError.ERROR_HTTP_DATA_ERROR;
            details = String.valueOf(response.code());
        }
        onFail(error, details);
    }

    private static long getFreeSpaceAvailable() {
        File dataFolder = UserPreferences.getDataFolder(null);
        if (dataFolder != null) {
            StatFs stat = new StatFs(dataFolder.getAbsolutePath());
            long availableBlocks = stat.getAvailableBlocksLong();
            long blockSize = stat.getBlockSizeLong();
            return availableBlocks * blockSize;
        } else {
            return 0;
        }
    }

    private void onSuccess() {
        Log.d(TAG, "Download was successful");
        result.setSuccessful();
    }

    private void onFail(DownloadError reason, String reasonDetailed) {
        Log.d(TAG, "onFail() called with: " + "reason = [" + reason + "], reasonDetailed = [" + reasonDetailed + "]");
        result.setFailed(reason, reasonDetailed);
    }

    private void onCancelled() {
        Log.d(TAG, "Download was cancelled");
        result.setCancelled();
        cancelled = true;
    }
}
