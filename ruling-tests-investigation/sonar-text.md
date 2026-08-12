# sonar-text - Ruling Tests

## Ruling Tests Exist: No (no traditional ruling tests)

## Alternative Approach
- Uses **specification-based example testing** via `AbstractRuleExampleTest`
- Rules are validated through inline YAML specification examples
- No LITS-based or SIT-based ruling tests

## How It Works
- Each rule has inline examples in its specification
- `AbstractRuleExampleTest` parses these examples and validates the rule behavior
- No golden file comparison or real-project analysis
