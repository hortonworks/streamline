#!/bin/bash

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
  nohup $JAVA $STREAMLINE_HEAP_OPTS $STREAMLINE_JVM_PERFORMANCE_OPTS -cp $CLASSPATH $STREAMLINE_OPTS "com.hortonworks.streamline.webservice.StreamlineApplication" "server" "$@" > "$CONSOLE_OUTPUT_FILE" 2>&1 < /dev/null &
else
  exec $JAVA $STREAMLINE_HEAP_OPTS $STREAMLINE_JVM_PERFORMANCE_OPTS -cp $CLASSPATH $STREAMLINE_OPTS "com.hortonworks.streamline.webservice.StreamlineApplication" "server" "$@"
fi
