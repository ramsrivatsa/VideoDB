package nl.tno.stormcv.operation;

import backtype.storm.task.TopologyContext;
import nl.tno.stormcv.model.CVParticle;
import nl.tno.stormcv.model.Feature;
import nl.tno.stormcv.model.Frame;
import nl.tno.stormcv.model.GroupOfFrames;
import nl.tno.stormcv.model.serializer.GroupOfFramesSerializer;
import nl.tno.stormcv.util.MatFeatureUtils;
import nl.tno.stormcv.util.NativeUtils;
import org.opencv.core.Mat;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import xyz.unlimitedcodeworks.operations.extra.Captioner;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Captioner using S2VT
 * Created by Aetf (aetf at unlimitedcodeworks dot xyz) on 16-7-19.
 */
public class CaptionerOp extends OpenCVOp<GroupOfFrames>
        implements ISingleInputOperation<GroupOfFrames> {
    private static final long serialVersionUID = 889734984649506398L;
    protected Logger logger = LoggerFactory.getLogger(CaptionerOp.class);

    private String outputMetaName;
    private String vocab;
    private String lstmProto;
    private String modelBin;
    private String vggFeatureName;
    private Captioner captioner;

    private boolean useGPU;
    private int maxGPUNum = -1;

    public CaptionerOp(String outputMetaName, String vocab, String lstmProto,
                       String modelBin, String vggFeatureName, boolean useGPU) {
        this.outputMetaName = outputMetaName;
        this.vocab = vocab;
        this.lstmProto = lstmProto;
        this.modelBin = modelBin;
        this.vggFeatureName = vggFeatureName;
        this.useGPU = useGPU;

        if (vocab.charAt(0) != '/') this.vocab = OPENCV_RES_HOME + vocab;
        if (lstmProto.charAt(0) != '/') this.lstmProto = OPENCV_RES_HOME + lstmProto;
        if (modelBin.charAt(0) != '/') this.modelBin = OPENCV_RES_HOME + modelBin;
    }

    /**
     * How many GPU devices to use when using Caffe on GPU. Use -1 to indicate maximum available number
     * @param num number of GPU to use
     * @return this for chain
     */
    public CaptionerOp maxGPUNum(int num) {
        maxGPUNum = num;
        return this;
    }

    @Override
    public void prepareOpenCVOp(Map stormConf, TopologyContext context) throws Exception {
        int thisTaskIndex = context.getThisTaskIndex();

        try {
            File vocabFile = NativeUtils.getAsLocalFile(vocab);
            File lstmProtoFile = NativeUtils.getAsLocalFile(lstmProto);
            File modelBinFile = NativeUtils.getAsLocalFile(modelBin);
            captioner = new Captioner(vocabFile.getAbsolutePath(), lstmProtoFile.getAbsolutePath(),
                                      modelBinFile.getAbsolutePath(),
                                      useGPU, thisTaskIndex, maxGPUNum);
        } catch (Exception e) {
            logger.error("Unable to instantiate CaptionerOp due to: " + e.getMessage(), e);
            throw e;
        }
    }

    @Override
    public List<GroupOfFrames> execute(CVParticle input) throws Exception {
        List<GroupOfFrames> result = new ArrayList<>();

        if(!(input instanceof GroupOfFrames)) {
            logger.error("particle not instance of Frame, it's {}", input.getClass().getName());
            return result;
        }
        List<Frame> frames = ((GroupOfFrames) input).getFrames();

        List<Mat> vggFeatures = new ArrayList<>();
        for (Frame frame : frames) {
            for (Feature ft : frame.getFeatures()) {
                if (ft.getName().equals(vggFeatureName)) {
                    vggFeatures.add(MatFeatureUtils.featureToMat(ft, 0));
                }
            }
        }

        String caption = captioner.captioning(vggFeatures);

        input.getMetadata().put(outputMetaName, caption);

        result.add((GroupOfFrames) input);
        return result;
    }

    @Override
    public void deactivate() {
    }

    @Override
    public GroupOfFramesSerializer getSerializer() {
        return new GroupOfFramesSerializer();
    }
}
