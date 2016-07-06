package nl.tno.stormcv.operation;

import backtype.storm.task.TopologyContext;
import nl.tno.stormcv.model.CVParticle;
import nl.tno.stormcv.model.serializer.CVParticleSerializer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Created by Aetf (aetf at unlimitedcodeworks dot xyz) on 16-7-5.
 */
public class SimpleBatchOp implements IBatchOperation<CVParticle> {

    protected Logger logger = LoggerFactory.getLogger(SequentialFrameOp.class);
    protected ISingleInputOperation<? extends CVParticle> operation;

    public SimpleBatchOp(ISingleInputOperation<? extends CVParticle> operation) {
        this.operation = operation;
    }

    @Override
    public List<CVParticle> execute(List<CVParticle> input) throws Exception {
        List<CVParticle> result = new ArrayList<>();
        for (CVParticle cvt : input) {
            result.addAll(operation.execute(cvt));
        }
        return result;
    }

    @Override
    public void prepare(Map stormConf, TopologyContext context) throws Exception {
        operation.prepare(stormConf, context);
    }

    @Override
    public void deactivate() {
        operation.deactivate();
    }

    @Override
    public CVParticleSerializer<CVParticle> getSerializer() {
        return (CVParticleSerializer<CVParticle>) operation.getSerializer();
    }
}
