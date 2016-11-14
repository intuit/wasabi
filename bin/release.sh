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

usage() {
  [ "${1}" ] && echo "error: ${1}"

  cat << EOF

usage: `basename ${0}` [options] [commands]

options:
  -h | --help  : help message

commands:
  start        : start release
  finish       : finish release
  feature      : feature branch release

EOF

  exit ${2:-0}
}

fromPom() {
  mvn ${WASABI_MAVEN} -f $1/pom.xml help:evaluate -Dexpression=$2 | sed -n -e '/^\[.*\]/ !{ p; }'
}

start() {
  version=$(fromPom . application.version).`date -u "+%Y%m%d%H%M%S"`

  echo "milestone version: $version"

  cp ./bin/git/hooks/* .git/hooks
  git flow release start -F $version
}

finish() {
  version=`git name-rev --name-only HEAD | sed -e 's/release\/\(.*\)/\1/g'`

  echo "releasing from release/$version to master..."

  git flow release finish -m "milestone: $version" -p -D $version
}

feature() {
  from_version=$(fromPom . project.version)
  to_version=$(fromPom . application.version).`date -u "+%Y%m%d%H%M%S"`-SNAPSHOT

  find . -name pom.xml -type f -not -path "*target*" \
    -exec sed -i '' -e "s#<version>${from_version}</version>#<version>${to_version}</version>#g" {} \; 2>/dev/null
}

optspec=":h-:"

while getopts "${optspec}" opt; do
  case "${opt}" in
    -)
      case "${OPTARG}" in
        help) usage;;
        *) [ "${OPTERR}" = 1 ] && [ "${optspec:0:1}" != ":" ] && echo "unknown option --${OPTARG}";;
      esac;;
    h) usage;;
    :) usage "option -${OPTARG} requires an argument" 1;;
    \?) [ "${OPTERR}" != 1 ] || [ "${optspec:0:1}" = ":" ] && usage "unknown option -${OPTARG}" 1;;
  esac
done

if [[ $# -eq 0 ]] ; then
  start
  finish
fi

for command in ${@:$OPTIND}; do
  case "${command}" in
    "start") start;;
    "finish") finish;;
    "feature") feature;;
    "") usage "unknown command: ${command}" 1;;
    *) usage "unknown command: ${command}" 1;;
  esac
done
