# sonar-plsql - Ruling Tests

## Ruling Tests Exist: Yes

## Structure
- Located in `its/ruling/` module under `its` parent (which also has `its/plugin/`)
- Test class: `PlSqlRulingTest.java`
- ~120 expected JSON files in `its/ruling/src/test/resources/expected/`

## Technologies
- **Orchestrator** (`sonar-orchestrator-junit5`): Starts SonarQube Enterprise instance
- **LITS Plugin** (`sonar-lits-plugin:0.11.0.2659`): Issue comparison
- **ProfileGenerator** from `sonar-analyzer-commons`: Generates quality profile with custom parameters
- **JUnit 5**: Test framework
- **Maven**: Build system

## Baseline Format
- JSON files per rule (e.g., `plsql-S104.json`, `plsql-SelectStarCheck.json`)
- Format: `{"file:path": [line1, line2, ...]}`

## How It Works
1. Orchestrator starts SonarQube Enterprise with PL/SQL plugin and LITS plugin
2. `ProfileGenerator.generateProfile()` creates quality profile with extensive custom parameter overrides (naming patterns, complexity thresholds, etc.)
3. One rule disabled (`S5245`)
4. Single `@Test` runs SonarScanner on `its/sources/` with wide file suffixes
5. LITS properties configured for comparison
6. Asserts differences file is empty

## Notable Features
- Extensive rule parameter customization via ProfileGenerator
- Wide file suffix support (`.sql,.spl,.txt,.alz,.ins,.tns,.INS,.spb,.sps,.ppb,.pps,.log,.SP,.tab,.pkb,.bak,.pks,.seq`)

## Source Management
- Sources in `its/sources/` directory (contains real PL/SQL projects like `3dcitydb`)
