Configurations for the cluster with:
    - Ambari URL: "http://172.18.128.67:8080/api/v1/clusters/rmp_performance"
    - Credentials: admin:admin

hbase-site.json, hivemetastore-site.json
    contains the JSON representation of the map containing the hbase-site/hivemetastore-site
    configuration as returned by the method
    com.hortonworks.iotas.streams.catalog.ServiceConfiguration#getConfigurationMap