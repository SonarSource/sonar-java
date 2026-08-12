# SonarJS - Ruling Tests

## Ruling Tests Exist: Yes

## Structure
- Located in `its/ruling/` module
- Covers JS, TS, and CSS languages
- Source projects are in `its/sources/` git submodule (`jsts-test-sources`)
- 50+ test projects analyzed

## Technologies
- **Orchestrator**: Starts a real SonarQube server instance
- **LITS Plugin**: Captures issues and compares to baseline
- **Maven**: Build system
- **ProfileGenerator**: Generates quality profile with all rules

## Baseline Format
- JSON files per rule per project stored in expected resources
- Format: `{"file:path": [line1, line2, ...]}`

## How It Works
1. Orchestrator starts SonarQube with the JS/TS plugin and LITS plugin
2. ProfileGenerator creates a quality profile activating all rules
3. Test methods analyze real open-source JavaScript/TypeScript projects
4. LITS plugin captures detected issues and compares to baseline JSON files
5. Test passes only if actual results match expected (zero differences)

## Source Management
- Git submodule (`jsts-test-sources`)
