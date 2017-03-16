###############################################################################
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
###############################################################################

###############################################################################
# Wasabi Docker Image Builder
#
# This image will create a container which, when run with the docker socket,
# will build the final wasabi-main container. From the git root directory,
# just run:
#
# ./bin/wasabi.sh docker-build
#
# After a long time (like 20 minutes), you should have a container named
# wasabi-main
###############################################################################
FROM ubuntu:latest

RUN apt-get update

COPY . /wasabi

WORKDIR /wasabi

RUN ./bin/wasabi.sh bootstrap && \
  ./bin/wasabi.sh build

ENTRYPOINT ["./bin/container.sh", "build"]
