#!/usr/bin/env bash
set -euo pipefail

base="${ARTIFACTORY_URL}/sonarsource-public-qa/org/sonarsource/semsitter/analyzer-wire/2.0.0"
dest="${RUNNER_TEMP}/analyzer-wire"
mkdir -p "$dest"
curl -fsS -u "${ARTIFACTORY_USERNAME}:${ARTIFACTORY_ACCESS_TOKEN}" \
  -o "$dest/analyzer-wire-2.0.0.pom" "$base/analyzer-wire-2.0.0.pom"
curl -fsS -u "${ARTIFACTORY_USERNAME}:${ARTIFACTORY_ACCESS_TOKEN}" \
  -o "$dest/analyzer-wire-2.0.0.jar" "$base/analyzer-wire-2.0.0.jar"
mvn -B org.apache.maven.plugins:maven-install-plugin:3.1.2:install-file \
  -Dfile="$dest/analyzer-wire-2.0.0.jar" \
  -DpomFile="$dest/analyzer-wire-2.0.0.pom"
