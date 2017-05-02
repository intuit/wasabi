#!/bin/sh

MAX_WAIT_SECS=210
SLEEP_INTERVAL_SECS=3

#let total_secs_elapsed=12
#echo "sleeping for $total_secs_elapsed to wait for service startup"
#sleep $total_secs_elapsed

let total_secs_elapsed=0
echo "waiting for service startup.."
health_check_count=`nohup curl http://localhost:8080/api/v1/ping 2>&1 | grep \"healthy\":true | wc -l`
while [ ${health_check_count} = "0" ] && [ $total_secs_elapsed -le $MAX_WAIT_SECS ]
do
   sleep $SLEEP_INTERVAL_SECS
   echo -n "."
   let total_secs_elapsed=total_secs_elapsed+3
   health_check_count=`nohup curl http://localhost:8080/api/v1/ping 2>&1 | grep \"healthy\":true | wc -l`
done
echo "total_secs_elapsed = $total_secs_elapsed"

begin_time=$(date +"%s")

#(sh /vagrant/target/scripts/run-functional-tests.sh > /vagrant/target/integration-test.out) 
(sh /vagrant/target/scripts/run-functional-tests.sh) 
echo "integration test exit status = $?"

end_time=$(date +"%s")

diff_time=$(($end_time-$begin_time))
echo "$(($diff_time / 60)) minutes and $(($diff_time % 60)) seconds elapsed for running Java functional tests."



