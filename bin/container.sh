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

project=wasabi
cassandra=cassandra:2.1
mysql=mysql:5.6
docker_network=${project}_nw
verify_default=false
sleep_default=3
red=`tput setaf 9`
green=`tput setaf 10`
reset=`tput sgr0`

usage() {
  [ "${1}" ] && echo "${red}error: ${1}${reset}"

  cat << EOF
${green}
usage: `basename ${0}` [options] [commands]

options:
  -v | --verify [ true | false ]         : verify installation configuration; default: ${verify_default}
  -s | --sleep [ sleep-time ]            : sleep/wait time in seconds; default: ${sleep_default}
  -h | --help                            : help message

commands:
  start[:wasabi,cassandra,mysql,docker]  : start all, wasabi, cassandra, mysql, docker
  stop[:wasabi,cassandra,mysql,docker]   : stop all, wasabi, cassandra, mysql, docker
  console[:cassandra,mysql]              : console all, cassandra, mysql
  status                                 : status
  remove[:wasabi,cassandra,mysql]]       : remove all, wasabi, cassandra, mysql
${reset}
EOF

  exit ${2:-0}
}

fromPom() {
    mvn -f $1/pom.xml -P $2 help:evaluate -Dexpression=$3 | sed -n -e '/^\[.*\]/ !{ p; }'
}

beerMe() {
  sleepTime=${1:-sleep_default}
  cntr=0

  echo -ne "${green}chill'ax ${reset}"

  while (( cntr < ${sleepTime} )); do
    echo -ne "${green}\xF0\x9F\x8D\xBA ${reset}"
    sleep 3
    cntr=$(($cntr + 3))
  done

  echo ""
}

start_docker() {
  docker ps >/dev/null 2>&1
  [[ $? != 0 && "${WASABI_OS}" == "${wasabi_os_default}" ]] && open /Applications/Docker.app

  while :; do
    docker ps >/dev/null 2>&1
    [[ $? = 0 ]] && break
    beerMe 3
  done
}

stop_docker() {
  if [ "${WASABI_OS}" == "${wasabi_os_default}" ]; then
    osascript -e 'quit app "Docker"'
  fi
}

start_container() {
  docker network ls | grep ${docker_network} 1>/dev/null || \
    docker network create --driver bridge ${docker_network} >/dev/null 2>&1

  cid=$(docker ps -aqf name=${1})

  if [ "${cid}" == "" ]; then
    eval "docker run --net=${docker_network} --name ${1} ${3} -d ${2} ${4}" || \
      usage "docker run --net=${docker_network} --name ${1} ${3} -d ${2} ${4}" 1
    # todo: ?better way? ... see about moving polling to the app-start
    beerMe 30
  else
    cids=$(docker inspect --format '{{.State.Status}}' ${cid})

    if [ "${cids}" == "paused" ]; then
      op=unpause
    elif [ "${cids}" == "exited" ]; then
      op=restart
    fi

    if [ ! -z "${op}" ]; then
      docker ${op} ${cid} || usage "unable to run command: % docker ${op} ${cid}" 1

      while [ "${cids}" != "running" ]; do
        beerMe 1
        cids=$(docker inspect --format '{{.State.Status}}' ${cid})
      done

      if [ "${op}" == "restart" ]; then
        # todo: ?better way?
        beerMe 15
      fi
    fi
  fi
}

stop_container() {
  cid=$(docker ps -aqf name=${1})

  [ "${cid}" != "" ] && docker stop ${cid}
}


remove_container() {
  [ ${1} ] && container=${1}

  if [ ${container} ]; then
    stop_container ${container} >/dev/null 2>&1
    docker rm -fv ${container} >/dev/null 2>&1
  fi
}

start_wasabi() {
  start_docker

  id=$(fromPom modules/main development application.name)

  remove_container ${project}-main

  if [ "${verify}" = true ] || ! [ docker inspect ${project}-main >/dev/null 2>&1 ]; then
    echo "${green}${project}: building${reset}"

    docker build -t ${project}-main:$(git rev-parse --short=8 HEAD) -t ${project}-main:latest modules/main/target/${id}
  fi

  echo "${green}${project}: starting${reset}"

  start_container ${project}-main ${project}-main:$(git rev-parse --short=8 HEAD) "-p 8080:8080 -p 8090:8090 -p 8180:8180 -e WASABI_CONFIGURATION=\"-DnodeHosts=${project}-cassandra -Ddatabase.url.host=${project}-mysql\""
  echo -ne "${green}chill'ax ${reset}"

  status
}

start_cassandra() {
  start_docker
  start_container ${project}-cassandra ${cassandra} "--privileged=true -p 9042:9042 -p 9160:9160"

  [ "${verify}" = true ] && console_cassandra
}

console_cassandra() {
  docker run --net=${docker_network} -it --rm ${cassandra} cqlsh ${project}-cassandra || \
    usage "unable to run command: % docker run --net=${docker_network} -it --rm ${cassandra} cqlsh ${project}-cassandra" 1
}

start_mysql() {
  pwd=mypass

  start_docker
  start_container ${project}-mysql ${mysql} "-p 3306:3306 -e MYSQL_ROOT_PASSWORD=${pwd} -e MYSQL_DATABASE=${project} -e MYSQL_USER=readwrite -e MYSQL_PASSWORD=readwrite"

  [ "${verify}" = true ] && console_mysql
}

console_mysql() {
  docker run --net=${docker_network} -it --rm ${mysql} mysql -h${project}-mysql -P3306 -uroot -p${pwd} || \
    usage "unable to run command: % docker run --net=${docker_network} -it --rm ${mysql} mysql -h${project}-mysql -P3306 -uroot -p${pwd}" 1
}

status() {
  wget -q --spider --tries=20 --waitretry=3 http://localhost:8080/api/v1/ping
  [ $? -ne 0 ] && usage "not started" 1

  cat << EOF

${green}
wasabi is operational:

  ui: % open http://localhost:8080     note: sign in as admin/admin
  ping: % curl -i http://localhost:8080/api/v1/ping
  debug: attach to localhost:8180
${reset}
EOF

  docker ps 2>/dev/null
}

optspec=":f:p:v:s:h-:"

while getopts "${optspec}" opt; do
  case "${opt}" in
    -)
      case "${OPTARG}" in
        verify) verify="${!OPTIND}"; OPTIND=$(( ${OPTIND} + 1 ));;
        verify=*) verify="${OPTARG#*=}";;
        sleep) sleep="${!OPTIND}"; OPTIND=$(( ${OPTIND} + 1 ));;
        sleep=*) sleep="${OPTARG#*=}";;
        help) usage;;
        *) [ "${OPTERR}" = 1 ] && [ "${optspec:0:1}" != ":" ] && echo "unknown option --${OPTARG}";;
      esac;;
    v) verify=${OPTARG};;
    s) sleep=${OPTARG};;
    h) usage;;
    :) usage "option -${OPTARG} requires an argument" 1;;
    \?) [ "${OPTERR}" != 1 ] || [ "${optspec:0:1}" = ":" ] && usage "unknown option -${OPTARG}" 1;;
  esac
done

verify=${verify:=${verify_default}}
sleep=${sleep:=${sleep_default}}

[[ $# -eq 0 ]] && usage

for command in ${@:$OPTIND}; do
  case "${command}" in
    start) command="start:cassandra,mysql,wasabi";&
    start:*) commands=$(echo ${command} | cut -d':' -f 2)
      (IFS=','; for command in ${commands}; do start_${command}; done);;
    stop) command="stop:main,cassandra,mysql";&
    stop:*) commands=$(echo ${command} | cut -d':' -f 2)
      (IFS=','; for command in ${commands}; do stop_container ${project}-${command/${project}/main}; done);;
    console) command="console:cassandra,mysql";&
    console:*) commands=$(echo ${command} | cut -d':' -f 2)
      (IFS=','; for command in ${commands}; do console_${command}; done);;
    status) status;;
    remove) command="remove:wasabi,cassandra,mysql";&
    remove:*) commands=$(echo ${command} | cut -d':' -f 2)
      (IFS=','; for command in ${commands}; do remove_container ${project}-${command/${project}/main}; done);;
    "") usage "unknown command: ${command}" 1;;
    *) usage "unknown command: ${command}" 1;;
  esac
done
