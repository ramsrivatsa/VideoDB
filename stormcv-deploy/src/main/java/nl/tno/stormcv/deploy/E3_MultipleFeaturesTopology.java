package nl.tno.stormcv.deploy;

import backtype.storm.Config;
import backtype.storm.StormSubmitter;
import backtype.storm.topology.TopologyBuilder;
import backtype.storm.tuple.Fields;
import nl.tno.stormcv.StormCVConfig;
import nl.tno.stormcv.batcher.SequenceNrBatcher;
import nl.tno.stormcv.batcher.SlidingWindowBatcher;
import nl.tno.stormcv.bolt.BatchInputBolt;
import nl.tno.stormcv.bolt.SingleInputBolt;
import nl.tno.stormcv.model.Frame;
import nl.tno.stormcv.model.serializer.FrameSerializer;
import nl.tno.stormcv.operation.*;
import nl.tno.stormcv.spout.CVParticleSpout;
import nl.tno.stormcv.utils.OpBuilder;
import org.opencv.features2d.DescriptorExtractor;
import org.opencv.features2d.FeatureDetector;

public class E3_MultipleFeaturesTopology {

	public static void main(String[] args){
        // process args
        final String switchKeyword = "--";
        int scaleHint = 1;
        int faceHint = 8;
        int siftHint = 36;
        int drawerHint = 5;
        int maxSpoutPending = 128;
        int msgTimeout = 25;
        int cacheTimeout = 30;
        int numWorkers = 1;
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
                    case "sift":
                        siftHint = value;
                        break;
                    case "face":
                        faceHint = value;
                        break;
                    case "num-workers":
                        numWorkers = value;
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
		conf.put(Config.TOPOLOGY_MESSAGE_TIMEOUT_SECS , msgTimeout);
        // indicates if the spout must be fault tolerant; i.e. spouts do NOT! replay tuples on fail
		conf.put(StormCVConfig.STORMCV_SPOUT_FAULTTOLERANT, true);
        // TTL (seconds) for all elements in all caches throughout the topology (avoids memory overload)
		conf.put(StormCVConfig.STORMCV_CACHES_TIMEOUT_SEC, cacheTimeout);

        // Enable time profiling for spout and bolt
        conf.put(StormCVConfig.STORMCV_LOG_PROFILING, true);


		// now create the topology itself
        // (spout -> scale -> {face detection, sift} -> drawer -> streamer)
		TopologyBuilder builder = new TopologyBuilder();

		 // just one spout reading video files, extracting 1 frame out of 25 (i.e. 1 per second)
		builder.setSpout("spout", new CVParticleSpout(opBuilder.buildFetcher()), 1);
		
		// add bolt that scales frames down to 25% of the original size 
		builder.setBolt("scale", new SingleInputBolt( new ScaleImageOp(0.25f)), scaleHint)
                .shuffleGrouping("spout");
		
		// one bolt with a HaarCascade classifier detecting faces. This operation outputs a Frame including the Features with detected faces.
		// the xml file must be present on the classpath!
		builder.setBolt("face", new SingleInputBolt(
                        new HaarCascadeOp("face", "lbpcascade_frontalface.xml").outputFrame(true)), faceHint)
                        .shuffleGrouping("scale");

		// add a bolt that performs SIFT keypoint extraction
		builder.setBolt("sift", new SingleInputBolt(
                        new FeatureExtractionOp("sift", FeatureDetector.SIFT,
                                                DescriptorExtractor.SIFT).outputFrame(false)), siftHint)
                        .shuffleGrouping("scale");
		
		// Batch bolt that waits for input from both the face and sift detection bolts and combines them in a single frame object
		builder.setBolt("combiner", new BatchInputBolt(new SequenceNrBatcher(2), new FeatureCombinerOp()), 1)
			.fieldsGrouping("sift", new Fields(FrameSerializer.STREAMID))
			.fieldsGrouping("face", new Fields(FrameSerializer.STREAMID));
		
		// simple bolt that draws Features (i.e. locations of features) into the frame
		builder.setBolt("drawer", new SingleInputBolt(new DrawFeaturesOp()), drawerHint)
			.shuffleGrouping("combiner");
		
		// add bolt that creates a webservice on port 8558 enabling users to view the result
		builder.setBolt("streamer", new BatchInputBolt(
				new SlidingWindowBatcher(2, opBuilder.frameSkip).maxSize(32),
				new MjpegStreamingOp().port(8558).framerate(6)).groupBy(new Fields(FrameSerializer.STREAMID))
			, 1)
			.shuffleGrouping("drawer");

		// NOTE: if the topology is started (locally) go to http://localhost:8558/streaming/tiles and click the image to see the stream!
		
		try {
			
			// run in local mode
			//LocalCluster cluster = new LocalCluster();
			//cluster.submitTopology( "multifeature", conf, builder.createTopology() );
			//Utils.sleep(120*1000); // run two minutes and then kill the topology
			//cluster.shutdown();
			//System.exit(1);
			
			// run on a storm cluster
			StormSubmitter.submitTopology("feature_extraction", conf, builder.createTopology());
		} catch (Exception e){
			e.printStackTrace();
		}
	}

}
