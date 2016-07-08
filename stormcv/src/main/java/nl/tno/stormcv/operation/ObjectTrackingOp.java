package nl.tno.stormcv.operation;

import backtype.storm.task.TopologyContext;
import nl.tno.stormcv.model.CVParticle;
import nl.tno.stormcv.model.Descriptor;
import nl.tno.stormcv.model.Feature;
import nl.tno.stormcv.model.Frame;
import nl.tno.stormcv.model.serializer.CVParticleSerializer;
import nl.tno.stormcv.model.serializer.FeatureSerializer;
import nl.tno.stormcv.model.serializer.FrameSerializer;
import org.opencv.core.Mat;
import org.opencv.core.MatOfByte;
import org.opencv.core.Rect;
import org.opencv.core.RotatedRect;
import org.opencv.imgcodecs.Imgcodecs;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import xyz.unlimitedcodeworks.operations.extra.CMTTracker;

import java.awt.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * An operation used to detect and tracking objects
 * @author Aetf
 */
public class ObjectTrackingOp extends OpenCVOp<CVParticle>
        implements ISingleInputOperation<CVParticle>, IBatchOperation<CVParticle> {

    private static final long serialVersionUID = 3575211578440683490L;
    private Logger logger = LoggerFactory.getLogger(getClass());
    private String featureName;
    private boolean outputFrame = false;
    @SuppressWarnings("rawtypes")
    private CVParticleSerializer serializer = new FeatureSerializer();

    private CMTTracker tracker = null;
    private Rect roi;

    /**
     * @param featureName    the name of the feature which will be put in the generated Feature's name field
     * @param roi            region of interest, the bounding box that you'd like to track
     */
    public ObjectTrackingOp(String featureName, Rect roi) {
        this.featureName = featureName;
        this.roi = roi;
    }

    /**
     * Sets the output of this Operation to be a {@link Frame} which contains all the features. If set to false
     * this Operation will return a {@link Feature} object which means the Frame will no longer be available.
     * Default value after construction is FALSE.
     *
     * @param frame the new value
     * @return this instance for chaining
     */
    public ObjectTrackingOp outputFrame(boolean frame) {
        this.outputFrame = frame;
        if (outputFrame) {
            this.serializer = new FrameSerializer();
        } else {
            this.serializer = new FeatureSerializer();
        }
        return this;
    }

    @SuppressWarnings("rawtypes")
    @Override
    protected void prepareOpenCVOp(Map stormConf, TopologyContext context) throws Exception {
    }

    @Override
    public void deactivate() {
    }

    @SuppressWarnings("unchecked")
    @Override
    public CVParticleSerializer<CVParticle> getSerializer() {
        return this.serializer;
    }

    @Override
    public List<CVParticle> execute(CVParticle particle) throws Exception {
        List<CVParticle> result = new ArrayList<>();
        if (!(particle instanceof Frame)) return result;

        Frame frame = (Frame) particle;
        if (frame.getImageType().equals(Frame.NO_IMAGE)) return result;
        try {
            MatOfByte mob = new MatOfByte(frame.getImageBytes());
            Mat image = Imgcodecs.imdecode(mob, Imgcodecs.CV_LOAD_IMAGE_ANYCOLOR);

            if (image.empty()) {
                logger.error("!!!!!!!!!!!!!!!!!At StreamID: {}, Sequence Nr: {}!!!!!!!!!!!!!!!!!!!!!!!!!!Got a empty image even after check", frame.getStreamId(), frame.getSequenceNr());
                return result;
            }

            ensureTracker(image);
            tracker.trackImage(image);
            RotatedRect rr = tracker.currentPosition();

            List<Descriptor> descriptors = new ArrayList<>();
            descriptors.add(rotatedRectToDescriptor(frame, rr));
            Feature feature = new Feature(frame.getStreamId(), frame.getSequenceNr(), featureName,
                                          0, descriptors, null);

            if (outputFrame) {
                frame.getFeatures().add(feature);
                result.add(frame);
            } else {
                result.add(feature);
            }
        } catch (Exception e) {
            // catching exception at this point will prevent the sent of a fail!
            logger.warn("Unable to track objects for frame!", e);
        }
        return result;
    }

    @Override
    public List<CVParticle> execute(List<CVParticle> inputs) throws Exception {
        List<CVParticle> result = new ArrayList<>();

        for (CVParticle cvt : inputs) {
            result.addAll(execute(cvt));
        }

        return result;
    }

    private void ensureTracker(Mat frame) {
        if (tracker != null) {
            return;
        }
        tracker = new CMTTracker(roi, frame);
    }

    private Descriptor rotatedRectToDescriptor(Frame frame, RotatedRect rr) {
        float[] values = new float[5];
        values[0] = (float) rr.center.x;
        values[1] = (float) rr.center.y;
        values[2] = (float) rr.size.width;
        values[3] = (float) rr.size.height;
        values[4] = (float) rr.angle;
        Rect br = rr.boundingRect();

        return new Descriptor(frame.getStreamId(), frame.getSequenceNr(),
                              new Rectangle(br.x, br.y, br.width, br.height),
                              0, values);
    }
}

