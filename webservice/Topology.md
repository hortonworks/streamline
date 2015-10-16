# Iotas Topology REST API


**Topology**

A *Topology* is a top level entity that is expected to be created by the 
user through UI. It can be thought of as a graph representing different 
components of an IoTaS topology and how they are connected. The components here
are the nodes of the graph and the connections between them are the edges. There
are types of components. They are DataSources, DataSinks and Processors.
Each component within a Topology is identified by a unique name in the UI. 
A Topology entity has the following attributes.

Field| Type | Comment
---|---|----
id| Long | The primary key
name| String| Name given by the user from UI
config| String| String representation of json that depicts the graph
timestamp | Long | Time or creation or last update
 
Due to the verbose nature of the config property a sample json can be referred 
to at http://www.jsoneditoronline.org/?id=d5058d9447fcfd0c145a61f0af00e13e 

**Components**

DataSource is a component that user will already have added using UI. It is 
presumed to be present in the catalog. The Processor component is what does
processing on data flowing through topology. Currently there are two 
processor components. Parser and Rule. 

## Rest API

Note that config json property is actually a string. Hence for a REST request to
the server it first needs to be JSON.stringify(config). 

Below is an explanation in detail how topology is related to DataSources and 
different APIs that UI can leverage to construct topology using the editor.
The first request for UI is to know what components are supported in the 
topology. Below is the request for that.

### Get

`GET /api/v1/catalog/system/componentdefinitions`

**Success Response**

    HTTP/1.1 200 OK
    Content-Type: application/json
```json
{
    "responseCode":1000,
    "responseMessage":"Success",
    "entities":["SOURCE","PROCESSOR","LINK","SINK"]
}
```

Next step is to get different fields that can be configured for a given
component like a SOURCE. For each component there is an endpoint to get the
fields. The response returns an array of objects. Each object representing a
field. The object will have 3 key value pairs. Name, defaultValue and if its
optional or not. Based on this UI can prompt user to enter the values for
these fields. The value entered by the user should be used to create the final
config object that will be a part of the config json property of the topology
that can be saved and deployed. The structure of that topology will be clear in
the later section. The endpoint of a component and its sample response is as
follows. Note that without any query parameters the response will return the
configuration field for all sources. Ideally UI should pass in two parameters
for getting configuration for a DataSource added in the catalog. They are
streamignEngine and subType An example URL with the query parameters is 
/api/v1/catalog/system/componentdefinitions/SOURCE?streamingEngine=STORM&subType=KAFKA 

The streaming engine here is STORM for now. The subType field comes from the 
type(previously endpoint) property of the DataFeed entity. Since the data 
source in the topology will be added from a drop down of data sources already
added in  device registration process, the subType parameter should be used 
to link. For DataSink and Processor components following URLs can be used to
get the supported components for a streaming technology which can then be used
to populate the drop down for the user to pick one of those components.

http://localhost:8080/api/v1/catalog/system/componentdefinitions/PROCESSOR?streamingEngine=STORM
http://localhost:8080/api/v1/catalog/system/componentdefinitions/SINK?streamingEngine=STORM

Below is a sample response for a get. Note that these configurations returned
can also be added/updated/deleted with the same endpoint but with a 
POST/PUT/DELETE request respectively.

Note that the transformationClass property is used to define the class that 
will handle the translation of the json to the underlying streaming engine 
equivalent. For the default storm components that we add by default that 
value will be populated already and UI should put that key and value in the 
topology json that user will create using UI.

### Get

`GET /api/v1/catalog/system/componentdefinitions/SOURCE`

**Success Response**

    HTTP/1.1 200 OK
    Content-Type: application/json
    
```json
{
    "responseCode":1000,
    "responseMessage":"Success",
    "entities":[
        {"id":1,
        "name":"kafkaSpoutComponent",
        "type":"SOURCE",
        "timestamp":1440118796994,
        "streamingEngine":"STORM",
        "subType":"KAFKA",
        "config":"[
            {\"name\":\"brokerHostsImpl\",\"isOptional\":false,\"defaultValue\":\"storm.kafka.ZkHosts\"},
            {\"name\":\"zkUrl\",\"isOptional\":false,\"defaultValue\":\"localhost:2181\"},
            {\"name\":\"zkPath\",\"isOptional\":true,\"defaultValue\":\"/brokers\"},
            {\"name\":\"refreshFreqSecs\",\"isOptional\":true,\"defaultValue\":60},
            {\"name\":\"topic\",\"isOptional\":false,\"defaultValue\":\"\"},
            {\"name\":\"zkRoot\",\"isOptional\":false,\"defaultValue\":\"/unique-zk-node-per-spout\"},
            {\"name\":\"spoutConfigId\",\"isOptional\":false,\"defaultValue\":\"unique-spout-config-id\"},
            {\"name\":\"fetchSizeBytes\",\"isOptional\":true,\"defaultValue\":1048576},
            {\"name\":\"socketTimeoutMs\",\"isOptional\":true,\"defaultValue\":10000},
            {\"name\":\"fetchMaxWait\",\"isOptional\":true,\"defaultValue\":10000},
            {\"name\":\"bufferSizeBytes\",\"isOptional\":true,\"defaultValue\":1048576},
            {\"name\":\"multiSchemeImpl\",\"isOptional\":true,\"defaultValue\":\"backtype.storm.spout.RawMultiScheme\"},
            {\"name\":\"ignoreZkOffsets\",\"isOptional\":true,\"defaultValue\":false},
            {\"name\":\"maxOffsetBehind\",\"isOptional\":true,\"defaultValue\":9223372036854776000},
            {\"name\":\"useStartOffsetTimeIfOffsetOutOfRange\",\"isOptional\":true,\"defaultValue\":true},
            {\"name\":\"metricsTimeBucketSizeInSecs\",\"isOptional\":true,\"defaultValue\":60},
            {\"name\":\"zkServers\",\"isOptional\":true,\"defaultValue\":[]},
            {\"name\":\"zkPort\",\"isOptional\":true,\"defaultValue\":2181},
            {\"name\":\"stateUpdateIntervalMs\",\"isOptional\":true,\"defaultValue\":2000},
            {\"name\":\"retryInitialDelayMs\",\"isOptional\":true,\"defaultValue\":0},
            {\"name\":\"retryDelayMultiplier\",\"isOptional\":true,\"defaultValue\":1},
            {\"name\":\"retryDelayMaxMs\",\"isOptional\":true,\"defaultValue\":60000},
            {\"name\":\"parallelism\",\"isOptional\":true,\"defaultValue\":1}
        ]",
        "transformationClass":"com.hortonworks.iotas.topology.storm.KafkaSpoutFluxComponent"}
    ]
}
```
### Create a Topology 

`POST /api/v1/catalog/topologies`

**Sample Input**

Please refer to the link for sample input and returned response. 
   
**Success Response**

    HTTP/1.1 201 Created
    Content-Type: application/json

### Get

`GET /api/v1/catalog/topologies/1`

**Success Response**

    GET /api/v1/catalog/topologies/1
    HTTP/1.1 200 OK
    Content-Type: application/json

Please refer to the link above for response json

**Error Response**

    GET /api/v1/catalog/topologies/10
    HTTP/1.1 404 Not Found
    Content-Type: application/json
    
```json
{
  "responseCode": 1101,
  "responseMessage": "Entity with id [10] not found."
}
```

### Get All

`GET /api/v1/catalog/topologies`

    GET /api/v1/catalog/topologies
    HTTP/1.1 200 OK
    Content-Type: application/json


The response will have an array of jsons as per link above

### Update

`PUT /api/v1/catalog/topologies/1`

*Sample Input*

Same as above with an updated value for name and/or config fields

*Success Response*

    HTTP/1.1 200 OK
    Content-Type: application/json

Please refer to the link above for json response

### Delete

`DELETE /api/v1/catalog/topologies/1`

*Success Response*

    HTTP/1.1 200 OK
    Content-Type: application/json

```json
{
  "responseCode": 1000,
  "responseMessage": "Success",
  "entity": {
  }
}
```

The entity property will have a config property which will be json at the url
above 

Once the topology is saved successfully using the POST/PUT above there are a 
bunch of actions that can be taken on the topology. The urls for these 
actions are below. They are all POST requests

/api/v1/catalog//topologies/1/actions/validate
/api/v1/catalog//topologies/1/actions/deploy
/api/v1/catalog//topologies/1/actions/kill
/api/v1/catalog//topologies/1/actions/suspend
/api/v1/catalog//topologies/1/actions/resume


**More details on config property of Topology**

Referring to the config json at the link above, UI will be populating it based on 
information entered by user. The top level config property is an open map 
and UI can prompt user to add any key value pairs there. However the two 
properties UI must enforce the user to enter in the top level config can be 
found at the URL. This will be validated when the user tries to deploy the
topology. For all other components within the json, each component has the 
same structure. uiname is expected to be entered by the user and validated by
UI to be unique within that topology. The type field is the sub type property
from the component section above. The transformation class is also a value 
that should be fetched from the component json response and passed here. The 
config property for each component is basically key value pair based on the 
fields returned by the component response above and entered by user.

The final section is links. This is what wires up the components described 
before. They can be thought of edges connecting those components. The key 
used for connecting components is the uiname. For now links are expected to 
go from a data source to a processor/data sink or from a processor to a 
processor/data sink. Note that processor incoming edge is connected using uiname 
of the processor. However, the outgoing edges are connected using uiname of 
the rule. Processor can be thought of as a component with one input pin and 
multiple output pins. The validations will be done on the server side. 
However the UI should be able to create the json described from a visual 
graph so that it can be submitted via rest api and vice versa(i.e. produce a 
visual graph for a json retrieved from rest api)
