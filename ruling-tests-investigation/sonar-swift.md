# sonar-swift - Ruling Tests

## Ruling Tests Exist: Yes

## Structure
- SIT-based approach
- `RulingRules.java` discovers rules by scanning plugin JAR metadata
- ~95 golden files for expected results
- Custom diff logic implemented in Java

## Technologies
- **SIT** (`sonar-scanner-integration-tester`): Runs scanner engine in-process, no SQ server needed
- **Custom Java diff logic**: Compares actual issues against golden files
- Rules discovered from JAR metadata JSON resources

## Baseline Format
- JSON golden files (~95 files)
- Format: `{"file:path": [line1, line2, ...]}`

## How It Works
1. SIT creates mock server context with the Swift plugin loaded
2. Rules discovered by scanning JAR metadata entries
3. Scanner runs in-process against test sources
4. Custom diff logic compares actual vs expected issues
5. Test passes only if no differences found

## Source Management
- Test sources managed as part of the project
