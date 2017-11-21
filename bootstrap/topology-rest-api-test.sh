#!/usr/bin/env bash
#
# Copyright 2017 Hortonworks.
#
# Licensed under the Apache License, Version 2.0 (the "License");
# you may not use this file except in compliance with the License.
# You may obtain a copy of the License at

#   http://www.apache.org/licenses/LICENSE-2.0

# Unless required by applicable law or agreed to in writing, software
# distributed under the License is distributed on an "AS IS" BASIS,
# WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
# See the License for the specific language governing permissions and
# limitations under the License.


# --
# Creates and deploys a test topology using the topology REST APIs.
# --

function getId {
  str=$1
  echo $str | grep -o -E "\"id\":\d+" | head -n1 | cut -d : -f2
}


host=${1:-"localhost"}
port=${2:-"8080"}
catalogurl="http://$host:$port/api/v1/catalog"

echo "Catalog url: $catalogurl"

# --
# Create a topology
# --
echo -e "\n------"
out=$(curl -s -X POST -H "Content-Type: application/json" -H "Cache-Control: no-cache"  -d '{
    "name": "test",
    "config": "{\"local.parser.jar.path\": \"/tmp\", \"local.notifier.jar.path\": \"/tmp\"}"
}' "${catalogurl}/topologies")

echo $out
topologyid=$(getId $out)

# --
# Create streams
# --
echo -e "\n------"
out=$(curl -s -X POST -H "Content-Type: application/json" -H "Cache-Control: no-cache"  -d '{
    "streamId": "default",
    "fields": [{"name": "streamline.event", "type": "NESTED"} ]
}' "${catalogurl}/topologies/$topologyid/streams")

echo $out
streamid1=$(getId $out)

echo -e "\n------"
out=$(curl -s -X POST -H "Content-Type: application/json" -H "Cache-Control: no-cache"  -d '{
    "streamId": "parsedTuplesStream",
    "fields": [{"name": "temperature", "type": "LONG"}, {"name": "humidity", "type": "LONG"}]
}' "${catalogurl}/topologies/$topologyid/streams")

echo $out
parserStream=$(getId $out)

echo -e "\n------"
out=$(curl -s -X POST -H "Content-Type: application/json" -H "Cache-Control: no-cache"  -d '{
    "streamId": "sink-stream",
    "fields": [{"name": "streamline.event", "type": "NESTED"} ]
}' "${catalogurl}/topologies/$topologyid/streams")

echo $out
streamid3=$(getId $out)

echo -e "\n------Create normalization output stream"
out=$(curl -s -X POST -H "Content-Type: application/json" -H "Cache-Control: no-cache"  -d '{
    "streamId": "norm-output-stream",
    "fields": [{"name": "temperature", "type": "LONG"}, {"name": "humidity", "type": "LONG"}]
}' "${catalogurl}/topologies/$topologyid/streams")

echo $out
normOutputStreamId=$(getId $out)

# --
# Create kafka data source
# --
out=$(curl -s -X GET -H "Content-Type: application/json" -H "Cache-Control: no-cache" "${catalogurl}/streams/componentbundles/SOURCE?subType=KAFKA")
bundleId=$(getId $out)
echo -e "\n------ create kafka data source, bundle id: ${bundleId}"
out=$(curl -s -X POST -H "Content-Type: application/json" -H "Cache-Control: no-cache"  -d '{
    "name": "kafkaDataSource",
    "config": {
        "properties": {
            "topic": "nest-topic",
            "zkRoot": "/Streamline-kafka-spout",
            "spoutConfigId": "nest-kafka-spout-config",
            "zkUrl": "localhost:2181",
            "zkPath": "/brokers",
            "refreshFreqSecs": 60
        }
    },
    "topologyComponentBundleId": '"${bundleId}"',
    "outputStreamIds": ['"$streamid1"', '$parserStream']
}' "${catalogurl}/topologies/$topologyid/sources")

echo $out
sourceid=$(getId $out)

# --
# Create kinesis stream
# --
echo -e "\n------"
out=$(curl -s -X POST -H "Content-Type: application/json" -H "Cache-Control: no-cache"  -d '{
    "streamId": "kinesis",
    "fields": [{"name": "streamline.event", "type": "NESTED"} ]
}' "${catalogurl}/topologies/$topologyid/streams")

echo $out
kinesisstream=$(getId $out)


# --
# Create kinesis data source
# --
out=$(curl -s -X GET -H "Content-Type: application/json" -H "Cache-Control: no-cache" "${catalogurl}/streams/componentbundles/SOURCE?subType=KAFKA")
bundleId=$(getId $out)
echo -e "\n------ create Kinesis data source, bundle id: ${bundleId}"
out=$(curl -s -X POST -H "Content-Type: application/json" -H "Cache-Control: no-cache"  -d '{
    "name": "kinesisDataSource",
    "config": {
        "properties": {
            "streamName": "teststream",
            "shardIteratorType": "TRIM_HORIZON",
            "zkUrl": "localhost:2181",
            "zkPath": "/brokers",
            "region": "US_WEST_2"
        }
    },
    "topologyComponentBundleId": '"${bundleId}"',
    "outputStreamIds": ['"$kinesisstream"']
}' "${catalogurl}/topologies/$topologyid/sources")

echo $out
kinesisid=$(getId $out)


# --
# Create eventhub stream
# --
echo -e "\n------"
out=$(curl -s -X POST -H "Content-Type: application/json" -H "Cache-Control: no-cache"  -d '{
    "streamId": "eventhub",
    "fields": [{"name": "streamline.event", "type": "NESTED"} ]
}' "${catalogurl}/topologies/$topologyid/streams")

echo $out
eventhubstream=$(getId $out)

# --
# Create eventhub data source
# --
out=$(curl -s -X GET -H "Content-Type: application/json" -H "Cache-Control: no-cache" "${catalogurl}/streams/componentbundles/SOURCE?subType=KAFKA")
bundleId=$(getId $out)
echo -e "\n------ create Eventhub data source, bundle id: ${bundleId}"
out=$(curl -s -X POST -H "Content-Type: application/json" -H "Cache-Control: no-cache"  -d '{
    "name": "eventhubDataSource",
    "config": {
        "properties": {
            "username": "test",
            "password": "test",
            "namespace": "testNamespace",
            "entityPath": "foo",
            "partitionCount": 1,
            "zkConnectionString": "localhost:2181",
            "checkpointIntervalInSeconds": 60,
            "receiverCredits": 100,
            "maxPendingMsgsPerPartition": 100,
            "enqueueTimeFilter": 1000,
            "consumerGroupName": "group1"
        }
    },
    "topologyComponentBundleId": '"${bundleId}"',
    "outputStreamIds": ['"$eventhubstream"']
}' "${catalogurl}/topologies/$topologyid/sources")

echo $out
eventhubid=$(getId $out)

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
        "__type": "com.hortonworks.streamline.streams.layout.component.rule.action.TransformAction"
      },
      {
        "name": "hdfssink",
        "outputStreams": ["sink-stream"],
        "__type": "com.hortonworks.streamline.streams.layout.component.rule.action.TransformAction"
      },
      {
        "name": "notificationsink",
        "outputFieldsAndDefaults": {
          "body": "rule_1 fired"
         },
        "outputStreams": ["sink-stream"],
         "notifierName": "v001__email_notifier.json",
          "__type": "com.hortonworks.streamline.streams.layout.component.rule.action.NotifierAction"
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
    "sql": "select max(temperature), stddev(temperature) from parsedTuplesStream where humidity > 90",
    "window": {
        "windowLength": {
          "class": ".Window$Duration",
          "durationMs": 60000
        },
        "slidingInterval": {
          "class": ".Window$Duration",
          "durationMs": 60000
        },
        "tsField": null,
        "lagMs": 0
     },
    "actions": [
      {
        "name": "hbasesink",
        "outputStreams": ["sink-stream"],
        "__type": "com.hortonworks.streamline.streams.layout.component.rule.action.TransformAction"
      },
      {
        "name": "hdfssink",
        "outputStreams": ["sink-stream"],
        "__type": "com.hortonworks.streamline.streams.layout.component.rule.action.TransformAction"
      }
    ]
}' "${catalogurl}/topologies/$topologyid/rules")

echo $out
windowruleid=$(getId $out)

#--
# Create a rule by directly specifying the condition and input stream
# --
echo -e "\n------"
out=$(curl -s -X POST -H "Content-Type: application/json" -H "Cache-Control: no-cache" -d '{
    "name": "rule1",
    "description": "rule test",
    "streams": ["parsedTuplesStream"],
    "condition": "humidity < 10",
    "actions": [
      {
        "name": "hbasesink",
        "outputStreams": ["sink-stream"],
        "__type": "com.hortonworks.streamline.streams.layout.component.rule.action.TransformAction"
      },
      {
        "name": "hdfssink",
        "outputStreams": ["sink-stream"],
        "__type": "com.hortonworks.streamline.streams.layout.component.rule.action.TransformAction"
      },
      {
        "name": "notificationsink",
        "outputFieldsAndDefaults": {
          "body": "rule_1 fired"
         },
        "outputStreams": ["sink-stream"],
         "notifierName": "v001__email_notifier.json",
          "__type": "com.hortonworks.streamline.streams.layout.component.rule.action.NotifierAction"
       }
    ]
}' "${catalogurl}/topologies/$topologyid/rules")

echo "$out"
rulid2=$(getId $out)

#--
# Create a windowed rule by directly specifying the input stream and groupby keys
# --
echo -e "\n------"
out=$(curl -s -X POST -H "Content-Type: application/json" -H "Cache-Control: no-cache" -d '{
    "name": "rule3",
    "description": "windowed rule test",
    "projections": [
        {"expr": "humidity"},
        {"functionName": "max", "args": ["temperature"], "outputFieldName": "maxTemp"},
        {"functionName": "topN", "args": ["5", "temperature"], "outputFieldName": "topTwoTemp"}
    ],
    "streams": ["parsedTuplesStream"],
    "groupbykeys": ["humidity"],
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
        "__type": "com.hortonworks.streamline.streams.layout.component.rule.action.TransformAction"
      },
      {
        "name": "hdfssink",
        "outputStreams": ["sink-stream"],
        "__type": "com.hortonworks.streamline.streams.layout.component.rule.action.TransformAction"
      }
    ]
}' "${catalogurl}/topologies/$topologyid/windows")

echo "$out"
rulid3=$(getId $out)

# --
# Create Rule processor
# --
out=$(curl -s -X GET -H "Content-Type: application/json" -H "Cache-Control: no-cache" "${catalogurl}/streams/componentbundles/PROCESSOR?subType=RULE")
bundleId=$(getId $out)
echo -e "\n------ create Rule, bundle id: ${bundleId}"
out=$(curl -s -X POST -H "Content-Type: application/json" -H "Cache-Control: no-cache"  -d '{
    "name": "RuleProcessor",
    "config": {
        "properties": {
            "rules": ['$ruleid','$windowruleid','$rulid2']
        }
    },
    "topologyComponentBundleId": '"${bundleId}"',
    "outputStreamIds": ['$streamid3']
}' "${catalogurl}/topologies/$topologyid/processors")

echo $out
ruleprocessorid=$(getId $out)

# --
# Create Windowed processor
# --
out=$(curl -s -X GET -H "Content-Type: application/json" -H "Cache-Control: no-cache" "${catalogurl}/streams/componentbundles/PROCESSOR?subType=WINDOW")
bundleId=$(getId $out)
echo -e "\n------ create Windowed rule, bundle id: ${bundleId}"
out=$(curl -s -X POST -H "Content-Type: application/json" -H "Cache-Control: no-cache"  -d '{
    "name": "Windowed processor",
    "config": {
        "properties": {
            "rules": ['$rulid3']
        }
    },
    "topologyComponentBundleId": '"${bundleId}"',
    "outputStreamIds": ['$streamid3']
}' "${catalogurl}/topologies/$topologyid/processors")

echo $out
windowedruleprocessorid=$(getId $out)

# --
# Get the notifier details for email notifier
# --
notifierJar=$(curl -s "${catalogurl}/notifiers?name=v001__email_notifier.json" | grep -oE 'jarFileName\":"\S+?\"'|cut -d \" -f 3)
# --
# Create notification sink
# --
out=$(curl -s -X GET -H "Content-Type: application/json" -H "Cache-Control: no-cache" "${catalogurl}/streams/componentbundles/SINK?subType=NOTIFICATION")
bundleId=$(getId $out)
echo -e "\n------ create Notification, bundle id: ${bundleId}"
out=$(curl -s  -X POST -H "Content-Type: application/json" -H "Cache-Control: no-cache"  -d '{
        "name": "notificationsink",
        "topologyComponentBundleId": '"${bundleId}"',
        "config": {
            "properties" : {
          "notifierName": "v001__email_notifier.json",
          "jarFileName": "'$notifierJar'",
          "className": "com.hortonworks.streamline.streams.notifiers.EmailNotifier",
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


# ------------------------------------------------------------------------
# Normalization
# ------------------------------------------------------------------------
out=$(curl -s -X GET -H "Content-Type: application/json" -H "Cache-Control: no-cache" "${catalogurl}/streams/componentbundles/PROCESSOR?subType=NORMALIZATION")
bundleId=$(getId $out)
echo -e "\n---- Create Fine grained normalization processor, bundle id: ${bundleId} --"
out=$(curl -s -X POST -H "Content-Type: application/json" -H "Cache-Control: no-cache"  -d '{
    "name": "FinegrainedNormalizationProcessor",
    "config": {
        "properties": {
          "type": "fineGrained",
          "normalizationConfig": {
          "'$parserStream'": {
          "__type": "com.hortonworks.streamline.streams.layout.component.impl.normalization.FieldBasedNormalizationConfig",
            "transformers": [
              {
                "inputField": {
                  "name": "temp",
                  "type": "INTEGER",
                  "optional": false
                },
                "outputField": {
                  "name": "temperature",
                  "type": "FLOAT",
                  "optional": false
                },
                "converterScript": "new Float((temp-32)*5/9f)"
              }
            ],
            "fieldsToBeFiltered": [
              "Input-schema-field-1",
              "input-schema-field-2"
            ],
            "newFieldValueGenerators": [
              {
                "field": {
                  "name": "new-field",
                  "type": "STRING",
                  "optional": false
                },
                "script": "new-value-generator-script"
              }
            ]
          }
        }
      }
    },
    "topologyComponentBundleId": '"${bundleId}"',
    "outputStreamIds": ["'$normOutputStreamId'"]
}' "${catalogurl}/topologies/$topologyid/processors")

echo $out
fgNormProcId=$(getId $out)

out=$(curl -s -X GET -H "Content-Type: application/json" -H "Cache-Control: no-cache" "${catalogurl}/streams/componentbundles/PROCESSOR?subType=NORMALIZATION")
bundleId=$(getId $out)
echo -e "\n---- Create Bulk normalization processor, bundle id: ${bundleId} --"
out=$(curl -s -X POST -H "Content-Type: application/json" -H "Cache-Control: no-cache"  -d '{
    "name": "BulkNormalizationProcessor",
    "config": {
        "properties": {
          "type": "bulk",
          "normalizationConfig": {
            "'$parserStream'": {
            "__type": "com.hortonworks.streamline.streams.layout.component.impl.normalization.BulkNormalizationConfig",
              "normalizationScript": "Map<String, Object> result = new HashMap<>();return result;"
            }
          }
        }
    },
    "topologyComponentBundleId": '"${bundleId}"',
    "outputStreamIds": ["'$normOutputStreamId'"]
}' "${catalogurl}/topologies/$topologyid/processors")

echo $out
bulkNormProcId=$(getId $out)


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
# Kinesis -> Rule processor
# --
echo -e "\n------ Kinesis -> Rule processor"
curl -X POST -H "Content-Type: application/json" -H "Cache-Control: no-cache"  -d '{
    "fromId": '$kinesisid',
    "toId": '$ruleprocessorid',
    "streamGroupings": [{"streamId": '$kinesisstream', "grouping": "SHUFFLE"}]
}' "${catalogurl}/topologies/$topologyid/edges"

# --
# Eventhub -> Rule processor
# --
echo -e "\n------ Evenhub -> Rule processor"
curl -X POST -H "Content-Type: application/json" -H "Cache-Control: no-cache"  -d '{
    "fromId": '$eventhubid',
    "toId": '$ruleprocessorid',
    "streamGroupings": [{"streamId": '$eventhubstream', "grouping": "SHUFFLE"}]
}' "${catalogurl}/topologies/$topologyid/edges"

# --
# Deploy
# --
echo -e "\n------"
curl -X POST -H "Content-Type: application/json" -H "Cache-Control: no-cache" -d '' "${catalogurl}/topologies/$topologyid/actions/deploy"

echo -e "\n------"

