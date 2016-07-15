package nl.tno.stormcv.fetcher;

import backtype.storm.task.TopologyContext;
import backtype.storm.utils.Utils;
import nl.tno.stormcv.StormCVConfig;
import nl.tno.stormcv.model.Frame;
import nl.tno.stormcv.model.serializer.CVParticleSerializer;
import nl.tno.stormcv.model.serializer.FrameSerializer;
import nl.tno.stormcv.util.ImageUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import xyz.unlimitedcodeworks.utils.Timing;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.LinkedBlockingQueue;

/**
 * This {@link IFetcher} implementation reads images that refresh constantly. Each url provided will be read
 * each SLEEP milliseconds. Each image will be emitted into the topology as a {@link Frame} object. How often
 * the image is read can be controlled by setting the sleep time (default = 40 ms) 
 * 
 * @author Corne Versloot
 *
 */
public class RefreshingImageFetcher implements IFetcher<Frame> {

	private static final long serialVersionUID = 7578821428365233524L;
	private Logger logger = LoggerFactory.getLogger(getClass());
	private LinkedBlockingQueue<Frame> frameQueue; // queue used to store frames
	private List<String> locations;
	private List<ImageReader> readers;
	private String imageType;
	private boolean autoSleep = false;
	private int startDelay = 0;
    private int sendingFps = 0;

	public RefreshingImageFetcher(List<String> locations){
		this.locations = locations;
	}
	
    /**
     * Try to send frames at FPS `fps`. Note sleeping interferences with this, you can't
     * use these two in the same time.
     * @param fps
     * @return
     */
    public RefreshingImageFetcher sendingFps(int fps) {
        this.sendingFps = fps;
        return this;
    }

    /**
     * Delay before sending out the first frame after activated. The default delay is 0 ms.
     * @param ms
     * @return
     */
    public RefreshingImageFetcher startDelay(int ms) {
        this.startDelay = ms;
        return this;
    }

	/**
	 * Whether enable auto queue size based sleep
	 * @param auto
	 * @return
	 */
	public RefreshingImageFetcher autoSleep(boolean auto) {
		this.autoSleep = auto;
		return this;
	}

	@SuppressWarnings("rawtypes")
	@Override
	public void prepare(Map stormConf, TopologyContext context) throws Exception {
		frameQueue = new LinkedBlockingQueue<Frame>();
		
		if(stormConf.containsKey(StormCVConfig.STORMCV_FRAME_ENCODING)){
			imageType = (String)stormConf.get(StormCVConfig.STORMCV_FRAME_ENCODING);
		}
		
		int nrTasks = context.getComponentTasks(context.getThisComponentId()).size();
		int taskIndex = context.getThisTaskIndex();
		
		// change the list based on the number of tasks working on it
		if(this.locations != null && this.locations.size() > 0){
			int batchSize = (int)Math.floor(locations.size() / nrTasks) + 1;
			int start = batchSize * taskIndex;
			locations = locations.subList(start, Math.min(start + batchSize, locations.size()));
		}
		readers = new ArrayList<ImageReader>();
	}

	@Override
	public CVParticleSerializer<Frame> getSerializer() {
		return new FrameSerializer();
	}

	@Override
	public void activate() {
		for(String location : locations){
			try {
				ImageReader ir = new ImageReader(new URL(location), autoSleep,
                                                 startDelay, sendingFps, frameQueue);
				new Thread(ir).start();
				readers.add(ir);
			} catch (MalformedURLException e) {
				logger.warn(location+" is not a valid URL!");
			}
		}
	}

	@Override
	public void deactivate() {
		for(ImageReader reader : readers){
			reader.stop();
		}
		readers.clear();
		frameQueue.clear();
	}

	@Override
	public Frame fetchData() {
		return frameQueue.poll();
	}
	
	private class ImageReader implements Runnable {

		private Logger logger = LoggerFactory.getLogger(getClass());
		private LinkedBlockingQueue<Frame> frameQueue;
		private URL url;
		private int sequenceNr;
		private boolean running = true;
        private boolean autoSleep = false;
        private int startDelay = 0;

        private int stepRatio = 0;
        private int stepFps = 0;
        private int remainFps = 0;
        private long binSize = 0;
		
		public ImageReader(URL url, boolean autoSleep, int startDelay, int sendingFps, LinkedBlockingQueue<Frame> frameQueue){
			this.url = url;
            this.autoSleep = autoSleep;
            this.startDelay = startDelay;
			this.frameQueue = frameQueue;

            // Pre-compute some values
            if (sendingFps != 0) {
                this.stepRatio = 5;
                this.stepFps = sendingFps / stepRatio;
                this.remainFps = sendingFps - stepFps * stepRatio;
                this.binSize = 1000 / stepRatio;
            }
		}
		
		@Override
		public void run() {
            try{
                BufferedImage image = ImageIO.read(url);
                byte[] buffer = ImageUtils.imageToBytes(image, imageType);
                if (startDelay != 0) {
                    Utils.sleep(startDelay);
                }

                long prevBin = -1;
                int currStepFps = 0;

                while(running){
					long currentTimeMs = Timing.currentTimeMillis();

                    if (stepRatio > 0) {
                        long currBin = currentTimeMs / binSize;
                        if (currBin != prevBin) {
                            prevBin = currBin;
                            currStepFps = 0;
                            continue;
                        }

                        int limit = stepFps;
                        if ((currBin % stepRatio) < remainFps)
                            ++limit;
                        if (currStepFps == limit) {
                            //Utils.sleep(0);
                            continue;
                        }

                        ++currStepFps;
                    }

					Frame frame = new Frame(url.getFile().substring(1), sequenceNr, imageType, buffer, currentTimeMs,
                                            new Rectangle(image.getWidth(), image.getHeight()));
					frame.getMetadata().put("uri", url);

                    logger.info("[Timing] RequestID: {} StreamID: {} SequenceNr: {} Entering queue: {}",
                                0, frame.getStreamId(), sequenceNr, Timing.currentTimeMillis());

					frameQueue.put(frame);
					sequenceNr++;
					if(autoSleep && frameQueue.size() > 20)
                        Utils.sleep(frameQueue.size());
                }
            }catch(Exception e){
                logger.warn("Exception while reading "+url+" : "+e.getMessage());
            }
		}
		
		public void stop(){
			this.running = false;
		}
		
	}

}
