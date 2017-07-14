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
migration_default=false
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
  -m | --migration [ true | false ]      : refresh cassandra migration scripts; default: ${migration_default}
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
    mvn ${WASABI_MAVEN} -f $1/pom.xml -P $2 help:evaluate -Dexpression=$3 | sed -n -e '/^\[.*\]/ !{ p; }'
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
  [[ $? != 0 && "${WASABI_OS}" == "${WASABI_OSX}" ]] && open /Applications/Docker.app

  while :; do
    docker ps >/dev/null 2>&1
    [[ $? = 0 ]] && break
    beerMe 3
  done
}

stop_docker() {
  if [ "${WASABI_OS}" == "${WASABI_OSX}" ]; then
    osascript -e 'quit app "Docker"'
  fi
}

start_container() {
  # fix: do not re-create
  docker network create --driver bridge ${docker_network} >/dev/null 2>&1

  cid=$(docker ps -aqf name=${1})

  if [ "${cid}" == "" ]; then
    eval "docker run --net=${docker_network} --name ${1} ${3} -d ${2} ${4}" || \
      usage "docker run --net=${docker_network} --name ${1} ${3} -d ${2} ${4}" 1
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
  fi
}

start_wasabi() {
  start_docker

  id=$(fromPom modules/main development application.name)

  CASSANDRA_CONTAINER_NAME=${CASSANDRA_CONTAINER:-${project}-cassandra}
  IS_CONTAINER=`docker inspect -f {{.State.Running}} $CASSANDRA_CONTAINER_NAME`
  if [ "$IS_CONTAINER" = true ] ; then
    wcip=$(docker inspect --format "{{ .NetworkSettings.Networks.${docker_network}.IPAddress }}" ${CASSANDRA_CONTAINER_NAME})
  elif [ -z ${NODE_HOST} ]; then
    echo "[ERROR] Cassandra container must be running or NODE_HOST environment variable must be set"
    exit 1;
  else
    wcip=${NODE_HOST}
  fi

  MYSQL_CONTAINER_NAME=${MYSQL_CONTAINER:-${project}-mysql}
  IS_CONTAINER=`docker inspect -f {{.State.Running}} $MYSQL_CONTAINER_NAME`
  if [ "$IS_CONTAINER" = true ] ; then
    wmip=$(docker inspect --format "{{ .NetworkSettings.Networks.${docker_network}.IPAddress }}" ${MYSQL_CONTAINER_NAME})
  elif [ -z ${MYSQL_HOST} ]; then
    echo "[ERROR] Mysql container must be running or MYSQL_HOST environment variable must be set"
    exit 1;
  else
    wmip=${MYSQL_HOST}
  fi

  remove_container ${project}-main

  echo "${green}${project}: building${reset}"

# sed -i -e "s|\(http://\)localhost\(:8080\)|\1${mip}\2|g" modules/main/target/${id}/content/ui/dist/scripts/config.js 2>/dev/null;
  docker build -t ${project}-main:${USER}-$(date +%s) -t ${project}-main:latest modules/main/target/${id}

  echo "${green}${project}: starting${reset}"

  wenv="WASABI_CONFIGURATION=-DnodeHosts=${wcip} -Ddatabase.url.host=${wmip}"

#   fixme: try to reuse the start_container() method instead of 'docker run...' directly; currently a problem with quotes in ${wenv} being passed into container.
  docker run --net=${docker_network} --name ${project}-main -p 8080:8080 -p 8090:8090 -p 8180:8180 \
    -e "${wenv}" -d ${project}-main || \
    usage "docker run --net=${docker_network} --name ${project}-main -p 8080:8080 -p 8090:8090 -p 8180:8180 -e \"${wenv}\" -d ${project}-main" 1

  echo -ne "${green}chill'ax ${reset}"

  status
}

start_cassandra() {
  start_docker
  start_container ${project}-cassandra ${cassandra} "--privileged=true -p 9042:9042 -p 9160:9160"

  [ "${verify}" = true ] && console_cassandra

  CONTAINER_NAME=${CASSANDRA_CONTAINER:-${project}-cassandra}
  IS_CONTAINER=`docker inspect -f {{.State.Running}} $CONTAINER_NAME`

  if [ "$IS_CONTAINER" = true ] ; then
    echo "${green}${project}: [Start] creating keyspace and migration schemas${reset}"
    CURRENT_DIR="$(dirname "${BASH_SOURCE[0]}")"

    docker inspect wasabi-keyspace >/dev/null 2>&1
    IS_IMAGE_AVAILABLE=$?
    if [ "${migration}" = true ] || ! [ ${IS_IMAGE_AVAILABLE} -eq 0 ]; then
        echo "${green}${project}: [Start] Building wasabi keyspace image${reset}"
        docker build --force-rm --no-cache -t wasabi-keyspace:latest -f "${CURRENT_DIR}/./docker/cqlsh.docker" "${CURRENT_DIR}/./docker/"
    fi
    docker run -it --rm -e CASSANDRA_KEYSPACE_PREFIX=${project} -e CQLSH_HOST=${project}-cassandra -e CASSANDRA_PORT=9042 --net=${docker_network} --name wasabi_create_keyspace wasabi-keyspace

    docker inspect wasabi-migration >/dev/null 2>&1
    IS_IMAGE_AVAILABLE=$?
    if [ "${migration}" = true ] || ! [ ${IS_IMAGE_AVAILABLE} -eq 0 ]; then
        echo "${green}${project}: [Start] Building wasabi migration image${reset}"
        docker build --force-rm --no-cache -t wasabi-migration:latest -f "${CURRENT_DIR}/./docker/migration.docker" "${CURRENT_DIR}/../"
    fi
    docker run -it --rm -e CQLSH_HOST=${project}-cassandra -e CASSANDRA_PORT=9042 --net=${docker_network} --name wasabi_migration wasabi-migration
    echo "${green}${project}: [DONE] creating keyspace and migration schemas${reset}"
  else
    echo "[ERROR] Failed to start cassandra container, please check the logs"
    exit 1;
  fi


}

console_cassandra() {
  wcip=$(docker inspect --format "{{ .NetworkSettings.Networks.${docker_network}.IPAddress }}" ${project}-cassandra)

  docker run --net=${docker_network} -it --rm ${cassandra} cqlsh ${wcip} || \
    usage "unable to run command: docker run --net=${docker_network} -it --rm ${cassandra} cqlsh ${wcip}" 1
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

exec_commands_simple() {
  prefix=$1
  commands=$(echo $2 | cut -d ':' -f 2)
  (IFS=','; for command in ${commands}; do ${prefix}${command}; done)
}

exec_commands_project() {
  prefix=$1
  commands=$(echo $2 | cut -d ':' -f 2)
  (IFS=','; for command in ${commands}; do ${prefix} ${project}-${command/${project}/main}; done)
}


optspec=":f:p:v:m:s:h-:"

while getopts "${optspec}" opt; do
  case "${opt}" in
    -)
      case "${OPTARG}" in
        verify) verify="${!OPTIND}"; OPTIND=$(( ${OPTIND} + 1 ));;
        verify=*) verify="${OPTARG#*=}";;
        migration) migration="${!OPTIND}"; OPTIND=$(( ${OPTIND} + 1 ));;
        migration=*) migration="${OPTARG#*=}";;
        sleep) sleep="${!OPTIND}"; OPTIND=$(( ${OPTIND} + 1 ));;
        sleep=*) sleep="${OPTARG#*=}";;
        help) usage;;
        *) [ "${OPTERR}" = 1 ] && [ "${optspec:0:1}" != ":" ] && echo "unknown option --${OPTARG}";;
      esac;;
    v) verify=${OPTARG};;
    m) migration=${OPTARG};;
    s) sleep=${OPTARG};;
    h) usage;;
    :) usage "option -${OPTARG} requires an argument" 1;;
    \?) [ "${OPTERR}" != 1 ] || [ "${optspec:0:1}" = ":" ] && usage "unknown option -${OPTARG}" 1;;
  esac
done

verify=${verify:=${verify_default}}
migration=${migration:=${migration_default}}
sleep=${sleep:=${sleep_default}}

[[ $# -eq 0 ]] && usage

for command in ${@:$OPTIND}; do
  case "${command}" in
    start) exec_commands_simple start_ cassandra,mysql,wasabi;;
    start:*) exec_commands_simple start_ ${command};;
    stop) exec_commands_project stop_container main,cassandra,mysql;;
    stop:*) exec_commands_project stop_container ${command};;
    console) exec_commands_simple console_ cassandra,mysql;;
    console:*) exec_commands_simple console_ ${command};;
    status) status;;
    remove) exec_commands_project remove_container wasabi,cassandra,mysql;;
    remove:*) exec_commands_project remove_container ${command};;
    "") usage "unknown command: ${command}" 1;;
    *) usage "unknown command: ${command}" 1;;
  esac
done
