# sonar-css - Ruling Tests

## Ruling Tests Exist: Yes

## Structure
- Located in `its/ruling/` module
- Uses older Orchestrator version (0.8.0.1209)

## Technologies
- **Orchestrator** (older version `0.8.0.1209`): Starts a real SonarQube server instance
- **LITS Plugin**: Captures issues and compares to baseline
- **JUnit 4**: Test framework (via vintage engine)

## Baseline Format
- JSON files per rule stored in expected resources
- Format: `{"file:path": [line1, line2, ...]}`

## How It Works
1. Orchestrator starts SonarQube with the CSS plugin and LITS plugin
2. Quality profile activates all CSS rules
3. Scanner analyzes CSS test sources
4. LITS plugin captures issues and compares to baseline
5. Test passes only if differences file is empty

## Source Management
- Git submodule for test sources
