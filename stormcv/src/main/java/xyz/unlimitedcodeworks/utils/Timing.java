package xyz.unlimitedcodeworks.utils;

/**
 * Created by Aetf (aetf at unlimitedcodeworks dot xyz) on 16-5-31.
 */
public class Timing {
    public static long currentTimeMillis() {
        return System.nanoTime() / 1000000;
    }
}
