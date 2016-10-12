#!/bin/sh

export WASABI_SERVER=http://localhost:8080/api/v1
# CHANGE THIS TO BE YOUR LOGIN
export WASABI_LOGIN=admin:admin
export APP_NAME=WasabiUI

echo Creating experiment CardViewTest in new application $APP_NAME
curl -u ${WASABI_LOGIN} -H "Content-Type: application/json" -d "{\"applicationName\":\"${APP_NAME}\",\"label\":\"CardViewTest\",\"samplingPercent\":1,\"startTime\":\"2016-04-27T00:00:00-0700\",\"endTime\":\"2017-08-19T00:00:00-0700\",\"description\":\"Experiment used to control whether users see the Card View.\"}" "${WASABI_SERVER}/experiments/?createNewApplication=true"
echo Created experiment, getting ID

export EXPERIMENT_ID=$(curl -u ${WASABI_LOGIN} ${WASABI_SERVER}/applications/${APP_NAME}/experiments/CardViewTest | python -mjson.tool | sed -n 's/^.*id\".*\"\(.*\)\".*/\1/p')
echo Experiment ID: ${EXPERIMENT_ID}

curl -u ${WASABI_LOGIN} -H "Content-Type: application/json" -d "{\"label\":\"NoCardView\",\"allocationPercent\":1,\"description\":\"If a user is in this bucket, which all users are by default, they will NOT see the Card View checkbox.  To see the checkbox, they need to manually be put in the null bucket.\",\"isControl\":true}" \
    "${WASABI_SERVER}/experiments/${EXPERIMENT_ID}/buckets"
echo Created bucket NoCardView

echo Starting experiment
curl -u ${WASABI_LOGIN} -X PUT -H "Content-Type: application/json" -d '{"state":"RUNNING"}' "${WASABI_SERVER}/experiments/${EXPERIMENT_ID}"
echo Experiment started
