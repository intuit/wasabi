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

# configurables:
#
#   profile                      : project name
#   build                        : build kill switch; default:false
#   profile                      : maven profile; default:test
#   modules                      : project modules to build; default:main ui
#   execute_integration_tests    : execute integration test kill switch; default:true
#   execute_unit_tests           : execute unit test kill switch; default:true
#   deploy_host                  : integration test host; default:deploy.host
#   deploy_host_url              : integration test deploy user; default:deploy.user
#   sonar_host_url               : sonar host; default:+-Dsonar.host.url=SONAR_HOST_URL
#   sonar_auth_token             : sonar authorization token; default:-Dsonar.login=SONAR_AUTH_TOKEN
#   nexus_archive                : nexus archive kill switch; default:false
#   nexus_repositories           : nexus repositories
#   nexus_repository_id          : nexus milestone repository id
#   nexus_snapshot_repository_id : nexus snapshot repository id
#   nexus_deploy                 : nexus deploy user; default:usr:pwd
#   file_repository              : file repository; default:file.host:/data/dropbox
#   file_repository_user         : file repository user; default:usr
#   internal_project             : internal project name
#   internal_project_repository  : internal project repository
#   internal_project_branch      : internal project repository branch
#   internal_project_user        : internal project user

project=wasabi
build=${PROJECT_BUILD:-false}
profile=${PROJECT_PROFILE:-test}
modules=${PROJECT_MODULES:-main ui}
execute_integration_tests=${PROJECT_INTEGRATION_TEST:-true}
execute_unit_tests=${PROJECT_UNIT_TEST:-true}
deploy_host=${PROJECT_DEPLOY_HOST:-deploy.host}
deploy_host_user=${PROJECT_DEPLOY_USER:-usr}
sonar_host_url=${SONAR_HOST_URL:+-Dsonar.host.url=$SONAR_HOST_URL}
sonar_auth_token=${SONAR_AUTH_TOKEN:+-Dsonar.login=$SONAR_AUTH_TOKEN}
nexus_archive=${NEXUS_ARCHIVE:-false}
nexus_repositories=${NEXUS_REPOSITORIES}
nexus_repository_id=${NEXUS_REPOSITORY_ID}
nexus_snapshot_repository_id=${NEXUS_SNAPSHOT_REPOSITORY_ID}
nexus_deploy=${NEXUS_DEPLOY:-usr:pwd}
deploy_resource=${deploy_host_user}@${deploy_host}
file_repository=${FILE_REPOSITORY:-file.host:/data/dropbox}
file_repository_user=${FILE_REPOSITORY_USER:-usr}
internal_project=${PROJECT_INTERNAL_PROJECT}
internal_project_repository=${PROJECT_INTERNAL_REPOSITORY}
internal_project_branch=${PROJECT_INTERNAL_BRANCH}
internal_project_user=${PROJECT_INTERNAL_USER:-usr:pwd}
project_env="WASABI_OS=native WASABI_MAVEN=\"--settings ./settings.xml\""

exitOnError() {
  echo "error cause: $1"
  java -jar jenkins-cli.jar set-build-result unstable
  exit 1
}

# fetch jenkins cli client

wget ${JENKINS_URL}jnlpJars/jenkins-cli.jar || \
  exitOnError "unable to retrieve jenkins-cli.jar: wget ${JENKINS_URL}jnlpjars/jenkins-cli.jar"

# exit build if not enabled

[[ "${build}" == "false" ]] && exitOnError "project build: ${build}"

# fetch internal project

echo "cloning: ${internal_project_repository} / ${internal_project_branch}"
git clone -b ${internal_project_branch} https://${internal_project_user}@${internal_project_repository} || \
  exitOnError "unable to clone project: git clone -b ${internal_project_branch} https://${internal_project_user}@${internal_project_repository}"

# construct viable/complete settings.xml
# note: need to add distributionManagement/repository to [ws]/pom.xml to map to settings.xml in order to mvn-deploy internally
# note: add internal repository to settings.xml; see: https://maven.apache.org/guides/mini/guide-multiple-repositories.html

cat ~/.m2/settings.xml | sed "s|</profiles>|$(cat ${internal_project}/profile.xml | tr -d '\n')</profiles>|" | sed "s|\[PWD\]|$(pwd)|" > settings.xml

# extract meta-data

service=$(mvn --settings ./settings.xml -f ./modules/main/pom.xml -P ${profile} help:evaluate -Dexpression=application.name | sed -n -e '/^\[.*\]/ !{ p; }')
group=$(mvn --settings ./settings.xml -f ./modules/main/pom.xml -P ${profile} help:evaluate -Dexpression=project.groupId | sed -n -e '/^\[.*\]/ !{ p; }')
version=$(mvn --settings ./settings.xml -f ./modules/main/pom.xml -P ${profile} help:evaluate -Dexpression=project.version | sed -n -e '/^\[.*\]/ !{ p; }')

# build

echo "packaging: ${project} / ${profile}"
(eval ${project_env} ./bin/${project}.sh --profile=${profile} --buildtests=${execute_unit_tests} --verify=true package) || \
  exitOnError "unable to build project : (${project_env} ./bin/${project}.sh --profile=${profile} --buildtests=${execute_unit_tests} --verify=true package)"
echo "end packaging"

for module in ${modules}; do
  if [[ ! -z "${module// }" ]]; then
    # derive module rpm

    echo "prepare deploy: $(find ./target -name ${project}-${module}-${profile}-*.noarch.rpm -type f)"
    rpm=`basename $(find ./target -name ${project}-${module}-${profile}-*.noarch.rpm -type f)` || \
      exitOnError "failed to find ./target/${project}-${module}-${profile}-*.noarch.rpm"
    status=0

    if [[ "${execute_integration_tests}" == "true" ]]; then
      echo "deploying: ${rpm}"

      # deploy module rpm, note: remote side daemon process will install

      # note: if we rm the file, it needs to be chmod' such that user:deploy can read/scp the new file
#      (ssh ${deploy_resource} "rm /tmp/${project}/jacoco-it.exec")
      (scp ./target/${rpm} ${deploy_host_user}@${deploy_host}:; ssh ${deploy_host_user}@${deploy_host} "mv ${rpm} inbox") || \
        exitOnError "failed to deploy application: (scp ./target/${rpm} ${deploy_host_user}@${deploy_host}:; ssh ${deploy_host_user}@${deploy_host} \"mv ${rpm} inbox\")"

      sleep 120

      if [ "${module}" == "main" ]; then
        # test module

        echo "testing: ${rpm} http://${deploy_host}:8080"
        (eval ${project_env} ./bin/${project}.sh --profile=${profile} --endpoint=${deploy_host}:8080 test)
        status=$?

        # stop application to flush the jacoco file to disk and fetch it

        (ssh ${deploy_resource} "/home/jenkins/bin/init-d ${service} stop") || \
          exitOnError "unable to stop project: (ssh ${deploy_resource} \"/home/jenkins/bin/init-d ${service} stop\")"
        (scp ${deploy_resource}:/tmp/${project}/jacoco-it.exec ./modules/main/target/jacoco-it.exec) || \
          exitOnError "unable to retrieve test report: (scp ${deploy_resource}:/tmp/${project}/jacoco-it.exec ./target/jacoco-it.exec)"
      fi
    fi

    # publish sonar report

    echo "publishing sonar report"
    (mvn --settings ./settings.xml ${sonar_host_url} ${sonar_auth_token} -P ${profile} sonar:sonar) || \
      exitOnError "unable to report to sonar: (mvn --settings ./settings.xml [sonar_host_url] [sonar_auth_token] -P ${profile} sonar:sonar)"

    [ "${status}" -ne "0" ] && exitOnError "integration tests failed: (cd ${project}; eval ${project_env} ./bin/${project}.sh --profile=${profile} --endpoint=${deploy_host}:8080 test)"

    # fixme: conflicts with distributionManagement in base pom; fallback: use jenkins:publish-to-maven post-build action
    if [[ "${nexus_archive}" == "true" ]]; then
      # publish artifacts to nexus

      echo "publishing nexus artifacts"
      (mvn --settings ./settings.xml -Dmaven.test.skip=true -P ${profile} deploy) || \
        exitOnError "unable to report to sonar: (mvn --settings ./settings.xml -Dmaven.test.skip=true -P ${profile} deploy)"
    fi

    # determine MILESTONE or SNAPSHOT repository

    if [[ "${version/-SNAPSHOT}" == "${version}" ]]; then
      artifact_repository_id=${nexus_repository_id}
    elif [[ "${version}" == *SNAPSHOT ]]; then
      artifact_repository_id=${nexus_snapshot_repository_id}
    fi

    if [ "${version/-SNAPSHOT}" == "${version}" ]; then
      # archive MILESTONE rpms

      artifact=$(mvn --settings ./settings.xml -f ./modules/main/pom.xml -P ${profile} help:evaluate -Dexpression=project.artifactId | sed -n -e '/^\[.*\]/ !{ p; }')
      path=${nexus_repositories}/${artifact_repository_id}/`echo ${group} | sed "s/\./\//g"`/${artifact}/${version}
      rpm_path=${path}/${rpm}

      echo "archiving: ${rpm} ${rpm_path}"
      curl -v -u ${nexus_deploy} --upload-file ./target/${rpm} ${rpm_path} || \
        exitOnError "archive rpm failed: curl -v -u [nexus_deploy] --upload-file ./target/${rpm} ${rpm_path}"
    fi

    # Always push the UI zip file because we need it for wasabi-intuit builds
    if [ "${module}" == "ui" ]; then
      # archive MILESTONE ui.zip
      artifact=ui
      path=${nexus_repositories}/${artifact_repository_id}/`echo ${group} | sed "s/\./\//g"`/${artifact}/${version}
      zip=${project}-${artifact}-${profile}-${version}.zip
      zip_path=${path}/${zip}

      echo "archiving: ${zip} ${zip_path}"
      curl -v -u ${nexus_deploy} --upload-file ./modules/ui/target/dist.zip ${zip_path} || \
        exitOnError "archive failed: curl -v -u [nexus_deploy] --upload-file ./modules/ui/dist.zip ${zip_path}"
    fi
  fi
done