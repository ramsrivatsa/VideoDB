package xyz.unlimitedcodeworks.operations.extra;

import nl.tno.stormcv.util.NativeUtils;
import org.opencv.core.Mat;
import org.opencv.utils.Converters;

import java.util.List;

/**
 * Created by Aetf (aetf at unlimitedcodeworks dot xyz) on 16-7-19.
 */
public class Captioner {
    static {
        try {
            NativeUtils.loadLibrary("opencv_core", true);
            NativeUtils.loadLibrary("opencv_imgproc", true);
            NativeUtils.loadLibrary("caffe", true);
            NativeUtils.loadLibrary("gpu_utils");
            NativeUtils.loadLibrary("stormcv_common");
            NativeUtils.loadLibrary("stormcv_s2vt");
        } catch (Exception ex) {
            System.err.println("Error loading Captioner native libraries");
            ex.printStackTrace();
            throw ex;
        }
    }

    public Captioner(String vocabFile, String lstmProto, String modelBin,
                     boolean useGPU, int taskIndex, int maxGPUNum) {
        nativeObj = n_create(vocabFile, lstmProto, modelBin, useGPU, taskIndex, maxGPUNum);
    }

    public String captioning(List<Mat> frameFeatures) {
        Mat frameFeatures_mat = Converters.vector_Mat_to_Mat(frameFeatures);

        return n_captioning(nativeObj, frameFeatures_mat.getNativeObjAddr());
    }

    @Override
    protected void finalize() throws Throwable {
        n_delete(nativeObj);
        super.finalize();
    }

    private static native long n_create(String vocabFile, String lstmProto, String modelBin,
                                        boolean useGPU, int taskIndex, int maxGPUNum);

    private static native void n_delete(long nativeObj);

    private static native String n_captioning(long nativeObj, long frameFeatures_nativeObj);

    private long nativeObj = 0;
}
