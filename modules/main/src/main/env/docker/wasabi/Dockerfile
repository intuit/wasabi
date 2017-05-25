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
from centos

ENV WASABI_MODULE wasabi-main
ENV WASABI_UI_MODULE wasabi-ui-main
ENV WASABI_PORT 8080
ENV WASABI_JMX_PORT 8090
ENV WASABI_DEBUG_PORT 8180

ENV WASABI_SRC_DIR ${application.name}
ENV WASABI_HOME /usr/local/${application.name}

ENV WASABI_JAVA_OPTIONS ""
ENV JDK_MAJOR_VERSION 8u131
ENV JDK_MINOR_VERSION b11
ENV JDK_VERSION ${JDK_MAJOR_VERSION}-${JDK_MINOR_VERSION}

RUN yum -y update && yum install -y wget

RUN wget --no-check-certificate --no-cookies --header "Cookie: oraclelicense=accept-securebackup-cookie" http://download.oracle.com/otn-pub/java/jdk/${JDK_VERSION}/d54c1d3a095b4ff2b6607d096fa80163/jdk-${JDK_MAJOR_VERSION}-linux-x64.rpm \
	&& rpm -ivh jdk-${JDK_MAJOR_VERSION}-linux-x64.rpm && rm jdk-${JDK_MAJOR_VERSION}-linux-x64.rpm

COPY ./ ${WASABI_HOME}/
COPY entrypoint.sh /usr/local/bin/
RUN sed -i -e $'s/1>>.*2>&1//' ${WASABI_HOME}/bin/run 2>/dev/null;

EXPOSE ${WASABI_PORT}
EXPOSE ${WASABI_JMX_PORT}
EXPOSE ${WASABI_DEBUG_PORT}

ENTRYPOINT ["entrypoint.sh"]
CMD ["wasabi"]