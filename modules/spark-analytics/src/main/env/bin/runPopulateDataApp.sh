#!/usr/bin/env bash

app_id="populate-data"

#-D$app_id.allocation=\"1-45411423 1-40236675 3-37127352 1-27493622 1-22366365 4-18051356 6-9314769 5-6519225 13-4173668 69-1014089 30-372998 76-84454 160-7600 503-95\"

java_options="\
-D$app_id.allocation=503-95 \
-D$app_id.batchSize=100000 \
-Dapp_id=$app_id \
-D$app_id.datastore.keyspace=jabba_experiments \
-D$app_id.spark.spark.driver.memory=4g \
-D$app_id.spark.spark.executor.memory=96g \
"

./runApp.sh "$java_options"

