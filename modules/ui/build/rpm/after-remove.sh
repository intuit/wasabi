#!/bin/sh

case "$1" in
  0|1)
    echo "nothing to do" > /dev/null
    ;;
  *)
    echo "`basename $0` called with unknown argument: $1" >&2
    ;;
esac
