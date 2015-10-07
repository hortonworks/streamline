# IoTas REST API

[Cluster api](webservice/Cluster.md)

[Errorcodes](webservice/ErrorCodes.md)

## DataSource
### Create
POST /api/v1/catalog/datasources

*Sample Input*

```json
{
 "dataSourceName": "NestDevice",
 "description": "Thisisanestdevice",
 "tags": "tag1",
 "type": "DEVICE",
 "typeConfig": "{\"deviceId\": 1, \"version\":1}"
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
    "description": "Thisisanestdevice",
    "tags": "tag1",
    "timestamp": 1439879278020,
    "type": "DEVICE",
    "typeConfig": "{\"deviceId\": 1, \"version\":1}"
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
    "description": "Thisisanestdevice",
    "tags": "tag1",
    "timestamp": 1439879278020,
    "type": "DEVICE",
    "typeConfig": "{\"deviceId\":\"1\",\"version\":1}"
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
      "description": "Thisisanestdevice",
      "tags": "tag1",
      "timestamp": 1439879278020,
      "type": "DEVICE",
      "typeConfig": "{\"deviceId\":\"1\",\"version\":1}"
    },
    {
      "dataSourceId": 2,
      "dataSourceName": "New Device",
      "description": "Foo",
      "tags": "bar",
      "timestamp": 1439879484562,
      "type": "DEVICE",
      "typeConfig": "{\"deviceId\":\"1\",\"version\":1}"
    }
    ..
    ..
  ]
}
```

**Query Datasource by type**

Data sources matching a specific type can be queried as follows

GET /api/v1/catalog/datasources/type/DEVICE

In addition, query params can be passed to filter results matching certain criteria. For example to list all devices with deviceid 'nest' with tag 'tag1',
   
    GET /api/v1/catalog/datasources/type/DEVICE/?deviceId=nest&version=1
    HTTP/1.1 200 OK
    Content-Type: application/json
  
```json
{
  "responseCode": 1000,
  "responseMessage": "Success",
  "entities": [
    {
      "dataSourceId": 3,
      "dataSourceName": "NestDevice",
      "description": "This is a nest device",
      "tags": "tag1",
      "timestamp": 1440060823724,
      "type": "DEVICE",
      "typeConfig": "{\"deviceId\":\"nest\",\"version\":1}"
    }
  ]
}
```

    GET /api/v1/catalog/datasources/type/DEVICE/?deviceId=foobar&version=1
    HTTP/1.1 404 Not Found
    Content-Type: application/json

```json
{
  "responseCode": 1104,
  "responseMessage": "Datasource not found for type [DEVICE], query params [[QueryParam{name='deviceId', value='foobar'}, QueryParam{name='version', value='1'}]]."
}
```

### Update
PUT /api/v1/catalog/datasources/ID

*Sample Input*

```json
{
 "dataSourceName": "NestDevice",
 "description": "This is a nest device",
 "tags": "foobar",
 "type": "DEVICE",
 "typeConfig": "{\"deviceId\": 1, \"version\":1}"
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
    "tags": "foobar",
    "timestamp": 1439882682614,
    "type": "DEVICE",
    "typeConfig": "{\"deviceId\": 1, \"version\":1}"
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
    "tags": "foobar",
    "timestamp": 1439882682614,
    "type": "DEVICE",
    "typeConfig": "{\"deviceId\": 1, \"version\":1}"
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

## Parser
### Create
POST /api/v1/catalog/parsers



*Sample Input*

    curl -X POST -v -F parserJar=@parsers-0.1-SNAPSHOT.jar -F parserInfo=@ParserInfo.json http://localhost:8080/api/v1/catalog/parsers

*Sample ParserInfo.json*

```json
{
  "parserName": "TestParser",
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
    "parserId": 1,
    "parserName": "TestParser",
    "className": "com.hortonworks.iotas.parsers.json.JsonParser",
    "jarStoragePath": "/tmp/parsers-0.1-SNAPSHOT.jar",
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
    
### Get
GET /api/v1/catalog/parsers/ID

*Success Response*

    GET /api/v1/catalog/parsers/1
    HTTP/1.1 200 OK
    Content-Type: application/json

```json
{
  "responseCode": 1000,
  "responseMessage": "Success",
  "entity": {
    "parserId": 1,
    "parserName": "TestParser",
    "className": "com.hortonworks.iotas.parsers.json.JsonParser",
    "jarStoragePath": "/tmp/parsers-0.1-SNAPSHOT.jar",
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
      "parserId": 1,
      "parserName": "TestParser",
      "className": "com.hortonworks.iotas.parsers.json.JsonParser",
      "jarStoragePath": "/tmp/parsers-0.1-SNAPSHOT.jar",
      "parserSchema": {
        "fields": [{"name": "DeviceName","type": "STRING"}]
      },
      "version": 1,
      "timestamp": 1439885941140
    },
    {
      "parserId": 2,
      "parserName": "New Parser",
      "className": "com.hortonworks.iotas.parsers.TestParser",
      "jarStoragePath": "/tmp/test-0.1-SNAPSHOT.jar",
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
    "parserId": 1,
    "parserName": "TestParser",
    "className": "com.hortonworks.iotas.parsers.json.JsonParser",
    "jarStoragePath": "/tmp/parsers-0.1-SNAPSHOT.jar",
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
  "dataFeedName": "feed1",
  "description": "test feed",
  "tags": "tag1",
  "parserId": 1,
  "endpoint": "sample endpoint"
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
    "dataFeedId": 1,
    "dataSourceId": 1,
    "dataFeedName": "feed1",
    "description": "test feed",
    "tags": "tag1",
    "parserId": 1,
    "endpoint": "sample endpoint",
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
    "dataFeedId": 1,
    "dataSourceId": 1,
    "dataFeedName": "feed1",
    "description": "test feed",
    "tags": "tag1",
    "parserId": 1,
    "endpoint": "sample endpoint",
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
      "dataFeedId": 1,
      "dataSourceId": 1,
      "dataFeedName": "feed1",
      "description": "test feed",
      "tags": "tag1",
      "parserId": 1,
      "endpoint": "sample endpoint",
      "timestamp": 1439884044033
    },
    {
      "dataFeedId": 2,
      "dataSourceId": 1,
      "dataFeedName": "feed1",
      "description": "new feed",
      "tags": "foo",
      "parserId": 1,
      "endpoint": "hdfs://url",
      "timestamp": 1439884162102
    }
    ..
    ..
  ]
}
```

**Query Params**

Query params can be passed to filter results matching certain criteria. 

For example to list the datafeeds for dataSourceId '1' with tag 'tag1',
   
    GET /api/v1/catalog/feeds/?dataSourceId=1&tag=tag1
    HTTP/1.1 200 OK
    Content-Type: application/json
  
```json
{
  "responseCode": 1000,
  "responseMessage": "Success",
  "entities": [
    {
      "dataFeedId": 1,
      "dataSourceId": 1,
      "dataFeedName": "feed1",
      "description": "test feed",
      "tags": "tag1",
      "parserId": 1,
      "endpoint": "sample endpoint",
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
  "dataFeedName": "feed1",
  "description": "test feed",
  "tags": "tag1",
  "parserId": 1,
  "endpoint": "hdfs://url"
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
    "dataFeedId": 1,
    "dataSourceId": 1,
    "dataFeedName": "feed1",
    "description": "test feed",
    "tags": "tag1",
    "parserId": 1,
    "endpoint": "hdfs://url",
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
    "dataFeedId": 2,
    "dataSourceId": 1,
    "dataFeedName": "feed1",
    "description": "test feed",
    "tags": "tag1",
    "parserId": 1,
    "endpoint": "hdfs://url",
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

## DataSources with DataFeed 
### Create
POST /api/v1/catalog/datasourceswithdatafeed

*Sample Input*

```json
{
 "dataSourceName": "NestDevice",
 "description": "This is a nest device",
 "tags": "tag1",
 "type": "DEVICE",
 "typeConfig": "{\"deviceId\": 1, \"version\":1}",
 "dataFeedName": "feed1",
 "parserId": 1,
 "endpoint": "hdfs://url"
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
    "typeConfig": "{\"deviceId\": 1, \"version\":1}",
    "dataFeedName": "feed1",
    "parserId": 1,
    "endpoint": "hdfs://url"
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

### A sample use case

Please go through [the sample use case](REST-Sample.md) to understand how these APIs can be used.

  

