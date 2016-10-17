package com.hortonworks.iotas.streams.metrics.storm.topology;

public class TopologyNotAliveException extends RuntimeException {
  public TopologyNotAliveException(String msg) {
    super(msg);
  }
}
