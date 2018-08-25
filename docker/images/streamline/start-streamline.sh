#!/usr/bin/env bash

STREAMLINE_CONFIG="${STREAMLINE_HOME}"/conf/streamline.yaml
STREAMLINE_BOOTSTRAP_STORAGE_DONE="${STREAMLINE_HOME}/bootstrap_storage.done"
STREAMLINE_BOOTSTRAP_DONE="${STREAMLINE_HOME}/bootstrap.done"

die() {
    echo "${@}"
    exit 1
}

# Push a new directory on to the bash directory stack, or exit with a failure message.
#
# $1: The directory push on to the directory stack.
must_pushd() {
    local target_dir="${1}"
    pushd -- "${target_dir}" &> /dev/null || die "failed to change directory to ${target_dir}"
}

# Pop a directory from the bash directory stack, or exit with a failure message.
must_popd() {
    popd &> /dev/null || die "failed to popd"
}

update_config() {
    local registry_url="${REGISTRY_URL}"
    local flink_home_dir="${FLINK_HOME_DIR}"
    local storm_home_dir="${STORM_HOME_DIR}"
    local db_type="${DB_TYPE}"
    local data_src_class_name="${DATA_SRC_CLASS_NAME}"
    local db_url="${DB_URL}"
    local db_user="${DB_USER}"
    local db_password="${DB_PASSWORD}"

    sed -r -i -e "s#(db.type:) \"(.*)\"#\1 \"$db_type\"#"                   \
        -e "s#(dataSourceClassName:) \"(.*)\"#\1 \"$data_src_class_name\"#" \
        -e "s#(dataSource.url:) \"(.*)\"#\1 \"$db_url\"#"                   \
        -e "s#(dataSource.user:) \"(.*)\"#\1 \"$db_user\"#"                 \
        -e "s#(dataSource.password:) \"(.*)\"#\1 \"$db_password\"#" \
        -e "s#(flinkHomeDir:) (.*)#\1 \"$flink_home_dir\"#" \
        -e "s#(stormHomeDir:) (.*)#\1 \"$storm_home_dir\"#" \
        -e "s#(schemaRegistryUrl:) \"(.*)\"#\1 \"$registry_url\"#" \
        "${STREAMLINE_CONFIG}"
}

run_bootstrap_storage() {
    must_pushd "${STREAMLINE_HOME}"/bootstrap
    echo "Bootstrap dir : " $PWD
    if [ ! -f $STREAMLINE_BOOTSTRAP_STORAGE_DONE ]; then
        "./bootstrap-storage.sh" create
        touch $STREAMLINE_BOOTSTRAP_STORAGE_DONE
    fi

    if [[ $? -ne 0 ]]; then
        die "Bootstrap storage script failed!"
    else
        echo "Bootstrap storage script succeeded!"
    fi
    must_popd
}


run_bootstrap() {
    must_pushd "${STREAMLINE_HOME}"/bootstrap
    echo "Bootstrap dir : " $PWD
    if [ ! -f $STREAMLINE_BOOTSTRAP_DONE ]; then
        "./bootstrap.sh" migrate
        touch $STREAMLINE_BOOTSTRAP_DONE
    fi

    if [[ $? -ne 0 ]]; then
        die "Bootstrap script failed!"
    else
        echo "Bootstrap script succeeded!"
    fi
    must_popd
}

run_streamline() {
    must_pushd "${STREAMLINE_HOME}"/bin
    "./streamline" start "${@}"
    if [[ $? -ne 0 ]]; then
        die "Starting the Streamline failed!"
    else
        echo "Streamline started successfully!"
    fi
    must_popd
}

update_config
run_bootstrap_storage
run_streamline "${@}"
echo "Waiting for Streamline server to start"
#let's wait for the streamline server to be up and running
until $(curl --output /dev/null --silent --head --fail http://localhost:8080); do
    printf '.'
    sleep 2
done
run_bootstrap
