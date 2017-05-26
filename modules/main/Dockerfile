FROM felixgao/jre8-jacoco:8u92-0.7.6

ENV WASABI_HOME /usr/local/jabba
ENV WASABI_LOG_DIR /var/log/jabba


ADD target/classes/logback.xml ${WASABI_HOME}/conf/
ADD target/classes/metrics.properties ${WASABI_HOME}/conf/
ADD target/classes/web.properties ${WASABI_HOME}/conf/
ADD target/extra-resources/service/entrypoint.sh ${WASABI_HOME}/entrypoint.sh

RUN chmod 775 ${WASABI_HOME}/entrypoint.sh && mkdir ${WASABI_LOG_DIR}

ADD target/wasabi-main-*-all.jar ${WASABI_HOME}/wasabi.main.jar

EXPOSE 8080
WORKDIR ${WASABI_HOME}

CMD ["./entrypoint.sh"]
