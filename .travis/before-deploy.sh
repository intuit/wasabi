#!/usr/bin/env bash

if [ "$TRAVIS_BRANCH" == 'develop' ] || [ "$TRAVIS_BRANCH" == 'master' ]; then
  if [ "$TRAVIS_PULL_REQUEST" == 'false' ]; then
    openssl aes-256-cbc -K $encrypted_a6540285227b_key -iv $encrypted_a6540285227b_iv \
      -in .travis/codesigning.asc.enc -out .travis/codesigning.asc -d
    gpg --fast-import .travis/codesigning.asc
  fi
fi
