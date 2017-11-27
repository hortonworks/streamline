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


# defaults
bootstrap_dir=$(dirname $0)
CONFIG_FILE_PATH=${bootstrap_dir}/../conf/streamline.yaml
MIGRATION_SCRIPT_PATH=${bootstrap_dir}/shell

# Which java to use
if [ -z "${JAVA_HOME}" ]; then
  JAVA="java"
else
  JAVA="${JAVA_HOME}/bin/java"
fi

SHELL_MIGRATION_INITIALIZER_MAIN_CLASS=com.hortonworks.registries.storage.tool.shell.ShellMigrationInitializer
for file in "${bootstrap_dir}"/lib/*.jar;
do
    CLASSPATH="$CLASSPATH":"$file"
done

function execute {
    echo "Using Configuration file: ${CONFIG_FILE_PATH}"
    ${JAVA} -cp ${CLASSPATH} ${SHELL_MIGRATION_INITIALIZER_MAIN_CLASS} -c ${CONFIG_FILE_PATH} -s ${MIGRATION_SCRIPT_PATH} --${1}
}

function printUsage {
    cat <<-EOF
USAGE: $0 [migrate|info|validate|repair]
   migrate          : Applies all the pending migrations. Use "info" to see the current version and the pending migrations
   info             : Shows the list of migrations applied and the pending migration waiting to be applied on the target database
   validate         : Checks if the all the migrations haven been applied on the target database
   repair           : Repairs the SCRIPT_CHANGE_LOG table which is used to track all the migrations on the target database.
                      This involves removing entries for the failed migrations and update the checksum of migrations already applied on the target databsase.
EOF
}

if [ $# -gt 1 ]
then
    echo "More than one argument specified, please use only one of the below options"
    printUsage
    exit 1
fi

opt="$1"

case "${opt}" in
 migrate | info | validate | repair )
    execute "${opt}"
    ;;
*)
    printUsage
    exit 1
    ;;
esac