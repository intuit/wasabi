#!/usr/bin/env bash

echo "TRAVIS_PULL_REQUEST: ${TRAVIS_PULL_REQUEST}"
#if [ "$TRAVIS_BRANCH" == 'develop' ] || [ "$TRAVIS_BRANCH" == 'master' ]; then
#  if [ "$TRAVIS_PULL_REQUEST" == 'false' ]; then
    mvn deploy -DskipTests=true -P sign --settings .travis/settings.xml
#  fi
#fi