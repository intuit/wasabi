#!/bin/sh
# This script will create the necessary application, experiment and buckets
# needed by the example application.

# NOTE: You will need to change this variable to point at the correct Wasabi api
# server.  For example, if you were testing against your local server, it will probably be
# http://localhost:8080 .
export WASABI_SERVER=http://localhost:8080/api/v1
# NOTE: CHANGE THIS TO BE YOUR LOGIN
export WASABI_LOGIN=admin:admin
export APP_NAME=MyStore
export EXP_NAME=TestBuyButtonColor

echo Creating experiment $EXP_NAME in new application $APP_NAME
curl -u ${WASABI_LOGIN} -H "Content-Type: application/json" -d "{\"applicationName\":\"${APP_NAME}\",\"label\":\"${EXP_NAME}\",\"samplingPercent\":1,\"startTime\":\"2016-04-27T00:00:00-0700\",\"endTime\":\"2017-08-19T00:00:00-0700\",\"description\":\"Experiment to test effect of different color Buy buttons.\"}" "${WASABI_SERVER}/experiments/?createNewApplication=true"
echo Created experiment, getting ID

# Get the experiment ID, needed in later API calls
export EXPERIMENT_ID=$(curl -u ${WASABI_LOGIN} ${WASABI_SERVER}/applications/${APP_NAME}/experiments/${EXP_NAME} | python -mjson.tool | sed -n 's/^.*id\".*\"\(.*\)\".*/\1/p')
echo Experiment ID: ${EXPERIMENT_ID}

# Create buckets
curl -u ${WASABI_LOGIN} -H "Content-Type: application/json" -d "{\"label\":\"Control\",\"allocationPercent\":0.3334,\"description\":\"Show the default button color, white.\",\"isControl\":true}" \
    "${WASABI_SERVER}/experiments/${EXPERIMENT_ID}/buckets"
echo Created bucket Control
curl -u ${WASABI_LOGIN} -H "Content-Type: application/json" -d "{\"label\":\"BlueButton\",\"allocationPercent\":0.3333,\"description\":\"Show a blue Buy button.\",\"isControl\":false}" \
    "${WASABI_SERVER}/experiments/${EXPERIMENT_ID}/buckets"
echo Created bucket BlueButton
curl -u ${WASABI_LOGIN} -H "Content-Type: application/json" -d "{\"label\":\"GreenButton\",\"allocationPercent\":0.3333,\"description\":\"Show a green Buy button.\",\"isControl\":false}" \
    "${WASABI_SERVER}/experiments/${EXPERIMENT_ID}/buckets"
echo Created bucket GreenButton

echo Starting experiment
curl -u ${WASABI_LOGIN} -X PUT -H "Content-Type: application/json" -d '{"state":"RUNNING"}' "${WASABI_SERVER}/experiments/${EXPERIMENT_ID}"
echo Experiment started
