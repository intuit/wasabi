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

exec > >(tee ./provision.log|logger -t provision -s 2>/dev/console)

sudo yum -y install wget
sudo yum -y install vim

# install apache
yum -y install httpd
sudo service httpd start

# install Oracle JDK
wget --no-check-certificate -c --header "Cookie: oraclelicense=accept-securebackup-cookie" http://download.oracle.com/otn-pub/java/jdk/8u131-b11/d54c1d3a095b4ff2b6607d096fa80163/jdk-8u131-linux-x64.rpm

sudo yum -y localinstall jdk-8u131-linux-x64.rpm

cat <<EOF | sudo tee -a /etc/yum.repos.d/datastax.repo
[datastax]
name = DataStax Repo for Apache Cassandra
baseurl = http://rpm.datastax.com/community
enabled = 1
gpgcheck = 0
EOF

sudo yum -y install dsc21
sudo service cassandra start
sudo systemctl enable cassandra.service

# install mysql
wget http://repo.mysql.com/mysql-community-release-el7-5.noarch.rpm
sudo rpm -ivh mysql-community-release-el7-5.noarch.rpm
sudo yum -y update
sudo yum -y install mysql-server
sudo systemctl start mysqld

# create MySQL db
MYSQL_HOST="localhost"
MYSQL_DBNAME="wasabi"

mysql -s -N -h $MYSQL_HOST  --execute="create database $MYSQL_DBNAME;"
mysql -s -N -h $MYSQL_HOST --execute="create user 'readwrite'@'localhost' identified by 'readwrite';"
mysql -s -N -h $MYSQL_HOST --execute="grant all privileges on wasabi.* to 'readwrite'@'localhost' identified by 'readwrite';"
mysql -s -N -h $MYSQL_HOST --execute="flush privileges;"

# install runit
curl -s https://packagecloud.io/install/repositories/imeyer/runit/script.rpm.sh | sudo bash
sudo yum -y install runit-2.1.2-3.el7.centos.x86_64
