#!/usr/bin/env bash

# Resolve links - $0 may be a softlink
PRG="${0}"

while [ -h "${PRG}" ]; do
  ls=`ls -ld "${PRG}"`
  link=`expr "$ls" : '.*-> \(.*\)$'`
  if expr "$link" : '/.*' > /dev/null; then
    PRG="$link"
  else
    PRG=`dirname "${PRG}"`/"$link"
  fi
done

BOOTSTRAP_DIR=`dirname ${PRG}`
CONFIG_FILE_PATH=${BOOTSTRAP_DIR}/../conf/streamline.yaml

# Which java to use
if [ -z "$JAVA_HOME" ]; then
  JAVA="java"
else
  JAVA="$JAVA_HOME/bin/java"
fi

CONF_READER_MAIN_CLASS=org.apache.streamline.storage.tool.StorageProviderConfigurationReader
SCRIPT_RUNNER_MAIN_CLASS=org.apache.streamline.storage.tool.SQLScriptRunner
CLASSPATH=${BOOTSTRAP_DIR}/lib/storage-tool-0.1.0-SNAPSHOT.jar:

echo "Configuration file: ${CONFIG_FILE_PATH}"

CONF_READ_OUTPUT=`exec $JAVA -cp $CLASSPATH $CONF_READER_MAIN_CLASS $CONFIG_FILE_PATH`

# if it doesn't exit with code 0, just give up
if [ $? -ne 0 ]; then
  exit 1
fi

echo "JDBC connection informations: ${CONF_READ_OUTPUT}"

declare -a OUTPUT_ARRAY=(${CONF_READ_OUTPUT})

DB_TYPE=${OUTPUT_ARRAY[0]} 
JDBC_DRIVER_CLASS=${OUTPUT_ARRAY[1]} 
JDBC_URL=${OUTPUT_ARRAY[2]}

echo "DB TYPE: ${DB_TYPE}"
echo "JDBC_DRIVER_CLASS: ${JDBC_DRIVER_CLASS}"
echo "JDBC_URL: ${JDBC_URL}"

if [ $DB_TYPE="phoenix" ];
then
  DELIM="\n"
else
  DELIM=";"
fi

echo "Script delimiter: ${DELIM}"

SCRIPT_DIR="${BOOTSTRAP_DIR}/sql/${DB_TYPE}"
FILE_OPT="-f ${SCRIPT_DIR}/drop_tables.sql -f ${SCRIPT_DIR}/create_tables.sql"

echo "Script files option: $FILE_OPT"

exec $JAVA -cp $CLASSPATH $SCRIPT_RUNNER_MAIN_CLASS -c $JDBC_DRIVER_CLASS -u $JDBC_URL -d $DELIM $FILE_OPT
