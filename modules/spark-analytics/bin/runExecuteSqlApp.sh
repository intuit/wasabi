#!/usr/bin/env bash


app_id="execute-sql"

sql_stmt=$1

if [ "x$sql_stmt" -eq "x" ]; then
    echo "Please provide SQL statement to execute as an argument..."
    exit 1
fi

sql_result_limit=$2
if [ "x$sql_result_limit" -eq "x" ]; then
    sql_result_limit=100
fi

java_options="\
-D$app_id.sql=\"$sql_stmt\" \
-D$app_id.limit=$sql_result_limit \
-Dapp_id=$app_id \
-D$app_id.datastore.keyspace=jabba_experiments \
-D$app_id.spark.spark.driver.memory=4g \
-D$app_id.spark.spark.executor.memory=24g \
-D$app_id.spark.spark.cores.max=48 \
-D$app_id.spark.spark.cassandra.connection.timeout_ms=300000 \
-D$app_id.spark.spark.cassandra.connection.keep_alive_ms=300000 \
-D$app_id.spark.spark.cassandra.input.fetch.size_in_rows=2000000 \
-D$app_id.spark.spark.cassandra.input.split.size_in_mb=209715200 \
"

./runApp.sh "$java_options"
