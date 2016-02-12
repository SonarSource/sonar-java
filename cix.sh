#!/bin/bash
set -euo pipefail
echo "Running $TEST with SQ=$SQ_VERSION"

case "$TEST" in
ruling)
  #fetch submodule containing sources of ruling projects
  git submodule update --init --recursive
  #ruling requires java 8
  export JAVA_HOME=/opt/sonarsource/jvm/java-1.8.0-sun-x64
;;
plugin|performancing)
;;
*)
  echo "Unexpected TEST mode: $TEST"
  exit 1
;;

esac
cd its/$TEST
mvn package -Pit-$TEST -Dsonar.runtimeVersion="$SQ_VERSION" -Dmaven.test.redirectTestOutputToFile=false -B -e -V
