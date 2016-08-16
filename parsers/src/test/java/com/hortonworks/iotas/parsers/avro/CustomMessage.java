package com.hortonworks.iotas.parsers.avro;

import java.io.Serializable;

public class CustomMessage implements Serializable {

	/**
	 * 
	 */
	private static final long serialVersionUID = 2664019774505507888L;
	private String id = null;
	private String payload = null;
	public String getId() {
		return id;
	}
	public void setId(String id) {
		this.id = id;
	}
	public String getPayload() {
		return payload;
	}
	public void setPayload(String payload) {
		this.payload = payload;
	}
	
	
}
