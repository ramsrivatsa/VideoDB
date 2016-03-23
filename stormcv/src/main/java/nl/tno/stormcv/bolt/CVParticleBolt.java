package nl.tno.stormcv.bolt;

import backtype.storm.Config;
import backtype.storm.task.OutputCollector;
import backtype.storm.task.TopologyContext;
import backtype.storm.topology.base.BaseRichBolt;
import backtype.storm.tuple.Tuple;
import clojure.lang.PersistentArrayMap;
import nl.tno.stormcv.StormCVConfig;
import nl.tno.stormcv.model.CVParticle;
import nl.tno.stormcv.model.serializer.CVParticleSerializer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * StormCV's basic BaseRichBolt implementation that supports the use of {@link CVParticle} objects.
 * This bolt supports fault tolerance if it is configured to do so and supports the serialization of model objects.
 *
 * @author Corne Versloot
 */
public abstract class CVParticleBolt extends BaseRichBolt {

    private static final long serialVersionUID = -5421951488628303992L;

    protected Logger logger = LoggerFactory.getLogger(CVParticleBolt.class);
    protected boolean profiling = false;
    protected HashMap<String, CVParticleSerializer<? extends CVParticle>> serializers = new HashMap<>();
    protected OutputCollector collector;
    protected String boltName;
    protected long idleTimestamp = -1;

    @SuppressWarnings({"rawtypes", "unchecked"})
    @Override
    public void prepare(Map conf, TopologyContext context, OutputCollector collector) {
        this.collector = collector;
        this.boltName = context.getThisComponentId();
        if (conf.containsKey(StormCVConfig.STORMCV_LOG_PROFILING)) {
            profiling = (Boolean) conf.get(StormCVConfig.STORMCV_LOG_PROFILING);
        }

        try {
            PersistentArrayMap map = (PersistentArrayMap) conf.get(Config.TOPOLOGY_KRYO_REGISTER);
            for (Object className : map.keySet()) {
                serializers.put((String) className,
                        (CVParticleSerializer<? extends CVParticle>) Class.forName((String) map.get(className)).newInstance());
            }
        } catch (Exception e) {
            logger.error("Unable to prepare CVParticleBolt due to ", e);
        }

        this.prepare(conf, context);
    }

    @SuppressWarnings({"rawtypes"})
    @Override
    public void execute(Tuple input) {
        boolean hadError = false;
        long beginExecute = System.currentTimeMillis();
        long endExecute;
        try {
            CVParticle cvt = deserialize(input);

            if (profiling) {
                logger.info("[Timing] StreamID: {} SequenceNr: {} Entering {}: {}",
                        cvt.getStreamId(), cvt.getSequenceNr(), boltName, beginExecute);
            }

            List<? extends CVParticle> results = execute(cvt);
            for (CVParticle output : results) {
                output.setRequestId(cvt.getRequestId());
                CVParticleSerializer serializer = serializers.get(output.getClass().getName());
                if (serializer != null) {
                    collector.emit(input, serializer.toTuple(output));
                } else {
                    logger.error("Can't get serializer " + output.getClass().getName());
                    hadError = true;
                    break;
                }
            }

            if (profiling) {
                endExecute = System.currentTimeMillis();
                logger.info("[Timing] StreamID: {} SequenceNr: {} Leaving {}: {} (latency {})",
                        cvt.getStreamId(), cvt.getSequenceNr(), boltName, endExecute, endExecute - beginExecute);
            }
        } catch (Exception e) {
            logger.warn("Unable to process input", e);
            hadError = true;
        }

        if (hadError) {
            collector.fail(input);
        } else {
            collector.ack(input);
        }

        idleTimestamp = System.currentTimeMillis();
    }

    /**
     * Deserializes a Tuple into a CVParticle type
     *
     * @param tuple
     * @return
     * @throws IOException
     */
    protected CVParticle deserialize(Tuple tuple) throws IOException {
        String typeName = tuple.getStringByField(CVParticleSerializer.TYPE);
        return serializers.get(typeName).fromTuple(tuple);
    }

    /**
     * Subclasses must implement this method which is responsible for analysis of
     * received CVParticle objects. A single input object may result in zero or more
     * resulting objects which will be serialized and emitted by this Bolt.
     *
     * @param input
     * @return
     */
    abstract List<? extends CVParticle> execute(CVParticle input) throws Exception;

    @SuppressWarnings("rawtypes")
    abstract void prepare(Map stormConf, TopologyContext context);
}
