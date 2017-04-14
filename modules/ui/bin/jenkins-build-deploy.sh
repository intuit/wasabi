#!/usr/bin/env bash
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

# configurables:
#
#   profile                      : maven profile; default:test
#   nexus_deploy                 : nexus deploy user; default:usr:pwd

project=wasabi
profile=${PROJECT_PROFILE:-development}
nexus_deploy=${NEXUS_DEPLOY:-usr:pwd}
nexus_repositories=${NEXUS_REPOSITORIES}
nexus_repository_id=${NEXUS_REPOSITORY_ID}
nexus_snapshot_repository_id=${NEXUS_SNAPSHOT_REPOSITORY_ID}

exitOnError() {
  echo "error cause: $1"
  exit 1
}

fromPom() {
  mvn -f ./modules/$1/pom.xml -P $2 help:evaluate -Dexpression=$3 | sed -n -e '/^\[.*\]/ !{ p; }'
}

version=`fromPom main ${profile} project.version`
echo "++ version= ${version}"

echo "++ Building: UI module - STARTED"
echo "++ Grunt build - STARTED"

  echo "++ Installing required dependencies is they are missing - STARTED"
  # Install required dependencies is they are missing
  if [ "${WASABI_OS}" == "${WASABI_OSX}" ]; then
    brew list node
    if [[ $? -eq 1 ]]; then
      echo "++ Node.js is not installed. Installing Node.js packages..."
      echo "++ execute: brew install node"
      brew install node

      echo "++ execute: npm install -g yo grunt-cli bower grunt-contrib-compass"
      npm install -g yo grunt-cli bower grunt-contrib-compass

      echo "++ execute: sudo gem install compass"
      sudo gem install compass
    fi
  fi

  echo "++ execute: (cd ./modules/ui && npm install && bower install && grunt build)"
  (cd ./modules/ui && npm install && bower install && grunt build)

  echo "++ Installing required dependencies is they are missing - FINISHED"

  (for contrib_dir in $CONTRIB_PLUGINS_TO_INSTALL; do
       if [ -d contrib/$contrib_dir ]; then
         echo "++ Installing plugin from contrib/$contrib_dir"
         if [ -d contrib/$contrib_dir/plugins ]; then
           cp -R contrib/$contrib_dir/plugins modules/ui/dist
         fi
         if [ -f contrib/$contrib_dir/scripts/plugins.js ]; then
             if [ -f modules/ui/dist/scripts/plugins.js ] && [ `cat modules/ui/dist/scripts/plugins.js | wc -l` -gt 3 ]; then
               echo Need to merge
               # Get all but the last line of the current plugins.js file
               sed -e "1,$(($(cat modules/ui/dist/scripts/plugins.js | wc -l) - 2))p;d" modules/ui/dist/scripts/plugins.js > tmp.txt
               # Since this should end in a } we want to add a comma
               echo ',' >> tmp.txt
               # Copy all but the first and last lines of this plugins's config.  This assumes first line defines var and array, last line ends array.
               sed -e "2,$(($(cat contrib/$contrib_dir/scripts/plugins.js | wc -l) - 1))p;d" contrib/$contrib_dir/scripts/plugins.js >> tmp.txt
               sed '$p;d' modules/ui/dist/scripts/plugins.js >> tmp.txt
               cp tmp.txt modules/ui/dist/scripts/plugins.js
               rm tmp.txt
             else
               echo Overwriting file
               cp contrib/$contrib_dir/scripts/plugins.js modules/ui/dist/scripts
             fi
         fi
         echo Merged in $contrib_dir
       fi;
  done)

  (cd modules/ui; \
    mkdir -p target; \
    for f in app node_modules bower.json Gruntfile.js constants.json karma.conf.js karma-e2e.conf.js package.json test .bowerrc; do \
      cp -r ${f} target; \
    done; \
    echo Getting merged plugins.js file and plugins directory; \
    cp dist/scripts/plugins.js target/app/scripts/plugins.js; \
    cp -R dist/plugins target/app; \
    sed -i '' -e "s|VERSIONLOC|${version}|g" target/app/index.html 2>/dev/null; \
    #(cd target; npm install; bower install --no-optional; grunt clean); \
    (cd target; grunt clean); \
    (cd target; grunt build --target=develop --no-color) \
  )
  echo "++ Grunt build - FINISHED"

#  echo "Create UI RPM - STARTED"
#  (  cp -r build target; \
#    for pkg in deb rpm; do \
#      sed -i '' -e "s|\${application.home}|${home}|g" target/build/${pkg}/before-install.sh 2>/dev/null; \
#      sed -i '' -e "s|\${application.name}|${api_name}|g" target/build/${pkg}/before-install.sh 2>/dev/null; \
#      sed -i '' -e "s|\${application.user}|${user}|g" target/build/${pkg}/before-install.sh 2>/dev/null; \
#      sed -i '' -e "s|\${application.group}|${group}|g" target/build/${pkg}/before-install.sh 2>/dev/null; \
#      sed -i '' -e "s|\${application.ui.home}|${ui_home}|g" target/build/${pkg}/after-install.sh 2>/dev/null; \
#      sed -i '' -e "s|\${application.http.content.directory}|${content}|g" target/build/${pkg}/after-install.sh 2>/dev/null; \
#      sed -i '' -e "s|\${application.user}|${user}|g" target/build/${pkg}/after-install.sh 2>/dev/null; \
#      sed -i '' -e "s|\${application.group}|${group}|g" target/build/${pkg}/after-install.sh 2>/dev/null; \
#      sed -i '' -e "s|\${application.http.content.directory}|${content}|g" target/build/${pkg}/before-remove.sh 2>/dev/null; \
#    done; \
#    (cd target; ../bin/fpm.sh -n ${name} -v ${version} -p ${profile})
#  )
#  find . -type f \( -name "*.rpm" -or -name "*.deb" \) -exec mv {} ./target 2>/dev/null \;
#  echo "Create UI RPM - FINISHED"

echo "++ Building: UI module - FINISHED"

echo "++ Push UI ZIP to internal Nexus - STARTED"

if [[ "${version/-SNAPSHOT}" == "${version}" ]]; then
  artifact_repository_id=${nexus_repository_id}
elif [[ "${version}" == *SNAPSHOT ]]; then
  artifact_repository_id=${nexus_snapshot_repository_id}
fi

group=`fromPom main ${profile} project.groupId`
artifact=ui
path=${nexus_repositories}/${artifact_repository_id}/`echo ${group} | sed "s/\./\//g"`/${artifact}/${version}
zip=${project}-${artifact}-${profile}-${version}.zip
zip_path=${path}/${zip}

echo "++ Archiving: ${zip} ${zip_path}"
curl -v -u ${nexus_deploy} --upload-file ./modules/ui/target/dist.zip ${zip_path} || \
exitOnError "archive failed: curl -v -u [nexus_deploy] --upload-file ./modules/ui/dist.zip ${zip_path}"

echo "++ Push UI ZIP to internal Nexus - FINISHED"
