package nl.tno.stormcv.operation;

import backtype.storm.task.TopologyContext;
import nl.tno.stormcv.model.CVParticle;
import nl.tno.stormcv.model.Feature;
import nl.tno.stormcv.model.Frame;
import nl.tno.stormcv.model.serializer.CVParticleSerializer;
import nl.tno.stormcv.model.serializer.FeatureSerializer;
import nl.tno.stormcv.model.serializer.FrameSerializer;
import nl.tno.stormcv.util.MatFeatureUtils;
import nl.tno.stormcv.util.NativeUtils;
import org.opencv.core.Mat;
import org.opencv.core.MatOfByte;
import org.opencv.imgcodecs.Imgcodecs;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import xyz.unlimitedcodeworks.opencv.dnn.ForwardNet;

import java.awt.*;
import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Pass the frames/images through a neural network using OpenCV's dnn module.
 * The model must be provided on construction.
 *
 * @author Aetf
 * @see <a href="http://docs.opencv.org/3.1.0/d6/d0f/group__dnn.html">OpenCV Documentation</a>
 */
public class DnnForwardOp extends OpenCVOp<CVParticle> implements ISingleInputOperation<CVParticle> {
    private static final long serialVersionUID = 1672563550721443006L;
    private Logger logger = LoggerFactory.getLogger(HaarCascadeOp.class);
    private String name;
    private ForwardNet net;
    private String modelTxt;
    private String modelBin;

    private boolean outputFrame;
    @SuppressWarnings("rawtypes")
    private CVParticleSerializer serializer = new FeatureSerializer();

    /**
     * Constructs a {@link DnnForwardOp} using the provided caffe model (.prototxt and .caffemodel)
     * and will emit Features with the provided name
     *
     * @param name     the feature name to use
     * @param modelBin path to .caffemodel file for the model
     * @param modelTxt path to .prototxt file for the model
     */
    public DnnForwardOp(String name, String modelTxt, String modelBin) {
        this.name = name;
        this.modelTxt = modelTxt;
        this.modelBin = modelBin;

        if (modelTxt.charAt(0) != '/') this.modelTxt = OPENCV_RES_HOME + modelTxt;
        if (modelBin.charAt(0) != '/') this.modelBin = OPENCV_RES_HOME + modelBin;
    }

    /**
     * Sets the output of this Operation to be a {@link Frame} which contains all the features.
     * If set to false * this Operation will return each {@link Feature} separately.
     * Default value after construction is FALSE
     *
     * @param frame new value
     * @return this instance for chaining
     */
    public DnnForwardOp outputFrame(boolean frame) {
        outputFrame = frame;
        if (frame) {
            this.serializer = new FrameSerializer();
        } else {
            this.serializer = new FeatureSerializer();
        }
        return this;
    }

    @Override
    protected void prepareOpenCVOp(Map stormConf, TopologyContext context) throws Exception {
        try {
            File modelTxtFile = NativeUtils.getAsLocalFile(modelTxt);
            File modelBinFile = NativeUtils.getAsLocalFile(modelBin);
            net = new ForwardNet(modelTxtFile.getAbsolutePath(), modelBinFile.getAbsolutePath());
        } catch (Exception e) {
            logger.error("Unable to instantiate DnnForwardOp due to: " + e.getMessage(), e);
            throw e;
        }
    }

    @Override
    public List<CVParticle> execute(CVParticle input) throws Exception {
        List<CVParticle> result = new ArrayList<>();
        if (!(input instanceof Frame)) return result;

        Frame frame = (Frame) input;
        if (frame.getImageType().equals(Frame.NO_IMAGE))
            return result;

        MatOfByte mob = new MatOfByte(frame.getImageBytes());
        Mat image = Imgcodecs.imdecode(mob, Imgcodecs.CV_LOAD_IMAGE_COLOR);

        Mat output = net.forward(image);

        Feature f = MatFeatureUtils.featureFromMat(input.getStreamId(), input.getSequenceNr(),
                name, 0, new Rectangle(0, 0, (int) image.size().width, (int) image.size().height),
                output);

        if (outputFrame) {
            frame.getFeatures().add(f);
        } else {
            result.add(f);
        }
        return result;
    }

    @Override
    public void deactivate() {
        net = null;
    }

    @SuppressWarnings("unchecked")
    @Override
    public CVParticleSerializer<CVParticle> getSerializer() {
        return this.serializer;
    }
}
