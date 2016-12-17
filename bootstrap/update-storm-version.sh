#!/usr/bin/env bash 

bootstrap_dir=$(dirname $0)
storm_version=""
if [[ $# -eq 1 ]]
then
  storm_version=$1
else
  echo "Script needs only one argument i.e. storm maven version"
  exit 1
fi
find $bootstrap_dir -name "*-component.json" | xargs sed -i.bak s/STORM_VERSION/$storm_version/g

