#!/bin/bash
set -euo pipefail
echo "Running $TEST with SQ=$SQ_VERSION"

case "$TEST" in
  ci)
    # only verify build
    mvn verify -B -e -V
    exit 0;
  ;;
  ruling)
    if [[ $GITHUB_BRANCH == "PULLREQUEST-"* && $SLAVE != "windows" ]]
    then
      #No ruling on PR: those have been run on travis, just keep ruling on windows for safety
      exit 0;
    fi
    #fetch submodule containing sources of ruling projects
    git submodule update --init --recursive
    #ruling requires java 8
    export JAVA_HOME=/opt/sonarsource/jvm/java-1.8.0-sun-x64
    export PATH=$JAVA_HOME/bin:$PATH
  ;;
  performancing)
    #fetch submodule containing sources of performancing projects
    git submodule update --init --recursive
  ;;
  plugin)
  ;;
  semantic)
    #fetch submodule containing sources of projects used for semantic ITs
    git submodule update --init --recursive
    #Semantic IT projects requires java 8
    export JAVA_HOME=/opt/sonarsource/jvm/java-1.8.0-sun-x64
    export PATH=$JAVA_HOME/bin:$PATH
  ;;
  *)
    echo "Unexpected TEST mode: $TEST"
    exit 1
  ;;
esac
cd its/$TEST
mvn package -Pit-$TEST -Dsonar.runtimeVersion="$SQ_VERSION" -Dmaven.test.redirectTestOutputToFile=false -B -e -V
