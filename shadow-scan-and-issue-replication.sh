#!/usr/bin/env bash

set -euo pipefail

# IRIS: Issue Replication for Sonarqube
IRIS_JAR_URL="${ARTIFACTORY_URL}/sonarsource-private-releases/com/sonarsource/iris/iris/\[RELEASE\]/iris-\[RELEASE\]-jar-with-dependencies.jar"
IRIS_JAR_PATH="target/libs/iris.jar"

function build_and_analyze_the_project() {
  echo
  echo "===== Build and analyze the project targeting a shadow SonarQube instance"
  local BUILD_CMD
  if [[ -e "gradlew" ]]; then
    BUILD_CMD="./gradlew --info --stacktrace --console plain build sonar"
  else
    source set_maven_build_version "$BUILD_NUMBER"
    BUILD_CMD="mvn -Pcoverage -Dmaven.test.redirectTestOutputToFile=false --batch-mode --errors --show-version verify sonar:sonar"
  fi
  ${BUILD_CMD} \
    -DbuildNumber="${BUILD_NUMBER}" \
    -Dsonar.host.url="${SHADOW_SONAR_HOST_URL}" \
    -Dsonar.token="${SHADOW_SONAR_TOKEN}" \
    -Dsonar.organization="${SHADOW_ORGANIZATION}" \
    -Dsonar.projectKey="${SHADOW_PROJECT_KEY}" \
    -Dsonar.analysis.buildNumber="${BUILD_NUMBER}" \
    -Dsonar.analysis.repository="${GITHUB_REPO}" \
    "$@"
}

function download_iris() {
  echo
  echo "===== Download ${IRIS_JAR_URL}"
  mkdir -p target/libs
  curl --silent --fail-with-body --location --header "Authorization: Bearer ${ARTIFACTORY_PRIVATE_PASSWORD}" \
      --output "${IRIS_JAR_PATH}" "${IRIS_JAR_URL}"
}

function run_iris() {
  local DRY_RUN="$1"
  java \
    -Diris.source.projectKey="${SONAR_PROJECT_KEY}" \
    -Diris.source.url="${SONAR_HOST_URL}" \
    -Diris.source.token="${SONAR_TOKEN}" \
    -Diris.destination.projectKey="${SHADOW_PROJECT_KEY}" \
    -Diris.destination.organization="${SHADOW_ORGANIZATION}" \
    -Diris.destination.url="${SHADOW_SONAR_HOST_URL}" \
    -Diris.destination.token="${SHADOW_SONAR_TOKEN}" \
    -Diris.dryrun="${DRY_RUN}" \
    -jar "${IRIS_JAR_PATH}"
}

function run_iris_with_and_without_dry_run() {
  echo
  echo "===== Execute IRIS as dry-run"
  if run_iris true; then
    echo "===== Successful IRIS execution as dry-run"
    echo "===== Execute IRIS for real"
    if run_iris false; then
      echo "===== Successful IRIS execution for real"
      return 0
    else
      echo "===== Failed IRIS execution for real"
      return 1
    fi
  else
    echo "===== Failed IRIS execution as dry-run"
    return 1
  fi
}

build_and_analyze_the_project "$@"
download_iris
run_iris_with_and_without_dry_run
