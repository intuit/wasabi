#!/bin/sh

APPLICATION_INSTRUMENT="-javaagent:/vagrant/target/org.jacoco.agent-${jacoco.version}-runtime.jar=destfile=/vagrant/target/jacoco/jacoco-it.exec,append=false"
CONSOLE_LOG=wasabi-os-console.log
MAIN_JAR=/vagrant/target/wasabi-main-*-all.jar

JAVA_OPTIONS="-server -Xmx4096m \
  ${APPLICATION_INSTRUMENT} \
  -Dlogback.configurationFile=./logback.xml"
  
java ${JAVA_OPTIONS} -jar ${MAIN_JAR} 1>>${CONSOLE_LOG} 2>&1 &
