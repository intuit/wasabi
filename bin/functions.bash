#@IgnoreInspection BashAddShebang

# Color lib
if [[ -t 1 ]]; then
  color_red=`tput bold; tput setaf 1`
  color_green=`tput setaf 2`
  color_blue=`tput setaf 4`
  color_cyan=`tput setaf 6`
  color_white=`tput bold; tput setaf 7`
  color_reset=`tput sgr0`
else
  # No color for non-tty IO
  color_red=''
  color_green=''
  color_blue=''
  color_cyan=''
  color_white=''
  color_reset=''
fi

function red(){
  echo -n $"${color_red}$@${color_reset}"
}
function green(){
  echo -n $"${color_green}$@${color_reset}"
}
function blue(){
  echo -n $"${color_blue}$@${color_reset}"
}
function cyan(){
  echo -n $"${color_cyan}$@${color_reset}"
}
function white(){
  echo -n $"${color_white}$@${color_reset}"
}

function die(){
  message=${1:-''}
  exit_code=${2:-1}
  exit_code=$(( $exit_code + 0 )) # cast return code to numeric

  [[ "${message}" ]] &&
    red $"$message"
    echo ""
  exit ${exit_code}
}

fromPom() {
    pom_root=$1
    mvn_profiles="$2"
    expression="$3"

    mvn ${MAVEN_FLAGS} \
      --file ${pom_root}/pom.xml \
      --activate-profiles ${mvn_profiles} \
      help:evaluate \
      --define expression=${expression} \
    | sed -n -e '/^\[.*\]/d'
}

sleepIter() {
  sleepIterations=${1:-sleep_default}
  counter=0

  green "Waiting…"

  while [[ counter -le ${sleepIterations} ]]; do
    cyan '.'
    sleep 1
    counter=$(( $counter + 1 ))
  done

  echo ''
}

build() {
  ./bin/build.sh -b ${1:-false} -t ${2:-false} -p ${3:-development}
}

clean() {
  mvn ${WASABI_MAVEN} clean
  cd modules/ui \
    && grunt clean
}

start() {
  ./bin/container.sh -v ${verify} -m ${migration} start${1:+:$1}
}

test_api() {
  green "Testing API…"
  wget -q --spider --tries=20 --waitretry=3 http://${endpoint}/api/v1/ping
  [[ $? -ne 0 ]] && die "Can't reach endpoint: http://${endpoint}/api/v1/ping" 1

  [[ ! -e ./modules/functional-test/target/wasabi-functional-test-*-SNAPSHOT-jar-with-dependencies.jar ]] \
    && build false ${buildtests}

  # FIXME: derive usr/pwd from env
  mkdir -p test.logs
  cd modules/functional-test/target \
    && classes=$(find . -name 'wasabi-functional-test-*-jar-with-dependencies.jar' -printf "%f:" | sed -e 's/:$//') \
    && java \
            -Dapi.server.name=${endpoint} \
            -Duser.name=admin \
            -Duser.password=admin \
            -classpath classes:${classes} \
            org.testng.TestNG \
            -d ../../../test.logs \
            classes/testng.xml
}

stop() {
  ./bin/container.sh stop${1:+:$1}
}

status() {
  ./bin/container.sh status
}
