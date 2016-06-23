#!/bin/sh

CONTENT=${application.http.content.directory}

case "$1" in
  0|1)
    if [ -d $CONTENT ]; then
      for f in `ls $CONTENT`; do
        if [ "${f}" != "swagger" ]; then
          /bin/rm -rf $CONTENT/${f}
        fi
      done
    fi
    ;;
  *)
    echo "`basename $0` called with unknown argument: $1" >&2
    ;;
esac
