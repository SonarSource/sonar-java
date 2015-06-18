#!/bin/bash

set -euo pipefail

function installTravisTools {
  curl -sSL https://raw.githubusercontent.com/sonarsource/travis-utils/v2.1/install.sh | bash
}

if [ "$TESTS" == "CI" ]; then
  mvn verify -B -e -V
else
  mvn install -DskipTests=true

  installTravisTools
  travis_run_its "${TESTS}"
fi

if [ "${TRAVIS_PULL_REQUEST}" != "false" ] 
then
	echo "Start pullrequest analysis"
 mvn sonar:sonar \
 -Dsonar.analysis.mode=preview \
 -Dsonar.github.pullRequest=$TRAVIS_PULL_REQUEST \
 -Dsonar.github.repository=$SONAR_GITHUB_REPOSITORY \
 -Dsonar.forceUpdate=true \
 -Dsonar.github.login=$SONAR_GITHUB_LOGIN \
 -Dsonar.github.oauth=$SONAR_GITHUB_OAUTH \
 -Dsonar.host.url=$SONAR_HOST_URL 
fi 
