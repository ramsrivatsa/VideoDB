/**
 * 
 */
package nl.tno.stormcv.example.util;


import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.opencv.core.Mat;
import org.opencv.video.BackgroundSubtractor;
import org.opencv.video.BackgroundSubtractorMOG;
import org.opencv.video.BackgroundSubtractorMOG2;

import backtype.storm.task.TopologyContext;
import nl.tno.stormcv.model.CVParticle;
import nl.tno.stormcv.model.Frame;
import nl.tno.stormcv.model.serializer.CVParticleSerializer;
import nl.tno.stormcv.model.serializer.FrameSerializer;
import nl.tno.stormcv.operation.ISingleInputOperation;
import nl.tno.stormcv.operation.OpenCVOp;
import nl.tno.stormcv.util.ImageUtils;

/**
 * @author John Schavemaker
 *
 */
public class BackgroundSubtractionOp extends OpenCVOp<Frame> implements ISingleInputOperation<Frame>
{
	/**
	 * enumerations
	 */
	public enum BSAlgorithm { MOG, MOG2	}
	
	/**
	 * static class members
	 */
	private static final long serialVersionUID = 6414013958672877897L;
	
	/**
	 * class members
	 */
	@SuppressWarnings("rawtypes")
	private CVParticleSerializer serializer = new FrameSerializer();  // serializer
	private Mat 				 fgMaskMOG  = null; 	              // foreground mask generated by MOG method
	private Mat 				 fgMaskMOG2 = null; 	              // foreground mask generated by MOG2 method
	private BackgroundSubtractor pMOG       = null; 			      // MOG Background subtractor
	private BackgroundSubtractor pMOG2      = null; 		          // MOG2 Background subtractor
	private BSAlgorithm          algorithm  = BSAlgorithm.MOG;        // Background subtraction algorithm (MOG or MOG2)
	
	/**
	 * @return the algorithm
	 */
	public BSAlgorithm getAlgorithm() 
	{
		return algorithm;
	}

	/**
	 * @param the algorithm to set
	 */
	public BackgroundSubtractionOp setAlgorithm(BSAlgorithm algorithm) 
	{
		this.algorithm = algorithm;
		return this;
	}

	/**
	 * class members
	 */
	public BackgroundSubtractionOp() 
	{
		super();
	}

	/**
	 * deactivate
	 */
	@Override
	public void deactivate() {}

	/**
	 * getSerializer
	 */
	@Override
	public CVParticleSerializer<Frame> getSerializer() 
	{
		return serializer;
	}

	/**
	 * execute
	 */
	@Override
	public List<Frame> execute(CVParticle input) throws Exception 
	{
		ArrayList<Frame> result       = new ArrayList<Frame>();
		Frame            input_frame  = null;
		Frame            output_frame = null;
		Mat              input_image  = null;
		Mat              output_image = null;
		
		// sanity check
		if( !( input instanceof Frame ) ) 
			return result;
		
		// initialize input and output result
		input_frame  = (Frame) input;
		output_frame = input_frame;
			
		// check if input frame has an image
		if( input_frame.getImageType().equals(Frame.NO_IMAGE) ) 
			return result;

		// decode input image to OpenCV Mat
		input_image = ImageUtils.bytes2Mat(input_frame.getImageBytes());
		
		// update the background model
	    pMOG.apply(input_image, fgMaskMOG);
	    pMOG2.apply(input_image, fgMaskMOG2);
	    
	    // return output image depending on algorithm choice
	    if ( algorithm == BSAlgorithm.MOG )
	    	output_image = fgMaskMOG;
	    else
	    	output_image = fgMaskMOG2;
	    
	    // convert output image to byte array and set image in output frame
	    byte[] outputBytes = ImageUtils.Mat2ImageBytes( output_image, input_frame.getImageType() );
        output_frame.setImage( outputBytes, input_frame.getImageType() );
		result.add( output_frame );
		
		return result;
	}

	/**
	 * prepareOpenCVOp
	 */
	@Override
	protected void prepareOpenCVOp(Map arg0, TopologyContext arg1)
			throws Exception 
	{
		this.fgMaskMOG  = new Mat();
		this.fgMaskMOG2 = new Mat();
		this.pMOG       = new BackgroundSubtractorMOG();
		this.pMOG2      = new BackgroundSubtractorMOG2();
	}
}
