# Streamline Namespace REST API

**Namespace**

A *Namespace* is a virtual group which maps services and clusters. 
Namespace associates service name and cluster id which are stored to NamespaceServiceClusterMapping.  

A Namespace entity has the following attributes:

Field| Type | Comment
---|---|----
id| Long | The primary key
name| String| Name given by the user from UI
description| String| Description given by the user from UI 
timestamp | Long | Time or creation or last update

## Rest API

## Create Namespace

`POST /api/v1/catalog/namespaces`

*Sample Input*

```json
{
	"name": "production",
	"description": "namespace for production env."
}
```

*Success Response*

```json
{
  "responseCode": 1000,
  "responseMessage": "Success",
  "entity": {
    "id": 5,
    "name": "production",
    "description": "namespace for production env.",
    "timestamp": 1478664425684
  }
}
```

### List Namespaces

`GET /api/v1/catalog/namespaces`

**Success Response**

    HTTP/1.1 200 OK
    Content-Type: application/json
    
```json
{
  "responseCode": 1000,
  "responseMessage": "Success",
  "entities": [
    {
      "id": 5,
      "name": "production",
      "description": "namespace for production env.",
      "timestamp": 1478664425684
    },
    {
      "id": 1,
      "name": "dev",
      "description": "namespace for development",
      "timestamp": 1478664402960
    },
    {
      "id": 3,
      "name": "stage",
      "description": "namespace for staging env.",
      "timestamp": 1478664416290
    }
  ]
}
```

Users can also provide `detail=true` to query parameter and get corresponding mappings, too.
 
`GET /api/v1/catalog/namespaces?detail=true`

**Success Response**

    HTTP/1.1 200 OK
    Content-Type: application/json

```json
{
  "entities": [
    {
      "namespace": {
        "id": 3,
        "name": "production3",
        "streamingEngine": "STORM",
        "timeSeriesDB": "AMBARI_METRICS",
        "description": "namespace for production env. 3",
        "timestamp": 1480338278241
      },
      "mappings": []
    },
    {
      "namespace": {
        "id": 2,
        "name": "production2",
        "streamingEngine": "STORM",
        "timeSeriesDB": "AMBARI_METRICS",
        "description": "namespace for production env. 2",
        "timestamp": 1480338238483
      },
      "mappings": [
        {
          "namespaceId": 2,
          "serviceName": "HBASE",
          "clusterId": 1
        },
        {
          "namespaceId": 2,
          "serviceName": "HDFS",
          "clusterId": 1
        }
      ]
    },
    {
      "namespace": {
        "id": 1,
        "name": "production",
        "streamingEngine": "STORM",
        "timeSeriesDB": "AMBARI_METRICS",
        "description": "namespace for production env.",
        "timestamp": 1480338084732
      },
      "mappings": [
        {
          "namespaceId": 1,
          "serviceName": "KAFKA",
          "clusterId": 1
        },
        {
          "namespaceId": 1,
          "serviceName": "STORM",
          "clusterId": 1
        }
      ]
    }
  ]
}
```

### Get Namespace

`GET /api/v1/catalog/namespaces/:namespaceId`

**Success Response**

    HTTP/1.1 200 OK
    Content-Type: application/json
    
```json
{
  "responseCode": 1000,
  "responseMessage": "Success",
  "entity": {
    "id": 1,
    "name": "dev",
    "description": "namespace for development",
    "timestamp": 1478664402960
  }
}
```

**Error Response**

    GET /api/v1/catalog/namespaces/10
    HTTP/1.1 404 Not Found
    Content-Type: application/json
    
```json
{
  "responseCode": 1101,
  "responseMessage": "Entity with id [10] not found. Please check webservice/ErrorCodes.md for more details."
}
```

Users can also provide `detail=true` to query parameter and get corresponding mappings, too.
 
`GET /api/v1/catalog/namespaces/1?detail=true`

**Success Response**

    HTTP/1.1 200 OK
    Content-Type: application/json

```json
{
  "namespace": {
    "id": 1,
    "name": "production",
    "streamingEngine": "STORM",
    "timeSeriesDB": "AMBARI_METRICS",
    "description": "namespace for production env.",
    "timestamp": 1480338084732
  },
  "mappings": [
    {
      "namespaceId": 1,
      "serviceName": "KAFKA",
      "clusterId": 1
    },
    {
      "namespaceId": 1,
      "serviceName": "STORM",
      "clusterId": 1
    }
  ]
}
```

### Get Namespace by name

`GET /api/v1/catalog/namespaces/:namespaceName`

**Success Response**

    HTTP/1.1 200 OK
    Content-Type: application/json

```json
{
  "responseCode": 1000,
  "responseMessage": "Success",
  "entity": {
    "id": 5,
    "name": "production",
    "description": "namespace for production env.",
    "timestamp": 1478664425684
  }
}
```

**Error Response**

    GET /api/v1/catalog/namespaces/name/production2
    HTTP/1.1 404 Not Found
    Content-Type: application/json
    
```json
{
  "responseCode": 1102,
  "responseMessage": "Entity with name [production2] not found. Please check webservice/ErrorCodes.md for more details."
}
```

Users can also provide `detail=true` to query parameter and get corresponding mappings, too.

`GET /api/v1/catalog/namespaces/production?detail=true`

**Success Response**

    HTTP/1.1 200 OK
    Content-Type: application/json

```json
{
  "namespace": {
    "id": 1,
    "name": "production",
    "streamingEngine": "STORM",
    "timeSeriesDB": "AMBARI_METRICS",
    "description": "namespace for production env.",
    "timestamp": 1480338084732
  },
  "mappings": [
    {
      "namespaceId": 1,
      "serviceName": "KAFKA",
      "clusterId": 1
    },
    {
      "namespaceId": 1,
      "serviceName": "STORM",
      "clusterId": 1
    }
  ]
}
```

### Update Namespace

`PUT /api/v1/catalog/namespaces/:namespaceId`

*Sample Input*

```json
{
	"id": 5,
	"name": "production-new",
	"description": "modified namespace for production env."
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
    "id": 5,
    "name": "production-new",
    "description": "modified namespace for production env.",
    "timestamp": 1478665221829
  }
}
```

### Delete Namespace

`DELETE /api/v1/catalog/namespaces/:namespaceId`

*Success Response*

    HTTP/1.1 200 OK
    Content-Type: application/json

```json
{
  "responseCode": 1000,
  "responseMessage": "Success",
  "entity": {
    "id": 5,
    "name": "production-new",
    "description": "modified namespace for production env.",
    "timestamp": 1478665221829
  }
}
```

*Error Response*

    HTTP/1.1 404 Not Found
    Content-Type: application/json

```json
{
  "responseCode": 1101,
  "responseMessage": "Entity with id [5] not found. Please check webservice/ErrorCodes.md for more details."
}
```


### List mapping on services to clusters in Namespace

`GET /api/v1/catalog/namespaces/:namespaceId/mapping`

*Success Response*

    HTTP/1.1 200 OK
    Content-Type: application/json

```json
{
  "responseCode": 1000,
  "responseMessage": "Success",
  "entities": [
    {
      "namespaceId": 1,
      "serviceName": "KAFKA",
      "clusterId": 1
    },
    {
      "namespaceId": 1,
      "serviceName": "HDFS",
      "clusterId": 2
    },
    {
      "namespaceId": 1,
      "serviceName": "HBASE",
      "clusterId": 2
    },
    {
      "namespaceId": 1,
      "serviceName": "STORM",
      "clusterId": 1
    }
  ]
}
```

### Get mapped Clusters for Service in Namespace

`GET /api/v1/catalog/namespaces/:namespaceId/mapping/:serviceName`

*Success Response*

    HTTP/1.1 200 OK
    Content-Type: application/json

```json
{
  "responseCode": 1000,
  "responseMessage": "Success",
  "entities": [
      {
        "namespaceId": 1,
        "serviceName": "STORM",
        "clusterId": 1
      },
      {
         "namespaceId": 1,
         "serviceName": "KAFKA",
         "clusterId": 1
      },
      {
         "namespaceId": 1,
         "serviceName": "KAFKA",
         "clusterId": 2
      }
  ]
}
```

### Map Service to Cluster in Namespace

`POST /api/v1/catalog/namespaces/:namespaceId/mapping`

*Sample Input*

```json
{
	"namespaceId": 1,
	"serviceName": "KAFKA",
	"clusterId": 1
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
    "namespaceId": 1,
    "serviceName": "KAFKA",
    "clusterId": 1
  }
}
```

*Error Response*

    POST /api/v1/catalog/namespaces/10/mapping
    HTTP/1.1 404 Not Found
    Content-Type: application/json

```json
{
  "responseCode": 1101,
  "responseMessage": "Entity with id [10] not found. Please check webservice/ErrorCodes.md for more details."
}
```

### Map multiple Services to Cluster in Namespace

`POST /api/v1/catalog/namespaces/:namespaceId/mapping/bulk`


*Sample Input*

```json
[
	{
		"namespaceId": 2,
		"serviceName": "HDFS",
		"clusterId": 1
	},
	{
		"namespaceId": 2,
		"serviceName": "HBASE",
		"clusterId": 1
	}
]
```

*Success Response*

    HTTP/1.1 200 OK
    Content-Type: application/json

```json
{
  "entities": [
    {
      "namespaceId": 2,
      "serviceName": "HDFS",
      "clusterId": 1
    },
    {
      "namespaceId": 2,
      "serviceName": "HBASE",
      "clusterId": 1
    }
  ]
}
```

*Error Response*

    POST /api/v1/catalog/namespaces/5/mapping/bulk
    HTTP/1.1 404 Not Found
    Content-Type: application/json

```json
{
  "responseMessage": "Entity with id [5] not found."
}
```

### Unmap Service in Namespace
 
`DELETE /api/v1/catalog/namespaces/1/mapping/:serviceName/cluster/:clusterId`

*Success Response*

    HTTP/1.1 200 OK
    Content-Type: application/json

```json
{
  "responseCode": 1000,
  "responseMessage": "Success",
  "entity": {
    "namespaceId": 1,
    "serviceName": "STORM",
    "clusterId": 1
  }
}
```

*Error Response*

    DELETE /api/v1/catalog/namespaces/1/mapping/NIFI
    HTTP/1.1 404 Not Found
    Content-Type: application/json

```json
{
  "responseCode": 1101,
  "responseMessage": "Entity with id [Namespace: 1 / Service name: NIFI] not found. Please check webservice/ErrorCodes.md for more details."
}
```