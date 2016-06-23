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

NAME=${application.name}
DAEMON=${application.daemon.enable}

case "$1" in
  remove|purge|upgrade)
    if [ "$DAEMON" = "true" ]; then
      /etc/init.d/$NAME stop
      /bin/rm -rf /etc/service/$NAME
    fi

    sleep 5
    ;;
  deconfigure|failed-upgrade)
    ;;
  *)
    echo "`basename $0` called with unknown argument: $1" >&2
    ;;
esac