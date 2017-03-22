# Streamline Custom Processor REST API


**Custom Processor**

A *Custom Processor* is a feature in Streamline that will let a user plug in custom
processing logic in their topologies. The first step to being able to do this
is registering the custom processor components. The rest endpoint for adding,
updating, deleting and getting custom processors is 
/api/v1/catalog/streams/componentbundles/PROCESSOR/custom 

Following are the properties in the json needed to register a custom processor.
Note that along with the json, the request also takes a file for each custom
processor. This file is the jar file that will contain the implementation of the
CustomProcessor interface

Field| Type | Comment
---|---|----
streamingEngine| String| Streaming Engine for this custom processor - e.g. STORM
name| String| Name of the custom processor. This should uniquely identify the custom processor 
description| String| Description of the custom processor
jarFileName| String| Unique name of the jar file that will be used to upload/download the jar
customProcessorImpl| String| Fully qualified class name implementing the interface
inputSchema| Schema| Input schema that this custom processor expects
outputStreamToSchema| Map<String,Schema>| Output schema per stream that it emits
topologyComponentUISpecification|TopologyComponentUISpecification| List of fields needed from user. Used by UI in topology editor


## Rest API

### Get

`GET /api/v1/catalog/streams/componentbundles/PROCESSOR/custom?name=Console%20Custom%20Processor`

**Success Response**

    HTTP/1.1 200 OK
    Content-Type: application/json
    
```json
{
    "responseCode":1000,
    "responseMessage":"Success",
    "entities":[
        {
            "streamingEngine":"STORM",
            "name":"Console Custom Processor",
            "description":"Console Custom Processor",
            "jarFileName":"streamline-core.jar",
            "inputSchema":{
                "fields":[
                    {
                        "name":"childField1",
                        "type":"INTEGER",
                        "optional":false
                    },
                    {
                        "name":"childField2",
                        "type":"BOOLEAN",
                        "optional":false
                    },
                    {
                        "name":"topLevelStringField",
                        "type":"STRING",
                        "optional":false
                    }
                ]
            },
            "outputStreamToSchema":{
                "stream1":{
                    "fields":[
                        {
                            "name":"childField1",
                            "type":"INTEGER","optional":false
                        },
                        {
                            "name":"childField2",
                            "type":"BOOLEAN",
                            "optional":false
                        },
                        {
                            "name":"topLevelStringField",
                            "type":"STRING",
                            "optional":false
                        }
                    ]
                }
            },
            "customProcessorImpl":"com.hortonworks.iotas.streams.runtime.processor.ConsoleCustomProcessorRuntime"
        }
    ]
}
```

### Post

`POST /api/v1/catalog/streams/componentbundles/PROCESSOR/custom`

This request takes jar file as well. A curl request below shows how to add a custom processor
along with its jar file. console_custom_processor is a file containing the json described
above.

`curl -sS -X POST -i -F jarFile=@../core/target/core-0.1.0-SNAPSHOT.jar  http://localhost:8080/api/v1/catalog/streams/componentbundles/PROCESSOR/custom -F customProcessorInfo=@console_custom_processor`
   
**Success Response**

    HTTP/1.1 201 Created
    Content-Type: application/json


### Put

`PUT /api/v1/catalog/streams/componentbundles/PROCESSOR/custom`

*Sample Input*

Same as POST above except that the file will now contain updated data with the same custom processor name

*Success Response*

    HTTP/1.1 200 OK
    Content-Type: application/json

### Delete

`DELETE /api/v1/catalog/streams/componentbundles/PROCESSOR/custom`

*Success Response*

    HTTP/1.1 200 OK
    Content-Type: application/json

All the above requests - POST, PUT and DELETE will return the below response message on successful
operation. The property entity is the object added, updated or deleted.

```json
{
  "responseCode": 1000,
  "responseMessage": "Success",
  "entity": {
  }
}
```

### Get jar file

To download the jar file associated with the custom processor use the GET requests below.
Note that the get URL ends with the name of the file. This has to be the same name that
was used to assign the property jarFileName in custom processor post/put.

`GET /api/v1/catalog/streams/componentbundles/PROCESSOR/custom/streamline-core.jar`

## Auto upload for Custom Processors

Streamline also provides a way to drop and upload custom processors to the system so that they can be used
in topology editor, just as if they were uploaded using a POST request using REST endpoint. For this,
Streamline server needs to be started with three configuration parameters set in streamline.yaml. They are

customProcessorWatchPath: "/tmp"

customProcessorUploadFailPath: "/tmp/failed"

customProcessorUploadSuccessPath: "/tmp/uploaded"

customProcessorWatchPath is where Streamline will poll for any tar files that are created. Those tar files
will automatically be tried for upload as CustomProcessor implementations. If the upload succeeds the
tar file will be moved to customProcessorUploadSuccessPath location and customProcessorUploadFailPath
otherwise. Note that these 3 directories are expected to be created with right permissions before
starting Streamline. Any files other than tar will be ignored and moved to customProcessorUploadFailPath.
The tar file is expected to have 2 files in it. The main file is info.json. This is the json file 
containing json representing CustomProcessorInfo. A sample json is shown below. The name of the file
has to be info.json. Otherwise it will fail to upload. The other file is the jar file.
As mentioned above jar file should contain the class implementing the CustomProcessorRuntime interface. Name of
the class should be the same as the value of the property customProcessorImpl in info.json. The jar
file in the tar should have the same name as the jarFileName property in info.json

```json
{
  "streamingEngine": "STORM",
  "name": "Console Custom Processor",
  "description": "Console Custom Processor",
  "jarFileName": "streamline-core.jar",
  "topologyComponentUISpecification": {"fields": []},
  "inputSchema": {"fields":[{"name":"childField1","type":"INTEGER"},{"name":"childField2","type":"BOOLEAN"},{"name":"topLevelStringField","type":"STRING"}]},
  "outputStreamToSchema": {"stream1":{"fields":[{"name":"childField1","type":"INTEGER"},{"name":"childField2","type":"BOOLEAN"},{"name":"topLevelStringField","type":"STRING"}]}},
  "customProcessorImpl": "com.hortonworks.iotas.streams.runtime.processor.ConsoleCustomProcessorRuntime"
}
```
