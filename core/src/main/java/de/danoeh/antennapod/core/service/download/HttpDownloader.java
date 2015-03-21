package de.danoeh.antennapod.core.service.download;

import android.util.Log;

import com.squareup.okhttp.Credentials;
import com.squareup.okhttp.OkHttpClient;
import com.squareup.okhttp.Request;
import com.squareup.okhttp.Response;
import com.squareup.okhttp.ResponseBody;

import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.http.HttpStatus;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.RandomAccessFile;
import java.net.HttpURLConnection;
import java.net.SocketTimeoutException;
import java.net.URI;
import java.net.UnknownHostException;

import de.danoeh.antennapod.core.BuildConfig;
import de.danoeh.antennapod.core.ClientConfig;
import de.danoeh.antennapod.core.R;
import de.danoeh.antennapod.core.feed.FeedImage;
import de.danoeh.antennapod.core.util.DownloadError;
import de.danoeh.antennapod.core.util.StorageUtils;
import de.danoeh.antennapod.core.util.URIUtil;

public class HttpDownloader extends Downloader {
    private static final String TAG = "HttpDownloader";

    private static final int BUFFER_SIZE = 8 * 1024;

    public HttpDownloader(DownloadRequest request) {
        super(request);
    }

    @Override
    protected void download() {
        File destination = new File(request.getDestination());
        final boolean fileExists = destination.exists();

        if (request.isDeleteOnFailure() && fileExists) {
            Log.w(TAG, "File already exists");
            if (request.getFeedfileType() != FeedImage.FEEDFILETYPE_FEEDIMAGE) {
                onFail(DownloadError.ERROR_FILE_EXISTS, null);
                return;
            } else {
                onSuccess();
                return;
            }
        }

        OkHttpClient httpClient = AntennapodHttpClient.getHttpClient();
        RandomAccessFile out = null;
        InputStream connection;
        ResponseBody responseBody = null;

        try {
            final URI uri = URIUtil.getURIFromRequestUrl(request.getSource());
            Request.Builder httpReq = new Request.Builder().url(uri.toURL())
                    .header("User-Agent", ClientConfig.USER_AGENT);

            // add authentication information
            String userInfo = uri.getUserInfo();
            if (userInfo != null) {
                String[] parts = userInfo.split(":");
                if (parts.length == 2) {
                    String credentials = Credentials.basic(parts[0], parts[1]);
                    httpReq.header("Authorization", credentials);
                }
            } else if (!StringUtils.isEmpty(request.getUsername()) && request.getPassword() != null) {
                String credentials = Credentials.basic(request.getUsername(), request.getPassword());
                httpReq.header("Authorization", credentials);
            }

            // add range header if necessary
            if (fileExists) {
                request.setSoFar(destination.length());
                httpReq.addHeader("Range",
                        "bytes=" + request.getSoFar() + "-");
                if (BuildConfig.DEBUG) Log.d(TAG, "Adding range header: " + request.getSoFar());
            }

            Response response = httpClient.newCall(httpReq.build()).execute();
            responseBody = response.body();

            String contentEncodingHeader = response.header("Content-Encoding");

            final boolean isGzip = StringUtils.equalsIgnoreCase(contentEncodingHeader, "gzip");

            if (BuildConfig.DEBUG)
                Log.d(TAG, "Response code is " + response.code());

            if (!response.isSuccessful() || response.body() == null) {
                final DownloadError error;
                final String details;
                if (response.code() == HttpURLConnection.HTTP_UNAUTHORIZED) {
                    error = DownloadError.ERROR_UNAUTHORIZED;
                    details = String.valueOf(response.code());
                } else {
                    error = DownloadError.ERROR_HTTP_DATA_ERROR;
                    details = String.valueOf(response.code());
                }
                onFail(error, details);
                return;
            }

            if (!StorageUtils.storageAvailable(ClientConfig.applicationCallbacks.getApplicationInstance())) {
                onFail(DownloadError.ERROR_DEVICE_NOT_FOUND, null);
                return;
            }

            connection = new BufferedInputStream(responseBody.byteStream());

            String contentRangeHeader = (fileExists) ? response.header("Content-Range") : null;

            if (fileExists && response.code() == HttpStatus.SC_PARTIAL_CONTENT
                    && !StringUtils.isEmpty(contentRangeHeader)) {
                String start = contentRangeHeader.substring("bytes ".length(),
                        contentRangeHeader.indexOf("-"));
                request.setSoFar(Long.valueOf(start));
                Log.d(TAG, "Starting download at position " + request.getSoFar());

                out = new RandomAccessFile(destination, "rw");
                out.seek(request.getSoFar());
            } else {
                destination.delete();
                destination.createNewFile();
                out = new RandomAccessFile(destination, "rw");
            }


            byte[] buffer = new byte[BUFFER_SIZE];
            int count = 0;
            request.setStatusMsg(R.string.download_running);
            if (BuildConfig.DEBUG)
                Log.d(TAG, "Getting size of download");
            request.setSize(responseBody.contentLength() + request.getSoFar());
            if (BuildConfig.DEBUG)
                Log.d(TAG, "Size is " + request.getSize());
            if (request.getSize() < 0) {
                request.setSize(DownloadStatus.SIZE_UNKNOWN);
            }

            long freeSpace = StorageUtils.getFreeSpaceAvailable();
            if (BuildConfig.DEBUG)
                Log.d(TAG, "Free space is " + freeSpace);

            if (request.getSize() != DownloadStatus.SIZE_UNKNOWN
                    && request.getSize() > freeSpace) {
                onFail(DownloadError.ERROR_NOT_ENOUGH_SPACE, null);
                return;
            }

            if (BuildConfig.DEBUG)
                Log.d(TAG, "Starting download");
            while (!cancelled
                    && (count = connection.read(buffer)) != -1) {
                out.write(buffer, 0, count);
                request.setSoFar(request.getSoFar() + count);
                request.setProgressPercent((int) (((double) request
                        .getSoFar() / (double) request
                        .getSize()) * 100));
            }
            if (cancelled) {
                onCancelled();
            } else {
                // check if size specified in the response header is the same as the size of the
                // written file. This check cannot be made if compression was used
                if (!isGzip && request.getSize() != DownloadStatus.SIZE_UNKNOWN &&
                        request.getSoFar() != request.getSize()) {
                    onFail(DownloadError.ERROR_IO_ERROR,
                            "Download completed but size: " +
                                    request.getSoFar() +
                                    " does not equal expected size " +
                                    request.getSize()
                    );
                    return;
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
            onFail(DownloadError.ERROR_IO_ERROR, e.getMessage());
        } catch (NullPointerException e) {
            // might be thrown by connection.getInputStream()
            e.printStackTrace();
            onFail(DownloadError.ERROR_CONNECTION_ERROR, request.getSource());
        } finally {
            IOUtils.closeQuietly(out);
            AntennapodHttpClient.cleanup();
            IOUtils.closeQuietly(responseBody);
        }
    }

    private void onSuccess() {
        if (BuildConfig.DEBUG)
            Log.d(TAG, "Download was successful");
        result.setSuccessful();
    }

    private void onFail(DownloadError reason, String reasonDetailed) {
        if (BuildConfig.DEBUG) {
            Log.d(TAG, "Download failed");
        }
        result.setFailed(reason, reasonDetailed);
        if (request.isDeleteOnFailure()) {
            cleanup();
        }
    }

    private void onCancelled() {
        if (BuildConfig.DEBUG)
            Log.d(TAG, "Download was cancelled");
        result.setCancelled();
        cleanup();
    }

    /**
     * Deletes unfinished downloads.
     */
    private void cleanup() {
        if (request.getDestination() != null) {
            File dest = new File(request.getDestination());
            if (dest.exists()) {
                boolean rc = dest.delete();
                if (BuildConfig.DEBUG)
                    Log.d(TAG, "Deleted file " + dest.getName() + "; Result: "
                            + rc);
            } else {
                if (BuildConfig.DEBUG)
                    Log.d(TAG, "cleanup() didn't delete file: does not exist.");
            }
        }
    }

}
