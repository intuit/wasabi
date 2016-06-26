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
waittime=ping
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
  docker-machine status ${project} >/dev/null 2>&1 || docker-machine create -d virtualbox ${project}

  dms=$(docker-machine status ${project})

  if [ "${dms}" != "Running" ]; then
    docker-machine restart ${project} || usage "unable to run command: % docker-machine restart ${project}" 1
  fi
}

stop_docker() {
  dms=$(docker-machine status ${project})

  if [ "${dms}" == "Running" ]; then
    docker-machine stop ${project} || usage "unable to run command: % docker-machine stop ${project}" 1
  fi
}

start_container() {
  eval $(docker-machine env wasabi)
  docker network create --driver bridge ${docker_network} >/dev/null 2>&1

  cid=$(docker ps -aqf name=${1})

  if [ "${cid}" == "" ]; then
    eval "docker run --net=${docker_network} --name ${1} ${3} -d ${2} ${4}" || \
      usage "unable to run command: % docker run --name ${1} ${3} -d ${2} ${4}" 1
    # todo: ?better way? ... see about moving polling to the app-start
    beerMe 30
  elif [ "${cid}" != "running" ]; then
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
  else
    docker-machine rm -f ${project} >/dev/null 2>&1
    vboxmanage hostonlyif remove vboxnet0 >/dev/null 2>&1
  fi
}

start_wasabi() {
  start_docker

  id=$(fromPom modules/main development application.name)
  wcip=$(docker inspect --format "{{ .NetworkSettings.Networks.${docker_network}.IPAddress }}" ${project}-cassandra)
  wmip=$(docker inspect --format "{{ .NetworkSettings.Networks.${docker_network}.IPAddress }}" ${project}-mysql)
  mip=$(docker-machine ip ${project})

  remove_container ${project}-main

  if [ "${verify}" = true ] || ! [ docker inspect ${project}-main >/dev/null 2>&1 ]; then
    echo "${green}${project}: building${reset}"

    sed -i -e "s|\(http://\)localhost\(:8080\)|\1${mip}\2|g" modules/main/target/${id}/content/ui/dist/scripts/config.js 2>/dev/null;
    docker build -t ${project}-main:${USER}-$(date +%s) -t ${project}-main:latest modules/main/target/${id}
  fi

  echo "${green}${project}: starting${reset}"

  wenv="WASABI_CONFIGURATION=-DnodeHosts=${wcip} -Ddatabase.url.host=${wmip}"

#   fixme: try to reuse the start_container() method instead of 'docker run...' directly; currently a problem with quotes in ${wenv} being passed into container.
  docker run --net=${docker_network} --name ${project}-main -p 8080:8080 -p 8090:8090 -p 8180:8180 -e "${wenv}" -d ${project}-main

  if [[ "${waittime}" == "ping" ]]; then
    echo -ne "${green}chill'ax ${reset}"
    for trial in $(seq 1 20); do
      curl ${mip}:8080/api/v1/ping >/dev/null 2>&1
      status=$?
      [[ ${status} -eq 0 ]] && break
#      beerMe
      echo -ne "${green}\xF0\x9F\x8D\xBA ${reset}"
      sleep 3
    done
    [[ ${status} -ne 0 ]] && usage "\nGiving up on the ping. Wasabi might not be running!" 1
  else
    beerMe "${waittime}"
  fi

  cat << EOF

${green}
wasabi is operational:

  ui: % open http://${mip}:8080     note: sign in as admin/admin
  api: % curl -i http://${mip}:8080/api/v1/ping
  debug: attach debuger to ${mip}:8180
${reset}
EOF
}

start_cassandra() {
  start_docker
  start_container ${project}-cassandra ${cassandra} "--privileged=true -p 9042:9042 -p 9160:9160"

  [ "${verify}" = true ] && console_cassandra
}

console_cassandra() {
  wcip=$(docker inspect --format "{{ .NetworkSettings.Networks.${docker_network}.IPAddress }}" ${project}-cassandra)

  docker run --net=${docker_network} -it --rm ${cassandra} cqlsh ${wcip} || \
    usage "unable to run command: % docker run -it --rm ${cassandra} cqlsh ${wcip}" 1
}

start_mysql() {
  pwd=mypass

  start_docker
  start_container ${project}-mysql ${mysql} "-p 3306:3306 -e MYSQL_ROOT_PASSWORD=${pwd}"

  wmip=$(docker inspect --format "{{ .NetworkSettings.Networks.${docker_network}.IPAddress }}" ${project}-mysql)
  sql=$(cat << EOF
    create database if not exists ${project};
    grant all privileges on ${project}.* to 'readwrite'@'localhost' identified by 'readwrite';
    grant all on *.* to 'readwrite'@'%' identified by 'readwrite';
    flush privileges;
EOF
)

  docker run --net=${docker_network} -it --rm ${mysql} mysql -h${wmip} -P3306 -uroot -p${pwd} -e "${sql}" || \
    usage "unable to run command: % docker run --net=${docker_network} -it --rm ${mysql} mysql -h${wmip} -P3306 -uroot -p${pwd} -e \"${sql}\"" 1

  [ "${verify}" = true ] && console_mysql
}

console_mysql() {
  pwd=mypass
  wmip=$(docker inspect --format "{{ .NetworkSettings.Networks.${docker_network}.IPAddress }}" ${project}-mysql)

  docker run --net=${docker_network} -it --rm ${mysql} mysql -h${wmip} -P3306 -uroot -p${pwd} || \
    usage "unable to run command: % docker run --net=${docker_network} -it --rm ${mysql} mysql -h${wmip} -P3306 -uroot -p${pwd}" 1
}

status() {
  eval $(docker-machine env ${project}) 2>/dev/null
  docker-machine active 2>/dev/null | grep ${project} || usage "start ${project}" 1
  eval $(docker-machine env ${project})
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

eval $(docker-machine env ${project}) 2>/dev/null

for command in ${@:$OPTIND}; do
  case "${command}" in
    start) start_cassandra; start_mysql; start_wasabi;;
    start:*) commands=$(echo ${command} | cut -d':' -f 2)
      (IFS=','; for cmd in ${commands}; do start_${cmd}; done);;
    stop) stop_container ${project}-main; stop_container ${project}-cassandra; stop_container ${project}-mysql;
      stop_docker;;
    stop:*) commands=$(echo ${command} | cut -d':' -f 2)
      (IFS=','; for cmd in ${commands}; do stop_container ${project}-${cmd/${project}/main}; done);;
    console) console_cassandra; console_mysql;;
    console:*) commands=$(echo ${command} | cut -d':' -f 2)
      (IFS=','; for cmd in ${commands}; do console_${cmd}; done);;
    status) status;;
    remove) remove_container;;
    remove:*) commands=$(echo ${command} | cut -d':' -f 2)
      (IFS=','; for cmd in ${commands}; do remove_container ${project}-${cmd/${project}/main}; done);;
    "") usage "unknown command: ${command}" 1;;
    *) usage "unknown command: ${command}" 1;;
  esac
done