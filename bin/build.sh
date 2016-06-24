#!/usr/bin/env bash

(cd slate && bundle exec middleman build --clean)
rm -rf v1/guide && mkdir -p v1/guide && cp -R slate/build/* v1/guide