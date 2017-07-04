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
jarFile="$(find ${bootstrap_dir}/notifier-jars/ -name 'streamline-notifier-*.jar')"
if [[ ! -f ${jarFile} ]]
then
  # try local build path
  jarFile="$(find ${dir}/streams/notifier/target/ -name 'streamline-notifier-*.jar')"
  if [[ ! -f ${jarFile} ]]
  then
    echo "Could not find streamline-notifier jar, Exiting ..."
    exit 1
  fi
fi
curl -i --negotiate -u:anyUser  -b /tmp/cookiejar.txt -c /tmp/cookiejar.txt -X POST "${CATALOG_ROOT_URL}/notifiers" -F notifierJarFile=@${jarFile} -F notifierConfig='{
  "name": "Email Notifier",
  "description": "Sends email notifications",
  "className": "com.hortonworks.streamline.streams.notifiers.EmailNotifier",
  "builtin": true,
  "properties": {},
  "fieldValues": {}
};type=application/json'

echo

#echo "Adding Console notifier"
#curl -i --negotiate -u:anyUser  -b /tmp/cookiejar.txt -c /tmp/cookiejar.txt -X POST "${CATALOG_ROOT_URL}/notifiers" -F notifierJarFile=@${jarFile} -F notifierConfig='{
#  "name": "Console Notifier",
#  "description": "Logs messages to stdout",
#  "className": "com.hortonworks.streamline.streams.notifiers.ConsoleNotifier",
#  "builtin": true
#};type=application/json'

#echo
