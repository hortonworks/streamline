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

SCRIPT_RUNNER_MAIN_CLASS=com.hortonworks.streamline.storage.tool.SQLScriptRunner
CLASSPATH=${BOOTSTRAP_DIR}/lib/storage-tool-0.1.0-SNAPSHOT.jar:

echo "Using Configuration file: ${CONFIG_FILE_PATH}"

function streamlineBootstrapStorage {
    SCRIPT_DIR="${BOOTSTRAP_DIR}/sql/<dbtype>"
    FILE_OPT="-f ${SCRIPT_DIR}/drop_tables.sql -f ${SCRIPT_DIR}/create_tables.sql"

    echo "Script files option: $FILE_OPT"
    exec ${JAVA} -cp ${CLASSPATH} ${SCRIPT_RUNNER_MAIN_CLASS} -c ${CONFIG_FILE_PATH} ${FILE_OPT}
}

function main {
    echo ""
    echo "===================================================================================="
    echo "Running bootstrap-storage will drop any existing streamline tables and re-create them."
    read -p "Are you sure you want to proceed. (y/n)? " yesorno
    
    case ${yesorno:0:1} in
        y|Y)
            streamlineBootstrapStorage;;
        * )
            exit;;
    esac
}

main
