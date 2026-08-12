# sonar-apex - Ruling Tests

## Ruling Tests Exist: Yes

## Structure
- Located in `its/ruling/` module
- Part of the Slang family of analyzers
- Includes metric assertions in ruling test

## Technologies
- **Orchestrator**: Starts a real SonarQube server instance
- **LITS Plugin**: Captures issues and compares to baseline
- **JUnit 4**: Test framework

## Baseline Format
- JSON files per rule stored in expected resources
- Format: `{"file:path": [line1, line2, ...]}`

## How It Works
1. Orchestrator starts SonarQube with the Apex plugin and LITS plugin
2. Quality profile activates all Apex rules
3. Scanner analyzes Apex test sources
4. LITS plugin captures issues and compares to baseline
5. Test passes only if differences file is empty
6. Additional metric assertions verify code metrics

## Source Management
- Standard Slang family source management
