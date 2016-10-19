#!/usr/bin/env bash
###############################################################################
# Copyright 2016 Intuit
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

endpoint_default=localhost:8080

usage() {
  [ "${1}" ] && echo "error: ${1}"

  cat << EOF

usage: `basename ${0}` [options] [commands]

options:
  -e | --endpoint [ host:port ]          : api endpoint; default: ${endpoint_default}
  -h | --help                            : help message

commands:
  create                     : create and start an experiment
  remove:<experimentUUID>    : remove the experiment with <experimentUUID>

EOF

  exit ${2:-0}
}

create() {
  #CREATE EXPERIMENT AND SAVE UUID TO BE PASSED INTO FUTURE CALLS
  uuid=`curl -s -u admin:admin -H'Content-Type: application/json' \
    -d '{"applicationName": "Demo_App", "samplingPercent": 1.0,  "label": "BuyButton", "description": "BuyButton experiment", "startTime": "2016-06-10T00:00:00-0000", "endTime": "2018-12-25T00:00:00-0000"}' $endpoint/api/v1/experiments | \

    python -mjson.tool | \

    grep '"id"' | \

    awk -F "\"" '{print $4}'`

  echo -e "\nUUID: $uuid\n"
  #RETRIEVE EXPERIMENT
  curl -s -u admin:admin $endpoint/api/v1/experiments/$uuid

  echo -e "\nExperiment UUID <experimentUUID>: $uuid\n"

  #CREATE 2 BUCKETS
  curl -s -u admin:admin -H 'Content-Type: application/json' \
    -d '{"label": "BucketA", "allocationPercent": 0.50, "isControl": "true","description": "green button control bucket","payload":"green"}' \
    $endpoint/api/v1/experiments/$uuid/buckets | python -mjson.tool

  curl -s -u admin:admin -H 'Content-Type: application/json' \
    -d '{"label": "BucketB", "allocationPercent": 0.50, "isControl": "false","description": "orange button bucket","payload":"orange"}' \
    $endpoint/api/v1/experiments/$uuid/buckets | python -mjson.tool

  #CHANGE EXPERIMENT FROM DRAFT TO RUNNING STATE
  curl -s -u admin:admin -H 'Content-Type: application/json' -X PUT -d '{"state": "RUNNING"}' $endpoint/api/v1/experiments/$uuid

}

remove() {
  [ ${1} ] && uuid=${1}

  #PAUSE EXPERIMENT
  curl -s -u admin:admin -H'Content-Type: application/json' -X PUT -d '{"state": "PAUSED"}' $endpoint/api/v1/experiments/$uuid

  #TERMINATE EXPERIMENT
  curl -s -u admin:admin -H'Content-Type: application/json' -X PUT -d '{"state": "TERMINATED"}' $endpoint/api/v1/experiments/$uuid

  #DELETE EXPERIMENT
  curl -s -u admin:admin -H'Content-Type: application/json' -X PUT -d '{"state": "DELETED"}' $endpoint/api/v1/experiments/$uuid
}

optspec=":e:h-:"

while getopts "${optspec}" opt; do
  case "${opt}" in
    -)
      case "${OPTARG}" in
        endpoint) endpoint="${!OPTIND}"; OPTIND=$(( ${OPTIND} + 1 ));;
        endpoint=*) endpoint="${OPTARG#*=}";;
        help) usage;;
        *) [ "${OPTERR}" = 1 ] && [ "${optspec:0:1}" != ":" ] && echo "unknown option --${OPTARG}";;
      esac;;
    h) usage;;
    :) usage "option -${OPTARG} requires an argument" 1;;
    \?) [ "${OPTERR}" != 1 ] || [ "${optspec:0:1}" = ":" ] && usage "unknown option -${OPTARG}" 1;;
  esac
done

[ $# -eq 0 ] && usage "unspecified command" 1

endpoint=${endpoint:=${endpoint_default}}

for command in ${@:$OPTIND}; do
  case "${command}" in
    "create") create;;
    remove:*) commands=$(echo ${command} | cut -d ':' -f 2)
      (IFS=','; for cmd in ${commands}; do remove ${cmd}; done);;
    "") usage "unknown command: ${command}" 1;;
    *) usage "unknown command: ${command}" 1;;
  esac
done
