# sonar-skunk - Ruling Tests

## Ruling Tests Exist: No (non-traditional)

## Alternative Approach
- Uses **SIT exporter** + **diffsit** tool
- GitHub Actions workflows for CI
- No traditional LITS or Orchestrator-based ruling tests

## How It Works
- SIT exporter generates analysis results
- `diffsit` tool compares exported results
- CI workflows in GitHub Actions orchestrate the process
- Does not follow the standard ruling test pattern
