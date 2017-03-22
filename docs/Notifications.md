# Streamline Notifications API

## Get Notification by Id 

GET /api/v1/notification/notifications/ID

*Success Response*

    GET /api/v1/notification/notifications/f3f08846-11c1-4b90-8dcb-941664d8b34c
    HTTP/1.1 200 OK
    Content-Type: application/json

```json
{
  "responseCode": 1000,
  "responseMessage": "Success",
  "entity": {
    "id": "f3f08846-11c1-4b90-8dcb-941664d8b34c",
    "fieldsAndValues": {"humidity": "100", "temperature": "100"},
    "eventIds": ["53b4e13a-000c-482a-b505-5df3b72a93fa"],
    "dataSourceIds": ["1"],
    "ruleId": "1",
    "status": "DELIVERED",
    "notifierName": "console_notifier",
    "ts": 1444714749066
  }
}
```

## Get Notifications based on query criteria
`GET /api/v1/notification/notifications/[dataSourceId=<>|ruleId=<>|notifierName=<>]&[startTs=<>]&[endTs=<>][numRows=<>]&[desc]`

All the parameters are optional. 

If no parameters are specified, returns the oldest 10 notifications from the notifications store.

`GET /api/v1/notification/notifications/`

```json
{
  "responseCode": 1000,
  "responseMessage": "Success",
  "entities": [
    {
      "id": "f3f08846-11c1-4b90-8dcb-941664d8b34c",
      "fieldsAndValues": {"humidity": "100", "temperature": "100"},
      "eventIds": ["53b4e13a-000c-482a-b505-5df3b72a93fa"],
      "dataSourceIds": ["1"],
      "ruleId": "1",
      "status": "DELIVERED",
      "notifierName": "console_notifier",
      "ts": 1444714749066
    },
    {
      "id": "1fee16ef-052c-4de9-8007-720f34ee7595",
      "fieldsAndValues": {"humidity": "100", "temperature": "100"},
      "eventIds": ["0f69607a-95f0-487c-be34-335acf3c9308"],
      "dataSourceIds": ["1"],
      "ruleId": "1",
      "status": "DELIVERED",
      "notifierName": "console_notifier",
      "ts": 1444714749069
    },
    ..
    ..
  ]
}
```

### Sample query criterias
|Criteria|Result|
|-------|-------|
|`?desc`|latest 10 notifications in descending order|
|`?numRows=20&desc`|latest 20 notifications in descending order|
|`?status=FAILED&desc`|latest 10 failed notifications in descending order|
|`?startTs=1444714750000&desc`|notifications with ts later than (>=) 1444714750000 millis in descending order|
|`?endTs=1444714750000&desc`|notifications with ts older than (<) 1444714750000 millis in descending order|
|`?startTs=1444714750000&endTs=1444714760000&desc`|notifications with ts between start and end ts in descending order (ts >= 1444714750000 && ts < 1444714760000)|
|`?notifierName=console_notifier&desc`|latest 10 notifications for notifier name 'console_notifier', in descending order|
|`?notifierName=console_notifier&status=FAILED&desc`|latest (10) failed notifications for notifier name 'console_notifier', in descending order|
|`?dataSourceId=123&desc`|latest 10 notifications due to events originated from datasource with id=123 in descending order|
|`?ruleId=100&desc`|latest 10 notifications triggered by rule with id=100 in descending order|

The startTs, endTs, numRows and desc can be used with other criteria to limit and order the results.

## Update notification status
This is to force update the notification status via API for troubleshooting.

PUT /notifications/{id}/{status}

status = NEW | FAILED | DELIVERED

```json
PUT /api/v1/notification/notifications/1fee16ef-052c-4de9-8007-720f34ee7595/FAILED
HTTP/1.1 200 OK
Content-Type: application/json
 
{
  "responseCode": 1000,
  "responseMessage": "Success",
  "entity": {
    "id": "1fee16ef-052c-4de9-8007-720f34ee7595",
    "fieldsAndValues": {"humidity": "100","temperature": "100"},
    "eventIds": ["0f69607a-95f0-487c-be34-335acf3c9308"],
    "dataSourceIds": ["1"],
    "ruleId": "1",
    "status": "FAILED",
    "notifierName": "console_notifier",
    "ts": 1444714749069
  }
}
```

## Get Events by ID

This API is to look up the event details for the events associated with a notification.

GET /api/v1/notification/events/ID

*Success Response*

    GET /api/v1/notification/events/0f69607a-95f0-487c-be34-335acf3c9308
    HTTP/1.1 200 OK
    Content-Type: application/json

```json
{
  "responseCode": 1000,
  "responseMessage": "Success",
  "entity": {
    "fieldsAndValues": {
      "is_online": "�",
      "locale": "en-US",
      "target_temperature_low_f": "\u0000\u0000\u0000B",
      "target_temperature_high_c": "\u0000\u0000\u0000\u001a",
      "target_temperature_f": "\u0000\u0000\u0000I",
      "software_version": "4.1",
      "target_temperature_c": "\u0000\u0000\u0000\u0016",
      "name": "Basement (F44A)",
      "away_temperature_high_f": "\u0000\u0000\u0000L",
      "ambient_temperature_f": "\u0000\u0000\u0000�",
      "ambient_temperature_c": "\u0000\u0000\u0000<",
      "away_temperature_low_c": "\u0000\u0000\u0000\f",
      "name_long": "Basement Thermostat (F44A)",
      "can_cool": "�",
      "away_temperature_low_f": "\u0000\u0000\u00007",
      "away_temperature_high_c": "\u0000\u0000\u0000\u0018",
      "has_leaf": "�",
      "target_temperature_low_c": "\u0000\u0000\u0000\u0013",
      "temperature_scale": "F",
      "can_heat": "�",
      "structure_id": "2Zywj6pjveHGouqUFvY09KcEvKq0HiUgLD_DrVUtcsDRDMt8dJaJoA",
      "device_id": "NZ517j3ynnLqDCXgVH4znuh-UurXzzD_",
      "hvac_mode": "cool",
      "is_using_emergency_heat": "\u0000",
      "target_temperature_high_f": "\u0000\u0000\u0000O",
      "hvac_state": "heating",
      "has_fan": "�",
      "fan_timer_active": "\u0000",
      "humidity": "\u0000\u0000\u0000_"
    },
    "dataSourceId": "1",
    "id": "0f69607a-95f0-487c-be34-335acf3c9308"
  }
}
```
