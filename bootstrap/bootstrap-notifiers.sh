#!/usr/bin/env bash

dir=$(dirname $0)/..
host="${1:-localhost}"
port="${2:-8080}"

# Load Notifiers
echo "Adding Email notifier"
jarFile=./notifier-jars/streamline-notifier-0.1.0-SNAPSHOT.jar
if [[ ! -f ${jarFile} ]]
then
  # try local build path
  jarFile=${dir}/streams/notifier/target/streamline-notifier-0.1.0-SNAPSHOT.jar
  if [[ ! -f ${jarFile} ]]
  then
    echo "Could not find streamline-notifier jar, Exiting ..."
    exit 1
  fi
fi
curl -X POST "http://${host}:${port}/api/v1/catalog/notifiers" -F notifierJarFile=@${jarFile} -F notifierConfig='{
  "name": "email_notifier.json",
  "description": "testing",
  "className": "org.apache.streamline.streams.notifiers.EmailNotifier",
  "properties": {
    "username": "hwemailtest@gmail.com",
    "password": "testing12",
    "host": "smtp.gmail.com",
    "port": "587",
    "starttls": "true",
    "debug": "true"
  },
  "fieldValues": {
    "from": "hwemailtest@gmail.com",
    "to": "hwemailtest@gmail.com",
    "subject": "Testing email notifications",
    "contentType": "text/plain",
    "body": "default body"
  }
};type=application/json'

echo

echo "Adding Console notifier"
curl -X POST "http://${host}:${port}/api/v1/catalog/notifiers" -F notifierJarFile=@${jarFile} -F notifierConfig='{
  "name": "console_notifier",
  "description": "testing",
  "className": "org.apache.streamline.streams.notifiers.ConsoleNotifier"
};type=application/json'

echo
