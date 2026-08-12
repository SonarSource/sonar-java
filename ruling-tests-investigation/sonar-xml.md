# sonar-xml - Ruling Tests

## Ruling Tests Exist: Yes

## Structure
- Located in `its/ruling/` module
- Single ruling test class
- Sources committed directly in the repository (not a git submodule)

## Technologies
- **Orchestrator**: Starts a real SonarQube server instance
- **LITS Plugin**: Captures issues and compares to baseline
- **Maven**: Build system

## Baseline Format
- JSON files per rule stored in expected resources
- Format: `{"file:path": [line1, line2, ...]}`

## How It Works
1. Orchestrator starts SonarQube with the XML plugin and LITS plugin
2. Quality profile activates all XML rules
3. Scanner analyzes XML test sources
4. LITS plugin captures issues and compares to baseline
5. Test passes only if differences file is empty

## Source Management
- Sources committed directly in the repository (not a git submodule)
