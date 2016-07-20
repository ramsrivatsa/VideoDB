package nl.tno.stormcv.deploy;

import backtype.storm.Config;
import backtype.storm.StormSubmitter;
import backtype.storm.task.TopologyContext;
import backtype.storm.topology.TopologyBuilder;
import nl.tno.stormcv.StormCVConfig;
import nl.tno.stormcv.bolt.SingleInputBolt;
import nl.tno.stormcv.model.CVParticle;
import nl.tno.stormcv.model.Frame;
import nl.tno.stormcv.model.serializer.CVParticleSerializer;
import nl.tno.stormcv.model.serializer.FrameSerializer;
import nl.tno.stormcv.operation.ISingleInputOperation;
import nl.tno.stormcv.spout.CVParticleSpout;
import nl.tno.stormcv.utils.OpBuilder;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Created by Aetf (aetf at unlimitedcodeworks dot xyz) on 16-3-19.
 */
public class SpoutOnly {
    public static void main(String[] args) {
        // process args
        final String switchKeyword = "--";
        int maxSpoutPending = 128;
        int msgTimeout = 25;
        int cacheTimeout = 30;
        int numWorkers = 1;
        for (String arg : args) {
            if (arg.startsWith(switchKeyword)) {
                String[] kv = arg.substring(switchKeyword.length()).split("=");
                if (kv.length != 2) continue;
                int value;
                try {
                    value = Integer.parseInt(kv[1]);
                } catch (NumberFormatException ex) {
                    continue;
                }
                switch (kv[0]) {
                    case "num-workers":
                        numWorkers = value;
                        break;
                    case "max-spout-pending":
                        maxSpoutPending = value;
                        break;
                    case "msg-timeout":
                        msgTimeout = value;
                        break;
                    case "cache-timeout":
                        cacheTimeout = value;
                        break;
                }
            }
        }
        OpBuilder opBuilder = new OpBuilder(args);

        // first some global (topology configuration)
        StormCVConfig conf = new StormCVConfig();

        // number of workers in the topology
        conf.setNumWorkers(numWorkers);
        // maximum un-acked/un-failed frames per spout (spout blocks if this number is reached)
        conf.setMaxSpoutPending(maxSpoutPending);
        // indicates frames will be encoded as JPG throughout the topology
        conf.put(StormCVConfig.STORMCV_FRAME_ENCODING, Frame.JPG_IMAGE);
        // True if Storm should timeout messages or not.
        conf.put(Config.TOPOLOGY_ENABLE_MESSAGE_TIMEOUTS, true);
        // The maximum amount of time given to the topology to fully process a message emitted by a spout (default = 30)
        conf.put(Config.TOPOLOGY_MESSAGE_TIMEOUT_SECS, msgTimeout);
        // indicates if the spout must be fault tolerant
        conf.put(StormCVConfig.STORMCV_SPOUT_FAULTTOLERANT, true);
        // TTL (seconds) for all elements in all caches throughout the topology (avoids memory overload)
        conf.put(StormCVConfig.STORMCV_CACHES_TIMEOUT_SEC, cacheTimeout);

        // Internal message buffers
        //conf.put(Config.TOPOLOGY_TRANSFER_BUFFER_SIZE,            32);
        //conf.put(Config.TOPOLOGY_EXECUTOR_RECEIVE_BUFFER_SIZE, 16384);
        //conf.put(Config.TOPOLOGY_EXECUTOR_SEND_BUFFER_SIZE,    16384);


        // Enable time profiling for spout and bolt
        conf.put(StormCVConfig.STORMCV_LOG_PROFILING, true);

        // now create the topology itself
        // (spout -> scale -> fat[face detection & dnn] -> drawer -> streamer)
        TopologyBuilder builder = new TopologyBuilder();
        builder.setSpout("fetcher", new CVParticleSpout(opBuilder.buildFetcher()),
                1);
        builder.setBolt("noop", new SingleInputBolt(new ISingleInputOperation<CVParticle>() {
            private long lastSequence = -1;
            @Override
            public List<CVParticle> execute(CVParticle cvParticle) throws Exception {
                List<CVParticle> res = new ArrayList<>();
                res.add(cvParticle);
                if (cvParticle.getSequenceNr() != lastSequence + 1) {
                    LoggerFactory.getLogger("order_verifier").warn("Out-of-Order frame {}, should be {}",
                            cvParticle.getSequenceNr(), lastSequence);
                } else {
                    lastSequence += 1;
                }
                return res;
            }

            @Override
            public void prepare(Map map, TopologyContext topologyContext) throws Exception {

            }

            @Override
            public void deactivate() { }

            private FrameSerializer serializer = new FrameSerializer();
            @Override
            public CVParticleSerializer getSerializer() {
                return serializer;
            }
        }), 1).shuffleGrouping("fetcher");
        /*
        builder.setBolt("noop", new BaseBasicBolt() {
            @Override
            public void execute(Tuple tuple, BasicOutputCollector basicOutputCollector) {
            }

            @Override
            public void declareOutputFields(OutputFieldsDeclarer outputFieldsDeclarer) {
                outputFieldsDeclarer.declare(new Fields());
            }
        });
        */

        try {

            // run in local mode
            //LocalCluster cluster = new LocalCluster();
            //cluster.submitTopology("spout_only", conf, builder.createTopology());
            //Utils.sleep(120 * 1000); // run for two minutes and then kill the topology
            //cluster.shutdown();

            // run on a storm cluster
            StormSubmitter.submitTopology("spout_only", conf, builder.createTopology());
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
