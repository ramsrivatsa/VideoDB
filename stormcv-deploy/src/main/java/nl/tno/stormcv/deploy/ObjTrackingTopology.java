package nl.tno.stormcv.deploy;

import backtype.storm.Config;
import backtype.storm.StormSubmitter;
import backtype.storm.topology.TopologyBuilder;
import backtype.storm.tuple.Fields;
import nl.tno.stormcv.StormCVConfig;
import nl.tno.stormcv.batcher.SlidingWindowBatcher;
import nl.tno.stormcv.bolt.BatchInputBolt;
import nl.tno.stormcv.bolt.SingleInputBolt;
import nl.tno.stormcv.fetcher.FileFrameFetcher;
import nl.tno.stormcv.fetcher.IFetcher;
import nl.tno.stormcv.fetcher.RefreshingImageFetcher;
import nl.tno.stormcv.model.Frame;
import nl.tno.stormcv.model.serializer.FrameSerializer;
import nl.tno.stormcv.operation.DrawFeaturesOp;
import nl.tno.stormcv.operation.MjpegStreamingOp;
import nl.tno.stormcv.operation.ObjectTrackingOp;
import nl.tno.stormcv.operation.ScaleImageOp;
import nl.tno.stormcv.spout.CVParticleSpout;
import org.opencv.core.Rect;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by Aetf (aetf at unlimitedcodeworks dot xyz) on 16-7-08.
 */
public class ObjTrackingTopology {
    public static void main(String[] args) {
        // process args
        final String switchKeyword = "--";
        int scaleHint = 1;
        int drawerHint = 5;
        int maxSpoutPending = 128;
        int msgTimeout = 25;
        int cacheTimeout = 30;
        boolean autoSleep = false;
        int frameSkip = 1;
        int numWorkers = 1;
        int sleepMs = 40;
        int sendingFps = 0;
        int startDelay = 0;
        Rect roi = new Rect();
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
                    case "roi":
                        String[] svals = kv[1].split(",");
                        double[] vals = new double[4];
                        for (int i = 0; i!= 4; ++i) {
                            vals[i] = Double.valueOf(svals[i]);
                        }
                        roi.set(vals);
                        System.out.println("Using ROI: " + roi.toString());
                        break;
                    case "start-delay":
                        startDelay = value;
                        break;
                    case "fps":
                        sleepMs = 1000 / value;
                        //sleepMs = 0;
                        sendingFps = value;
                        break;
                    case "fetcher":
                        fetcherType = kv[1];
                        break;
                    case "num-workers":
                        numWorkers = value;
                        break;
                    case "frame-skip":
                        frameSkip = value;
                        break;
                    case "drawer":
                        drawerHint = value;
                        break;
                    case "scale":
                        scaleHint = value;
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
        IFetcher fetcher;
        switch(fetcherType) {
            case "video":
                fetcher = new FileFrameFetcher(files).frameSkip(frameSkip)
                            .autoSleep(autoSleep).sleep(sleepMs);
                break;
            default:
            case "image":
                fetcher = new RefreshingImageFetcher(files).sendingFps(sendingFps)
                            .autoSleep(autoSleep).startDelay(startDelay);
        }
        builder.setSpout("fetcher", new CVParticleSpout(fetcher), 1);
        // add bolt that scales frames down to 80% of the original size
        builder.setBolt("scale", new SingleInputBolt(new ScaleImageOp(0.5f)), scaleHint)
                .shuffleGrouping("fetcher");

        builder.setBolt("obj_track", new BatchInputBolt(
                    new SlidingWindowBatcher(2, 0).maxSize(6),
                    new ObjectTrackingOp("obj1", roi).outputFrame(true)).groupBy(new Fields(FrameSerializer.STREAMID)),
                1)
                .shuffleGrouping("scale");

        // simple bolt that draws Features (i.e. locations of features) into the frame
        builder.setBolt("drawer", new SingleInputBolt(new DrawFeaturesOp().drawMetadata(true)),
                drawerHint)
                .shuffleGrouping("obj_track");

        // add bolt that creates a webservice on port 8558 enabling users to view the result
        builder.setBolt("streamer", new BatchInputBolt(
                    new SlidingWindowBatcher(2, 0).maxSize(6),
                    new MjpegStreamingOp().port(8558).framerate(5)).groupBy(new Fields(FrameSerializer.STREAMID)),
                1)
                .shuffleGrouping("drawer");

        try {
            // run in local mode
            //LocalCluster cluster = new LocalCluster();
            //cluster.submitTopology("dnn_classification", conf, builder.createTopology());
            //Utils.sleep(120 * 1000); // run for two minutes and then kill the topology
            //cluster.shutdown();

            // run on a storm cluster
            StormSubmitter.submitTopology("dnn_classification", conf, builder.createTopology());
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
