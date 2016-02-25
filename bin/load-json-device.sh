#!/usr/bin/env bash
curl -X POST -i -F parserJar=@../parsers/target/parsers-0.1.0-SNAPSHOT.jar -F parserInfo='{"id":1, "name":"SimpleJsonParser", "parserSchema":{"fields":[{"name":"field1", "type":"STRING"},{"name":"humidity", "type":"INTEGER"}]}, "className":"com.hortonworks.iotas.parsers.json.JsonParser","version":1}' http://localhost:8080/api/v1/catalog/parsers &&\
curl -X POST http://localhost:8080/api/v1/catalog/datasources --data @simple-json-datasource -H "Content-Type: application/json"
curl -X POST http://localhost:8080/api/v1/catalog/datasources --data @nested-json-datasource -H "Content-Type: application/json"
