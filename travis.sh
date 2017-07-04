#!/bin/bash

set -euo pipefail

function configureTravis {
  mkdir ~/.local
  curl -sSL https://github.com/SonarSource/travis-utils/tarball/v36 | tar zx --strip-components 1 -C ~/.local
  source ~/.local/bin/install
}
configureTravis

if [ "$TEST" != "CI_MACOSX" ]; then
  . installJDK8
fi

function strongEcho {
  echo ""
  echo "================ $1 ================="
}
case "$TEST" in

CI)
  if [ "$TRAVIS_BRANCH" == "master" ] && [ "$TRAVIS_PULL_REQUEST" == "false" ]; then
    strongEcho "Build and analyze commit in master"
    SONAR_PROJECT_VERSION=`maven_expression "project.version"`

    # Do not deploy a SNAPSHOT version but the release version related to this build
    . set_maven_build_version $TRAVIS_BUILD_NUMBER
    # integration of jacoco report is quite memory-consuming
    export MAVEN_OPTS="-Xmx1536m -Xms128m"
    git fetch --unshallow
    mvn org.jacoco:jacoco-maven-plugin:prepare-agent deploy sonar:sonar -B -e -V \
       -Pcoverage-per-test,deploy-sonarsource,release \
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
    . set_maven_build_version $TRAVIS_BUILD_NUMBER
  else
    echo "======= Found RELEASE version ======="
  fi

  export MAVEN_OPTS="-Xmx1536m -Xms128m"
  mvn deploy \
        -Pdeploy-sonarsource,release \
        -B -e -V $*

  elif [ "$TRAVIS_PULL_REQUEST" != "false" ]; then
    strongEcho "Build and analyze pull request"
    export MAVEN_OPTS="-Xmx1G -Xms128m"
    SONAR_PROJECT_VERSION=`maven_expression "project.version"`

    if [ -n "${GITHUB_TOKEN-}" ]; then
      strongEcho "SonarSource pull request"
      # Do not deploy a SNAPSHOT version but the release version related to this build
      . set_maven_build_version $TRAVIS_BUILD_NUMBER
      mvn deploy sonar:sonar -B -e -V \
          -Pdeploy-sonarsource \
          -Dsonar.analysis.mode=issues \
          -Dsonar.github.pullRequest=$TRAVIS_PULL_REQUEST \
          -Dsonar.github.repository=$TRAVIS_REPO_SLUG \
          -Dsonar.github.oauth=$GITHUB_TOKEN \
          -Dsonar.host.url=$SONAR_HOST_URL \
          -Dsonar.login=$SONAR_TOKEN
    else
      strongEcho "External pull request"
      # external PR : no deployment to repox
      mvn install sonar:sonar -B -e -V \
        -Dsonar.analysis.mode=issues \
        -Dsonar.github.pullRequest=$TRAVIS_PULL_REQUEST \
        -Dsonar.github.repository=$TRAVIS_REPO_SLUG \
        -Dsonar.github.oauth=$GITHUB_TOKEN_EXTERNAL_PR \
        -Dsonar.host.url=$SONAR_HOST_URL_EXTERNAL_PR \
        -Dsonar.login=$SONAR_TOKEN_EXTERNAL_PR
    fi


  else
    strongEcho 'Build, no analysis'
    # Build branch, without any analysis

    # No need for Maven goal "install" as the generated JAR file does not need to be installed
    # in Maven local repository
    mvn verify -B -e -V
  fi
  ;;

ruling)
  if [ "$TRAVIS_PULL_REQUEST" == "false" ]; then
   strongEcho "ruling tests are only run on pull requests!"
   exit 0;
  fi

  [ "$TEST" = "ruling" ] && git submodule update --init --recursive
  EXTRA_PARAMS=
  [ -n "${PROJECT:-}" ] && EXTRA_PARAMS="-DfailIfNoTests=false -Dtest=JavaRulingTest#$PROJECT"
  mvn install -Dsonar.runtimeVersion="$SQ_VERSION" -Dmaven.test.redirectTestOutputToFile=false -B -e -V -Pit-$TEST $EXTRA_PARAMS
  ;;

CI_MACOSX)
  strongEcho 'Verify build on MAC OS X, no analysis'
  mvn verify -B -e -V
  ;;

*)
  echo "Unexpected TEST mode: $TEST"
  exit 1
  ;;

esac
