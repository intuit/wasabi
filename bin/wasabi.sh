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

formulas=("bash" "cask" "git" "maven" "python" "ruby" "node" "docker" "docker-machine")
casks=("java" "vagrant" "virtualbox")
build_default=false
endpoint_default=localhost:8080
verify_default=false
sleep_default=30
red=`tput setaf 9`
green=`tput setaf 10`
reset=`tput sgr0`

usage() {
  [ "${1}" ] && echo "${red}error: ${1}${reset}"

  cat << EOF
${green}
usage: `basename ${0}` [options] [commands]

options:
  -b | --build [ true | false ]          : build; default: ${build_default}
  -e | --endpoint [ host:port ]          : api endpoint; default: ${endpoint_default}
  -v | --verify [ true | false ]         : verify installation configuration; default: ${verify_default}
  -s | --sleep [ sleep-time ]            : sleep/wait time in seconds; default: ${sleep_default}
  -h | --help                            : help message

commands:
  bootstrap                              : install dependencies
  start[:cassandra,mysql,wasabi]         : start all, cassandra, mysql, wasabi
  test                                   : test wasabi
  stop[:wasabi,cassandra,mysql]          : stop all, wasabi, cassandra, mysql
  resource[:ui,api,doc,cassandra,mysql]  : open resource api, javadoc, cassandra, mysql
  status                                 : display resource status
  remove[:wasabi,cassandra,mysql]        : remove all, wasabi, cassandra, mysql
  package                                : build deployable packages
  release[:start,finish]                 : promote release
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
    echo -ne "\xF0\x9F\x8D\xBA "
    sleep 3
    cntr=$(($cntr + 3))
  done

  echo ""
}

bootstrap() {
  if ! hash brew 2>/dev/null; then
    echo "${green}installing homebrew ...${reset}"

    ruby -e "$(curl -fsSL https://raw.githubusercontent.com/Homebrew/install/master/install)"

    echo "${green}installed homebrew${reset}"
  fi

  brew update
  brew doctor
  brew cleanup

  echo "${green}installing dependencies: ${formulas[@]} ${casks[@]} ...${reset}"

  for formula in "${formulas[@]}"; do
    [[ ! $(brew list ${formula} 2>/dev/null) ]] && brew install ${formula} || brew upgrade ${formula} 2>/dev/null
  done

  for cask in "${casks[@]}"; do
    [[ $(brew cask list ${cask} 2>/dev/null) ]] && brew cask uninstall --force ${cask} 2>/dev/null
    brew cask install --force ${cask}
  done

  npm config set prefix $(brew --prefix)

  for n in yo grunt-cli bower; do
    [[ ! $(npm -g list 2>/dev/null | grep ${n}) ]] && npm -g install ${n}
  done

  [[ ! $(gem list | grep compass) ]] && gem install compass

  echo "${green}installed dependencies: ${formulas[@]} ${casks[@]}${reset}"
}

start() {
  [ ${build} = "true" ] && ./bin/build.sh -b ${build} -t ${verify}

  ./bin/container.sh -v ${verify} start${1:+:$1}
}

test() {
  if [ ${endpoint} == ${endpoint_default} ]; then
    endpoint=$(docker-machine ip wasabi):8080
    [[ $? -ne 0 ]] && endpoint=${endpoint_default}
  fi

  sleepTime=${1:-sleep}
  cntr=0

  while (( cntr < ${sleepTime} )); do
    echo "${endpoint}/api/v1/ping"
    curl ${endpoint}/api/v1/ping >/dev/null 2>&1
    [[ $? -eq 0 ]] && break
    [[ ${cntr} < ${sleepTime} ]] && usage "unable to ping application: ${endpoint}/api/v1/ping" 1
    cntr=$(($cntr + 3))
    beerMe 3
  done

  [ ! -e ./modules/functional-test/target/wasabi-functional-test-*-SNAPSHOT-jar-with-dependencies.jar ] && \
    ./bin/build.sh -b true -t ${verify}

  # FIXME: derive usr/pwd from env
  mkdir test.log >/dev/null 2>&1
  (cd modules/functional-test/target;
    java -Dwasabi.api.server.name=${endpoint} -Dwasabi.user.name=admin -Dwasabi.user.password=admin \
      -classpath classes:`ls wasabi-functional-test-*-SNAPSHOT-jar-with-dependencies.jar` org.testng.TestNG \
      -d ../../../test.log classes/testng.xml)
}

resource() {
  for resource in $1; do
    case "${1}" in
      ui) [ ! -f ./modules/ui/dist/index.html ] && ./bin/build.sh
        ./bin/wasabi.sh status >/dev/null 2>&1 || ./bin/wasabi.sh start
        open http://$(docker-machine ip wasabi):8080/index.html;;
      api) [[ ! -f ./modules/swagger-ui/target/swaggerui/index.html || \
        ! -f ./modules/api/target/generated/swagger-ui/swagger.json ]] && ./bin/build.sh
        ./bin/wasabi.sh status >/dev/null 2>&1 || ./bin/wasabi.sh start:docker
        jip=$(docker-machine ip wasabi)
        ./bin/wasabi.sh remove:wasabi >/dev/null 2>&1
        profile=development
        module=main
        home=./modules/${module}/target
        artifact=$(fromPom ./modules/${module} ${profile} project.artifactId)
        version=$(fromPom . ${profile} project.version)
        id=${artifact}-${version}-${profile}
        content=${home}/${id}/content/ui/dist
        sed -i '' "s/localhost/${jip}/g" ${content}/swagger/swaggerjson/swagger.json
        sed -i '' "s/this.model.validatorUrl.*$/this.model.validatorUrl = null;/g" ${content}/swagger/swagger-ui.js
        ./bin/wasabi.sh start
        beerMe 6
        open http://$(docker-machine ip wasabi):8080/swagger/index.html;;
      doc) [ ! -f ./target/site/apidocs/index.html ] && ./bin/build.sh
        open ./target/site/apidocs/index.html;;
      mysql|cassandra) ./bin/wasabi.sh status 2>/dev/null | grep wasabi-${1} 1>/dev/null || ./bin/wasabi.sh start
        ./bin/container.sh console:${1};;
      *) usage "unknown command: ${command}" 1;;
    esac
  done
}

stop() {
  ./bin/container.sh stop${1:+:$1}
}

status() {
  ./bin/container.sh status
}

package() {
  profile=build

  ./bin/build.sh -b true -p ${profile}

  (export VAGRANT_CWD=./bin; vagrant up)
  (export VAGRANT_CWD=./bin; vagrant ssh -c "cd wasabi; mvn dependency:resolve")
  beerMe 10
  (export VAGRANT_CWD=./bin; vagrant ssh -c "cd wasabi; ./bin/fpm.sh -p ${profile}")

  # FIXME: move to modules/ui/build.sh
  version=$(fromPom . build project.version)
  # FIXME: server ip
  server="http://localhost:8080"
  home=$(fromPom . build application.home)
  name=wasabi-ui #$(fromPom main build application.name)
  api_name=$(fromPom . build application.name)
  user=$(fromPom ./modules/main build application.user)
  group=$(fromPom ./modules/main build application.group)
  content=$(fromPom ./modules/main build application.http.content.directory)
  ui_home=${home}/../${name}-${version}-${profile}

  (cd modules/ui; \
    mkdir -p target; \
    for f in app bower.json feedbackserver Gruntfile.js constants.json karma.conf.js karma-e2e.conf.js package.json test .bowerrc; do \
      cp -r ${f} target; \
    done; \
    sed -i '' -e "s|http://localhost:8080|${server}|g" target/constants.json 2>/dev/null; \
    sed -i '' -e "s|VERSIONLOC|${version}|g" target/app/index.html 2>/dev/null; \
    (cd target; \
      npm install; \
      bower install; \
      grunt clean; \
      grunt build --target=develop --no-color); \
#      grunt test); \
    cp -r build target; \
    for pkg in deb rpm; do \
      sed -i '' -e "s|\${application.home}|${home}|g" target/build/${pkg}/before-install.sh 2>/dev/null; \
      sed -i '' -e "s|\${application.name}|${api_name}|g" target/build/${pkg}/before-install.sh 2>/dev/null; \
      sed -i '' -e "s|\${application.user}|${user}|g" target/build/${pkg}/before-install.sh 2>/dev/null; \
      sed -i '' -e "s|\${application.group}|${group}|g" target/build/${pkg}/before-install.sh 2>/dev/null; \
      sed -i '' -e "s|\${application.ui.home}|${ui_home}|g" target/build/${pkg}/after-install.sh 2>/dev/null; \
      sed -i '' -e "s|\${application.http.content.directory}|${content}|g" target/build/${pkg}/after-install.sh 2>/dev/null; \
      sed -i '' -e "s|\${application.user}|${user}|g" target/build/${pkg}/after-install.sh 2>/dev/null; \
      sed -i '' -e "s|\${application.group}|${group}|g" target/build/${pkg}/after-install.sh 2>/dev/null; \
      sed -i '' -e "s|\${application.http.content.directory}|${content}|g" target/build/${pkg}/before-remove.sh 2>/dev/null; \
    done)

  (export VAGRANT_CWD=./bin; vagrant ssh -c "cd wasabi/modules/ui; ./bin/fpm.sh -n ${name} -v ${version} -p ${profile}")
  (export VAGRANT_CWD=./bin; vagrant halt)

  echo "deployable build packages:"

  find ./modules -type f \( -name "*.rpm" -or -name "*.deb" \)
}

release() {
  ./bin/release.sh ${1:+$1}
}

remove() {
  ./bin/container.sh remove${1:+:$1}
}

optspec=":b:e:f:p:v:s:h-:"

while getopts "${optspec}" opt; do
  case "${opt}" in
    -)
      case "${OPTARG}" in
        build) build="${!OPTIND}"; OPTIND=$(( ${OPTIND} + 1 ));;
        build=*) build="${OPTARG#*=}";;
        endpoint) endpoint="${!OPTIND}"; OPTIND=$(( ${OPTIND} + 1 ));;
        endpoint=*) endpoint="${OPTARG#*=}";;
        verify) verify="${!OPTIND}"; OPTIND=$(( ${OPTIND} + 1 ));;
        verify=*) verify="${OPTARG#*=}";;
        sleep) sleep="${!OPTIND}"; OPTIND=$(( ${OPTIND} + 1 ));;
        sleep=*) sleep="${OPTARG#*=}";;
        help) usage;;
        *) [ "${OPTERR}" = 1 ] && [ "${optspec:0:1}" != ":" ] && echo "unknown option --${OPTARG}";;
      esac;;
    b) build=${OPTARG};;
    e) endpoint=${OPTARG};;
    v) verify=${OPTARG};;
    s) sleep=${OPTARG};;
    h) usage;;
    :) usage "option -${OPTARG} requires an argument" 1;;
    \?) [ "${OPTERR}" != 1 ] || [ "${optspec:0:1}" = ":" ] && usage "unknown option -${OPTARG}" 1;;
  esac
done

[ $# -eq 0 ] && usage "unspecified command" 1

build=${build:=${build_default}}
endpoint=${endpoint:=${endpoint_default}}
verify=${verify:=${verify_default}}
sleep=${sleep:=${sleep_default}}

[[ $# -eq 0 ]] && usage

for command in ${@:$OPTIND}; do
  case "${command}" in
    bootstrap) bootstrap;;
    start) start;;
    start:*) commands=$(echo ${command} | cut -d ':' -f 2)
      (IFS=','; for cmd in ${commands}; do start ${cmd}; done);;
    test) test;;
    stop) stop;;
    stop:*) commands=$(echo ${command} | cut -d ':' -f 2)
      (IFS=','; for cmd in ${commands}; do stop ${cmd}; done);;
    resource) command="resource:ui,api,doc,casssandra,mysql";&
    resource:*) commands=$(echo ${command} | cut -d ':' -f 2)
      (IFS=','; for cmd in ${commands}; do resource ${cmd}; done);;
    status) status;;
    remove) remove;;
    remove:*) commands=$(echo ${command} | cut -d ':' -f 2)
      (IFS=','; for cmd in ${commands}; do remove ${cmd}; done);;
    package) package;;
    release) release;;
    release:*) commands=$(echo ${command} | cut -d ':' -f 2)
      (IFS=','; for cmd in ${commands}; do release ${cmd}; done);;
    "") usage "unknown command: ${command}" 1;;
    *) usage "unknown command: ${command}" 1;;
  esac
done
