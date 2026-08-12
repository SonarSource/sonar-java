# sonar-cpp - Ruling Tests

## Ruling Tests Exist: No (no traditional ruling tests)

## Alternative Approach
- Uses **LLVM lit tests** for rule validation - inline test expectations in C++ test files
- Uses **Orchestrator** for plugin integration tests (not ruling-style)
- No LITS-based ruling tests

## How It Works
- Rules are validated through lit-style tests where expected diagnostics are annotated inline in test files
- Plugin integration tests use Orchestrator but do not compare issue snapshots against golden files
