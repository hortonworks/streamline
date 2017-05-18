package com.hortonworks.streamline.streams.storm.common;

import java.util.Map;

public interface ServiceConfigurationReadable {
    Map<Long, Map<String, String>> readAllClusters(String serviceName);
    Map<String, String> read(Long clusterId, String serviceName);
    Map<String, String> read(String clusterName, String serviceName);
}
