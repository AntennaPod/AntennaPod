package de.danoeh.antennapod.core;

/**
 * Stores callbacks for core classes like Services, DB classes etc. and other configuration variables.
 * Apps using the core module of AntennaPod should register implementations of all interfaces here.
 */
public class ClientConfig {
    public static ApplicationCallbacks applicationCallbacks;
}
