#!/bin/bash

set -eo pipefail

# check to see if the superset config already exists, if it does skip to
# running the user supplied docker-entrypoint.sh, note that this means
# that users can copy over a prewritten superset config and that will be used
# without being modified
echo "Checking for existing Superset config..."
if [ ! -f $SUPERSET_HOME/superset_config.py ]; then
  echo "No Superset config found, creating from environment"
  touch $SUPERSET_HOME/superset_config.py

  cat > $SUPERSET_HOME/superset_config.py <<EOF
ROW_LIMIT = ${SUP_ROW_LIMIT}
WEBSERVER_THREADS = ${SUP_WEBSERVER_THREADS}
SUPERSET_WEBSERVER_PORT = ${SUP_WEBSERVER_PORT}
SUPERSET_WEBSERVER_TIMEOUT = ${SUP_WEBSERVER_TIMEOUT}
SECRET_KEY = '${SUP_SECRET_KEY}'
SQLALCHEMY_DATABASE_URI = '${SUP_META_DB_URI}'
CSRF_ENABLED = ${SUP_CSRF_ENABLED}
EOF
fi

# check for existence of /docker-entrypoint.sh & run it if it does
echo "Checking for docker-entrypoint"
if [ -f /docker-entrypoint.sh ]; then
  echo "docker-entrypoint found, running"
  chmod +x /docker-entrypoint.sh
  . docker-entrypoint.sh
fi

# set up Superset if we haven't already
if [ ! -f $SUPERSET_HOME/.setup-complete ]; then
  echo "Running first time setup for Superset"

  echo "Creating admin user ${ADMIN_USERNAME}"
  cat > $SUPERSET_HOME/admin.config <<EOF
${ADMIN_USERNAME}
${ADMIN_FIRST_NAME}
${ADMIN_LAST_NAME}
${ADMIN_EMAIL}
${ADMIN_PWD}
${ADMIN_PWD}
EOF

  /bin/sh -c '/usr/local/bin/fabmanager create-admin --app superset < $SUPERSET_HOME/admin.config'

  rm $SUPERSET_HOME/admin.config

  echo "Initializing database"
  superset db upgrade

  echo "Creating default roles and permissions"
  superset init

  touch $SUPERSET_HOME/.setup-complete
else
  # always upgrade the database, running any pending migrations
  superset db upgrade
fi

echo "Starting up Superset"
superset runserver -p 8088 -a 0.0.0.0 -t ${SUP_WEBSERVER_TIMEOUT}
