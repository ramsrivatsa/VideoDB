package nl.tno.stormcv.operation;

import backtype.storm.task.TopologyContext;
import nl.tno.stormcv.model.CVParticle;
import nl.tno.stormcv.model.Feature;
import nl.tno.stormcv.model.Frame;
import nl.tno.stormcv.model.GroupOfFrames;
import nl.tno.stormcv.model.serializer.GroupOfFramesSerializer;
import nl.tno.stormcv.util.MatFeatureUtils;
import org.opencv.core.Mat;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import xyz.unlimitedcodeworks.operations.extra.Captioner;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Created by Aetf (aetf at unlimitedcodeworks dot xyz) on 16-7-19.
 */
public class CaptionerOp  implements ISingleInputOperation<GroupOfFrames> {
    private static final long serialVersionUID = 889734984649506398L;
    protected Logger logger = LoggerFactory.getLogger(CaptionerOp.class);

    private String outputMetaName;
    private String vocabFile;
    private String lstmProto;
    private String modelBin;
    private String vggFeatureName;
    private Captioner captioner;

    public CaptionerOp(String outputMetaName, String vocabFile, String lstmProto,
                       String modelBin, String vggFeatureName) {
        this.outputMetaName = outputMetaName;
        this.vocabFile = vocabFile;
        this.lstmProto = lstmProto;
        this.modelBin = modelBin;
        this.vggFeatureName = vggFeatureName;
    }

    @Override
    public void prepare(Map stormConf, TopologyContext context) throws Exception {
        captioner = new Captioner(vocabFile, lstmProto, modelBin);
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
                if (ft.getName() == vggFeatureName) {
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
