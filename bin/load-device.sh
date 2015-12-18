#!/usr/bin/env bash
curl -X POST -i -F parserJar=@../parsers/target/parsers-0.1-SNAPSHOT.jar -F parserInfo='{"name":"NestParser","className":"com.hortonworks.iotas.parsers.nest.NestParser","version":0}' -F schemaFromParserJar=true http://localhost:8080/api/v1/catalog/parsers &&\
curl -X POST http://localhost:8080/api/v1/catalog/feeds --data @datafeed -H "Content-Type: application/json" &&\
curl -X POST http://localhost:8080/api/v1/catalog/deprecated/datasources --data @datasource -H "Content-Type: application/json" &&\
curl -X POST http://localhost:8080/api/v1/catalog/notifiers --data @console_notifier -H "Content-Type: application/json" &&\
curl -X POST http://localhost:8080/api/v1/catalog/notifiers --data @email_notifier -H "Content-Type: application/json"
curl -X POST http://localhost:8080/api/v1/catalog/topologies --data @topology -H "Content-Type: application/json" &&\
curl -X POST http://localhost:8080/api/v1/catalog/system/componentdefinitions/SOURCE --data @kafka-topology-component -H "Content-Type: application/json" &&\
curl -X POST http://localhost:8080/api/v1/catalog/system/componentdefinitions/PROCESSOR --data @rule-topology-component -H "Content-Type: application/json" &&\
curl -X POST http://localhost:8080/api/v1/catalog/system/componentdefinitions/PROCESSOR --data @parser-topology-component -H "Content-Type: application/json" &&\
curl -X POST http://localhost:8080/api/v1/catalog/system/componentdefinitions/SINK --data @hdfs-topology-component -H "Content-Type: application/json" &&\
curl -X POST http://localhost:8080/api/v1/catalog/system/componentdefinitions/SINK --data @hbase-topology-component -H "Content-Type: application/json" &&\
curl -X POST http://localhost:8080/api/v1/catalog/system/componentdefinitions/SINK --data @notification-topology-component -H "Content-Type: application/json" &&\
curl -X POST http://localhost:8080/api/v1/catalog/system/componentdefinitions/LINK --data @all-grouping-link-topology-component -H "Content-Type: application/json"
curl -X POST http://localhost:8080/api/v1/catalog/system/componentdefinitions/LINK --data @direct-grouping-link-topology-component -H "Content-Type: application/json"
curl -X POST http://localhost:8080/api/v1/catalog/system/componentdefinitions/LINK --data @shuffle-grouping-link-topology-component -H "Content-Type: application/json"
curl -X POST http://localhost:8080/api/v1/catalog/system/componentdefinitions/LINK --data @local-or-shuffle-grouping-link-topology-component -H "Content-Type: application/json"
curl -X POST http://localhost:8080/api/v1/catalog/system/componentdefinitions/LINK --data @fields-grouping-link-topology-component -H "Content-Type: application/json"
curl -X POST http://localhost:8080/api/v1/catalog/system/componentdefinitions/LINK --data @global-grouping-link-topology-component -H "Content-Type: application/json"
curl -X POST http://localhost:8080/api/v1/catalog/system/componentdefinitions/LINK --data @none-grouping-link-topology-component -H "Content-Type: application/json"
