package com.hortonworks.streamline.streams.cluster.service.metadata;

import mockit.Expectations;
import mockit.Mocked;
import mockit.integration.junit4.JMockit;
import org.apache.hadoop.conf.Configuration;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;

@RunWith(JMockit.class)
public class HDFSMetadataServiceTest {
    public static final String TEST_FS_URL = "hdfs://localhost:8020";

    @Mocked
    private Configuration configuration;

    @Test
    public void getDefaultFsUrl() throws Exception {
        HDFSMetadataService hdfsMetadataService = new HDFSMetadataService(configuration);

        new Expectations() {{
            configuration.get(HDFSMetadataService.CONFIG_KEY_DEFAULT_FS);
            result = TEST_FS_URL;
        }};

        Assert.assertEquals(TEST_FS_URL, hdfsMetadataService.getDefaultFsUrl());
    }
}