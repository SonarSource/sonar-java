# Source-only Java analysis comparison

**Runner:** Orchestrator MavenBuild (`sonar:sonar`)

**Comparison:** VALID · **Candidate:** configured

## Summary

| Metric | Current | Candidate |
|---|---:|---:|
| Scan status | SUCCESS | SUCCESS |
| Java files analyzed | 69 | 69 |
| Identifiers (total) | 5608 | 5608 |
| Known identifiers | 4171 | 4171 |
| Unknown identifiers | 1437 | 1437 |
| Unknown identifiers (%) | 25.624% | 25.624% |
| Files with no unknown identifiers | 3 / 69 (4.348%) | 3 / 69 (4.348%) |
| Findings | 1 | 1 |
| Median Maven/server time (ms) | 14282.0 | 14323.0 |
| Median JavaSensor time (ms) | 4589.0 | 4714.0 |
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

## Unknown identifier occurrences

| Metric | Count |
|---|---:|
| Still unknown in both runs | 1437 |
| No longer reported unknown | 0 |
| Newly reported unknown | 0 |

### No longer reported unknown

None.

### Newly reported unknown

None.

### Unknown occurrences by AST context

| Context | Current | Candidate |
|---|---:|---:|
| ANNOTATION | 83 | 83 |
| ARGUMENTS | 75 | 75 |
| ASSIGNMENT | 110 | 110 |
| CLASS | 27 | 27 |
| CONDITIONAL_AND | 1 | 1 |
| EQUAL_TO | 1 | 1 |
| INSTANCE_OF | 1 | 1 |
| LIST | 4 | 4 |
| MEMBER_SELECT | 705 | 705 |
| METHOD_INVOCATION | 156 | 156 |
| METHOD_REFERENCE | 8 | 8 |
| NEW_CLASS | 14 | 14 |
| PARAMETERIZED_TYPE | 14 | 14 |
| PLUS | 2 | 2 |
| TYPE_ARGUMENTS | 12 | 12 |
| TYPE_CAST | 12 | 12 |
| VARIABLE | 212 | 212 |

## Run metadata

| Setting | Value |
|---|---|
| Analyzer build manifest revision | cfc05f237ae1feecc218e6c935913943847a144b |
| Analyzer checkout dirty | false |
| Analyzer checkout revision | e79839da813b65381943b68c7400b3f7decf9e4b |
| Analyzer plugin SHA-256 | cdc872df59c8a9d387e54f5721e3f558e594b0d5fa33ef4c7f1afa82c5a09369 |
| Analyzer plugin path | /private/tmp/sonar-java-sourcepath-evaluation/sonar-java-plugin/target/sonar-java-plugin-8.45.0-SNAPSHOT.jar |
| Analyzer plugin version | 8.45.0-SNAPSHOT |
| Binding correctness/candidate | Semantic binding probes: 5 checked, 0 failed. |
| Binding correctness/current | Semantic binding probes: 5 checked, 0 failed. |
| Binding probe plugin SHA-256 | c3bb68cdb4e20105e12e78ece32ec8e941b3113e3271f29ce04cad497ac56a82 |
| Candidate feature | PR #6308 source-path resolution enabled; same plugin used for both runs |
| Candidate properties | {sonar.java.sourcepath=sonar-xml-plugin/src/main/java} |
| Candidate source paths | sonar-xml-plugin/src/main/java |
| Current source paths | none |
| JDK | 21.0.10 / Microsoft |
| Java language level | 21 |
| Maven scanner version | 5.9.0.7291 |
| Measured repetitions per mode | 1 |
| Profile | Sonar way |
| Recorded at (UTC) | 2026-10-07T09:17:25.579510Z |
| Repeated result stability | stable |
| Server version | 26.10.0.132816 |
| Server workspace | /Users/matthew.elliott/Work/Code/sonar-java/its/plugin/tests/target/comparison-orchestrator-13921017383943873975 |
| Source-path fixture/analyzed files | consumer/src/main/java/bindings/BindingFixture.java only; dependency sources excluded |
| Source-path fixture/candidate (project bindings expected known) | Semantic binding probes: 5 checked, 0 failed. |
| Source-path fixture/current (project bindings expected unknown) | Semantic binding probes: 5 checked, 0 failed. |
| Target checkout dirty | false |
| Target checkout revision | 627ac4747bef73345338aae435b7c987873cf86f |
| Target source snapshot SHA-256 | 9ac9e9d9cc60dfc68a4e174741e5db76abcee26dbc7ecd41a72af5bd38303586 |
| Test harness dirty | true |
| Test harness revision | cfc05f237ae1feecc218e6c935913943847a144b |
| Timing protocol | single measured pair; no warm-up |

## Measured timings

Wall time includes Maven and server processing; analyzer time measures JavaSensor execution (parsing, semantics, checks, and metrics). Warm-ups are excluded. Missing analyzer timings are N/A.

| Sample | Current wall (ms) | Candidate wall (ms) | Current analyzer (ms) | Candidate analyzer (ms) |
|---|---:|---:|---:|---:|
| 1 | 14282 | 14323 | 4589.0 | 4714.0 |
| Median | 14282.0 | 14323.0 | 4589.0 | 4714.0 |

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
| sonar-xml-plugin/src/main/java/org/sonar/plugins/xml/XmlSensor.java | 139 / 139 | 9.673% / 9.673% |
| sonar-xml-plugin/src/main/java/org/sonar/plugins/xml/XmlHighlighting.java | 93 / 93 | 6.472% / 6.472% |
| sonar-xml-plugin/src/main/java/org/sonar/plugins/xml/checks/maven/DisallowedDependenciesCheck.java | 70 / 70 | 4.871% / 4.871% |
| sonar-xml-plugin/src/main/java/org/sonar/plugins/xml/LineCounter.java | 62 / 62 | 4.315% / 4.315% |
| sonar-xml-plugin/src/main/java/org/sonar/plugins/xml/checks/security/HardcodedCredentialsCheck.java | 57 / 57 | 3.967% / 3.967% |

## Semantics per file

Counts and percentages are **current / candidate**. Change is candidate minus current.

| File | Total identifiers | Known identifiers | Unknown identifiers | Unknown % | Change (pp) |
|---|---:|---:|---:|---:|---:|
| sonar-xml-plugin/src/main/java/org/sonar/plugins/xml/LineCounter.java | 213 / 213 | 151 / 151 | 62 / 62 | 29.108% / 29.108% | +0.000 |
| sonar-xml-plugin/src/main/java/org/sonar/plugins/xml/Utils.java | 33 / 33 | 23 / 23 | 10 / 10 | 30.303% / 30.303% | +0.000 |
| sonar-xml-plugin/src/main/java/org/sonar/plugins/xml/Xml.java | 82 / 82 | 72 / 72 | 10 / 10 | 12.195% / 12.195% | +0.000 |
| sonar-xml-plugin/src/main/java/org/sonar/plugins/xml/XmlHighlighting.java | 252 / 252 | 159 / 159 | 93 / 93 | 36.905% / 36.905% | +0.000 |
| sonar-xml-plugin/src/main/java/org/sonar/plugins/xml/XmlPlugin.java | 31 / 31 | 17 / 17 | 14 / 14 | 45.161% / 45.161% | +0.000 |
| sonar-xml-plugin/src/main/java/org/sonar/plugins/xml/XmlRulesDefinition.java | 50 / 50 | 35 / 35 | 15 / 15 | 30.000% / 30.000% | +0.000 |
| sonar-xml-plugin/src/main/java/org/sonar/plugins/xml/XmlSensor.java | 364 / 364 | 225 / 225 | 139 / 139 | 38.187% / 38.187% | +0.000 |
| sonar-xml-plugin/src/main/java/org/sonar/plugins/xml/XmlSonarWayProfile.java | 53 / 53 | 35 / 35 | 18 / 18 | 33.962% / 33.962% | +0.000 |
| sonar-xml-plugin/src/main/java/org/sonar/plugins/xml/api/XmlProfileRegistrar.java | 15 / 15 | 12 / 12 | 3 / 3 | 20.000% / 20.000% | +0.000 |
| sonar-xml-plugin/src/main/java/org/sonar/plugins/xml/api/package-info.java | 8 / 8 | 7 / 7 | 1 / 1 | 12.500% / 12.500% | +0.000 |
| sonar-xml-plugin/src/main/java/org/sonar/plugins/xml/checks/CharBeforePrologCheck.java | 33 / 33 | 14 / 14 | 19 / 19 | 57.576% / 57.576% | +0.000 |
| sonar-xml-plugin/src/main/java/org/sonar/plugins/xml/checks/CheckList.java | 51 / 51 | 51 / 51 | 0 / 0 | 0.000% / 0.000% | +0.000 |
| sonar-xml-plugin/src/main/java/org/sonar/plugins/xml/checks/CommentContainsPatternChecker.java | 90 / 90 | 66 / 66 | 24 / 24 | 26.667% / 26.667% | +0.000 |
| sonar-xml-plugin/src/main/java/org/sonar/plugins/xml/checks/CommentedOutCodeCheck.java | 209 / 209 | 177 / 177 | 32 / 32 | 15.311% / 15.311% | +0.000 |
| sonar-xml-plugin/src/main/java/org/sonar/plugins/xml/checks/FixmeCommentCheck.java | 21 / 21 | 19 / 19 | 2 / 2 | 9.524% / 9.524% | +0.000 |
| sonar-xml-plugin/src/main/java/org/sonar/plugins/xml/checks/IndentationCheck.java | 282 / 282 | 251 / 251 | 31 / 31 | 10.993% / 10.993% | +0.000 |
| sonar-xml-plugin/src/main/java/org/sonar/plugins/xml/checks/LineLengthCheck.java | 75 / 75 | 62 / 62 | 13 / 13 | 17.333% / 17.333% | +0.000 |
| sonar-xml-plugin/src/main/java/org/sonar/plugins/xml/checks/NewlineCheck.java | 217 / 217 | 176 / 176 | 41 / 41 | 18.894% / 18.894% | +0.000 |
| sonar-xml-plugin/src/main/java/org/sonar/plugins/xml/checks/ParsingErrorCheck.java | 17 / 17 | 13 / 13 | 4 / 4 | 23.529% / 23.529% | +0.000 |
| sonar-xml-plugin/src/main/java/org/sonar/plugins/xml/checks/TabCharacterCheck.java | 115 / 115 | 90 / 90 | 25 / 25 | 21.739% / 21.739% | +0.000 |
| sonar-xml-plugin/src/main/java/org/sonar/plugins/xml/checks/TodoCommentCheck.java | 21 / 21 | 19 / 19 | 2 / 2 | 9.524% / 9.524% | +0.000 |
| sonar-xml-plugin/src/main/java/org/sonar/plugins/xml/checks/XPathCheck.java | 285 / 285 | 246 / 246 | 39 / 39 | 13.684% / 13.684% | +0.000 |
| sonar-xml-plugin/src/main/java/org/sonar/plugins/xml/checks/ejb/DefaultInterceptorsLocationCheck.java | 32 / 32 | 16 / 16 | 16 / 16 | 50.000% / 50.000% | +0.000 |
| sonar-xml-plugin/src/main/java/org/sonar/plugins/xml/checks/ejb/InterceptorExclusionsCheck.java | 40 / 40 | 23 / 23 | 17 / 17 | 42.500% / 42.500% | +0.000 |
| sonar-xml-plugin/src/main/java/org/sonar/plugins/xml/checks/ejb/package-info.java | 9 / 9 | 8 / 8 | 1 / 1 | 11.111% / 11.111% | +0.000 |
| sonar-xml-plugin/src/main/java/org/sonar/plugins/xml/checks/hibernate/DatabaseSchemaUpdateCheck.java | 48 / 48 | 36 / 36 | 12 / 12 | 25.000% / 25.000% | +0.000 |
| sonar-xml-plugin/src/main/java/org/sonar/plugins/xml/checks/hibernate/package-info.java | 9 / 9 | 8 / 8 | 1 / 1 | 11.111% / 11.111% | +0.000 |
| sonar-xml-plugin/src/main/java/org/sonar/plugins/xml/checks/maven/ArtifactIdNamingConventionCheck.java | 79 / 79 | 62 / 62 | 17 / 17 | 21.519% / 21.519% | +0.000 |
| sonar-xml-plugin/src/main/java/org/sonar/plugins/xml/checks/maven/DependencyWithSystemScopeCheck.java | 87 / 87 | 65 / 65 | 22 / 22 | 25.287% / 25.287% | +0.000 |
| sonar-xml-plugin/src/main/java/org/sonar/plugins/xml/checks/maven/DeprecatedPomPropertiesCheck.java | 111 / 111 | 68 / 68 | 43 / 43 | 38.739% / 38.739% | +0.000 |
| sonar-xml-plugin/src/main/java/org/sonar/plugins/xml/checks/maven/DisallowedDependenciesCheck.java | 178 / 178 | 108 / 108 | 70 / 70 | 39.326% / 39.326% | +0.000 |
| sonar-xml-plugin/src/main/java/org/sonar/plugins/xml/checks/maven/GroupIdNamingConventionCheck.java | 79 / 79 | 62 / 62 | 17 / 17 | 21.519% / 21.519% | +0.000 |
| sonar-xml-plugin/src/main/java/org/sonar/plugins/xml/checks/maven/PomElementOrderCheck.java | 146 / 146 | 122 / 122 | 24 / 24 | 16.438% / 16.438% | +0.000 |
| sonar-xml-plugin/src/main/java/org/sonar/plugins/xml/checks/maven/helpers/MavenDependencyMatcher.java | 100 / 100 | 100 / 100 | 0 / 0 | 0.000% / 0.000% | +0.000 |
| sonar-xml-plugin/src/main/java/org/sonar/plugins/xml/checks/maven/helpers/PatternMatcher.java | 43 / 43 | 43 / 43 | 0 / 0 | 0.000% / 0.000% | +0.000 |
| sonar-xml-plugin/src/main/java/org/sonar/plugins/xml/checks/maven/helpers/RangedVersionMatcher.java | 168 / 168 | 164 / 164 | 4 / 4 | 2.381% / 2.381% | +0.000 |
| sonar-xml-plugin/src/main/java/org/sonar/plugins/xml/checks/maven/helpers/StringMatcher.java | 39 / 39 | 38 / 38 | 1 / 1 | 2.564% / 2.564% | +0.000 |
| sonar-xml-plugin/src/main/java/org/sonar/plugins/xml/checks/maven/helpers/package-info.java | 10 / 10 | 9 / 9 | 1 / 1 | 10.000% / 10.000% | +0.000 |
| sonar-xml-plugin/src/main/java/org/sonar/plugins/xml/checks/maven/package-info.java | 9 / 9 | 8 / 8 | 1 / 1 | 11.111% / 11.111% | +0.000 |
| sonar-xml-plugin/src/main/java/org/sonar/plugins/xml/checks/package-info.java | 8 / 8 | 7 / 7 | 1 / 1 | 12.500% / 12.500% | +0.000 |
| sonar-xml-plugin/src/main/java/org/sonar/plugins/xml/checks/security/HardcodedCredentialsCheck.java | 446 / 446 | 389 / 389 | 57 / 57 | 12.780% / 12.780% | +0.000 |
| sonar-xml-plugin/src/main/java/org/sonar/plugins/xml/checks/security/android/AbstractAndroidManifestCheck.java | 20 / 20 | 17 / 17 | 3 / 3 | 15.000% / 15.000% | +0.000 |
| sonar-xml-plugin/src/main/java/org/sonar/plugins/xml/checks/security/android/AndroidApplicationBackupCheck.java | 80 / 80 | 58 / 58 | 22 / 22 | 27.500% / 27.500% | +0.000 |
| sonar-xml-plugin/src/main/java/org/sonar/plugins/xml/checks/security/android/AndroidClearTextCheck.java | 74 / 74 | 43 / 43 | 31 / 31 | 41.892% / 41.892% | +0.000 |
| sonar-xml-plugin/src/main/java/org/sonar/plugins/xml/checks/security/android/AndroidComponentWithIntentFilterExportedCheck.java | 38 / 38 | 19 / 19 | 19 / 19 | 50.000% / 50.000% | +0.000 |
| sonar-xml-plugin/src/main/java/org/sonar/plugins/xml/checks/security/android/AndroidCustomPermissionCheck.java | 52 / 52 | 27 / 27 | 25 / 25 | 48.077% / 48.077% | +0.000 |
| sonar-xml-plugin/src/main/java/org/sonar/plugins/xml/checks/security/android/AndroidExportedContentPermissionsCheck.java | 38 / 38 | 19 / 19 | 19 / 19 | 50.000% / 50.000% | +0.000 |
| sonar-xml-plugin/src/main/java/org/sonar/plugins/xml/checks/security/android/AndroidPermissionsCheck.java | 106 / 106 | 97 / 97 | 9 / 9 | 8.491% / 8.491% | +0.000 |
| sonar-xml-plugin/src/main/java/org/sonar/plugins/xml/checks/security/android/AndroidProviderPermissionCheck.java | 66 / 66 | 19 / 19 | 47 / 47 | 71.212% / 71.212% | +0.000 |
| sonar-xml-plugin/src/main/java/org/sonar/plugins/xml/checks/security/android/AndroidReceivingIntentsCheck.java | 38 / 38 | 19 / 19 | 19 / 19 | 50.000% / 50.000% | +0.000 |
| sonar-xml-plugin/src/main/java/org/sonar/plugins/xml/checks/security/android/DebugFeatureCheck.java | 59 / 59 | 27 / 27 | 32 / 32 | 54.237% / 54.237% | +0.000 |
| sonar-xml-plugin/src/main/java/org/sonar/plugins/xml/checks/security/android/Utils.java | 23 / 23 | 20 / 20 | 3 / 3 | 13.043% / 13.043% | +0.000 |
| sonar-xml-plugin/src/main/java/org/sonar/plugins/xml/checks/security/android/package-info.java | 10 / 10 | 9 / 9 | 1 / 1 | 10.000% / 10.000% | +0.000 |
| sonar-xml-plugin/src/main/java/org/sonar/plugins/xml/checks/security/package-info.java | 9 / 9 | 8 / 8 | 1 / 1 | 11.111% / 11.111% | +0.000 |
| sonar-xml-plugin/src/main/java/org/sonar/plugins/xml/checks/security/web/BaseWebCheck.java | 73 / 73 | 65 / 65 | 8 / 8 | 10.959% / 10.959% | +0.000 |
| sonar-xml-plugin/src/main/java/org/sonar/plugins/xml/checks/security/web/BasicAuthenticationCheck.java | 42 / 42 | 23 / 23 | 19 / 19 | 45.238% / 45.238% | +0.000 |
| sonar-xml-plugin/src/main/java/org/sonar/plugins/xml/checks/security/web/CrossOriginResourceSharingCheck.java | 41 / 41 | 20 / 20 | 21 / 21 | 51.220% / 51.220% | +0.000 |
| sonar-xml-plugin/src/main/java/org/sonar/plugins/xml/checks/security/web/HttpOnlyOnCookiesCheck.java | 120 / 120 | 78 / 78 | 42 / 42 | 35.000% / 35.000% | +0.000 |
| sonar-xml-plugin/src/main/java/org/sonar/plugins/xml/checks/security/web/MimeNosniffCheck.java | 52 / 52 | 29 / 29 | 23 / 23 | 44.231% / 44.231% | +0.000 |
| sonar-xml-plugin/src/main/java/org/sonar/plugins/xml/checks/security/web/PasswordsInWebConfigCheck.java | 48 / 48 | 34 / 34 | 14 / 14 | 29.167% / 29.167% | +0.000 |
| sonar-xml-plugin/src/main/java/org/sonar/plugins/xml/checks/security/web/ValidationFiltersCheck.java | 78 / 78 | 42 / 42 | 36 / 36 | 46.154% / 46.154% | +0.000 |
| sonar-xml-plugin/src/main/java/org/sonar/plugins/xml/checks/security/web/package-info.java | 10 / 10 | 9 / 9 | 1 / 1 | 10.000% / 10.000% | +0.000 |
| sonar-xml-plugin/src/main/java/org/sonar/plugins/xml/checks/spring/DefaultMessageListenerContainerCheck.java | 99 / 99 | 68 / 68 | 31 / 31 | 31.313% / 31.313% | +0.000 |
| sonar-xml-plugin/src/main/java/org/sonar/plugins/xml/checks/spring/SingleConnectionFactoryCheck.java | 81 / 81 | 50 / 50 | 31 / 31 | 38.272% / 38.272% | +0.000 |
| sonar-xml-plugin/src/main/java/org/sonar/plugins/xml/checks/spring/package-info.java | 7 / 7 | 6 / 6 | 1 / 1 | 14.286% / 14.286% | +0.000 |
| sonar-xml-plugin/src/main/java/org/sonar/plugins/xml/checks/struts/ActionNumberCheck.java | 73 / 73 | 42 / 42 | 31 / 31 | 42.466% / 42.466% | +0.000 |
| sonar-xml-plugin/src/main/java/org/sonar/plugins/xml/checks/struts/FormNameDuplicationCheck.java | 97 / 97 | 53 / 53 | 44 / 44 | 45.361% / 45.361% | +0.000 |
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
- Unknown occurrences match by file, name, and token range. Removed means no longer reported unknown; correctness is checked separately. AST context is diagnostic, not a method/type/field classification.
- Unknown percentage = unknown / total × 100. The project percentage uses aggregate counts, not an average of file percentages. No identifiers means N/A.
- Per-file counts are current / candidate. A negative change in unknown percentage means fewer unresolved identifiers; it does not prove semantic correctness.
- Files with no unknown identifiers must contain at least one identifier; their percentage uses all analyzed files. Files with zero identifiers are excluded from improved/unchanged/regressed counts.
- Configured rules may be disabled when dependencies are absent; zero findings do not prove a rule ran.
- Wall times include Maven startup, scanning, and server processing. Analyzer timings are reported separately when available; test-server startup is excluded. Repeated samples exclude warm-up runs.
