package com.hortonworks.streamline.streams.util;

/**
 * Utility functions that can be used across UDF/UDAFs
 */
public final class Utils {

    private Utils() {
    }

    public static String trim(String input, char ch, boolean left, boolean right) {
        int len = input.length();
        int st = 0;

        if (left) {
            while ((st < len) && (input.charAt(st) == ch)) {
                st++;
            }
        }
        if (right) {
            while ((st < len) && (input.charAt(len - 1) == ch)) {
                len--;
            }
        }
        return ((st > 0) || (len < input.length())) ? input.substring(st, len) : input;
    }

}
