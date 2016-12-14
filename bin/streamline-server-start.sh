#!/bin/bash
# Licensed to the Apache Software Foundation (ASF) under one or more
# contributor license agreements.  See the NOTICE file distributed with
# this work for additional information regarding copyright ownership.
# The ASF licenses this file to You under the Apache License, Version 2.0
# (the "License"); you may not use this file except in compliance with
# the License.  You may obtain a copy of the License at
#
#    http://www.apache.org/licenses/LICENSE-2.0
#
# Unless required by applicable law or agreed to in writing, software
# distributed under the License is distributed on an "AS IS" BASIS,
# WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
# See the License for the specific language governing permissions and
# limitations under the License.

if [ $# -lt 1 ];
then
        echo "USAGE: $0 [-daemon] streamline.yaml"
        exit 1
fi
base_dir=$(dirname $0)/..

if [ "x$STREAMLINE_HEAP_OPTS" = "x" ]; then
    export STREAMLINE_HEAP_OPTS="-Xmx1G -Xms1G"
fi

EXTRA_ARGS="-name StreamlineServer"

# create logs directory
if [ "x$LOG_DIR" = "x" ]; then
    LOG_DIR="$base_dir/logs"
fi

if [ ! -d "$LOG_DIR" ]; then
    mkdir -p "$LOG_DIR"
fi

# Exclude jars not necessary for running commands.
regex="\-(test|src|javadoc|runtime-storm).+(\.jar|\.jar\.asc)$"
should_include_file() {
    if [ "$INCLUDE_TEST_JARS" = true ]; then
        return 0
    fi
    file=$1
    if [ -z "$(echo "$file" | egrep "$regex")" ] ; then
        return 0
    else
        return 1
    fi
}

# classpath addition for release
for file in "$base_dir"/libs/*.jar;
do
    if should_include_file "$file"; then
        CLASSPATH="$CLASSPATH":"$file"
    fi
done

echo "CLASSPATH: ${CLASSPATH}"

COMMAND=$1
case $COMMAND in
  -name)
    DAEMON_NAME=$2
    CONSOLE_OUTPUT_FILE=$LOG_DIR/$DAEMON_NAME.out
    shift 2
    ;;
  -daemon)
    DAEMON_MODE=true
    shift
    ;;
  *)
    ;;
esac

# Which java to use
if [ -z "$JAVA_HOME" ]; then
  JAVA="java"
else
  JAVA="$JAVA_HOME/bin/java"
fi

# JVM performance options
if [ -z "$STREAMLINE_JVM_PERFORMANCE_OPTS" ]; then
  STREAMLINE_JVM_PERFORMANCE_OPTS="-server -XX:+UseParNewGC -XX:+UseConcMarkSweepGC -XX:+CMSClassUnloadingEnabled -XX:+CMSScavengeBeforeRemark -XX:+DisableExplicitGC -Djava.awt.headless=true"
fi

# Launch mode
if [ "x$DAEMON_MODE" = "xtrue" ]; then
  nohup $JAVA $STREAMLINE_HEAP_OPTS $STREAMLINE_JVM_PERFORMANCE_OPTS -cp $CLASSPATH $STREAMLINE_OPTS "org.apache.streamline.webservice.StreamlineApplication" "server" "$@" > "$CONSOLE_OUTPUT_FILE" 2>&1 < /dev/null &
else
  exec $JAVA $STREAMLINE_HEAP_OPTS $STREAMLINE_JVM_PERFORMANCE_OPTS -cp $CLASSPATH $STREAMLINE_OPTS "org.apache.streamline.webservice.StreamlineApplication" "server" "$@"
fi
