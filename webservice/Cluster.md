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

`GET /api/v1/catalog/clusters/name/:clustername`

**Success Response**

    GET /api/v1/catalog/clusters/name/production
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

    GET /api/v1/catalog/clusters/name/production10
    HTTP/1.1 404 Not Found
    Content-Type: application/json
    
```json
{
  "responseCode": 1102,
  "responseMessage": "Entity with name [production10] not found. Please check webservice/ErrorCodes.md for more details."
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

`GET /api/v1/catalog/clusters/name/:clusterName/services`

### Get Service

`GET /api/v1/catalog/clusters/{clusterId}/services/{id}`

`GET /api/v1/catalog/clusters/name/{clusterName}/services/name/{serviceName}`

### Update Service

`PUT /api/v1/catalog/clusters/{clusterId}/services/{id}`

### Delete Service

`DELETE /api/v1/catalog/clusters/{clusterId}/services/{id}`

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

`GET /api/v1/catalog/clusters/name/:clusterName/services/name/:serviceName/configurations/:configurationName`

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

`GET /api/v1/catalog/clusters/name/:clusterName/services`

### Get Component

`GET /api/v1/catalog/clusters/{clusterId}/services/{id}`

`GET /api/v1/catalog/clusters/name/{clusterName}/services/name/{serviceName}`

### Update Component

`PUT /api/v1/catalog/services/{serviceId}/configurations/{id}`

### Delete Component

`DELETE /api/v1/catalog/services/{serviceId}/configurations/{id}`

