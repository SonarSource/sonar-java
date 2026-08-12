# sonar-ruby - Ruling Tests

## Ruling Tests Exist: Yes

## Structure
- Located in `its/ruling/` module
- Part of the Slang family of analyzers

## Technologies
- **Orchestrator**: Starts a real SonarQube server instance
- **LITS Plugin**: Captures issues and compares to baseline
- **JUnit 4** (via vintage engine): Test framework

## Baseline Format
- JSON files per rule stored in expected resources
- Format: `{"file:path": [line1, line2, ...]}`

## How It Works
1. Orchestrator starts SonarQube with the Ruby plugin and LITS plugin
2. Quality profile activates all Ruby rules
3. Scanner analyzes Ruby test sources
4. LITS plugin captures issues and compares to baseline
5. Test passes only if differences file is empty

## Source Management
- Standard source management (Slang family pattern)
