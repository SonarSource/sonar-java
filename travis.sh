#!/bin/bash

set -euo pipefail

function installTravisTools {
  mkdir ~/.local
  curl -sSL https://github.com/SonarSource/travis-utils/tarball/v16 | tar zx --strip-components 1 -C ~/.local
  source ~/.local/bin/install
}

case "$TESTS" in

CI)
  mvn verify -B -e -V
  ;;

IT-DEV)
  installTravisTools

  mvn install -T2 -Dsource.skip=true -Denforcer.skip=true -Danimal.sniffer.skip=true -Dmaven.test.skip=true

  build_snapshot "SonarSource/sonarqube"

  cd its/plugin
  mvn -DjavaVersion="DEV" -Dsonar.runtimeVersion="DEV" -Dmaven.test.redirectTestOutputToFile=false install
  ;;

RULING)
  installTravisTools

  mvn install -Dsource.skip=true -T2 -Denforcer.skip=true -Danimal.sniffer.skip=true -Dmaven.test.skip=true

  export SONAR_IT_SOURCES=$(pwd)/its/sources

  cd its/ruling
  mvn clean install -Dmaven.test.redirectTestOutputToFile=false -DjavaVersion=DEV -Dsonar.runtimeVersion=5.1.1
  ;;

IT-LTS)
  installTravisTools

  mvn install -T2 -Dsource.skip=true -Denforcer.skip=true -Danimal.sniffer.skip=true -Dmaven.test.skip=true

  cd its/plugin
  mvn -DjavaVersion="DEV" -Dsonar.runtimeVersion="LTS_OR_OLDEST_COMPATIBLE" -Dmaven.test.redirectTestOutputToFile=false install
  ;;

esac
