#!/bin/bash
#  Licensed under the Apache License, Version 2.0 (the "License");
#  you may not use this file except in compliance with the License.
#  You may obtain a copy of the License at
#
#    http://www.apache.org/licenses/LICENSE-2.0
#
#  Unless required by applicable law or agreed to in writing, software
#  distributed under the License is distributed on an "AS IS" BASIS,
#  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
#  See the License for the specific language governing permissions and
#  limitations under the License.

echo "Python version :  " `python -V 2>&1`
echo "Maven version  :  " `mvn -v`

SRC_ROOT_DIR=$1

TRAVIS_SCRIPT_DIR=$( cd "$( dirname "${BASH_SOURCE[0]}" )" && pwd )

cd ${SRC_ROOT_DIR}
  
# Travis only has 3GB of memory, lets use 1GB for build, and 1.5GB for forked JVMs
export MAVEN_OPTS="-Xmx1024m"

mvn --batch-mode test -fae 
BUILD_RET_VAL=$?

for dir in `find . -type d -and -wholename \*/target/\*-reports`;
do
echo "Looking for errors in ${dir}"
python ${TRAVIS_SCRIPT_DIR}/print-errors-from-test-reports.py "${dir}"
done

exit ${BUILD_RET_VAL}
