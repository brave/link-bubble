package com.chrislacy.linkbubble;

/**
 * Created by gw on 2/10/13.
 */
public class Util {
    public static void Assert(boolean condition) {
        Assert(condition, "Unknown Error");
    }

    public static void Assert(boolean condition, String message) {
        if (!condition) {
            throw new AssertionError(message);
        }
    }

    public static float clamp(float v0, float v, float v1) {
        return Math.max(v0, Math.min(v, v1));
    }

    public static int clamp(int v0, int v, int v1) {
        return Math.max(v0, Math.min(v, v1));
    }
}
