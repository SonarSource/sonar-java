# sonar-vb - Ruling Tests

## Ruling Tests Exist: Yes

## Structure
- Located in `its/ruling/` module
- Test class: `VbRulingTest.java`
- Expected: ~50 JSON files in `its/ruling/src/test/resources/expected/`

## Technologies
- **Orchestrator** (`sonar-orchestrator-junit5`): Starts SonarQube Enterprise LW instance
- **LITS Plugin** (`sonar-lits-plugin:0.11.0.2659`): Issue comparison
- **ProfileGenerator** from `sonar-analyzer-commons`: Generates quality profile
- **fest-assert**: Assertion library
- **JUnit 5**: Test framework
- **Maven**: Build system

## Baseline Format
- JSON files per rule (e.g., `vb-S103.json`, `vb-ParseError.json`)
- Format: `{"file:path": [line1, line2, ...]}`

## How It Works
1. Orchestrator starts SonarQube Enterprise LW with VB plugin and LITS plugin
2. `ProfileGenerator.generateProfile()` creates quality profile with parameter overrides
3. Single `@Test` runs SonarScanner on `its/sources/`
4. LITS properties configured for comparison
5. Asserts differences file is empty

## Notable Features
- Simple, single-test ruling setup
- Uses `fest-assert` (older assertion library) instead of AssertJ

## Source Management
- Sources committed directly in the repository (no git submodule)
- Contains projects: RIMS_20110513, vbmake, multimodule, etc.
