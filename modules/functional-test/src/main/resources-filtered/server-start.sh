###############################################################################
# Copyright 2017 Intuit
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
#!/usr/bin/env bash

APPLICATION_INSTRUMENT="-javaagent:/vagrant/target/org.jacoco.agent-${jacoco.version}-runtime.jar=destfile=/vagrant/target/jacoco-it.exec,append=false"
CONSOLE_LOG=wasabi-os-console.log
MAIN_JAR=/vagrant/target/wasabi-main-*-all.jar

JAVA_OPTIONS="-server -Xmx4096m \
  ${APPLICATION_INSTRUMENT} \
  -Dlogback.configurationFile=./logback.xml \
  -Djava.util.logging.config.file=./logging.properties"
  
  
java ${JAVA_OPTIONS} -jar ${MAIN_JAR} 1>>${CONSOLE_LOG} 2>&1 &
