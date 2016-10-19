package org.apache.streamline.streams.metrics.storm.topology;

public class TopologyNotAliveException extends RuntimeException {
  public TopologyNotAliveException(String msg) {
    super(msg);
  }
}
