package nl.tno.stormcv.operation;

import backtype.storm.task.TopologyContext;
import backtype.storm.utils.Utils;
import nl.tno.stormcv.model.CVParticle;
import nl.tno.stormcv.model.Descriptor;
import nl.tno.stormcv.model.Feature;
import nl.tno.stormcv.model.Frame;
import nl.tno.stormcv.model.serializer.CVParticleSerializer;
import nl.tno.stormcv.model.serializer.FeatureSerializer;
import nl.tno.stormcv.model.serializer.FrameSerializer;
import nl.tno.stormcv.util.NativeUtils;
import org.opencv.core.*;
import org.opencv.imgcodecs.Imgcodecs;
import org.opencv.objdetect.CascadeClassifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.awt.*;
import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Detects Haar Cascades in frames/images using OpenCV's CascadeClassifier. 
 * The name of the objects detected as well as the model must be provided on construction. 
 * It is possible to specify the minimum and maximum dimensions of detections as well.
 * @see <a href="http://docs.opencv.org/2.4.8/modules/objdetect/doc/cascade_classification.html">OpenCV Documentation</a>
 * 
 * @author Corne Versloot
 *
 */
public class HaarCascadeOp extends OpenCVOp<CVParticle> implements ISingleInputOperation<CVParticle> {

	private static final long serialVersionUID = 1672563520721443005L;
	private Logger logger = LoggerFactory.getLogger(HaarCascadeOp.class);
	private String name;
	private CascadeClassifier haarDetector;
	private String haarXML;
	private int[] minSize = new int[]{0,0};
	private int[] maxSize = new int[]{1000, 1000};
	private int minNeighbors = 3;
	private int flags = 0;
	private float scaleFactor = 1.1f;
	private boolean outputFrame = false;
	@SuppressWarnings("rawtypes")
	private CVParticleSerializer serializer = new FeatureSerializer();

	/**
	 * Constructs a {@link HaarCascadeOp} using the provided haarXML and will emit Features with the provided name 
	 * @param name
	 * @param haarXML
	 */
	public HaarCascadeOp(String name, String haarXML){
		this.name = name;
		this.haarXML = haarXML;
	}

	public HaarCascadeOp minSize(int w, int h){
		this.minSize = new int[]{w, h};
		return this;
	}
	
	public HaarCascadeOp maxSize(int w, int h){
		this.maxSize = new int[]{w, h};
		return this;
	}
	
	public HaarCascadeOp minNeighbors(int number){
		this.minNeighbors = number;
		return this;
	}
	
	public HaarCascadeOp flags(int f){
		this.flags = f;
		return this;
	}
	
	public HaarCascadeOp scale(float factor){
		this.scaleFactor  = factor;
		return this;
	}
	
	/**
	 * Sets the output of this Operation to be a {@link Frame} which contains all the features. If set to false
	 * this Operation will return each {@link Feature} separately. Default value after construction is FALSE
	 * @param frame
	 * @return
	 */
	public HaarCascadeOp outputFrame(boolean frame){
		this.outputFrame = frame;
		if(outputFrame){
			this.serializer = new FrameSerializer();
		}else{
			this.serializer = new FeatureSerializer();
		}
		return this;
	}
	
	@SuppressWarnings("rawtypes")
	@Override
	protected void prepareOpenCVOp(Map stormConf, TopologyContext context) throws Exception { 
		try {
			if(haarXML.charAt(0) != '/') haarXML = "/"+haarXML;
			File cascadeFile = NativeUtils.extractTmpFileFromJar(haarXML, true);
			haarDetector = new CascadeClassifier(cascadeFile.getAbsolutePath());
			Utils.sleep(100); // make sure the classifier has loaded before removing the tmp xml file
			if(!cascadeFile.delete()) cascadeFile.deleteOnExit();
		} catch (Exception e) {
			logger.error("Unable to instantiate SimpleFaceDetectionBolt due to: "+e.getMessage(), e);
			throw e;
		}
	}
	
	@Override
	public List<CVParticle> execute(CVParticle input) throws Exception {
		long startTime =System.nanoTime();
		ArrayList<CVParticle> result = new ArrayList<CVParticle>();
		Frame frame = (Frame)input;
		if(frame.getImageType().equals(Frame.NO_IMAGE)) return result;

		MatOfByte mob = new MatOfByte(frame.getImageBytes());
		Mat image = Imgcodecs.imdecode(mob, Imgcodecs.CV_LOAD_IMAGE_COLOR);
		
		/*
		mob = new MatOfByte();
		Imgcodecs.imencode(".png", image, mob);
		BufferedImage bi = ImageUtils.bytesToImage(mob.toArray());
		ImageIO.write(bi, "png", new File("testOutput/"+sf.getStreamId()+"_"+sf.getSequenceNr()+".png"));
		*/
		
		MatOfRect haarDetections = new MatOfRect();
		haarDetector.detectMultiScale(image, haarDetections, scaleFactor, minNeighbors, flags, new Size(minSize[0], minSize[1]), new Size(maxSize[0], maxSize[1]));
		ArrayList<Descriptor> descriptors = new ArrayList<Descriptor>();
		for(Rect rect : haarDetections.toArray()){
			Rectangle box = new Rectangle(rect.x, rect.y, rect.width, rect.height);
			descriptors.add(new Descriptor(input.getStreamId(), input.getSequenceNr(), box, 0, new float[0]));
		}
		
		Feature feature = new Feature(input.getStreamId(), input.getSequenceNr(), name, 0, descriptors, null);
		if(outputFrame){
			frame.getFeatures().add(feature);
			result.add(frame);
		}else{
			result.add(feature);
		}
		long endTime = System.nanoTime();
		long latency = endTime - startTime;
		logger.info("Haar Classifier sequence nr : " + input.getSequenceNr());
		//logger.info("Haar Classifier latency : " + latency);
		return result;
	}

	@Override
	public void deactivate() {
		haarDetector = null;
	}

	@SuppressWarnings("unchecked")
	@Override
	public CVParticleSerializer<CVParticle> getSerializer() {
		return this.serializer;
	}

}
