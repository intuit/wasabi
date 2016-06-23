#!/bin/sh

case "$1" in
  purge|remove)
    echo "nothing to do" > /dev/null
    ;;
  abort-install|abort-upgrade|upgrade|failed-upgrade|disappear)
    ;;
  *)
    echo "`basename $0` called with unknown argument: $1" >&2
    ;;
esac
