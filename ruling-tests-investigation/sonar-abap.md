# sonar-abap - Ruling Tests

## Ruling Tests Exist: Yes (hybrid SIT + LITS)

## Structure
- Located in `its/ruling/` module
- Uses SIT as the test runner but LITS for issue comparison

## Technologies
- **SIT** (`sonar-scanner-integration-tester:0.5.0.1256`): Older SIT version as test runner
- **LITS Plugin**: Still used for issue comparison (dump/diff mechanism)
- **Maven**: Build system

## Baseline Format
- JSON files per rule stored in expected resources
- Format: `{"file:path": [line1, line2, ...]}`

## How It Works
1. SIT creates mock server context with the ABAP plugin and LITS plugin loaded
2. Quality profile loaded from static XML or generated
3. Scanner runs in-process via SIT
4. LITS plugin captures issues via scanner properties
5. Test passes only if LITS differences file is empty

## Source Management
- Test sources managed within the project
