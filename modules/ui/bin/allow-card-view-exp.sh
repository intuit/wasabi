#!/bin/sh

export WASABI_SERVER=http://localhost:8080/api/v1
# CHANGE THIS TO BE YOUR LOGIN
export WASABI_LOGIN=admin:admin
export APP_NAME=WasabiUI
export user=${1:-'admin'}

echo Assigning user to null bucket for experiment CardViewTest
curl -u ${WASABI_LOGIN} -H "Content-Type: application/json" -X PUT -d '{"assignment":null, "overwrite": true }' ${WASABI_SERVER}/assignments/applications/${APP_NAME}/experiments/CardViewTest/users/$user
echo Assigned user

  spotify:
    clientId: 4f9ea44544be4b789e54bfd9c23ebdc9
    secret: cedd084f7dd3440782cd3c3aa1a6ec5f



spotify:
    clientId: 4f9ea44544be4b789e54bfd9c23ebdc9
    secret: cfb4f730af0d47f3a49a2bde85763922


