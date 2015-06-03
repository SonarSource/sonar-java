#!/bin/bash

set -euo pipefail

function installTravisTools {
  curl -sSL https://raw.githubusercontent.com/dgageot/travis-utils/master/install.sh | sh
  source /tmp/travis-utils/utils.sh
}

if [ "$TESTS" == "CI" ]; then
  mvn verify -B -e -V
else
  installTravisTools

  mvn install -DskipTests=true
  run_its "${TESTS:3}"
fi
