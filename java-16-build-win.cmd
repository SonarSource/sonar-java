echo %JAVA_HOME%
choco install AdoptOpenJDK15 --params="/ADDLOCAL=FeatureMain,FeatureEnvironment,FeatureJarFileRunWith,FeatureJavaHome"
echo %JAVA_HOME%
mvn.cmd clean verify
