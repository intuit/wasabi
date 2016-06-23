#!/bin/sh

HOME=${application.home}
NAME=${application.name}
USER=${application.user}
GROUP=${application.group}

case "$1" in
  install|upgrade)
    getent group $GROUP >/dev/null || groupadd -r $GROUP
    getent passwd $USER >/dev/null || useradd -r -g $GROUP -d $HOME -s /usr/sbin/nologin -c "Wasabi / $NAME" $USER
    ;;
  abort-upgrade)
    ;;
  *)
    echo "`basename $0` called with unknown argument: $1" >&2
    ;;
esac
