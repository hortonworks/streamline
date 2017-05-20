package com.hortonworks.streamline.streams.cluster.service.metadata.json;

import com.hortonworks.streamline.streams.catalog.ServiceConfiguration;

import java.io.IOException;
import java.util.Map;
import java.util.stream.Collectors;

public class Keytabs {
    //Key is the name of the service component of the principal. Val is the keytab for the principal
    private Map<String, String> keytabs;

    public Keytabs(Map<String, String> keytabs) {
        this.keytabs = keytabs;
    }

    /**
     * Instance built from map with Ambari configurations
     */
    public static Keytabs fromAmbariConfig(ServiceConfiguration serviceConfig) throws IOException {
        return fromAmbariConfig(serviceConfig.getConfigurationMap());
    }

    /**
     * Instance built from map with Ambari configurations
     */
    public static Keytabs fromAmbariConfig(Map<String, String> config) throws IOException {
        Map<String, String> kts = config.entrySet()
                .stream()
                .filter((e) -> e.getKey().contains("keytab"))
                .collect(Collectors.toMap(
                        (k) -> {
                            String key = k.getKey().split("keytab")[0];
                            return key.substring(0, key.length() - 1);
                        },
                        Map.Entry::getValue));

        return new Keytabs(kts);
    }

    /**
     * Instance built from map with service (e.g Hive, HBase) properties
     */
    public static Keytabs fromServiceProperties(ServiceConfiguration serviceConfig) throws IOException {
        return fromServiceProperties(serviceConfig.getConfigurationMap());
    }

    /**
     * Instance built from map with service (e.g Hive, HBase) properties
     */
    public static Keytabs fromServiceProperties(Map<String, String> props) throws IOException {
        Map<String, String> kts = props.entrySet()
                .stream()
                .filter((e) -> e.getKey().contains("keytab"))
                .collect(Collectors.toMap(
                        Map.Entry::getKey,
                        Map.Entry::getValue));

        return new Keytabs(kts);
    }

    public Map<String, String> toMap() {
        return keytabs;
    }

    @Override
    public String toString() {
        return "{" +
                "Keytabs=" + keytabs +
                '}';
    }
}
