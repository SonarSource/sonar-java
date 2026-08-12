# sonar-rpg - Ruling Tests

## Ruling Tests Exist: Yes (SIT-based, no LITS)

## Structure
- Located in `its/ruling/` module
- Test class: `RpgRulingTest.java`
- Helper class: `ExpectedIssues.java` (custom JSON loader)
- Expected: `its/ruling/src/test/resources/expected/` (~65 JSON files)
- Internal expected: `its/ruling/src/test/resources/internal-expected/` (6 JSON files)
- Static `profile.xml` for quality profile

## Technologies
- **SIT** (`sonar-scanner-integration-tester:0.5.0.1256`): Test runner
- **No LITS Plugin**: Custom issue comparison via `ExpectedIssues` helper
- **JUnit 4**: Test framework
- **Gson**: JSON parsing for expected issues
- **Maven**: Build system

## Baseline Format
- JSON files per rule (e.g., `rpg-S100.json`, `rpg-S1192.json`)
- Format: `{"projectKey:filePath": [line1, line2, ...]}`
- Line 0 represents a file-level issue

## How It Works
1. SIT creates mock server context (`Product.CLOUD`) with RPG plugin loaded
2. Active rules loaded from `profile.xml` via `loadActiveRulesFromXmlProfile()`
3. Two test projects analyzed: `sources` (external) and `internal`
4. After scanning, issues collected from `ScannerOutputReader`, grouped by rule key and file path
5. `ExpectedIssues.load()` parses expected JSON files using Gson
6. AssertJ `assertThat(actual).isEqualTo(expected)` compares directly

## Notable Features
- **No LITS dependency** - custom `ExpectedIssues` class handles JSON comparison
- Two separate test data sets (open-source + internal)
- Verifies no parse errors in log output

## Source Management
- External sources in `its/sources/src/`
- Internal sources in `its/internal/src/`
