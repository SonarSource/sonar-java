# sonar-iac / sonar-iac-enterprise - Ruling Tests

## Ruling Tests Exist: Yes

## Structure
- Located in `private/its/ruling/` (enterprise) and `ruling/` (community)
- Enterprise test: `IacRulingTest.java` - one `@Test` per language/domain
- Community test: `IacCommunityRulingTest.java` - community plugin only
- `RulingRules.java` discovers rules by scanning analyzer JAR metadata
- Covers: Terraform, CloudFormation, Kubernetes, Docker, ARM, Ansible, JSON, YAML, GitHub Actions, Azure Pipelines, Shell, JVM framework config

## Technologies
- **SIT** (`sonar-scanner-integration-tester`): Runs scanner engine in-process, no SQ server needed
- **LITS Plugin** (`sonar-lits-plugin:0.11.0.2659`): Still used for issue comparison via scanner properties
- **Gradle**: Build system (Kotlin DSL)
- **JUnit 5**: Test framework

## Baseline Format
- JSON files per rule per language stored in `src/integrationTest/resources/expected/<language>/`
- Format: `{"file:path": [line1, line2, ...]}`
- Community Docker tests use separate `expected/docker-community/` override files

## How It Works
1. SIT creates mock server context with enterprise/community plugin + LITS plugin
2. Rules discovered from JAR metadata JSON resources (`RulingRules.nativeRules()`)
3. Scanner runs in-process with `sonar.lits.dump.old`/`sonar.lits.dump.new`/`sonar.lits.differences` properties
4. Each test class runs in a fresh JVM (`setForkEvery(1)`) due to native library constraints
5. Test passes if differences file is empty

## Source Management
- Git submodules under `private/its/sources/`
- Additional inline sources in `src/integrationTest/resources/sources/<language>/`
