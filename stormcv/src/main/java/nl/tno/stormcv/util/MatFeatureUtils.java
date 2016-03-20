package nl.tno.stormcv.util;

import nl.tno.stormcv.model.Descriptor;
import nl.tno.stormcv.model.Feature;
import org.opencv.core.Mat;

import java.awt.*;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Created by Aetf (aetf at unlimitedcodeworks dot xyz) on 16-3-19.
 */
public class MatFeatureUtils {
    public static void removeMatMetadataFromFeature(Feature feature) {
        feature.getMetadata().remove("rows");
        feature.getMetadata().remove("cols");
        feature.getMetadata().remove("type");
    }

    public static Mat featureToMat(Feature feature, int index) {
        Map<String, Object> metadata = feature.getMetadata();
        if (metadata == null) return new Mat();

        int cols = (Integer) metadata.getOrDefault("cols", -1);
        int rows = (Integer) metadata.getOrDefault("rows", -1);
        int type = (Integer) metadata.getOrDefault("type", -1);

        if (cols == -1 || rows == -1 || type == -1) return new Mat();

        Descriptor desc = feature.getSparseDescriptors().get(index);
        float[] data = desc.getValues();

        if (data == null) return new Mat();

        Mat mat = new Mat(rows, cols, type);
        mat.put(0, 0, data);
        return mat;
    }

    public static List<Mat> featureToMats(Feature feature) {
        List<Mat> results = new ArrayList<>();
        for (int i = 0; i!= feature.getSparseDescriptors().size(); i++) {
            Mat mat = featureToMat(feature, i);
            if (!mat.empty()) {
                results.add(mat);
            }
        }
        return results;
    }

    public static Feature featureFromMat(String streamId, long sequenceNr, String name, long duration, Rectangle box, Mat mat) {
        int cols = mat.cols();
        int rows = mat.rows();
        int elemSize = (int) mat.elemSize();
        int type = mat.type();

        float[] data = new float[cols * rows * elemSize];
        mat.get(0, 0, data);

        List<Descriptor> descriptors = new ArrayList<>();
        descriptors.add(new Descriptor(streamId, sequenceNr, box, 0, data));

        Feature f = new Feature(streamId, sequenceNr, name, 0, descriptors, null);
        HashMap<String, Object> metadata = new HashMap<>();
        metadata.put("rows", rows);
        metadata.put("cols", cols);
        metadata.put("type", type);
        f.getMetadata().putAll(metadata);

        return f;
    }

    public static Feature featureFromMats(String streamId, long sequenceNr, String name, long duration,
                                          List<Rectangle> boxes, List<Mat> mats) {
        int number = boxes.size() > mats.size() ? mats.size() : boxes.size();
        Feature res = null;
        for (int i = 0; i != number; i++) {
            Feature f = featureFromMat(streamId, sequenceNr, name, duration, boxes.get(i), mats.get(i));
            if (res == null) {
                res = f;
            } else {
                res.getSparseDescriptors().addAll(f.getSparseDescriptors());
            }
        }
        return res;
    }
}
