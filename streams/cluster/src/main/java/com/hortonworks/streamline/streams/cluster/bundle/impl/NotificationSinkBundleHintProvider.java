package com.hortonworks.streamline.streams.cluster.bundle.impl;

import com.hortonworks.streamline.streams.catalog.Cluster;
import com.hortonworks.streamline.streams.catalog.Service;
import com.hortonworks.streamline.streams.catalog.ServiceConfiguration;
import com.hortonworks.streamline.streams.catalog.exception.ServiceConfigurationNotFoundException;
import com.hortonworks.streamline.streams.catalog.exception.ServiceNotFoundException;
import com.hortonworks.streamline.streams.cluster.Constants;
import com.hortonworks.streamline.streams.cluster.bundle.AbstractBundleHintProvider;

import javax.security.auth.Subject;
import javax.ws.rs.core.SecurityContext;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

public class NotificationSinkBundleHintProvider extends AbstractBundleHintProvider {

    public static final String FIELD_NAME_HOST = "properties.host";
    public static final String FIELD_NAME_PORT = "properties.port";
    public static final String FIELD_NAME_SSL = "properties.ssl";
    public static final String FIELD_NAME_STARTTLS = "properties.starttls";
    public static final String FIELD_NAME_PROTOCOL = "properties.protocol";
    public static final String FIELD_NAME_AUTH = "properties.auth";

    @Override
    public String getServiceName() {
        return Constants.Email.SERVICE_NAME;
    }

    @Override
    public Map<String, Object> getHintsOnCluster(Cluster cluster, SecurityContext securityContext, Subject subject) {
        Map<String, Object> hintMap = new HashMap<>();

        try {
            Service email = environmentService.getServiceByName(cluster.getId(), Constants.Email.SERVICE_NAME);
            if (email == null) {
                throw new ServiceNotFoundException(Constants.Email.SERVICE_NAME);
            }

            ServiceConfiguration properties = environmentService.getServiceConfigurationByName(email.getId(), Constants.Email.CONF_TYPE_PROPERTIES);
            if (properties == null) {
                throw new ServiceConfigurationNotFoundException(Constants.Email.CONF_TYPE_PROPERTIES);
            }

            Map<String, String> configurationMap = properties.getConfigurationMap();
            putToHintMapIfAvailable(configurationMap, hintMap, Constants.Email.PROPERTY_KEY_HOST, FIELD_NAME_HOST);
            putToHintMapIfAvailable(configurationMap, hintMap, Constants.Email.PROPERTY_KEY_PORT, FIELD_NAME_PORT);
            putToHintMapIfAvailable(configurationMap, hintMap, Constants.Email.PROPERTY_KEY_SSL, FIELD_NAME_SSL);
            putToHintMapIfAvailable(configurationMap, hintMap, Constants.Email.PROPERTY_KEY_STARTTLS, FIELD_NAME_STARTTLS);
            putToHintMapIfAvailable(configurationMap, hintMap, Constants.Email.PROPERTY_KEY_PROTOCOL, FIELD_NAME_PROTOCOL);
            putToHintMapIfAvailable(configurationMap, hintMap, Constants.Email.PROPERTY_KEY_AUTH, FIELD_NAME_AUTH);
        } catch (ServiceNotFoundException e) {
            // we access it from mapping information so shouldn't be here
            throw new IllegalStateException("Service " + Constants.Email.SERVICE_NAME + " in cluster " + cluster.getName() +
                    " not found but mapping information exists.");
        } catch (ServiceConfigurationNotFoundException e) {
            // there's Email service configuration but not having enough information
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
