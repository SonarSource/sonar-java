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
  
  elif [[ "${TRAVIS_BRANCH}" == "branch-"* ]] && [ "$TRAVIS_PULL_REQUEST" == "false" ]; then
  # no dory analysis on release branch

  # Fetch all commit history so that SonarQube has exact blame information
  # for issue auto-assignment
  # This command can fail with "fatal: --unshallow on a complete repository does not make sense" 
  # if there are not enough commits in the Git repository (even if Travis executed git clone --depth 50).
  # For this reason errors are ignored with "|| true"
  git fetch --unshallow || true 

  # get current version from pom
  CURRENT_VERSION=`maven_expression "project.version"`
  
  if [[ $CURRENT_VERSION =~ "-SNAPSHOT" ]]; then
    echo "======= Found SNAPSHOT version ======="
    # Do not deploy a SNAPSHOT version but the release version related to this build
    set_maven_build_version $TRAVIS_BUILD_NUMBER
  else
    echo "======= Found RELEASE version ======="
  fi

  export MAVEN_OPTS="-Xmx1536m -Xms128m"
  mvn deploy \
        -Pdeploy-sonarsource,release \
        -B -e -V $*

  elif [ "$TRAVIS_PULL_REQUEST" != "false" ] && [ -n "${GITHUB_TOKEN-}" ]; then
    # For security reasons environment variables are not available on the pull requests
    # coming from outside repositories
    # http://docs.travis-ci.com/user/pull-requests/#Security-Restrictions-when-testing-Pull-Requests
    # That's why the analysis does not need to be executed if the variable GITHUB_TOKEN is not defined.
    strongEcho "Build and analyze pull request"
    SONAR_PROJECT_VERSION=`maven_expression "project.version"`
    # Do not deploy a SNAPSHOT version but the release version related to this build
    set_maven_build_version $TRAVIS_BUILD_NUMBER
    export MAVEN_OPTS="-Xmx1G -Xms128m"
    mvn deploy sonar:sonar -B -e -V \
        -Pdeploy-sonarsource \
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
  if [ "$TRAVIS_PULL_REQUEST" == "false" ]; then
   strongEcho "plugin or ruling tests are only run on pull requests!"
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
