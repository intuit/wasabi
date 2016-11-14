#!/usr/bin/env bash

app_id="migrate-data"

java_options="\
-Dapp_id=$app_id \
-D$app_id.migration.datastores.src.keyspace=jabba_experiments \
-D$app_id.migration.datastores.dest.keyspace=jabba_experiments \
-D$app_id.spark.spark.driver.memory=4g \
-D$app_id.spark.spark.executor.memory=96g \
-D$app_id.spark.spark.cores.max=30 \
-D$app_id.spark.cassandra.output.batch.size.bytes=52428800 \
-D$app_id.spark.cassandra.output.batch.size.rows=200000 \
-D$app_id.spark.spark.cassandra.input.fetch.size_in_rows=200000 \
-D$app_id.spark.spark.cassandra.input.split.size_in_mb=52428800 \
"

./runApp.sh "$java_options"
