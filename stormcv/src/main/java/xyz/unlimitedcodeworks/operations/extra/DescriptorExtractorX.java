package xyz.unlimitedcodeworks.operations.extra;

import nl.tno.stormcv.util.NativeUtils;
import org.opencv.core.CvException;
import org.opencv.core.Mat;
import org.opencv.core.MatOfKeyPoint;
import org.opencv.features2d.DescriptorExtractor;
import org.opencv.utils.Converters;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by Aetf (aetf at unlimitedcodeworks dot xyz) on 16-6-23.
 */
public class DescriptorExtractorX extends DescriptorExtractor {
    static {
        try {
            NativeUtils.loadLibrary("opencv_core", true);
            NativeUtils.loadLibrary("stormcv_common");
            NativeUtils.loadLibrary("stormcv_extractorx");
        } catch (Exception ex) {
            System.err.println("Error loading ForwardNet native libraries");
            ex.printStackTrace();
            throw ex;
        }
    }

    protected boolean patched = false;
    protected DescriptorExtractorX(long nativeObj) {
        super(nativeObj);
    }

    public static DescriptorExtractor create(int extractorType) {
        DescriptorExtractor retVal;
        try {
            retVal = DescriptorExtractor.create(extractorType);
        } catch (CvException ex) {
            retVal = new DescriptorExtractorX(n_create(extractorType));
            ((DescriptorExtractorX) retVal).patched = true;
        }
        return retVal;
    }

    public boolean empty() {
        if (!patched)
            return super.empty();
        return n_empty(this.nativeObj);
    }

    public int descriptorSize() {
        if (!patched)
            return super.descriptorSize();
        return n_descriptorSize(this.nativeObj);
    }

    public int descriptorType() {
        if (!patched)
            return super.descriptorType();
        return n_descriptorType(this.nativeObj);
    }

    public void compute(Mat image, MatOfKeyPoint keypoints, Mat descriptors) {
        if (!patched) {
            super.compute(image, keypoints, descriptors);
            return;
        }
        n_compute_0(this.nativeObj, image.nativeObj, keypoints.nativeObj, descriptors.nativeObj);
    }

    public void compute(List<Mat> images, List<MatOfKeyPoint> keypoints, List<Mat> descriptors) {
        if (!patched) {
            super.compute(images, keypoints, descriptors);
            return;
        }
        Mat images_mat = Converters.vector_Mat_to_Mat(images);
        ArrayList keypoints_tmplm = new ArrayList(keypoints != null?keypoints.size():0);
        Mat keypoints_mat = Converters.vector_vector_KeyPoint_to_Mat(keypoints, keypoints_tmplm);
        Mat descriptors_mat = new Mat();
        n_compute_1(this.nativeObj, images_mat.nativeObj, keypoints_mat.nativeObj, descriptors_mat.nativeObj);
        Converters.Mat_to_vector_vector_KeyPoint(keypoints_mat, keypoints);
        keypoints_mat.release();
        Converters.Mat_to_vector_Mat(descriptors_mat, descriptors);
        descriptors_mat.release();
    }

    public void read(String fileName) {
        if (!patched) {
            super.read(fileName);
            return;
        }
        n_read(this.nativeObj, fileName);
    }

    public void write(String fileName) {
        if (!patched) {
            super.write(fileName);
            return;
        }
        n_write(this.nativeObj, fileName);
    }

    protected void finalize() throws Throwable {
        if (!patched) {
            super.finalize();
            return;
        }
        n_delete(this.nativeObj);
    }

    private static native boolean n_empty(long nativeObj);

    private static native int n_descriptorSize(long nativeObj);

    private static native int n_descriptorType(long nativeObj);

    private static native long n_create(int extractorType);

    private static native void n_compute_0(long nativeObj, long img_nativeObj, long keypoints_nativeObj, long descriptors_nativeObj);

    private static native void n_compute_1(long nativeObj, long imgs_nativeObj, long keypoints_nativeObj, long descriptors_nativeObj);

    private static native void n_read(long nativeObj, String var2);

    private static native void n_write(long nativeObj, String var2);

    private static native void n_delete(long nativeObj);

}
