package de.danoeh.antennapod.core.service.download;

import android.net.http.AndroidHttpClient;
import android.util.Log;

import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.http.Header;
import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.HttpStatus;
import org.apache.http.auth.UsernamePasswordCredentials;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.auth.BasicScheme;
import org.apache.http.message.BasicHeader;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.RandomAccessFile;
import java.net.HttpURLConnection;
import java.net.SocketTimeoutException;
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

        HttpClient httpClient = AntennapodHttpClient.getHttpClient();
        RandomAccessFile out = null;
        InputStream connection = null;
        try {
            HttpGet httpGet = new HttpGet(URIUtil.getURIFromRequestUrl(request.getSource()));

            // add authentication information
            String userInfo = httpGet.getURI().getUserInfo();
            if (userInfo != null) {
                String[] parts = userInfo.split(":");
                if (parts.length == 2) {
                    httpGet.addHeader(BasicScheme.authenticate(
                            new UsernamePasswordCredentials(parts[0], parts[1]),
                            "UTF-8", false));
                }
            } else if (!StringUtils.isEmpty(request.getUsername()) && request.getPassword() != null) {
                httpGet.addHeader(BasicScheme.authenticate(new UsernamePasswordCredentials(request.getUsername(),
                        request.getPassword()), "UTF-8", false));
            }

            // add range header if necessary
            if (fileExists) {
                request.setSoFar(destination.length());
                httpGet.addHeader(new BasicHeader("Range",
                        "bytes=" + request.getSoFar() + "-"));
                if (BuildConfig.DEBUG) Log.d(TAG, "Adding range header: " + request.getSoFar());
            }

            HttpResponse response = httpClient.execute(httpGet);
            HttpEntity httpEntity = response.getEntity();
            int responseCode = response.getStatusLine().getStatusCode();
            Header contentEncodingHeader = response.getFirstHeader("Content-Encoding");

            final boolean isGzip = contentEncodingHeader != null &&
                    contentEncodingHeader.getValue().equalsIgnoreCase("gzip");

            if (BuildConfig.DEBUG)
                Log.d(TAG, "Response code is " + responseCode);

            if (responseCode / 100 != 2 || httpEntity == null) {
                final DownloadError error;
                final String details;
                if (responseCode == HttpURLConnection.HTTP_UNAUTHORIZED) {
                    error = DownloadError.ERROR_UNAUTHORIZED;
                    details = String.valueOf(responseCode);
                } else {
                    error = DownloadError.ERROR_HTTP_DATA_ERROR;
                    details = String.valueOf(responseCode);
                }
                onFail(error, details);
                return;
            }

            if (!StorageUtils.storageAvailable(ClientConfig.applicationCallbacks.getApplicationInstance())) {
                onFail(DownloadError.ERROR_DEVICE_NOT_FOUND, null);
                return;
            }

            connection = new BufferedInputStream(AndroidHttpClient
                    .getUngzippedContent(httpEntity));

            Header[] contentRangeHeaders = (fileExists) ? response.getHeaders("Content-Range") : null;

            if (fileExists && responseCode == HttpStatus.SC_PARTIAL_CONTENT
                    && contentRangeHeaders != null && contentRangeHeaders.length > 0) {
                String start = contentRangeHeaders[0].getValue().substring("bytes ".length(),
                        contentRangeHeaders[0].getValue().indexOf("-"));
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
            request.setSize(httpEntity.getContentLength() + request.getSoFar());
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
