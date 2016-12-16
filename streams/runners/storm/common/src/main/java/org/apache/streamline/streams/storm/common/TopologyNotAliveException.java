package org.apache.streamline.streams.storm.common;

public class TopologyNotAliveException extends RuntimeException {
  public TopologyNotAliveException(String msg) {
    super(msg);
  }
}
