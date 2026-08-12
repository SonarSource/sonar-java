# sonar-tsql - Ruling Tests

## Ruling Tests Exist: Yes

## Structure
- Located in `its/ruling/` module
- Test class: `TsqlRulingTest.java`
- Expected: ~70+ JSON files in `its/ruling/src/test/resources/expected/`
- Static `profile.xml` as base, augmented programmatically

## Technologies
- **Orchestrator** (`sonar-orchestrator-junit5`): Starts SonarQube Enterprise LW instance
- **LITS Plugin** (`sonar-lits-plugin:0.11.0.2659`): Issue comparison
- **JUnit 5**: Test framework
- **sonar-plugin-api**: For reading rule annotations
- **sonar-ws**: For WS API calls (template rule creation)
- **Maven**: Build system

## Baseline Format
- JSON files per rule (e.g., `tsql-S103.json`, `tsql-S125.json`)
- Format: `{"file:path": [line1, line2, ...]}`

## How It Works
1. `generateFullProfile()` reads base `profile.xml`, adds all rules from `TsqlCheckList.checks()` via annotation scanning
2. Orchestrator starts SonarQube Enterprise LW with T-SQL plugin and LITS plugin
3. Profile restored at startup via `restoreProfileAtStartup()`
4. Two template rules instantiated via WS API (S4820: DateFirstConfigured, ArithAbortConfigured)
5. Single `ruling()` test runs SonarScanner on `its/sources/`
6. LITS properties configured for comparison
7. Asserts differences file is empty; fails with diff content if non-empty

## Notable Features
- Auto-generates profile from check list annotations (reads `@Rule(key=...)`)
- Template rule instantiation via WS API
- Additional `@Disabled` methods for manual debugging (single project, keep SQ alive, attach debugger)
- Sources committed directly in the repository

## Source Management
- Sources in `its/sources/` (committed in repo, not a submodule)
