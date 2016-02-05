package nl.tno.stormcv.model.serializer;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import nl.tno.stormcv.model.CVParticle;
import backtype.storm.tuple.Fields;
import backtype.storm.tuple.Tuple;
import backtype.storm.tuple.Values;

import com.esotericsoftware.kryo.Kryo;
import com.esotericsoftware.kryo.Serializer;
import com.esotericsoftware.kryo.io.Input;
import com.esotericsoftware.kryo.io.Output;

/**
 * Abstract class used to serialize subclasses of {@link CVParticle} implementations. This class contains the basic methods which can be called by
 * others to (de)serialize objects. Subclasses must be registered to Storm configuration in order for Storm to use them.  
 *  
 * @author Corne Versloot
 *
 * @param <Type> extends GenericType and indicates the objects this serializer will be able to (de)serialize
 */
public abstract class CVParticleSerializer<Type extends CVParticle> extends Serializer<Type>{

	public static final String REQUESTID = "requestID";
	public static final String STREAMID = "streamID";
	public static final String SEQUENCENR = "sequenceNR";
	public static final String TYPE = "type";
	public static final String METADATA = "metadata";

	/**
	 * Generates a Type Object from the provided tuple
	 * @param tuple
	 * @return
	 * @throws IOException
	 */
	public Type fromTuple(Tuple tuple) throws IOException{
		Type type = createObject(tuple);
		return type;
	}
	
	/**
	 * Converts a Type Object to a Values tuple which can be emitted by storm
	 * @param object
	 * @return
	 * @throws IOException
	 */
	public Values toTuple(CVParticle object) throws IOException{
		Values values = new Values(object.getRequestId(), object.getClass().getName(), object.getStreamId(), object.getSequenceNr(), object.getMetadata());
		values.addAll(getValues(object));
		return values;
	}
	
	/**
	 * Returns the field names of the tuple generated by this serializer (also see toTuple)
	 * @return
	 */
	public Fields getFields(){
		List<String> fields = new ArrayList<String>();
		fields.add(REQUESTID);
		fields.add(TYPE);
		fields.add(STREAMID);
		fields.add(SEQUENCENR);
		fields.add(METADATA);
		fields.addAll(getTypeFields());
		return new Fields(fields);
	}
	
	
	@SuppressWarnings("unchecked")
	@Override
	public Type read(Kryo kryo, Input input, Class<Type> clas) {
		long requestId = input.readLong();
		String streamId = input.readString();
		long sequenceNr = input.readLong();
		HashMap<String, Object> metadata = kryo.readObject(input, HashMap.class);
		try {
			Type result = readObject(kryo, input, clas, requestId, streamId, sequenceNr);
			result.setMetadata(metadata);
			return result;
		} catch (Exception e) {
			//TODO
		}
		return null;
	}

	@Override
	public void write(Kryo kryo, Output output, Type type) {
		output.writeLong(type.getRequestId());
		output.writeString(type.getStreamId());
		output.writeLong(type.getSequenceNr());
		kryo.writeObject(output, type.getMetadata());
		try {
			this.writeObject(kryo, output, type);
		} catch (Exception e) {
			//TODO
		}
		output.flush();
		//output.close();
	}
	
	/**
	 * Writes Type specific values to the output
	 * @param kryo
	 * @param output
	 * @param type
	 */
	abstract protected void writeObject(Kryo kryo, Output output, Type type) throws Exception;


	/**
	 * Reads type specific values from the input (general items have already been read)
	 * @param kryo
	 * @param input
	 * @param clas
	 * @param requestId the requestId to be set in the object
	 * @param streamId streamId the streamId to be set in the object
	 * @param sequenceNr the sequenceNr to be set in the object
	 * @return
	 */
	abstract protected Type readObject(Kryo kryo, Input input, Class<Type> clas, long requestId, String streamId, long sequenceNr) throws Exception;
	
	/**
	 * Generates a Type Object from the provided tuple.
	 * @param tuple the tuple containing information used to create the object
	 * @return
	 * @throws IOException
	 */
	abstract protected Type createObject(Tuple tuple) throws IOException;
	
	/**
	 * Generates the values for the Object <b>(excluding the type, streamId and seunceNr!)</b>
	 * @param object
	 * @return
	 * @throws IOException
	 */
	abstract protected Values getValues(CVParticle object) throws IOException;
	
	/**
	 * Gets the fields specific for the type to be serialized <b>(excluding the type, streamId and seunceNr!)</b>
	 * @return
	 */
	abstract protected List<String> getTypeFields();
	
}
