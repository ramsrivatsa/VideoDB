package nl.tno.stormcv.operation;

import backtype.storm.task.TopologyContext;
import nl.tno.stormcv.model.CVParticle;
import nl.tno.stormcv.model.Descriptor;
import nl.tno.stormcv.model.Feature;
import nl.tno.stormcv.model.Frame;
import nl.tno.stormcv.model.serializer.CVParticleSerializer;
import nl.tno.stormcv.model.serializer.FeatureSerializer;
import nl.tno.stormcv.model.serializer.FrameSerializer;
import nl.tno.stormcv.util.MatFeatureUtils;
import nl.tno.stormcv.util.NativeUtils;
import org.opencv.core.Core;
import org.opencv.core.Mat;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Process the features extracted by a previous DnnForwardOp.
 * The feature outputName to process must be given on construction
 *
 * @author Aetf
 */
public class DnnClassifyOp extends OpenCVOp<CVParticle> implements ISingleInputOperation<CVParticle> {
    private static final long serialVersionUID = 1672563550721443006L;
    private Logger logger = LoggerFactory.getLogger(DnnClassifyOp.class);
    private String name;
    private boolean addMetadata = false;
    private String classNamePath;
    private List<String> classNames = new ArrayList<>();

    @SuppressWarnings("rawtypes")
    private CVParticleSerializer serializer = new FeatureSerializer();

    /**
     * Constructs a {@link DnnClassifyOp}, which processes features with name
     * and will emit the processed feature under the same name
     *
     * @param name the feature name to process
     * @param classNamePath path to a simple txt file contains class id -> class name
     */
    public DnnClassifyOp(String name, String classNamePath) {
        this.name = name;
        this.classNamePath = classNamePath;
    }

    /**
     * Whether to add class name as metadata
     * @param value new value
     * @return this instance for chaining
     */
    public DnnClassifyOp addMetadata(boolean value) {
        this.addMetadata = value;
        return this;
    }

    /**
     * Sets the output of this Operation to be a {@link Frame} which contains all the features.
     * If set to false * this Operation will return each {@link Feature} separately.
     * Default value after construction is FALSE
     *
     * @param frame new value
     * @return this instance for chaining
     */
    public DnnClassifyOp outputFrame(boolean frame) {
        if (frame) {
            this.serializer = new FrameSerializer();
        } else {
            this.serializer = new FeatureSerializer();
        }
        return this;
    }

    @Override
    protected void prepareOpenCVOp(Map stormConf, TopologyContext context) throws IOException {
        File f = NativeUtils.getAsLocalFile(classNamePath);
        try (BufferedReader reader = new BufferedReader(new FileReader(f))) {
            String line = reader.readLine();
            while (line != null) {
                classNames.add(line.substring(line.indexOf(' ') + 1));
                line = reader.readLine();
            }
        }
    }

    @Override
    public List<CVParticle> execute(CVParticle input) throws Exception {
        List<CVParticle> result = new ArrayList<>();

        if (input instanceof Frame) {
            Frame frame = (Frame) input;
            List<Feature> newFeatures = new ArrayList<>();
            for (Feature f : frame.getFeatures()) {
                if (f.getName().equals(name)) {
                    newFeatures.add(processFeature(frame, f));
                } else {
                    newFeatures.add(f);
                }
            }
            frame.getFeatures().clear();
            frame.getFeatures().addAll(newFeatures);
            result.add(frame);
        } else if (input instanceof Feature) {
            Feature f = (Feature) input;
            result.add(processFeature(null, f));
        }
        return result;
    }

    @Override
    public void deactivate() {
    }

    @SuppressWarnings("unchecked")
    @Override
    public CVParticleSerializer<CVParticle> getSerializer() {
            return this.serializer;
    }

    private Feature processFeature(Frame frame, Feature inputFeature) {
        Feature f = inputFeature.deepCopy();
        Map<String, Object> metadata = new HashMap<>();

        List<Descriptor> descriptors = new ArrayList<>();
        for (int i = 0; i!= f.getSparseDescriptors().size(); i++) {
            Mat mat = MatFeatureUtils.featureToMat(f, i);
            Core.MinMaxLocResult minMax = Core.minMaxLoc(mat);
            float[] classification = new float[2];
            classification[0] = (float) minMax.maxLoc.x;
            classification[1] = (float) minMax.maxVal;

            if (addMetadata) {
                metadata.put("classname-"+i, classNames.get((int) minMax.maxLoc.x) + "(" + minMax.maxVal + ")");
            }

            descriptors.add(new Descriptor(inputFeature.getStreamId(), inputFeature.getSequenceNr(),
                    f.getSparseDescriptors().get(i).getBoundingBox(), 0, classification));
        }
        f.getSparseDescriptors().clear();
        f.getSparseDescriptors().addAll(descriptors);

        MatFeatureUtils.removeMatMetadataFromFeature(f);
        if (addMetadata && frame != null)
            frame.getMetadata().putAll(metadata);

        return f;
    }
}
