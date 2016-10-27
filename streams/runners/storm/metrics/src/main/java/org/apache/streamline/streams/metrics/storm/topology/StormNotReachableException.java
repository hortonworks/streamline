package org.apache.streamline.streams.metrics.storm.topology;

public class StormNotReachableException extends RuntimeException {
  public StormNotReachableException(String msg, Throwable e) {
    super(msg, e);
  }

  public StormNotReachableException(String msg) {
    super(msg);
  }
}
