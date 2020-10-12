package com.bormannqds.mds.lib.configuration;

import com.bormannqds.mds.lib.protocoladaptor.ComboConnection;

/**
 * Created by bormanng on 30/06/15.
 */
public class MulticastConfig {
    public MulticastConfig(String baseTicker, double multiplier, ComboConnection connection) {
        this.baseTicker = baseTicker;
        this.multiplier = multiplier;
        this.connection = connection;
    }

    public String getBaseTicker() {
        return baseTicker;
    }

    public double getMultiplier() {
        return multiplier;
    }

    public ComboConnection getConnection() {
        return connection;
    }

    private final String baseTicker;
    private final double multiplier;
    private final ComboConnection connection;
}
