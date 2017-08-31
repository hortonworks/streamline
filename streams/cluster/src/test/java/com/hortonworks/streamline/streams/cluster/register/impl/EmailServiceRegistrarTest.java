package com.hortonworks.streamline.streams.cluster.register.impl;

import com.hortonworks.streamline.common.Config;
import com.hortonworks.streamline.streams.cluster.catalog.Cluster;
import com.hortonworks.streamline.streams.cluster.catalog.Service;
import com.hortonworks.streamline.streams.cluster.catalog.ServiceConfiguration;
import com.hortonworks.streamline.streams.cluster.Constants;
import org.junit.Before;
import org.junit.Test;

import java.util.Collections;

import static org.junit.Assert.*;

public class EmailServiceRegistrarTest extends AbstractServiceRegistrarTest<EmailServiceRegistrar> {

    public EmailServiceRegistrarTest() {
        super(EmailServiceRegistrar.class);
    }

    @Before
    public void setUp() {
        resetEnvironmentService();
    }

    @Test
    public void testRegister() throws Exception {
        Cluster cluster = getTestCluster(1L);

        EmailServiceRegistrar registrar = initializeServiceRegistrar();

        Config config = new Config();
        config.setAny("host", "host");
        config.setAny("port", 1111);
        config.setAny("ssl", true);
        config.setAny("starttls", true);
        config.setAny("protocol", "smtp");
        config.setAny("auth", true);

        registrar.register(cluster, config, Collections.emptyList());

        Service emailService = environmentService.getServiceByName(cluster.getId(), Constants.Email.SERVICE_NAME);
        assertNotNull(emailService);
        ServiceConfiguration propertiesConf = environmentService.getServiceConfigurationByName(emailService.getId(),
                Constants.Email.CONF_TYPE_PROPERTIES);
        assertNotNull(propertiesConf);
    }

}