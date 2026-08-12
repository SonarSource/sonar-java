# sonar-php - Ruling Tests

## Ruling Tests Exist: Yes

## Structure
- Located in `its/ruling/` module
- Main test class: `PhpRulingTest.java`
- Source projects are in `its/sources/` git submodule
- Expected results in `its/ruling/src/test/resources/expected/`

## Technologies
- **Orchestrator**: Starts a real SonarQube server instance
- **LITS Plugin** (`sonar-lits-plugin`): Captures issues and compares to baseline
- **Gradle**: Build system
- **ProfileGenerator** from `sonar-analyzer-commons`: Generates quality profile with all rules
- 8 test projects analyzed

## Baseline Format
- JSON files per rule per project stored in expected resources
- Format: `{"file:path": [line1, line2, ...]}`

## How It Works
1. Orchestrator starts SonarQube with the PHP plugin and LITS plugin
2. ProfileGenerator creates a quality profile activating all rules
3. Each test method analyzes a real open-source PHP project
4. LITS plugin captures detected issues and compares to baseline JSON files
5. Test passes only if actual results match expected (zero differences)

## Source Management
- Git submodule pointing to external test sources repository
