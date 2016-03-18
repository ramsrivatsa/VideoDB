package nl.tno.stormcv.operation;

import backtype.storm.task.TopologyContext;
import nl.tno.stormcv.model.CVParticle;
import nl.tno.stormcv.model.Descriptor;
import nl.tno.stormcv.model.Feature;
import nl.tno.stormcv.model.Frame;
import nl.tno.stormcv.model.serializer.CVParticleSerializer;
import nl.tno.stormcv.model.serializer.FeatureSerializer;
import nl.tno.stormcv.model.serializer.FrameSerializer;
import nl.tno.stormcv.util.NativeUtils;
import org.opencv.core.Mat;
import org.opencv.core.MatOfByte;
import org.opencv.imgcodecs.Imgcodecs;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import xyz.unlimitedcodeworks.opencv.dnn.ForwardNet;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Pass the frames/images through a neural network using OpenCV's dnn module.
 * The model must be provided on construction.
 * @see <a href="http://docs.opencv.org/3.1.0/d6/d0f/group__dnn.html">OpenCV Documentation</a>
 *
 * @author Aetf
 *
 */
public class DnnForwardOp extends OpenCVOp<CVParticle> implements ISingleInputOperation<CVParticle> {
    private static final long serialVersionUID = 1672563550721443006L;
    private Logger logger = LoggerFactory.getLogger(HaarCascadeOp.class);
    private String name;
    private ForwardNet net;
    private String modelTxt;
    private String modelBin;
    private boolean outputFrame = false;

    @SuppressWarnings("rawtypes")
    private CVParticleSerializer serializer = new FeatureSerializer();

    /**
     * Constructs a {@link DnnForwardOp} using the provided caffe model (.prototxt and .caffemodel)
     * and will emit Features with the provided name
     * @param name
     * @param modelBin path to .caffemodel file for the model
     * @param modelTxt path to .prototxt file for the model
     */
    public DnnForwardOp(String name, String modelTxt, String modelBin){
        this.name = name;
        this.modelTxt = modelTxt;
        this.modelBin = modelBin;

        if(modelTxt.charAt(0) != '/') modelTxt = OPENCV_RES_HOME + modelTxt;
        if(modelBin.charAt(0) != '/') modelBin = OPENCV_RES_HOME + modelBin;
    }

    /**
     * Sets the output of this Operation to be a {@link Frame} which contains all the features.
     * If set to false * this Operation will return each {@link Feature} separately.
     * Default value after construction is FALSE
     * @param frame
     * @return
     */
    public DnnForwardOp outputFrame(boolean frame){
        this.outputFrame = frame;
        if(outputFrame){
            this.serializer = new FrameSerializer();
        }else{
            this.serializer = new FeatureSerializer();
        }
        return this;
    }

    @Override
    protected void prepareOpenCVOp(Map stormConf, TopologyContext context) throws Exception {
        try {
            File modelTxtFile = NativeUtils.extractTmpFileFromJar(modelTxt, true);
            File modelBinFile = NativeUtils.extractTmpFileFromJar(modelBin, true);
            net = new ForwardNet(modelTxtFile.getAbsolutePath(), modelBinFile.getAbsolutePath());
        } catch (Exception e) {
            logger.error("Unable to instantiate DnnForwardOp due to: "+e.getMessage(), e);
            throw e;
        }
    }

    @Override
    public List<CVParticle> execute(CVParticle input) throws Exception {
        //long startTime =System.nanoTime();

        List<CVParticle> result = new ArrayList<>();
        Frame frame = (Frame) input;
        if(frame.getImageType().equals(Frame.NO_IMAGE))
            return result;

        MatOfByte mob = new MatOfByte(frame.getImageBytes());
        Mat image = Imgcodecs.imdecode(mob, Imgcodecs.CV_LOAD_IMAGE_COLOR);

        Mat output = net.forward(image);
        float[] data = new float[(int) output.total() * output.channels()];
        output.get(0, 0, data);

        List<Descriptor> descriptors = new ArrayList<>();
        descriptors.add(new Descriptor(input.getStreamId(),
                                       input.getSequenceNr(),
                                       frame.getBoundingBox(), 0,
                                       data));

        result.add(new Feature(input.getStreamId(),
                               input.getSequenceNr(),
                               name, 0,
                               descriptors, null));
        return result;
    }

    @Override
    public void deactivate() {
        net = null;
    }

    @Override
    public CVParticleSerializer<CVParticle> getSerializer() {
        return this.serializer;
    }
}
