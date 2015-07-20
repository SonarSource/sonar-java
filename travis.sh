#!/bin/bash

set -euo pipefail

function installTravisTools {
  curl -sSL https://raw.githubusercontent.com/sonarsource/travis-utils/v10/install.sh | bash
  source /tmp/travis-utils/env.sh
}

case "$TESTS" in

CI)
  mvn verify -B -e -V
  ;;

IT-DEV)
  installTravisTools

  mvn install -T2 -Dsource.skip=true -Denforcer.skip=true -Danimal.sniffer.skip=true -Dmaven.test.skip=true

  travis_build_green "SonarSource/sonarqube" "master"

  cd its/plugin
  mvn -DjavaVersion="DEV" -Dsonar.runtimeVersion="DEV" -Dmaven.test.redirectTestOutputToFile=false install
  ;;

RULING)
	mkdir -p ~/.m2/repository/org/codehaus/sonar/sonar-application/5.1.1
	curl -sSL http://downloads.sonarsource.com/sonarqube/sonarqube-5.1.1.zip -o ~/.m2/repository/org/codehaus/sonar/sonar-application/5.1.1/sonar-application-5.1.1.zip
	mvn install -Dsource.skip=true -T2 -Denforcer.skip=true -Danimal.sniffer.skip=true -Dmaven.test.skip=true
  git clone https://github.com/SonarSource/ruling_java.git it_sources

  export SONAR_IT_SOURCES=$(pwd)/it_sources
  cd its/ruling

  installTravisTools
  #travis_reset_ruby
  unset GEM_PATH GEM_HOME RAILS_ENV
  mvn clean install -Dmaven.test.redirectTestOutputToFile=false -DjavaVersion=DEV -Dsonar.runtimeVersion=5.1.1
  ;;

IT-LATEST)
  installTravisTools

  mvn install -T2 -Dsource.skip=true -Denforcer.skip=true -Danimal.sniffer.skip=true -Dmaven.test.skip=true

  travis_download_sonarqube_release "5.1.1"

  cd its/plugin
  mvn -DjavaVersion="DEV" -Dsonar.runtimeVersion="LATEST_RELEASE" -Dmaven.test.redirectTestOutputToFile=false install
  ;;

esac
