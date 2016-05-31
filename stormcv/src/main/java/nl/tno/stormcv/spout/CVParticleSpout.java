package nl.tno.stormcv.spout;

import backtype.storm.spout.SpoutOutputCollector;
import backtype.storm.task.TopologyContext;
import backtype.storm.topology.IRichSpout;
import backtype.storm.topology.OutputFieldsDeclarer;
import backtype.storm.tuple.Values;
import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import nl.tno.stormcv.StormCVConfig;
import nl.tno.stormcv.fetcher.IFetcher;
import nl.tno.stormcv.model.CVParticle;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import xyz.unlimitedcodeworks.utils.Timing;

import java.io.IOException;
import java.util.Map;
import java.util.concurrent.TimeUnit;

/**
 * Basic spout implementation that includes fault tolerance (if activated). It should be noted that running the spout in fault
 * tolerant mode will use more memory because emitted tuples are cashed for a limited amount of time (which is configurable).
 * <p/>
 * THe actual reading of data is done by {@link IFetcher} implementations creating {@link CVParticle} objects which are serialized
 * and emitted by this spout.
 *
 * @author Corne Versloot
 */
public class CVParticleSpout implements IRichSpout {

    private static final long serialVersionUID = 2828206148753936815L;

    private Logger logger = LoggerFactory.getLogger(CVParticleSpout.class);
    private boolean profiling = false;
    private String spoutName = "";
    private Cache<Object, Object> tupleCache; // a cache holding emitted tuples so they can be replayed on failure
    protected SpoutOutputCollector collector;
    private boolean faultTolerant = false;
    private IFetcher<? extends CVParticle> fetcher;

    public CVParticleSpout(IFetcher<? extends CVParticle> fetcher) {
        this.fetcher = fetcher;
    }

    /**
     * Indicates if this Spout must cache tuples it has emitted so they can be replayed on failure.
     * This setting does not effect anchoring of tuples (which is always done to support
     * TOPOLOGY_MAX_SPOUT_PENDING configuration)
     *
     * @param faultTolerant
     * @return
     */
    public CVParticleSpout setFaultTolerant(boolean faultTolerant) {
        //logger.info(" ************** Setting value of falultTolerant ****************** " + faultTolerant);
        this.faultTolerant = faultTolerant;
        return this;
    }

    /**
     * Configures the spout by fetching optional parameters from the provided configuration.
     * If faultTolerant is true the open function will also construct the cache to hold the
     * emitted tuples. * Configuration options are:
     * <ul>
     * <li>stormcv.faulttolerant --> boolean: indicates if the spout must operate in fault tolerant mode (i.e. replay tuples after failure)</li>
     * <li>stormcv.tuplecache.timeout --> long: timeout (seconds) for tuples in the cache </li>
     * <li>stormcv.tuplecache.maxsize --> int: maximum number of tuples in the cache (used to avoid memory overload)</li>
     * </ul>
     */
    @SuppressWarnings("rawtypes")
    @Override
    public void open(Map conf, TopologyContext context, SpoutOutputCollector collector) {
        this.collector = collector;
        this.spoutName = context.getThisComponentId();
        Object obj = conf.get(StormCVConfig.STORMCV_SPOUT_FAULTTOLERANT);
        if (obj != null) {
            faultTolerant = (Boolean) obj;
        }
        if (faultTolerant) {
            long timeout = conf.get(StormCVConfig.STORMCV_CACHES_TIMEOUT_SEC) == null ? 30 : (Long) conf.get(StormCVConfig.STORMCV_CACHES_TIMEOUT_SEC);
            int maxSize = conf.get(StormCVConfig.STORMCV_CACHES_MAX_SIZE) == null ? 500 : ((Long) conf.get(StormCVConfig.STORMCV_CACHES_MAX_SIZE)).intValue();
            tupleCache = CacheBuilder.newBuilder()
                    .maximumSize(maxSize)
                    .expireAfterAccess(timeout, TimeUnit.SECONDS)
                    .build();
        }

        obj = conf.get(StormCVConfig.STORMCV_LOG_PROFILING);
        if (obj != null) {
            profiling = (Boolean) obj;
        }

        // pass configuration to subclasses
        try {
            fetcher.prepare(conf, context);
        } catch (Exception e) {
            logger.warn("Unable to configure spout due to ", e);
        }
    }

    @Override
    public void declareOutputFields(OutputFieldsDeclarer declarer) {
        declarer.declare(fetcher.getSerializer().getFields());
    }

    @Override
    public void nextTuple() {
        CVParticle particle = fetcher.fetchData();

        if (particle != null) try {
            Values values = fetcher.getSerializer().toTuple(particle);
            String id = new MessageId(particle.getStreamId(),
                                      particle.getSequenceNr(),
                                      particle.getRequestId())
                        .toString();
            if (faultTolerant && tupleCache != null) tupleCache.put(id, values);
            collector.emit(values, id);

            if (profiling) {
                logger.info("[Timing] RequestID: {} StreamID: {} SequenceNr: {} Leaving {}: {} Size: {}",
                            particle.getRequestId(), particle.getStreamId(), particle.getSequenceNr(), spoutName,
                            Timing.currentTimeMillis(), particle.estimatedByteSize());
            }
        } catch (IOException e) {
            logger.warn("Unable to fetch next frame from queue due to: " + e.getMessage());
        }
    }

    @Override
    public void close() {
        if (faultTolerant && tupleCache != null) {
            tupleCache.cleanUp();
        }
        fetcher.deactivate();
    }

    @Override
    public void activate() {
        fetcher.activate();
    }

    @Override
    public void deactivate() {
        fetcher.deactivate();
    }

    @Override
    public void ack(Object msgId) {
        if (faultTolerant && tupleCache != null) {
            tupleCache.invalidate(msgId);
        }
        MessageId mid = new MessageId(msgId);
        logger.info("[Timing] RequestID: {} StreamID: {} SequenceNr: {} Ack {}: {} Size: {}",
                mid.requestId, mid.streamId, mid.sequenceNr,
                "ack", Timing.currentTimeMillis(), 0);
    }

    @Override
    public void fail(Object msgId) {
        MessageId mid = new MessageId(msgId);
        if (faultTolerant && tupleCache != null && tupleCache.getIfPresent(msgId) != null) {
                Values v = (Values) tupleCache.getIfPresent(msgId);
                long requestId = (Long) v.get(0);
                requestId++;
                v.set(0, requestId);
                mid.requestId = requestId;

                if (profiling) {
                    logger.info("[Timing] RequestID: {} StreamID: {} SequenceNr: {} Retry {}: {} Size: {}",
                            mid.requestId, mid.streamId, mid.sequenceNr,
                            spoutName, Timing.currentTimeMillis(), 0);
                }
                collector.emit(v, mid.toString());
        } else {
            if (profiling) {
                logger.info("[Timing] RequestID: {} StreamID: {} SequenceNr: {} Failed {}: {} Size: {}",
                        mid.requestId, mid.streamId, mid.sequenceNr,
                        spoutName, Timing.currentTimeMillis(), 0);
            }
        }
    }

    @Override
    public Map<String, Object> getComponentConfiguration() {
        // TODO Auto-generated method stub
        return null;
    }

    class MessageId {
        public String streamId = "";
        public long sequenceNr = 0;
        public long requestId = 0;

        public MessageId(String streamId, long sequenceNr, long requestId) {
            this.streamId = streamId;
            this.sequenceNr = sequenceNr;
            this.requestId = requestId;
        }

        public MessageId(Object msgId) {
            String[] list = msgId.toString().split("\\|");
            if (list.length != 3) {
                logger.error(String.format("Invalid message id: {}", msgId));
                return;
            }

            streamId = list[0];
            sequenceNr = Integer.valueOf(list[1]);
            requestId = Integer.valueOf(list[2]);
        }

        public String toString() {
            return streamId + "|" + sequenceNr + "|" + requestId;
        }
    }
}
