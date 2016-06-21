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
    "fields": [{"name": "iotas.event", "type": "NESTED"} ]
}' "${catalogurl}/topologies/$topologyid/streams")

echo $out
streamid2=$(getId $out)

echo -e "\n------"
out=$(curl -s -X POST -H "Content-Type: application/json" -H "Cache-Control: no-cache"  -d '{
    "streamId": "rule_processsor_1.rule_1.1.notificationsink",
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
# Create Rule processor
# --
echo -e "\n------"
out=$(curl -s -X POST -H "Content-Type: application/json" -H "Cache-Control: no-cache"  -d '{
    "name": "RuleProcessor",
    "config": {
        "properties": {
            "rules": [
              {
                "name": "rule_1",
                "id": 1,
                "ruleProcessorName": "rule_processsor_1",
                "condition": {
                  "expression": {
                    "class": "com.hortonworks.iotas.topology.component.rule.condition.BinaryExpression",
                    "operator": "AND",
                    "first": {
                      "class": "com.hortonworks.iotas.topology.component.rule.condition.BinaryExpression",
                      "operator": "LESS_THAN",
                      "first": {
                        "class": "com.hortonworks.iotas.topology.component.rule.condition.FieldExpression",
                        "value": {
                          "name": "humidity",
                          "type": "STRING",
                          "optional": false
                        }
                      },
                      "second": {
                        "class": "com.hortonworks.iotas.topology.component.rule.condition.Literal",
                        "value": "1000"
                      }
                    },
                    "second": {
                      "class": "com.hortonworks.iotas.topology.component.rule.condition.BinaryExpression",
                      "operator": "GREATER_THAN",
                      "first": {
                        "class": "com.hortonworks.iotas.topology.component.rule.condition.FieldExpression",
                        "value": {
                          "name": "humidity",
                          "type": "STRING",
                          "optional": false
                        }
                      },
                      "second": {
                        "class": "com.hortonworks.iotas.topology.component.rule.condition.Literal",
                        "value": "10"
                      }
                    }
                  }
                },
                "actions": [
                  {
                    "name": "hbasesink"
                  },
                  {
                    "name": "hdfssink"
                  },
                  {
                    "name": "notificationsink",
                    "outputFieldsAndDefaults": {
                      "body": "rule_1 fired"
                    },
                    "includeMeta": true,
                    "notifierName": "email_notifier"
                  }
                ],
                "description": "rule_1_desc"
              }
              ]
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

