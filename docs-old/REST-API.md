# Streamline UI REST API

## Adding Service Pool

### /api/v1/clusters (GET All) configuration
 returns cluster configuration.  Below is a sample response but doesn't include all the config fileds.
 
 __Request Params__ : For more detail of clusters with metrics.

| Variable      | Type          | Default      |
| ------------- |:-------------:| ------------:|
| details       | Boolean       | true         |
 
 __Sample Response without params__:
 
```json
{
    "entities": [{
        "id": 1,
        "name": "cluster1",
        "ambariImportUrl": "http://172.22.120.135:8080/api/v1/clusters/cluster1",
        "description": "This is an auto generated description",
        "timestamp": 1485489085273
        }, {
        "id": 2,
        "name": "c1",
        "ambariImportUrl": "http://172.22.124.117:8080/api/v1/clusters/c1",
        "description": "This is an auto generated description",
        "timestamp": 1485489148299
        }]
}
```


__Response Fields__:

|Field  |Value|Description
|---	|---	|---
|id|Integer| cluster unique-id|
|name | String | cluter name|
|ambariImportUrl |String| valid url of ambari cluster|
|description:|String|cluster description|
|timestamp:|String|cluster created time|

### /api/v1/clusters/:id?detail=true (GET) 
  returns cluster information with metrics.
 
__Request Fields__:

|Parameter |Value   |Description  |
|----------|--------|-------------|
|id   	   |String (required)| cluster unique-Id  |
|details   |Boolean (required)| true  |

__Sample Response__:
  
```json
{
  "cluster": {
    "id": 1,
    "name": "cluster1",														"ambariImportUrl":"http://172.22.120.135:8080/api/v1/clusters/cluster1",
    "description": "This is an auto generated description",
    "timestamp": 1485489085273
  },
  "services": [{
    "service": {
    "id": 20,
    "clusterId": 1,
    "name": "STORM",
    "description": "",
    "timestamp": 1486450229424
    },
    "configurations": [],
    "components": []
  }]
}
```

__Response Fields__:

|Field  |Value |Description|
|---	|---	|---
|id   | String | cluster unique-id|
|name   | String | cluster name|
|ambariImportUrl   | String | valid Url of ambari cluster|
|description:|String|cluster description|
|timestamp   | String | Cluster created time|
|services   |Array of Object| Array of services with service, configuration and  component details|

### /api/v1/catalog/cluster/import/ambari/verify/url (POST)
verify the ambari url for the given cluster.

__Request Fields__ :

|Field  |Value |Description|
|---	|---	|---
|ambariRestApiRootUrl  | String |valid Url of ambari cluster|
|Username  | String | user name|
|Password  | String | password|

__Request JSON__:

``` json
{     		      "ambariRestApiRootUrl":"http://172.22.120.135:8080/api/v1/clusters/cluster1",
 "password":"admin",
 "username":"admin"
}
```

__Sample response__:

```json
{"verified":true}
```

__Response Fields__ :

|Field  |Value |Description|
|---	|---	|---
|verified  | Boolean | if true then, Given url is valid ambari cluster|

### api/v1/catalog/cluster/import/ambari (POST)
update the services of cluster to ambari

__Request Fields__ :

|Field  |Value |Description|
|---	|---	|---|
|Clusterid  | Integer | cluster unique-id|
|ambariRestApiRootUrl  | String |valid Url of ambari cluster|
|Username  | String | user name|
|Password  | String | password|

__Request JSON__:

``` json
{
  "clusterId": 1,    "ambariRestApiRootUrl":"http://172.22.120.135:8080/api/v1/clusters/cluster1",
  "password":"admin",
  "username":"admin"
}
```

__Sample response__:

``` json
{
	"cluster": {
	"id": 1,
	"name": "cluster1",
	"ambariImportUrl":"http://172.22.120.135:8080/api/v1/clusters/cluster1",
	"description": "This is an auto generated description",
	"timestamp": 1485489085273
	},
	"services": [{
		"service": {
			"id": 20,
			"clusterId": 1,
			"name": "STORM",
			"description": "",
			"timestamp": 1486450229424
		},
		"configurations": [],
		"components": []
	}]
}
```

__Response Fields__:

|Field  |Value |Description|
|---	|---	|---
|id   | String | cluster unique-id|
|name   | String | cluster name|
|ambariImportUrl   | String | valid Url of ambari cluster|
|description:|String|cluster description|
|timestamp   | String | Cluster created time|
|services   |Array of Object| Array of services with service, configuration and  component details|


### api/v1/catalog/clusters/:id (DELETE)
delete cluster from service pool

__Request Params__:

|Field  |Value |Description|
|---	|---	|---
|id   | Integer | cluster unique-id|

__Sample response__:

``` json
{
	"cluster": {
	"id": 1,
	"name": "cluster1",
	"ambariImportUrl":"http://172.22.120.135:8080/api/v1/clusters/cluster1",
	"description": "This is an auto generated description",
	"timestamp": 1485489085273
	},
	"services": [{
		"service": {
			"id": 20,
			"clusterId": 1,
			"name": "STORM",
			"description": "",
			"timestamp": 1486450229424
		},
		"configurations": [],
		"components": []
	}]
}

```
__Response Fields__:

|Field  |Value |Description|
|---	|---	|---
|id   | String | cluster unique-id|
|name   | String | cluster name|
|ambariImportUrl   | String | valid Url of ambari cluster|
|description:|String|cluster description|
|timestamp   | String | Cluster created time|
|services   |Array of Object| Array of services with service, configuration and  component details|




## Creating Environments
__We refer Environment as namespaces in this doc__


### /api/v1/namespaces (GET All) configuration
 returns namespaces configuration.  Below is a sample response but doesn't include all the config fileds.
 
 __Request Params__ : For more detail of namespaces with mapping.

| Variable      | Type          | Default      |
| ------------- |:-------------:| ------------:|
| details       | Boolean       | true         |
 
 __Sample Response without params__:  
 
```json
{
  "entities": [{
    "namespace": {
    "id": 1,
    "name": "double_env",
    "streamingEngine": "STORM",
    "timeSeriesDB": "AMBARI_METRICS",
    "description": "ddsf",
    "timestamp": 1486360316889
    }]
}
```

__Response Fields__:

|Field  |Value|Description
|---	|---	|---
|id|Integer| namespaces unique-id|
|name | String | namespaces name|
|StreamingEngine | String | namespaces run on this Stream Engine|
|timeSeriesDB |String| service name of supporting timeseries |
|description |String| description about the namespaces|
|timestamp:|String|namespaces created time|


### /api/v1/namespaces/:id?detail=true (GET) 
  returns namespaces information with mapping services.
 
__Request Params__:

|Parameter |Value   |Description  |
|----------|--------|-------------|
|id   	   |String (required)| namespaces Id  |
|details   |Boolean (required)| true  |

__Sample Response__: 
 
```json
{
  "entities": [{
    "namespace": {
    "id": 1,
    "name": "double_env",
    "streamingEngine": "STORM",
    "timeSeriesDB": "AMBARI_METRICS",
    "description": "ddsf",
    "timestamp": 1486360316889
    },
    "mappings": []
    }]
}
```

__Response Fields__:

|Field  |Value|Description
|---	|---	|---
|id|Integer| namespaces unique-id|
|name | String | namespaces name|
|streamingEngine | String | namespaces run on this Stream Engine|
|timeSeriesDB |String| service name of supporting timeseries|
|description |String| description about the namespaces|
|timestamp:|String|namespaces created time|
|mapping:|Array of Object|Array of services mapped to the namespaces|


### /api/v1/namespaces (POST) 
create the namespaces

__Request fields__:

|Parameter |Value   |Description  |
|----------|--------|-------------|
|name   |String | namespace name |
|streamingEngine | String | namespaces run on this Stream Engine|
|timeSeriesDB |String| service name of supporting timeseries |
|description |String| description about the namespaces|
|timestamp:|String|namespaces created time|

__Request JSON__ :

``` json
{
  "name":"test3",
  "streamingEngine":"STORM",
  "timeSeriesDB":"AMBARI_METRICS",
  "description":"werewr",
  "timestamp":1486462712630
}
```

__Sample response__:

``` json
{
  "id":2,
  "name":"test3",
  "streamingEngine":"STORM",
  "timeSeriesDB":"AMBARI_METRICS",
  "description":"werewr",
  "timestamp":1486462712630
}

```
__Response Fields__ :

|Fields |Value   |Description  |
|----------|--------|-------------|
|id        |Integer| namespaces unique-id|
|name   |String | namespace name |
|streamingEngine | String | namespaces run on this Stream Engine|
|timeSeriesDB |String| service name of supporting timeseries |
|description |String| description about the namespaces|
|timestamp:|String|namespaces created time|



### /api/v1/catalog/namespaces/:id/mapping/bulk (PUT) 
update the namespace with mapped services

__Request fields__:

|Parameter |Value   |Description  |
|----------|--------|-------------|
|namespaceId       |Integer| namespaces unique-id|
|serviceName   |String | name of service |
|clusterId |Integer| cluster unique-id |

__Request JSON__:

``` json
{
	"entities": [{
		"namespaceId": 2,
		"serviceName": "HIVE",
		"clusterId": 2
	}, {
		"namespaceId": 2,
		"serviceName": "STORM",
		"clusterId": 2
	}, {
		"namespaceId": 2,
		"serviceName": "HDFS",
		"clusterId": 2
	}, {
		"namespaceId": 2,
		"serviceName": "HBASE",
		"clusterId": 2
	}, {
		"namespaceId": 2,
		"serviceName": "KAFKA",
		"clusterId": 2
	}, {
		"namespaceId": 2,
		"serviceName": "ZOOKEEPER",
		"clusterId": 2
	}, {
		"namespaceId": 2,
		"serviceName": "AMBARI_METRICS",
		"clusterId": 2
	}]
}
```

__Sample response__:

``` json
{
	"entities": [{
		"namespaceId": 2,
		"serviceName": "HIVE",
		"clusterId": 2
	}, {
		"namespaceId": 2,
		"serviceName": "STORM",
		"clusterId": 2
	}, {
		"namespaceId": 2,
		"serviceName": "HDFS",
		"clusterId": 2
	}, {
		"namespaceId": 2,
		"serviceName": "HBASE",
		"clusterId": 2
	}, {
		"namespaceId": 2,
		"serviceName": "KAFKA",
		"clusterId": 2
	}, {
		"namespaceId": 2,
		"serviceName": "ZOOKEEPER",
		"clusterId": 2
	}, {
		"namespaceId": 2,
		"serviceName": "AMBARI_METRICS",
		"clusterId": 2
	}]
}
```

__Response Fields__ :

|Fields |Value   |Description  |
|----------|--------|-------------|
|namespaceId       |Integer| namespaces unique-id|
|serviceName   |String | name of service |
|clusterId |Integer| cluster unique-id |


### api/v1/catalog/namespaces/:id (DELETE)
delete namespace from Environments


__Request Params__:

|Parameter  |Value |Description|
|---	|---	|---
|id   | Integer | namespaces's id|

__Sample response__:

``` json
{
	"id": 2,
	"name": "test3",
	"streamingEngine": "STORM",
	"timeSeriesDB": "AMBARI_METRICS",
	"description": "werewr",
	"timestamp": 1486462712630
}
```

__Response Fields__ :

|Fields |Value   |Description  |
|----------|--------|-------------|
|id        |Integer| namespaces unique-id|
|name   |String | namespace name |
|streamingEngine | String | namespaces run on this Stream Engine|
|timeSeriesDB |String| service name of supporting timeseries |
|description |String| description about the namespaces|
|timestamp:|String|namespaces created time|





## Creating Topology


### api/v1/catalog/topologies?detail=true &sort=last_updated&latencyTopN=3 (GET All) configuration
 returns topology configuration.  Below is a sample response include all the config fields.
 
 __Request Params__ : Topology with runtime include metric, latencyTopN.

| Variable      | Type          | Default      |
| ------------- |:-------------:| ------------:|
| details       | Boolean  | true         |
| sort       | String      | last_updated       |
| latencyTopN  | Integer    | 3         |
 
 __Sample Response with params__: 
  
```json
{
	"entities": [{
		"topology": {
			"id": 2,
			"versionId": 5,
			"name": "test1-clone",
			"description": null,
			"namespaceId": 1,
			"config": {}
			},
			"timestamp": 1482919724562
		},
		"running": "RUNNING",
		"namespaceName": "demo",
		"runtime": {
			"runtimeTopologyId": "streamline-2-test2-25-1483435990",
			"metric": {}
			},
			"latencyTopN": []
		}
	}]
}
```

__Response Fields__:

|Field  |Value|Description
|---	|---	|---
|id|Integer| topology unique-id|
|name | String | topology name|
|versionId | Integer | topology version id |
|description | String | topology description|
|namespaceId | Integer | topology associated with namespace unique-id |
|config | Object | topology configuration |
|timestamp | String | topology created time |
|running | String | topology status |
|namespaceName | String | namespace name |
|runtime | Object | runtime object contain metric and latencyTopN |



### api/v1/catalog/topologies/:id?detail=true &sort=last_updated&latencyTopN=3 (GET) 
  returns topology information with runtime include metric, latencyTopN.
 
 __Request Params__ :

| Variable      | Type          | Default      |
| ------------- |:-------------:| ------------:|
| id       | Integer  | topology unique-id         |
| details       | Boolean  | true         |
| sort       | String      | last_updated       |
| latencyTopN  | Integer    | 3         |

__Sample Response__:  
```json
{
	"entity": [{
		"topology": {
			"id": 2,
			"versionId": 5,
			"name": "test1-clone",
			"description": null,
			"namespaceId": 1,
			"config": {}
			},
			"timestamp": 1482919724562
		},
		"running": "RUNNING",
		"namespaceName": "demo",
		"runtime": {
			"runtimeTopologyId": "streamline-2-test2-25-1483435990",
			"metric": {}
			},
			"latencyTopN": []
		}
	}]
}
```

__Response Fields__:

|Field  |Value|Description
|---	|---	|---
|id|Integer| topology unique-id|
|name | String | topology name|
|versionId | Integer | topology version id |
|description | String | topology description|
|namespaceId | Integer | topology associated with namespace unique-id |
|config | Object | topology configuration |
|timestamp | String | topology created time |
|running | String | topology status |
|namespaceName | String | namespace name |
|runtime | Object | runtime object contain metric and latencyTopN |


### api/v1/catalog/topologies/:id (PUT) 
update topology

__Request fields__:

|Parameter |Value   |Description  |
|----------|--------|-------------|
|id|Integer| topology unique-id|
|config | Object | topology configuration |
|namespaceId|Integer| namespace unique-id|

__Request json__:

``` json
{
	"id": 23,
	"versionId": 23,
	"name": "ertyty",
	"description": null,
	"namespaceId": 1,
	"config": "{topology.workers": 1,"topology.acker.executors": 1,	"topology.message.timeout.secs": 30,"hbaseConf": {"hbase.rootdir": "hdfs://localhost:9000/tmp/hbase"}"
	},
	"timestamp": 1486470588724
}

```

__Response Fields__ :

|Field  |Value|Description
|---	|---	|---
|id|Integer| topology unique-id|
|name | String | topology name|
|versionId | Integer | topology version id |
|description | String | topology description|
|namespaceId | Integer | topology associated with namespace unique-id |
|config | Object | topology configuration |
|timestamp | String | topology created time |


### api/v1/catalog/topologies/:topologyId/actions/clone?namespaceId=id (POST) 
to clone the topology

__Request fields__:

|Parameter |Value   |Description  |
|----------|--------|-------------|
|topologyId|Integer| topology unique-id|
|namespaceId|Integer| namespace unique-id|


__Sample response__:

``` json
{
	"id": 29,
	"versionId": 29,
	"name": "er-clone",
	"description": null,
	"namespaceId": 1,
	"config": {},
	"timestamp": 1486468599903
}
```
__Response Fields__ :

|Field  |Value|Description
|---	|---	|---
|id|Integer| topology unique-id|
|name | String | topology name|
|versionId | Integer | topology version id |
|description | String | topology description|
|namespaceId | Integer | topology associated with namespace unique-id |
|config | Object | topology configuration |
|timestamp | String | topology created time |


### /api/v1/catalog/topologies/actions/import (POST) 
import topology through json file

__Request Form-data__:

|Parameter |Value   |Description  |
|----------|--------|-------------|
|__Form-Data__       | |
|name    | String| topology name|
|file-name | file| topology json file|
| namespaceId | Integer | namespace unique-id|

__Sample response__:

``` json
{
	"id": 30,
	"versionId": 30,
	"name": "sam",
	"description": null,
	"namespaceId": 1,
	"config": {},
	"timestamp": 1486469448996
}
```

|Field  |Value|Description
|---	|---	|---
|id|Integer| topology unique-id|
|name | String | topology name|
|versionId | Integer | topology version id |
|description | String | topology description|
|namespaceId | Integer | topology associated with namespace unique-id |
|config | Object | topology configuration |
|timestamp | String | topology created time |


### /api/v1/catalog/topologies/:id/actions/export (GET) 

__Request fields__:

|Parameter |Value   |Description  |
|----------|--------|-------------|
|id|Integer| topology unique-id|



### api/v1/catalog/topologies/:id (DELETE)
delete topology from Applications


__Request Params__:

|Parameter  |Value |Description|
|---	|---	|---
|id   | Integer | topology unique-id|

__Sample response__:

``` json
{
	"id": 24,
	"versionId": 24,
	"name": "rt",
	"description": null,
	"namespaceId": 1,
	"config": {},
	"timestamp": 1486461277672
}
```

__Response Fields__ :

|Field  |Value|Description
|---	|---	|---
|id|Integer| topology unique-id|
|name | String | topology name|
|versionId | Integer | topology version id |
|description | String | topology description|
|namespaceId | Integer | topology associated with namespace unique-id |
|config | Object | topology configuration |
|timestamp | String | topology created time |

## Adding topology metadata

### /api/v1/catalog/system/topologyeditormetadata (POST)
Create a meta information entity.

__Request fields__:

|Field  |Value |Description|
|---  |---  |---
| topologyId | Integer | topology id|
| data | Object | components metadata|
| sources | Array of Objects | graph co-ordinates of all sources in topology |
| processors | Array of Objects | graph co-ordinates of all processors in topology |
| sinks | Array of Objects | graph co-ordinates of all sinks in topology |
| graphTransforms | Object | zooming and position of graph |
| dragCoords | Array | position of graph on drag|
| zoomScale | Integer | zoom in/zoom out scale |
| customNames | Array of Objects | configuration name for all custom processors in the topology |

__Request JSON__ :

``` json
{
  "topologyId":1,
    "data":"{\"sources\":[{\"x\":187.5,\"y\":145,\"id\":1}],\"processors\":[{\"x\":523.75,\"y\":71.25,\"id\":2}],\"sinks\":[],\"graphTransforms\":{\"dragCoords\":[27,-2],\"zoomScale\":0.8},\"customNames\":[{\"uiname\":\"custom\",\"customProcessorName\":\"ConsoleCustomProcessor\"}]}",
}
```

__Sample response__:

``` json
{
   "topologyId":1,
    "versionId":1,
    "data":"{\"sources\":[{\"x\":187.5,\"y\":145,\"id\":1}],\"processors\":[{\"x\":523.75,\"y\":71.25,\"id\":2}],\"sinks\":[],\"graphTransforms\":{\"dragCoords\":[27,-2],\"zoomScale\":0.8},\"customNames\":[{\"uiname\":\"custom\",\"customProcessorName\":\"ConsoleCustomProcessor\"}]}",
   "timestamp":1486534906974
}
```

__Response Fields__ :

|Field  |Value |Description|
|---  |---  |---
| topologyId | Integer | topology id|
| data | Object | components metadata|
| sources | Array of Objects | graph co-ordinates of all sources in topology |
| processors | Array of Objects | graph co-ordinates of all processors in topology |
| sinks | Array of Objects | graph co-ordinates of all sinks in topology |
| graphTransforms | Object | zooming and position of graph |
| dragCoords | Array | position of graph on drag|
| zoomScale | Integer | zoom in/zoom out scale |
| customNames | Array of Objects | configuration name for all custom processors in the topology |
| timestamp | String |  time when metadata was added|

### /api/v1/catalog/system/topologyeditormetadata/:id (PUT)
Update meta information for topology.

__Request params__:

|Field|Value|Description|
|---  |---  |---
|id |Integer|topology id|

__Request fields__:

|Field  |Value |Description|
|---  |---  |---
| topologyId | Integer | topology id|
| data | Object | components metadata|
| sources | Array of Objects | graph co-ordinates of all sources in topology |
| processors | Array of Objects | graph co-ordinates of all processors in topology |
| sinks | Array of Objects | graph co-ordinates of all sinks in topology |
| graphTransforms | Object | zooming and position of graph |
| dragCoords | Array | position of graph on drag|
| zoomScale | Integer | zoom in/zoom out scale |
| customNames | Array of Objects | configuration name for all custom processors in the topology |

__Request JSON__ :

```
{
  "topologyId":"1",
  "data":"{\"sources\":[{\"x\":187.5,\"y\":145,\"id\":1}],\"processors\":[{\"x\":523.75,\"y\":71.25,\"id\":2}}],\"sinks\":[],\"graphTransforms\":{\"dragCoords\":[27,-2],\"zoomScale\":0.8},\"customNames\":[{\"uiname\":\"custom\",\"customProcessorName\":\"ConsoleCustomProcessor\"}]}"
}
```
__Sample response__:

``` json
{
  "topologyId":1,
    "versionId":1,
    "data":"{\"sources\":[{\"x\":187.5,\"y\":145,\"id\":1}],\"processors\":[{\"x\":523.75,\"y\":71.25,\"id\":2}],\"sinks\":[],\"graphTransforms\":{\"dragCoords\":[27,-2],\"zoomScale\":0.8},\"customNames\":[{\"uiname\":\"custom\",\"customProcessorName\":\"ConsoleCustomProcessor\"}]}",
    "timestamp":1486643327986
}
```

__Response Fields__ :

|Field  |Value |Description|
|---  |---  |---
| topologyId | Integer | topology id|
| data | Object | components metadata|
| sources | Array of Objects | graph co-ordinates of all sources in topology |
| processors | Array of Objects | graph co-ordinates of all processors in topology |
| sinks | Array of Objects | graph co-ordinates of all sinks in topology |
| graphTransforms | Object | zooming and position of graph |
| dragCoords | Array | position of graph on drag|
| zoomScale | Integer | zoom in/zoom out scale |
| customNames | Array of Objects | configuration name for all custom processors in the topology  |
| timestamp | String |  time when metadata was updated|


## Adding a source


### /api/v1/catalog/topologies/:id/sources (POST)
Adds a source component.

__Request params__:

|Field|Value|Description|
|---  |---  |---
|id |Integer|topology id|

__Request fields__:

|Field  |Value |Description|
|---  |---  |---
|name  | String | source UI name|
|config | Object | configuration data|
|topologyComponentBundleId | Integer | source type |
|outputStreamIds| Array of Integers | source component's output stream id |

__Request JSON__ :

```json
{
    "name": "KAFKA",
    "config": {
        "properties": {
          "cluster": "1",
          "clusters": "cluster1",
          "consumerGroupId": "topology-id-26",
            "topic": "topic1",
            "zkRoot": "/topologyId",
            "zkUrl": "localhost:2181"
        }
    },
    "topologyComponentBundleId":1,
    "outputStreamIds": [1]
}
```

__Sample response__:

```json
{
   "id":1,
   "topologyId":1,
   "topologyComponentBundleId":1,
   "versionId":1,
   "name":"KAFKA",
   "description":"",
   "config":{
    "properties":{
          "cluster": "1",
          "clusters": "cluster1",
          "consumerGroupId": "topology-id-26",
            "topic": "topic1",
            "zkRoot": "/topologyId",
            "zkUrl": "localhost:2181"
        }
    },
   "outputStreams":[
    {"id":1,
    "versionId":1,
    "streamId":"kafka_stream_1",
    "description":null,
    "topologyId":1,
    "fields":[{"name":"driverId","type":"INTEGER","optional":false},      {"name":"truckId","type":"INTEGER","optional":false},{"name":"eventTime","type":"STRING","optional":false},{"name":"eventType","type":"STRING","optional":false}],
    "timestamp":1486543209874
   }
   ],
   "timestamp":1486542959113
}
```

__Response Fields__ :

|Field  |Value |Description|
|---  |---  |---
| id |Integer  | unique source id |
| topologyId | Integer | topology id |
| topologyComponentBundleId | Integer | source type |
| versionId | Integer | current topology version id |
| name  | String | source UI name |
| config  | Object | configuration data|
| outputStreams| Array of Objects | source component's output stream data |
| timestamp | Integer | time when source was created |

### /api/v1/catalog/topologies/:topologyId/versions/versionId/sources (GET)
 Returns all sources in the topology.

__Request params__:

|Field|Value|Description|
|---  |---  |---
|topologyId |Integer|topology id|
|versionId  |Integer|current version of topology|

__Sample response__:

```json
{"entities":[
  {"id":1,
  "topologyId":1,
  "topologyComponentBundleId":1,
  "versionId":1,
  "name":"KAFKA",
  "description":"",
  "config":{
      "properties":{
          "cluster":"1",
      "consumerGroupId":"topology-id-26",
      "zkPort":2181,
            "zkPath":"/brokers",
      "zkUrl":"localhost:2181",
      "topic":"topic1",
      "clusters":"c1"
            }
    },
  "outputStreams":[{"id":1,
    "versionId":1,
    "streamId":"kafka_stream_1",
    "description":null,
    "topologyId":1
        "fields":[{"name":"driverId","type":"INTEGER","optional":false},{"name":"truckId","type":"INTEGER","optional":false},{"name":"eventTime","type":"STRING","optional":false},{"name":"eventType","type":"STRING","optional":false}]}]
  }
]
}
```

__Response Fields__ :

|Field  |Value |Description|
|---  |---  |---
| id |Integer  | unique source id |
| topologyId | Integer | topology id |
| topologyComponentBundleId | Integer | source type |
| versionId | Integer | current topology version id |
| name  | String | source UI name |
| config  | Object | configuration data|
| outputStreams| Array of Objects | source component's output stream data |

### /api/v1/catalog/topologies/:topologyId/sources/:id (PUT)
Update the source component.

__Request params__:

|Field|Value|Description|
|---  |---  |---
|topologyId |Integer|topology id|
|id |Integer|source component id|

__Request fields__:

|Field  |Value |Description|
|---  |---  |---
|name  | String | source UI name|
|config  | Object | configuration data|
|topologyComponentBundleId | Integer | source type |
|outputStreams| Array of Objects | source component's output stream data |

__Request JSON__ :

```json
{
    "name": "Kafka",
    "config": {
        "properties": {
          "cluster": "1",
          "clusters": "cluster1",
          "consumerGroupId": "topology-id-26",
            "topic": "topic1",
            "zkRoot": "/topologyId",
            "zkUrl": "localhost:2181"
        }
    },
    "topologyComponentBundleId":1,
    "outputStreams":[{"id":1,
    "versionId":1,
    "streamId":"kafka_stream_1",
    "description":null,
    "topologyId":1
        "fields":[{"name":"driverId","type":"INTEGER","optional":false},{"name":"truckId","type":"INTEGER","optional":false},{"name":"eventTime","type":"STRING","optional":false},{"name":"eventType","type":"STRING","optional":false}]}]
    }]
}
```

__Sample response__:

```json
{"entities":[
  {"id":1,
  "topologyId":1,
  "topologyComponentBundleId":1,
  "versionId":1,
  "name":"Kafka",
  "description":"",
  "config":{
      "properties":{
        "cluster":"1",
        "consumerGroupId":"topology-id-26",
        "zkPort":2181,
        "zkPath":"/brokers",
        "zkUrl":"localhost:2181",
        "topic":"topic1",
        "clusters":"c1"
      }
    },
  "outputStreams":[{"id":1,
    "versionId":1,
    "streamId":"kafka_stream_1",
    "description":null,
    "topologyId":1
        "fields":[{"name":"driverId","type":"INTEGER","optional":false},{"name":"truckId","type":"INTEGER","optional":false},{"name":"eventTime","type":"STRING","optional":false},{"name":"eventType","type":"STRING","optional":false}]}]
  }
]
}
```

__Response Fields__ :

|Field  |Value |Description|
|---  |---  |---
| id |Integer  | unique source id |
| topologyId | Integer | topology id |
| topologyComponentBundleId | Integer | source type |
| versionId | Integer | current topology version id |
| name  | String | source name |
| config  | Object | configuration data|
| outputStreams| Array of Objects | source component's output stream data |


### /api/v1/catalog/topologies/:topologyId/sources/:id?removeEdges=true (DELETE)

Removes a source component.
__Request params__:

|Field|Value|Description|
|---  |---  |---
|topologyId | Integer |topology id|
|id | Integer | source component id|
|removeEdges | Boolean | removes all outgoing edges when true |

__Sample response__:

```json
{
 "id":1,
   "topologyId":1,
   "topologyComponentBundleId":1,
   "versionId":1,
   "name":"KAFKA",
   "description":"",
   "config":{
    "properties":{
          "cluster": "1",
          "clusters": "cluster1",
          "consumerGroupId": "topology-id-26",
            "topic": "topic1",
            "zkRoot": "/topologyId",
            "zkUrl": "localhost:2181"
        }
    },
   "outputStreams":[
    {"id":1,
    "versionId":1,
    "streamId":"kafka_stream_1",
    "description":null,
    "topologyId":1,
    "fields":[{"name":"driverId","type":"INTEGER","optional":false},      {"name":"truckId","type":"INTEGER","optional":false},{"name":"eventTime","type":"STRING","optional":false},{"name":"eventType","type":"STRING","optional":false}],
"timestamp":1486555689947}
],
"timestamp":1486555690088
}
```

__Response Fields__ :

|Field  |Value |Description|
|---  |---  |---
| id |Integer  | unique source id |
| topologyId | Integer | topology id |
| topologyComponentBundleId | Integer | source type |
| versionId | Integer | current topology version id |
| name  | String | source name |
| config  | Object | configuration data|
| outputStreams| Array of Objects | source component's output stream data |
| timestamp | Integer | time when source data was deleted |


## Adding a processor

### /api/v1/catalog/topologies/:id/processors (POST)
Adds a processor component.

__Request params__:

|Field|Value|Description|
|---  |---  |---
|id |Integer|topology id|

__Request fields__:

|Field  |Value |Description|
|---  |---  |---
|name  | String | processor name|
|config | Object | configuration data|
|topologyComponentBundleId | Integer | processor type |
|outputStreamIds| Array of Integers | processor's output stream id |

__Request JSON__ :

```json
{
    "name": "Rule",
    "config": {"properties": {"rules": [1]}},
    "outputStreamIds": [2],
    "topologyComponentBundleId": 3,
}
```

__Sample response__:

```json
{
  "id":1,
  "topologyId":1,
  "topologyComponentBundleId":3,
  "versionId":1,
  "name":"RULE",
  "description":"",
  "config":{
    "properties":{
      "rules":[1]
        }
  },
  "outputStreams":[
  {
      "id":2,
      "versionId":1,
    "streamId":"rule_transform_stream_1",
    "description":null,
    "topologyId":1,
    "fields":[{"name":"driverId","type":"INTEGER","optional":false},{"name":"truckId","type":"INTEGER","optional":false},{"name":"eventTime","type":"STRING","optional":false},{"name":"eventType","type":"STRING","optional":false}],
    "timestamp":1486546263445
      }
  ],
  "timestamp":1486546263475
}
```

__Response Fields__ :

|Field  |Value |Description|
|---  |---  |---
| id |Integer  | unique processor id |
| topologyId | Integer | topology id |
| topologyComponentBundleId | Integer | processor type |
| versionId | Integer | current topology version id |
| name  | String | processor name |
| config  | Object | configuration data|
| outputStreams| Array of Objects | processor's output stream data |
| timestamp | Integer | time when processor was created |

### /api/v1/catalog/topologies/:topologyId/versions/versionId/processors (GET)
 returns all processors in the topology.

 __Request params__:
 
|Field|Value|Description|
|---  |---  |---
|topologyId |Integer|topology id|
|versionId  |Integer|current version of topology|

__Sample response__:

```json
{"entities":[
  {
     "id":1,
    "topologyId":1,
    "topologyComponentBundleId":3,
    "versionId":1,
    "name":"RULE",
    "description":"",
    "config":{
      "properties":{
          "rules":[1]
        }
    },
    "outputStreams":[
    {
      "id":2,
      "versionId":1,
    "streamId":"rule_transform_stream_1",
    "description":null,
    "topologyId":1,
    "fields":[{"name":"driverId","type":"INTEGER","optional":false},{"name":"truckId","type":"INTEGER","optional":false},{"name":"eventTime","type":"STRING","optional":false},{"name":"eventType","type":"STRING","optional":false}]
      }
    ]
    }
]
}
```

__Response Fields__ :

|Field  |Value |Description|
|---  |---  |---
| id |Integer  | unique processor id |
| topologyId | Integer | topology id |
| topologyComponentBundleId | Integer | processor type |
| versionId | Integer | current topology version id |
| name  | String | processor name |
| config  | Object | configuration data|
| outputStreams| Array of Objects | processor's output stream data |

### /api/v1/catalog/topologies/:topologyId/processors/:id (PUT)
Update the processor.

__Request params__:

|Field|Value|Description|
|---  |---  |---
|topologyId |Integer|topology id|
|id |Integer|processor component id|

__Request fields__:

|Field  |Value |Description|
|---  |---  |---
|name  | String | processor name|
|config | Object | configuration data|
|topologyComponentBundleId | Integer | processor type |
|outputStreams| Array of Objects | processor's output stream data |

__Request JSON__ :

```json
{
    "name": "Rule",
    "config": {"properties": {"rules": [1]}},
    "outputStreams":[
      {
        "id":2,
        "versionId":1,
      "streamId":"rule_transform_stream_1",
      "description":null,
      "topologyId":1,
      "fields":[{"name":"driverId","type":"INTEGER","optional":false},{"name":"truckId","type":"INTEGER","optional":false},{"name":"eventTime","type":"STRING","optional":false},{"name":"eventType","type":"STRING","optional":false}]
        }
    ]
    "topologyComponentBundleId": 3,
}
```

__Sample response__:

```json
{
  "id":1,
  "topologyId":1,
  "topologyComponentBundleId":3,
  "versionId":1,
  "name":"RULE",
  "description":"",
  "config":{
    "properties":{
      "rules":[1]
        }
  },
  "outputStreams":[
    {
      "id":2,
      "versionId":1,
      "streamId":"rule_transform_stream_1",
      "description":null,
      "topologyId":1,
      "fields":[{"name":"driverId","type":"INTEGER","optional":false},{"name":"truckId","type":"INTEGER","optional":false},{"name":"eventTime","type":"STRING","optional":false},{"name":"eventType","type":"STRING","optional":false}
      ],
      "timestamp":1486546263445
      }
  ],
  "timestamp":1486546263475
}
```

__Response Fields__ :

|Field  |Value |Description|
|---  |---  |---
| id |Integer  | unique processor id |
| topologyId | Integer | topology id |
| topologyComponentBundleId | Integer | processor type |
| versionId | Integer | current version id |
| name  | String | processor name |
| config  | Object | configuration data|
| outputStreams| Array of Objects | processor's output stream data |
| timestamp | Integer | time when processor was updated |

### /api/v1/catalog/topologies/:topologyId/processors/:id?removeEdges=true (DELETE)
Removes a processor component.

__Request params__:

|Field|Value|Description|
|---  |---  |---
|topologyId | Integer|topology id|
|id | Integer |processor component id|
|removeEdges | Boolean |removes all outgoing edges when true|

__Sample response__:

```json
{
  "id":1,
  "topologyId":1,
  "topologyComponentBundleId":3,
  "versionId":1,
  "name":"RULE",
  "description":"",
  "config":{
    "properties":{
      "rules":[1]
        }
  },
  "outputStreams":[
  {
      "id":2,
      "versionId":1,
    "streamId":"rule_transform_stream_1",
    "description":null,
    "topologyId":1,
    "fields":[{"name":"driverId","type":"INTEGER","optional":false},{"name":"truckId","type":"INTEGER","optional":false},{"name":"eventTime","type":"STRING","optional":false},{"name":"eventType","type":"STRING","optional":false}],
    "timestamp":1486546263445
      }
  ],
  "timestamp":1486546263475
}
```

__Response Fields__ :

|Field  |Value |Description|
|---  |---  |---
| id |Integer  | unique processor id |
| topologyId | Integer | topology id |
| topologyComponentBundleId | Integer | processor type |
| versionId | Integer | current version id |
| name  | String | processor name |
| config  | Object | configuration data|
| outputStreams| Array of Objects | processor's output stream data |
| timestamp | Integer | time when processor was deleted |


## Adding a sink

### api/v1/catalog/topologies/:id/sinks (POST)
Adds a sink component.

__Request params__:

|Field|Value|Description|
|---  |---  |---
|id |Integer|topology id|

__Request fields__:

|Field  |Value |Description|
|---  |---  |---
|name  | String | sink name|
|config | Object | configuration data|
|topologyComponentBundleId | Integer | sink type |

__Request JSON__ :

```json
{
  "name": "HDFS",
    "config": {
      "properties": {
          "prefix":"",
            "extension":".txt",
            "rotationPolicy":{
              "timeBasedRotation":{
                "rotationInterval":10,
                "rotationIntervalUnit":"SECONDS"
                }
            },
            "parallelism":1,
            "fsUrl":"hdfs://localhost:9000",
            "path":"/storm",
            "countPolicyValue":1,
            "outputFields":["driverId"]
      }
    },
    "topologyComponentBundleId": 8
}
```

__Sample response__:

```json
{
  "id":1,
  "topologyId":1,
  "topologyComponentBundleId":8,
  "versionId":1,
  "name":"HDFS",
  "description":"",
  "config":{
      "properties":{
          "prefix":"",
            "extension":".txt",
            "rotationPolicy":{
              "timeBasedRotation":{
                "rotationInterval":10,
                "rotationIntervalUnit":"SECONDS"
                }
            },
            "parallelism":1,
            "fsUrl":"hdfs://localhost:9000",
            "path":"/storm",
            "countPolicyValue":1,
            "outputFields":["driverId"]
        }
    },
  "timestamp":1486622322662
}
```

__Response Fields__ :

|Field  |Value |Description|
|---  |---  |---
| id |Integer  | unique sink id |
| topologyId | Integer | sink id |
| topologyComponentBundleId | Integer | sink type |
| versionId | Integer | current topology version id |
| name  | String | sink name |
| config  | Object | configuration data|
| timestamp | Integer | time when sink was created |

### /api/v1/catalog/topologies/:topologyId/versions/versionId/sinks (GET)
 returns all sinks in the topology.

 __Request params__:
 
|Field|Value|Description|
|---  |---  |---
|topologyId |Integer|topology id|
|versionId  |Integer|current version of topology|

__Sample response__:

```json
{
"entities": [
  {
      "id":1,
    "topologyId":1,
    "topologyComponentBundleId":8,
    "versionId":1,
    "name":"HDFS",
    "description":"",
    "config":{
      "properties":{
          "prefix":"",
            "extension":".txt",
            "rotationPolicy":{
              "timeBasedRotation":{
                "rotationInterval":10,
                "rotationIntervalUnit":"SECONDS"
                }
            },
            "parallelism":1,
            "fsUrl":"hdfs://localhost:9000",
            "path":"/storm",
            "countPolicyValue":1,
            "outputFields":["driverId"]
          }
      }
  }
]
}
```

__Response Fields__ :

|Field  |Value |Description|
|---  |---  |---
| id |Integer  | unique sink id |
| topologyId | Integer | sink id |
| topologyComponentBundleId | Integer | sink type |
| versionId | Integer | current version id |
| name  | String | sink name |
| config  | Object | configuration data|

### /api/v1/catalog/topologies/:topologyId/sinks/:id (PUT)
Update the sink.

__Request params__:

|Field|Value|Description|
|---  |---  |---
|topologyId |Integer|topology id|
|id |Integer|sink component id|

__Request fields__:

|Field  |Value |Description|
|---  |---  |---
|name  | String | sink name|
|config | Object | configuration data|
|topologyComponentBundleId | Integer | sink type |

__Request JSON__ :

```json
{
  "name": "HDFS",
    "config": {
      "properties": {
          "prefix":"",
            "extension":".txt",
            "rotationPolicy":{
              "timeBasedRotation":{
                "rotationInterval":10,
                "rotationIntervalUnit":"SECONDS"
                }
            },
            "parallelism":1,
            "fsUrl":"hdfs://localhost:9000",
            "path":"/storm",
            "countPolicyValue":1,
            "outputFields":["driverId"]
      }
    },
    "topologyComponentBundleId": 8
}
```

__Sample response__:

```json
{
  "id":1,
  "topologyId":1,
  "topologyComponentBundleId":8,
  "versionId":1,
  "name":"HDFS",
  "description":"",
  "config":{
      "properties":{
          "prefix":"",
            "extension":".txt",
            "rotationPolicy":{
              "timeBasedRotation":{
                "rotationInterval":10,
                "rotationIntervalUnit":"SECONDS"
                }
            },
            "parallelism":1,
            "fsUrl":"hdfs://localhost:9000",
            "path":"/storm",
            "countPolicyValue":1,
            "outputFields":["driverId"]
        }
    },
  "timestamp":1486622322662
}
```

__Response Fields__ :

|Field  |Value |Description|
|---  |---  |---
| id |Integer  | unique sink id |
| topologyId | Integer | sink id |
| topologyComponentBundleId | Integer | sink type |
| versionId | Integer | current version id |
| name  | String | sink name |
| config  | Object | configuration data|
| timestamp | Integer | time when sink was updated |


### /api/v1/catalog/topologies/:topologyId/sinks/:id?removeEdges=true (DELETE)
Removes a sink component.

__Request params__:

|Field|Value|Description|
|---  |---  |---
|topologyId | Integer| topology id|
|id |Integer| sink component id|
|removeEdges | Boolean | removes all outgoing edges when true |

__Sample response__:

```json
{
      "id":1,
    "topologyId":1,
    "topologyComponentBundleId":8,
    "versionId":1,
    "name":"HDFS",
    "description":"",
    "config":{
      "properties":{
          "prefix":"",
            "extension":".txt",
            "rotationPolicy":{
              "timeBasedRotation":{
                "rotationInterval":10,
                "rotationIntervalUnit":"SECONDS"
                }
            },
            "parallelism":1,
            "fsUrl":"hdfs://localhost:9000",
            "path":"/storm",
            "countPolicyValue":1,
            "outputFields":["driverId"]
          }
      },
    "timestamp":1486622322662
  }
```

__Response Fields__ :

|Field  |Value |Description|
|---  |---  |---
| id |Integer  | unique sink id |
| topologyId | Integer | sink id |
| topologyComponentBundleId | Integer | sink type |
| versionId | Integer | current version id |
| name  | String | sink name |
| config  | Object | configuration data|
| timestamp | Integer | time when sink was deleted |

## Adding a stream

### /api/v1/catalog/topologies/:id/streams (POST)
Creates a stream for the given schema.

__Request params__:

|Field|Value|Description|
|---  |---  |---
|id |Integer|topology id|

__Request fields__:

|Field|Value|Description|
|---  |---  |---
| streamId | String | stream name|
| fields | Array of objects | schema fields |

__Request JSON__ :

```json
{
  "streamId":"rule_transform_stream_1",
  "fields":[
   		 {"name":"driverName","type":"STRING","optional":true},
    	 {"name":"miles_SUM","type":"DOUBLE","optional":false}
    ]
}
```

__Sample response__:

```json
{
  "id":2,
  "versionId":1,
  "streamId":"rule_transform_stream_1",
  "description":null,
  "topologyId":1,
  "fields":[{"name":"driverName","type":"STRING","optional":true}, 
  			{"name":"miles_SUM","type":"DOUBLE","optional":false}
    ],
  "timestamp":1486551105061
}
```

__Response Fields__ :

|Field|Value|Description|
|---  |---  |---
| id | Integer | stream id |
| versionId | Integer | current topology version id |
| topologyId | Integer | topology id |
| streamId | String | stream name|
| fields | Array of objects | schema fields |
| timestamp | Integer | time when stream was created |

### /api/v1/catalog/topologies/:topologyId/versions/:versionId/streams (GET)

returns all streams in the topology.

__Request params__:

|Field|Value|Description|
|---  |---  |---
|topologyId |Integer|topology id|
|versionId |Integer|current topology version id|

__Sample response__:

```json
{
"entities":[
  {
    "id":1,
  "versionId":1,
  "streamId":"kafka_stream_1",
  "description":null,
  "topologyId":1,
  "fields":[
  		{"name":"driverId","type":"INTEGER","optional":false},
    	{"name":"truckId","type":"INTEGER","optional":false},
    	{"name":"eventTime","type":"STRING","optional":false},
    	{"name":"eventType","type":"STRING","optional":false}]
    },
    {
    "id":2,
  "versionId":1,
  "streamId":"rule_transform_stream_1",
  "description":null,
  "topologyId":1,
  "fields":[{"name":"driverName","type":"STRING","optional":true},  
            {"name":"miles_SUM","type":"DOUBLE","optional":false}]
  }
]
}
```

__Response Fields__ :

|Field|Value|Description|
|---  |---  |---
| id | Integer | stream id |
| versionId | Integer | current topology version id |
| topologyId | Integer | topology id |
| streamId | String | stream name|
| fields | Array of objects | schema fields |

### /api/v1/catalog/topologies/:topologyId/streams/:id (PUT)
Updates the stream of given id.

__Request params__:

|Field|Value|Description|
|---  |---  |---
|topologyId |Integer| topology id|
|id |Integer| stream id|

__Request fields__:

|Field|Value|Description|
|---  |---  |---
| streamId | String | stream name|
| fields | Array of objects | schema fields |

__Request JSON__ :

```json
{
  "streamId":"rule_transform_stream_1",
  "fields":[
		{"name":"driverName","type":"STRING","optional":true}, 
       {"name":"miles_SUM","type":"DOUBLE","optional":false}
    ]
}
```
__Sample response__:

```json
{
  "id":2,
  "versionId":1,
  "streamId":"rule_transform_stream_1",
  "description":null,
  "topologyId":1,
  "fields":[{"name":"driverName","type":"STRING","optional":true}, 
         {"name":"miles_SUM","type":"DOUBLE","optional":false}
    ],
  "timestamp":1486551105061
}
```
__Response Fields__ :

|Field|Value|Description|
|---  |---  |---
| id | Integer | stream id |
| versionId | Integer | current topology version id |
| topologyId | Integer | topology id |
| streamId | String | stream name|
| fields | Array of objects | schema fields |
| timestamp | Integer | time when stream was updated |

### /api/v1/catalog/topologies/:topologyId/streams/:id (DELETE)

Removes the stream from topology.

__Request params__:

|Field|Value|Description|
|---  |---  |---
|topologyId |Integer|topology id|
|id |Integer|stream id|

__Sample response__:

```json
{
  "id":2,
  "versionId":1,
  "streamId":"rule_transform_stream_1",
  "description":null,
  "topologyId":1,
  "fields":[{"name":"driverName","type":"STRING","optional":true},
              {"name":"miles_SUM","type":"DOUBLE","optional":false}],
  "timestamp":1486633488787
}
```
__Response Fields__ :

|Field|Value|Description|
|---  |---  |---
| id | Integer | stream id |
| versionId | Integer | current topology version id |
| topologyId | Integer | topology id |
| streamId | String | stream name|
| fields | Array of objects | schema fields |
| timestamp | Integer | time when stream was deleted |


## Adding an edge

### /api/v1/catalog/topologies/:id/edges (POST)
Creates an edge between two components.

__Request params__:

|Field|Value|Description|
|---  |---  |---
|id |Integer|topology id|

__Request fields__:

|Field|Value|Description|
|---  |---  |---
|fromId |Integer| component id from which edge is created|
|toId |Integer| target component id|
|streamGroupings |Array of Objects | output stream data selected for edge|

__Request JSON__ :

```json
{
  "fromId": 1,
  "toId": 2,
  "streamGroupings":[{"streamId":2,"grouping":"SHUFFLE"}]
}
```
__Sample response__:

```json
{
  "id":1,
  "versionId":1,
  "topologyId":1,
  "fromId":1,
  "toId":2,
  "streamGroupings":[
      {"streamId":2,
    "grouping":"SHUFFLE",
      "fields":null
        }],
  "timestamp":1486554729887
}
```
__Response Fields__ :

|Field|Value|Description|
|---  |---  |---
| id | Integer | edge id |
| versionId | Integer | current topology version id |
| topologyId | Integer | topology id |
| fromId |Integer| component id from which edge is created|
| toId |Integer| target component id|
| streamGroupings |Array of Objects | output stream data selected for edge|
| timestamp | Integer | time when edge was created |

### /api/v1/catalog/topologies/:id/edges (GET)

Returns all edges in topology.

__Request params__:

|Field|Value|Description|
|---  |---  |---
|id |Integer|topology id|

__Sample response__:

```json
{
"entities": [
  {
    "id":1,
  "versionId":1,
    "topologyId":1,
    "fromId":1,
    "toId":2,
    "streamGroupings":[
      {"streamId":2,
      "grouping":"SHUFFLE",
        "fields":null
      }]
    },
    {
    "id":2,
  "versionId":1,
    "topologyId":1,
    "fromId":2,
    "toId":3,
    "streamGroupings":[
      {"streamId":3,
      "grouping":"SHUFFLE",
        "fields":null
      }]
    }
]
}
```

__Response Fields__ :

|Field|Value|Description|
|---  |---  |---
| id | Integer | edge id |
| versionId | Integer | current topology version id |
| topologyId | Integer | topology id |
| fromId |Integer| component id from which edge is created|
| toId |Integer| target component id|
| streamGroupings |Array of Objects | output stream data selected for edge|


### /api/v1/catalog/topologies/:topologyId/edges/:id (PUT)
Updates the edge with given id.

__Request params__:

|Field|Value|Description|
|---  |---  |---
|topologyId |Integer|topology id|
|id |Integer| edge id|

__Request fields__:

|Field|Value|Description|
|---  |---  |---
|fromId |Integer| component id from which edge is created|
|toId |Integer| target component id|
|streamGroupings |Array of Objects | output stream data selected for edge|

__Request JSON__ :

```json
{
  "fromId":1,
  "toId":2,
  "streamGroupings":[{"streamId":2,"grouping":"SHUFFLE"}]
}
```
__Sample response__:

```json
{
  "id":2,
  "versionId":1,
  "topologyId":1,
  "fromId":1,
  "toId":2,
  "streamGroupings":[{"streamId":2,"grouping":"SHUFFLE","fields":null}],
  "timestamp":1486638035786
}
```
__Response Fields__ :

|Field|Value|Description|
|---  |---  |---
| id | Integer | edge id |
| versionId | Integer | current topology version id |
| topologyId | Integer | topology id |
| fromId |Integer| component id from which edge is created|
| toId |Integer| target component id|
| streamGroupings |Array of Objects | output stream data selected for edge|
| timestamp | Integer | time when edge was updated |

### /api/v1/catalog/topologies/:topologyId/edges/:id (DELETE)
Removes edge from topology.

__Request params__:

|Field|Value|Description|
|---  |---  |---
|topologyId |Integer|topology id|
|id |Integer|edge id|

__Sample response__:

```json
{
"id":51,
"versionId":1,
"topologyId":1,
"fromId":3,
"toId":28,
"streamGroupings":[{
  "streamId":6,
  "grouping":"SHUFFLE",
  "fields":null
    }],
"timestamp":1486630988607
}
```
__Response Fields__ :

|Field|Value|Description|
|---  |---  |---
| id | Integer | edge id |
| versionId | Integer | current topology version id |
| topologyId | Integer | topology id |
| fromId |Integer| component id from which edge is created|
| toId |Integer| target component id|
| streamGroupings |Array of Objects | output stream data selected for edge|
| timestamp | Integer | time when edge was deleted |


## Adding rules

### /api/v1/catalog/topologies/:id/rules (POST)
Creates a rule for Rule processor.

__Request params__:

|Field|Value|Description|
|---  |---  |---
|id |Integer|topology id|

__Request fields__:

|Field|Value|Description|
|---  |---  |---
|name | String | rule name |
|description | String | description for rule |
|streams | Array | input stream name of fields in this rule |
|condition | String | logical operations on fields |
|outputStreams | Array | output streams associated with the rule |
|actions | Array | action added when rule processor has outgoing edges |

__Request JSON__ :

```json
{
  "name":"rule1",
  "description":"processor rule",
  "streams":["kafka_stream_1"],
  "condition":"eventTime < eventTime",
    "outputStreams":["rule_transform_stream_1"],
  "actions":[{
      "outputStreams":["rule_transform_stream_1"],
      "name":"transformAction", 
      "__type":"com.hortonworks.streamline.streams.layout.component.rule.action.TransformAction",
      "transforms":[]
    }]
}
```

__Sample response__:

```json
{
  "id":1,
  "versionId":1,
  "topologyId":1,
  "name":"rule1",
  "description":"dghhg",
  "streams":["kafka_stream_1"],
  "condition":"eventTime < eventTime",
  "sql":"SELECT *  FROM kafka_stream_1 WHERE eventTime < eventTime",
    "outputStreams":["rule_transform_stream_1"],
  "actions":[{
      "outputStreams":["rule_transform_stream_1"],
      "name":"transformAction", 
       "__type":"com.hortonworks.streamline.streams.layout.component.rule.action.TransformAction",
      "transforms":[]
    }],
  "timestamp":1486638795380
}
```

__Response Fields__ :

|Field|Value|Description|
|---  |---  |---
| id | Integer | edge id |
| versionId | Integer | current topology version id |
| topologyId | Integer | topology id |
| name | String | rule name |
| description | String | description for rule |
| streams | Array | input stream name of fields in this rule |
| condition | String | logical operations on fields |
| outputStreams | Array | output streams associated with the rule |
| actions | Array | action added when rule processor has outgoing edges |
| timestamp | Integer | time when rule was created |

### /api/v1/catalog/topologies/:id/rules (GET)

returns all rules in the topology.

__Request params__:

|Field|Value|Description|
|---  |---  |---
|id |Integer|topology id|

__Sample response__:

```json
{
"entities":[
  {
  "id":1,
  "versionId":1,
  "topologyId":1,
  "name":"rule1",
  "description":"dghhg",
  "streams":["kafka_stream_1"],
  "condition":"eventTime < eventTime",
  "sql":"SELECT *  FROM kafka_stream_1 WHERE eventTime < eventTime",
    "outputStreams":["rule_transform_stream_1"],
  "actions":[{
      "outputStreams":["rule_transform_stream_1"],
      "name":"transformAction",     "__type":"com.hortonworks.streamline.streams.layout.component.rule.action.TransformAction",
      "transforms":[]
    }]
}
]
}
```

__Response Fields__ :

|Field|Value|Description|
|---  |---  |---
| id | Integer | edge id |
| versionId | Integer | current topology version id |
| topologyId | Integer | topology id |
| name | String | rule name |
| description | String | description for rule |
| streams | Array | input stream name of fields in this rule |
| condition | String | logical operations on fields |
| outputStreams | Array | output streams associated with the rule |
| actions | Array | action added when rule processor has outgoing edges |

### /api/v1/catalog/topologies/:topologyId/rules/:id (PUT)
Updates the rule with given id.

__Request params__:

|Field|Value|Description|
|---  |---  |---
|topologyId |Integer|topology id|
|id |Integer| edge id|

__Request fields__:

|Field|Value|Description|
|---  |---  |---
|name | String | rule name |
|description | String | description for rule |
|streams | Array | input stream name of fields in this rule |
|condition | String | logical operations on fields |
|outputStreams | Array | output streams associated with the rule |
|actions | Array | action added when rule processor has outgoing edges |

__Request JSON__ :

```json
{
  "name":"rule1",
  "description":"processor rule",
  "streams":["kafka_stream_1"],
  "condition":"eventTime < eventTime",
  "outputStreams":["rule_transform_stream_1"],
  "actions":[{
      "outputStreams":["rule_transform_stream_1"],
      "name":"transformAction",
      "__type":"com.hortonworks.streamline.streams.layout.component.rule.action.TransformAction",
      "transforms":[]
    }]
}
```

__Sample response__:

```json
{
  "id":1,
  "versionId":1,
  "topologyId":1,
  "name":"rule1",
  "description":"dghhg",
  "streams":["kafka_stream_1"],
  "condition":"eventTime < eventTime",
  "sql":"SELECT *  FROM kafka_stream_1 WHERE eventTime < eventTime",
  "outputStreams":["rule_transform_stream_1"],
  "actions":[{
      "outputStreams":["rule_transform_stream_1"],
      "name":"transformAction",
      "__type":"com.hortonworks.streamline.streams.layout.component.rule.action.TransformAction",
      "transforms":[]
    }],
  "timestamp":1486638795380
}
```
__Response Fields__ :

|Field|Value|Description|
|---  |---  |---
| id | Integer | edge id |
| versionId | Integer | current topology version id |
| topologyId | Integer | topology id |
| name | String | rule name |
| description | String | description for rule |
| streams | Array | input stream name of fields in this rule |
| condition | String | logical operations on fields |
| outputStreams | Array | output streams associated with the rule |
| actions | Array | action added when rule processor has outgoing edges |
| timestamp | Integer | time when rule was updated |


### /api/v1/catalog/topologies/:topologyId/rules/:id (DELETE)
Removes rule from topology.

__Request params__:

|Field|Value|Description|
|---  |---  |---
|topologyId |Integer|topology id|
|id |Integer|rule id|

__Sample response__:

```json
{
  "id":1,
  "versionId":1,
  "topologyId":1,
  "name":"rule1",
  "description":"dghhg",
  "streams":["kafka_stream_1"],
  "condition":"eventTime < eventTime",
  "sql":"SELECT *  FROM kafka_stream_1 WHERE eventTime < eventTime",
    "outputStreams":["rule_transform_stream_1"],
  "actions":[{
      "outputStreams":["rule_transform_stream_1"],
      "name":"transformAction",
      "__type":"com.hortonworks.streamline.streams.layout.component.rule.action.TransformAction",
      "transforms":[]
    }],
  "timestamp":1486638795380
}
```
__Response Fields__ :

|Field|Value|Description|
|---  |---  |---
| id | Integer | edge id |
| versionId | Integer | current topology version id |
| topologyId | Integer | topology id |
| name | String | rule name |
| description | String | description for rule |
| streams | Array | input stream name of fields in this rule |
| condition | String | logical operations on fields |
| outputStreams | Array | output streams associated with the rule |
| actions | Array | action added when rule processor has outgoing edges |
| timestamp | Integer | time when rule was deleted |


### /api/v1/catalog/topologies/:id/branchrules (POST)
Creates a rule for Branch processor.

__Request params__:

|Field|Value|Description|
|---  |---  |---
|id |Integer|topology id|

__Request fields__:

|Field|Value|Description|
|---  |---  |---
|name | String | branch rule name |
|description | String | description for branch rule |
|streams | Array | input stream name of fields in this rule |
|condition | String | logical operations on fields |
|outputStreams | Array | output streams associated with the rule |
|actions | Array | action added when branch processor has outgoing edges |

__Request JSON__ :

```json
{
  "name":"r1",
  "description":"dfgfgh",
  "stream":"kafka_stream_1",
  "condition":"truckId <> truckId",
  "actions":[{
      "outputStreams":["branch_transform_stream_1"],
      "name":"transformAction",
      "__type":"com.hortonworks.streamline.streams.layout.component.rule.action.TransformAction",
      "transforms":[]
    }]
}
```
__Sample response__:

```json
{
  "id":1,
  "topologyId":1,
  "versionId":1,
  "name":"r1",
  "description":"dfgfgh",
  "stream":"kafka_stream_1",
  "condition":"truckId <> truckId",
  "actions":[{
      "outputStreams":["branch_transform_stream_1"],
      "name":"transformAction",
      "__type":"com.hortonworks.streamline.streams.layout.component.rule.action.TransformAction",
      "transforms":[]
    }],
  "timestamp":1486640516783
}
```
__Response Fields__ :

|Field|Value|Description|
|---  |---  |---
| id | Integer | edge id |
| versionId | Integer | current topology version id |
| topologyId | Integer | topology id |
| name | String | branch rule name |
| description | String | description for branch rule |
| streams | Array | input stream name of fields in this rule |
| condition | String | logical operations on fields |
| outputStreams | Array | output streams associated with the rule |
| actions | Array | action added when branch processor has outgoing edges |
| timestamp | Integer | time when rule was created |

### /api/v1/catalog/topologies/:id/branchrules (GET)

returns all branch rules in the topology.

__Request params__:

|Field|Value|Description|
|---  |---  |---
|id |Integer|topology id|

__Sample response__:

```json
{
"entities":[
  {
  "id":1,
  "topologyId":1,
  "versionId":1,
  "name":"r1",
  "description":"dfgfgh",
  "stream":"kafka_stream_1",
  "condition":"truckId <> truckId",
  "actions":[{
      "outputStreams":["branch_transform_stream_1"],
      "name":"transformAction",
      "__type":"com.hortonworks.streamline.streams.layout.component.rule.action.TransformAction",
      "transforms":[]
    }]
}
]
}
```
__Response Fields__ :

|Field|Value|Description|
|---  |---  |---
| id | Integer | edge id |
| versionId | Integer | current topology version id |
| topologyId | Integer | topology id |
| name | String | branch rule name |
| description | String | description for branch rule |
| streams | Array | input stream name of fields in this rule |
| condition | String | logical operations on fields |
| outputStreams | Array | output streams associated with the rule |
| actions | Array | action added when branch processor has outgoing edges |

### /api/v1/catalog/topologies/:topologyId/branchrules/:id (PUT)
Updates the branch rule with given id.

__Request params__:

|Field|Value|Description|
|---  |---  |---
|topologyId |Integer|topology id|
|id |Integer| edge id|

__Request fields__:

|Field|Value|Description|
|---  |---  |---
|name | String | branch rule name |
|description | String | description for branch rule |
|streams | Array | input stream name of fields in this rule |
|condition | String | logical operations on fields |
|outputStreams | Array | output streams associated with the rule |
|actions | Array | action added when branch processor has outgoing edges |

__Request JSON__ :

```json
{
  "name":"r1",
  "description":"dfgfgh",
  "stream":"kafka_stream_1",
  "condition":"truckId <> truckId",
  "actions":[{
      "outputStreams":["branch_transform_stream_1"],
      "name":"transformAction",     "__type":"com.hortonworks.streamline.streams.layout.component.rule.action.TransformAction",
      "transforms":[]
    }]
}
```
__Sample response__:

```json
{
  "id":1,
  "topologyId":1,
  "versionId":1,
  "name":"r1",
  "description":"dfgfgh",
  "stream":"kafka_stream_1",
  "condition":"truckId <> truckId",
  "actions":[{
      "outputStreams":["branch_transform_stream_1"],
      "name":"transformAction",
      "__type":"com.hortonworks.streamline.streams.layout.component.rule.action.TransformAction",
      "transforms":[]
    }],
  "timestamp":1486640516783
}
```
__Response Fields__ :

|Field|Value|Description|
|---  |---  |---
| id | Integer | edge id |
| versionId | Integer | current topology version id |
| topologyId | Integer | topology id |
| name | String | branch rule name |
| description | String | description for branch rule |
| streams | Array | input stream name of fields in this rule |
| condition | String | logical operations on fields |
| outputStreams | Array | output streams associated with the rule |
| actions | Array | action added when branch processor has outgoing edges |
| timestamp | Integer | time when rule was updated |


### /api/v1/catalog/topologies/:topologyId/branchrules/:id (DELETE)
Removes branch rule from topology.

__Request params__:

|Field|Value|Description|
|---  |---  |---
|topologyId |Integer|topology id|
|id |Integer|rule id|

__Sample response__:

```json
{
  "id":1,
  "topologyId":1,
  "versionId":1,
  "name":"r1",
  "description":"dfgfgh",
  "stream":"kafka_stream_1",
  "condition":"truckId <> truckId",
  "actions":[{
      "outputStreams":["branch_transform_stream_1"],
      "name":"transformAction",
      "__type":"com.hortonworks.streamline.streams.layout.component.rule.action.TransformAction",
      "transforms":[]
    }],
  "timestamp":1486640516783
}
```
__Response Fields__ :

|Field|Value|Description|
|---  |---  |---
| id | Integer | edge id |
| versionId | Integer | current topology version id |
| topologyId | Integer | topology id |
| name | String | branch rule name |
| description | String | description for branch rule |
| streams | Array | input stream name of fields in this rule |
| condition | String | logical operations on fields |
| outputStreams | Array | output streams associated with the rule |
| actions | Array | action added when branch processor has outgoing edges |
| timestamp | Integer | time when rule was deleted |


### /api/v1/catalog/topologies/:id/windows (POST)
Creates a window for Aggregate processor.

__Request params__:
|Field|Value|Description|
|---  |---  |---
|id |Integer|topology id|

__Request fields__:
|Field|Value|Description|
|---  |---  |---
| name | String | window name |
| description | String | description for window |
| streams | Array | input stream name of fields in this window |
| projections | Array of Objects | output fields and selected functions |
| groupbykeys | Array | selected fields from input stream |
| window | Object | window interval as time or count |
| outputStreams | Array | output streams associated with the window |
| actions | Array | action added when aggregate processor has outgoing edges |

__Request JSON__ :

```json
{
  "name":"window_auto_generated",
  "description":"window description auto generated",
  "projections":[
    {"args":["driverId"],
    "functionName":"MIN",
    "outputFieldName":"driverId_MIN"
    },
    {"expr":"driverId"},
    {"expr":"truckId"}
  ],
  "streams":["kafka_stream_1"],
  "groupbykeys":["driverId","truckId"],
  "window":{
      "windowLength":{"class":".Window$Duration",
      "durationMs":2000
            },
    "slidingInterval":{"class":".Window$Duration",
      "durationMs":2000}
            },
  "actions":[{
      "outputStreams":["window_transform_stream_1"],
      "name":"transformAction",
      "__type":"com.hortonworks.streamline.streams.layout.component.rule.action.TransformAction",
      "transforms":[]
    }],,
  "outputStreams":["window_transform_stream_1","window_notifier_stream_1"]
}
```
__Sample response__:

```json
{
  "id":1,
  "versionId":1,
  "topologyId":1,
  "name":"window_auto_generated",
  "description":"window description auto generated",
  "streams":["kafka_stream_1"],
  "window":{
      "windowLength":{"class":".Window$Duration","durationMs":2000},
    "slidingInterval":{"class":".Window$Duration","durationMs":2000},
      "tsField":null,
      "lagMs":0
    },
  "actions":[{
      "outputStreams":["window_transform_stream_1"],
      "name":"transformAction",
      "__type":"com.hortonworks.streamline.streams.layout.component.rule.action.TransformAction",
      "transforms":[]
    }],,
  "projections":[{"functionName":"MIN","args":["driverId"],
    "outputFieldName":"driverId_MIN"},
    {"expr":"driverId"},
    {"expr":"truckId"}
     ],
  "groupbykeys":["driverId","truckId"],
  "outputStreams":[
    "window_transform_stream_1","window_notifier_stream_1"],
  "timestamp":1486640823606
}
```
__Response Fields__ :

|Field|Value|Description|
|---  |---  |---
| id | Integer | edge id |
| versionId | Integer | current topology version id |
| topologyId | Integer | topology id |
| name | String | window name |
| description | String | description for window |
| streams | Array | input stream name of fields in this window |
| projections | Array of Objects | output fields and selected functions |
| groupbykeys | Array | selected fields from input stream |
| window | Object | window interval as time or count |
| outputStreams | Array | output streams associated with the window |
| actions | Array | action added when aggregate processor has outgoing edges |
| timestamp | Integer | time when window was created |

### /api/v1/catalog/topologies/:id/windows (GET)

returns all windows in the topology.

__Request params__:

|Field|Value|Description|
|---  |---  |---
|id |Integer|topology id|

__Sample response__:

```json
{
"entities":[
  {
  "id":1,
  "versionId":1,
  "topologyId":1,
  "name":"window_auto_generated",
  "description":"window description auto generated",
  "streams":["kafka_stream_1"],
  "window":{
      "windowLength":{"class":".Window$Duration","durationMs":2000},
    "slidingInterval":{"class":".Window$Duration","durationMs":2000},
      "tsField":null,
      "lagMs":0
    },
  "actions":[{
      "outputStreams":["window_transform_stream_1"],
      "name":"transformAction",
      "__type":"com.hortonworks.streamline.streams.layout.component.rule.action.TransformAction",
      "transforms":[]
    }],,
  "projections":[{"functionName":"MIN","args":["driverId"],
    "outputFieldName":"driverId_MIN"},
    {"expr":"driverId"},
    {"expr":"truckId"}
     ],
  "groupbykeys":["driverId","truckId"],
  "outputStreams":[
    "window_transform_stream_1","window_notifier_stream_1"]
}
]
}
```
__Response Fields__ :

|Field|Value|Description|
|---  |---  |---
| id | Integer | edge id |
| versionId | Integer | current topology version id |
| topologyId | Integer | topology id |
| name | String | window name |
| description | String | description for window |
| streams | Array | input stream name of fields in this window |
| projections | Array of Objects | output fields and selected functions |
| groupbykeys | Array | selected fields from input stream |
| window | Object | window interval as time or count |
| outputStreams | Array | output streams associated with the window |
| actions | Array | action added when aggregate processor has outgoing edges |

### /api/v1/catalog/topologies/:topologyId/windows/:id (PUT)
Updates the window with given id.

__Request params__:
|Field|Value|Description|
|---  |---  |---
|topologyId |Integer|topology id|
|id |Integer| edge id|

__Request fields__:
|Field|Value|Description|
|---  |---  |---
| name | String | window name |
| description | String | description for window |
| streams | Array | input stream name of fields in this window |
| projections | Array of Objects | output fields and selected functions |
| groupbykeys | Array | selected fields from input stream |
| window | Object | window interval as time or count |
| outputStreams | Array | output streams associated with the window |
| actions | Array | action added when aggregate processor has outgoing edges |

__Request JSON__ :

```json
{
  "name":"window_auto_generated",
  "description":"window description auto generated",
  "projections":[
    {"args":["driverId"],
    "functionName":"MIN",
    "outputFieldName":"driverId_MIN"
    },
    {"expr":"driverId"},
    {"expr":"truckId"}
  ],
  "streams":["kafka_stream_1"],
  "groupbykeys":["driverId","truckId"],
  "window":{
      "windowLength":{"class":".Window$Duration",
      "durationMs":2000
            },
    "slidingInterval":{"class":".Window$Duration",
      "durationMs":2000}
            },
  "actions":[{
      "outputStreams":["window_transform_stream_1"],
      "name":"transformAction",
      "__type":"com.hortonworks.streamline.streams.layout.component.rule.action.TransformAction",
      "transforms":[]
    }],,
  "outputStreams":["window_transform_stream_1","window_notifier_stream_1"]
}
```
__Sample response__:

```json
{
  "id":1,
  "versionId":1,
  "topologyId":1,
  "name":"window_auto_generated",
  "description":"window description auto generated",
  "streams":["kafka_stream_1"],
  "window":{
      "windowLength":{"class":".Window$Duration","durationMs":2000},
    "slidingInterval":{"class":".Window$Duration","durationMs":2000},
      "tsField":null,
      "lagMs":0
    },
  "actions":[{
      "outputStreams":["window_transform_stream_1"],
      "name":"transformAction",
      "__type":"com.hortonworks.streamline.streams.layout.component.rule.action.TransformAction",
      "transforms":[]
    }],,
  "projections":[{"functionName":"MIN","args":["driverId"],
    "outputFieldName":"driverId_MIN"},
    {"expr":"driverId"},
    {"expr":"truckId"}
     ],
  "groupbykeys":["driverId","truckId"],
  "outputStreams":[
    "window_transform_stream_1","window_notifier_stream_1"],
  "timestamp":1486640823606
}
```
__Response Fields__ :

|Field|Value|Description|
|---  |---  |---
| id | Integer | edge id |
| versionId | Integer | current topology version id |
| topologyId | Integer | topology id |
| name | String | window name |
| description | String | description for window |
| streams | Array | input stream name of fields in this window |
| projections | Array of Objects | output fields and selected functions |
| groupbykeys | Array | selected fields from input stream |
| window | Object | window interval as time or count |
| outputStreams | Array | output streams associated with the window |
| actions | Array | action added when aggregate processor has outgoing edges |
| timestamp | Integer | time when window was updated |

### /api/v1/catalog/topologies/:topologyId/windows/:id (DELETE)
Removes window from topology.

__Request params__:

|Field|Value|Description|
|---  |---  |---
|topologyId |Integer|topology id|
|id |Integer|rule id|

__Sample response__:

```json
{
  "id":1,
  "versionId":1,
  "topologyId":1,
  "name":"window_auto_generated",
  "description":"window description auto generated",
  "streams":["kafka_stream_1"],
  "window":{
      "windowLength":{"class":".Window$Duration","durationMs":2000},
    "slidingInterval":{"class":".Window$Duration","durationMs":2000},
      "tsField":null,
      "lagMs":0
    },
  "actions":[{
      "outputStreams":["window_transform_stream_1"],
      "name":"transformAction",
      "__type":"com.hortonworks.streamline.streams.layout.component.rule.action.TransformAction",
      "transforms":[]
    }],,
  "projections":[{"functionName":"MIN","args":["driverId"],
    "outputFieldName":"driverId_MIN"},
    {"expr":"driverId"},
    {"expr":"truckId"}
     ],
  "groupbykeys":["driverId","truckId"],
  "outputStreams":[
    "window_transform_stream_1","window_notifier_stream_1"],
  "timestamp":1486640823606
}
```
__Response Fields__ :

|Field|Value|Description|
|---  |---  |---
| id | Integer | edge id |
| versionId | Integer | current topology version id |
| topologyId | Integer | topology id |
| name | String | window name |
| description | String | description for window |
| streams | Array | input stream name of fields in this window |
| projections | Array of Objects | output fields and selected functions |
| groupbykeys | Array | selected fields from input stream |
| window | Object | window interval as time or count |
| outputStreams | Array | output streams associated with the window |
| actions | Array | action added when aggregate processor has outgoing edges |
| timestamp | Integer | time when window was deleted |






