package xyz.unlimitedcodeworks.utils;

/**
 * Created by Aetf (aetf at unlimitedcodeworks dot xyz) on 16-5-31.
 */
public class Timing {
    static {
        adjustment = millisFromSys() - millisFromNano();
    }

    public static long currentTimeMillis() {
        return millisFromNano() + adjustment;
    }

    static long millisFromNano() {
        return System.nanoTime() / 1000000;
    }

    static long millisFromSys() {
        return System.currentTimeMillis();
    }

    public static long adjustment = 0;
}
