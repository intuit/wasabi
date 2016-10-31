#!/usr/bin/env bash
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

project=wasabi
profile=${WASABI_PROFILE:-test}
modules=${WASABI_MODULES:-main ui}
execute_integration_tests=${WASABI_INTEGRATION_TEST:-true}
deploy_host=${WASABI_DEPLOY_HOST:-deploy.host}
deploy_host_user=${WASABI_DEPLOY_USER:-usr}
sonar_host_url=${SONAR_HOST_URL:+-Dsonar.host.url=$SONAR_HOST_URL}
sonar_auth_token=${SONAR_AUTH_TOKEN:+-Dsonar.login=$SONAR_AUTH_TOKEN}
nexus_archive=${NEXUS_ARCHIVE:+true}
nexus_repositories=${NEXUS_REPOSITORIES}
nexus_repository_id=${NEXUS_REPOSITORY_ID}
nexus_snapshot_repository_id=${NEXUS_SNAPSHOT_REPOSITORY_ID}
nexus_deploy=${NEXUS_DEPLOY:-usr:pwd}
deploy_resource=${deploy_host_user}@${deploy_host}
file_repository=${FILE_REPOSITORY:-file.host:/data/dropbox}
file_repository_user=${FILE_REPOSITORY_USER:-usr}
internal_project=${WASABI_INTERNAL_PROJECT}
internal_project_repository=${WASABI_INTERNAL_REPOSITORY}
internal_project_branch=${WASABI_INTERNAL_BRANCH}
internal_project_user=${WASABI_INTERNAL_USER:-usr:pwd}
wasabi_env="WASABI_OS=native WASABI_MAVEN=\"--settings ${internal_project}/settings.xml\""

env

echo "project:${project}"
echo "profile:${profile}"
echo "modules:${modules}"
echo "execute_integration_tests:${execute_integration_tests}"
echo "deploy_host:${deploy_host}"
echo "deploy_host_user:${deploy_host_user}"
echo "sonar_host_url:${sonar_host_url}"
echo "sonar_auth_token:${sonar_auth_token}"
echo "nexus_archive:${nexus_archive}"
echo "nexus_repositories:${nexus_repositories}"
echo "nexus_repository_id:${nexus_repository_id}"
echo "nexus_snapshot_repository_id:${nexus_snapshot_repository_id}"
echo "nexus_deploy:${nexus_deploy}"
echo "deploy_resource:${deploy_resource}"
echo "file_repository:${file_repository}"
echo "file_repository_user:${file_repository_user}"
echo "internal_project:${internal_project}"
echo "internal_project_repository:${internal_project_repository}"
echo "internal_project_branch:${internal_project_branch}"
echo "internal_project_user:${internal_project_user}"
echo "wasabi_env:${wasabi_env}"

exitOnError() {
  echo "error cause: $1"
  java -jar jenkins-cli.jar set-build-result unstable
  exit 1
}

wget ${JENKINS_URL}jnlpJars/jenkins-cli.jar || \
  exitOnError "unable to retrieve jenkins-cli.jar: wget ${JENKINS_URL}jnlpjars/jenkins-cli.jar"

echo "cloning: ${internal_project_repository} / ${internal_project_branch}"
git clone -b ${internal_project_branch} https://${internal_project_user}@${internal_project_repository} || \
  exitOnError "unable to clone project: git clone -b ${internal_project_branch} https://${internal_project_user}@${internal_project_repository}"

(cd ${internal_project}; cat ~/.m2/settings.xml | sed "s|</profiles>|$(cat profile.xml | tr -d '\n')</profiles>|" | sed "s|\[PWD\]|$(pwd)|" > settings.xml)

service=$(mvn --settings ${internal_project}/settings.xml -f ./modules/main/pom.xml -P ${profile} help:evaluate -Dexpression=application.name | sed -n -e '/^\[.*\]/ !{ p; }')
group=$(mvn --settings ${internal_project}/settings.xml -f ./modules/main/pom.xml -P ${profile} help:evaluate -Dexpression=project.groupId | sed -n -e '/^\[.*\]/ !{ p; }')
version=$(mvn --settings ${internal_project}/settings.xml -f ./modules/main/pom.xml -P ${profile} help:evaluate -Dexpression=project.version | sed -n -e '/^\[.*\]/ !{ p; }')

echo "packaging: ${project} / ${profile}"
(eval ${wasabi_env} ./bin/${project}.sh --profile=${profile} --verify=true package) || \
  exitOnError "unable to build project : (${wasabi_env} ./bin/${project}.sh --profile=${profile} --verify=true package)"

for module in ${modules}; do
  if [[ ! -z "${module// }" ]]; then
    echo "prepare deploy: $(find ./${project}/target -name ${project}-${module}-${profile}-*.noarch.rpm -type f)"
    rpm=`basename $(find ./${project}/target -name ${project}-${module}-${profile}-*.noarch.rpm -type f)` || \
      exitOnError "failed to find ./${project}/target/${project}-${module}-${profile}-*.noarch.rpm"
    status=0

    if [[ "${execute_integration_tests}" == "true" ]]; then
      echo "deploying: ${rpm}"

# FIXME: if we rm the file, it needs to be chmod' such that user:deploy can read/scp the new file
#      (ssh ${deploy_resource} "rm /tmp/${project}/jacoco-it.exec")
      (scp ./${project}/target/${rpm} ${deploy_host_user}@${deploy_host}:; ssh ${deploy_host_user}@${deploy_host} "mv ${rpm} inbox") || \
        exitOnError "failed to deploy application: (scp ./${project}/target/${rpm} ${deploy_host_user}@${deploy_host}:; ssh ${deploy_host_user}@${deploy_host} \"mv ${rpm} inbox\")"

      sleep 120

      if [ "${module}" == "main" ]; then
        echo "testing: ${rpm} http://${deploy_host}:8080"
        (eval ${wasabi_env} ./bin/${project}.sh --profile=${profile} --endpoint=${deploy_host}:8080 test)
        status=$?

        (ssh ${deploy_resource} "/home/jenkins/bin/init-d ${service} stop") || \
          exitOnError "unable to stop project: (ssh ${deploy_resource} \"/home/jenkins/bin/init-d ${service} stop\")"
        (scp ${deploy_resource}:/tmp/${project}/jacoco-it.exec ./modules/main/target/jacoco-it.exec) || \
          exitOnError "unable to retrieve test report: (scp ${deploy_resource}:/tmp/${project}/jacoco-it.exec ./target/jacoco-it.exec)"
      fi
    fi

    echo "publishing sonar report"
    (mvn --settings ${internal_project}/settings.xml ${sonar_host_url} ${sonar_auth_token} -P ${profile} package sonar:sonar) || \
      exitOnError "unable to report to sonar: (mvn --settings ${internal_project}/settings.xml [sonar_host_url] [sonar_auth_token] -P ${profile} package sonar:sonar)"

    [ "${status}" -ne "0" ] && exitOnError "integration tests failed: (cd ${project}; eval ${wasabi_env} ./bin/${project}.sh --profile=${profile} --endpoint=${deploy_host}:8080 test)"

    if [[ ! -z "${nexus_archive}" ]]; then
      echo "publishing nexus artifacts"
      (mvn --settings ${internal_project}/settings.xml -Dmaven.test.skip=true -P ${profile} deploy) || \
        exitOnError "unable to report to sonar: (mvn --settings ${internal_project}/settings.xml -Dmaven.test.skip=true -P ${profile} deploy)"
    fi

    if [[ "${version/-SNAPSHOT}" == "${version}" ]]; then
      artifact_repository_id=${nexus_repository_id}
    elif [[ "${version}" == *SNAPSHOT ]]; then
      artifact_repository_id=${nexus_snapshot_repository_id}
    fi

    if [ "${version/-SNAPSHOT}" == "${version}" ]; then
      artifact=$(mvn --settings ${internal_project}/settings.xml -f ./modules/main/pom.xml -P ${profile} help:evaluate -Dexpression=project.artifactId | sed -n -e '/^\[.*\]/ !{ p; }')
      path=${nexus_repositories}/${artifact_repository_id}/`echo ${group} | sed "s/\./\//g"`/${artifact}/${version}
      rpm_path=${path}/${rpm}

      echo "archiving: ${rpm} ${rpm_path}"
      curl -v -u ${nexus_deploy} --upload-file ./${project}/target/${rpm} ${rpm_path} || \
        exitOnError "archive rpm failed: curl -v -u [nexus_deploy] --upload-file ./${project}/target/${rpm} ${rpm_path}"

      if [ "${module}" == "ui" ]; then
        artifact=ui
        path=${nexus_repositories/${artifact_repository_id}/`echo ${group} | sed "s/\./\//g"`/${artifact}/${version}
        zip=${project}-${artifact}-${profile}-${version}.zip
        zip_path=${path}/${zip}

        echo "archiving: ${zip} ${zip_path}"
        curl -v -u ${nexus_deploy} --upload-file ./${project}/modules/ui/target/dist.zip ${zip_path} || \
          exitOnError "archive failed: curl -v -u [nexus_deploy] --upload-file ./${project}/modules/ui/dist.zip ${zip_path}"
      fi
    fi
  fi
done
