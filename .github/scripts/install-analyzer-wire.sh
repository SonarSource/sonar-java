#!/usr/bin/env bash
set -euo pipefail

base="${ARTIFACTORY_URL}/sonarsource-public-qa/org/sonarsource/semsitter/analyzer-wire/2.0.0"
dest="${RUNNER_TEMP}/analyzer-wire"
mkdir -p "$dest"
curl -fsS -u "${ARTIFACTORY_USERNAME}:${ARTIFACTORY_ACCESS_TOKEN}" \
  -o "$dest/analyzer-wire-2.0.0.pom" "$base/analyzer-wire-2.0.0.pom"
curl -fsS -u "${ARTIFACTORY_USERNAME}:${ARTIFACTORY_ACCESS_TOKEN}" \
  -o "$dest/analyzer-wire-2.0.0.jar" "$base/analyzer-wire-2.0.0.jar"

settings=()
if [[ ! -f "${MAVEN_CONFIG:-$HOME/.m2}/settings.xml" ]]; then
  cat > "$dest/settings.xml" <<EOF
<settings>
  <mirrors>
    <mirror>
      <id>sonarsource</id>
      <url>${ARTIFACTORY_URL}/sonarsource-qa</url>
      <mirrorOf>*</mirrorOf>
    </mirror>
  </mirrors>
  <servers>
    <server>
      <id>sonarsource</id>
      <username>${ARTIFACTORY_USERNAME}</username>
      <password>${ARTIFACTORY_ACCESS_TOKEN}</password>
    </server>
  </servers>
</settings>
EOF
  settings=(-s "$dest/settings.xml")
fi
mvn -B "${settings[@]}" org.apache.maven.plugins:maven-install-plugin:3.1.2:install-file \
  -Dfile="$dest/analyzer-wire-2.0.0.jar" \
  -DpomFile="$dest/analyzer-wire-2.0.0.pom"
