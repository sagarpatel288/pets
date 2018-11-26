package com.example.android.pets.utils;

public final class StringUtils {
    private StringUtils() {
    }

    public static boolean isNotNullNotEmpty(String s) {
        return s != null && !s.isEmpty();
    }
}
