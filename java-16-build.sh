#!/bin/sh

mkdir .jdk
curl -L "https://download.java.net/java/GA/jdk15.0.2/0d1cfde4252546c6931946de8db48ee2/7/GPL/openjdk-15.0.2_linux-x64_bin.tar.gz" -o .jdk/jdk.tar.gz
tar -xzf .jdk/jdk.tar.gz -C .jdk --strip-components 1
.jdk/bin/java -version
export JAVA_HOME=$PWD/.jdk

regular_mvn_build_deploy_analyze
