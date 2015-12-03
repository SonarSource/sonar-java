#!/bin/bash

set -euo pipefail

function installTravisTools {
  mkdir ~/.local
  curl -sSL https://github.com/SonarSource/travis-utils/tarball/v16 | tar zx --strip-components 1 -C ~/.local
  source ~/.local/bin/install
}

case "$TEST" in

CI)
  mvn verify -B -e -V
  ;;

plugin|ruling)
  installTravisTools

  if [ "$SQ_VERSION" = "DEV" ] ; then
    build_snapshot "SonarSource/sonarqube"
  fi

  EXTRA_PARAMS=
  [ -n "${PROJECT:-}" ] && EXTRA_PARAMS="-DfailIfNoTests=false -Dtest=JavaRulingTest#$PROJECT"
  mvn package -Dsonar.runtimeVersion="$SQ_VERSION" -Dmaven.test.redirectTestOutputToFile=false -Pit-$TEST $EXTRA_PARAMS
  ;;

*)
  echo "Unexpected TEST mode: $TEST"
  exit 1
  ;;

esac
