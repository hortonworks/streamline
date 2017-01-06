package com.hortonworks.streamline.streams.catalog.service.metadata;

import com.google.common.collect.ImmutableList;
import org.apache.hadoop.conf.Configuration;
import com.hortonworks.streamline.streams.catalog.exception.ServiceConfigurationNotFoundException;
import com.hortonworks.streamline.streams.catalog.exception.ServiceNotFoundException;
import com.hortonworks.streamline.streams.catalog.service.EnvironmentService;
import com.hortonworks.streamline.streams.catalog.service.metadata.common.OverrideHadoopConfiguration;
import com.hortonworks.streamline.streams.cluster.discovery.ambari.ServiceConfigurations;

import java.io.IOException;
import java.util.List;

public class HDFSMetadataService {
    private static final List<String> STREAMS_JSON_SCHEMA_CONFIG_HDFS =
            ImmutableList.copyOf(new String[] {ServiceConfigurations.HDFS.getConfNames()[0]});
    public static final String CONFIG_KEY_DEFAULT_FS = "fs.defaultFS";

    private final Configuration hdfsConfiguration;

    public HDFSMetadataService(Configuration hdfsConfiguration) {
        this.hdfsConfiguration = hdfsConfiguration;
    }

    public static HDFSMetadataService newInstance(EnvironmentService environmentService, Long clusterId)
            throws ServiceConfigurationNotFoundException, IOException, ServiceNotFoundException {
        Configuration hdfsConfiguration = OverrideHadoopConfiguration.override(environmentService, clusterId,
                ServiceConfigurations.HDFS, STREAMS_JSON_SCHEMA_CONFIG_HDFS, new Configuration());

        return new HDFSMetadataService(hdfsConfiguration);
    }

    /**
     * @return default FS url for this HDFS service
     */
    public String getDefaultFsUrl() {
        return hdfsConfiguration.get(CONFIG_KEY_DEFAULT_FS);
    }
}
