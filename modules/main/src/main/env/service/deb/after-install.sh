#!/bin/sh
#*******************************************************************************
# Copyright 2016 Intuit
#
# Licensed under the Apache License, Version 2.0 (the "License");
# you may not use this file except in compliance with the License.
# You may obtain a copy of the License at
#
#     http://www.apache.org/licenses/LICENSE-2.0
#
# Unless required by applicable law or agreed to in writing, software
# distributed under the License is distributed on an "AS IS" BASIS,
# WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
# See the License for the specific language governing permissions and
# limitations under the License.
#*******************************************************************************

HOME=${application.home}
CONTENT=${application.http.content.directory}
NAME=${application.name}
USER=${application.user}
GROUP=${application.group}
LOG=${log.dir}
DAEMON=${application.daemon.enable}

case "$1" in
  configure|abort-remove|abort-deconfigure)
    for d in "$HOME" "$LOG"; do \
      mkdir -p $d
      chown -R $USER:$GROUP $d
    done

    chmod 755 "$HOME/bin/run"

    # note: fixes swagger relative json paths in line with sed
    sed -i.bak "s/\(path\" : \"\)/\1\/\.\./g" $CONTENT/${swaggerServicePath}
    chown -R $USER:$GROUP $HOME

    if [ "$DAEMON" = "true" ]; then
      mkdir -p /etc/service/$NAME
      cp $HOME/bin/run /etc/service/$NAME
      chown -R root:root /etc/service/$NAME
      chmod 755 /etc/service/$NAME/run
      ln -s /sbin/sv /etc/init.d/$NAME
      /etc/init.d/$NAME start
    fi
    ;;
  abort-upgrade)
    ;;
  *)
    echo "`basename $0` called with unknown argument: $1" >&2
    ;;
esac