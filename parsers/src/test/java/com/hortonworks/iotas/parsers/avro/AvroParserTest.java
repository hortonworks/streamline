package com.hortonworks.iotas.parsers.avro;

import java.io.IOException;
import java.util.Map;
import java.util.Set;

import org.apache.avro.Schema;
import org.apache.avro.generic.GenericData;
import org.apache.avro.generic.GenericRecord;

import org.junit.Before;
import org.junit.Test;

import com.hortonworks.iotas.exception.ParserException;

import org.junit.Assert;


public class AvroParserTest {
	Schema schema = null;
	@Before public void init() throws IOException{
		 schema = new Schema.Parser().parse(AvroParserTest.class.getResourceAsStream("/com/hortonworks/iotas/parsers/avro/CustomMessage.avsc"));
			
	}
	
	/*//for testing avro generated objects
	@Test public void testJavaAvroUtilConversion() throws IOException{
		
		//Schema schema = new Schema.Parser().parse(AvroParserTest.class.getResourceAsStream("/com/hortonworks/iotas/parsers/avro/CustomMessage.avsc"));
		
		CustomMessage message = new CustomMessage();
		message.setId("device1");
		message.setPayload("{'type':'internal json'}");
		
		AvroUtils<CustomMessage> utils = new AvroUtils<CustomMessage>();
		//create avro
		byte[] avro = utils.serializeJava(message, schema);

		//recreate object
		//GenericDatumReader<CustomMessage> reader = new SpecificDatumReader<CustomMessage>(schema);	        
		//Decoder decoder = DecoderFactory.get().binaryDecoder(avro, null);
		//CustomMessage msg2 = reader.read(null, decoder);
		CustomMessage msg2 = utils.avroToAvroJava(avro, schema);
		Assert.assertEquals(msg2, message);
		
	}*/
	
	@Test public void testAvroUtils() throws IOException{
		
		GenericRecord mesg = new GenericData.Record(schema);		
		mesg.put("id", "device1");
		mesg.put("payload", "{'type':'internal json'}");
		
		//create avro
		byte[] avro = AvroUtils.serializeJson(mesg.toString(), schema);
		
		//avro to java
		CustomMessage msg1  = new AvroUtils<CustomMessage>().avroToJava(avro, schema, CustomMessage.class);
		Assert.assertEquals(msg1.getPayload(),mesg.get("payload"));
	}
	
	@Test public void testAvroJsonConversion() throws IOException{
		
		//create json
		GenericRecord mesg = new GenericData.Record(schema);		
		mesg.put("id", "device1");
		mesg.put("payload", "{'type':'internal json'}");
		

		System.out.println(mesg.toString());
		byte[] avro = AvroUtils.serializeJson(mesg.toString(), schema);
		
		///send byte array in mqtt
		
		///receive data
		String json = AvroUtils.avroToJson(avro, schema);
		
		System.out.println(json);
		
		Assert.assertTrue(json.contains(mesg.get("payload").toString()));
		}
	
	
	@Test public void testAvroParser() throws IOException, ParserException{
		
		//create json
		GenericRecord mesg = new GenericData.Record(schema);		
		mesg.put("id", "device1");
		mesg.put("payload", "{'type':'internal json'}");
		
		System.out.println(mesg.toString());
		byte[] avro = AvroUtils.serializeJson(mesg.toString(), schema);
		
		///send byte array in mqtt
		
		///receive data
		
		AvroParser parser = new AvroParser();
		parser.setSchema(schema);
		Map<String, Object> map = parser.parse(avro);
		
		Set<String> keys = map.keySet() ;
		for(String key : keys){
			System.out.println(key+" = "+map.get(key));;
		}
		

		Assert.assertEquals(mesg.get("id"), map.get("id"));
		Assert.assertEquals(mesg.get("payload"), map.get("payload"));
		
		}

}
