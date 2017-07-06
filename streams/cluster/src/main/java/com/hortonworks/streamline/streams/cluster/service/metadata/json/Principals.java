package com.hortonworks.streamline.streams.cluster.service.metadata.json;

import com.hortonworks.streamline.streams.catalog.Component;
import com.hortonworks.streamline.streams.catalog.ComponentProcess;
import com.hortonworks.streamline.streams.catalog.ServiceConfiguration;

import org.apache.commons.math3.util.Pair;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.security.Principal;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static java.util.stream.Collectors.toList;

public class Principals {
    private static final Logger LOG = LoggerFactory.getLogger(Principals.class);

    /*
     * The map Key is the principal's service component . Val is list of principals for this service.
     * Typically it is one principal per host running this service.
     */
    private Map<String, List<Principal>> principals;

    public Principals(Map<String, List<Principal>> principals) {
        this.principals = principals;
    }

    /**
     * Instance built from services configurations in Ambari
     */
    public static Principals fromAmbariConfig(ServiceConfiguration serviceConfig,
            Map<String, Pair<Component, Collection<ComponentProcess>>> serviceToComponent) throws IOException {
        return fromAmbariConfig(serviceConfig.getConfigurationMap(), serviceToComponent);
    }

    /**
     * Instance built from map with Ambari configurations
     */
    public static Principals fromAmbariConfig(Map<String, String> config,
            Map<String, Pair<Component, Collection<ComponentProcess>>> serviceToComponent) throws IOException {

        final Map<String, List<Principal>> allPrincs = new HashMap<>();
        for (Map.Entry<String, Pair<Component, Collection<ComponentProcess>>> stc : serviceToComponent.entrySet()) {     // stc - serviceToComponent
            final String serviceName = stc.getKey();
            final Pair<Component, Collection<ComponentProcess>> componentAndProcesses = stc.getValue();
            final Collection<String> hosts = componentAndProcesses.getSecond().stream()
                    .map(cp -> cp.getHost()).collect(toList());
            final Map<String, List<Principal>> princs = config.entrySet()
                    .stream()
                    .filter((e) -> e.getKey().contains("principal") && e.getKey().replace("_principal_name","").equals(serviceName))
                    .peek((e) -> LOG.debug("Processing Ambari property [{}]=[{}] for service [{}]", e.getKey(), e.getValue(), serviceName))
                    .collect(Collectors.toMap(
                            (e) -> {
                                // Extracts the principal service component from Ambari property key
                                String key = e.getKey().split("principal")[0];
                                return key.substring(0, key.length() - 1);          // remove _ at the end
                            },
                            (e) ->  hosts.stream()   // get hosts for service component (e.g nimbus, storm_ui, kafka broker)
                                    .map((host) -> host == null || host.isEmpty()
                                            ? UserPrincipal.fromPrincipal(e.getValue())
                                            : ServicePrincipal.forHost(e.getValue(), host))
                                    .collect(toList())));

            LOG.debug("Processed {}", princs);
            allPrincs.putAll(princs);
        }

        return new Principals(allPrincs);
    }

    /**
     * Instance built from service configuration properties (e.g hive-metastore.xml, hbase-site.xml)
     */
    public static Principals fromServiceProperties(ServiceConfiguration serviceConfig, Pair<Component, Collection<ComponentProcess>> component) throws IOException {
        return fromServiceProperties(serviceConfig.getConfigurationMap(), component);
    }

    /**
     * Instance built from service configuration properties (e.g hive-metastore.xml, hbase-site.xml)
     */
    public static Principals fromServiceProperties(Map<String, String> props, Pair<Component, Collection<ComponentProcess>> component) throws IOException {
        final List<String> hosts = component.getSecond().stream().map(ComponentProcess::getHost).collect(toList());
        final Map<String, List<Principal>> princs = props.entrySet()
                .stream()
                .filter((e) -> e.getKey().contains("principal"))
                .collect(Collectors.toMap(
                        Map.Entry::getKey,
                        (e) ->  hosts   // get hosts for service component (e.g HBase master or Hive metastore)
                                .stream()
                                .map((host) -> ServicePrincipal.forHost(e.getValue(), host))
                                .collect(toList())));

        return new Principals(princs);
    }

    public Map<String, List<Principal>> toMap() {
        return principals;
    }

    @Override
    public String toString() {
        return "{" +
                "Principals=" + principals +
                '}';
    }
}
