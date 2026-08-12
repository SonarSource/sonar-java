# sonar-dart - Ruling Tests

## Ruling Tests Exist: No (no traditional ruling tests)

## Alternative Approach
- Uses **PVF (Performance Validation Framework)** for A/B comparison
- Compares baseline and candidate analyzer versions
- No LITS-based or golden-file ruling tests

## How It Works
- PVF runs the baseline version and candidate version against real projects
- Compares output between the two versions
- Focuses on performance and correctness regression detection
