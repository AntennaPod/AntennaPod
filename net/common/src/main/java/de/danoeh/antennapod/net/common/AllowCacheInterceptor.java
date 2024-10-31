package de.danoeh.antennapod.net.common;

import androidx.annotation.NonNull;
import okhttp3.Interceptor;
import okhttp3.Response;

import java.io.IOException;

/**
 * Some servers insist on not being cacheable, even though they deliver static content.
 * This avoids having to re-fetch things.
 */
public class AllowCacheInterceptor implements Interceptor {
    @NonNull
    @Override
    public Response intercept(Chain chain) throws IOException {
        Response response = chain.proceed(chain.request());
        return response.newBuilder().removeHeader("Cache-Control").build();
    }
}
