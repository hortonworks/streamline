#!/usr/bin/env bash
# --
# Creates and deploys a test topology using the topology REST APIs.
# --

function getId {
  str=$1
  echo $str | grep -o -E "\"id\":\d+" | head -n1 | cut -d : -f2
}

# --
# Upload parser
# --

host=${1:-"localhost"}
port=${2:-"8080"}
catalogurl="http://$host:$port/api/v1/catalog"

echo "Catalog url: $catalogurl"

echo -e "\n------"
curl -X POST -i -F parserJar=@parsers/target/parsers-0.1.0-SNAPSHOT.jar -F parserInfo='{"name":"Nest","className":"com.hortonworks.iotas.parsers.nest.NestParser","version":1}' ${catalogurl}/parsers

# --
# Create a device 
# --
echo -e "\n------"
curl -X POST -H "Content-Type: application/json" -H "Cache-Control: no-cache"  -d '{
"type":"DEVICE",
"dataSourceName":"test",
"description":"test",
"tags": "device",
"dataFeedName":"feed1",
"parserId":"1",
"dataFeedType":"KAFKA",
"typeConfig":"{\"make\":\"nest\",\"model\":\"m-1\"}",
"parserName":"nest"
}' "${catalogurl}/datasources"

# --
# Create a topology
# --
echo -e "\n------"
out=$(curl -s -X POST -H "Content-Type: application/json" -H "Cache-Control: no-cache"  -d '{
    "name": "test",
    "config": "{\"config\": {\"catalog.root.url\": \"http://localhost:8080/api/v1/catalog\", \"local.parser.jar.path\": \"/tmp\", \"local.notifier.jar.path\": \"/tmp\"}}"
}' "${catalogurl}/topologies")

echo $out
topologyid=$(getId $out)

# --
# Create streams
# --
echo -e "\n------"
out=$(curl -s -X POST -H "Content-Type: application/json" -H "Cache-Control: no-cache"  -d '{
    "streamId": "default",
    "fields": [{"name": "iotas.event", "type": "NESTED"} ]
}' "${catalogurl}/topologies/$topologyid/streams")

echo $out
streamid1=$(getId $out)

echo -e "\n------"
out=$(curl -s -X POST -H "Content-Type: application/json" -H "Cache-Control: no-cache"  -d '{
    "streamId": "parsedTuplesStream",
    "fields": [{"name": "temperature", "type": "LONG"}, {"name": "humidity", "type": "LONG"}]
}' "${catalogurl}/topologies/$topologyid/streams")

echo $out
streamid2=$(getId $out)

echo -e "\n------"
out=$(curl -s -X POST -H "Content-Type: application/json" -H "Cache-Control: no-cache"  -d '{
    "streamId": "sink-stream",
    "fields": [{"name": "iotas.event", "type": "NESTED"} ]
}' "${catalogurl}/topologies/$topologyid/streams")

echo $out
streamid3=$(getId $out)

# --
# Create kafka data source
# --
echo -e "\n------"
out=$(curl -s -X POST -H "Content-Type: application/json" -H "Cache-Control: no-cache"  -d '{
    "name": "kafkaDataSource",
    "config": {
        "properties": {
            "topic": "nest-topic",
            "zkRoot": "/Iotas-kafka-spout",
            "spoutConfigId": "nest-kafka-spout-config",
            "zkUrl": "localhost:2181",
            "zkPath": "/brokers",
            "refreshFreqSecs": 60
        }
    },
    "type": "KAFKA",
    "outputStreamIds": ['"$streamid1"']
}' "${catalogurl}/topologies/$topologyid/sources")

echo $out
sourceid=$(getId $out)


# --
# Create parser processor
# --
echo -e "\n------"
out=$(curl -s -X POST -H "Content-Type: application/json" -H "Cache-Control: no-cache"  -d '{
    "name": "ParserProcessor",
    "config": {
        "properties": {
            "parallelism": 1,
            "parsedTuplesStream": "parsedTuplesStream",
            "failedTuplesStream": "failedTuplesStream"
        }
    },
    "type": "PARSER",
    "outputStreamIds": ['$streamid2']
}' "${catalogurl}/topologies/$topologyid/processors")

echo $out
parserid=$(getId $out)

# --
# Create a rule
# --
echo -e "\n------"
out=$(curl -s -X POST -H "Content-Type: application/json" -H "Cache-Control: no-cache" -d '{
    "name": "rule1",
    "description": "rule test",
    "sql": "select temperature, humidity from parsedTuplesStream where humidity > 90 AND temperature > 80",
    "actions": [
      {
        "name": "hbasesink",
        "outputStreams": ["sink-stream"],
        "__type": "com.hortonworks.iotas.topology.component.rule.action.TransformAction"
      },
      {
        "name": "hdfssink",
        "outputStreams": ["sink-stream"],
        "__type": "com.hortonworks.iotas.topology.component.rule.action.TransformAction"
      },
      {
        "name": "notificationsink",
        "outputFieldsAndDefaults": {
          "body": "rule_1 fired"
         },
        "outputStreams": ["sink-stream"],
         "notifierName": "email_notifier",
          "__type": "com.hortonworks.iotas.topology.component.rule.action.NotifierAction"
       }
    ]
}' "${catalogurl}/topologies/$topologyid/rules")

echo $out
ruleid=$(getId $out)

# --
# Create a windowed rule
# --
echo -e "\n------"
out=$(curl -s -X POST -H "Content-Type: application/json" -H "Cache-Control: no-cache" -d '{
    "name": "rule1",
    "description": "windowed rule test",
    "sql": "select max(temperature) from parsedTuplesStream where humidity > 90",
    "window": {
        "windowLength": {
          "class": ".Window$Duration",
          "durationMs": 500
        },
        "slidingInterval": {
          "class": ".Window$Duration",
          "durationMs": 500
        },
        "tsField": null,
        "lagMs": 0
     },
    "actions": [
      {
        "name": "hbasesink",
        "outputStreams": ["sink-stream"],
        "__type": "com.hortonworks.iotas.topology.component.rule.action.TransformAction"
      },
      {
        "name": "hdfssink",
        "outputStreams": ["sink-stream"],
        "__type": "com.hortonworks.iotas.topology.component.rule.action.TransformAction"
      }
    ]
}' "${catalogurl}/topologies/$topologyid/rules")

echo $out
windowruleid=$(getId $out)

# --
# Create Rule processor
# --
echo -e "\n------"
out=$(curl -s -X POST -H "Content-Type: application/json" -H "Cache-Control: no-cache"  -d '{
    "name": "RuleProcessor",
    "config": {
        "properties": {
            "rules": ['$ruleid','$windowruleid']
        }
    },
    "type": "RULE",
    "outputStreamIds": ['$streamid3']
}' "${catalogurl}/topologies/$topologyid/processors")


echo $out
ruleprocessorid=$(getId $out)

# --
# Create notification sink
# --
echo -e "\n------"
out=$(curl -s  -X POST -H "Content-Type: application/json" -H "Cache-Control: no-cache"  -d '{
        "name": "notificationsink",
        "type": "NOTIFICATION",
        "config": {
            "properties" : {
          "notifierName": "email_notifier",
          "jarFileName": "notifiers.jar",
          "className": "com.hortonworks.iotas.notification.notifiers.EmailNotifier",
          "properties": {
            "username": "hwemailtest@gmail.com",
            "password": "testing12",
            "host": "smtp.gmail.com",
            "port": 587,
            "starttls": true,
            "debug": true,
            "ssl": false,
            "auth": true,
            "protocol": "smtp"
          },
          "fieldValues": {
            "from": "hwemailtest@gmail.com",
            "to": "hwemailtest@gmail.com",
            "subject": "Testing email notifications",
            "contentType": "text/plain",
            "body": "default body"
          },
          "parallelism": 1
        }
        }
      }' "${catalogurl}/topologies/$topologyid/sinks")

echo $out
notificationsinkid=$(getId $out)

# --
# Kafka -> Parser
# --
echo -e "\n------"
curl -X POST -H "Content-Type: application/json" -H "Cache-Control: no-cache"  -d '{
    "fromId": '$sourceid',
    "toId": '$parserid',
    "streamGroupings": [{"streamId": '$streamid1', "grouping": "SHUFFLE"}]
}' "${catalogurl}/topologies/$topologyid/edges"

# --
# Parser -> Rule processor
# --
echo -e "\n------"
curl -X POST -H "Content-Type: application/json" -H "Cache-Control: no-cache"  -d '{
    "fromId": '$parserid',
    "toId": '$ruleprocessorid',
    "streamGroupings": [{"streamId": '$streamid2', "grouping": "SHUFFLE"}]
}' "${catalogurl}/topologies/$topologyid/edges"

# --
# Rule processor -> Notification
# --
echo -e "\n------"
curl -X POST -H "Content-Type: application/json" -H "Cache-Control: no-cache"  -d '{
    "fromId": '$ruleprocessorid',
    "toId": '$notificationsinkid',
    "streamGroupings": [{"streamId": '$streamid3', "grouping": "SHUFFLE"}]
}' "${catalogurl}/topologies/$topologyid/edges"

# --
# Deploy
# --
echo -e "\n------"
curl -X POST -H "Content-Type: application/json" -H "Cache-Control: no-cache" -d '' "${catalogurl}/topologies/$topologyid/actions/deploy"

echo -e "\n------"

