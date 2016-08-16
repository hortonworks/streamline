package com.hortonworks.iotas.parsers.avro;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.Map;
import org.apache.avro.Schema;
import org.codehaus.jackson.map.ObjectMapper;
import org.codehaus.jackson.type.TypeReference;

import com.hortonworks.iotas.exception.ParserException;

public class AvroParser {

    private Schema schema = null;

    public void setSchema(Schema schema) {
        this.schema = schema;
    }
    
    /**
     * 
     * @param avroSchemaFile Avro Schema as File.
     * @throws IOException
     */
    public void setSchema(File avroSchemaFile) throws IOException  {
    	this.schema = new Schema.Parser().parse(avroSchemaFile);
    }

    /**
     * 
     * @param avroSchemaString Avro Schema as String.
     * @throws IOException
     */
    public void setSchema(String avroSchemaString)  {
    	this.schema = new Schema.Parser().parse(avroSchemaString);
    }
    
    /**
     * 
     * @param avroSchemaStream Avro Schema as InputStream.
     * @throws IOException
     */
    public void setSchema(InputStream avroSchemaStream) throws IOException  {
    	this.schema = new Schema.Parser().parse(avroSchemaStream);
    }
    
    public Schema schema() {
        return schema;
    }

    public Map<String, Object> parse(byte[] avrodata) throws ParserException {
        try {
        	ObjectMapper mapper = new ObjectMapper();
        	String data =  AvroUtils.avroToJson(avrodata, schema);
        	
            return mapper.readValue(data, new TypeReference<Map<String, Object>>(){});
        } catch (IOException e) {
            throw new ParserException("Error trying to parse data.", e);
        }
    }
    
}

