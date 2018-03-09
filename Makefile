.PHONY: all stop clean load main cassandra mysql load-main load-keyspace load-migration

all: load main

stop:
	docker-compose down

clean: stop
	sudo -n rm -rf config data logs

load: load-main load-keyspace load-migration

main: load-main cassandra mysql
	docker-compose up -d wasabi-main
	while ! curl \
			  --retry 1 \
			  --retry-delay 1 \
			  --connect-timeout 1 \
			  --retry-connrefused \
			  --silent \
			  --fail \
			  http://127.0.0.1:8080/api/v1/ping ; \
	do \
	  sleep 1 ; \
	  echo -n . ; \
	done
	xdg-open http://127.0.0.1:8080 || open http://127.0.0.1

cassandra: load-keyspace load-migration
	docker-compose up -d wasabi-cassandra
	while ! bash -c "echo -ne '\03\0\0\0\05\0\0\0\0' | nc -w 1 127.0.0.1 9042 | grep -qF VERSION" ; do sleep 1 ; done
	docker-compose run --rm wasabi-keyspace
	docker-compose run --rm wasabi-migration

mysql:
	docker-compose up -d wasabi-mysql
	while ! bash -c "echo | nc -w 1 127.0.0.1 3306 | grep -qF mysql_" ; do sleep 1; done

load-keyspace:
	docker inspect --type=image wasabi-keyspace >/dev/null || \
	docker load --input=../wasabi-images/keyspace.txz

load-migration:
	docker inspect --type=image wasabi-migration >/dev/null || \
	docker load --input=../wasabi-images/migration.txz

load-main:
	docker inspect --type=image wasabi-main >/dev/null || \
	docker load --input=../wasabi-images/main.txz
