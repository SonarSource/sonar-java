#!/bin/bash
set -euo pipefail
echo "Running $TEST"

echo "cloning Tomcat 8 project"
cd its/sources
if [ ! -d tomcat80 ]; then
  git clone git@github.com:apache/tomcat80.git
  cd tomcat80
  #fix version of sources tested
  git checkout db9c56ab66fa96e136bba36e40d466b92378b5c5
  else
  cd tomcat80
fi

#build tomcat 8
ant clean deploy

cd ../../parallelizing
echo "Downloading SonarQube"
if [ -f sonar-application.zip ]; then
  rm sonar-application.zip
fi
if [ ! -f sonar-application.zip ]; then
  curl https://repox.sonarsource.com/sonarsource-dev/org/sonarsource/sonarqube/sonar-application/6.5.0.26422/sonar-application-6.5.0.26422.zip -o sonar-application.zip
fi

case "$TEST" in
  baseline)
    mvn package -Pit-parallelizing -Dsonar.runtimeVersion=LATEST_RELEASE -Dversion.toCopy=4.11.0.10618 -Dmaven.test.redirectTestOutputToFile=false -B -e -V
  ;;
  parallelized)
    mvn package -Pit-parallelizing -Dsonar.runtimeVersion=LATEST_RELEASE -Dmaven.test.redirectTestOutputToFile=false -B -e -V
  ;;
  *)
    echo "Unexpected TEST mode: $TEST"
    exit 1
  ;;
esac
