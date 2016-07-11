package xyz.unlimitedcodeworks.operations.extra;

import nl.tno.stormcv.util.NativeUtils;
import org.opencv.core.Mat;
import org.opencv.core.Rect;
import org.opencv.core.RotatedRect;

/**
 * Created by Aetf (aetf at unlimitedcodeworks dot xyz) on 16-7-8.
 */
public class CMTTracker {

    static {
        try {
            NativeUtils.loadLibrary("opencv_core", true);
            NativeUtils.loadLibrary("opencv_imgproc", true);
            NativeUtils.loadLibrary("opencv_video", true);
            NativeUtils.loadLibrary("opencv_features2d", true);
            NativeUtils.loadLibrary("cmt");
            NativeUtils.loadLibrary("stormcv_tarcker");
        } catch (Exception ex) {
            System.err.println("Error loading CMTTracker native libraries");
            ex.printStackTrace();
            throw ex;
        }
    }

    public CMTTracker(Rect boundingBox, Mat firstFrame) {
        nativeObj = n_create(boundingBox.x, boundingBox.y, boundingBox.width, boundingBox.height,
                             firstFrame.getNativeObjAddr());
    }

    public void trackImage(Mat frame) {
        n_track(nativeObj, frame.getNativeObjAddr());
    }

    public RotatedRect currentPosition() {
        double[] data = n_currentPosition(nativeObj);
        RotatedRect rr = new RotatedRect();
        rr.set(data);
        return rr;
    }

    @Override
    protected void finalize() throws Throwable {
        n_delete(nativeObj);
        super.finalize();
    }

    private static native long n_create(int x, int y, int width, int height, long nativeFirstFrame);

    private static native void n_delete(long nativeObj);

    private static native void n_track(long nativeObj, long nativeFrame);

    private static native double[] n_currentPosition(long nativeObj);

    private long nativeObj = 0;
}
