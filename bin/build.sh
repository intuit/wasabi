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

profile_default=development
build_default=false
test_default=false

usage() {
  [ "$1" ] && echo "error: ${1}"

  cat << EOF

usage: `basename ${0}` [options]

options:
  -b | --build [ true | false ]  : build; default: ${build_default}
  -p | --profile [profile]       : build profile; default: ${profile_default}
  -t | --test [ true | false ]   : test; default: ${test_default}
  -h | --help                    : help message
EOF

  exit ${2:-0}
}

fromPom() {
  mvn ${WASABI_MAVEN} -f $1/pom.xml -P$2 help:evaluate -Dexpression=$3 -B \
    -Dorg.slf4j.simpleLogger.log.org.apache.maven.cli.transfer.Slf4jMavenTransferListener=error | \
    sed -n -e '/^\[.*\]/ !{ p; }'
}

optspec=":p:b:t:h-:"

while getopts "${optspec}" opt; do
  case "${opt}" in
    -)
      case "${OPTARG}" in
        profile) profile="${!OPTIND}"; OPTIND=$(( ${OPTIND} + 1 ));;
        profile=*) profile="${OPTARG#*=}";;
        build) build="${!OPTIND}"; OPTIND=$(( ${OPTIND} + 1 ));;
        build=*) build="${OPTARG#*=}";;
        test) test="${!OPTIND}"; OPTIND=$(( ${OPTIND} + 1 ));;
        test=*) test="${OPTARG#*=}";;
        help) usage;;
        *) [ "${OPTERR}" = 1 ] && [ "${optspec:0:1}" != ":" ] && echo "unknown option --${OPTARG}";;
      esac;;
    p) profile=${OPTARG};;
    b) build=${OPTARG};;
    t) test=${OPTARG};;
    h) usage;;
    :) usage "option -${OPTARG} requires an argument" 1;;
    \?) [ "${OPTERR}" != 1 ] || [ "${optspec:0:1}" = ":" ] && usage "unknown option -${OPTARG}" 1;;
  esac
done

profile=${profile:=${profile_default}}
build=${build:=${build_default}}
test=${test:=${test_default}}
module=main

[[ "${build}" != "true" && "${build}" != "false" ]] && usage "invalid build parameter" 1
[[ "${test}" != "true" && "${test}" != "false" ]] && usage "invalid test parameter" 1
[ ! -e ./modules/main/target/wasabi-main-*-SNAPSHOT-${profile}-all.jar ] && build_jar=true

if [[ "${build}" = true || "${test}" = true || "${build_jar}" = true ]]; then
  [ "${build}" = true ] && package=package
  [ ! -e ./modules/main/target/wasabi-main-*-SNAPSHOT-${profile}-all.jar ] && package=package
  [ "${test}" = true ] && tests="org.jacoco:jacoco-maven-plugin:prepare-agent findbugs:check test"

  mvn ${WASABI_MAVEN} -P${profile} clean ${tests:--Dmaven.test.skip=true} ${package} javadoc:aggregate || \
    usage "invalid: mvn ${WASABI_MAVEN} -P${profile} clean ${tests:--Dmaven.test.skip=true} ${package} javadoc:aggregate" 1
fi

artifact=$(fromPom ./modules/${module} ${profile} project.artifactId)
version=$(fromPom . ${profile} project.version)
home=./modules/${module}/target
id=${artifact}-${version}-${profile}

/bin/rm -rf ${home}/${id}
mkdir -p ${home}/${id}/bin ${home}/${id}/conf ${home}/${id}/lib ${home}/${id}/logs

cp ${home}/extra-resources/service/run ${home}/${id}/bin
cp ${home}/extra-resources/docker/wasabi/Dockerfile ${home}/${id}
cp ${home}/extra-resources/docker/wasabi/entrypoint.sh ${home}/${id}
cp ${home}/classes/logback-access.xml ${home}/${id}/conf
cp ${home}/classes/logback.xml ${home}/${id}/conf
cp ${home}/classes/metrics.properties ${home}/${id}/conf
cp ${home}/classes/web.properties ${home}/${id}/conf
cp ./modules/analytics/target/classes/analytics.properties ${home}/${id}/conf
cp ./modules/api/target/classes/api.properties ${home}/${id}/conf
cp ./modules/assignment/target/classes/assignment.properties ${home}/${id}/conf
cp ./modules/assignment/target/classes/ehcache.xml ${home}/${id}/conf
cp ./modules/auditlog/target/classes/auditlog.properties ${home}/${id}/conf
cp ./modules/authentication/target/classes/authentication.properties ${home}/${id}/conf
cp ./modules/authorization/target/classes/authorization.properties ${home}/${id}/conf
cp ./modules/database/target/classes/database.properties ${home}/${id}/conf
cp ./modules/email/target/classes/email.properties ${home}/${id}/conf
cp ./modules/event/target/classes/event.properties ${home}/${id}/conf
cp ./modules/eventlog/target/classes/eventlog.properties ${home}/${id}/conf
cp ./modules/export/target/classes/export.properties ${home}/${id}/conf
cp ./modules/repository-datastax/target/classes/cassandra_client_config.properties ${home}/${id}/conf
cp ./modules/repository-datastax/target/classes/repository.properties ${home}/${id}/conf
cp ./modules/user-directory/target/classes/userDirectory.properties ${home}/${id}/conf
cp ${home}/${id}-all.jar ${home}/${id}/lib

chmod 755 ${home}/${id}/bin/run
chmod 755 ${home}/${id}/entrypoint.sh
sed -i '' -e "s/chpst -u [^:]*:[^ ]* //" ${home}/${id}/bin/run 2>/dev/null
[ ! -e ./modules/ui/target/dist/scripts/wasabi.js ] && build_js=true

if [[ "${build}" = true || "${build_js}" = true ]]; then
  if [ "${WASABI_OS}" == "${WASABI_OSX}" ]; then
    brew list node
    if [[ $? -eq 1 ]]; then
      echo "Node.js is not installed. Installing Node.js packages..."
      brew install node
      npm install -g yo grunt-cli bower grunt-contrib-compass
      sudo gem install compass
    fi
  fi
  (cd ./modules/ui && npm install && bower install && grunt build)
fi

content=${home}/${id}/content/ui/dist

mkdir -p ${content}
cp -R ./modules/ui/dist/* ${content}
mkdir -p ${content}/swagger/swaggerjson
cp -R ./modules/swagger-ui/target/swaggerui/ ${content}/swagger
cp -R ./modules/api/target/generated/swagger-ui/swagger.json ${content}/swagger/swaggerjson
