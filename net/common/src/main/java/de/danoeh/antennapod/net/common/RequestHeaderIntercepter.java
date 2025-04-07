package de.danoeh.antennapod.net.common;

import androidx.annotation.NonNull;

import okhttp3.Interceptor;
import okhttp3.Response;

import java.io.IOException;
import java.util.Locale;

public class RequestHeaderIntercepter implements Interceptor {
    public static String ACCEPT_ENCODING = "gzip, identity";
   // public static String ACCEPT_ENCODING = "gzip";


    @Override
    public Response intercept(Chain chain) throws IOException {
        String existingEncoding = chain.request().header("Accept-Encoding");
        String newEncodingValue = getEncodeString(existingEncoding);
        return chain.proceed(chain.request().newBuilder()
                .header("Accept-Encoding", newEncodingValue)
                .build());
    }
    @NonNull
    private static String getEncodeString(String existingEncoding) {
        String newEncodingValue;
        if (existingEncoding == null || existingEncoding.isEmpty()) {
            newEncodingValue = ACCEPT_ENCODING;
        } else {
            StringBuilder encodingBuilder = new StringBuilder(existingEncoding);
            if (!existingEncoding.toLowerCase(Locale.ROOT).contains("deflate")) {
              encodingBuilder.append(", identity");
            }
            newEncodingValue = encodingBuilder.toString();
        }
        return newEncodingValue;
    }
}
