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

MAVEN_JAR_URL_PATH=https://dev.mysql.com/get/Downloads/Connector-J/mysql-connector-java-5.1.40.zip

# Which java to use
if [ -z "${JAVA_HOME}" ]; then
  JAVA="java"
else
  JAVA="${JAVA_HOME}/bin/java"
fi

SCRIPT_RUNNER_MAIN_CLASS=com.hortonworks.streamline.storage.tool.SQLScriptRunner
for file in "${BOOTSTRAP_DIR}"/lib/*.jar;
do
    CLASSPATH="$CLASSPATH":"$file"
done

echo "Configuration file: ${CONFIG_FILE_PATH}"

SCRIPT_DIR="${BOOTSTRAP_DIR}/sql/<dbtype>"
FILE_OPT="-f ${SCRIPT_DIR}/drop_tables.sql -f ${SCRIPT_DIR}/create_tables.sql"

echo "Script files option: $FILE_OPT"
exec ${JAVA} -Dstreamline.bootstrap.dir=$BOOTSTRAP_DIR -cp ${CLASSPATH} ${SCRIPT_RUNNER_MAIN_CLASS} -c ${CONFIG_FILE_PATH} ${FILE_OPT} -m ${MAVEN_JAR_URL_PATH}
