package nl.tno.stormcv.bolt;

import backtype.storm.task.TopologyContext;
import backtype.storm.topology.OutputFieldsDeclarer;
import nl.tno.stormcv.StormCVConfig;
import nl.tno.stormcv.model.CVParticle;
import nl.tno.stormcv.operation.ISingleInputOperation;
import xyz.unlimitedcodeworks.utils.Timing;

import java.util.List;
import java.util.Map;

/**
 * A basic {@link CVParticleBolt} implementation that works with single items received (hence maintains no
 * history of items received). The bolt will ask the provided {@link ISingleInputOperation} implementation to
 * work on the input it received and produce zero or more results which will be emitted by the bolt.
 * If an operation throws an exception the input will be failed in all other situations the input will be acked.
 *
 * @author Corne Versloot
 */
public class SingleInputBolt extends CVParticleBolt {

    private static final long serialVersionUID = 8954087163234223475L;

    private ISingleInputOperation<? extends CVParticle> operation;

    protected boolean profiling = false;

    /**
     * Constructs a SingleInputOperation
     *
     * @param operation the operation to be performed
     */
    public SingleInputBolt(ISingleInputOperation<? extends CVParticle> operation) {
        this.operation = operation;
    }

    @SuppressWarnings("rawtypes")
    @Override
    void prepare(Map stormConf, TopologyContext context) {
        if (stormConf.containsKey(StormCVConfig.STORMCV_LOG_PROFILING)) {
            profiling = (Boolean) stormConf.get(StormCVConfig.STORMCV_LOG_PROFILING);
        }
        try {
            operation.prepare(stormConf, context);
        } catch (Exception e) {
            logger.error("Unable to prepare Operation ", e);
        }
    }

    @Override
    public void declareOutputFields(OutputFieldsDeclarer declarer) {
        declarer.declare(operation.getSerializer().getFields());
    }

    @Override
    List<? extends CVParticle> execute(CVParticle input) throws Exception {

        long beginExecute = Timing.currentTimeMillis();
        long endExecute;
        long totalEstimatedSize = 0;
        if (profiling) {
            logger.info("[Timing] RequestID: {} StreamID: {} SequenceNr: {} Entering {}: {}",
                    input.getRequestId(), input.getStreamId(), input.getSequenceNr(),
                    boltName, beginExecute);
        }

        List<? extends CVParticle> result = operation.execute(input);

        // copy metadata from input to output if configured to do so
        for (CVParticle s : result) {
            if (profiling) totalEstimatedSize += s.estimatedByteSize();
            for (String key : input.getMetadata().keySet()) {
                if (!s.getMetadata().containsKey(key)) {
                    s.getMetadata().put(key, input.getMetadata().get(key));
                }
            }
        }

        if (profiling) {
            endExecute = Timing.currentTimeMillis();
            logger.info("[Timing] RequestID: {} StreamID: {} SequenceNr: {} Leaving {}: {} Size: {}",
                    input.getRequestId(), input.getStreamId(), input.getSequenceNr(), boltName,
                    endExecute, totalEstimatedSize);
        }
        return result;
    }
}
