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
import xyz.unlimitedcodeworks.operations.extra.ForwardNet;

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
public class DnnForwardOp extends OpenCVOp<CVParticle> implements ISingleInputOperation<CVParticle>,
        IBatchOperation<CVParticle> {
    private static final long serialVersionUID = 1672563550721443006L;
    private Logger logger = LoggerFactory.getLogger(DnnForwardOp.class);
    private String name;
    private ForwardNet net;

    private String modelTxt;
    private String modelBin;
    private String outputName;
    private String meanBin;
    private boolean caffeOnCPU;
    private boolean useCaffe;

    private int kernelThreadPriority = 0;
    private long kernelThreadId;
    private int thisTaskIndex;

    private int maxGPUNum = -1;

    private boolean outputFrame;
    @SuppressWarnings("rawtypes")
    private CVParticleSerializer serializer = new FeatureSerializer();

    /**
     * Constructs a {@link DnnForwardOp} using the provided caffe model (.prototxt and .caffemodel)
     * and will emit Features with the provided name
     *
     * @param name       the feature name to use
     * @param modelBin   path to .caffemodel file for the model
     * @param modelTxt   path to .prototxt file for the model
     * @param outputName the name of the output blob
     */
    public DnnForwardOp(String name, String modelTxt, String modelBin, String outputName) {
        this.name = name;
        this.useCaffe = false;
        this.modelTxt = modelTxt;
        this.modelBin = modelBin;
        this.outputName = outputName;

        if (modelTxt.charAt(0) != '/') this.modelTxt = OPENCV_RES_HOME + modelTxt;
        if (modelBin.charAt(0) != '/') this.modelBin = OPENCV_RES_HOME + modelBin;
    }

    public DnnForwardOp(String name, String modelTxt, String modelBin, String outputName,
                        String meanBin, boolean caffeOnCPU) {
        this.name = name;
        this.useCaffe = true;
        this.modelTxt = modelTxt;
        this.modelBin = modelBin;
        this.outputName = outputName;
        this.meanBin = meanBin;
        this.caffeOnCPU = caffeOnCPU;

        if (modelTxt.charAt(0) != '/') this.modelTxt = OPENCV_RES_HOME + modelTxt;
        if (modelBin.charAt(0) != '/') this.modelBin = OPENCV_RES_HOME + modelBin;
        if (meanBin.charAt(0) != '/') this.meanBin = OPENCV_RES_HOME + meanBin;
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

    /**
     * Sets the kernel thread priority of this Operation
     * Default value after construction is 0, which implies SCHED_OTHER
     * Other values implies SCHED_RR, and the value is bounded to range [1,99]
     * Changes only take effect before prepare.
     *
     * @param priority new value
     * @return this instance for chaining
     */
    public DnnForwardOp threadPriority(int priority) {
        kernelThreadPriority = priority;
        if (kernelThreadPriority < 0)
            kernelThreadPriority = 0;
        if (kernelThreadPriority > 99)
            kernelThreadPriority = 99;
        return this;
    }

    /**
     * How many GPU devices to use when using Caffe on GPU. Use -1 to indicate maximum available number
     * @param num
     * @return
     */
    public DnnForwardOp maxGPUNum(int num) {
        maxGPUNum = num;
        return this;
    }

    @Override
    protected void prepareOpenCVOp(Map stormConf, TopologyContext context) throws Exception {
        kernelThreadId = ForwardNet.getCurrentTid();
        thisTaskIndex = context.getThisTaskIndex();

        logger.info("Preparing DnnForwardOp[{}] on thread {}", context.getThisTaskIndex(), kernelThreadId);
        try {
            File modelTxtFile = NativeUtils.getAsLocalFile(modelTxt);
            File modelBinFile = NativeUtils.getAsLocalFile(modelBin);
            if (useCaffe) {
                File meanBinFile = NativeUtils.getAsLocalFile(meanBin);
                net = new ForwardNet(modelTxtFile.getAbsolutePath(), modelBinFile.getAbsolutePath(),
                                     outputName, meanBinFile.getAbsolutePath(),
                                     useCaffe, caffeOnCPU, thisTaskIndex, maxGPUNum);
            } else {
                net = new ForwardNet(modelTxtFile.getAbsolutePath(),
                                     modelBinFile.getAbsolutePath(), outputName);
            }
            net.setThreadPriority(kernelThreadPriority);
        } catch (Exception e) {
            logger.error("Unable to instantiate DnnForwardOp due to: " + e.getMessage(), e);
            throw e;
        }
    }

    private void ensureThreadPriority() {
        // TODO: verify that prepareOpenCVOp is always called in the same kernel thread as execute,
        // and won't change during the whole run. Then we can remove this call.
        long currentTid = ForwardNet.getCurrentTid();
        if (currentTid != kernelThreadId) {
            logger.warn("DnnForwardOp[{}] got moved to a different kernel thread: {} -> {}",
                    thisTaskIndex, kernelThreadId, currentTid);
            kernelThreadId = currentTid;
            net.setThreadPriority(kernelThreadPriority);
        }
    }

    @Override
    public List<CVParticle> execute(CVParticle input) throws Exception {
        ensureThreadPriority();

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
            result.add(frame);
        } else {
            result.add(f);
        }
        return result;
    }

    @Override
    public List<CVParticle> execute(List<CVParticle> input) throws Exception {
        List<CVParticle> validInput = new ArrayList<>();
        List<CVParticle> result = new ArrayList<>();

        for (CVParticle cvt : input) {
            if (!(cvt instanceof Frame)) continue;
            Frame frame = (Frame) cvt;
            if (frame.getImageType().equals(Frame.NO_IMAGE)) continue;

            validInput.add(cvt);
        }

        List<Mat> images = new ArrayList<>();
        List<Feature> features = new ArrayList<>();
        for (CVParticle cvt : validInput) {
            Frame frame = (Frame) cvt;
            MatOfByte mob = new MatOfByte(frame.getImageBytes());
            Mat image = Imgcodecs.imdecode(mob, Imgcodecs.CV_LOAD_IMAGE_COLOR);
            images.add(image);
        }

        List<Mat> output = net.forward(images);

        for (int i = 0; i!= validInput.size(); ++i) {
            Frame frame = (Frame) validInput.get(i);
            Mat image = images.get(i);

            Feature f = MatFeatureUtils.featureFromMat(frame.getStreamId(), frame.getSequenceNr(),
                    name, 0, new Rectangle(0, 0, (int) image.size().width, (int) image.size().height),
                    output.get(i));

            if (outputFrame) {
                frame.getFeatures().add(f);
                result.add(frame);
            } else {
                result.add(f);
            }
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
