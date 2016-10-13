#!/bin/sh

export WASABI_SERVER=http://localhost:8080/api/v1
# CHANGE THIS TO BE YOUR LOGIN
export WASABI_LOGIN=admin:admin
export APP_NAME=WasabiUI
export user=${1:-'admin'}

echo Assigning user to null bucket for experiment CardViewTest
curl -u ${WASABI_LOGIN} -H "Content-Type: application/json" -X PUT -d '{"assignment":null, "overwrite": true }' ${WASABI_SERVER}/assignments/applications/${APP_NAME}/experiments/CardViewTest/users/$user
echo Assigned user

