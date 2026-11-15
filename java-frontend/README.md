# java-frontend

## Running with the SemSitter analyzer

The frontend gets its semantic model from the SemSitter analyzer, an executable it starts per module.

- `sonar.java.semsitter.executable` (or the `SEMSITTER_ANALYZER` environment variable): path of the analyzer.
  When unset, the executable packaged under `/semsitter/analyzer-<platform>.xz` is extracted and used.
- `sonar.java.semsitter.args`: extra command-line arguments for the analyzer, separated by spaces.
- `sonar.java.semsitter.scanFiles`: `false` lets the analyzer walk the source roots instead of the analyzed files.
- `sonar.java.semsitter.prefetch`: number of files whose answers are requested ahead.

Unit tests that parse Java need the analyzer. When none is available they are skipped, not failed:

```
mvn test -Dsonar.java.semsitter.executable=/path/to/analyzer
```
