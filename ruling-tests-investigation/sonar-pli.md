# sonar-pli - Ruling Tests

## Ruling Tests Exist: Yes (hybrid SIT + LITS)

## Structure
- Located in `its/ruling/` module
- Test class: `PliRulingTest.java`
- Static `profile.xml` for quality profile
- Expected results: 17 JSON files in `its/ruling/src/test/resources/expected/`

## Technologies
- **SIT** (`sonar-scanner-integration-tester:0.5.0.1256`): Test runner (no SQ server needed)
- **LITS Plugin** (`sonar-lits-plugin:0.11.0.2659`): Loaded as plugin for issue comparison
- **JUnit 4**: Test framework
- **Maven**: Build system

## Baseline Format
- JSON files per rule (e.g., `pli-Dcl.json`, `pli-S1147.json`)
- Format: `{"file:path": [line1, line2, ...]}`

## How It Works
1. SIT creates SonarQube Cloud-like embedded engine (`SonarServerContext.Product.CLOUD`)
2. Loads PL/I plugin from local build + LITS plugin from Maven
3. Active rules loaded from static `profile.xml` via `loadActiveRulesFromXmlProfile()`
4. Scanner runs in-process with PL/I-specific settings (`sonar.pli.marginLeft`, `sonar.pli.marginRight`)
5. LITS properties set: `sonar.lits.dump.old` (expected), `sonar.lits.dump.new` (actual), `sonar.lits.differences`
6. Asserts differences file is empty

## Notable Features
- Single `@Test` method scanning all sources against all rules at once
- Uses SIT's DSL-based API (`SonarServerContext.builder()` / `ScannerInput.create()`)

## Source Management
- Git submodule (`its/sources` -> `pli-test-sources`)
