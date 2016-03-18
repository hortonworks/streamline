# IoTas REST API

[Cluster api](webservice/Cluster.md)
[Notifications api](webservice/Notifications.md)

[Errorcodes](webservice/ErrorCodes.md)


## DataSources with DataFeed 
### Create
POST /api/v1/catalog/datasources

*Sample Input*

```json
{
 "dataSourceName": "NestDevice",
 "description": "This is a nest device",
 "tags": "tag1",
 "type": "DEVICE",
 "typeConfig": "{\"id\": 1, \"version\":1}",
 "dataFeedName": "feed1",
 "parserId": 1,
 "parserName": "JSON Parser",
 "dataFeedType": "KAFKA"
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
    "dataSourceId": 1,
    "dataSourceName": "NestDevice",
    "description": "This is a nest device",
    "tags": "tag1",
    "timestamp": 1444216784208,
    "type": "DEVICE",
    "typeConfig": "{\"id\": 1, \"version\":1}",
    "dataFeedName": "feed1",
    "parserId": 1,
    "parserName": "JSON Parser",
    "dataFeedType": "KAFKA"
  }
}
```


*Error Response*

    HTTP/1.1 500 Internal Server Error
    Content-Type: application/json
    
```json    
{
 "responseCode": 1102,
 "responseMessage": "An exception with message [msg] was thrown while processing request."
}
```

### Get
GET /api/v1/catalog/datasources/ID

*Success Response*

    GET /api/v1/catalog/datasources/1
    HTTP/1.1 200 OK
    Content-Type: application/json

```json
{
  "responseCode": 1000,
  "responseMessage": "Success",
  "entity": {
    "dataSourceId": 1,
    "dataSourceName": "NestDevice",
    "description": "This is a nest device",
    "tags": "tag1",
    "timestamp": 1444216784208,
    "type": "DEVICE",
    "typeConfig": "{\"id\": 1, \"version\":1}",
    "dataFeedName": "feed1",
    "parserId": 1,
    "parserName": "JSON Parser",    
    "dataFeedType": "KAFKA"
  }
}
```

*Error Response*

    GET /api/v1/catalog/datasources/10
    HTTP/1.1 404 Not Found
    Content-Type: application/json
    
```json    
{
  "responseCode": 1101,
  "responseMessage": "Entity with id [10] not found."
}
```
    
### Get All
GET /api/v1/catalog/datasources

    GET /api/v1/catalog/datasources
    HTTP/1.1 200 OK
    Content-Type: application/json
    
```json    
{
  "responseCode": 1000,
  "responseMessage": "Success",
  "entities": [
    {
      "dataSourceId": 1,
      "dataSourceName": "NestDevice",
      "description": "This is a nest device",
      "tags": "tag1",
      "timestamp": 1444216784208,
      "type": "DEVICE",
      "typeConfig": "{\"id\": 1, \"version\":1}",
      "dataFeedName": "feed1",
      "parserId": 1,
      "parserName": "JSON Parser",      
      "dataFeedType": "KAFKA"
    },
    {
      "dataSourceId": 2,
      "dataSourceName": "New Device",
      "description": "This is a new device",
      "tags": "foo",
      "timestamp": 1444216784208,
      "type": "DEVICE",
      "typeConfig": "{\"id\": 1, \"version\":1}",
      "dataFeedName": "feed2",
      "parserId": 1,
      "parserName": "JSON Parser",
      "dataFeedType": "KAFKA"
    },    
    ..
    ..
  ]
}
```

**Query Datasource by type**

Data sources matching a specific type can be queried as follows

GET /api/v1/catalog/datasources/type/DEVICE

In addition, query params can be passed to filter results matching certain criteria. For example to list all devices with deviceid 'nest' with tag 'tag1',
   
    GET /api/v1/catalog/datasources/type/DEVICE/?id=nest&version=1
    HTTP/1.1 200 OK
    Content-Type: application/json
  
```json
{
  "responseCode": 1000,
  "responseMessage": "Success",
  "entities": [
    {
      "dataSourceId": 1,
      "dataSourceName": "NestDevice",
      "description": "This is a nest device",
      "tags": "tag1",
      "timestamp": 1444216784208,
      "type": "DEVICE",
      "typeConfig": "{\"id\": \"nest\", \"version\":1}",
      "dataFeedName": "feed1",
      "parserId": 1,
      "parserName": "JSON Parser",      
      "dataFeedType": "KAFKA"
    }
  ]
}
```

    GET /api/v1/catalog/datasources/type/DEVICE/?id=foobar&version=1
    HTTP/1.1 404 Not Found
    Content-Type: application/json

```json
{
  "responseCode": 1104,
  "responseMessage": "Datasource not found for type [DEVICE], query params [[QueryParam{name='id', value='foobar'}, QueryParam{name='version', value='1'}]]."
}
```

### Update
PUT /api/v1/catalog/datasources/ID

*Sample Input*

```json
{
 "dataSourceName": "NestDevice",
 "description": "This is a nest device",
 "tags": "tag1",
 "type": "DEVICE",
 "typeConfig": "{\"id\": 1, \"version\":1}",
 "dataFeedName": "feed1",
 "parserId": 1,
 "dataFeedType": "KAFKA"
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
    "dataSourceId": 1,
    "dataSourceName": "NestDevice",
    "description": "This is a nest device",
    "tags": "tag1",
    "timestamp": 1444216784208,
    "type": "DEVICE",
    "typeConfig": "{\"id\": 1, \"version\":1}",
    "dataFeedName": "feed1",
    "parserId": 1,
    "parserName": "JSON Parser",
    "dataFeedType": "KAFKA"
  }
}
```

**Note:** The current behavior is for PUT to create a new resource if the resource with the given ID does not exist yet.
 
### Delete
DELETE /api/v1/catalog/datasources/ID


*Success Response*

    HTTP/1.1 200 OK
    Content-Type: application/json
    
```json    
{
  "responseCode": 1000,
  "responseMessage": "Success",
  "entity": {
    "dataSourceId": 1,
    "dataSourceName": "NestDevice",
    "description": "This is a nest device",
    "tags": "tag1",
    "timestamp": 1444216784208,
    "type": "DEVICE",
    "typeConfig": "{\"id\": 1, \"version\":1}",
    "dataFeedName": "feed1",
    "parserId": 1,
    "parserName": "JSON Parser",
    "dataFeedType": "KAFKA"
  }
}
```

*Error Response*

    DELETE /api/v1/catalog/datasources/10
    HTTP/1.1 404 Not Found
    Content-Type: application/json

```json
{
  "responseCode": 1101,
  "responseMessage": "Entity with id [10] not found."
}
```


## DataSource 
### Below APIs are deprecated

### Create
POST /api/v1/catalog/deprecated/datasources

*Sample Input*

```json
{
 "name": "NestDevice",
 "description": "Thisisanestdevice",
 "tags": "tag1",
 "type": "DEVICE",
 "typeConfig": "{\"id\": 1, \"version\":1}"
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
    "name": "NestDevice",
    "description": "Thisisanestdevice",
    "tags": "tag1",
    "timestamp": 1439879278020,
    "type": "DEVICE",
    "typeConfig": "{\"id\": 1, \"version\":1}"
  }
}
```


*Error Response*

    HTTP/1.1 500 Internal Server Error
    Content-Type: application/json
    
```json    
{
 "responseCode": 1102,
 "responseMessage": "An exception with message [msg] was thrown while processing request."
}
```
    
### Get
GET /api/v1/catalog/deprecated/datasources/ID

*Success Response*

    GET /api/v1/catalog/deprecated/datasources/1
    HTTP/1.1 200 OK
    Content-Type: application/json

```json
{
  "responseCode": 1000,
  "responseMessage": "Success",
  "entity": {
    "id": 1,
    "name": "NestDevice",
    "description": "Thisisanestdevice",
    "tags": "tag1",
    "timestamp": 1439879278020,
    "type": "DEVICE",
    "typeConfig": "{\"id\":\"1\",\"version\":1}"
  }
}
```

*Error Response*

    GET /api/v1/catalog/deprecated/datasources/10
    HTTP/1.1 404 Not Found
    Content-Type: application/json
    
```json    
{
  "responseCode": 1101,
  "responseMessage": "Entity with id [10] not found."
}
```
    
### Get All
GET /api/v1/catalog/deprecated/datasources

    GET /api/v1/catalog/deprecated/datasources
    HTTP/1.1 200 OK
    Content-Type: application/json
    
```json    
{
  "responseCode": 1000,
  "responseMessage": "Success",
  "entities": [
    {
      "id": 1,
      "name": "NestDevice",
      "description": "Thisisanestdevice",
      "tags": "tag1",
      "timestamp": 1439879278020,
      "type": "DEVICE",
      "typeConfig": "{\"id\":\"1\",\"version\":1}"
    },
    {
      "id": 2,
      "name": "New Device",
      "description": "Foo",
      "tags": "bar",
      "timestamp": 1439879484562,
      "type": "DEVICE",
      "typeConfig": "{\"id\":\"1\",\"version\":1}"
    }
    ..
    ..
  ]
}
```

**Query Datasource by type**

Data sources matching a specific type can be queried as follows

GET /api/v1/catalog/deprecated/datasources/type/DEVICE

In addition, query params can be passed to filter results matching certain criteria. For example to list all devices with deviceid 'nest' with tag 'tag1',
   
    GET /api/v1/catalog/deprecated/datasources/type/DEVICE/?id=nest&version=1
    HTTP/1.1 200 OK
    Content-Type: application/json
  
```json
{
  "responseCode": 1000,
  "responseMessage": "Success",
  "entities": [
    {
      "id": 3,
      "name": "NestDevice",
      "description": "This is a nest device",
      "tags": "tag1",
      "timestamp": 1440060823724,
      "type": "DEVICE",
      "typeConfig": "{\"id\":\"nest\",\"version\":1}"
    }
  ]
}
```

    GET /api/v1/catalog/deprecated/datasources/type/DEVICE/?id=foobar&version=1
    HTTP/1.1 404 Not Found
    Content-Type: application/json

```json
{
  "responseCode": 1104,
  "responseMessage": "Datasource not found for type [DEVICE], query params [[QueryParam{name='id', value='foobar'}, QueryParam{name='version', value='1'}]]."
}
```

### Update
PUT /api/v1/catalog/deprecated/datasources/ID

*Sample Input*

```json
{
 "name": "NestDevice",
 "description": "This is a nest device",
 "tags": "foobar",
 "type": "DEVICE",
 "typeConfig": "{\"id\": 1, \"version\":1}"
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
    "name": "NestDevice",
    "description": "This is a nest device",
    "tags": "foobar",
    "timestamp": 1439882682614,
    "type": "DEVICE",
    "typeConfig": "{\"id\": 1, \"version\":1}"
  }
}
```

**Note:** The current behavior is for PUT to create a new resource if the resource with the given ID does not exist yet.
 
### Delete
DELETE /api/v1/catalog/deprecated/datasources/ID


*Success Response*

    HTTP/1.1 200 OK
    Content-Type: application/json
    
```json    
{
  "responseCode": 1000,
  "responseMessage": "Success",
  "entity": {
    "id": 1,
    "name": "NestDevice",
    "description": "This is a nest device",
    "tags": "foobar",
    "timestamp": 1439882682614,
    "type": "DEVICE",
    "typeConfig": "{\"id\": 1, \"version\":1}"
  }
}
```

*Error Response*

    DELETE /api/v1/catalog/deprecated/datasources/10
    HTTP/1.1 404 Not Found
    Content-Type: application/json

```json
{
  "responseCode": 1101,
  "responseMessage": "Entity with id [10] not found."
}
```

## Parser
### Create
POST /api/v1/catalog/parsers



*Sample Input*

    curl -X POST -v -F parserJar=@parsers-0.1.0-SNAPSHOT.jar -F parserInfo=@ParserInfo.json http://localhost:8080/api/v1/catalog/parsers

*Sample ParserInfo.json*

```json
{
  "name": "TestParser",
  "className":"com.hortonworks.iotas.parsers.json.JsonParser",
  "parserSchema": {
                    "fields": [{"name": "DeviceName", "type": "STRING"}]
                  },
  "version":1
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
    "name": "TestParser",
    "className": "com.hortonworks.iotas.parsers.json.JsonParser",
    "jarStoragePath": "/tmp/parsers-0.1.0-SNAPSHOT.jar",
    "parserSchema": {
      "fields": [
        {
          "name": "DeviceName",
          "type": "STRING"
        }
      ]
    },
    "version": 1,
    "timestamp": 1439885941140
  }
}
```


*Error Response*

    HTTP/1.1 500 Internal Server Error
    Content-Type: application/json
    
```json    
{
 "responseCode": 1102,
 "responseMessage": "An exception with message [msg] was thrown while processing request."
}
```

**Note:** If schemaFromParserJar is set to 'true' (-F schemaFromParserJar=true) and if the `parserSchema` is not provided, the api will try to figure out the schema by loading the jar and
invoking `Parser.schema()` method on the parser implementation instance.

### Get
GET /api/v1/catalog/parsers/{id}

*Success Response*

    GET /api/v1/catalog/parsers/1
    HTTP/1.1 200 OK
    Content-Type: application/json

```json
{
  "responseCode": 1000,
  "responseMessage": "Success",
  "entity": {
    "id": 1,
    "name": "TestParser",
    "className": "com.hortonworks.iotas.parsers.json.JsonParser",
    "jarStoragePath": "/tmp/parsers-0.1.0-SNAPSHOT.jar",
    "parserSchema": {
      "fields": [{"name": "DeviceName", "type": "STRING"}]
    },
    "version": 1,
    "timestamp": 1439885941140
  }
}
```

*Error Response*

    GET /api/v1/catalog/parsers/10
    HTTP/1.1 404 Not Found
    Content-Type: application/json
    
```json    
{
  "responseCode": 1101,
  "responseMessage": "Entity with id [10] not found."
}
```

### Get parser schema
GET /api/v1/catalog/parsers/{id}/schema

```json
    GET /api/v1/catalog/parsers/1/schema
    HTTP/1.1 200 OK
    Content-Type: application/json
{
    "responseCode":1000,
    "responseMessage":"Success",
    "entity":{
        "fields":[
            {"name":"device_id","type":"STRING"}, 
            {"name":"locale","type":"STRING"},
            {"name":"software_version","type":"STRING"},
            {"name":"structure_id","type":"STRING"},
            {"name":"name","type":"STRING"},
            {"name":"name_long","type":"STRING"},
            {"name":"last_connection","type":"STRING"},
            {"name":"is_online","type":"STRING"},
            ..
            ..
        ]
    }
}
```
    
### Get All
GET /api/v1/catalog/parsers

    GET /api/v1/catalog/parsers
    HTTP/1.1 200 OK
    Content-Type: application/json
    

```json
{
  "responseCode": 1000,
  "responseMessage": "Success",
  "entities": [
    {
      "id": 1,
      "name": "TestParser",
      "className": "com.hortonworks.iotas.parsers.json.JsonParser",
      "jarStoragePath": "/tmp/parsers-0.1.0-SNAPSHOT.jar",
      "parserSchema": {
        "fields": [{"name": "DeviceName","type": "STRING"}]
      },
      "version": 1,
      "timestamp": 1439885941140
    },
    {
      "id": 2,
      "name": "New Parser",
      "className": "com.hortonworks.iotas.parsers.TestParser",
      "jarStoragePath": "/tmp/test-0.1.0-SNAPSHOT.jar",
      "parserSchema": {
        "fields": [{"name": "DeviceName","type": "STRING"}]
      },
      "version": 1,
      "timestamp": 1439886289774
    }
    ..
    ..
  ]
}    

```
 
### Delete
DELETE /api/v1/catalog/parsers/ID


*Success Response*

    HTTP/1.1 200 OK
    Content-Type: application/json
    
```json    
{
  "responseCode": 1000,
  "responseMessage": "Success",
  "entity": {
    "id": 1,
    "name": "TestParser",
    "className": "com.hortonworks.iotas.parsers.json.JsonParser",
    "jarStoragePath": "/tmp/parsers-0.1.0-SNAPSHOT.jar",
    "parserSchema": {
      "fields": [{"name": "DeviceName", "type": "STRING"}]
    },
    "version": 1,
    "timestamp": 1439885941140
  }
}
```

*Error Response*

    DELETE /api/v1/catalog/parsers/10
    HTTP/1.1 404 Not Found
    Content-Type: application/json

```json
{
  "responseCode": 1101,
  "responseMessage": "Entity with id [10] not found."
}
```

### Verify uploading parser
GET /api/v1/catalog/parsers/verify-upload

    curl -X POST -i -F parserJar=@parsers-0.1.0-SNAPSHOT.jar http://localhost:8080/api/v1/catalog/parsers/upload-verify
    
*Success Response*

    HTTP/1.1 100 Continue
    
    HTTP/1.1 200 OK
    Date: Mon, 14 Mar 2016 01:08:01 GMT
    Content-Type: application/json
    Content-Length: 158
    
    {"responseCode":1000,"responseMessage":"Success","entities":["com.hortonworks.iotas.parsers.json.JsonParser","com.hortonworks.iotas.parsers.nest.NestParser"]}%
    
*Error Response*

    *Error Response*
    
        HTTP/1.1 500 Internal Server Error
        Content-Type: application/json
        
    ```json    
    {
     "responseCode": 1102,
     "responseMessage": "An exception with message [msg] was thrown while processing request."
    }
    ```

### Download Jar
GET /api/v1/catalog/parsers/download/ID


    curl http://localhost:8080/api/v1/catalog/parsers/download/1 -o new.jar
    
*Success Response*
    
        HTTP/1.1 200 OK
        Content-Type: application/java-archive
        
*Error Response*
    
     GET /api/v1/catalog/parsers/download/10
     HTTP/1.1 404 Not Found
     Content-Type: application/json
    
```json    
{
  "responseCode": 1101,
  "responseMessage": "Entity with id [10] not found."
}
```

    HTTP/1.1 500 Internal Server Error
    Content-Type: application/json
    
```json    
{
 "responseCode": 1102,
 "responseMessage": "An exception with message [msg] was thrown while processing request."
}
```

## DataFeed
### Create
POST /api/v1/catalog/feeds

**Note:** A *DataSource* and *Parser* should have been already created and those IDs needs to be provided in the input.

*Sample Input*

```json
{
  "dataSourceId": 1,
  "name": "feed1",
  "description": "test feed",
  "tags": "tag1",
  "parserId": 1,
  "type": "KAFKA"
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
    "dataSourceId": 1,
    "name": "feed1",
    "description": "test feed",
    "tags": "tag1",
    "parserId": 1,
    "type": "KAFKA",
    "timestamp": 1439884044033
 }
}
```


*Error Response*

    HTTP/1.1 500 Internal Server Error
    Content-Type: application/json
    
```json    
{
 "responseCode": 1102,
 "responseMessage": "An exception with message [msg] was thrown while processing request."
}
```
    
### Get
GET /api/v1/catalog/feeds/ID

*Success Response*

    GET /api/v1/catalog/feeds/1
    HTTP/1.1 200 OK
    Content-Type: application/json

```json
{
  "responseCode": 1000,
  "responseMessage": "Success",
  "entity": {
    "id": 1,
    "dataSourceId": 1,
    "name": "feed1",
    "description": "test feed",
    "tags": "tag1",
    "parserId": 1,
    "type": "KAFKA",
    "timestamp": 1439884044033
  }
}
```

*Error Response*

    GET /api/v1/catalog/feeds/10
    HTTP/1.1 404 Not Found
    Content-Type: application/json
    
```json    
{
  "responseCode": 1101,
  "responseMessage": "Entity with id [10] not found."
}
```

### Get feed schema
GET /api/v1/catalog/feeds/{id}/schema

```json
    GET /api/v1/catalog/feeds/1/schema
    HTTP/1.1 200 OK
    Content-Type: application/json
{
    "responseCode":1000,
    "responseMessage":"Success",
    "entity":{
        "fields":[
            {"name":"device_id","type":"STRING"}, 
            {"name":"locale","type":"STRING"},
            {"name":"software_version","type":"STRING"},
            {"name":"structure_id","type":"STRING"},
            {"name":"name","type":"STRING"},
            {"name":"name_long","type":"STRING"},
            {"name":"last_connection","type":"STRING"},
            {"name":"is_online","type":"STRING"},
            ..
            ..
        ]
    }
}
```

**Note:** This fetches the schema from the parser associated with the data feed. 
    
### Get All
GET /api/v1/catalog/feeds

    GET /api/v1/catalog/feeds
    HTTP/1.1 200 OK
    Content-Type: application/json
    
```json    
{
  "responseCode": 1000,
  "responseMessage": "Success",
  "entities": [
    {
      "id": 1,
      "dataSourceId": 1,
      "name": "feed1",
      "description": "test feed",
      "tags": "tag1",
      "parserId": 1,
      "type": "KAFKA",
      "timestamp": 1439884044033
    },
    {
      "id": 2,
      "dataSourceId": 1,
      "name": "feed1",
      "description": "new feed",
      "tags": "foo",
      "parserId": 1,
      "type": "KAFKA",
      "timestamp": 1439884162102
    }
    ..
    ..
  ]
}
```

**Query Params**

Query params can be passed to filter results matching certain criteria. 

For example to list the datafeeds for id '1' with tag 'tag1',
   
    GET /api/v1/catalog/feeds/?id=1&tag=tag1
    HTTP/1.1 200 OK
    Content-Type: application/json
  
```json
{
  "responseCode": 1000,
  "responseMessage": "Success",
  "entities": [
    {
      "id": 1,
      "dataSourceId": 1,
      "name": "feed1",
      "description": "test feed",
      "tags": "tag1",
      "parserId": 1,
      "type": "KAFKA",
      "timestamp": 1439917400442
    }
  ]
}
```

    GET /api/v1/catalog/feeds/?tag=tag2
    HTTP/1.1 404 Not Found
    Content-Type: application/json

```json
{
  "responseCode": 1104,
  "responseMessage": "Datafeed not found for query params [[QueryParam{name='tags', value='tag2'}]]."
}
```

### Update
PUT /api/v1/catalog/feeds/ID

*Sample Input*

```json
{
  "dataSourceId": 1,
  "name": "feed1",
  "description": "test feed",
  "tags": "tag1",
  "parserId": 1,
  "type": "KAFKA"
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
    "dataSourceId": 1,
    "name": "feed1",
    "description": "test feed",
    "tags": "tag1",
    "parserId": 1,
    "type": "KAFKA",
    "timestamp": 1439884266987
  }
}
```

**Note:** The current behavior is for PUT to create a new resource if the resource with the given ID does not exist yet.
 
### Delete
DELETE /api/v1/catalog/feeds/ID


*Success Response*

    HTTP/1.1 200 OK
    Content-Type: application/json
    
```json    
{
  "responseCode": 1000,
  "responseMessage": "Success",
  "entity": {
    "id": 2,
    "dataSourceId": 1,
    "name": "feed1",
    "description": "test feed",
    "tags": "tag1",
    "parserId": 1,
    "type": "KAFKA",
    "timestamp": 1439884675590
  }
}
```

*Error Response*

    DELETE /api/v1/catalog/feeds/10
    HTTP/1.1 404 Not Found
    Content-Type: application/json

```json
{
  "responseCode": 1101,
  "responseMessage": "Entity with id [10] not found."
}
```

## TopologyEditorMetadata 
### Create
POST /api/v1/catalog/system/topologyeditormetadata

*Sample Input*

```json
{
  "topologyId": 1,
  "data": "some json stringified string"
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
    "topologyId": 1,
    "data": "some json stringified string",
    "timestamp": 1439884044033
 }
}
```


*Error Response*

    HTTP/1.1 500 Internal Server Error
    Content-Type: application/json
    
```json    
{
 "responseCode": 1102,
 "responseMessage": "An exception with message [msg] was thrown while processing request."
}
```
    
### Get
GET /api/v1/catalog/system/topologyeditormetadata/ID

*Success Response*

    GET /api/v1/catalog/system/topologyeditormetadata/1
    HTTP/1.1 200 OK
    Content-Type: application/json

```json
{
  "responseCode": 1000,
  "responseMessage": "Success",
  "entity": {
    "topologyId": 1,
    "data": "some json stringified string",
    "timestamp": 1439884044033
  }
}
```

*Error Response*

    GET /api/v1/catalog/system/topologyeditormetadata/10
    HTTP/1.1 404 Not Found
    Content-Type: application/json
    
```json    
{
  "responseCode": 1101,
  "responseMessage": "Entity with id [10] not found."
}
```

### Get All
GET /api/v1/catalog/system/topologyeditormetadata

    GET /api/v1/catalog/system/topologyeditormetadata
    HTTP/1.1 200 OK
    Content-Type: application/json
    
```json    
{
  "responseCode": 1000,
  "responseMessage": "Success",
  "entities": [
    {
      "topologyId": 1,
      "data": "some json stringified string",
      "timestamp": 1439884044033
    },
    {
      "topologyId": 2,
      "data": "some json stringified string 2",
      "timestamp": 1439884162102
    }
    ..
    ..
  ]
}
```

### Update
PUT /api/v1/catalog/system/topologyeditormetadata/ID

*Sample Input*

```json
{
  "topologyId": 1,
  "data": "some modified json stringified string"
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
    "topologyId": 1,
    "data": "some modified json stringified string",
    "timestamp": 1439884266987
  }
}
```

**Note:** The current behavior is for PUT to create a new resource if the resource with the given ID does not exist yet.
 
### Delete
DELETE /api/v1/catalog/system/topologyeditormetadata/ID

*Success Response*

    HTTP/1.1 200 OK
    Content-Type: application/json
    
```json    
{
  "responseCode": 1000,
  "responseMessage": "Success",
  "entity": {
    "topologyId": 1,
    "data": "some json stringified string",
    "timestamp": 1439884675590
  }
}
```

*Error Response*

    DELETE /api/v1/catalog/system/topologyeditormetadata/10
    HTTP/1.1 404 Not Found
    Content-Type: application/json

```json
{
  "responseCode": 1101,
  "responseMessage": "Entity with id [10] not found."
}
```

### A sample use case

Please go through [the sample use case](REST-Sample.md) to understand how these APIs can be used.

  

