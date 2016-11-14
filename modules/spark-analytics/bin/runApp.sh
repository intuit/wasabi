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

app_req_jars_comma="$lib_path/junit-4.10.jar\
,$lib_path/logback-classic-1.1.3.jar\
,$lib_path/logback-core-1.1.3.jar\
,$lib_path/cats-kernel_2.10-0.7.2.jar\
,$lib_path/cats-macros_2.10-0.7.2.jar\
,$lib_path/cats-core_2.10-0.7.2.jar\
,$lib_path/spark-csv_2.10-1.5.0.jar\
,$lib_path/commons-csv-1.1.jar\
"
echo "app_req_jars_comma=$app_req_jars_comma"

app_req_jars_colon="\
$lib_path/junit-4.10.jar\
:$lib_path/logback-classic-1.1.3.jar\
:$lib_path/logback-core-1.1.3.jar\
:$lib_path/cats-kernel_2.10-0.7.2.jar\
:$lib_path/cats-macros_2.10-0.7.2.jar\
:$lib_path/cats-core_2.10-0.7.2.jar\
:$lib_path/spark-csv_2.10-1.5.0.jar\
:$lib_path/commons-csv-1.1.jar\
"
echo "app_req_jars_colon=$app_req_jars_colon"


app_jar="$cfs_lib_path/wasabi-spark-analytics-1.0.20160902232623-SNAPSHOT.jar"
echo "app_jar=$app_jar"

dse -u cassandra -p cassandra spark-submit \
--deploy-mode=$deploy_mode \
--jars "$app_req_jars_comma" \
--driver-class-path "$app_req_jars_colon" \
--class $class_name \
--driver-java-options "$java_options" \
--supervise \
$app_jar




