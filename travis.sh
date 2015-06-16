#!/bin/bash

set -euo pipefail

if [ "$TESTS" == "CI" ]; then
  mvn verify -B -e -V
else
  mvn install -DskipTests=true
  travis_run_its "${TESTS:3}"
fi
