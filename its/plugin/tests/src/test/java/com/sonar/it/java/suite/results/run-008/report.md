# Source-only Java analysis comparison

**Runner:** Orchestrator MavenBuild (`sonar:sonar`)

**Comparison:** VALID · **Candidate:** placeholder (same analyzer and settings)

## Summary

| Metric | Current | Candidate |
|---|---:|---:|
| Scan status | SUCCESS | SUCCESS |
| Java files analyzed | 69 | 69 |
| Identifiers (total) | 8007 | 8007 |
| Known identifiers | 4171 | 4171 |
| Unknown identifiers | 3836 | 3836 |
| Unknown identifiers (%) | 47.908% | 47.908% |
| Files with no unknown identifiers | 1 / 69 (1.449%) | 1 / 69 (1.449%) |
| Findings | 1 | 1 |
| Median Maven/server time (ms) | 17227.0 | 16216.0 |
| Median JavaSensor time (ms) | 5785.0 | 5812.0 |
| Source characters analyzed | 182211 | 182211 |
| Undefined-type errors | 390 | 390 |
| Source characters with parse errors | N/A | N/A |
| Source characters with analysis exceptions | N/A | N/A |

| Comparison metric | Value |
|---|---:|
| Change in unknown identifiers (percentage points) | +0.000 |
| Net change in unknown identifier count | +0 |
| Files improved (unknown %) | 0 |
| Files unchanged (unknown %) | 69 |
| Files regressed (unknown %) | 0 |
| Files without comparable identifier percentages | 0 |
| Rules compared | 599 |
| Rules with findings | 1 |
| Shared findings | 1 |
| Current-only findings | 0 |
| Candidate-only findings | 0 |
| Retention of current findings | 100.0% |

## Run metadata

| Setting | Value |
|---|---|
| Analyzer checkout dirty | true |
| Analyzer checkout revision | a971ce3c211246045c6d70c5f4ff11f3ecd45747 |
| Analyzer plugin SHA-256 | d6157e011a17c5c18339cfbf75f0a92e6b678720757c803fbad3295921ed806c |
| Analyzer plugin version | 8.45.0-SNAPSHOT |
| Binding correctness/candidate | Semantic binding probes: 5 checked, 0 failed. |
| Binding correctness/current | Semantic binding probes: 5 checked, 0 failed. |
| Binding probe plugin SHA-256 | dc41818645a3b8c28861435869d3c318e20b15797ca812f3a5dbb9c3dae7512b |
| Candidate properties | none (placeholder) |
| JDK | 21.0.10 / Microsoft |
| Java language level | 21 |
| Maven scanner version | 5.9.0.7291 |
| Measured repetitions per mode | 3 |
| Profile | Sonar way |
| Recorded at (UTC) | 2026-10-07T08:21:40.940535Z |
| Repeated result stability | stable |
| Server version | 26.10.0.132816 |
| Server workspace | /Users/matthew.elliott/Work/Code/sonar-java/its/plugin/tests/target/comparison-orchestrator-10293193997941770545 |
| Target checkout dirty | false |
| Target checkout revision | 627ac4747bef73345338aae435b7c987873cf86f |
| Target source snapshot SHA-256 | 9ac9e9d9cc60dfc68a4e174741e5db76abcee26dbc7ecd41a72af5bd38303586 |
| Timing protocol | one warm-up per mode excluded; measured order alternates current/candidate |

## Measured timings

Wall time includes Maven and server processing; analyzer time measures JavaSensor execution (parsing, semantics, checks, and metrics). Warm-ups are excluded. Missing analyzer timings are N/A.

| Sample | Current wall (ms) | Candidate wall (ms) | Current analyzer (ms) | Candidate analyzer (ms) |
|---|---:|---:|---:|---:|
| 1 | 15988 | 15959 | 5675.0 | 5764.0 |
| 2 | 40135 | 18072 | 21932.0 | 5961.0 |
| 3 | 17227 | 16216 | 5785.0 | 5812.0 |
| Median | 17227.0 | 16216.0 | 5785.0 | 5812.0 |

## Largest semantic improvements

Top five by absolute change in unknown percentage.

None.

## Largest semantic regressions

Top five by absolute change in unknown percentage.

None.

## Top files contributing unknown identifiers

Top five by the largest unknown count in either run. Values are **current / candidate**.

| File | Unknown identifiers | Share of project unknown identifiers |
|---|---:|---:|
| sonar-xml-plugin/src/main/java/org/sonar/plugins/xml/XmlSensor.java | 294 / 294 | 7.664% / 7.664% |
| sonar-xml-plugin/src/main/java/org/sonar/plugins/xml/checks/CheckList.java | 263 / 263 | 6.856% / 6.856% |
| sonar-xml-plugin/src/main/java/org/sonar/plugins/xml/XmlHighlighting.java | 167 / 167 | 4.353% / 4.353% |
| sonar-xml-plugin/src/main/java/org/sonar/plugins/xml/checks/security/HardcodedCredentialsCheck.java | 146 / 146 | 3.806% / 3.806% |
| sonar-xml-plugin/src/main/java/org/sonar/plugins/xml/checks/maven/DisallowedDependenciesCheck.java | 141 / 141 | 3.676% / 3.676% |

## Semantics per file

Counts and percentages are **current / candidate**. Change is candidate minus current.

| File | Total identifiers | Known identifiers | Unknown identifiers | Unknown % | Change (pp) |
|---|---:|---:|---:|---:|---:|
| sonar-xml-plugin/src/main/java/org/sonar/plugins/xml/LineCounter.java | 291 / 291 | 151 / 151 | 140 / 140 | 48.110% / 48.110% | +0.000 |
| sonar-xml-plugin/src/main/java/org/sonar/plugins/xml/Utils.java | 49 / 49 | 23 / 23 | 26 / 26 | 53.061% / 53.061% | +0.000 |
| sonar-xml-plugin/src/main/java/org/sonar/plugins/xml/Xml.java | 102 / 102 | 72 / 72 | 30 / 30 | 29.412% / 29.412% | +0.000 |
| sonar-xml-plugin/src/main/java/org/sonar/plugins/xml/XmlHighlighting.java | 326 / 326 | 159 / 159 | 167 / 167 | 51.227% / 51.227% | +0.000 |
| sonar-xml-plugin/src/main/java/org/sonar/plugins/xml/XmlPlugin.java | 50 / 50 | 17 / 17 | 33 / 33 | 66.000% / 66.000% | +0.000 |
| sonar-xml-plugin/src/main/java/org/sonar/plugins/xml/XmlRulesDefinition.java | 71 / 71 | 35 / 35 | 36 / 36 | 50.704% / 50.704% | +0.000 |
| sonar-xml-plugin/src/main/java/org/sonar/plugins/xml/XmlSensor.java | 519 / 519 | 225 / 225 | 294 / 294 | 56.647% / 56.647% | +0.000 |
| sonar-xml-plugin/src/main/java/org/sonar/plugins/xml/XmlSonarWayProfile.java | 81 / 81 | 35 / 35 | 46 / 46 | 56.790% / 56.790% | +0.000 |
| sonar-xml-plugin/src/main/java/org/sonar/plugins/xml/api/XmlProfileRegistrar.java | 28 / 28 | 12 / 12 | 16 / 16 | 57.143% / 57.143% | +0.000 |
| sonar-xml-plugin/src/main/java/org/sonar/plugins/xml/api/package-info.java | 8 / 8 | 7 / 7 | 1 / 1 | 12.500% / 12.500% | +0.000 |
| sonar-xml-plugin/src/main/java/org/sonar/plugins/xml/checks/CharBeforePrologCheck.java | 59 / 59 | 14 / 14 | 45 / 45 | 76.271% / 76.271% | +0.000 |
| sonar-xml-plugin/src/main/java/org/sonar/plugins/xml/checks/CheckList.java | 314 / 314 | 51 / 51 | 263 / 263 | 83.758% / 83.758% | +0.000 |
| sonar-xml-plugin/src/main/java/org/sonar/plugins/xml/checks/CommentContainsPatternChecker.java | 110 / 110 | 66 / 66 | 44 / 44 | 40.000% / 40.000% | +0.000 |
| sonar-xml-plugin/src/main/java/org/sonar/plugins/xml/checks/CommentedOutCodeCheck.java | 279 / 279 | 177 / 177 | 102 / 102 | 36.559% / 36.559% | +0.000 |
| sonar-xml-plugin/src/main/java/org/sonar/plugins/xml/checks/FixmeCommentCheck.java | 25 / 25 | 19 / 19 | 6 / 6 | 24.000% / 24.000% | +0.000 |
| sonar-xml-plugin/src/main/java/org/sonar/plugins/xml/checks/IndentationCheck.java | 344 / 344 | 251 / 251 | 93 / 93 | 27.035% / 27.035% | +0.000 |
| sonar-xml-plugin/src/main/java/org/sonar/plugins/xml/checks/LineLengthCheck.java | 114 / 114 | 62 / 62 | 52 / 52 | 45.614% / 45.614% | +0.000 |
| sonar-xml-plugin/src/main/java/org/sonar/plugins/xml/checks/NewlineCheck.java | 271 / 271 | 176 / 176 | 95 / 95 | 35.055% / 35.055% | +0.000 |
| sonar-xml-plugin/src/main/java/org/sonar/plugins/xml/checks/ParsingErrorCheck.java | 34 / 34 | 13 / 13 | 21 / 21 | 61.765% / 61.765% | +0.000 |
| sonar-xml-plugin/src/main/java/org/sonar/plugins/xml/checks/TabCharacterCheck.java | 168 / 168 | 90 / 90 | 78 / 78 | 46.429% / 46.429% | +0.000 |
| sonar-xml-plugin/src/main/java/org/sonar/plugins/xml/checks/TodoCommentCheck.java | 25 / 25 | 19 / 19 | 6 / 6 | 24.000% / 24.000% | +0.000 |
| sonar-xml-plugin/src/main/java/org/sonar/plugins/xml/checks/XPathCheck.java | 367 / 367 | 246 / 246 | 121 / 121 | 32.970% / 32.970% | +0.000 |
| sonar-xml-plugin/src/main/java/org/sonar/plugins/xml/checks/ejb/DefaultInterceptorsLocationCheck.java | 59 / 59 | 16 / 16 | 43 / 43 | 72.881% / 72.881% | +0.000 |
| sonar-xml-plugin/src/main/java/org/sonar/plugins/xml/checks/ejb/InterceptorExclusionsCheck.java | 71 / 71 | 23 / 23 | 48 / 48 | 67.606% / 67.606% | +0.000 |
| sonar-xml-plugin/src/main/java/org/sonar/plugins/xml/checks/ejb/package-info.java | 9 / 9 | 8 / 8 | 1 / 1 | 11.111% / 11.111% | +0.000 |
| sonar-xml-plugin/src/main/java/org/sonar/plugins/xml/checks/hibernate/DatabaseSchemaUpdateCheck.java | 83 / 83 | 36 / 36 | 47 / 47 | 56.627% / 56.627% | +0.000 |
| sonar-xml-plugin/src/main/java/org/sonar/plugins/xml/checks/hibernate/package-info.java | 9 / 9 | 8 / 8 | 1 / 1 | 11.111% / 11.111% | +0.000 |
| sonar-xml-plugin/src/main/java/org/sonar/plugins/xml/checks/maven/ArtifactIdNamingConventionCheck.java | 122 / 122 | 62 / 62 | 60 / 60 | 49.180% / 49.180% | +0.000 |
| sonar-xml-plugin/src/main/java/org/sonar/plugins/xml/checks/maven/DependencyWithSystemScopeCheck.java | 132 / 132 | 65 / 65 | 67 / 67 | 50.758% / 50.758% | +0.000 |
| sonar-xml-plugin/src/main/java/org/sonar/plugins/xml/checks/maven/DeprecatedPomPropertiesCheck.java | 145 / 145 | 68 / 68 | 77 / 77 | 53.103% / 53.103% | +0.000 |
| sonar-xml-plugin/src/main/java/org/sonar/plugins/xml/checks/maven/DisallowedDependenciesCheck.java | 249 / 249 | 108 / 108 | 141 / 141 | 56.627% / 56.627% | +0.000 |
| sonar-xml-plugin/src/main/java/org/sonar/plugins/xml/checks/maven/GroupIdNamingConventionCheck.java | 122 / 122 | 62 / 62 | 60 / 60 | 49.180% / 49.180% | +0.000 |
| sonar-xml-plugin/src/main/java/org/sonar/plugins/xml/checks/maven/PomElementOrderCheck.java | 197 / 197 | 122 / 122 | 75 / 75 | 38.071% / 38.071% | +0.000 |
| sonar-xml-plugin/src/main/java/org/sonar/plugins/xml/checks/maven/helpers/MavenDependencyMatcher.java | 100 / 100 | 100 / 100 | 0 / 0 | 0.000% / 0.000% | +0.000 |
| sonar-xml-plugin/src/main/java/org/sonar/plugins/xml/checks/maven/helpers/PatternMatcher.java | 47 / 47 | 43 / 43 | 4 / 4 | 8.511% / 8.511% | +0.000 |
| sonar-xml-plugin/src/main/java/org/sonar/plugins/xml/checks/maven/helpers/RangedVersionMatcher.java | 174 / 174 | 164 / 164 | 10 / 10 | 5.747% / 5.747% | +0.000 |
| sonar-xml-plugin/src/main/java/org/sonar/plugins/xml/checks/maven/helpers/StringMatcher.java | 46 / 46 | 38 / 38 | 8 / 8 | 17.391% / 17.391% | +0.000 |
| sonar-xml-plugin/src/main/java/org/sonar/plugins/xml/checks/maven/helpers/package-info.java | 10 / 10 | 9 / 9 | 1 / 1 | 10.000% / 10.000% | +0.000 |
| sonar-xml-plugin/src/main/java/org/sonar/plugins/xml/checks/maven/package-info.java | 9 / 9 | 8 / 8 | 1 / 1 | 11.111% / 11.111% | +0.000 |
| sonar-xml-plugin/src/main/java/org/sonar/plugins/xml/checks/package-info.java | 8 / 8 | 7 / 7 | 1 / 1 | 12.500% / 12.500% | +0.000 |
| sonar-xml-plugin/src/main/java/org/sonar/plugins/xml/checks/security/HardcodedCredentialsCheck.java | 535 / 535 | 389 / 389 | 146 / 146 | 27.290% / 27.290% | +0.000 |
| sonar-xml-plugin/src/main/java/org/sonar/plugins/xml/checks/security/android/AbstractAndroidManifestCheck.java | 42 / 42 | 17 / 17 | 25 / 25 | 59.524% / 59.524% | +0.000 |
| sonar-xml-plugin/src/main/java/org/sonar/plugins/xml/checks/security/android/AndroidApplicationBackupCheck.java | 123 / 123 | 58 / 58 | 65 / 65 | 52.846% / 52.846% | +0.000 |
| sonar-xml-plugin/src/main/java/org/sonar/plugins/xml/checks/security/android/AndroidClearTextCheck.java | 118 / 118 | 43 / 43 | 75 / 75 | 63.559% / 63.559% | +0.000 |
| sonar-xml-plugin/src/main/java/org/sonar/plugins/xml/checks/security/android/AndroidComponentWithIntentFilterExportedCheck.java | 83 / 83 | 19 / 19 | 64 / 64 | 77.108% / 77.108% | +0.000 |
| sonar-xml-plugin/src/main/java/org/sonar/plugins/xml/checks/security/android/AndroidCustomPermissionCheck.java | 81 / 81 | 27 / 27 | 54 / 54 | 66.667% / 66.667% | +0.000 |
| sonar-xml-plugin/src/main/java/org/sonar/plugins/xml/checks/security/android/AndroidExportedContentPermissionsCheck.java | 74 / 74 | 19 / 19 | 55 / 55 | 74.324% / 74.324% | +0.000 |
| sonar-xml-plugin/src/main/java/org/sonar/plugins/xml/checks/security/android/AndroidPermissionsCheck.java | 160 / 160 | 97 / 97 | 63 / 63 | 39.375% / 39.375% | +0.000 |
| sonar-xml-plugin/src/main/java/org/sonar/plugins/xml/checks/security/android/AndroidProviderPermissionCheck.java | 120 / 120 | 19 / 19 | 101 / 101 | 84.167% / 84.167% | +0.000 |
| sonar-xml-plugin/src/main/java/org/sonar/plugins/xml/checks/security/android/AndroidReceivingIntentsCheck.java | 74 / 74 | 19 / 19 | 55 / 55 | 74.324% / 74.324% | +0.000 |
| sonar-xml-plugin/src/main/java/org/sonar/plugins/xml/checks/security/android/DebugFeatureCheck.java | 109 / 109 | 27 / 27 | 82 / 82 | 75.229% / 75.229% | +0.000 |
| sonar-xml-plugin/src/main/java/org/sonar/plugins/xml/checks/security/android/Utils.java | 29 / 29 | 20 / 20 | 9 / 9 | 31.034% / 31.034% | +0.000 |
| sonar-xml-plugin/src/main/java/org/sonar/plugins/xml/checks/security/android/package-info.java | 10 / 10 | 9 / 9 | 1 / 1 | 10.000% / 10.000% | +0.000 |
| sonar-xml-plugin/src/main/java/org/sonar/plugins/xml/checks/security/package-info.java | 9 / 9 | 8 / 8 | 1 / 1 | 11.111% / 11.111% | +0.000 |
| sonar-xml-plugin/src/main/java/org/sonar/plugins/xml/checks/security/web/BaseWebCheck.java | 91 / 91 | 65 / 65 | 26 / 26 | 28.571% / 28.571% | +0.000 |
| sonar-xml-plugin/src/main/java/org/sonar/plugins/xml/checks/security/web/BasicAuthenticationCheck.java | 66 / 66 | 23 / 23 | 43 / 43 | 65.152% / 65.152% | +0.000 |
| sonar-xml-plugin/src/main/java/org/sonar/plugins/xml/checks/security/web/CrossOriginResourceSharingCheck.java | 65 / 65 | 20 / 20 | 45 / 45 | 69.231% / 69.231% | +0.000 |
| sonar-xml-plugin/src/main/java/org/sonar/plugins/xml/checks/security/web/HttpOnlyOnCookiesCheck.java | 162 / 162 | 78 / 78 | 84 / 84 | 51.852% / 51.852% | +0.000 |
| sonar-xml-plugin/src/main/java/org/sonar/plugins/xml/checks/security/web/MimeNosniffCheck.java | 87 / 87 | 29 / 29 | 58 / 58 | 66.667% / 66.667% | +0.000 |
| sonar-xml-plugin/src/main/java/org/sonar/plugins/xml/checks/security/web/PasswordsInWebConfigCheck.java | 79 / 79 | 34 / 34 | 45 / 45 | 56.962% / 56.962% | +0.000 |
| sonar-xml-plugin/src/main/java/org/sonar/plugins/xml/checks/security/web/ValidationFiltersCheck.java | 111 / 111 | 42 / 42 | 69 / 69 | 62.162% / 62.162% | +0.000 |
| sonar-xml-plugin/src/main/java/org/sonar/plugins/xml/checks/security/web/package-info.java | 10 / 10 | 9 / 9 | 1 / 1 | 10.000% / 10.000% | +0.000 |
| sonar-xml-plugin/src/main/java/org/sonar/plugins/xml/checks/spring/DefaultMessageListenerContainerCheck.java | 138 / 138 | 68 / 68 | 70 / 70 | 50.725% / 50.725% | +0.000 |
| sonar-xml-plugin/src/main/java/org/sonar/plugins/xml/checks/spring/SingleConnectionFactoryCheck.java | 120 / 120 | 50 / 50 | 70 / 70 | 58.333% / 58.333% | +0.000 |
| sonar-xml-plugin/src/main/java/org/sonar/plugins/xml/checks/spring/package-info.java | 10 / 10 | 6 / 6 | 4 / 4 | 40.000% / 40.000% | +0.000 |
| sonar-xml-plugin/src/main/java/org/sonar/plugins/xml/checks/struts/ActionNumberCheck.java | 115 / 115 | 42 / 42 | 73 / 73 | 63.478% / 63.478% | +0.000 |
| sonar-xml-plugin/src/main/java/org/sonar/plugins/xml/checks/struts/FormNameDuplicationCheck.java | 143 / 143 | 53 / 53 | 90 / 90 | 62.937% / 62.937% | +0.000 |
| sonar-xml-plugin/src/main/java/org/sonar/plugins/xml/checks/struts/package-info.java | 9 / 9 | 8 / 8 | 1 / 1 | 11.111% / 11.111% | +0.000 |
| sonar-xml-plugin/src/main/java/org/sonar/plugins/xml/package-info.java | 7 / 7 | 6 / 6 | 1 / 1 | 14.286% / 14.286% | +0.000 |

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
- Identifier counts come from the semantic report: known = total − unknown. They count identifier occurrences, not distinct fields or properties.
- Unknown percentage = unknown / total × 100. The project percentage uses aggregate counts, not an average of file percentages. No identifiers means N/A.
- Per-file counts are current / candidate. A negative change in unknown percentage means fewer unresolved identifiers; it does not prove semantic correctness.
- Files with no unknown identifiers must contain at least one identifier; their percentage uses all analyzed files. Files with zero identifiers are excluded from improved/unchanged/regressed counts.
- Configured rules may be disabled when dependencies are absent; zero findings do not prove a rule ran.
- Wall times include Maven startup, scanning, and server processing. Analyzer timings are reported separately when available; test-server startup is excluded. Repeated samples exclude warm-up runs.
