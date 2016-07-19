package nl.tno.stormcv.deploy;

import backtype.storm.Config;
import backtype.storm.StormSubmitter;
import backtype.storm.topology.TopologyBuilder;
import nl.tno.stormcv.StormCVConfig;
import nl.tno.stormcv.batcher.RandomBatcher;
import nl.tno.stormcv.bolt.BatchInputBolt;
import nl.tno.stormcv.bolt.SingleInputBolt;
import nl.tno.stormcv.model.Frame;
import nl.tno.stormcv.operation.DnnForwardOp;
import nl.tno.stormcv.operation.FrameGrouperOp;
import nl.tno.stormcv.operation.ResultSinkOp;
import nl.tno.stormcv.operation.ScaleImageOp;
import nl.tno.stormcv.spout.CVParticleSpout;
import nl.tno.stormcv.utils.OpBuilder;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by Aetf (aetf at unlimitedcodeworks dot xyz) on 16-7-19.
 */
public class CaptionerTopology {
    public static void main(String[] args) {
        // process args
        final String switchKeyword = "--";
        int scaleHint = 1;
        int vggHint = 1;
        int grouperHint = 1;
        int captionerHint = 10;

        int minGroupSize = 1;
        int maxGroupSize = 10;

        int maxSpoutPending = 128;
        int msgTimeout = 25;
        int cacheTimeout = 30;
        boolean autoSleep = false;
        int numWorkers = 1;
        int sleepMs = 40;
        int sendingFps = 0;
        int frameSkip = 1;
        int startDelay = 0;
        String fetcherType = "video";
        List<String> files = new ArrayList<>();
        for (String arg : args) {
            if (arg.startsWith(switchKeyword)) {
                String[] kv = arg.substring(switchKeyword.length()).split("=");
                if (kv.length != 2) continue;
                int value = 1;
                try {
                    value = Integer.parseInt(kv[1]);
                } catch (NumberFormatException ex) {
                    // nothing
                }
                switch (kv[0]) {
                    case "min-group-size":
                        minGroupSize = value;
                        break;
                    case "max-group-size":
                        maxGroupSize = value;
                        break;
                    case "start-delay":
                        startDelay = value;
                        break;
                    case "fps":
                        sleepMs = 1000 / value;
                        //sleepMs = 0;
                        sendingFps = value;
                        break;
                    case "frame-skip":
                        frameSkip = value;
                        break;
                    case "fetcher":
                        fetcherType = kv[1];
                        break;
                    case "num-workers":
                        numWorkers = value;
                        break;
                    case "scale":
                        scaleHint = value;
                        break;
                    case "vgg":
                        vggHint = value;
                        break;
                    case "grouper":
                        grouperHint = value;
                        break;
                    case "captioner":
                        captionerHint = value;
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
                    case "auto-sleep":
                        autoSleep = value != 0;
                        break;
                }
            } else {
                // Multiple files will be spread over the available spouts
                files.add("file://" + arg);
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

        builder.setSpout("fetcher", new CVParticleSpout(opBuilder.buildFetcher()), 1);

        builder.setBolt("scale", new SingleInputBolt(new ScaleImageOp(0.5f)), scaleHint)
               .shuffleGrouping("fetcher");

        DnnForwardOp dnnforward = opBuilder.buildVggNet("vgg", "fc7").retainImage(false);
        builder.setBolt("vgg_feature", new SingleInputBolt(dnnforward), vggHint)
                .shuffleGrouping("scale");

        builder.setBolt("frame_grouper", new BatchInputBolt(
                    new RandomBatcher(minGroupSize, maxGroupSize),
                    new FrameGrouperOp()),
                1).shuffleGrouping("vgg_feature");

        builder.setBolt("captioner", new SingleInputBolt(opBuilder.buildCaptioner("caption", "vgg")),
                    captionerHint)
                .shuffleGrouping("frame_grouper");

        builder.setBolt("streamer", new SingleInputBolt(new ResultSinkOp().port(8558).topNumber(20)),
                1)
                .shuffleGrouping("captioner");

        try {
            // run in local mode
            //LocalCluster cluster = new LocalCluster();
            //cluster.submitTopology("dnn_classification", conf, builder.createTopology());
            //Utils.sleep(120 * 1000); // run for two minutes and then kill the topology
            //cluster.shutdown();

            // run on a storm cluster
            StormSubmitter.submitTopology("captioning", conf, builder.createTopology());
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
