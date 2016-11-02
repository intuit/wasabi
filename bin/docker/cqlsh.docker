FROM python:2-alpine

RUN pip install cqlsh==5.0.3 && \
    cp /usr/local/bin/cqlsh /usr/local/bin/cqlsh4 && \
    sed -i '/DEFAULT_PROTOCOL_VERSION = 4/c\DEFAULT_PROTOCOL_VERSION = 3' /usr/local/bin/cqlsh

#By default alpine uses sh instead of bash so all environment subsitution is not supported

#ENV CASSANDRA_KEYSPACE_PREFIX wasabi
#ENV CASSANDRA_REPLICATION 1
#ENV CQLSH_HOST localhost
#ENV CASSANDRA_PORT 9042

ADD create_keyspace.sh /

ENTRYPOINT ["/create_keyspace.sh"]