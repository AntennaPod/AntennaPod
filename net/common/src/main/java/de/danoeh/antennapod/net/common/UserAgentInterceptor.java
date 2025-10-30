package de.danoeh.antennapod.net.common;

import okhttp3.Interceptor;
import okhttp3.Response;

import java.io.IOException;

public class UserAgentInterceptor implements Interceptor {
    public static String USER_AGENT = "AntennaPod/0.0.0";

    @Override
    public Response intercept(Chain chain) throws IOException {
        return chain.proceed(chain.request().newBuilder()
                .header("User-Agent", USER_AGENT)
                .build());
    }
}
