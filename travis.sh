#!/bin/bash

set -euo pipefail

function configureTravis {
  mkdir ~/.local
  curl -sSL https://github.com/SonarSource/travis-utils/tarball/v25 | tar zx --strip-components 1 -C ~/.local
  source ~/.local/bin/install
}

function strongEcho {
  echo ""
  echo "================ $1 ================="
}
case "$TEST" in

CI)
  configureTravis
  if [ "$TRAVIS_BRANCH" == "master" ] && [ "$TRAVIS_PULL_REQUEST" == "false" ]; then
    strongEcho "Build and analyze commit in master"
    SONAR_PROJECT_VERSION=`maven_expression "project.version"`

    # Do not deploy a SNAPSHOT version but the release version related to this build
    set_maven_build_version $TRAVIS_BUILD_NUMBER
    # integration of jacoco report is quite memory-consuming
    export MAVEN_OPTS="-Xmx1536m -Xms128m"
    git fetch --unshallow
    mvn org.jacoco:jacoco-maven-plugin:prepare-agent deploy sonar:sonar -B -e -V \
       -Pcoverage-per-test,deploy-sonarsource \
       -Dsonar.host.url=$SONAR_HOST_URL \
       -Dsonar.projectVersion=$SONAR_PROJECT_VERSION \
       -Dsonar.login=$SONAR_TOKEN

  elif [ "$TRAVIS_PULL_REQUEST" != "false" ] && [ -n "${GITHUB_TOKEN-}" ]; then
    # For security reasons environment variables are not available on the pull requests
    # coming from outside repositories
    # http://docs.travis-ci.com/user/pull-requests/#Security-Restrictions-when-testing-Pull-Requests
    # That's why the analysis does not need to be executed if the variable GITHUB_TOKEN is not defined.
    strongEcho "Build and analyze pull request"
    export MAVEN_OPTS="-Xmx1G -Xms128m"
    mvn verify sonar:sonar -B -e -V \
        -Dsonar.analysis.mode=issues \
        -Dsonar.github.pullRequest=$TRAVIS_PULL_REQUEST \
        -Dsonar.github.repository=$TRAVIS_REPO_SLUG \
        -Dsonar.github.oauth=$GITHUB_TOKEN \
        -Dsonar.host.url=$SONAR_HOST_URL \
        -Dsonar.login=$SONAR_TOKEN

  else
    strongEcho 'Build, no analysis'
    # Build branch, without any analysis

    # No need for Maven goal "install" as the generated JAR file does not need to be installed
    # in Maven local repository
    mvn verify -B -e -V
  fi
  ;;

plugin|ruling)
  if [ "$TRAVIS_BRANCH" != "master" ] && [ "$TRAVIS_PULL_REQUEST" == "false" ]; then
   strongEcho "We do not run plugin or ruling tests on branches, open a pull request to run those!"
   exit 0;
  fi

  configureTravis
  [ "$TEST" = "ruling" ] && git submodule update --init --recursive
  EXTRA_PARAMS=
  [ -n "${PROJECT:-}" ] && EXTRA_PARAMS="-DfailIfNoTests=false -Dtest=JavaRulingTest#$PROJECT"
  mvn install -Dsonar.runtimeVersion="$SQ_VERSION" -Dmaven.test.redirectTestOutputToFile=false -B -e -V -Pit-$TEST $EXTRA_PARAMS
  ;;

*)
  echo "Unexpected TEST mode: $TEST"
  exit 1
  ;;

esac
