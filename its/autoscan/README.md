# Autoscan

The tests in this module are designed to detect differences between the issues the Java analyzer can find with and without compiled code.
The goal here is to spot and fix the potential FPs, and verify the expected FNs between that would show up in [SonarCloud's automatic analysis](https://docs.sonarcloud.io/advanced-setup/automatic-analysis/).

## Testing

### Compiling the sources

Make sure that the `java-checks-tests-sources` module has been compiled (ie: the .class files in `java-checks-tests-sources/target/` are up to date).

In doubt, go to the top-level of the project and run:
```shell
# cd ../../ or back to the top level of sonar-java
mvn clean compile --projects java-checks-test-sources --also-make-dependents
```

## Running the tests

To run the tests from this folder, run:
```shell
mvn clean package --batch-mode --errors --show-version \
   --activate-profiles it-autoscan \
  -Dsonar.runtimeVersion=LATEST_RELEASE[9.9]
```


## Updating the expected results

The expected results are listed in [autoscan-diff-by-rules.json](src%2Ftest%2Fresources%2Fautoscan%2Fautoscan-diff-by-rules.json). 
