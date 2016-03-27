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
import nl.tno.stormcv.model.Frame;
import nl.tno.stormcv.model.serializer.FrameSerializer;
import nl.tno.stormcv.operation.*;
import nl.tno.stormcv.spout.CVParticleSpout;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by Aetf (aetf at unlimitedcodeworks dot xyz) on 16-3-19.
 */
public class DNNTopology {
    public static void main(String[] args) {

        // first some global (topology configuration)
        StormCVConfig conf = new StormCVConfig();

        // number of workers in the topology
        conf.setNumWorkers(4);
        // maximum un-acked/un-failed frames per spout (spout blocks if this number is reached)
        conf.setMaxSpoutPending(128);
        // indicates frames will be encoded as JPG throughout the topology
        conf.put(StormCVConfig.STORMCV_FRAME_ENCODING, Frame.JPG_IMAGE);
        // True if Storm should timeout messages or not.
        conf.put(Config.TOPOLOGY_ENABLE_MESSAGE_TIMEOUTS, true);
        // The maximum amount of time given to the topology to fully process a message emitted by a spout (default = 30)
        conf.put(Config.TOPOLOGY_MESSAGE_TIMEOUT_SECS, 25);
        // indicates if the spout must be fault tolerant
        conf.put(StormCVConfig.STORMCV_SPOUT_FAULTTOLERANT, true);
        // TTL (seconds) for all elements in all caches throughout the topology (avoids memory overload)
        conf.put(StormCVConfig.STORMCV_CACHES_TIMEOUT_SEC, 30);

        // Internal message buffers
        //conf.put(Config.TOPOLOGY_TRANSFER_BUFFER_SIZE,            32);
        //conf.put(Config.TOPOLOGY_EXECUTOR_RECEIVE_BUFFER_SIZE, 16384);
        //conf.put(Config.TOPOLOGY_EXECUTOR_SEND_BUFFER_SIZE,    16384);


        // Enable time profiling for spout and bolt
        conf.put(StormCVConfig.STORMCV_LOG_PROFILING, true);

        // create a list with files to be processed, in this case just one.
        // Multiple files will be spread over the available spouts
        List<String> files = new ArrayList<>();
        for (String path : args) {
            files.add("file://" + path);
        }

        // specify the list with SingleInputOperations to be executed sequentially by the 'fat' bolt
        List<ISingleInputOperation> operations = new ArrayList<>();
        operations.add(new HaarCascadeOp("face", "haarcascade_frontalface_default.xml"));
        operations.add(new DnnForwardOp("classprob", "/data/bvlc_googlenet.prototxt", "/data/bvlc_googlenet.caffemodel").outputFrame(true));
        operations.add(new DnnClassifyOp("classprob", "/data/synset_words.txt").addMetadata(true).outputFrame(true));
        //operations.add(new FeatureExtractionOp("sift", FeatureDetector.SIFT, DescriptorExtractor.SIFT));

        int frameSkip = 1;
        // now create the topology itself
        // (spout -> scale -> fat[face detection & dnn] -> drawer -> streamer)
        TopologyBuilder builder = new TopologyBuilder();
        builder.setSpout("fetcher", new CVParticleSpout(
                        new FileFrameFetcher(files).frameSkip(frameSkip)),
                1);
        // add bolt that scales frames down to 80% of the original size
        builder.setBolt("scale", new SingleInputBolt(new ScaleImageOp(0.5f)), 1)
                .shuffleGrouping("fetcher");

        // 'fat' bolts containing a SequentialFrameOperation will will emit a Frame object containing the detected features
        builder.setBolt("fat_features", new SingleInputBolt(
                        new SequentialFrameOp(operations).outputFrame(true).retainImage(true)), 52)
                .shuffleGrouping("scale");

        // simple bolt that draws Features (i.e. locations of features) into the frame
        builder.setBolt("drawer", new SingleInputBolt(new DrawFeaturesOp().drawMetadata(true)), 5)
                .shuffleGrouping("fat_features");

        // add bolt that creates a webservice on port 8558 enabling users to view the result
        builder.setBolt("streamer", new BatchInputBolt(
                        new SlidingWindowBatcher(2, frameSkip).maxSize(6),
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
