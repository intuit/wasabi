#!/usr/bin/env bash

# note: so that we can pick up the mvn env
. ~/.bashrc

usage () {
  echo "usage: `basename $0` [-n name] [-v version] [-p profile] [-h home] [-l log] [-t timestamp] [-d directory]"
  exit
}

fromPom() {
  case $# in
    2) mvn -f modules/$1/pom.xml help:evaluate -Dexpression=$2 | sed -n -e '/^\[.*\]/ !{ p; }';;
    3) mvn -f modules/$1/pom.xml help:evaluate -Dexpression=$2 | sed -n -e '/^\[.*\]/ !{ p; }' | \
         python -c "import xml.etree.ElementTree as ET; import sys; field = ET.parse(sys.stdin).getroot().find(\"$3\"); print (field.text if field != None else '')"
  esac
}

name=wasabi.ui
#version=1.0.0
version=`fromPom main project.version`
email=`fromPom main project.properties application.email`
profile=development
timestamp=`date -u "+%Y%m%d%H%M%S"`
module=main
#dir=modules/$module/target
dir=./target

while getopts "n:v:p:i:h:l:t:" option; do
  case "$option" in
    n) name="$OPTARG";;
    v) version="$OPTARG";;
    p) profile="$OPTARG";;
    h) home="$OPTARG";;
    l) log="$OPTARG";;
    t) timestamp="$OPTARG";;
    d) dir="$OPTARG";;
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

id=${name}-${version}-${profile}
home=${home:-/usr/local/$id}
log=${log:-/var/log/$id}

echo "packaging service: $id"

common="-s dir --force --debug --architecture noarch --name ${name}-${profile} --version ${version}\
  --iteration ${timestamp} --license \"Apache License v2.0 : http://www.apache.org/licenses/LICENSE-2.0\"\
  --vendor \"You\"\ --category application --provides ${name}-${profile}\
  --description \"${name}, ${version} [${profile}] ...\" --url https://github.com/intuit/wasabi\
   --maintainer ${email} --directories ${home}"
resources="dist/=${home}/content/ui/dist"
deb="-t deb --deb-no-default-config-files"
rpm="-t rpm --rpm-os linux"
scripts="--before-install build/[PKG]/before-install.sh\
 --after-install build/[PKG]/after-install.sh\
 --before-remove build/[PKG]/before-remove.sh\
 --after-remove build/[PKG]/after-remove.sh"

(ls ${dir}) || mkdir ${dir}
(cd ${dir}; eval fpm ${common} ${rpm} ${resources})

for pkg in "deb" "rpm"; do
  fpm="${!pkg} $common `echo $scripts | sed -e "s/\[PKG\]/${pkg}/g"` $depends $resources"
  (cd target; eval fpm $fpm)
done
