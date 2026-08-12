# sonar-java - Ruling Tests

## Ruling Tests Exist: Yes

## Structure
- Located in `its/ruling/` module
- Main test class: `its/ruling/src/test/java/org/sonar/java/it/JavaRulingTest.java`
- Profile generator: `ProfileGenerator.java`
- Performance statistics: `PerformanceStatistics.java`
- Source projects are in `its/sources/` git submodule

## Technologies
- **Orchestrator**: Starts a real SonarQube server instance
- **LITS Plugin** (`sonar-lits-plugin:0.11.0.2659`): Captures issues and compares to baseline
- **Maven**: Projects are built and analyzed via `mvn clean package sonar:sonar`
- **JUnit 4**: Test framework for ruling tests

## Baseline Format
- JSON files per rule per project stored in `its/ruling/src/test/resources/{project}/java-{RULE_KEY}.json`
- Format: `{"file:path": [line1, line2, ...]}`
- One file per rule, containing all file paths and line numbers where issues are detected

## Test Projects
- commons-beanutils, eclipse-jetty, guava, jboss-ejb3-tutorial, mall, sonar-server, regex-examples, vibebot

## How It Works
1. Orchestrator starts SonarQube with the java plugin and LITS plugin
2. ProfileGenerator creates a quality profile with all rules enabled
3. Each test method analyzes a real open-source project
4. LITS plugin captures detected issues and compares to baseline JSON files
5. Test passes only if actual results match expected (zero differences)

## Updating Baselines
- Actual results written to `its/ruling/target/actual/`
- Developer reviews diffs and copies correct results to `src/test/resources/`

## Notable Features
- Supports both batch and file-by-file analysis modes
- Includes incremental analysis tests (Enterprise Edition only)
- Performance statistics collection during analysis
- CI workflows for ruling diff comments on PRs
