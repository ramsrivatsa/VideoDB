package nl.tno.stormcv.operation;

import backtype.storm.task.TopologyContext;
import backtype.storm.utils.Utils;
import nl.tno.stormcv.model.CVParticle;
import nl.tno.stormcv.model.Frame;
import nl.tno.stormcv.model.serializer.CVParticleSerializer;
import nl.tno.stormcv.model.serializer.FrameSerializer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Created by Aetf (aetf at unlimitedcodeworks dot xyz) on 16-7-18.
 */
public class SimpleLoopOp implements ISingleInputOperation<Frame>, IBatchOperation<Frame>{
    private static final long serialVersionUID = -8518918556266893232L;
    private Logger logger = LoggerFactory.getLogger(SimpleLoopOp.class);

    private long sleepMs;

    /**
     * Creates a Noop operation that will jusp sleep specific time (in ms) and pass on.
     * @param sleepMs
     */
    public SimpleLoopOp(long sleepMs){
        this.sleepMs = sleepMs;
    }

    @Override
    public void prepare(@SuppressWarnings("rawtypes") Map stormConf , TopologyContext context) throws Exception {	}

    @Override
    public void deactivate() {	}

    @Override
    public CVParticleSerializer<Frame> getSerializer() {
        return new FrameSerializer();
    }

    @Override
    public List<Frame> execute(CVParticle particle) throws Exception {
        List<Frame> result = new ArrayList<Frame>();
        if(!(particle instanceof Frame)) {
            logger.error("particle not instance of Frame");
            logger.error("It is {}", particle.getClass().getName());
            return result;
        }

        Frame frame = (Frame) particle;

        Utils.sleep(sleepMs);

        result.add(frame);
        return result;
    }

    @Override
    public List<Frame> execute(List<CVParticle> inputs) throws Exception {
        List<Frame> result = new ArrayList<>();

        for (CVParticle cvt : inputs) {
            result.addAll(execute(cvt));
        }

        if (result.size() != inputs.size()) {
            logger.error("Output batch size mismatch input batch size!!");
        }

        return result;
    }
}
