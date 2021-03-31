mkdir .jdk
curl -L "https://download.oracle.com/otn-pub/java/jdk/15.0.2%2B7/0d1cfde4252546c6931946de8db48ee2/jdk-15.0.2_windows-x64_bin.zip" -o .jdk\jdk.zip
unzip ".jdk\jdk.zip" ".jdk"
.jdk\bin\java -version
SET JAVA_HOME=%cd%\.jdk

mvn.cmd clean verify
