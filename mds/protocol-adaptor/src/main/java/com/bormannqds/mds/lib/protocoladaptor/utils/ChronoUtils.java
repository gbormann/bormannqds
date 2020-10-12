package com.bormannqds.mds.lib.protocoladaptor.utils;

import zmq.Clock;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;

/**
 * Created by bormanng on 07/07/15.
 */
public class ChronoUtils {
    public static final long US_IN_S = 1000000L;
    public static final long NS_IN_US = 1000L;
    public static final long US_IN_MS = 1000L;

    public static long estimateHpNowInUs() {
        return systemBaseTime + (Clock.now_us() - zmqBaseTime); // we could be off by 999us either way (assuming HP drift=0)
    }

    public static long timestamp2Micros(final Instant instant) {
        return instant.getEpochSecond() * 1000000L + instant.getNano() / 1000L;
    }

    /**
     *  e.g. 2,001,001us = 2,001.001ms = 2.001001s
     *      => (2,001,001us div 1,000,000us/s) = 2s
     *      and (2,001,001us mod 1,000,000us/s) = 1,001us = 1,001,000ns
     *
     * @param micros
     * @return Formatted date-time string up to millis
     */
    public static String micros2TimestampMs(long micros) {
        long tsS = micros / US_IN_S; // time fitting in epoch seconds
        int tsNs = (int)(NS_IN_US * (micros % US_IN_S)); // remainder of time in nanoseconds
        return LocalDateTime.ofEpochSecond(tsS, tsNs, ZoneOffset.UTC).format(DATE_TIME_FORMATTER_WITH_MS_FRACTION);
    }

    private static final long zmqBaseTime = Clock.now_us();
    private static final long systemBaseTime = java.time.Clock.systemUTC().millis() * US_IN_MS;
    private static final DateTimeFormatter DATE_TIME_FORMATTER_WITH_MS_FRACTION =
            DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss.SSS");
}
