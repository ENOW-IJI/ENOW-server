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

STORM_SRC_ROOT_DIR=$1

TRAVIS_SCRIPT_DIR=$( cd "$( dirname "${BASH_SOURCE[0]}" )" && pwd )

cd ${STORM_SRC_ROOT_DIR}
python ${TRAVIS_SCRIPT_DIR}/save-logs.py "install.txt" mvn clean install -DskipTests -P local --batch-mode
BUILD_RET_VAL=$?

exit ${BUILD_RET_VAL}
