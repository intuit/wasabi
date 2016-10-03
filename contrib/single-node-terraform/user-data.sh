#!/bin/bash -eu

exec > >(tee /var/log/user-data.log|logger -t user-data -s 2>/dev/console) 2>&1

sudo yum -y install wget
sudo yum -y install vim

# install apache
yum -y install httpd
sudo service httpd start

# install Oracle JDK
wget --no-cookies --no-check-certificate --header "Cookie: gpw_e24=http%3A%2F%2Fwww.oracle.com%2F; oraclelicense=accept-securebackup-cookie" "http://download.oracle.com/otn-pub/java/jdk/8u92-b14/jdk-8u92-linux-x64.rpm"
sudo yum -y localinstall jdk-8u92-linux-x64.rpm

cat <<EOF | sudo tee -a /etc/yum.repos.d/datastax.repo

[datastax]
name = DataStax Repo for Apache Cassandra
baseurl = http://rpm.datastax.com/community
enabled = 1
gpgcheck = 0
EOF

sudo yum -y install dsc20
sudo service cassandra start
sudo systemctl enable cassandra.service

# install mysql
wget http://repo.mysql.com/mysql-community-release-el7-5.noarch.rpm
sudo rpm -ivh mysql-community-release-el7-5.noarch.rpm
sudo yum -y update
sudo yum -y install mysql-server
sudo systemctl start mysqld

# creare db
MYSQL_HOST="localhost"
MYSQL_DBNAME="wasabi"

mysql -s -N -h $MYSQL_HOST  --execute="create database $MYSQL_DBNAME;"
mysql -s -N -h $MYSQL_HOST --execute="create user 'readwrite'@'localhost' identified by 'readwrite';"
mysql -s -N -h $MYSQL_HOST --execute="grant all privileges on wasabi.* to 'readwrite'@'localhost' identified by 'readwrite';"
mysql -s -N -h $MYSQL_HOST --execute="flush privileges;"

# install runit
curl -s https://packagecloud.io/install/repositories/imeyer/runit/script.rpm.sh | sudo bash
sudo yum -y install runit-2.1.2-3.el7.centos.x86_64
