# Cluster REST API

## Concepts

**Cluster**

A *Cluster* is a top level entity that can be used to create logical container which contains 
external services.

Field | Type | Comment
---|---|----
id | Long | The primary key
name | String | The name of cluster. It should be unique.
description | String | The description of cluster if any.

**Service**

A *Service* defines an external components like storm, kafka, and so on that project interacts with.
A service entity is belong to a cluster entity.

Field | Type | Comment
---|---|----
id | Long | The primary key
clusterId | Long | The cluster's primary key which a service is belong to.
name | String | The name of service. Combination of cluster id and service name should be unique.
description | String | The description of service if any.

**ServiceConfiguration**

A *ServiceConfiguration* defines a part of configurations of service. It is designed to match one 
configuration file of service, like `core-site.xml`, and `storm.yaml`, and so on.
A service configuration is belong to a service entity.

Field | Type | Comment
---|---|----
id | Long | The primary key
serviceId | Long | The service's primary key which a service configuration is belong to.
name | String | The name of service configuration. Combination of service id and service configuration name should be unique.
configuration | String | The JSON representation of actual configuration (key, value) pairs. Deserialized Java type is `Map<String, Object>`.
description | String | The description of service if any.
filename | String | The filename of configuration. If it's presented, it can be used to be stored and provide actual `config file` to topology.

**Component**

A *component* refers to an an entity within a cluster providing a specific functionality. 
E.g Storm Nimbus, Kafka Broker etc. To keep it simple, rather than creating a separate entity for host, the hostnames (and port) is  
directly specified as fields within a component.

A component refers to an entity within a cluster providing a specific functionality. E.g Storm Nimbus, 
Kafka Broker etc. To keep it simple, rather than creating a separate entity for host, the hostnames (and port, and protocol) is 
directly specified as fields within a component.

Field| Type | Comment
---|---|----
Id | Long | The primary key
serviceId | Long | The service's primary key which a component is belong to.
name | String | The name of component. Combination of service id and component name should be unique.
hosts | List<String> | A list of host names or IP where this component runs.
protocol | String | The protocol for communicating with this port.
port  | Integer | The port number where this component listens.

## Rest API

We'll describe full CRUD of Cluster, but not for others since Response format for same CRUD is really similar.
For Service, Service Configuration, and Component, we'll just enumerate which APIs are provided, 
and show only sample for creating a service, service configuration, component.

### Create a cluster

`POST /api/v1/catalog/clusters`

**Sample Input**

```json
{
 "name": "production",
 "description": "The production cluster that handles streaming events"
}
```
   
**Success Response**

    HTTP/1.1 201 Created
    Content-Type: application/json
    
```json
{
  "responseCode": 1000,
  "responseMessage": "Success",
  "entity": {
    "id": 1,
    "name": "production",
    "description": "The production cluster that handles streaming events",
    "timestamp": 1475657006739
  }
}
```

### Get Cluster

`GET /api/v1/catalog/clusters/:clusterid`

**Success Response**

    GET /api/v1/catalog/clusters/1
    HTTP/1.1 200 OK
    Content-Type: application/json

```json
{
  "responseCode": 1000,
  "responseMessage": "Success",
  "entity": {
    "id": 1,
    "name": "production",
    "description": "The production cluster that handles streaming events",
    "timestamp": 1475657006739
  }
}
```
**Error Response**

    GET /api/v1/catalog/clusters/10
    HTTP/1.1 404 Not Found
    Content-Type: application/json
    
```json
{
  "responseCode": 1101,
  "responseMessage": "Entity with id [10] not found. Please check webservice/ErrorCodes.md for more details."
}
```

### List Clusters

`GET /api/v1/catalog/clusters`

    GET /api/v1/catalog/clusters
    HTTP/1.1 200 OK
    Content-Type: application/json

```json
{
  "responseCode": 1000,
  "responseMessage": "Success",
  "entities": [
    {
      "id": 3,
      "name": "production2",
      "description": "The production2 cluster that handles streaming events",
      "timestamp": 1475657136244
    },
    {
      "id": 1,
      "name": "production",
      "description": "The production cluster that handles streaming events",
      "timestamp": 1475657006739
    }
  ]
}
```

### Update Cluster

`PUT /api/v1/catalog/clusters/:clusterid`

*Sample Input*

```json
{
	"name": "production-new",
	"description": "The new-production cluster that handles streaming events"
}
```

*Success Response*

    HTTP/1.1 200 OK
    Content-Type: application/json

```json
{
  "responseCode": 1000,
  "responseMessage": "Success",
  "entity": {
    "id": 1,
    "name": "production-new",
    "description": "The new-production cluster that handles streaming events",
    "timestamp": 1475657294903
  }
}
```

### Delete Cluster

`DELETE /api/v1/catalog/clusters/:clusterid`

*Success Response*

    HTTP/1.1 200 OK
    Content-Type: application/json

```json
{
  "responseCode": 1000,
  "responseMessage": "Success",
  "entity": {
    "id": 1,
    "name": "production-new",
    "description": "The new-production cluster that handles streaming events",
    "timestamp": 1475657294903
  }
}
```


### Create Services

`POST /api/v1/clusters/:clusterId/services`

**Sample Input**

```json
{
  "clusterId": 3,
  "name": "HIVE",
  "description": ""
}
```
   
**Success Response**

    HTTP/1.1 201 Created
    Content-Type: application/json
    
```json
{
  "responseCode": 1000,
  "responseMessage": "Success",
  "entity": {
    "id": 13,
    "clusterId": 3,
    "name": "HIVE",
    "description": "",
    "timestamp": 1475660875465
  }
}
```

### List Services

`GET /api/v1/catalog/clusters/:clusterId/services`

### Get Service

`GET /api/v1/catalog/clusters/{clusterId}/services/{id}`

### Update Service

`PUT /api/v1/catalog/clusters/{clusterId}/services/{id}`

### Delete Service

`DELETE /api/v1/catalog/clusters/{clusterId}/services/{id}`


### Create Service Bundle

`POST /api/v1/catalog/servicebundles`

**Sample Input**

```json
{
  "name": "STORM",
  "registerClass": "com.hortonworks.streamline.streams.cluster.register.impl.StormServiceRegistrar",
  "serviceUISpecification": {
    "fields": [
      {
        "uiName": "UI server hostname",
        "fieldName": "uiServerHostname",
        "isOptional": false,
        "tooltip": "Hostname of Storm UI server",
        "type": "string"
      },
      {
        "uiName": "Nimbuses hostnames",
        "fieldName": "nimbusesHostnames",
        "isOptional": false,
        "tooltip": "Hostnames of Nimbuses",
        "type": "array.string"
      },
      {
        "uiName": "storm.yaml",
        "fieldName": "storm.yaml",
        "isOptional": false,
        "tooltip": "Upload storm.yaml file which Storm cluster is using",
        "type": "file"
      }
    ]
  }
}
```
   
**Success Response**

    HTTP/1.1 201 Created
    Content-Type: application/json
    
```json
{
  "id": 1,
  "name": "STORM",
  "timestamp": 1487228012093,
  "serviceUISpecification": {
    "fields": [
      {
        "uiName": "UI server hostname",
        "fieldName": "uiServerHostname",
        "isUserInput": true,
        "tooltip": "Hostname of Storm UI server",
        "isOptional": false,
        "type": "string"
      },
      {
        "uiName": "Nimbuses hostnames",
        "fieldName": "nimbusesHostnames",
        "isUserInput": true,
        "tooltip": "Hostnames of Nimbuses",
        "isOptional": false,
        "type": "array.string"
      },
      {
        "uiName": "storm.yaml",
        "fieldName": "storm.yaml",
        "isUserInput": true,
        "tooltip": "Upload storm.yaml file which Storm cluster is using",
        "isOptional": false,
        "type": "file"
      }
    ]
  },
  "registerClass": "com.hortonworks.streamline.streams.cluster.register.impl.StormServiceRegistrar"
}
```

### List Services Bundle

`GET /api/v1/catalog/servicebundles`

### Get Service Bundle

`GET /api/v1/catalog/servicebundles/{id}`

### Get Service Bundle (by service name)

`GET /api/v1/catalog/servicebundles/name/{serviceName}`

### Delete Service Bundle

`DELETE /api/v1/catalog/servicebundles/{id}`

### Create Service Configurations

`POST /api/v1/catalog/services/{serviceId}/configurations`

**Sample Input**

```json
{
  "serviceId": 13,
  "name": "hive-interactive-site",
  "configuration": "{\"hive.driver.parallel.compilation\":\"true\",\"hive.exec.orc.split.strategy\":\"HYBRID\",\"hive.execution.engine\":\"tez\",\"hive.execution.mode\":\"llap\",\"hive.llap.auto.allow.uber\":\"false\",\"hive.llap.client.consistent.splits\":\"true\",\"hive.llap.daemon.allow.permanent.fns\":\"false\",\"hive.llap.daemon.memory.per.instance.mb\":\"250\",\"hive.llap.daemon.num.executors\":\"1\",\"hive.llap.daemon.queue.name\":\"default\",\"hive.llap.daemon.rpc.port\":\"15001\",\"hive.llap.daemon.service.hosts\":\"@llap0\",\"hive.llap.daemon.task.scheduler.enable.preemption\":\"true\",\"hive.llap.daemon.vcpus.per.instance\":\"${hive.llap.daemon.num.executors}\",\"hive.llap.daemon.work.dirs\":\"${yarn.nodemanager.local-dirs}\",\"hive.llap.daemon.yarn.container.mb\":\"341\",\"hive.llap.daemon.yarn.shuffle.port\":\"15551\",\"hive.llap.execution.mode\":\"all\",\"hive.llap.io.enabled\":\"true\",\"hive.llap.io.memory.mode\":\"cache\",\"hive.llap.io.memory.size\":\"0\",\"hive.llap.io.threadpool.size\":\"2\",\"hive.llap.io.use.lrfu\":\"true\",\"hive.llap.management.rpc.port\":\"15004\",\"hive.llap.object.cache.enabled\":\"true\",\"hive.llap.task.scheduler.locality.delay\":\"-1\",\"hive.llap.zk.sm.connectionString\":\"sandbox.hortonworks.com:2181\",\"hive.mapjoin.hybridgrace.hashtable\":\"false\",\"hive.metastore.event.listeners\":\"\",\"hive.metastore.uris\":\"\",\"hive.optimize.dynamic.partition.hashjoin\":\"true\",\"hive.prewarm.enabled\":\"false\",\"hive.server2.enable.doAs\":\"false\",\"hive.server2.tez.default.queues\":\"default\",\"hive.server2.tez.initialize.default.sessions\":\"true\",\"hive.server2.tez.sessions.per.default.queue\":\"1\",\"hive.server2.thrift.http.port\":\"10501\",\"hive.server2.thrift.port\":\"10500\",\"hive.server2.webui.port\":\"10502\",\"hive.server2.webui.use.ssl\":\"false\",\"hive.server2.zookeeper.namespace\":\"hiveserver2-hive2\",\"hive.tez.bucket.pruning\":\"true\",\"hive.tez.exec.print.summary\":\"true\",\"hive.tez.input.generate.consistent.splits\":\"true\",\"hive.vectorized.execution.mapjoin.minmax.enabled\":\"true\",\"hive.vectorized.execution.mapjoin.native.enabled\":\"true\",\"hive.vectorized.execution.mapjoin.native.fast.hashtable.enabled\":\"true\",\"hive.vectorized.execution.reduce.enabled\":\"true\",\"llap.shuffle.connection-keep-alive.enable\":\"true\",\"llap.shuffle.connection-keep-alive.timeout\":\"60\"}",
  "description": "",
  "filename": "hive-interactive-site.xml"
}
```
   
**Success Response**

    HTTP/1.1 201 Created
    Content-Type: application/json
    
```json
{
  "responseCode": 1000,
  "responseMessage": "Success",
  "entity": {
    "id": 37,
    "serviceId": 13,
    "name": "hive-interactive-site",
    "configuration": "{\"hive.driver.parallel.compilation\":\"true\",\"hive.exec.orc.split.strategy\":\"HYBRID\",\"hive.execution.engine\":\"tez\",\"hive.execution.mode\":\"llap\",\"hive.llap.auto.allow.uber\":\"false\",\"hive.llap.client.consistent.splits\":\"true\",\"hive.llap.daemon.allow.permanent.fns\":\"false\",\"hive.llap.daemon.memory.per.instance.mb\":\"250\",\"hive.llap.daemon.num.executors\":\"1\",\"hive.llap.daemon.queue.name\":\"default\",\"hive.llap.daemon.rpc.port\":\"15001\",\"hive.llap.daemon.service.hosts\":\"@llap0\",\"hive.llap.daemon.task.scheduler.enable.preemption\":\"true\",\"hive.llap.daemon.vcpus.per.instance\":\"${hive.llap.daemon.num.executors}\",\"hive.llap.daemon.work.dirs\":\"${yarn.nodemanager.local-dirs}\",\"hive.llap.daemon.yarn.container.mb\":\"341\",\"hive.llap.daemon.yarn.shuffle.port\":\"15551\",\"hive.llap.execution.mode\":\"all\",\"hive.llap.io.enabled\":\"true\",\"hive.llap.io.memory.mode\":\"cache\",\"hive.llap.io.memory.size\":\"0\",\"hive.llap.io.threadpool.size\":\"2\",\"hive.llap.io.use.lrfu\":\"true\",\"hive.llap.management.rpc.port\":\"15004\",\"hive.llap.object.cache.enabled\":\"true\",\"hive.llap.task.scheduler.locality.delay\":\"-1\",\"hive.llap.zk.sm.connectionString\":\"sandbox.hortonworks.com:2181\",\"hive.mapjoin.hybridgrace.hashtable\":\"false\",\"hive.metastore.event.listeners\":\"\",\"hive.metastore.uris\":\"\",\"hive.optimize.dynamic.partition.hashjoin\":\"true\",\"hive.prewarm.enabled\":\"false\",\"hive.server2.enable.doAs\":\"false\",\"hive.server2.tez.default.queues\":\"default\",\"hive.server2.tez.initialize.default.sessions\":\"true\",\"hive.server2.tez.sessions.per.default.queue\":\"1\",\"hive.server2.thrift.http.port\":\"10501\",\"hive.server2.thrift.port\":\"10500\",\"hive.server2.webui.port\":\"10502\",\"hive.server2.webui.use.ssl\":\"false\",\"hive.server2.zookeeper.namespace\":\"hiveserver2-hive2\",\"hive.tez.bucket.pruning\":\"true\",\"hive.tez.exec.print.summary\":\"true\",\"hive.tez.input.generate.consistent.splits\":\"true\",\"hive.vectorized.execution.mapjoin.minmax.enabled\":\"true\",\"hive.vectorized.execution.mapjoin.native.enabled\":\"true\",\"hive.vectorized.execution.mapjoin.native.fast.hashtable.enabled\":\"true\",\"hive.vectorized.execution.reduce.enabled\":\"true\",\"llap.shuffle.connection-keep-alive.enable\":\"true\",\"llap.shuffle.connection-keep-alive.timeout\":\"60\"}",
    "description": "",
    "filename": "hive-interactive-site.xml",
    "timestamp": 1475677909645
  }
}
```

### List Service Configurations

`GET /api/v1/catalog/services/:serviceId/configurations`

`GET /api/v1/catalog/services/name/:serviceName/configurations`

### Get Service Configuration

`GET /api/v1/catalog/services/:serviceId/configurations/:configurationId`

### Update Service Configuration

`PUT /api/v1/catalog/services/:serviceId/configurations/:configurationId`

### Delete Service Configuration

`DELETE /api/v1/catalog/services/:serviceId/configurations/:configurationId`

### Create components

`POST /api/v1/services/:serviceId/components`

**Sample Input**

```json
{
  "serviceId": 13,
  "name": "HIVE_SERVER",
  "hosts": [
    "sandbox.hortonworks.com"
  ],
  "protocol": "thrift",
  "port": 10500
}
```
   
**Success Response**

    HTTP/1.1 201 Created
    Content-Type: application/json
    
```json
{
  "responseCode": 1000,
  "responseMessage": "Success",
  "entity": {
    "id": 39,
    "serviceId": 13,
    "name": "HIVE_SERVER",
    "hosts": [
      "sandbox.hortonworks.com"
    ],
    "protocol": "thrift",
    "port": 10500,
    "timestamp": 1475678254672
  }
}
```

### List Components

`GET /api/v1/catalog/clusters/:clusterId/services`

### Get Component

`GET /api/v1/catalog/clusters/{clusterId}/services/{id}`

### Update Component

`PUT /api/v1/catalog/services/{serviceId}/configurations/{id}`

### Delete Component

`DELETE /api/v1/catalog/services/{serviceId}/configurations/{id}`


### Importing Ambari Cluster to Streamline Cluster
    
Streamline provides importing Ambari Cluster to Streamline Cluster, which means that known services and relevant components are automatically registered from Ambari.
User should create a new Cluster entity or specify existing one. If user chooses existing one, relevant services and service configurations, and components will be removed.

`POST /api/v1/cluster/import/ambari`

**Sample Input**

```json
{
  "clusterId": 1,
  "ambariRestApiRootUrl": "http://ambari-cluster:8080/api/v1/clusters/Sandbox",
  "username": "admin",
  "password": "admin"
}
```

**Success Response**

    HTTP/1.1 200 OK
    Content-Type: application/json

```json
{
  "responseCode": 1000,
  "responseMessage": "Success",
  "entity": {
    "cluster": {
      "id": 1,
      "name": "c1",
      "description": "test cluster",
      "timestamp": 1479709216707
    },
    "services": [
      {
        "service": {
          "id": 5,
          "clusterId": 1,
          "name": "KAFKA",
          "description": "",
          "timestamp": 1479709233415
        },
        "configurations": [
          {
            "id": 9,
            "serviceId": 5,
            "name": "kafka-broker",
            "configuration": "{\"auto.create.topics.enable\":\"true\",\"auto.leader.rebalance.enable\":\"true\",\"compression.type\":\"producer\",\"controlled.shutdown.enable\":\"true\",\"controlled.shutdown.max.retries\":\"3\",\"controlled.shutdown.retry.backoff.ms\":\"5000\",\"controller.message.queue.size\":\"10\",\"controller.socket.timeout.ms\":\"30000\",\"default.replication.factor\":\"1\",\"delete.topic.enable\":\"false\",\"external.kafka.metrics.exclude.prefix\":\"kafka.network.RequestMetrics,kafka.server.DelayedOperationPurgatory,kafka.server.BrokerTopicMetrics.BytesRejectedPerSec\",\"external.kafka.metrics.include.prefix\":\"kafka.network.RequestMetrics.ResponseQueueTimeMs.request.OffsetCommit.98percentile,kafka.network.RequestMetrics.ResponseQueueTimeMs.request.Offsets.95percentile,kafka.network.RequestMetrics.ResponseSendTimeMs.request.Fetch.95percentile,kafka.network.RequestMetrics.RequestsPerSec.request\",\"fetch.purgatory.purge.interval.requests\":\"10000\",\"kafka.ganglia.metrics.group\":\"kafka\",\"kafka.ganglia.metrics.host\":\"localhost\",\"kafka.ganglia.metrics.port\":\"8671\",\"kafka.ganglia.metrics.reporter.enabled\":\"true\",\"kafka.metrics.reporters\":\"org.apache.hadoop.metrics2.sink.kafka.KafkaTimelineMetricsReporter\",\"kafka.timeline.metrics.host\":\"{{metric_collector_host}}\",\"kafka.timeline.metrics.maxRowCacheSize\":\"10000\",\"kafka.timeline.metrics.port\":\"{{metric_collector_port}}\",\"kafka.timeline.metrics.protocol\":\"{{metric_collector_protocol}}\",\"kafka.timeline.metrics.reporter.enabled\":\"true\",\"kafka.timeline.metrics.reporter.sendInterval\":\"5900\",\"kafka.timeline.metrics.truststore.password\":\"{{metric_truststore_password}}\",\"kafka.timeline.metrics.truststore.path\":\"{{metric_truststore_path}}\",\"kafka.timeline.metrics.truststore.type\":\"{{metric_truststore_type}}\",\"leader.imbalance.check.interval.seconds\":\"300\",\"leader.imbalance.per.broker.percentage\":\"10\",\"listeners\":\"PLAINTEXT://localhost:6667\",\"log.cleanup.interval.mins\":\"10\",\"log.dirs\":\"/kafka-logs\",\"log.index.interval.bytes\":\"4096\",\"log.index.size.max.bytes\":\"10485760\",\"log.retention.bytes\":\"-1\",\"log.retention.hours\":\"168\",\"log.roll.hours\":\"168\",\"log.segment.bytes\":\"1073741824\",\"message.max.bytes\":\"1000000\",\"min.insync.replicas\":\"1\",\"num.io.threads\":\"8\",\"num.network.threads\":\"3\",\"num.partitions\":\"1\",\"num.recovery.threads.per.data.dir\":\"1\",\"num.replica.fetchers\":\"1\",\"offset.metadata.max.bytes\":\"4096\",\"offsets.commit.required.acks\":\"-1\",\"offsets.commit.timeout.ms\":\"5000\",\"offsets.load.buffer.size\":\"5242880\",\"offsets.retention.check.interval.ms\":\"600000\",\"offsets.retention.minutes\":\"86400000\",\"offsets.topic.compression.codec\":\"0\",\"offsets.topic.num.partitions\":\"50\",\"offsets.topic.replication.factor\":\"3\",\"offsets.topic.segment.bytes\":\"104857600\",\"port\":\"6667\",\"producer.purgatory.purge.interval.requests\":\"10000\",\"queued.max.requests\":\"500\",\"replica.fetch.max.bytes\":\"1048576\",\"replica.fetch.min.bytes\":\"1\",\"replica.fetch.wait.max.ms\":\"500\",\"replica.high.watermark.checkpoint.interval.ms\":\"5000\",\"replica.lag.max.messages\":\"4000\",\"replica.lag.time.max.ms\":\"10000\",\"replica.socket.receive.buffer.bytes\":\"65536\",\"replica.socket.timeout.ms\":\"30000\",\"socket.receive.buffer.bytes\":\"102400\",\"socket.request.max.bytes\":\"104857600\",\"socket.send.buffer.bytes\":\"102400\",\"zookeeper.connect\":\"sandbox.hortonworks.com:2181\",\"zookeeper.connection.timeout.ms\":\"25000\",\"zookeeper.session.timeout.ms\":\"30000\",\"zookeeper.sync.time.ms\":\"2000\"}",
            "description": "",
            "filename": null,
            "timestamp": 1479709233416,
            "configurationMap": {
              "auto.create.topics.enable": "true",
              "auto.leader.rebalance.enable": "true",
              "compression.type": "producer",
              "controlled.shutdown.enable": "true",
              "controlled.shutdown.max.retries": "3",
              "controlled.shutdown.retry.backoff.ms": "5000",
              "controller.message.queue.size": "10",
              "controller.socket.timeout.ms": "30000",
              "default.replication.factor": "1",
              "delete.topic.enable": "false",
              "external.kafka.metrics.exclude.prefix": "kafka.network.RequestMetrics,kafka.server.DelayedOperationPurgatory,kafka.server.BrokerTopicMetrics.BytesRejectedPerSec",
              "external.kafka.metrics.include.prefix": "kafka.network.RequestMetrics.ResponseQueueTimeMs.request.OffsetCommit.98percentile,kafka.network.RequestMetrics.ResponseQueueTimeMs.request.Offsets.95percentile,kafka.network.RequestMetrics.ResponseSendTimeMs.request.Fetch.95percentile,kafka.network.RequestMetrics.RequestsPerSec.request",
              "fetch.purgatory.purge.interval.requests": "10000",
              "kafka.ganglia.metrics.group": "kafka",
              "kafka.ganglia.metrics.host": "localhost",
              "kafka.ganglia.metrics.port": "8671",
              "kafka.ganglia.metrics.reporter.enabled": "true",
              "kafka.metrics.reporters": "org.apache.hadoop.metrics2.sink.kafka.KafkaTimelineMetricsReporter",
              "kafka.timeline.metrics.host": "{{metric_collector_host}}",
              "kafka.timeline.metrics.maxRowCacheSize": "10000",
              "kafka.timeline.metrics.port": "{{metric_collector_port}}",
              "kafka.timeline.metrics.protocol": "{{metric_collector_protocol}}",
              "kafka.timeline.metrics.reporter.enabled": "true",
              "kafka.timeline.metrics.reporter.sendInterval": "5900",
              "kafka.timeline.metrics.truststore.password": "{{metric_truststore_password}}",
              "kafka.timeline.metrics.truststore.path": "{{metric_truststore_path}}",
              "kafka.timeline.metrics.truststore.type": "{{metric_truststore_type}}",
              "leader.imbalance.check.interval.seconds": "300",
              "leader.imbalance.per.broker.percentage": "10",
              "listeners": "PLAINTEXT://localhost:6667",
              "log.cleanup.interval.mins": "10",
              "log.dirs": "/kafka-logs",
              "log.index.interval.bytes": "4096",
              "log.index.size.max.bytes": "10485760",
              "log.retention.bytes": "-1",
              "log.retention.hours": "168",
              "log.roll.hours": "168",
              "log.segment.bytes": "1073741824",
              "message.max.bytes": "1000000",
              "min.insync.replicas": "1",
              "num.io.threads": "8",
              "num.network.threads": "3",
              "num.partitions": "1",
              "num.recovery.threads.per.data.dir": "1",
              "num.replica.fetchers": "1",
              "offset.metadata.max.bytes": "4096",
              "offsets.commit.required.acks": "-1",
              "offsets.commit.timeout.ms": "5000",
              "offsets.load.buffer.size": "5242880",
              "offsets.retention.check.interval.ms": "600000",
              "offsets.retention.minutes": "86400000",
              "offsets.topic.compression.codec": "0",
              "offsets.topic.num.partitions": "50",
              "offsets.topic.replication.factor": "3",
              "offsets.topic.segment.bytes": "104857600",
              "port": "6667",
              "producer.purgatory.purge.interval.requests": "10000",
              "queued.max.requests": "500",
              "replica.fetch.max.bytes": "1048576",
              "replica.fetch.min.bytes": "1",
              "replica.fetch.wait.max.ms": "500",
              "replica.high.watermark.checkpoint.interval.ms": "5000",
              "replica.lag.max.messages": "4000",
              "replica.lag.time.max.ms": "10000",
              "replica.socket.receive.buffer.bytes": "65536",
              "replica.socket.timeout.ms": "30000",
              "socket.receive.buffer.bytes": "102400",
              "socket.request.max.bytes": "104857600",
              "socket.send.buffer.bytes": "102400",
              "zookeeper.connect": "sandbox.hortonworks.com:2181",
              "zookeeper.connection.timeout.ms": "25000",
              "zookeeper.session.timeout.ms": "30000",
              "zookeeper.sync.time.ms": "2000"
            }
          },
          {
            "id": 11,
            "serviceId": 5,
            "name": "kafka-env",
            "configuration": "{\"content\":\"\\n#!/bin/bash\\n\\n# Set KAFKA specific environment variables here.\\n\\n# The java implementation to use.\\nexport JAVA_HOME={{java64_home}}\\nexport PATH=$PATH:$JAVA_HOME/bin\\nexport PID_DIR={{kafka_pid_dir}}\\nexport LOG_DIR={{kafka_log_dir}}\\nexport KAFKA_KERBEROS_PARAMS={{kafka_kerberos_params}}\\n# Add kafka sink to classpath and related depenencies\\nif [ -e \"/usr/lib/ambari-metrics-kafka-sink/ambari-metrics-kafka-sink.jar\" ]; then\\n  export CLASSPATH=$CLASSPATH:/usr/lib/ambari-metrics-kafka-sink/ambari-metrics-kafka-sink.jar\\n  export CLASSPATH=$CLASSPATH:/usr/lib/ambari-metrics-kafka-sink/lib/*\\nfi\\nif [ -f /etc/kafka/conf/kafka-ranger-env.sh ]; then\\n. /etc/kafka/conf/kafka-ranger-env.sh\\nfi\",\"is_supported_kafka_ranger\":\"true\",\"kafka_log_dir\":\"/var/log/kafka\",\"kafka_pid_dir\":\"/var/run/kafka\",\"kafka_user\":\"kafka\",\"kafka_user_nofile_limit\":\"128000\",\"kafka_user_nproc_limit\":\"65536\"}",
            "description": "",
            "filename": null,
            "timestamp": 1479709233416,
            "configurationMap": {
              "content": "\n#!/bin/bash\n\n# Set KAFKA specific environment variables here.\n\n# The java implementation to use.\nexport JAVA_HOME={{java64_home}}\nexport PATH=$PATH:$JAVA_HOME/bin\nexport PID_DIR={{kafka_pid_dir}}\nexport LOG_DIR={{kafka_log_dir}}\nexport KAFKA_KERBEROS_PARAMS={{kafka_kerberos_params}}\n# Add kafka sink to classpath and related depenencies\nif [ -e \"/usr/lib/ambari-metrics-kafka-sink/ambari-metrics-kafka-sink.jar\" ]; then\n  export CLASSPATH=$CLASSPATH:/usr/lib/ambari-metrics-kafka-sink/ambari-metrics-kafka-sink.jar\n  export CLASSPATH=$CLASSPATH:/usr/lib/ambari-metrics-kafka-sink/lib/*\nfi\nif [ -f /etc/kafka/conf/kafka-ranger-env.sh ]; then\n. /etc/kafka/conf/kafka-ranger-env.sh\nfi",
              "is_supported_kafka_ranger": "true",
              "kafka_log_dir": "/var/log/kafka",
              "kafka_pid_dir": "/var/run/kafka",
              "kafka_user": "kafka",
              "kafka_user_nofile_limit": "128000",
              "kafka_user_nproc_limit": "65536"
            }
          }
        ],
        "components": [
          {
            "id": 13,
            "serviceId": 5,
            "name": "KAFKA_BROKER",
            "hosts": [
              "sandbox.hortonworks.com"
            ],
            "protocol": "PLAINTEXT",
            "port": 6667,
            "timestamp": 1479709233445
          }
        ]
      },
      {
        "service": {
          "id": 7,
          "clusterId": 1,
          "name": "HDFS",
          "description": "",
          "timestamp": 1479709233509
        },
        "configurations": [
          {
            "id": 13,
            "serviceId": 7,
            "name": "hdfs-site",
            "configuration": "{\"dfs.block.access.token.enable\":\"true\",\"dfs.blockreport.initialDelay\":\"120\",\"dfs.blocksize\":\"134217728\",\"dfs.client.read.shortcircuit\":\"true\",\"dfs.client.read.shortcircuit.streams.cache.size\":\"4096\",\"dfs.client.retry.policy.enabled\":\"false\",\"dfs.cluster.administrators\":\" hdfs\",\"dfs.content-summary.limit\":\"5000\",\"dfs.datanode.address\":\"0.0.0.0:50010\",\"dfs.datanode.balance.bandwidthPerSec\":\"6250000\",\"dfs.datanode.data.dir\":\"/hadoop/hdfs/data\",\"dfs.datanode.data.dir.perm\":\"750\",\"dfs.datanode.du.reserved\":\"1073741824\",\"dfs.datanode.failed.volumes.tolerated\":\"0\",\"dfs.datanode.http.address\":\"0.0.0.0:50075\",\"dfs.datanode.https.address\":\"0.0.0.0:50475\",\"dfs.datanode.ipc.address\":\"0.0.0.0:8010\",\"dfs.datanode.max.transfer.threads\":\"16384\",\"dfs.domain.socket.path\":\"/var/lib/hadoop-hdfs/dn_socket\",\"dfs.encrypt.data.transfer.cipher.suites\":\"AES/CTR/NoPadding\",\"dfs.encryption.key.provider.uri\":\"\",\"dfs.heartbeat.interval\":\"3\",\"dfs.hosts.exclude\":\"/etc/hadoop/conf/dfs.exclude\",\"dfs.http.policy\":\"HTTP_ONLY\",\"dfs.https.port\":\"50470\",\"dfs.journalnode.edits.dir\":\"/hadoop/hdfs/journalnode\",\"dfs.journalnode.http-address\":\"0.0.0.0:8480\",\"dfs.journalnode.https-address\":\"0.0.0.0:8481\",\"dfs.namenode.accesstime.precision\":\"0\",\"dfs.namenode.audit.log.async\":\"true\",\"dfs.namenode.avoid.read.stale.datanode\":\"true\",\"dfs.namenode.avoid.write.stale.datanode\":\"true\",\"dfs.namenode.checkpoint.dir\":\"/hadoop/hdfs/namesecondary\",\"dfs.namenode.checkpoint.edits.dir\":\"${dfs.namenode.checkpoint.dir}\",\"dfs.namenode.checkpoint.period\":\"21600\",\"dfs.namenode.checkpoint.txns\":\"1000000\",\"dfs.namenode.fslock.fair\":\"false\",\"dfs.namenode.handler.count\":\"25\",\"dfs.namenode.http-address\":\"sandbox.hortonworks.com:50070\",\"dfs.namenode.https-address\":\"sandbox.hortonworks.com:50470\",\"dfs.namenode.inode.attributes.provider.class\":\"org.apache.ranger.authorization.hadoop.RangerHdfsAuthorizer\",\"dfs.namenode.name.dir\":\"/hadoop/hdfs/namenode\",\"dfs.namenode.name.dir.restore\":\"true\",\"dfs.namenode.rpc-address\":\"sandbox.hortonworks.com:8020\",\"dfs.namenode.safemode.threshold-pct\":\"1.000\",\"dfs.namenode.secondary.http-address\":\"sandbox.hortonworks.com:50090\",\"dfs.namenode.stale.datanode.interval\":\"30000\",\"dfs.namenode.startup.delay.block.deletion.sec\":\"3600\",\"dfs.namenode.write.stale.datanode.ratio\":\"1.0f\",\"dfs.permissions.enabled\":\"true\",\"dfs.permissions.superusergroup\":\"hdfs\",\"dfs.replication\":\"1\",\"dfs.replication.max\":\"50\",\"dfs.support.append\":\"true\",\"dfs.webhdfs.enabled\":\"true\",\"fs.permissions.umask-mode\":\"022\",\"nfs.exports.allowed.hosts\":\"* rw\",\"nfs.file.dump.dir\":\"/tmp/.hdfs-nfs\"}",
            "description": "",
            "filename": "hdfs-site.xml",
            "timestamp": 1479709233509,
            "configurationMap": {
              "dfs.block.access.token.enable": "true",
              "dfs.blockreport.initialDelay": "120",
              "dfs.blocksize": "134217728",
              "dfs.client.read.shortcircuit": "true",
              "dfs.client.read.shortcircuit.streams.cache.size": "4096",
              "dfs.client.retry.policy.enabled": "false",
              "dfs.cluster.administrators": " hdfs",
              "dfs.content-summary.limit": "5000",
              "dfs.datanode.address": "0.0.0.0:50010",
              "dfs.datanode.balance.bandwidthPerSec": "6250000",
              "dfs.datanode.data.dir": "/hadoop/hdfs/data",
              "dfs.datanode.data.dir.perm": "750",
              "dfs.datanode.du.reserved": "1073741824",
              "dfs.datanode.failed.volumes.tolerated": "0",
              "dfs.datanode.http.address": "0.0.0.0:50075",
              "dfs.datanode.https.address": "0.0.0.0:50475",
              "dfs.datanode.ipc.address": "0.0.0.0:8010",
              "dfs.datanode.max.transfer.threads": "16384",
              "dfs.domain.socket.path": "/var/lib/hadoop-hdfs/dn_socket",
              "dfs.encrypt.data.transfer.cipher.suites": "AES/CTR/NoPadding",
              "dfs.encryption.key.provider.uri": "",
              "dfs.heartbeat.interval": "3",
              "dfs.hosts.exclude": "/etc/hadoop/conf/dfs.exclude",
              "dfs.http.policy": "HTTP_ONLY",
              "dfs.https.port": "50470",
              "dfs.journalnode.edits.dir": "/hadoop/hdfs/journalnode",
              "dfs.journalnode.http-address": "0.0.0.0:8480",
              "dfs.journalnode.https-address": "0.0.0.0:8481",
              "dfs.namenode.accesstime.precision": "0",
              "dfs.namenode.audit.log.async": "true",
              "dfs.namenode.avoid.read.stale.datanode": "true",
              "dfs.namenode.avoid.write.stale.datanode": "true",
              "dfs.namenode.checkpoint.dir": "/hadoop/hdfs/namesecondary",
              "dfs.namenode.checkpoint.edits.dir": "${dfs.namenode.checkpoint.dir}",
              "dfs.namenode.checkpoint.period": "21600",
              "dfs.namenode.checkpoint.txns": "1000000",
              "dfs.namenode.fslock.fair": "false",
              "dfs.namenode.handler.count": "25",
              "dfs.namenode.http-address": "sandbox.hortonworks.com:50070",
              "dfs.namenode.https-address": "sandbox.hortonworks.com:50470",
              "dfs.namenode.inode.attributes.provider.class": "org.apache.ranger.authorization.hadoop.RangerHdfsAuthorizer",
              "dfs.namenode.name.dir": "/hadoop/hdfs/namenode",
              "dfs.namenode.name.dir.restore": "true",
              "dfs.namenode.rpc-address": "sandbox.hortonworks.com:8020",
              "dfs.namenode.safemode.threshold-pct": "1.000",
              "dfs.namenode.secondary.http-address": "sandbox.hortonworks.com:50090",
              "dfs.namenode.stale.datanode.interval": "30000",
              "dfs.namenode.startup.delay.block.deletion.sec": "3600",
              "dfs.namenode.write.stale.datanode.ratio": "1.0f",
              "dfs.permissions.enabled": "true",
              "dfs.permissions.superusergroup": "hdfs",
              "dfs.replication": "1",
              "dfs.replication.max": "50",
              "dfs.support.append": "true",
              "dfs.webhdfs.enabled": "true",
              "fs.permissions.umask-mode": "022",
              "nfs.exports.allowed.hosts": "* rw",
              "nfs.file.dump.dir": "/tmp/.hdfs-nfs"
            }
          },
          {
            "id": 15,
            "serviceId": 7,
            "name": "hadoop-env",
            "configuration": "{\"content\":\"\\n# Set Hadoop-specific environment variables here.\\n\\n# The only required environment variable is JAVA_HOME.  All others are\\n# optional.  When running a distributed configuration it is best to\\n# set JAVA_HOME in this file, so that it is correctly defined on\\n# remote nodes.\\n\\n# The java implementation to use.  Required.\\nexport JAVA_HOME={{java_home}}\\nexport HADOOP_HOME_WARN_SUPPRESS=1\\n\\n# Hadoop home directory\\nexport HADOOP_HOME=${HADOOP_HOME:-{{hadoop_home}}}\\n\\n# Hadoop Configuration Directory\\n\\n{# this is different for HDP1 #}\\n# Path to jsvc required by secure HDP 2.0 datanode\\nexport JSVC_HOME={{jsvc_path}}\\n\\n\\n# The maximum amount of heap to use, in MB. Default is 1000.\\nexport HADOOP_HEAPSIZE=\"{{hadoop_heapsize}}\"\\n\\nexport HADOOP_NAMENODE_INIT_HEAPSIZE=\"-Xms{{namenode_heapsize}}\"\\n\\n# Extra Java runtime options.  Empty by default.\\nexport HADOOP_OPTS=\"-Djava.net.preferIPv4Stack=true ${HADOOP_OPTS}\"\\n\\n# Command specific options appended to HADOOP_OPTS when specified\\nHADOOP_JOBTRACKER_OPTS=\"-server -XX:ParallelGCThreads=8 -XX:+UseConcMarkSweepGC -XX:ErrorFile={{hdfs_log_dir_prefix}}/$USER/hs_err_pid%p.log -XX:NewSize={{jtnode_opt_newsize}} -XX:MaxNewSize={{jtnode_opt_maxnewsize}} -Xloggc:{{hdfs_log_dir_prefix}}/$USER/gc.log-`date +'%Y%m%d%H%M'` -verbose:gc -XX:+PrintGCDetails -XX:+PrintGCTimeStamps -XX:+PrintGCDateStamps -Xmx{{jtnode_heapsize}} -Dhadoop.security.logger=INFO,DRFAS -Dmapred.audit.logger=INFO,MRAUDIT -Dhadoop.mapreduce.jobsummary.logger=INFO,JSA ${HADOOP_JOBTRACKER_OPTS}\"\\n\\nHADOOP_TASKTRACKER_OPTS=\"-server -Xmx{{ttnode_heapsize}} -Dhadoop.security.logger=ERROR,console -Dmapred.audit.logger=ERROR,console ${HADOOP_TASKTRACKER_OPTS}\"\\n\\n{% if java_version < 8 %}\\nSHARED_HADOOP_NAMENODE_OPTS=\"-server -XX:ParallelGCThreads=8 -XX:+UseConcMarkSweepGC -XX:ErrorFile={{hdfs_log_dir_prefix}}/$USER/hs_err_pid%p.log -XX:NewSize={{namenode_opt_newsize}} -XX:MaxNewSize={{namenode_opt_maxnewsize}} -XX:PermSize={{namenode_opt_permsize}} -XX:MaxPermSize={{namenode_opt_maxpermsize}} -Xloggc:{{hdfs_log_dir_prefix}}/$USER/gc.log-`date +'%Y%m%d%H%M'` -verbose:gc -XX:+PrintGCDetails -XX:+PrintGCTimeStamps -XX:+PrintGCDateStamps -XX:CMSInitiatingOccupancyFraction=70 -XX:+UseCMSInitiatingOccupancyOnly -Xms{{namenode_heapsize}} -Xmx{{namenode_heapsize}} -Dhadoop.security.logger=INFO,DRFAS -Dhdfs.audit.logger=INFO,DRFAAUDIT\"\\nexport HADOOP_NAMENODE_OPTS=\"${SHARED_HADOOP_NAMENODE_OPTS} -XX:OnOutOfMemoryError=\\\\\"/usr/hdp/current/hadoop-hdfs-namenode/bin/kill-name-node\\\\\" -Dorg.mortbay.jetty.Request.maxFormContentSize=-1 ${HADOOP_NAMENODE_OPTS}\"\\nexport HADOOP_DATANODE_OPTS=\"-server -XX:ParallelGCThreads=4 -XX:+UseConcMarkSweepGC -XX:ErrorFile=/var/log/hadoop/$USER/hs_err_pid%p.log -XX:NewSize=200m -XX:MaxNewSize=200m -XX:PermSize=128m -XX:MaxPermSize=256m -Xloggc:/var/log/hadoop/$USER/gc.log-`date +'%Y%m%d%H%M'` -verbose:gc -XX:+PrintGCDetails -XX:+PrintGCTimeStamps -XX:+PrintGCDateStamps -Xms{{dtnode_heapsize}} -Xmx{{dtnode_heapsize}} -Dhadoop.security.logger=INFO,DRFAS -Dhdfs.audit.logger=INFO,DRFAAUDIT ${HADOOP_DATANODE_OPTS}\"\\n\\nexport HADOOP_SECONDARYNAMENODE_OPTS=\"${SHARED_HADOOP_NAMENODE_OPTS} -XX:OnOutOfMemoryError=\\\\\"/usr/hdp/current/hadoop-hdfs-secondarynamenode/bin/kill-secondary-name-node\\\\\" ${HADOOP_SECONDARYNAMENODE_OPTS}\"\\n\\n# The following applies to multiple commands (fs, dfs, fsck, distcp etc)\\nexport HADOOP_CLIENT_OPTS=\"-Xmx${HADOOP_HEAPSIZE}m -XX:MaxPermSize=512m $HADOOP_CLIENT_OPTS\"\\n\\n{% else %}\\nSHARED_HADOOP_NAMENODE_OPTS=\"-server -XX:ParallelGCThreads=8 -XX:+UseConcMarkSweepGC -XX:ErrorFile={{hdfs_log_dir_prefix}}/$USER/hs_err_pid%p.log -XX:NewSize={{namenode_opt_newsize}} -XX:MaxNewSize={{namenode_opt_maxnewsize}} -Xloggc:{{hdfs_log_dir_prefix}}/$USER/gc.log-`date +'%Y%m%d%H%M'` -verbose:gc -XX:+PrintGCDetails -XX:+PrintGCTimeStamps -XX:+PrintGCDateStamps -XX:CMSInitiatingOccupancyFraction=70 -XX:+UseCMSInitiatingOccupancyOnly -Xms{{namenode_heapsize}} -Xmx{{namenode_heapsize}} -Dhadoop.security.logger=INFO,DRFAS -Dhdfs.audit.logger=INFO,DRFAAUDIT\"\\nexport HADOOP_NAMENODE_OPTS=\"${SHARED_HADOOP_NAMENODE_OPTS} -XX:OnOutOfMemoryError=\\\\\"/usr/hdp/current/hadoop-hdfs-namenode/bin/kill-name-node\\\\\" -Dorg.mortbay.jetty.Request.maxFormContentSize=-1 ${HADOOP_NAMENODE_OPTS}\"\\nexport HADOOP_DATANODE_OPTS=\"-server -XX:ParallelGCThreads=4 -XX:+UseConcMarkSweepGC -XX:ErrorFile=/var/log/hadoop/$USER/hs_err_pid%p.log -XX:NewSize=200m -XX:MaxNewSize=200m -Xloggc:/var/log/hadoop/$USER/gc.log-`date +'%Y%m%d%H%M'` -verbose:gc -XX:+PrintGCDetails -XX:+PrintGCTimeStamps -XX:+PrintGCDateStamps -Xms{{dtnode_heapsize}} -Xmx{{dtnode_heapsize}} -Dhadoop.security.logger=INFO,DRFAS -Dhdfs.audit.logger=INFO,DRFAAUDIT ${HADOOP_DATANODE_OPTS}\"\\n\\nexport HADOOP_SECONDARYNAMENODE_OPTS=\"${SHARED_HADOOP_NAMENODE_OPTS} -XX:OnOutOfMemoryError=\\\\\"/usr/hdp/current/hadoop-hdfs-secondarynamenode/bin/kill-secondary-name-node\\\\\" ${HADOOP_SECONDARYNAMENODE_OPTS}\"\\n\\n# The following applies to multiple commands (fs, dfs, fsck, distcp etc)\\nexport HADOOP_CLIENT_OPTS=\"-Xmx${HADOOP_HEAPSIZE}m $HADOOP_CLIENT_OPTS\"\\n{% endif %}\\n\\nHADOOP_NFS3_OPTS=\"-Xmx{{nfsgateway_heapsize}}m -Dhadoop.security.logger=ERROR,DRFAS ${HADOOP_NFS3_OPTS}\"\\nHADOOP_BALANCER_OPTS=\"-server -Xmx{{hadoop_heapsize}}m ${HADOOP_BALANCER_OPTS}\"\\n\\n\\n# On secure datanodes, user to run the datanode as after dropping privileges\\nexport HADOOP_SECURE_DN_USER=${HADOOP_SECURE_DN_USER:-{{hadoop_secure_dn_user}}}\\n\\n# Extra ssh options.  Empty by default.\\nexport HADOOP_SSH_OPTS=\"-o ConnectTimeout=5 -o SendEnv=HADOOP_CONF_DIR\"\\n\\n# Where log files are stored.  $HADOOP_HOME/logs by default.\\nexport HADOOP_LOG_DIR={{hdfs_log_dir_prefix}}/$USER\\n\\n# History server logs\\nexport HADOOP_MAPRED_LOG_DIR={{mapred_log_dir_prefix}}/$USER\\n\\n# Where log files are stored in the secure data environment.\\nexport HADOOP_SECURE_DN_LOG_DIR={{hdfs_log_dir_prefix}}/$HADOOP_SECURE_DN_USER\\n\\n# File naming remote slave hosts.  $HADOOP_HOME/conf/slaves by default.\\n# export HADOOP_SLAVES=${HADOOP_HOME}/conf/slaves\\n\\n# host:path where hadoop code should be rsync'd from.  Unset by default.\\n# export HADOOP_MASTER=master:/home/$USER/src/hadoop\\n\\n# Seconds to sleep between slave commands.  Unset by default.  This\\n# can be useful in large clusters, where, e.g., slave rsyncs can\\n# otherwise arrive faster than the master can service them.\\n# export HADOOP_SLAVE_SLEEP=0.1\\n\\n# The directory where pid files are stored. /tmp by default.\\nexport HADOOP_PID_DIR={{hadoop_pid_dir_prefix}}/$USER\\nexport HADOOP_SECURE_DN_PID_DIR={{hadoop_pid_dir_prefix}}/$HADOOP_SECURE_DN_USER\\n\\n# History server pid\\nexport HADOOP_MAPRED_PID_DIR={{mapred_pid_dir_prefix}}/$USER\\n\\nYARN_RESOURCEMANAGER_OPTS=\"-Dyarn.server.resourcemanager.appsummary.logger=INFO,RMSUMMARY\"\\n\\n# A string representing this instance of hadoop. $USER by default.\\nexport HADOOP_IDENT_STRING=$USER\\n\\n# The scheduling priority for daemon processes.  See 'man nice'.\\n\\n# export HADOOP_NICENESS=10\\n\\n# Add database libraries\\nJAVA_JDBC_LIBS=\"\"\\nif [ -d \"/usr/share/java\" ]; then\\n  for jarFile in `ls /usr/share/java | grep -E \"(mysql|ojdbc|postgresql|sqljdbc)\" 2>/dev/null`\\n  do\\n    JAVA_JDBC_LIBS=${JAVA_JDBC_LIBS}:$jarFile\\n  done\\nfi\\n\\n# Add libraries to the hadoop classpath - some may not need a colon as they already include it\\nexport HADOOP_CLASSPATH=${HADOOP_CLASSPATH}${JAVA_JDBC_LIBS}\\n\\n# Setting path to hdfs command line\\nexport HADOOP_LIBEXEC_DIR={{hadoop_libexec_dir}}\\n\\n# Mostly required for hadoop 2.0\\nexport JAVA_LIBRARY_PATH=${JAVA_LIBRARY_PATH}\\n\\nexport HADOOP_OPTS=\"-Dhdp.version=$HDP_VERSION $HADOOP_OPTS\"\\n\\n{% if is_datanode_max_locked_memory_set %}\\n# Fix temporary bug, when ulimit from conf files is not picked up, without full relogin. \\n# Makes sense to fix only when runing DN as root \\nif [ \"$command\" == \"datanode\" ] && [ \"$EUID\" -eq 0 ] && [ -n \"$HADOOP_SECURE_DN_USER\" ]; then\\n  ulimit -l {{datanode_max_locked_memory}}\\nfi\\n{% endif %}\",\"dtnode_heapsize\":\"250m\",\"hadoop_heapsize\":\"250\",\"hadoop_pid_dir_prefix\":\"/var/run/hadoop\",\"hadoop_root_logger\":\"INFO,RFA\",\"hdfs_log_dir_prefix\":\"/var/log/hadoop\",\"hdfs_tmp_dir\":\"/tmp\",\"hdfs_user\":\"hdfs\",\"hdfs_user_nofile_limit\":\"128000\",\"hdfs_user_nproc_limit\":\"65536\",\"keyserver_host\":\" \",\"keyserver_port\":\"\",\"namenode_heapsize\":\"250m\",\"namenode_opt_maxnewsize\":\"100m\",\"namenode_opt_maxpermsize\":\"256m\",\"namenode_opt_newsize\":\"50m\",\"namenode_opt_permsize\":\"128m\",\"nfsgateway_heapsize\":\"1024\",\"proxyuser_group\":\"users\"}",
            "description": "",
            "filename": null,
            "timestamp": 1479709233510,
            "configurationMap": {
              "content": "\n# Set Hadoop-specific environment variables here.\n\n# The only required environment variable is JAVA_HOME.  All others are\n# optional.  When running a distributed configuration it is best to\n# set JAVA_HOME in this file, so that it is correctly defined on\n# remote nodes.\n\n# The java implementation to use.  Required.\nexport JAVA_HOME={{java_home}}\nexport HADOOP_HOME_WARN_SUPPRESS=1\n\n# Hadoop home directory\nexport HADOOP_HOME=${HADOOP_HOME:-{{hadoop_home}}}\n\n# Hadoop Configuration Directory\n\n{# this is different for HDP1 #}\n# Path to jsvc required by secure HDP 2.0 datanode\nexport JSVC_HOME={{jsvc_path}}\n\n\n# The maximum amount of heap to use, in MB. Default is 1000.\nexport HADOOP_HEAPSIZE=\"{{hadoop_heapsize}}\"\n\nexport HADOOP_NAMENODE_INIT_HEAPSIZE=\"-Xms{{namenode_heapsize}}\"\n\n# Extra Java runtime options.  Empty by default.\nexport HADOOP_OPTS=\"-Djava.net.preferIPv4Stack=true ${HADOOP_OPTS}\"\n\n# Command specific options appended to HADOOP_OPTS when specified\nHADOOP_JOBTRACKER_OPTS=\"-server -XX:ParallelGCThreads=8 -XX:+UseConcMarkSweepGC -XX:ErrorFile={{hdfs_log_dir_prefix}}/$USER/hs_err_pid%p.log -XX:NewSize={{jtnode_opt_newsize}} -XX:MaxNewSize={{jtnode_opt_maxnewsize}} -Xloggc:{{hdfs_log_dir_prefix}}/$USER/gc.log-`date +'%Y%m%d%H%M'` -verbose:gc -XX:+PrintGCDetails -XX:+PrintGCTimeStamps -XX:+PrintGCDateStamps -Xmx{{jtnode_heapsize}} -Dhadoop.security.logger=INFO,DRFAS -Dmapred.audit.logger=INFO,MRAUDIT -Dhadoop.mapreduce.jobsummary.logger=INFO,JSA ${HADOOP_JOBTRACKER_OPTS}\"\n\nHADOOP_TASKTRACKER_OPTS=\"-server -Xmx{{ttnode_heapsize}} -Dhadoop.security.logger=ERROR,console -Dmapred.audit.logger=ERROR,console ${HADOOP_TASKTRACKER_OPTS}\"\n\n{% if java_version < 8 %}\nSHARED_HADOOP_NAMENODE_OPTS=\"-server -XX:ParallelGCThreads=8 -XX:+UseConcMarkSweepGC -XX:ErrorFile={{hdfs_log_dir_prefix}}/$USER/hs_err_pid%p.log -XX:NewSize={{namenode_opt_newsize}} -XX:MaxNewSize={{namenode_opt_maxnewsize}} -XX:PermSize={{namenode_opt_permsize}} -XX:MaxPermSize={{namenode_opt_maxpermsize}} -Xloggc:{{hdfs_log_dir_prefix}}/$USER/gc.log-`date +'%Y%m%d%H%M'` -verbose:gc -XX:+PrintGCDetails -XX:+PrintGCTimeStamps -XX:+PrintGCDateStamps -XX:CMSInitiatingOccupancyFraction=70 -XX:+UseCMSInitiatingOccupancyOnly -Xms{{namenode_heapsize}} -Xmx{{namenode_heapsize}} -Dhadoop.security.logger=INFO,DRFAS -Dhdfs.audit.logger=INFO,DRFAAUDIT\"\nexport HADOOP_NAMENODE_OPTS=\"${SHARED_HADOOP_NAMENODE_OPTS} -XX:OnOutOfMemoryError=\"/usr/hdp/current/hadoop-hdfs-namenode/bin/kill-name-node\" -Dorg.mortbay.jetty.Request.maxFormContentSize=-1 ${HADOOP_NAMENODE_OPTS}\"\nexport HADOOP_DATANODE_OPTS=\"-server -XX:ParallelGCThreads=4 -XX:+UseConcMarkSweepGC -XX:ErrorFile=/var/log/hadoop/$USER/hs_err_pid%p.log -XX:NewSize=200m -XX:MaxNewSize=200m -XX:PermSize=128m -XX:MaxPermSize=256m -Xloggc:/var/log/hadoop/$USER/gc.log-`date +'%Y%m%d%H%M'` -verbose:gc -XX:+PrintGCDetails -XX:+PrintGCTimeStamps -XX:+PrintGCDateStamps -Xms{{dtnode_heapsize}} -Xmx{{dtnode_heapsize}} -Dhadoop.security.logger=INFO,DRFAS -Dhdfs.audit.logger=INFO,DRFAAUDIT ${HADOOP_DATANODE_OPTS}\"\n\nexport HADOOP_SECONDARYNAMENODE_OPTS=\"${SHARED_HADOOP_NAMENODE_OPTS} -XX:OnOutOfMemoryError=\"/usr/hdp/current/hadoop-hdfs-secondarynamenode/bin/kill-secondary-name-node\" ${HADOOP_SECONDARYNAMENODE_OPTS}\"\n\n# The following applies to multiple commands (fs, dfs, fsck, distcp etc)\nexport HADOOP_CLIENT_OPTS=\"-Xmx${HADOOP_HEAPSIZE}m -XX:MaxPermSize=512m $HADOOP_CLIENT_OPTS\"\n\n{% else %}\nSHARED_HADOOP_NAMENODE_OPTS=\"-server -XX:ParallelGCThreads=8 -XX:+UseConcMarkSweepGC -XX:ErrorFile={{hdfs_log_dir_prefix}}/$USER/hs_err_pid%p.log -XX:NewSize={{namenode_opt_newsize}} -XX:MaxNewSize={{namenode_opt_maxnewsize}} -Xloggc:{{hdfs_log_dir_prefix}}/$USER/gc.log-`date +'%Y%m%d%H%M'` -verbose:gc -XX:+PrintGCDetails -XX:+PrintGCTimeStamps -XX:+PrintGCDateStamps -XX:CMSInitiatingOccupancyFraction=70 -XX:+UseCMSInitiatingOccupancyOnly -Xms{{namenode_heapsize}} -Xmx{{namenode_heapsize}} -Dhadoop.security.logger=INFO,DRFAS -Dhdfs.audit.logger=INFO,DRFAAUDIT\"\nexport HADOOP_NAMENODE_OPTS=\"${SHARED_HADOOP_NAMENODE_OPTS} -XX:OnOutOfMemoryError=\"/usr/hdp/current/hadoop-hdfs-namenode/bin/kill-name-node\" -Dorg.mortbay.jetty.Request.maxFormContentSize=-1 ${HADOOP_NAMENODE_OPTS}\"\nexport HADOOP_DATANODE_OPTS=\"-server -XX:ParallelGCThreads=4 -XX:+UseConcMarkSweepGC -XX:ErrorFile=/var/log/hadoop/$USER/hs_err_pid%p.log -XX:NewSize=200m -XX:MaxNewSize=200m -Xloggc:/var/log/hadoop/$USER/gc.log-`date +'%Y%m%d%H%M'` -verbose:gc -XX:+PrintGCDetails -XX:+PrintGCTimeStamps -XX:+PrintGCDateStamps -Xms{{dtnode_heapsize}} -Xmx{{dtnode_heapsize}} -Dhadoop.security.logger=INFO,DRFAS -Dhdfs.audit.logger=INFO,DRFAAUDIT ${HADOOP_DATANODE_OPTS}\"\n\nexport HADOOP_SECONDARYNAMENODE_OPTS=\"${SHARED_HADOOP_NAMENODE_OPTS} -XX:OnOutOfMemoryError=\"/usr/hdp/current/hadoop-hdfs-secondarynamenode/bin/kill-secondary-name-node\" ${HADOOP_SECONDARYNAMENODE_OPTS}\"\n\n# The following applies to multiple commands (fs, dfs, fsck, distcp etc)\nexport HADOOP_CLIENT_OPTS=\"-Xmx${HADOOP_HEAPSIZE}m $HADOOP_CLIENT_OPTS\"\n{% endif %}\n\nHADOOP_NFS3_OPTS=\"-Xmx{{nfsgateway_heapsize}}m -Dhadoop.security.logger=ERROR,DRFAS ${HADOOP_NFS3_OPTS}\"\nHADOOP_BALANCER_OPTS=\"-server -Xmx{{hadoop_heapsize}}m ${HADOOP_BALANCER_OPTS}\"\n\n\n# On secure datanodes, user to run the datanode as after dropping privileges\nexport HADOOP_SECURE_DN_USER=${HADOOP_SECURE_DN_USER:-{{hadoop_secure_dn_user}}}\n\n# Extra ssh options.  Empty by default.\nexport HADOOP_SSH_OPTS=\"-o ConnectTimeout=5 -o SendEnv=HADOOP_CONF_DIR\"\n\n# Where log files are stored.  $HADOOP_HOME/logs by default.\nexport HADOOP_LOG_DIR={{hdfs_log_dir_prefix}}/$USER\n\n# History server logs\nexport HADOOP_MAPRED_LOG_DIR={{mapred_log_dir_prefix}}/$USER\n\n# Where log files are stored in the secure data environment.\nexport HADOOP_SECURE_DN_LOG_DIR={{hdfs_log_dir_prefix}}/$HADOOP_SECURE_DN_USER\n\n# File naming remote slave hosts.  $HADOOP_HOME/conf/slaves by default.\n# export HADOOP_SLAVES=${HADOOP_HOME}/conf/slaves\n\n# host:path where hadoop code should be rsync'd from.  Unset by default.\n# export HADOOP_MASTER=master:/home/$USER/src/hadoop\n\n# Seconds to sleep between slave commands.  Unset by default.  This\n# can be useful in large clusters, where, e.g., slave rsyncs can\n# otherwise arrive faster than the master can service them.\n# export HADOOP_SLAVE_SLEEP=0.1\n\n# The directory where pid files are stored. /tmp by default.\nexport HADOOP_PID_DIR={{hadoop_pid_dir_prefix}}/$USER\nexport HADOOP_SECURE_DN_PID_DIR={{hadoop_pid_dir_prefix}}/$HADOOP_SECURE_DN_USER\n\n# History server pid\nexport HADOOP_MAPRED_PID_DIR={{mapred_pid_dir_prefix}}/$USER\n\nYARN_RESOURCEMANAGER_OPTS=\"-Dyarn.server.resourcemanager.appsummary.logger=INFO,RMSUMMARY\"\n\n# A string representing this instance of hadoop. $USER by default.\nexport HADOOP_IDENT_STRING=$USER\n\n# The scheduling priority for daemon processes.  See 'man nice'.\n\n# export HADOOP_NICENESS=10\n\n# Add database libraries\nJAVA_JDBC_LIBS=\"\"\nif [ -d \"/usr/share/java\" ]; then\n  for jarFile in `ls /usr/share/java | grep -E \"(mysql|ojdbc|postgresql|sqljdbc)\" 2>/dev/null`\n  do\n    JAVA_JDBC_LIBS=${JAVA_JDBC_LIBS}:$jarFile\n  done\nfi\n\n# Add libraries to the hadoop classpath - some may not need a colon as they already include it\nexport HADOOP_CLASSPATH=${HADOOP_CLASSPATH}${JAVA_JDBC_LIBS}\n\n# Setting path to hdfs command line\nexport HADOOP_LIBEXEC_DIR={{hadoop_libexec_dir}}\n\n# Mostly required for hadoop 2.0\nexport JAVA_LIBRARY_PATH=${JAVA_LIBRARY_PATH}\n\nexport HADOOP_OPTS=\"-Dhdp.version=$HDP_VERSION $HADOOP_OPTS\"\n\n{% if is_datanode_max_locked_memory_set %}\n# Fix temporary bug, when ulimit from conf files is not picked up, without full relogin. \n# Makes sense to fix only when runing DN as root \nif [ \"$command\" == \"datanode\" ] && [ \"$EUID\" -eq 0 ] && [ -n \"$HADOOP_SECURE_DN_USER\" ]; then\n  ulimit -l {{datanode_max_locked_memory}}\nfi\n{% endif %}",
              "dtnode_heapsize": "250m",
              "hadoop_heapsize": "250",
              "hadoop_pid_dir_prefix": "/var/run/hadoop",
              "hadoop_root_logger": "INFO,RFA",
              "hdfs_log_dir_prefix": "/var/log/hadoop",
              "hdfs_tmp_dir": "/tmp",
              "hdfs_user": "hdfs",
              "hdfs_user_nofile_limit": "128000",
              "hdfs_user_nproc_limit": "65536",
              "keyserver_host": " ",
              "keyserver_port": "",
              "namenode_heapsize": "250m",
              "namenode_opt_maxnewsize": "100m",
              "namenode_opt_maxpermsize": "256m",
              "namenode_opt_newsize": "50m",
              "namenode_opt_permsize": "128m",
              "nfsgateway_heapsize": "1024",
              "proxyuser_group": "users"
            }
          },
          {
            "id": 17,
            "serviceId": 7,
            "name": "hadoop-policy",
            "configuration": "{\"security.admin.operations.protocol.acl\":\"hadoop\",\"security.client.datanode.protocol.acl\":\"*\",\"security.client.protocol.acl\":\"*\",\"security.datanode.protocol.acl\":\"*\",\"security.inter.datanode.protocol.acl\":\"*\",\"security.inter.tracker.protocol.acl\":\"*\",\"security.job.client.protocol.acl\":\"*\",\"security.job.task.protocol.acl\":\"*\",\"security.namenode.protocol.acl\":\"*\",\"security.refresh.policy.protocol.acl\":\"hadoop\",\"security.refresh.usertogroups.mappings.protocol.acl\":\"hadoop\"}",
            "description": "",
            "filename": null,
            "timestamp": 1479709233510,
            "configurationMap": {
              "security.admin.operations.protocol.acl": "hadoop",
              "security.client.datanode.protocol.acl": "*",
              "security.client.protocol.acl": "*",
              "security.datanode.protocol.acl": "*",
              "security.inter.datanode.protocol.acl": "*",
              "security.inter.tracker.protocol.acl": "*",
              "security.job.client.protocol.acl": "*",
              "security.job.task.protocol.acl": "*",
              "security.namenode.protocol.acl": "*",
              "security.refresh.policy.protocol.acl": "hadoop",
              "security.refresh.usertogroups.mappings.protocol.acl": "hadoop"
            }
          },
          {
            "id": 19,
            "serviceId": 7,
            "name": "core-site",
            "configuration": "{\"fs.defaultFS\":\"hdfs://sandbox.hortonworks.com:8020\",\"fs.trash.interval\":\"360\",\"ha.failover-controller.active-standby-elector.zk.op.retries\":\"120\",\"hadoop.http.authentication.simple.anonymous.allowed\":\"true\",\"hadoop.proxyuser.falcon.groups\":\"*\",\"hadoop.proxyuser.falcon.hosts\":\"*\",\"hadoop.proxyuser.hbase.groups\":\"*\",\"hadoop.proxyuser.hbase.hosts\":\"*\",\"hadoop.proxyuser.hcat.groups\":\"*\",\"hadoop.proxyuser.hcat.hosts\":\"sandbox.hortonworks.com\",\"hadoop.proxyuser.hdfs.groups\":\"*\",\"hadoop.proxyuser.hdfs.hosts\":\"*\",\"hadoop.proxyuser.hive.groups\":\"*\",\"hadoop.proxyuser.hive.hosts\":\"sandbox.hortonworks.com\",\"hadoop.proxyuser.hue.groups\":\"*\",\"hadoop.proxyuser.hue.hosts\":\"*\",\"hadoop.proxyuser.livy.groups\":\"*\",\"hadoop.proxyuser.livy.hosts\":\"*\",\"hadoop.proxyuser.oozie.groups\":\"*\",\"hadoop.proxyuser.oozie.hosts\":\"sandbox.hortonworks.com\",\"hadoop.proxyuser.root.groups\":\"*\",\"hadoop.proxyuser.root.hosts\":\"sandbox.hortonworks.com\",\"hadoop.security.auth_to_local\":\"DEFAULT\",\"hadoop.security.authentication\":\"simple\",\"hadoop.security.authorization\":\"false\",\"hadoop.security.key.provider.path\":\"\",\"io.compression.codec.lzo.class\":\"com.hadoop.compression.lzo.LzoCodec\",\"io.compression.codecs\":\"org.apache.hadoop.io.compress.GzipCodec,org.apache.hadoop.io.compress.DefaultCodec,org.apache.hadoop.io.compress.SnappyCodec,com.hadoop.compression.lzo.LzoCodec,com.hadoop.compression.lzo.LzopCodec\",\"io.file.buffer.size\":\"131072\",\"io.serializations\":\"org.apache.hadoop.io.serializer.WritableSerialization\",\"ipc.client.connect.max.retries\":\"50\",\"ipc.client.connection.maxidletime\":\"30000\",\"ipc.client.idlethreshold\":\"8000\",\"ipc.server.tcpnodelay\":\"true\",\"mapreduce.jobtracker.webinterface.trusted\":\"false\",\"net.topology.script.file.name\":\"/etc/hadoop/conf/topology_script.py\"}",
            "description": "",
            "filename": "core-site.xml",
            "timestamp": 1479709233510,
            "configurationMap": {
              "fs.defaultFS": "hdfs://sandbox.hortonworks.com:8020",
              "fs.trash.interval": "360",
              "ha.failover-controller.active-standby-elector.zk.op.retries": "120",
              "hadoop.http.authentication.simple.anonymous.allowed": "true",
              "hadoop.proxyuser.falcon.groups": "*",
              "hadoop.proxyuser.falcon.hosts": "*",
              "hadoop.proxyuser.hbase.groups": "*",
              "hadoop.proxyuser.hbase.hosts": "*",
              "hadoop.proxyuser.hcat.groups": "*",
              "hadoop.proxyuser.hcat.hosts": "sandbox.hortonworks.com",
              "hadoop.proxyuser.hdfs.groups": "*",
              "hadoop.proxyuser.hdfs.hosts": "*",
              "hadoop.proxyuser.hive.groups": "*",
              "hadoop.proxyuser.hive.hosts": "sandbox.hortonworks.com",
              "hadoop.proxyuser.hue.groups": "*",
              "hadoop.proxyuser.hue.hosts": "*",
              "hadoop.proxyuser.livy.groups": "*",
              "hadoop.proxyuser.livy.hosts": "*",
              "hadoop.proxyuser.oozie.groups": "*",
              "hadoop.proxyuser.oozie.hosts": "sandbox.hortonworks.com",
              "hadoop.proxyuser.root.groups": "*",
              "hadoop.proxyuser.root.hosts": "sandbox.hortonworks.com",
              "hadoop.security.auth_to_local": "DEFAULT",
              "hadoop.security.authentication": "simple",
              "hadoop.security.authorization": "false",
              "hadoop.security.key.provider.path": "",
              "io.compression.codec.lzo.class": "com.hadoop.compression.lzo.LzoCodec",
              "io.compression.codecs": "org.apache.hadoop.io.compress.GzipCodec,org.apache.hadoop.io.compress.DefaultCodec,org.apache.hadoop.io.compress.SnappyCodec,com.hadoop.compression.lzo.LzoCodec,com.hadoop.compression.lzo.LzopCodec",
              "io.file.buffer.size": "131072",
              "io.serializations": "org.apache.hadoop.io.serializer.WritableSerialization",
              "ipc.client.connect.max.retries": "50",
              "ipc.client.connection.maxidletime": "30000",
              "ipc.client.idlethreshold": "8000",
              "ipc.server.tcpnodelay": "true",
              "mapreduce.jobtracker.webinterface.trusted": "false",
              "net.topology.script.file.name": "/etc/hadoop/conf/topology_script.py"
            }
          }
        ],
        "components": [
          {
            "id": 15,
            "serviceId": 7,
            "name": "DATANODE",
            "hosts": [
              "sandbox.hortonworks.com"
            ],
            "protocol": null,
            "port": 50075,
            "timestamp": 1479709233545
          },
          {
            "id": 21,
            "serviceId": 7,
            "name": "NFS_GATEWAY",
            "hosts": [
              "sandbox.hortonworks.com"
            ],
            "protocol": null,
            "port": null,
            "timestamp": 1479709233580
          },
          {
            "id": 23,
            "serviceId": 7,
            "name": "SECONDARY_NAMENODE",
            "hosts": [
              "sandbox.hortonworks.com"
            ],
            "protocol": null,
            "port": null,
            "timestamp": 1479709233589
          },
          {
            "id": 17,
            "serviceId": 7,
            "name": "HDFS_CLIENT",
            "hosts": [
              "sandbox.hortonworks.com"
            ],
            "protocol": null,
            "port": null,
            "timestamp": 1479709233554
          },
          {
            "id": 19,
            "serviceId": 7,
            "name": "NAMENODE",
            "hosts": [
              "sandbox.hortonworks.com"
            ],
            "protocol": null,
            "port": null,
            "timestamp": 1479709233571
          }
        ]
      },
      ...
    ]
  }
}
```

### Register Service manually

Streamline also provides bundle-registered service and relevant components manually.
User should specify existing cluster to make new service belong to.

`POST /api/v1/clusters/:clusterId/services/register/:serviceName`

`Content-Type` of the API is `multipart/form-data`, since user needs to upload config files for service.

It receives two types of parameters - `config` and `config files`:

* config: component information JSON, same structure of topology component bundle
* config files: service relevant configuration files to upload

Each service bundle defines its specification (config), similar to topology component bundle.
Unless the type of field is `file`, the value of field should be passed to config json.

The sample format for config:

```json
{
  "properties": {
    "uiServerHostname": "jlim-streamlin-3.openstacklocal", 
    "nimbusesHostnames": "jlim-streamlin-2.openstacklocal,jlim-streamlin-3.openstacklocal"
  }
}
```

If the specification defines the type of field as `file`, you need to provide the value from separate parameter.
The parameter name is same as field name on definition.

Here is the sample STORM service registration via curl:

```
curl -v -H "Content-Type: multipart/form-data" -X POST \
-F config='{"properties": {"uiServerHostname": "storm-1", "nimbusesHostnames": "storm-2,storm-3"}}' \
-F storm.yaml=@./storm.yaml http://localhost:8080/api/v1/catalog/clusters/1/services/register/STORM
```