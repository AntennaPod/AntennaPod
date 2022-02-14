package de.danoeh.antennapod.core.service;

import android.text.TextUtils;
import android.util.Log;
import androidx.annotation.NonNull;
import de.danoeh.antennapod.core.service.download.DownloadRequest;
import de.danoeh.antennapod.core.service.download.HttpDownloader;
import de.danoeh.antennapod.core.storage.DBReader;
import de.danoeh.antennapod.core.util.URIUtil;
import java.io.IOException;
import java.net.HttpURLConnection;
import java.util.List;

import okhttp3.Interceptor;
import okhttp3.Request;
import okhttp3.Response;

public class BasicAuthorizationInterceptor implements Interceptor {
    private static final String TAG = "BasicAuthInterceptor";
    private static final String HEADER_AUTHORIZATION = "Authorization";

    @Override
    @NonNull
    public Response intercept(Chain chain) throws IOException {
        Request request = chain.request();

        Response response = chain.proceed(request);

        if (response.code() != HttpURLConnection.HTTP_UNAUTHORIZED) {
            return response;
        }

        Request.Builder newRequest = request.newBuilder();
        if (!TextUtils.equals(response.request().url().toString(), request.url().toString())) {
            // Redirect detected. OkHTTP does not re-add the headers on redirect, so calling the new location directly.
            newRequest.url(response.request().url());

            List<String> authorizationHeaders = request.headers().values(HEADER_AUTHORIZATION);
            if (!authorizationHeaders.isEmpty() && !TextUtils.isEmpty(authorizationHeaders.get(0))) {
                // Call already had authorization headers. Try again with the same credentials.
                newRequest.header(HEADER_AUTHORIZATION, authorizationHeaders.get(0));
                return chain.proceed(newRequest.build());
            }
        }

        String userInfo;
        if (request.tag() instanceof DownloadRequest) {
            DownloadRequest downloadRequest = (DownloadRequest) request.tag();
            userInfo = URIUtil.getURIFromRequestUrl(downloadRequest.getSource()).getUserInfo();
            if (TextUtils.isEmpty(userInfo)) {
                userInfo = downloadRequest.getUsername() + ":" + downloadRequest.getPassword();
            }
        } else {
            userInfo = DBReader.getImageAuthentication(request.url().toString());
        }

        if (TextUtils.isEmpty(userInfo)) {
            Log.d(TAG, "no credentials for '" + request.url() + "'");
            return response;
        }

        String[] parts = userInfo.split(":");
        if (parts.length != 2) {
            Log.d(TAG, "Invalid credentials for '" + request.url() + "'");
            return response;
        }

        Log.d(TAG, "Authorization failed, re-trying with ISO-8859-1 encoded credentials");
        String credentials = HttpDownloader.encodeCredentials(parts[0], parts[1], "ISO-8859-1");
        newRequest.header(HEADER_AUTHORIZATION, credentials);
        response = chain.proceed(newRequest.build());

        if (response.code() != HttpURLConnection.HTTP_UNAUTHORIZED) {
            return response;
        }

        Log.d(TAG, "Authorization failed, re-trying with UTF-8 encoded credentials");
        credentials = HttpDownloader.encodeCredentials(parts[0], parts[1], "UTF-8");
        newRequest.header(HEADER_AUTHORIZATION, credentials);
        return chain.proceed(newRequest.build());
    }
}
