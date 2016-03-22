package xyz.unlimitedcodeworks.opencv.dnn;

import nl.tno.stormcv.util.NativeUtils;
import org.opencv.core.Mat;

/**
 * Created by Aetf (aetf at unlimitedcodeworks dot xyz) on 16-3-17.
 */
public class ForwardNet {
    static {
        NativeUtils.loadLibrary("opencv_core");
        NativeUtils.loadLibrary("opencv_imgproc");
        NativeUtils.loadLibrary("opencv_dnn");
        NativeUtils.loadLibrary("ForwardNet");
    }

    public ForwardNet(String modelTxt, String modelBin) {
        nativeObj = create(modelTxt, modelBin);
    }

    public Mat forward(Mat input) {
        Mat res = new Mat();
        n_forward(nativeObj, input.getNativeObjAddr(), res.getNativeObjAddr());
        return res;
    }

    @Override
    protected void finalize() throws Throwable {
        n_delete(nativeObj);
        super.finalize();
    }

    private static native long create(String modelTxt, String modelBin);

    private static native void n_forward(long nativeObj, long nativeInput, long nativeOutput);

    private static native void n_delete(long nativeObj);

    private long nativeObj = 0;
}
