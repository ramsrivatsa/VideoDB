package xyz.unlimitedcodeworks.operations.extra;

import nl.tno.stormcv.util.NativeUtils;
import org.opencv.core.Mat;
import org.opencv.utils.Converters;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by Aetf (aetf at unlimitedcodeworks dot xyz) on 16-3-17.
 */
public class ForwardNet {
    static {
        try {
            NativeUtils.loadLibrary("opencv_core", true);
            NativeUtils.loadLibrary("opencv_imgproc", true);
            NativeUtils.loadLibrary("stormcv_common");
            NativeUtils.loadLibrary("stormcv_thread", true);
        } catch (Exception ex) {
            System.err.println("Error loading ForwardNet native libraries");
            ex.printStackTrace();
            throw ex;
        }
    }

    public ForwardNet(String modelTxt, String modelBin) {
        this(modelTxt, modelBin, "", false, false);
    }

    public ForwardNet(String modelTxt, String modelBin, String meanBin, boolean useCaffe, boolean caffeOnCPU) {
        lazyLoad(useCaffe);
        if (!useCaffe) {
            System.out.println("Use OpenCV::DNN as neural network");
            nativeObj = create(modelTxt, modelBin);
        } else {
            System.out.println("Use Caffe as neural network");
            nativeObj = create(modelTxt, modelBin, meanBin, caffeOnCPU);
        }
    }

    public Mat forward(Mat input) {
        Mat res = new Mat();
        n_forward(nativeObj, input.getNativeObjAddr(), res.getNativeObjAddr());
        return res;
    }

    public List<Mat> forward(List<Mat> input) {
        Mat imgs_mat = Converters.vector_Mat_to_Mat(input);
        Mat res = new Mat();
        n_forwardBatch(nativeObj, imgs_mat.getNativeObjAddr(), res.getNativeObjAddr());

        List<Mat> result = new ArrayList<>();
        Converters.Mat_to_vector_Mat(res, result);
        res.release();
        return result;
    }

    public static void setThreadPriority(int priority) {
        n_setPriority(priority);
    }

    public static long getCurrentTid() {
        return n_getCurrentTid();
    }

    @Override
    protected void finalize() throws Throwable {
        n_delete(nativeObj);
        super.finalize();
    }

    private static synchronized void lazyLoad(boolean useCaffe) {
        if (!lazyLoaded) {
            try {
                if (useCaffe) {
                    NativeUtils.loadLibrary("caffe", true);
                    NativeUtils.loadLibrary("stormcv_caffe");
                } else {
                    NativeUtils.loadLibrary("opencv_dnn", true);
                    NativeUtils.loadLibrary("stormcv_cv");
                }
                lazyLoaded = true;
            } catch (Exception ex) {
                System.err.println("Error loading ForwardNet native libraries");
                ex.printStackTrace();
                throw ex;
            }
        }
    }
    private static boolean lazyLoaded = false;

    private static native long create(String modelTxt, String modelBin);

    private static native long create(String modelTxt, String modelBin, String meanBin, boolean caffeOnCPU);

    private static native void n_forward(long nativeObj, long nativeInput, long nativeOutput);

    private static native void n_forwardBatch(long nativeObj, long nativeInput, long nativeOutput);

    private static native void n_delete(long nativeObj);

    private static native void n_setPriority(int priority);

    private static native long n_getCurrentTid();

    private long nativeObj = 0;
}
