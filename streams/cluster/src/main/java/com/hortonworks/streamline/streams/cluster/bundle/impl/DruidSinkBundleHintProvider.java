package com.hortonworks.streamline.streams.cluster.bundle.impl;

import com.hortonworks.streamline.streams.catalog.Cluster;
import com.hortonworks.streamline.streams.catalog.Service;
import com.hortonworks.streamline.streams.catalog.ServiceConfiguration;
import com.hortonworks.streamline.streams.catalog.exception.ServiceConfigurationNotFoundException;
import com.hortonworks.streamline.streams.catalog.exception.ServiceNotFoundException;
import com.hortonworks.streamline.streams.cluster.Constants;
import com.hortonworks.streamline.streams.cluster.bundle.AbstractBundleHintProvider;

import java.util.HashMap;
import java.util.Map;

public class DruidSinkBundleHintProvider extends AbstractBundleHintProvider {
    public static final String FIELD_NAME_ZK_CONNECT = "tranquilityZKconnect";
    public static final String FIELD_NAME_INDEX_SERVICE = "indexService";
    public static final String FIELD_NAME_DISCOVERY_PATH = "discoveryPath";

    @Override
    public String getServiceName() {
        return Constants.Druid.SERVICE_NAME;
    }

    @Override
    public Map<String, Object> getHintsOnCluster(Cluster cluster) {
        Map<String, Object> hintMap = new HashMap<>();

        try {
            Service druid = environmentService.getServiceByName(cluster.getId(), Constants.Druid.SERVICE_NAME);
            if (druid == null) {
                throw new ServiceNotFoundException(Constants.Druid.SERVICE_NAME);
            }

            ServiceConfiguration commonRuntime = environmentService.getServiceConfigurationByName(druid.getId(), Constants.Druid.CONF_TYPE_COMMON_RUNTIME);
            if (commonRuntime == null) {
                throw new ServiceConfigurationNotFoundException(Constants.Druid.CONF_TYPE_COMMON_RUNTIME);
            }

            Map<String, String> configurationMap = commonRuntime.getConfigurationMap();
            putToHintMapIfAvailable(configurationMap, hintMap, Constants.Druid.PROPERTY_KEY_ZK_SERVICE_HOSTS, FIELD_NAME_ZK_CONNECT);
            putToHintMapIfAvailable(configurationMap, hintMap, Constants.Druid.PROPERTY_KEY_INDEXING_SERVICE_NAME, FIELD_NAME_INDEX_SERVICE);
            putToHintMapIfAvailable(configurationMap, hintMap, Constants.Druid.PROPERTY_KEY_DISCOVERY_CURATOR_PATH, FIELD_NAME_DISCOVERY_PATH);

            // exceptional case for Ambari import
            if (!hintMap.containsKey(FIELD_NAME_INDEX_SERVICE)) {
                ServiceConfiguration druidOverload = environmentService.getServiceConfigurationByName(druid.getId(), Constants.Druid.CONF_TYPE_DRUID_OVERLOAD);
                if (druidOverload != null) {
                    putToHintMapIfAvailable(druidOverload.getConfigurationMap(), hintMap, Constants.Druid.PROPERTY_KEY_SERVICE_NAME, FIELD_NAME_INDEX_SERVICE);
                }
            }
        } catch (ServiceNotFoundException e) {
            // we access it from mapping information so shouldn't be here
            throw new IllegalStateException("Service " + Constants.Druid.SERVICE_NAME + " in cluster " + cluster.getName() +
                    " not found but mapping information exists.");
        } catch (ServiceConfigurationNotFoundException e) {
            // there's Druid service configuration but not having enough information
        } catch (Exception e) {
            throw new RuntimeException(e);
        }

        return hintMap;
    }

    private void putToHintMapIfAvailable(Map<String, String> configurationMap, Map<String, Object> hintMap,
                                         String confKey, String fieldName) {
        if (configurationMap.containsKey(confKey)) {
            hintMap.put(fieldName, configurationMap.get(confKey));
        }
    }
}
