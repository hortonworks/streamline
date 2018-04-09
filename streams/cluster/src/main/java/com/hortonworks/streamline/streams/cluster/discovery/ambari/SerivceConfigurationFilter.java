package com.hortonworks.streamline.streams.cluster.discovery.ambari;

import java.util.Map;

/**
 * Applies required transformation and filters configs.
 */
public interface SerivceConfigurationFilter {
    Map<String, String> filter(Map<String, String> input);
}
