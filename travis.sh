#!/bin/bash

set -euo pipefail

function installTravisTools {
  mkdir ~/.local
  curl -sSL https://github.com/SonarSource/travis-utils/tarball/v16 | tar zx --strip-components 1 -C ~/.local
  source ~/.local/bin/install
}

case "$TEST" in

CI)
  mvn verify -B -e -V
  # pr analysis
  if [ "$TRAVIS_PULL_REQUEST" != "false" ]; then
    source $HOME/.jdk_switcher_rc
    jdk_switcher use oraclejdk8
    mvn -V -B -e verify sonar:sonar \
      -DskipTests -Djacoco.skip -DskipObfuscation -Denforcer.skip \
      -Dsonar.analysis.mode=preview \
      -Dsonar.github.pullRequest=$TRAVIS_PULL_REQUEST \
      -Dsonar.github.repository=$TRAVIS_REPO_SLUG \
      -Dsonar.github.login=$SONAR_GITHUB_LOGIN \
      -Dsonar.github.oauth=$SONAR_GITHUB_OAUTH \
      -Dsonar.host.url=$SONAR_HOST_URL \
      -Dsonar.login=$SONAR_LOGIN \
      -Dsonar.password=$SONAR_PASSWD
  fi
  ;;

plugin|ruling)
  installTravisTools

  mvn package -T2 -Dsource.skip=true -Denforcer.skip=true -Danimal.sniffer.skip=true -Dmaven.test.skip=true

  if [ "$SQ_VERSION" = "DEV" ] ; then
    build_snapshot "SonarSource/sonarqube"
  fi

  cd its/$TEST
  EXTRA_PARAMS=
  [ -n "${PROJECT:-}" ] && EXTRA_PARAMS="-Dtest=JavaRulingTest#$PROJECT"
  mvn package -Dsonar.runtimeVersion="$SQ_VERSION" -Dmaven.test.redirectTestOutputToFile=false $EXTRA_PARAMS
  ;;

*)
  echo "Unexpected TEST mode: $TEST"
  exit 1
  ;;

esac
