#!/usr/bin/env bash

set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"

# This script create an android.jar file from the Android SDK by only keeping the following files:
# AndroidManifest.xml
# META-INF/MANIFEST.MF
# java/lang/Integer.class
# java/lang/Object.class
#
# Note: we don't keep "module-info.class" because it does not exist and the ".class" files version are Java 8.
# When the generated android.jar is added to the classpath of the Sonar Java analyzer (e.g. using "sonar.java.libraries")
# It should be considered as system classes. In this context the semantic information of "java.lang.Object" and
# "java.lang.Integer" should exists but not other classes like "java.lang.String".

docker run \
  --rm \
  --interactive \
  --tty \
  --name android_sdk \
  --memory "1g" \
  --cpus "2" \
  --volume "${SCRIPT_DIR}:/home/mobiledevops/tmp:rw" \
  -- \
  "mobiledevops/android-sdk-image:34.0.0" \
  "/bin/bash" -c "\
    mkdir /home/mobiledevops/android && \
    cd /home/mobiledevops/android && \
    jar xf /opt/android-sdk-linux/platforms/android-33/android.jar && \
    mkdir /home/mobiledevops/android-min && \
    cd /home/mobiledevops/android-min && \
    cp ../android/AndroidManifest.xml AndroidManifest.xml && \
    cp -r ../android/META-INF META-INF && \
    mkdir -p java/lang && \
    cp -r ../android/java/lang/Object.class java/lang/Object.class && \
    cp -r ../android/java/lang/Integer.class java/lang/Integer.class && \
    jar cf ../android-min.jar . && \
    cp /home/mobiledevops/android-min.jar /home/mobiledevops/tmp/android.jar"
