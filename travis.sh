#!/bin/bash

set -euo pipefail

function installTravisTools {
  curl -sSL https://raw.githubusercontent.com/sonarsource/travis-utils/v2.1/install.sh | bash
}

if [ "$TESTS" == "CI" ]; then
  mvn verify -B -e -V
else
  mvn install -Dsource.skip=true -T2 -Denforcer.skip=true -Danimal.sniffer.skip=true -Dmaven.test.skip=true

  installTravisTools
  travis_run_its "${TESTS}"
fi
