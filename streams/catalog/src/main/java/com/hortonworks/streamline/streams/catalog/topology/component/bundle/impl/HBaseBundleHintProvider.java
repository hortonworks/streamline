package org.apache.streamline.streams.catalog.topology.component.bundle.impl;

import org.apache.streamline.streams.catalog.Cluster;
import org.apache.streamline.streams.catalog.exception.ServiceConfigurationNotFoundException;
import org.apache.streamline.streams.catalog.exception.ServiceNotFoundException;
import org.apache.streamline.streams.catalog.service.metadata.HBaseMetadataService;
import org.apache.streamline.streams.catalog.topology.component.bundle.AbstractBundleHintProvider;

import java.util.HashMap;
import java.util.Map;

public class HBaseBundleHintProvider extends AbstractBundleHintProvider {
    public static final String SERVICE_NAME = "HBASE";
    public static final String FIELD_NAME_TABLE = "table";

    @Override
    public Map<String, Object> getHintsOnCluster(Cluster cluster) {
        Map<String, Object> hintMap = new HashMap<>();
        try (HBaseMetadataService hBaseMetadataService = HBaseMetadataService.newInstance(environmentService, cluster.getId())) {
            hintMap.put(FIELD_NAME_TABLE, hBaseMetadataService.getHBaseTables().getTables());
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
}
