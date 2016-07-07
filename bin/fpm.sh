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

usage () {
  echo "usage: `basename $0` [-n name] [-v version] [-p profile] [-h home] [-l log] [-t timestamp] [-m modules] [-d]"
  exit
}

export JAVA_HOME=${JAVA_HOME:-/usr/local/java}
export PATH=$JAVA_HOME/bin:$PATH
profile_default=development

fromPom() {
  case $# in
    2) mvn -f modules/$1/pom.xml help:evaluate -Dexpression=$2 | sed -n -e '/^\[.*\]/ !{ p; }';;
    3) mvn -f modules/$1/pom.xml help:evaluate -Dexpression=$2 | sed -n -e '/^\[.*\]/ !{ p; }' | \
         python -c "import xml.etree.ElementTree as ET; import sys; field = ET.parse(sys.stdin).getroot().find(\"$3\"); print (field.text if field != None else '')"
  esac
}

exitOnError() {
  echo "error cause: $1"

  exit 1
}

name=`fromPom . project.name`
version=`fromPom . project.version`
profile=${profile:=${profile_default}}
timestamp=`date -u "+%Y%m%d%H%M%S"`
modules=main
daemon=false

while getopts "n:v:p:i:h:l:t:m:d" option; do
  case "$option" in
    n) name="$OPTARG";;
    v) version="$OPTARG";;
    p) profile="$OPTARG";;
    h) home="$OPTARG";;
    l) log="$OPTARG";;
    t) timestamp="$OPTARG";;
    m) modules="$OPTARG";;
    d) daemon="true";;
    :) echo "Error: -$OPTARG requires an argument"
       usage
       exit 1
       ;;
    ?) echo "Error: unknown option -$OPTARG"
       usage
       exit 1
       ;;
  esac
done

for module in "$modules"; do
  name=`fromPom $module/. project.name`
  version=`fromPom $module/. project.version`
  email=`fromPom $module/. project.properties application.email`
  group=`fromPom $module/. project.groupId`
  id=$name-$version-$profile
  home=${home:-/usr/local/$id}
  log=${log:-/var/log/$id}
  deps=`fromPom $module/. project.properties application.dependencies`
  daemon=`fromPom $module/. project.properties application.daemon.enable`
  daemon_deps=`fromPom $module/. project.properties application.daemon.dependencies`

  echo "packaging service: $id"

  common="-s dir --force --debug --architecture noarch --name ${name}-${profile} --version ${version}\
    --iteration ${timestamp} --license \"Apache License v2.0 : http://www.apache.org/licenses/LICENSE-2.0\"\
    --vendor \"[tbd]\" --category application --provides ${name}-${profile}\
    --description \"${name}, ${version} [${profile}] ...\" --url https://github.com/intuit/wasabi\
    --maintainer ${email} --directories ${home} --directories ${log}"

  if [ "$daemon" = "true" ]; then
    common="$common    --directories /etc/service/${id}"
  fi

  resources="extra-resources/service/run=${home}/service/run\
    extra-resources/service/log/run=${home}/service/log/run\
    extra-resources/service/run=${home}/bin/run\
    ./classes/logback-access.xml=${home}/conf/logback-access.xml\
    ./classes/logback.xml=${home}/conf/logback.xml\
    ./classes/metrics.properties=${home}/conf/metrics.properties\
    ./classes/web.properties=${home}/conf/web.properties\
    ../../analytics/target/classes/analytics.properties=${home}/conf/analytics.properties\
    ../../api/target/classes/api.properties=${home}/conf/api.properties\
    ../../api/target/generated/swagger-ui/=${home}/content/ui/dist/swagger/swaggerjson\
    ../../assignment/target/classes/assignment.properties=${home}/conf/assignment.properties\
    ../../auditlog/target/classes/auditlog.properties=${home}/conf/auditlog.properties\
    ../../authentication/target/classes/authentication.properties=${home}/conf/authentication.properties\
    ../../authorization/target/classes/authorization.properties=${home}/conf/authorization.properties\
    ../../database/target/classes/database.properties=${home}/conf/database.properties\
    ../../email/target/classes/email.properties=${home}/conf/email.properties\
    ../../event/target/classes/event.properties=${home}/conf/event.properties\
    ../../eventlog/target/classes/eventlog.properties=${home}/conf/eventlog.properties\
    ../../export/target/classes/export.properties=${home}/conf/export.properties\
    ../../repository/target/classes/cassandra_experiments.properties=${home}/conf/cassandra_experiments.properties\
    ../../repository/target/classes/repository.properties=${home}/conf/repository.properties\
    ../../swagger-ui/target/swaggerui/=${home}/content/ui/dist/swagger\
    ../../user-directory/target/classes/userDirectory.properties=${home}/conf/userDirectory.properties\
    ${id}-all.jar=${home}/lib/${id}-all.jar"

  if [ "$daemon" = "true" ]; then
    resources="$resources    extra-resources/service/run=/etc/service/${id}/run"
  fi

  if [ ! -z "$deps" ]; then
    for dep in $deps; do
      depends="$depends --depends $dep"
    done
  fi

  if [ "$daemon" = "true" -a ! -z "$daemon_deps" ]; then
    for dep in $daemon_deps; do
      depends="$depends --depends $dep"
    done
  fi

  deb="-t deb --deb-no-default-config-files"
  rpm="-t rpm --rpm-os linux"
  scripts="--before-install extra-resources/service/[PKG]/before-install.sh\
    --after-install extra-resources/service/[PKG]/after-install.sh\
    --before-remove extra-resources/service/[PKG]/before-remove.sh\
    --after-remove extra-resources/service/[PKG]/after-remove.sh"

  # TODO: u/g fpm 1.4.0 to support deb
  # for pkg in "deb" "rpm"; do
  for pkg in "rpm"; do
    fpm="${!pkg} $common `echo $scripts | sed -e "s/\[PKG\]/${pkg}/g"` $depends $resources"
    (cd modules/$module/target; eval fpm $fpm) || exitOnError "failed to build rpm: $module"
  done
done
