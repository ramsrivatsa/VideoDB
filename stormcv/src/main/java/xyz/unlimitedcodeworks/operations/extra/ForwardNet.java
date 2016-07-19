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
            NativeUtils.loadLibrary("stormcv_thread");
        } catch (Exception ex) {
            System.err.println("Error loading ForwardNet native libraries");
            ex.printStackTrace();
            throw ex;
        }
    }

    public ForwardNet(String modelTxt, String modelBin, String outputName) {
        this(modelTxt, modelBin, outputName, "", false, false, -1, -1);
    }

    /**
     *
     * @param modelTxt
     * @param modelBin
     * @param meanBin
     * @param useCaffe true to use caffe, otherwise use OpenCV::DNN
     * @param caffeOnCPU CPU or GPU to do computation, only takes effect when useCaffe is true
     * @param taskIndex select which gpu to use based on task index. use -1 to indicate default gpu. Only takes effect when useCaffe is true
     * @param maxGPUNum maximum GPU devices to use
     */
    public ForwardNet(String modelTxt, String modelBin, String outputName, String meanBin,
                      boolean useCaffe, boolean caffeOnCPU, int taskIndex, int maxGPUNum) {
        lazyLoad(useCaffe, !caffeOnCPU);
        if (!useCaffe) {
            System.out.println("Use OpenCV::DNN as neural network");
            nativeObj = create_0(modelTxt, modelBin, outputName);
        } else {
            System.out.println("Use Caffe as neural network");
            if (!caffeOnCPU) {
                // check which gpu we are using
                String cuda_visible_devices = System.getenv("CUDA_VISIBLE_DEVICES");
                if (cuda_visible_devices == null || !cuda_visible_devices.equals("3,4")) {
                    throw new RuntimeException("GPU selection incorrect! CUDA_VISIBLE_DEVICES=" + cuda_visible_devices);
                }
            }
            nativeObj = create_1(modelTxt, modelBin, outputName, meanBin,
                                 caffeOnCPU, taskIndex, maxGPUNum);
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

    private static synchronized void lazyLoad(boolean useCaffe, boolean useGPU) {
        if (!lazyLoaded) {
            try {
                if (useCaffe) {
                    NativeUtils.loadLibrary("caffe", true);
                    NativeUtils.loadLibrary("gpu_utils");
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

    private static native long create_0(String modelTxt, String modelBin, String outputName);

    private static native long create_1(String modelTxt, String modelBin, String outputName,
                                        String meanBin, boolean caffeOnCPU, int taskIndex,
                                        int maxGPUNum);

    private static native void n_forward(long nativeObj, long nativeInput, long nativeOutput);

    private static native void n_forwardBatch(long nativeObj, long nativeInput, long nativeOutput);

    private static native void n_delete(long nativeObj);

    private static native void n_setPriority(int priority);

    private static native long n_getCurrentTid();

    private long nativeObj = 0;
}
