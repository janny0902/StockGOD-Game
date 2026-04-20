package com.jadiss.cldc.backend.model;

import java.util.Arrays;

public enum Timeframe {
    ONE_MINUTE("1m"),
    FIVE_MINUTES("5m"),
    TEN_MINUTES("10m"),
    FIFTEEN_MINUTES("15m"),
    THIRTY_MINUTES("30m"),
    SIXTY_MINUTES("60m"),
    SIX_HOURS("6h"),
    TWELVE_HOURS("12h"),
    ONE_DAY("1d"),
    ONE_WEEK("1w"),
    ONE_MONTH("1M");

    private final String code;

    Timeframe(String code) {
        this.code = code;
    }

    public String code() {
        return code;
    }

    public static Timeframe from(String code) {
        return Arrays.stream(values())
                .filter(v -> v.code.equalsIgnoreCase(code))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("Unsupported timeframe: " + code));
    }
}
