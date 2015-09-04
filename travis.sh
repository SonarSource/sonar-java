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

  mvn package -T2 -Dsource.skip=true -Denforcer.skip=true -Danimal.sniffer.skip=true -Dmaven.test.skip=true

  build_snapshot "SonarSource/sonarqube"

  cd its/plugin
  mvn package -Dsonar.runtimeVersion="DEV" -Dmaven.test.redirectTestOutputToFile=false
  ;;

RULING)
  installTravisTools

  mvn package -Dsource.skip=true -T2 -Denforcer.skip=true -Danimal.sniffer.skip=true -Dmaven.test.skip=true

  cd its/ruling
  mvn test -Dmaven.test.redirectTestOutputToFile=false -Dsonar.runtimeVersion=5.1.1 -Dtest=JavaRulingTest#$PROJECT
  ;;

IT-LTS)
  installTravisTools

  mvn package -T2 -Dsource.skip=true -Denforcer.skip=true -Danimal.sniffer.skip=true -Dmaven.test.skip=true

  cd its/plugin
  mvn package -Dsonar.runtimeVersion="LTS" -Dmaven.test.redirectTestOutputToFile=false
  ;;

*)
  echo "Unexpected TESTS mode: $TESTS"
  exit 1
  ;;

esac
