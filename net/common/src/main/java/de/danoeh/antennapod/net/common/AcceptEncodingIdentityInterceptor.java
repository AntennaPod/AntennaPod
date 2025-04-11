package de.danoeh.antennapod.net.common;

import androidx.annotation.NonNull;
import okhttp3.Interceptor;
import okhttp3.Response;

import java.io.IOException;

public class AcceptEncodingIdentityInterceptor implements Interceptor {
    @Override
    @NonNull
    public Response intercept(Chain chain) throws IOException {
        String encoding = chain.request().header("Accept-Encoding");
        if (encoding == null || encoding.contains("identity")) {
            return chain.proceed(chain.request());
        }
        encoding += ", identity";
        return chain.proceed(chain.request().newBuilder()
                .header("Accept-Encoding", encoding)
                .build());
    }
}
