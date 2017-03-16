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

fromPom() {
  mvn ${WASABI_MAVEN} -f modules/$1/pom.xml help:evaluate -Dexpression=$2 | sed -n -e '/^\[.*\]/ !{ p; }'
}

exitOnError() {
  echo "error cause: $1"

  exit 1
}

name=`fromPom . project.name`
version=`fromPom . project.version`
profile=development
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
  email=`fromPom $module/. application.email`
  group=`fromPom $module/. project.groupId`
  id=$name-$version-$profile
  home=${home:-/usr/local/$id}
  log=${log:-/var/log/$id}
  deps=`fromPom $module/. application.dependencies`
  daemon=`fromPom $module/. application.daemon.enable`
  daemon_deps=`fromPom $module/. application.daemon.dependencies`

  echo "packaging service: $id"

  common="-s dir --force --debug --architecture noarch --name ${name}-${profile} --version ${version}\
    --iteration ${timestamp} --license APLv2.0 --vendor tbd --category application --provides ${name}-${profile}\
    --description ${name}-${version}-${profile} --url https://github.com/intuit/wasabi\
    --maintainer ${email}" # --directories ${home} --directories ${log}"

  if [ "$daemon" = "true" ]; then
    common="$common    --directories /etc/service/${id}"
  fi

  resources="modules/${module}/target/extra-resources/service/run=${home}/service/run\
    modules/${module}/target/extra-resources/service/log/run=${home}/service/log/run\
    modules/${module}/target/extra-resources/service/run=${home}/bin/run\
    modules/${module}/target/classes/logback-access.xml=${home}/conf/logback-access.xml\
    modules/${module}/target/classes/logback.xml=${home}/conf/logback.xml\
    modules/${module}/target/classes/logging.properties=${home}/conf/logging.properties\
    modules/${module}/target/classes/metrics.properties=${home}/conf/metrics.properties\
    modules/${module}/target/classes/web.properties=${home}/conf/web.properties\
    modules/analytics/target/classes/analytics.properties=${home}/conf/analytics.properties\
    modules/api/target/classes/api.properties=${home}/conf/api.properties\
    modules/api/target/generated/swagger-ui/=${home}/content/ui/dist/swagger/swaggerjson\
    modules/assignment/target/classes/assignment.properties=${home}/conf/assignment.properties\
    modules/assignment/target/classes/ehcache.xml=${home}/conf/ehcache.xml\
    modules/auditlog/target/classes/auditlog.properties=${home}/conf/auditlog.properties\
    modules/authentication/target/classes/authentication.properties=${home}/conf/authentication.properties\
    modules/authorization/target/classes/authorization.properties=${home}/conf/authorization.properties\
    modules/database/target/classes/database.properties=${home}/conf/database.properties\
    modules/email/target/classes/email.properties=${home}/conf/email.properties\
    modules/event/target/classes/event.properties=${home}/conf/event.properties\
    modules/eventlog/target/classes/eventlog.properties=${home}/conf/eventlog.properties\
    modules/export/target/classes/export.properties=${home}/conf/export.properties\
    modules/repository-datastax/target/classes/cassandra_client_config.properties=${home}/conf/cassandra_client_config.properties\
    modules/repository-datastax/target/classes/repository.properties=${home}/conf/repository.properties\
    modules/swagger-ui/target/swaggerui/=${home}/content/ui/dist/swagger\
    modules/user-directory/target/classes/userDirectory.properties=${home}/conf/userDirectory.properties\
    modules/${module}/target/${id}-all.jar=${home}/lib/${id}-all.jar"

  if [ "$daemon" = "true" ]; then
    resources="$resources    modules/${module}/target/extra-resources/service/run=/etc/service/${id}/run"
  fi

  if [ ! -z "$deps" -a ! "$deps" == "null object or invalid expression" ]; then
    for dep in $deps; do
      depends="$depends --depends $dep"
    done
  fi

  if [ "$daemon" = "true" -a ! -z "$daemon_deps" -a ! "$daemon_deps" == "null object or invalid expression" ]; then
    for dep in $daemon_deps; do
      depends="$depends --depends $dep"
    done
  fi

  deb="-t deb" # --deb-no-default-config-files"
  rpm="-t rpm --rpm-os linux"
  scripts="--before-install modules/${module}/target/extra-resources/service/[PKG]/before-install.sh\
    --after-install modules/${module}/target/extra-resources/service/[PKG]/after-install.sh\
    --before-remove modules/${module}/target/extra-resources/service/[PKG]/before-remove.sh\
    --after-remove modules/${module}/target/extra-resources/service/[PKG]/after-remove.sh"

   for pkg in "deb" "rpm"; do
    fpm="${!pkg} $common `echo $scripts | sed -e "s/\[PKG\]/${pkg}/g"` $depends $resources"
    if [ "${WASABI_OS}" == "${WASABI_OSX}" ] || [ "${WASABI_OS}" == "${WASABI_LINUX}" ]; then
      docker run -it -v `pwd`:/build --rm liuedy/centos-fpm fpm ${fpm} || exitOnError "failed to build rpm: $module"
    else
      eval fpm $fpm || exitOnError "failed to build rpm: $module"
    fi
  done
done
