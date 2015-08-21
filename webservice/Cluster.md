# Cluster REST API

## Concepts



**Cluster**

A *Cluster* is a top level entity that can be used to define an external components like storm, kafka or spark clusters
that IOT framework interacts with. A cluster is an aggregation of components that runs on some *Host:port*. 

Field| Type | Comment
---|---|----
Id   | Integer | The primary key
name | String| 
type | Enum  | Storm, Kafka, Spark etc
description | String| 
tags | String | E.g For tagging this as test/prod etc
  
**Components**

A component refers to an entity within a cluster providing a specific functionality. E.g Storm Nimbus, 
Kafka Broker etc. To keep it simple, rather than creating a separate entity for host, the hostnames (and port) is 
directly specified as fields within a component.


Field| Type | Comment
---|---|----
Id | Integer | The primary key
clusterId | Integer | Foreign key reference to Cluster.
name | String |
type | Enum | The component type. Values are based on Cluster type {NIMBUS, SUPERVISOR, UI, ZK} in Storm, {BROKER, ZK} in Kafka
description | String | Detailed description of the component.
config | String | Optional component configuration (json key value pair). Can be used to override deployed properties.
hosts | String | A list of host names or IP (range expression) where this component runs.
port  | Integer | The port number where this component listens.

## Rest API

Only the create api is specifed now. Others will be added once we finalize.

### Create a cluster

`POST /api/v1/clusters`

**Sample Input**

```json
{
 "name": "Storm cluster",
 "type": "STORM",
 "description": "The storm cluster that handles IOT events",
 "tags": "production"
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
 "id" : 1,
 "clusterName": "Storm cluster",
 "type": "STORM",
 "description": "The storm cluster that handles IOT events",
 "tags": "production"
 }
}
```


### Create components

`POST /api/v1/clusters/:clusterid/components`

*Sample Input*

```json
{
 "name": "Nimbus",
 "type": "NIMBUS",
 "description": "Storm nimbus servers",
 "config": "",
 "hosts": "storm[1-2].hortonworks.com",
 "port": 6627
}
```
   
*Success Response*

    HTTP/1.1 201 Created
    Content-Type: application/json
    
```json
{
 "responseCode": 1000,
 "responseMessage": "Success",
 "entity": {
 "id": 1,
 "clusterId": 1,
 "name": "Nimbus",
 "type": "NIMBUS",
 "description": "Storm nimbus servers",
 "config": "",
 "hosts": "storm[1-2].hortonworks.com",
 "port": 6627
 }
}
```

