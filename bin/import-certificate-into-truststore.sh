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

# Read secret string
read_secret()
{
  # Save the old setting
  oldtty=`stty -g`

  # Disable echo.
  stty -echo

  # Set up trap to ensure echo is enabled before exiting if the script
  # is terminated while echo is disabled.
  trap 'stty ${oldtty}' EXIT

  # Read secret.
  read "$@"

  # Restore setting
  stty ${oldtty}
  trap - EXIT

  # Print a newline because the newline entered by the user after
  # entering the passcode is not echoed. This ensures that the
  # next line of output begins at a new line.
  echo
}

if [ $# -lt 3 ]; then
  echo "Usage: [host] [port] [truststore path] (truststore password)"
  exit 1
fi

HOST=$1
PORT=$2
TRUSTSTORE_FILE=$3

if [ $# -lt 4 ]; then
  printf "Password: "
  read_secret TRUSTSTORE_PASS
else
  TRUSTSTORE_PASS=$4
fi

which openssl > /dev/null 2>&1
if [ $? -ne 0 ]; then
  echo "openssl doesn't exist in PATH. Please install openssl and register to PATH."
  exit 1
fi

# Which java to use
if [ -z "${JAVA_HOME}" ]; then
  KEYTOOL="keytool"
else
  KEYTOOL="${JAVA_HOME}/bin/keytool"
fi

CERT_FILE="${HOST}.cert"

echo "Getting certificate for ${HOST}..."
openssl s_client -connect ${HOST}:${PORT} </dev/null \
    | sed -ne '/-BEGIN CERTIFICATE-/,/-END CERTIFICATE-/p' > ${CERT_FILE}

grep "BEGIN CERTIFICATE" ${CERT_FILE} > /dev/null 2>&1
if [ $? -ne 0 ]; then
  echo "Fail to get certificate for ${HOST}... Please check certification file ${CERT_FILE}"
  exit 2
fi

echo "Importing certificate into truststore ${TRUSTSTORE_FILE} ..."
${KEYTOOL} -import -noprompt -trustcacerts \
    -alias ${HOST} -file ${CERT_FILE} \
    -keystore ${TRUSTSTORE_FILE} -storepass ${TRUSTSTORE_PASS}

echo "Verifying import..."
${KEYTOOL} -list -v -keystore ${TRUSTSTORE_FILE} -storepass ${TRUSTSTORE_PASS} -alias ${HOST}

echo "Deleting downloaded certificate file..."
rm -f ${CERT_FILE}