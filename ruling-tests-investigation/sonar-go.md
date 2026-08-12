# sonar-go - Ruling Tests

## Ruling Tests Exist: Yes

## Structure
- Located in ruling test module
- Uses SIT-based approach (sonar-scanner-integration-tester)
- `RulingRules.java` discovers rules by scanning plugin JAR metadata
- 4 git submodules for test sources

## Technologies
- **SIT** (`sonar-scanner-integration-tester`): Runs scanner engine in-process, no SQ server needed
- **Custom diff logic**: Compares actual issues against golden JSON files in Java
- **LITS** still used for diff comparison at the data level

## Baseline Format
- JSON golden files, one per rule
- Format: `{"file:path": [line1, line2, ...]}`

## How It Works
1. SIT creates a mock server context with the Go plugin loaded
2. Rules are discovered by scanning JAR entries matching `(org|com)/sonar/l10n/<language>/rules/<language>/<RuleKey>.json`
3. Scanner runs in-process against test source projects
4. Results are compared against golden JSON files
5. Test passes only if actual matches expected

## Source Management
- 4 git submodules for test source code
