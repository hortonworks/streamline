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
if [ -z "${JAVA_HOME}" ]; then
  JAVA="java"
else
  JAVA="${JAVA_HOME}/bin/java"
fi

SCRIPT_RUNNER_MAIN_CLASS=org.apache.streamline.storage.tool.SQLScriptRunner
CLASSPATH=${BOOTSTRAP_DIR}/lib/storage-tool-0.1.0-SNAPSHOT.jar:

echo "Configuration file: ${CONFIG_FILE_PATH}"

SCRIPT_DIR="${BOOTSTRAP_DIR}/sql/<dbtype>"
FILE_OPT="-f ${SCRIPT_DIR}/drop_tables.sql -f ${SCRIPT_DIR}/create_tables.sql"

echo "Script files option: $FILE_OPT"
exec ${JAVA} -cp ${CLASSPATH} ${SCRIPT_RUNNER_MAIN_CLASS} -c ${CONFIG_FILE_PATH} ${FILE_OPT}
