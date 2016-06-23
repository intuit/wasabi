#!/bin/sh

HOME=${application.home}
NAME=${application.name}
USER=${application.user}
GROUP=${application.group}

case "$1" in
  1|2)
    getent group $GROUP >/dev/null || groupadd -r $GROUP
    getent passwd $USER >/dev/null || useradd -r -g $GROUP -d $HOME -s /usr/sbin/nologin -c "Wasabi / $NAME" $USER
    ;;
  *)
    echo "`basename $0` called with unknown argument: $1" >&2
    ;;
esac
