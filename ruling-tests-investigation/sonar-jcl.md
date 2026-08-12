# sonar-jcl - Ruling Tests

## Ruling Tests Exist: Yes

## Structure
- Located in `its/ruling/` module
- Test class: `RulingTest.java`
- Helper: `TestUtils.java`
- Expected results in `its/ruling/src/test/resources/expected/<projectKey>/jcl-<ruleKey>.json`
- 6 test projects: bbva, MainframeJCL, cobol-programming-course, mainframeadventures, mvs, sysgen

## Technologies
- **Orchestrator** (`sonar-orchestrator-junit5`): Starts SonarQube Enterprise LW instance
- **LITS Plugin** (`sonar-lits-plugin:0.11.0.2659`): Issue comparison
- **JUnit 5**: Test framework with `@ParameterizedTest @CsvSource`
- **Maven**: Build system
- No SIT dependency

## Baseline Format
- JSON files per rule per project
- Format: `{"file:path": [line1, line2, ...]}`

## How It Works
1. Orchestrator starts SonarQube Enterprise with JCL plugin and LITS plugin
2. Quality profile built in-memory via `TestUtils.profile()` with 20 standard rules
3. 7 template rule instances created via SonarQube WS API (e.g., bbvaForbiddenPgmCheck, bbvaJobClassCheck)
4. `@ParameterizedTest` runs 5 open-source projects + dedicated `@Test` for bbva
5. LITS plugin captures issues and compares to baseline
6. Test passes only if differences file is empty

## Notable Features
- Custom profile loading (builds XML in-memory rather than from file)
- Template rule instantiation via WS API during test setup
- Requires Enterprise LW edition

## Source Management
- Sources in `its/sources/` (referenced as `../sources/` from ruling module)
