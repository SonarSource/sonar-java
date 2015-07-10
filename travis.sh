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

IT-LATEST)
  installTravisTools

  mvn install -T2 -Dsource.skip=true -Denforcer.skip=true -Danimal.sniffer.skip=true -Dmaven.test.skip=true

  travis_download_sonarqube_release "5.1.1"

  cd its/plugin
  mvn -DjavaVersion="DEV" -Dsonar.runtimeVersion="LATEST_RELEASE" -Dmaven.test.redirectTestOutputToFile=false install
  ;;

esac
