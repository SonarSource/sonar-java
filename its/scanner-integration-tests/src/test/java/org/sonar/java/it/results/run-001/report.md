# Source-only Java analysis comparison

**Comparison:** VALID · **Candidate:** placeholder (same analyzer and settings)

## Summary

| Metric | Current | Candidate |
|---|---:|---:|
| Scan status | SUCCESS | SUCCESS |
| Java files analyzed | 69 | 69 |
| Findings | 1 | 1 |
| Scan time (ms) | 5946 | 5187 |
| Source characters analyzed | 182211 | 182211 |
| Undefined-type errors | 390 | 390 |
| Source characters with parse errors | N/A | N/A |
| Source characters with analysis exceptions | N/A | N/A |

| Comparison metric | Value |
|---|---:|
| Rules compared | 599 |
| Rules with findings | 1 |
| Shared findings | 1 |
| Current-only findings | 0 |
| Candidate-only findings | 0 |
| Retention of current findings | 100.0% |

## Rules with findings

| Rule | Current | Candidate | Shared | Current only | Candidate only | Retention |
|---|---:|---:|---:|---:|---:|---:|
| java:S6204 | 1 | 1 | 1 | 0 | 0 | 100.0% |

598 rules had no findings in either run and are omitted from this table.

## Current-only findings

None.

## Candidate-only findings

None.

## Current findings

| Rule | File | Line | Message |
|---|---|---:|---|
| java:S6204 | sonar-xml-plugin/src/main/java/org/sonar/plugins/xml/checks/maven/PomElementOrderCheck.java | 94 | Replace this usage of 'Stream.collect(Collectors.toList())' with 'Stream.toList()' and ensure that the list is unmodified. |

## Reading the data

- Both modes use sources and the JDK, without project bytecode or dependency JARs.
- Retention measures agreement with current source-only findings, not accuracy.
- Undefined-type errors are a diagnostic count, not resolution coverage. Missing telemetry is shown as N/A.
- Configured rules may be disabled when dependencies are absent; zero findings do not prove a rule ran.
- Scan times are individual wall-time samples, including engine setup.
