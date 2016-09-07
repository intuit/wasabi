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
from openjdk:8-jdk-alpine
	
COPY ./ /usr/local/${application.name}
RUN mv /usr/local/${application.name}/entrypoint.sh /usr/local/entrypoint.sh && \
	sed -i -e $'s/1>>.*2>&1//' /usr/local/${application.name}/bin/run 2>/dev/null;

EXPOSE 8080 8090 8180

ENTRYPOINT ["/usr/local/entrypoint.sh"]
CMD ["wasabi"]