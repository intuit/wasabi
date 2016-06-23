#!/usr/bin/env bash

if [ "$TRAVIS_BRANCH" == 'develop' ] || [ "$TRAVIS_BRANCH" == 'master' ]; then
  if [ "$TRAVIS_PULL_REQUEST" == 'false' ]; then
    openssl aes-256-cbc -K $encrypted_351032695731_key -iv $encrypted_351032695731_iv \
      -in .travis/codesigning.asc.enc -out .travis/codesigning.asc -d
    gpg --fast-import .travis/codesigning.asc
  fi
fi
