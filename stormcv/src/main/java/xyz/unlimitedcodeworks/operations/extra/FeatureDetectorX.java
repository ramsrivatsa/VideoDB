package xyz.unlimitedcodeworks.operations.extra;

import nl.tno.stormcv.util.NativeUtils;
import org.opencv.core.CvException;
import org.opencv.core.Mat;
import org.opencv.core.MatOfKeyPoint;
import org.opencv.features2d.FeatureDetector;
import org.opencv.utils.Converters;

import java.util.List;

/**
 * Created by Aetf (aetf at unlimitedcodeworks dot xyz) on 16-6-23.
 */
public class FeatureDetectorX extends FeatureDetector {
    static {
        try {
            NativeUtils.loadLibrary("opencv_core", true);
            NativeUtils.loadLibrary("opencv_xfeatures2d", true);
            NativeUtils.loadLibrary("stormcv_common");
            NativeUtils.loadLibrary("stormcv_detectorx");
        } catch (Exception ex) {
            System.err.println("Error loading FeatureDetectorX native libraries");
            ex.printStackTrace();
            throw ex;
        }
    }
    protected boolean patched = false;
    protected FeatureDetectorX(long nativeObj) {
        super(nativeObj);
    }

    public static FeatureDetector create(int detectorType) {
        FeatureDetector retVal;
        try {
            retVal = FeatureDetector.create(detectorType);
        } catch (CvException ex) {
            System.err.println("Using native xfeatures2d, you can ignore the previous OpenCV Error: Bad argument");
            retVal = new FeatureDetectorX(n_create(detectorType));
            ((FeatureDetectorX) retVal).patched = true;
        }
        return retVal;
    }

    public boolean empty() {
        if (patched)
            return n_empty(this.nativeObj);
        else
            return super.empty();
    }

    public void detect(Mat image, MatOfKeyPoint keypoints, Mat mask) {
        if (patched)
            n_detect_0(this.nativeObj, image.nativeObj, keypoints.nativeObj, mask.nativeObj);
        else
            super.detect(image, keypoints, mask);
    }

    public void detect(Mat image, MatOfKeyPoint keypoints) {
        if (patched)
            n_detect_0(this.nativeObj, image.nativeObj, keypoints.nativeObj);
        else
            super.detect(image, keypoints);
    }

    public void detect(List<Mat> images, List<MatOfKeyPoint> keypoints, List<Mat> masks) {
        if (!patched) {
            super.detect(images, keypoints, masks);
            return;
        }

        Mat images_mat = Converters.vector_Mat_to_Mat(images);
        Mat keypoints_mat = new Mat();
        Mat masks_mat = Converters.vector_Mat_to_Mat(masks);
        n_detect_1(this.nativeObj, images_mat.nativeObj, keypoints_mat.nativeObj, masks_mat.nativeObj);
        Converters.Mat_to_vector_vector_KeyPoint(keypoints_mat, keypoints);
        keypoints_mat.release();
    }

    public void detect(List<Mat> images, List<MatOfKeyPoint> keypoints) {
        if (!patched) {
            super.detect(images, keypoints);
            return;
        }

        Mat images_mat = Converters.vector_Mat_to_Mat(images);
        Mat keypoints_mat = new Mat();
        n_detect_1(this.nativeObj, images_mat.nativeObj, keypoints_mat.nativeObj);
        Converters.Mat_to_vector_vector_KeyPoint(keypoints_mat, keypoints);
        keypoints_mat.release();
    }

    public void read(String fileName) {
        if (patched)
            n_read(this.nativeObj, fileName);
        else
            super.read(fileName);
    }

    public void write(String fileName) {
        if (patched)
            n_write(this.nativeObj, fileName);
        else
            super.write(fileName);
    }

    protected void finalize() throws Throwable {
        if (patched)
            n_delete(this.nativeObj);
        else
            super.finalize();
    }

    private static native boolean n_empty(long nativeObj);

    private static native long n_create(int detectorType);

    private static native void n_detect_0(long nativeObj, long img_nativeObj, long keypoints_nativeObj, long mask_nativeObj);

    private static native void n_detect_0(long nativeObj, long img_nativeObj, long keypoints_nativeObj);

    private static native void n_detect_1(long nativeObj, long imgs_nativeObj, long keypoints_nativeObj, long masks_nativeObj);

    private static native void n_detect_1(long nativeObj, long imgs_nativeObj, long keypoints_nativeObj);

    private static native void n_read(long nativeObj, String fileName);

    private static native void n_write(long nativeObj, String fileName);

    private static native void n_delete(long nativeObj);

}
