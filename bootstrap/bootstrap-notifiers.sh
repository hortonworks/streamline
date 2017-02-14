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

dir=$(dirname $0)/..
bootstrap_dir=$(dirname $0)
CATALOG_ROOT_URL="${1:-http://localhost:8080/api/v1/catalog}"

# Load Notifiers
echo "Adding Email notifier"
jarFile=${bootstrap_dir}/notifier-jars/streamline-notifier-*.jar
if [[ ! -f ${jarFile} ]]
then
  # try local build path
  jarFile=${dir}/streams/notifier/target/streamline-notifier-*.jar
  if [[ ! -f ${jarFile} ]]
  then
    echo "Could not find streamline-notifier jar, Exiting ..."
    exit 1
  fi
fi
curl -X POST "${CATALOG_ROOT_URL}/notifiers" -F notifierJarFile=@${jarFile} -F notifierConfig='{
  "name": "email_notifier.json",
  "description": "testing",
  "className": "com.hortonworks.streamline.streams.notifiers.EmailNotifier",
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
curl -X POST "${CATALOG_ROOT_URL}/notifiers" -F notifierJarFile=@${jarFile} -F notifierConfig='{
  "name": "console_notifier",
  "description": "testing",
  "className": "com.hortonworks.streamline.streams.notifiers.ConsoleNotifier"
};type=application/json'

echo
