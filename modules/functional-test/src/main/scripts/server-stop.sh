#!/bin/sh

PIDS=$(ps ax | grep -i 'wasabi' | grep java | grep -v grep | awk '{print $1}')
echo PIDS $PIDS

if [ -z "$PIDS" ]; then
   echo "No wasabi server to stop"
   exit 1
else
   kill -s TERM $PIDS
   echo "Killed wasabi server"
fi