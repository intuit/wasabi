#!/usr/bin/env bash

while ! nc -w 1 -z wasabi-cassandra 9042; do sleep 0.1; done
while ! nc -w 1 -z wasabi-mysql 3306; do sleep 0.1; done

MAIN=com.intuit.wasabi.Main
CLASSPATH=${WASABI_HOME}/conf:${WASABI_HOME}/wasabi.main.jar

JAVA_OPTIONS="
  ${WASABI_AGENT}\
  -verbose:gc -Xloggc:${WASABI_LOG_DIR}/gc.log\
  -XX:+PrintGCDetails -XX:+PrintGCTimeStamps -XX:+PrintGCApplicationStoppedTime\
  -XX:+PrintGCApplicationConcurrentTime -XX:+PrintTenuringDistribution\
  ${WASABI_JAVA_OPTIONS}
  "
echo "java ${JAVA_OPTIONS} -cp ${CLASSPATH} ${MAIN} 2>&1 1>${WASABI_LOG_DIR}/application.log"
java ${JAVA_OPTIONS} -cp ${CLASSPATH} ${MAIN} 2>&1 1>${WASABI_LOG_DIR}/application.log