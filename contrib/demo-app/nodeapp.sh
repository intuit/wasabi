#!/bin/bash
DIR=/home/ubuntu/demo-app
PATH=/home/ubuntu/bin:/home/ubuntu/.local/bin:/usr/local/sbin:/usr/local/bin:/usr/sbin:/usr/bin:/sbin:/bin:/usr/games:/usr/local/games:/snap/bin
NODE_PATH=/usr/lib/nodejs:/usr/lib/node_modules:/usr/share/javascript
NODE=/usr/bin/node

test -x $NODE || exit 0

function start_app {
  NODE_ENV=production nohup "$NODE" "$DIR/index" 1>>"$DIR/logs/demo-app.log" 2>&1 &
  echo $! > "$DIR/pids/demo-app.pid"
}

function stop_app {
  kill `cat $DIR/pids/demo-app.pid`
}

case $1 in
   start)
      start_app ;;
    stop)
      stop_app ;;
    restart)
      stop_app
      start_app
      ;;
    *)
      echo "usage: demo-app.sh {start|stop}" ;;
esac
exit 0
