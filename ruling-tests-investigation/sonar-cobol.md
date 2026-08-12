# sonar-cobol - Ruling Tests

## Ruling Tests Exist: Yes

## Structure
- Located in `its/ruling/` module
- Static `profile.xml` for quality profile
- 5 dialect-specific tests (different COBOL dialects)

## Technologies
- **Orchestrator**: Starts a real SonarQube server instance
- **LITS Plugin**: Captures issues and compares to baseline
- **JUnit 5**: Test framework
- **Maven**: Build system

## Baseline Format
- JSON files per rule stored in expected resources
- Format: `{"file:path": [line1, line2, ...]}`

## How It Works
1. Orchestrator starts SonarQube with the COBOL plugin and LITS plugin
2. Static `profile.xml` defines the quality profile
3. Each test method targets a specific COBOL dialect
4. LITS plugin captures issues and compares to baseline
5. Test passes only if differences file is empty

## Source Management
- Git submodule for test sources
