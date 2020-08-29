package de.danoeh.antennapod.core.service;

import androidx.annotation.NonNull;
import okhttp3.Interceptor;
import okhttp3.Response;

import java.io.IOException;

public class ProviderInstallerInterceptor implements Interceptor {
    public static Runnable installer = () -> { };

    @Override
    @NonNull
    public Response intercept(Chain chain) throws IOException {
        installer.run();
        return chain.proceed(chain.request());
    }
}
