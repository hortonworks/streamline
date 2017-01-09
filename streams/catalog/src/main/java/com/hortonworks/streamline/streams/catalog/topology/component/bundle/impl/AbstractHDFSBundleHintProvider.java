package com.hortonworks.streamline.streams.catalog.topology.component.bundle.impl;

import com.hortonworks.streamline.streams.catalog.Cluster;
import com.hortonworks.streamline.streams.catalog.exception.ServiceConfigurationNotFoundException;
import com.hortonworks.streamline.streams.catalog.exception.ServiceNotFoundException;
import com.hortonworks.streamline.streams.catalog.service.metadata.HDFSMetadataService;
import com.hortonworks.streamline.streams.catalog.topology.component.bundle.AbstractBundleHintProvider;

import java.util.HashMap;
import java.util.Map;

public abstract class AbstractHDFSBundleHintProvider extends AbstractBundleHintProvider {
    public static final String SERVICE_NAME = "HDFS";

    @Override
    public Map<String, Object> getHintsOnCluster(Cluster cluster) {
        Map<String, Object> hintMap = new HashMap<>();

        try {
            HDFSMetadataService hdfsMetadataService = HDFSMetadataService.newInstance(environmentService, cluster.getId());
            hintMap.put(getFieldNameForFSUrl(), hdfsMetadataService.getDefaultFsUrl());
        } catch (ServiceNotFoundException e) {
            // we access it from mapping information so shouldn't be here
            throw new IllegalStateException("Service " + SERVICE_NAME + " in cluster " + cluster.getName() +
                    " not found but mapping information exists.");
        } catch (ServiceConfigurationNotFoundException e) {
            // there's HBASE service but not enough configuration info.
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
        return hintMap;
    }

    @Override
    public String getServiceName() {
        return SERVICE_NAME;
    }

    protected abstract String getFieldNameForFSUrl();
}
