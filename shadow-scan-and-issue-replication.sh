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

function sonarcloud_compute_engine_status_for_given_project() {
  local PROJECT_KEY="$1"
  local RESPONSE
  RESPONSE="$(
    curl --silent --fail-with-body --location --request GET \
      --header "Authorization: Bearer ${SHADOW_SONAR_TOKEN}" \
      --output - \
      "${SHADOW_SONAR_HOST_URL}/api/ce/component?component=${PROJECT_KEY}"
  )"
  local STATUS
  # we first check if there is one or more 'PENDING' tasks in the queue
  STATUS="$(echo "${RESPONSE}" | jq -r '.queue[].status')"
  if [[ "${STATUS}" == "null" ]]; then
    STATUS=""
  fi
  if [[ -z "${STATUS}" ]]; then
    # otherwise we get the status of the current task
    STATUS="$(echo "${RESPONSE}" | jq -r '.current.status')"
  fi
  echo -n "${STATUS}"
}

function wait_for_sonarcloud_compute_engine_to_finish() {
  local MAX_WAIT_TIME_SECONDS="300"  # Default to 5 minutes
  local SLEEP_INTERVAL_SECONDS="1"
  local ELAPSED_TIME=0
  local LAST_STATUS=""
  local STATUS

  echo "Waiting for SonarCloud compute engine to finish for project key: ${SHADOW_PROJECT_KEY}"
  while (( ELAPSED_TIME < MAX_WAIT_TIME_SECONDS )); do
    STATUS=$(sonarcloud_compute_engine_status_for_given_project "${SHADOW_PROJECT_KEY}")
    if [[ "${STATUS}" != "${LAST_STATUS}" ]]; then
      echo -n " ${STATUS} "
      LAST_STATUS="${STATUS}"
    fi

    if [[ "${STATUS}" == "PENDING" || "${STATUS}" == "IN_PROGRESS" ]]; then
      echo -n "."
    elif [[ "${STATUS}" == "FAILED" || "${STATUS}" == "CANCELED" ]]; then
      echo -e "\nERROR: SonarCloud compute engine finished with status: ${STATUS}"
      return 1
    elif [[ "${STATUS}" == "SUCCESS" ]]; then
      echo -e "\nSonarCloud compute engine finished successfully."
      return 0
    else
      echo -e "\nERROR: Unknown status: ${STATUS}"
      return 1
    fi
    sleep "${SLEEP_INTERVAL_SECONDS}"
    ELAPSED_TIME=$((ELAPSED_TIME + SLEEP_INTERVAL_SECONDS))
  done
  echo -e "\nERROR: Timeout reached after ${MAX_WAIT_TIME_SECONDS} seconds."
  return 1
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
wait_for_sonarcloud_compute_engine_to_finish
run_iris_with_and_without_dry_run
