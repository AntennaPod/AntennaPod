package de.danoeh.antennapod.model.download;

import androidx.annotation.Nullable;

import java.net.Proxy;

public class ProxyConfig {

    public final Proxy.Type type;
    @Nullable public final String host;
    public final int port;
    @Nullable public final String username;
    @Nullable public final String password;

    public static final int DEFAULT_PORT = 8080;

    public ProxyConfig(Proxy.Type type, String host, int port, String username, String password) {
        this.type = type;
        this.host = host;
        this.port = port;
        this.username = username;
        this.password = password;
    }
}