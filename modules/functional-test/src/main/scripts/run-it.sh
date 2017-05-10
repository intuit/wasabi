###############################################################################
# Copyright 2017 Intuit
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
###############################################################################
#!/usr/bin/env bash

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
   let total_secs_elapsed=total_secs_elapsed+$SLEEP_INTERVAL_SECS
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



