package com.hortonworks.streamline.streams.cluster.service.metadata.common;

import com.hortonworks.streamline.streams.catalog.ServiceConfiguration;
import com.hortonworks.streamline.streams.catalog.exception.ServiceConfigurationNotFoundException;
import com.hortonworks.streamline.streams.catalog.exception.ServiceNotFoundException;

import org.apache.hadoop.conf.Configuration;
import com.hortonworks.streamline.streams.cluster.service.EnvironmentService;
import com.hortonworks.streamline.streams.cluster.discovery.ambari.ServiceConfigurations;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.List;
import java.util.Map;

public class OverrideHadoopConfiguration {
    private static final Logger LOG = LoggerFactory.getLogger(OverrideHadoopConfiguration.class);

    public static <T extends Configuration> T override(EnvironmentService environmentService, Long clusterId,
                                                       ServiceConfigurations service, List<String> configNames, T configuration)
                throws IOException, ServiceConfigurationNotFoundException, ServiceNotFoundException {

        for (String configName : configNames) {
            final ServiceConfiguration serviceConfig = environmentService.getServiceConfigurationByName(
                    getServiceIdByClusterId(environmentService, clusterId, service), configName);

            if (serviceConfig != null) {
                final Map<String, String> configurationMap = serviceConfig.getConfigurationMap();
                if (configurationMap != null) {
                    for (Map.Entry<String, String> propKeyVal : configurationMap.entrySet()) {
                        final String key = propKeyVal.getKey();
                        final String val = propKeyVal.getValue();

                        if (key != null && val != null) {
                            configuration.set(key, val);
                            LOG.debug("Set property {}", propKeyVal);
                        } else {
                            LOG.warn("Skipping null key/val property {}", propKeyVal);
                        }
                    }
                }
            } else {
                throw new ServiceConfigurationNotFoundException("Required [" + configName +
                        "] configuration not found for service [" + service.name() + "]");
            }
        }
        return configuration;
    }

    private static Long getServiceIdByClusterId(EnvironmentService environmentService, Long clusterId,
            ServiceConfigurations service) throws ServiceNotFoundException {

        final Long serviceId = environmentService.getServiceIdByName(clusterId, service.name());
        if (serviceId == null) {
            throw new ServiceNotFoundException(clusterId, service.name());
        }
        return serviceId;
    }
}
