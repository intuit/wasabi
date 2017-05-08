###############################################################################
# Copyright 2017 Intuit
#
# Licensed under the Apache License, Version 2.0 (the "License");
# you may not use this file except in compliance with the License.
# You may obtain a copy of the License at
#
#     http://www.apache.org/licenses/LICENSE-2.0
#
# Unless required by applicable law or agreed to in writing, software
# distributed under the License is distributed on an "AS IS" BASIS,
# WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
# See the License for the specific language governing permissions and
# limitations under the License.
###############################################################################
#!/usr/bin/env bash
# Script to run the Java/TestNG based functional tests for Wasabi
#
# Relying on
# 1: Maven's maven-assembly-plugin producing a jar with all dependencies
# 2: Maven filtering to substitue values for variables: application.http.host, application.http.port, version

# Reading command line args
while getopts ":s:h:p:n:" opt; do
   case $opt in
     s) api_server="$OPTARG"
     ;;
     h) proxy_host="$OPTARG"
     ;;
     p) proxy_port="$OPTARG"
     ;;
     n) non_proxy_hosts="$OPTARG"
     ;;
     \?) echo "Invalid option -$OPTARG" >&2
     ;;
   esac
done

# Prep
root=$( cd "$( dirname "${BASH_SOURCE[0]}" )" && cd .. && pwd ) # The dir above the location of this script
cd "$root"                                               # ..../modules/wasabi-functional-test/target
echo "wasabi-functional-test's root = $root"

archivedir=functional-test-archive                        # To collect files for Jenkins to archive
mkdir -p $archivedir

# Next lines will give error "bad substituion" if the vars are not substitueted by Maven,
# but script works with default values set below.
HOST=${application.http.host}
PORT=${application.http.port}
USER_NAME=${application.user.name}
USER_PWD=${application.user.password}
EMAIL=${application.user.email}
LASTNAME=${application.user.lastname}

HOST=${HOST:-"localhost"} 
PORT=${PORT:-"8080"}
USER_NAME=${USER_NAME:-"admin"}
USER_PWD=${USER_PWD:-"admin"}
EMAIL=${EMAIL:-"admin@example.com"}
LASTNAME=${LASTNAME:-"Admin"}

functional_test_jar=`ls wasabi-functional-test-*-jar-with-dependencies.jar`
outputdir=$archivedir/functional-testng-out
testngxml=classes/testng.xml
logxmldir=classes # Directory that has logback.xml configurration file

echo "functional_test_jar = $functional_test_jar"
echo "outputdir = $outputdir"
echo "archivedir = $archivedir"
echo "testngxml = $testngxml"
echo "logxmldir = $logxmldir"

echo "USER_NAME=${USER_NAME}"
echo "USER_PWD=${USER_PWD}"
echo "EMAIL=${EMAIL}"
echo "LASTNAME=${LASTNAME}"
if [[ -z "$api_server" ]]; then
   api_server=${HOST}:${PORT}
fi

command="java -Duser-name=${USER_NAME} -Dpassword=${USER_PWD} -Duser-email=${EMAIL} \
        -Duser-lastname=${LASTNAME} -Dapi.server.name=$api_server -Dhttp.proxyHost=$proxy_host \
        -Dhttp.proxyPort=$proxy_port -Dhttp.nonProxyHosts=$non_proxy_hosts -Duser.name=${USER_NAME} \
        -Duser.password=${USER_PWD} -classpath $logxmldir/:$functional_test_jar org.testng.TestNG -d $outputdir $testngxml"

# Run
echo Going to run $command
echo "command = $command"
$command

# Post process. Prep archive for jenkins
echo "Copying post process archive files for Jenkins"
cp ../*.xml $archivedir/
cp ./*.log $archivedir/
cp -rp apidocs $archivedir/
cp -rp scripts $archivedir/
cp classes/*xml $archivedir/
cp classes/config.properties $archivedir/
if test -e surefire-reports; then
    mv surefire-reports $archivedir/surefire-reports-tests-for-unit-tests
fi
