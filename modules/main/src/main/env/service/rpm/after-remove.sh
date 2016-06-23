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
NAME=${application.name}
USER=${application.user}
GROUP=${application.group}
DAEMON=${application.daemon.enable}

case "$1" in
  0|1)
    for dir in "$HOME" "$LOG"; do
      /bin/rm -rf $dir
    done

    if [ "$DAEMON" = "true" ]; then
      /bin/rm -rf /etc/init.d/$NAME
    fi

    getent passwd $USER >/dev/null && userdel -f $USER
    getent group $GROUP >/dev/null && groupdel $GROUP

    # note: needed to swallow the !0 response code
    s=$?
    ;;
  *)
    echo "`basename $0` called with unknown argument: $1" >&2
    ;;
esac