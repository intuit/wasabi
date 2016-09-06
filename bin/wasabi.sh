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

formulas=("bash" "cask" "git" "git-flow-avh" "maven" "wget" "ruby" "node")
taps=("caskroom/cask")
casks=("java" "docker")
endpoint_default=localhost:8080
verify_default=false
sleep_default=30
red=`tput setaf 9`
green=`tput setaf 10`
reset=`tput sgr0`
export WASABI_OS=${WASABI_OS:-`uname -s`}
export WASABI_OSX="Darwin"
export WASABI_LINUX="Linux"

usage() {
  [ "${1}" ] && echo "${red}error: ${1}${reset}"

  cat << EOF
${green}
usage: `basename ${0}` [options] [commands]

options:
  -e | --endpoint [ host:port ]          : api endpoint; default: ${endpoint_default}
  -v | --verify [ true | false ]         : verify installation configuration; default: ${verify_default}
  -s | --sleep [ sleep-time ]            : sleep/wait time in seconds; default: ${sleep_default}
  -h | --help                            : help message

commands:
  bootstrap                              : install dependencies
  build                                  : build project
  clean                                  : clean build
  start[:cassandra,mysql,wasabi]         : start all, cassandra, mysql, wasabi
  test                                   : run the integration tests (needs a running wasabi)
  test[:module-name,...]                 : run the unit tests for the specified module(s) only
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
  if [ "${WASABI_OS}" == "${WASABI_OSX}" ]; then
    if ! hash brew 2>/dev/null; then
      echo "${green}installing homebrew ...${reset}"

      ruby -e "$(curl -fsSL https://raw.githubusercontent.com/Homebrew/install/master/install)"

      echo "${green}installed homebrew${reset}"
    fi

    brew update
    brew doctor
    brew cleanup

    echo "${green}installing dependencies: ${formulas[@]} ${taps[@]} ${casks[@]} ...${reset}"

    for formula in "${formulas[@]}"; do
      [[ ! $(brew list ${formula} 2>/dev/null) ]] && brew install ${formula} || brew upgrade ${formula} 2>/dev/null
    done

    for tap in "${taps[@]}"; do
      brew tap ${tap}
    done

    for cask in "${casks[@]}"; do
      [[ $(brew cask list ${cask} 2>/dev/null) ]] && brew cask uninstall --force ${cask} 2>/dev/null
      brew cask install --force ${cask}
    done

    npm config set prefix $(brew --prefix)

    echo "${green}installed dependencies: ${formulas[@]} ${taps[@]} ${casks[@]}${reset}"
  elif [ "${WASABI_OS}" == "${WASABI_LINUX}" ]; then
    echo "OS is Linux"
    if [ -f /etc/lsb-release ]; then
      . /etc/lsb-release
      DISTRO=$DISTRIB_ID
      DISTROVER=$DISTRIB_RELEASE
      if [ $DISTRO == "Ubuntu" ] && [ $DISTROVER == "16.04" ]; then
        echo "${green}Operating system Ubuntu 16.04${reset}"
      else
        echo "${red}Unsupported Linux distribution${reset}"
        exit 1
      fi
    fi

    #Install Maven
    sudo apt-get update
    sudo apt-get install -y maven

    #Install JAVA
    sudo apt-get install -y default-jdk
    sudo cp /etc/environment /tmp/environment
    sudo chmod 666 /tmp/environment
    sudo echo "JAVA_HOME=/usr/lib/jvm/java-8-openjdk-amd64/" >> /tmp/environment
    sudo cp /tmp/environment /etc/environment
    sudo rm -rf /tmp/environment

    #Install Nodejs
    curl -sL https://deb.nodesource.com/setup_6.x | sudo -E bash -
    sudo apt-get install -y nodejs
    sudo npm install -g bower
    sudo npm install -g grunt-cli
    npm config set prefix "/usr/local"

    #Install compass
    sudo apt-get install -y ruby
    sudo apt-get install -y ruby-compass

    #Install docker
    sudo apt-get install -y apt-transport-https ca-certificates
    sudo apt-key adv --keyserver hkp://p80.pool.sks-keyservers.net:80 --recv-keys 58118E89F3A912897C070ADBF76221572C52609D
    sudo echo "deb https://apt.dockerproject.org/repo ubuntu-xenial main" > /tmp/docker.list
    sudo cp /tmp/docker.list /etc/apt/sources.list.d/docker.list
    sudo rm -rf /tmp/docker.list
    sudo apt-get purge lxc-docker
    sudo apt-get update
    sudo apt-get install -y linux-image-extra-$(uname -r) linux-image-extra-virtual
    sudo apt-get install -y docker-engine

    sudo groupadd docker
    sudo usermod -aG docker $USER
    sudo usermod -aG docker root
    echo "${green}installed dependencies.${reset}"
  else
    echo "${green}FIXME: linux install of ( ${formulas[@]} ${taps[@]} ${casks[@]} ) not yet implemented${reset}"
  fi

  for n in yo grunt-cli bower; do
    [[ ! $(npm -g list 2>/dev/null | grep ${n}) ]] && npm -g install ${n}
  done

  [[ ! $(gem list | grep compass) ]] && gem install compass
}

build() {
  ./bin/build.sh -b ${1:-false} -t ${2:-false} -p ${3:-development}
}

clean() {
  mvn clean
  (cd modules/ui; grunt clean)
}

start() {
  ./bin/container.sh -v ${verify} start${1:+:$1}
}

test_api() {
  wget -q --spider --tries=20 --waitretry=3 http://${endpoint}/api/v1/ping
  [ $? -ne 0 ] && usage "unable to start" 1

  [ ! -e ./modules/functional-test/target/wasabi-functional-test-*-SNAPSHOT-jar-with-dependencies.jar ] && \
    build false ${verify}

  # FIXME: derive usr/pwd from env
  mkdir test.log >/dev/null 2>&1
  (cd modules/functional-test/target;
    java -Dapi.server.name=${endpoint} -Duser.name=admin -Duser.password=admin \
      -classpath classes:`ls wasabi-functional-test-*-SNAPSHOT-jar-with-dependencies.jar` org.testng.TestNG \
      -d ../../../test.log classes/testng.xml)
}

resource() {
  for resource in $1; do
    case "${1}" in
      ui) [ ! -f ./modules/ui/dist/index.html ] && build
        ./bin/wasabi.sh status >/dev/null 2>&1 || ./bin/wasabi.sh start
        open http://localhost:8080;;
      api) [[ ! -f ./modules/swagger-ui/target/swaggerui/index.html || \
        ! -f ./modules/api/target/generated/swagger-ui/swagger.json ]] && build
        ./bin/wasabi.sh status >/dev/null 2>&1 || ./bin/wasabi.sh start:docker
#        jip=localhost
        ./bin/wasabi.sh remove:wasabi >/dev/null 2>&1
        profile=development
        module=main
        home=./modules/${module}/target
        artifact=$(fromPom ./modules/${module} ${profile} project.artifactId)
        version=$(fromPom . ${profile} project.version)
        id=${artifact}-${version}-${profile}
        content=${home}/${id}/content/ui/dist
#        sed -i '' "s/localhost/${jip}/g" ${content}/swagger/swaggerjson/swagger.json
        # FIXME: this can fail after 'package' given the profile = build
        sed -i '' "s/this.model.validatorUrl.*$/this.model.validatorUrl = null;/g" ${content}/swagger/swagger-ui.js
        ./bin/wasabi.sh start
        beerMe 6
        open http://localhost:8080/swagger/index.html;;
      doc) [ ! -f ./target/site/apidocs/index.html ] && build
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

  build true ${verify} ${profile}

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

  ./bin/fpm.sh -n ${name} -v ${version} -p ${profile}

  (cd modules/ui; \
    mkdir -p target; \
    for f in app bower.json Gruntfile.js constants.json karma.conf.js karma-e2e.conf.js package.json test .bowerrc; do \
      cp -r ${f} target; \
    done; \
    sed -i '' -e "s|http://localhost:8080|${server}|g" target/constants.json 2>/dev/null; \
    sed -i '' -e "s|VERSIONLOC|${version}|g" target/app/index.html 2>/dev/null; \
    if [[ "${WASABI_OS}" == "${WASABI_OSX}" || "${WASABI_OS}" == "${WASABI_LINUX}" ]]; then \
#      (cd target; npm install; bower install; grunt clean); \
      (cd target; npm install; bower install --no-optional; grunt clean); \
    fi \
# fixme: shouldn't have to force or ignore tests \
#    (cd target; grunt build --target=develop --no-color; \
    (cd target; grunt build --force --target=develop --no-color; \
#      grunt test); \
    ); \
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
    done; \
    (cd target; ../bin/fpm.sh -n ${name} -v ${version} -p ${profile}))

  find . -type f \( -name "*.rpm" -or -name "*.deb" \) -exec mv {} ./target 2>/dev/null \;

  echo "deployable build packages:"

  find . -type f \( -name "*.rpm" -or -name "*.deb" \)
}

release() {
  echo "./bin/release.sh ${1:+$1}"
}

remove() {
  ./bin/container.sh remove${1:+:$1}
}

optspec=":b:e:f:p:v:s:h-:"

while getopts "${optspec}" opt; do
  case "${opt}" in
    -)
      case "${OPTARG}" in
        endpoint) endpoint="${!OPTIND}"; OPTIND=$(( ${OPTIND} + 1 ));;
        endpoint=*) endpoint="${OPTARG#*=}";;
        verify) verify="${!OPTIND}"; OPTIND=$(( ${OPTIND} + 1 ));;
        verify=*) verify="${OPTARG#*=}";;
        sleep) sleep="${!OPTIND}"; OPTIND=$(( ${OPTIND} + 1 ));;
        sleep=*) sleep="${OPTARG#*=}";;
        help) usage;;
        *) [ "${OPTERR}" = 1 ] && [ "${optspec:0:1}" != ":" ] && echo "unknown option --${OPTARG}";;
      esac;;
    e) endpoint=${OPTARG};;
    v) verify=${OPTARG};;
    s) sleep=${OPTARG};;
    h) usage;;
    :) usage "option -${OPTARG} requires an argument" 1;;
    \?) [ "${OPTERR}" != 1 ] || [ "${optspec:0:1}" = ":" ] && usage "unknown option -${OPTARG}" 1;;
  esac
done

[ $# -eq 0 ] && usage "unspecified command" 1

endpoint=${endpoint:=${endpoint_default}}
verify=${verify:=${verify_default}}
sleep=${sleep:=${sleep_default}}

[[ $# -eq 0 ]] && usage

for command in ${@:$OPTIND}; do
  case "${command}" in
    bootstrap) bootstrap;;
    build) build true;;
    clean) clean;;
    start) command="start:cassandra,mysql,wasabi";&
    start:*) commands=$(echo ${command} | cut -d ':' -f 2)
      (IFS=','; for command in ${commands}; do start ${command}; done);;
    test:*) commands=$(echo ${command} | cut -d ':' -f 2)
      (IFS=','; for command in ${commands}; do mvn "-Dtest=com.intuit.wasabi.${command/-/}.**" test -pl modules/${command} --also-make -DfailIfNoTests=false -q ; done);;
    test) test_api;;
    stop) command="stop:wasabi,mysql,cassandra";&
    stop:*) commands=$(echo ${command} | cut -d ':' -f 2)
      (IFS=','; for command in ${commands}; do stop ${command}; done);;
    resource) command="resource:ui,api,doc,casssandra,mysql";&
    resource:*) commands=$(echo ${command} | cut -d ':' -f 2)
      (IFS=','; for command in ${commands}; do resource ${command}; done);;
    status) status;;
    remove) command="remove:wasabi,cassandra,mysql";&
    remove:*) commands=$(echo ${command} | cut -d ':' -f 2)
      (IFS=','; for command in ${commands}; do remove ${command}; done);;
    package) package;;
    release) release;;
    release:*) commands=$(echo ${command} | cut -d ':' -f 2)
      (IFS=','; for command in ${commands}; do release ${command}; done);;
    "") usage "unknown command: ${command}" 1;;
    *) usage "unknown command: ${command}" 1;;
  esac
done
