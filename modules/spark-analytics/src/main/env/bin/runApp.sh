#!/usr/bin/env bash


if [ "$#" -ne 1 ]; then
    echo "Illegal number of parameters. Please provide only single argument - java_options. if there are multiple java_options then wrap then wrap input in diuble quotes..."
fi

java_options=$1
echo "java_options=$java_options"

class_name=com.intuit.wasabi.data.launcher.SparkApplicationLauncher
echo "class_name=$class_name"

deploy_mode=cluster
echo "deploy_mode=$deploy_mode"

cfs_prefix="cfs:"
echo "cfs_prefix=$cfs_prefix"

cfs_lib_path="$cfs_prefix/user/cassandra/wasabi/lib"
lib_path="$cfs_lib_path"
echo "lib_path=$lib_path"

DIR="$( cd "$( dirname "${BASH_SOURCE[0]}" )" && pwd )"
echo "Script directory: $DIR"

app_jar="$DIR/../lib/wasabi-spark-analytics-1.0.20160902232623-SNAPSHOT-development-all.jar"
echo "app_jar=$app_jar"

dse -u cassandra -p cassandra spark-submit \
--deploy-mode=$deploy_mode \
--class $class_name \
--driver-java-options "$java_options" \
--supervise \
$app_jar
