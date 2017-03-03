#!/usr/bin/env bash

if [ "$TRAVIS_BRANCH" == 'develop' ] || [ "$TRAVIS_BRANCH" == 'master' ]; then
  if [ "$TRAVIS_PULL_REQUEST" == 'false' ]; then
    mvn -q deploy -DskipTests=true -P sign --settings .travis/settings.xml
  fi
fi
