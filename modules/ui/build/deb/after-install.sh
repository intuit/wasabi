#!/bin/sh

UI_HOME=${application.ui.home}
CONTENT=${application.http.content.directory}
USER=${application.user}
GROUP=${application.group}

case "$1" in
  configure|abort-remove|abort-deconfigure)
    chown -R $USER:$GROUP $UI_HOME
    if [ -d $CONTENT ]; then
      cp -r $UI_HOME/content/ui/dist/* $CONTENT
      chown -R $USER:$GROUP $CONTENT
    else
      echo "[WARNING]target directory does not exist: $CONTENT"
    fi
    ;;
  abort-upgrade)
    ;;
  *)
    echo "`basename $0` called with unknown argument: $1" >&2
    ;;
esac
