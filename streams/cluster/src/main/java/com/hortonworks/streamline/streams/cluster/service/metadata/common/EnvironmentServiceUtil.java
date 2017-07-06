package com.hortonworks.streamline.streams.cluster.service.metadata.common;

import com.hortonworks.streamline.streams.catalog.Component;
import com.hortonworks.streamline.streams.catalog.ComponentProcess;
import com.hortonworks.streamline.streams.catalog.exception.ServiceComponentNotFoundException;
import com.hortonworks.streamline.streams.catalog.exception.ServiceNotFoundException;
import com.hortonworks.streamline.streams.cluster.service.EnvironmentService;

import java.util.Collection;
import java.util.Collections;
import java.util.List;

/**
 * Static class with a couple convenience methods to simplify the client code
 */
public class EnvironmentServiceUtil {
    public static Component getComponent(EnvironmentService es, Long clusterId, String serviceName, String componentName)
            throws ServiceNotFoundException, ServiceComponentNotFoundException {

        final Component component = es.getComponentByName(getServiceIdByName(es, clusterId, serviceName), componentName);
        if (component == null) {
            throw new ServiceComponentNotFoundException(clusterId, serviceName, componentName);
        }
        return component;
    }

    public static Collection<ComponentProcess> getComponentProcesses(EnvironmentService es, Long clusterId, String serviceName,
                                                                     String componentName)
            throws ServiceNotFoundException, ServiceComponentNotFoundException {
        final Component component = es.getComponentByName(getServiceIdByName(es, clusterId, serviceName), componentName);
        if (component == null) {
            throw new ServiceComponentNotFoundException(clusterId, serviceName, componentName);
        }

        final Collection<ComponentProcess> componentProcesses = es.listComponentProcessesInComponent(component.getId());
        return componentProcesses;
    }

    public static Long getServiceIdByName(EnvironmentService es, Long clusterId, String serviceName)
            throws ServiceNotFoundException {

        final Long serviceId = es.getServiceIdByName(clusterId, serviceName);
        if (serviceId == null) {
            throw new ServiceNotFoundException(clusterId, serviceName);
        }
        return serviceId;
    }
}
