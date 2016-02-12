#!/bin/bash
set -euo pipefail
echo "Running $TEST with SQ=$SQ_VERSION"

case "$TEST" in
PLUGIN)
  cd its/plugin
  mvn package -Pit-plugin -Dsonar.runtimeVersion="$SQ_VERSION" -Dmaven.test.redirectTestOutputToFile=false -B -e -V
;;
RULING)
  #fetch submodule containing sources of ruling projects
  git submodule update --init --recursive
  #ruling requires java 8
  export JAVA_HOME=/opt/sonarsource/jvm/java-1.8.0-sun-x64
  cd its/ruling
  mvn package -Pit-ruling -Dsonar.runtimeVersion="$SQ_VERSION" -Dmaven.test.redirectTestOutputToFile=false -B -e -V
;;
PERFORMANCE)
  echo "ALMOST DONE !!"
;;

*)
  echo "Unexpected TEST mode: $TEST"
  exit 1
;;
esac