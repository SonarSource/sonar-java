# Java source-path and internal-compilation comparison

One analyzer artifact, four modes: **Baseline**, **Source paths (#6308)**, **Bytecode (#6309)**, and **Combined**. Maven runs only `sonar:sonar`; no project bytecode is supplied.

## Results at a glance

Cells show **unknown identifiers (unknown %; change from the same dataset's baseline)**. A negative change means fewer unknown identifiers. Invalid or unstable comparisons have no change metric.

| Dataset / parser batching / external JARs | Baseline | Source paths | Bytecode | Combined |
|---|---:|---:|---:|---:|
| sonar-java / file-by-file / no-libraries | 93023 (42.288%; Δ +0) | 10462 (4.756%; Δ -82561) | 88332 (40.155%; Δ -4691) | 10462 (4.756%; Δ -82561) |
| sonar-java / file-by-file / dependencies | 83302 (37.869%; Δ +0) | 48 (0.022%; Δ -83254) | 69069 (31.398%; Δ -14233) | 48 (0.022%; Δ -83254) |

Normal batches are the ordinary-analysis control. File-by-file analysis is a sensitivity experiment that removes cross-file parser-batch resolution. Library-enabled datasets supply identical external compile-scope JARs to every mode. Feature effects are comparisons **within a row**; adding libraries changes the input and is a separate factor.

## Reviewed finding evidence

The clean fixture has one genuinely deprecated call, two nondeprecated control calls, and one empty statement. These are reviewed expectations, not inferred accuracy from analyzer agreement. Cells show actual findings and whether they match the reviewed code.

| Reviewed location / expected findings | Baseline | Source paths | Bytecode | Combined |
|---|---:|---:|---:|---:|
| S1874: deprecated call, line 8 / 1 | 0 (missed) | 1 (correct) | 1 (correct) | 1 (correct) |
| S1874: nondeprecated controls, lines 9–10 / 0 | 0 (correct) | 0 (correct) | 0 (correct) | 0 (correct) |
| S1116: empty statement, line 11 / 1 | 1 (correct) | 1 (correct) | 1 (correct) | 1 (correct) |
| S1874 reviewed fixture: TP / FP / FN | 0 / 0 / 1 | 1 / 0 / 0 | 1 / 0 / 0 | 1 / 0 / 0 |
| S1116 reviewed fixture: TP / FP / FN | 1 / 0 / 0 | 1 / 0 / 0 | 1 / 0 / 0 | 1 / 0 / 0 |
| Product findings in fixture | 1 | 2 | 2 | 2 |

A missed deprecated call is a demonstrated limitation of that mode in this fixture. Exact overload and inherited-field binding checks are reported separately below.

## Focused scenario results

Unknown identifiers; exact binding probes establish whether selected resolutions are correct.

| Scenario | Baseline | Source paths | Bytecode | Combined |
|---|---:|---:|---:|---:|
| Clean compilation | 8 | 0 | 0 | 0 |
| Encoding compatibility | 9 | 0 | 0 | 0 |
| Dependency outside analysis | 5 | 0 | 5 | 0 |
| Compilation failure recovery | 1 | 1 | 1 | 1 |

## Benefit and cost by configuration

Changes are relative to the same dataset's baseline. Timings summarize measured scans only; the min/median/max shows observed dispersion. Recursive graph diagnostics are DISABLED (recorded expansion limit 0 in every measured mode), so these scans perform no recursive graph observation. Full AST identifier/per-file observation and evaluation measurement hooks are included in these timings, so they are not raw production performance.

| Dataset | Mode | Scan | Comparison | Unknown Δ | Product findings | JavaSensor min / median / max (ms) | JavaSensor median Δ (ms) | Compilation | Compilation median (ms) | Generated classes | Cleanup |
|---|---|---|---|---:|---:|---:|---:|---|---:|---:|---|
| sonar-java / file-by-file / no-libraries | Baseline | SUCCESS | VALID | +0 | 89 | 73111.0 / 73111.0 / 73111.0 | +0.0 | DISABLED | 0.0 | 0 | true |
| sonar-java / file-by-file / no-libraries | Source paths | SUCCESS | VALID | -82561 | 107 | 205191.0 / 205191.0 / 205191.0 | +132080.0 | DISABLED | 0.0 | 0 | true |
| sonar-java / file-by-file / no-libraries | Bytecode | SUCCESS | VALID | -4691 | 89 | 79763.0 / 79763.0 / 79763.0 | +6652.0 | FAILED | 1452.0 | 192 | true |
| sonar-java / file-by-file / no-libraries | Combined | SUCCESS | VALID | -82561 | 107 | 212191.0 / 212191.0 / 212191.0 | +139080.0 | FAILED | 1379.0 | 192 | true |
| sonar-java / file-by-file / dependencies | Baseline | SUCCESS | VALID | +0 | 99 | 80280.0 / 80280.0 / 80280.0 | +0.0 | DISABLED | 0.0 | 0 | true |
| sonar-java / file-by-file / dependencies | Source paths | SUCCESS | VALID | -83254 | 117 | 213817.0 / 213817.0 / 213817.0 | +133537.0 | DISABLED | 0.0 | 0 | true |
| sonar-java / file-by-file / dependencies | Bytecode | SUCCESS | VALID | -14233 | 104 | 86662.0 / 86662.0 / 86662.0 | +6382.0 | FAILED | 1734.0 | 574 | true |
| sonar-java / file-by-file / dependencies | Combined | SUCCESS | VALID | -83254 | 117 | 222564.0 / 222564.0 / 222564.0 | +142284.0 | FAILED | 1558.0 | 574 | true |

### JavaSensor phases

The remaining analysis phase is each sample's JavaSensor time minus compilation time. It includes parsing, rule execution, and enabled semantic observations. Invalid or missing timers give N/A; phase medians need not sum to the overall median.

| Dataset | Mode | Compilation min / median / max (ms) | Analysis excluding compilation min / median / max (ms) |
|---|---|---:|---:|
| sonar-java / file-by-file / no-libraries | Baseline | 0.0 / 0.0 / 0.0 | 73111.0 / 73111.0 / 73111.0 |
| sonar-java / file-by-file / no-libraries | Source paths | 0.0 / 0.0 / 0.0 | 205191.0 / 205191.0 / 205191.0 |
| sonar-java / file-by-file / no-libraries | Bytecode | 1452.0 / 1452.0 / 1452.0 | 78311.0 / 78311.0 / 78311.0 |
| sonar-java / file-by-file / no-libraries | Combined | 1379.0 / 1379.0 / 1379.0 | 210812.0 / 210812.0 / 210812.0 |
| sonar-java / file-by-file / dependencies | Baseline | 0.0 / 0.0 / 0.0 | 80280.0 / 80280.0 / 80280.0 |
| sonar-java / file-by-file / dependencies | Source paths | 0.0 / 0.0 / 0.0 | 213817.0 / 213817.0 / 213817.0 |
| sonar-java / file-by-file / dependencies | Bytecode | 1734.0 / 1734.0 / 1734.0 | 84928.0 / 84928.0 / 84928.0 |
| sonar-java / file-by-file / dependencies | Combined | 1558.0 / 1558.0 / 1558.0 | 221006.0 / 221006.0 / 221006.0 |

## Observed tradeoffs

- **sonar-java / file-by-file / no-libraries / Source paths**: 82561 fewer unknown identifiers; JavaSensor median +132080.0 ms versus baseline.
- **sonar-java / file-by-file / no-libraries / Bytecode**: 4691 fewer unknown identifiers; JavaSensor median +6652.0 ms versus baseline; compilation FAILED.
- **sonar-java / file-by-file / no-libraries / Combined**: 82561 fewer unknown identifiers; JavaSensor median +139080.0 ms versus baseline; compilation FAILED.
- **sonar-java / file-by-file / dependencies / Source paths**: 83254 fewer unknown identifiers; JavaSensor median +133537.0 ms versus baseline.
- **sonar-java / file-by-file / dependencies / Bytecode**: 14233 fewer unknown identifiers; JavaSensor median +6382.0 ms versus baseline; compilation FAILED.
- **sonar-java / file-by-file / dependencies / Combined**: 83254 fewer unknown identifiers; JavaSensor median +142284.0 ms versus baseline; compilation FAILED.

These observations apply to the recorded source snapshot and environment. No universal winner or statistical significance is inferred.

## Semantic graph diagnostics

Unique symbol/type references reached during bounded traversal, which can expand as names resolve. Cells show the observed count or min–max range across measured repetitions. These are counts, not resolution-coverage percentages.

Recursive graph inspection is disabled by default for feature timing. Optional budgeted diagnostics can still be expensive. The initial unrestricted observer stalled for over seven minutes on a two-file smoke scan; that aborted scan is excluded from results. Identifier counts, per-file coverage, and product findings still cover the full analyzed AST.

**DISABLED** means a zero expansion limit; graph counts are N/A rather than zero. **COMPLETE** means all measured traversals finished. **PARTIAL** means at least one hit the budget. **UNKNOWN** means completeness was not recorded. Location-change interpretations require complete, stable graph observations. Dataset matrices show representative graph counts.

| Dataset | Mode | Traversal | Expansion limit / module | Expansions executed | Resolved symbols | Unknown symbols | Resolved types | Unknown types |
|---|---|---|---:|---:|---:|---:|---:|---:|
| sonar-java / file-by-file / no-libraries | Baseline | DISABLED | 0 | 0 | N/A | N/A | N/A | N/A |
| sonar-java / file-by-file / no-libraries | Source paths | DISABLED | 0 | 0 | N/A | N/A | N/A | N/A |
| sonar-java / file-by-file / no-libraries | Bytecode | DISABLED | 0 | 0 | N/A | N/A | N/A | N/A |
| sonar-java / file-by-file / no-libraries | Combined | DISABLED | 0 | 0 | N/A | N/A | N/A | N/A |
| sonar-java / file-by-file / dependencies | Baseline | DISABLED | 0 | 0 | N/A | N/A | N/A | N/A |
| sonar-java / file-by-file / dependencies | Source paths | DISABLED | 0 | 0 | N/A | N/A | N/A | N/A |
| sonar-java / file-by-file / dependencies | Bytecode | DISABLED | 0 | 0 | N/A | N/A | N/A | N/A |
| sonar-java / file-by-file / dependencies | Combined | DISABLED | 0 | 0 | N/A | N/A | N/A | N/A |

<details>
<summary>Focused fixture measurements and exact binding evidence</summary>


## Focused correctness scenarios

### Clean compilation

Three JDK-only files analyzed separately; exact overload and inherited-field bindings; one deprecated call, two nondeprecated controls, and one syntax finding; generated bytecode checked during analysis.

| Metric | Baseline | Source paths | Bytecode | Combined |
|---|---:|---:|---:|---:|
| Scan status | SUCCESS | SUCCESS | SUCCESS | SUCCESS |
| Files analyzed | 3 | 3 | 3 | 3 |
| Identifiers (total) | 56 | 56 | 56 | 56 |
| Known identifiers | 48 | 56 | 56 | 56 |
| Unknown identifiers | 8 | 0 | 0 | 0 |
| Compilation outcome | DISABLED | DISABLED | SUCCESS | SUCCESS |
| Generated class files before cleanup | 0 | 0 | 3 | 3 |
| Temporary bytecode cleaned up | YES | YES | YES | YES |
| Exact binding probe completion | Semantic binding probes: 4 checked, 0 failed. | Semantic binding probes: 4 checked, 0 failed. | Semantic binding probes: 4 checked, 0 failed. | Semantic binding probes: 4 checked, 0 failed. |
| Exact binding probes checked | 4 | 4 | 4 | 4 |
| Exact binding probe failures | 0 | 0 | 0 | 0 |
| Bytecode during analysis | Generated bytecode probe: absent. | Generated bytecode probe: absent. | Generated bytecode probe: present. | Generated bytecode probe: present. |
| Coverage versus baseline | VALID | VALID | VALID | VALID |
### Encoding compatibility

Latin-1 Java files contain a non-ASCII method identifier; all modes use ISO-8859-1 and bytecode must compile correctly.

| Metric | Baseline | Source paths | Bytecode | Combined |
|---|---:|---:|---:|---:|
| Scan status | SUCCESS | SUCCESS | SUCCESS | SUCCESS |
| Files analyzed | 3 | 3 | 3 | 3 |
| Identifiers (total) | 66 | 66 | 66 | 66 |
| Known identifiers | 57 | 66 | 66 | 66 |
| Unknown identifiers | 9 | 0 | 0 | 0 |
| Compilation outcome | DISABLED | DISABLED | SUCCESS | SUCCESS |
| Generated class files before cleanup | 0 | 0 | 3 | 3 |
| Temporary bytecode cleaned up | YES | YES | YES | YES |
| Exact binding probe completion | Semantic binding probes: 4 checked, 0 failed. | Semantic binding probes: 4 checked, 0 failed. | Semantic binding probes: 4 checked, 0 failed. | Semantic binding probes: 4 checked, 0 failed. |
| Exact binding probes checked | 4 | 4 | 4 | 4 |
| Exact binding probe failures | 0 | 0 | 0 | 0 |
| Bytecode during analysis | Generated bytecode probe: absent. | Generated bytecode probe: absent. | Generated bytecode probe: present. | Generated bytecode probe: present. |
| Coverage versus baseline | VALID | VALID | VALID | VALID |
### Dependency outside analysis

Only the consumer is indexed; valid dependency sources resolve project bindings without analyzing dependency files; internal compilation must cope with dependencies outside its indexed input.

| Metric | Baseline | Source paths | Bytecode | Combined |
|---|---:|---:|---:|---:|
| Scan status | SUCCESS | SUCCESS | SUCCESS | SUCCESS |
| Files analyzed | 1 | 1 | 1 | 1 |
| Identifiers (total) | 16 | 16 | 16 | 16 |
| Known identifiers | 11 | 16 | 11 | 16 |
| Unknown identifiers | 5 | 0 | 5 | 0 |
| Compilation outcome | DISABLED | DISABLED | FAILED | FAILED |
| Generated class files before cleanup | 0 | 0 | 0 | 0 |
| Temporary bytecode cleaned up | YES | YES | YES | YES |
| Exact binding probe completion | Semantic binding probes: 4 checked, 0 failed. | Semantic binding probes: 4 checked, 0 failed. | Semantic binding probes: 4 checked, 0 failed. | Semantic binding probes: 4 checked, 0 failed. |
| Exact binding probes checked | 4 | 4 | 4 | 4 |
| Exact binding probe failures | 0 | 0 | 0 | 0 |
| Bytecode during analysis | N/A | N/A | N/A | N/A |
| Coverage versus baseline | VALID | VALID | VALID | VALID |
### Compilation failure recovery

A missing type causes compiler failure; batch analysis still resolves project bindings and visits both files.

| Metric | Baseline | Source paths | Bytecode | Combined |
|---|---:|---:|---:|---:|
| Scan status | SUCCESS | SUCCESS | SUCCESS | SUCCESS |
| Files analyzed | 2 | 2 | 2 | 2 |
| Identifiers (total) | 30 | 30 | 30 | 30 |
| Known identifiers | 29 | 29 | 29 | 29 |
| Unknown identifiers | 1 | 1 | 1 | 1 |
| Compilation outcome | DISABLED | DISABLED | FAILED | FAILED |
| Generated class files before cleanup | 0 | 0 | 2 | 2 |
| Temporary bytecode cleaned up | YES | YES | YES | YES |
| Exact binding probe completion | Semantic binding probes: 5 checked, 0 failed. | Semantic binding probes: 5 checked, 0 failed. | Semantic binding probes: 5 checked, 0 failed. | Semantic binding probes: 5 checked, 0 failed. |
| Exact binding probes checked | 5 | 5 | 5 | 5 |
| Exact binding probe failures | 0 | 0 | 0 | 0 |
| Bytecode during analysis | N/A | N/A | N/A | N/A |
| Coverage versus baseline | VALID | VALID | VALID | VALID |

</details>

## Dataset details

<details>
<summary>sonar-java / file-by-file / no-libraries</summary>

Each production file is parsed separately; no external dependency JARs are supplied; native Maven module scope

## Summary

| Metric | Baseline | Source paths | Bytecode | Combined |
|---|---:|---:|---:|---:|
| Source paths enabled | NO | YES | NO | YES |
| Internal compilation enabled | NO | NO | YES | YES |
| Scan status | SUCCESS | SUCCESS | SUCCESS | SUCCESS |
| Java files analyzed | 1320 | 1320 | 1320 | 1320 |
| Identifiers (total) | 219976 | 219976 | 219976 | 219976 |
| Known identifiers | 126953 | 209514 | 131644 | 209514 |
| Unknown identifiers | 93023 | 10462 | 88332 | 10462 |
| Unknown identifiers (%) | 42.288% | 4.756% | 40.155% | 4.756% |
| Semantic graph resolved symbols | N/A | N/A | N/A | N/A |
| Semantic graph unknown symbols | N/A | N/A | N/A | N/A |
| Semantic graph resolved types | N/A | N/A | N/A | N/A |
| Semantic graph unknown types | N/A | N/A | N/A | N/A |
| Semantic graph traversal | DISABLED | DISABLED | DISABLED | DISABLED |
| Graph expansion limit per module | 0 | 0 | 0 | 0 |
| Graph expansions executed (representative sample) | 0 | 0 | 0 | 0 |
| Per-file semantic observations | AVAILABLE | AVAILABLE | AVAILABLE | AVAILABLE |
| Files with no unknown identifiers | 18 / 1320 | 203 / 1320 | 58 / 1320 | 203 / 1320 |
| Findings | 89 | 107 | 89 | 107 |
| Median Maven/server time (ms) | 151272.0 | 284094.0 | 160142.0 | 291581.0 |
| Median JavaSensor time, including compilation (ms) | 73111.0 | 205191.0 | 79763.0 | 212191.0 |
| Compilation outcome | DISABLED | DISABLED | FAILED | FAILED |
| Median internal compilation time (ms) | 0.0 | 0.0 | 1452.0 | 1379.0 |
| Compilation source files | 0 | 0 | 1320 | 1320 |
| Generated class files before cleanup | 0 | 0 | 192 | 192 |
| Temporary bytecode cleaned up | YES | YES | YES | YES |
| Source characters analyzed | 5688552 | 5688552 | 5688552 | 5688552 |
| Undefined-type errors | 21232 | 4362 | 20704 | 4362 |
| Source characters with parse errors | N/A | N/A | N/A | N/A |
| Source characters with analysis exceptions | N/A | N/A | N/A | N/A |

## Semantics per module

Each cell shows **total / known / unknown (unknown %)** from canonical module counts. Empty aggregator modules retain their actual zero counts.

| Module | Baseline | Source paths | Bytecode | Combined |
|---|---:|---:|---:|---:|
| check-list | 331 / 322 / 9 (2.719%) | 331 / 322 / 9 (2.719%) | 331 / 322 / 9 (2.719%) | 331 / 322 / 9 (2.719%) |
| external-reports | 1270 / 1012 / 258 (20.315%) | 1270 / 1029 / 241 (18.976%) | 1270 / 1012 / 258 (20.315%) | 1270 / 1029 / 241 (18.976%) |
| java-checks | 152148 / 81934 / 70214 (46.148%) | 152148 / 148268 / 3880 (2.550%) | 152148 / 81934 / 70214 (46.148%) | 152148 / 148268 / 3880 (2.550%) |
| java-checks-aws | 1269 / 663 / 606 (47.754%) | 1269 / 1244 / 25 (1.970%) | 1269 / 663 / 606 (47.754%) | 1269 / 1244 / 25 (1.970%) |
| java-checks-common | 1426 / 789 / 637 (44.670%) | 1426 / 1414 / 12 (0.842%) | 1426 / 789 / 637 (44.670%) | 1426 / 1414 / 12 (0.842%) |
| java-checks-test-sources/test-classpath-reader | 394 / 392 / 2 (0.508%) | 394 / 392 / 2 (0.508%) | 394 / 392 / 2 (0.508%) | 394 / 392 / 2 (0.508%) |
| java-checks-testkit | 5649 / 4264 / 1385 (24.518%) | 5649 / 5291 / 358 (6.337%) | 5649 / 4296 / 1353 (23.951%) | 5649 / 5291 / 358 (6.337%) |
| java-frontend | 54171 / 35477 / 18694 (34.509%) | 54171 / 49019 / 5152 (9.511%) | 54171 / 40077 / 14094 (26.018%) | 54171 / 49019 / 5152 (9.511%) |
| java-jsp | 659 / 504 / 155 (23.520%) | 659 / 523 / 136 (20.637%) | 659 / 504 / 155 (23.520%) | 659 / 523 / 136 (20.637%) |
| java-surefire | 1155 / 816 / 339 (29.351%) | 1155 / 965 / 190 (16.450%) | 1155 / 875 / 280 (24.242%) | 1155 / 965 / 190 (16.450%) |
| sonar-java-plugin | 1504 / 780 / 724 (48.138%) | 1504 / 1047 / 457 (30.386%) | 1504 / 780 / 724 (48.138%) | 1504 / 1047 / 457 (30.386%) |

### Feature comparisons

Changes are the first named mode minus the second.

| Comparison | Status | Unknown count change | Unknown % change (pp) | Files improved / unchanged / regressed | Shared findings | Second mode only | First mode only | Retention |
|---|---|---:|---:|---:|---:|---:|---:|---:|
| Source paths versus Baseline | VALID | -82561 | -37.532 | 1218 / 102 / 0 | 84 | 5 | 23 | 94.382% |
| Bytecode versus Baseline | VALID | -4691 | -2.133 | 308 / 1012 / 0 | 86 | 3 | 3 | 96.629% |
| Combined versus Baseline | VALID | -82561 | -37.532 | 1218 / 102 / 0 | 84 | 5 | 23 | 94.382% |
| Combined versus Source paths | VALID | +0 | +0.000 | 0 / 1320 / 0 | 107 | 0 | 0 | 100.000% |
| Combined versus Bytecode | VALID | -77870 | -35.399 | 1148 / 172 / 0 | 87 | 2 | 20 | 97.753% |

## Unknown occurrences by AST context

Context is the identifier's AST parent kind. Modes without occurrence details are UNAVAILABLE.

| AST context | Baseline | Source paths | Bytecode | Combined |
|---|---:|---:|---:|---:|
| AND | 1 | 0 | 1 | 0 |
| ANNOTATION | 2069 | 1867 | 1929 | 1867 |
| ARGUMENTS | 1771 | 94 | 1742 | 94 |
| ARRAY_TYPE | 90 | 22 | 81 | 22 |
| ASSIGNMENT | 1126 | 1107 | 1126 | 1107 |
| CASE_LABEL | 471 | 28 | 463 | 28 |
| CLASS | 1085 | 11 | 1069 | 11 |
| CONDITIONAL_AND | 2 | 0 | 2 | 0 |
| CONDITIONAL_EXPRESSION | 13 | 2 | 13 | 2 |
| CONDITIONAL_OR | 5 | 0 | 5 | 0 |
| EQUAL_TO | 27 | 1 | 26 | 1 |
| EXTENDS_WILDCARD | 110 | 13 | 92 | 13 |
| FOR_EACH_STATEMENT | 3 | 1 | 3 | 1 |
| GREATER_THAN | 21 | 0 | 21 | 0 |
| GREATER_THAN_OR_EQUAL_TO | 4 | 0 | 4 | 0 |
| IF_STATEMENT | 2 | 0 | 2 | 0 |
| INSTANCE_OF | 71 | 5 | 68 | 5 |
| LAMBDA_EXPRESSION | 5 | 2 | 5 | 2 |
| LESS_THAN | 9 | 2 | 9 | 2 |
| LIST | 452 | 37 | 334 | 37 |
| LOGICAL_COMPLEMENT | 3 | 0 | 3 | 0 |
| MEMBER_SELECT | 58079 | 5309 | 56561 | 5309 |
| METHOD | 1484 | 116 | 905 | 116 |
| METHOD_INVOCATION | 2033 | 46 | 1981 | 46 |
| METHOD_REFERENCE | 862 | 46 | 836 | 46 |
| MULTIPLY | 2 | 0 | 2 | 0 |
| NEW_ARRAY | 19 | 6 | 19 | 6 |
| NEW_CLASS | 511 | 95 | 410 | 95 |
| NOT_EQUAL_TO | 45 | 1 | 44 | 1 |
| PARAMETERIZED_TYPE | 3449 | 182 | 3094 | 182 |
| PATTERN_INSTANCE_OF | 8 | 0 | 14 | 0 |
| PLUS | 37 | 4 | 37 | 4 |
| REMAINDER | 1 | 0 | 1 | 0 |
| RETURN_STATEMENT | 26 | 2 | 26 | 2 |
| SUPER_WILDCARD | 1 | 0 | 1 | 0 |
| SWITCH_EXPRESSION | 2 | 0 | 2 | 0 |
| SWITCH_STATEMENT | 1 | 0 | 1 | 0 |
| TYPE_ARGUMENTS | 2364 | 142 | 2063 | 142 |
| TYPE_CAST | 2665 | 225 | 2515 | 225 |
| TYPE_PARAMETER | 10 | 2 | 10 | 2 |
| UNARY_MINUS | 1 | 0 | 1 | 0 |
| VARIABLE | 14083 | 1094 | 12811 | 1094 |

## Top files contributing unknown identifiers

Top five by the largest unknown count in any mode. Cells show **unknown count (share of project unknowns)**.

| File | Baseline | Source paths | Bytecode | Combined |
|---|---:|---:|---:|---:|
| java-frontend/src/main/java/org/sonar/java/model/JParser.java | 3484 (3.745%) | 2228 (21.296%) | 3109 (3.520%) | 2228 (21.296%) |
| java-checks/src/main/java/org/sonar/java/checks/tests/AssertJChainSimplificationIndex.java | 674 (0.725%) | 30 (0.287%) | 674 (0.763%) | 30 (0.287%) |
| java-frontend/src/main/java/org/sonar/java/cfg/CFG.java | 634 (0.682%) | 35 (0.335%) | 426 (0.482%) | 35 (0.335%) |
| java-checks-testkit/src/main/java/org/sonar/java/checks/verifier/internal/InternalCheckVerifier.java | 551 (0.592%) | 41 (0.392%) | 551 (0.624%) | 41 (0.392%) |
| java-checks/src/main/java/org/sonar/java/checks/ReplaceLambdaByMethodRefCheck.java | 537 (0.577%) | 7 (0.067%) | 537 (0.608%) | 7 (0.067%) |

## Semantics per file

Each cell shows **total / known / unknown (unknown %)**. Absent observations are UNAVAILABLE; missing coverage in an observed mode is N/A.

| File | Baseline | Source paths | Bytecode | Combined |
|---|---:|---:|---:|---:|
| check-list/src/main/java/org/sonar/java/CheckListGenerator.java | 327 / 319 / 8 (2.446%) | 327 / 319 / 8 (2.446%) | 327 / 319 / 8 (2.446%) | 327 / 319 / 8 (2.446%) |
| check-list/src/main/java/org/sonar/java/package-info.java | 4 / 3 / 1 (25.000%) | 4 / 3 / 1 (25.000%) | 4 / 3 / 1 (25.000%) | 4 / 3 / 1 (25.000%) |
| external-reports/src/main/java/org/sonar/java/externalreport/CheckstyleSensor.java | 115 / 86 / 29 (25.217%) | 115 / 91 / 24 (20.870%) | 115 / 86 / 29 (25.217%) | 115 / 91 / 24 (20.870%) |
| external-reports/src/main/java/org/sonar/java/externalreport/CheckstyleXmlReportReader.java | 218 / 196 / 22 (10.092%) | 218 / 196 / 22 (10.092%) | 218 / 196 / 22 (10.092%) | 218 / 196 / 22 (10.092%) |
| external-reports/src/main/java/org/sonar/java/externalreport/ExternalIssueUtils.java | 91 / 62 / 29 (31.868%) | 91 / 62 / 29 (31.868%) | 91 / 62 / 29 (31.868%) | 91 / 62 / 29 (31.868%) |
| external-reports/src/main/java/org/sonar/java/externalreport/ExternalRulesDefinition.java | 32 / 27 / 5 (15.625%) | 32 / 27 / 5 (15.625%) | 32 / 27 / 5 (15.625%) | 32 / 27 / 5 (15.625%) |
| external-reports/src/main/java/org/sonar/java/externalreport/PmdSensor.java | 89 / 64 / 25 (28.090%) | 89 / 67 / 22 (24.719%) | 89 / 64 / 25 (28.090%) | 89 / 67 / 22 (24.719%) |
| external-reports/src/main/java/org/sonar/java/externalreport/PmdXmlReportReader.java | 283 / 217 / 66 (23.322%) | 283 / 219 / 64 (22.615%) | 283 / 217 / 66 (23.322%) | 283 / 219 / 64 (22.615%) |
| external-reports/src/main/java/org/sonar/java/externalreport/SpotBugsSensor.java | 143 / 108 / 35 (24.476%) | 143 / 111 / 32 (22.378%) | 143 / 108 / 35 (24.476%) | 143 / 111 / 32 (22.378%) |
| external-reports/src/main/java/org/sonar/java/externalreport/SpotBugsXmlReportReader.java | 294 / 248 / 46 (15.646%) | 294 / 252 / 42 (14.286%) | 294 / 248 / 46 (15.646%) | 294 / 252 / 42 (14.286%) |
| external-reports/src/main/java/org/sonar/java/externalreport/package-info.java | 5 / 4 / 1 (20.000%) | 5 / 4 / 1 (20.000%) | 5 / 4 / 1 (20.000%) | 5 / 4 / 1 (20.000%) |
| java-checks-aws/src/main/java/org/sonar/java/checks/aws/AbstractAwsMethodVisitor.java | 66 / 30 / 36 (54.545%) | 66 / 66 / 0 (0.000%) | 66 / 30 / 36 (54.545%) | 66 / 66 / 0 (0.000%) |
| java-checks-aws/src/main/java/org/sonar/java/checks/aws/AwsBuilderMethodFinder.java | 300 / 152 / 148 (49.333%) | 300 / 300 / 0 (0.000%) | 300 / 152 / 148 (49.333%) | 300 / 300 / 0 (0.000%) |
| java-checks-aws/src/main/java/org/sonar/java/checks/aws/AwsConsumerBuilderUsageCheck.java | 216 / 106 / 110 (50.926%) | 216 / 213 / 3 (1.389%) | 216 / 106 / 110 (50.926%) | 216 / 213 / 3 (1.389%) |
| java-checks-aws/src/main/java/org/sonar/java/checks/aws/AwsCredentialsShouldBeSetExplicitlyCheck.java | 25 / 13 / 12 (48.000%) | 25 / 23 / 2 (8.000%) | 25 / 13 / 12 (48.000%) | 25 / 23 / 2 (8.000%) |
| java-checks-aws/src/main/java/org/sonar/java/checks/aws/AwsLambdaSyncCallCheck.java | 250 / 127 / 123 (49.200%) | 250 / 248 / 2 (0.800%) | 250 / 127 / 123 (49.200%) | 250 / 248 / 2 (0.800%) |
| java-checks-aws/src/main/java/org/sonar/java/checks/aws/AwsLongTermAccessKeysCheck.java | 28 / 15 / 13 (46.429%) | 28 / 26 / 2 (7.143%) | 28 / 15 / 13 (46.429%) | 28 / 26 / 2 (7.143%) |
| java-checks-aws/src/main/java/org/sonar/java/checks/aws/AwsRegionSetterCheck.java | 43 / 23 / 20 (46.512%) | 43 / 41 / 2 (4.651%) | 43 / 23 / 20 (46.512%) | 43 / 41 / 2 (4.651%) |
| java-checks-aws/src/main/java/org/sonar/java/checks/aws/AwsRegionShouldBeSetExplicitlyCheck.java | 25 / 13 / 12 (48.000%) | 25 / 23 / 2 (8.000%) | 25 / 13 / 12 (48.000%) | 25 / 23 / 2 (8.000%) |
| java-checks-aws/src/main/java/org/sonar/java/checks/aws/AwsReusableResourcesInitializedOnceCheck.java | 140 / 76 / 64 (45.714%) | 140 / 138 / 2 (1.429%) | 140 / 76 / 64 (45.714%) | 140 / 138 / 2 (1.429%) |
| java-checks-aws/src/main/java/org/sonar/java/checks/aws/package-info.java | 6 / 5 / 1 (16.667%) | 6 / 5 / 1 (16.667%) | 6 / 5 / 1 (16.667%) | 6 / 5 / 1 (16.667%) |
| java-checks-aws/src/main/java/org/sonar/java/checks/security/HardCodedCredentialsShouldNotBeUsedCheck.java | 164 / 98 / 66 (40.244%) | 164 / 156 / 8 (4.878%) | 164 / 98 / 66 (40.244%) | 164 / 156 / 8 (4.878%) |
| java-checks-aws/src/main/java/org/sonar/java/checks/security/package-info.java | 6 / 5 / 1 (16.667%) | 6 / 5 / 1 (16.667%) | 6 / 5 / 1 (16.667%) | 6 / 5 / 1 (16.667%) |
| java-checks-common/src/main/java/org/sonar/java/checks/helpers/CredentialMethod.java | 96 / 83 / 13 (13.542%) | 96 / 96 / 0 (0.000%) | 96 / 83 / 13 (13.542%) | 96 / 96 / 0 (0.000%) |
| java-checks-common/src/main/java/org/sonar/java/checks/helpers/CredentialMethodsLoader.java | 48 / 37 / 11 (22.917%) | 48 / 46 / 2 (4.167%) | 48 / 37 / 11 (22.917%) | 48 / 46 / 2 (4.167%) |
| java-checks-common/src/main/java/org/sonar/java/checks/helpers/ExpressionsHelper.java | 514 / 288 / 226 (43.969%) | 514 / 511 / 3 (0.584%) | 514 / 288 / 226 (43.969%) | 514 / 511 / 3 (0.584%) |
| java-checks-common/src/main/java/org/sonar/java/checks/helpers/HardcodedStringExpressionChecker.java | 397 / 181 / 216 (54.408%) | 397 / 397 / 0 (0.000%) | 397 / 181 / 216 (54.408%) | 397 / 397 / 0 (0.000%) |
| java-checks-common/src/main/java/org/sonar/java/checks/helpers/ReassignmentFinder.java | 166 / 86 / 80 (48.193%) | 166 / 161 / 5 (3.012%) | 166 / 86 / 80 (48.193%) | 166 / 161 / 5 (3.012%) |
| java-checks-common/src/main/java/org/sonar/java/checks/helpers/TreeHelper.java | 105 / 57 / 48 (45.714%) | 105 / 105 / 0 (0.000%) | 105 / 57 / 48 (45.714%) | 105 / 105 / 0 (0.000%) |
| java-checks-common/src/main/java/org/sonar/java/checks/helpers/package-info.java | 6 / 5 / 1 (16.667%) | 6 / 5 / 1 (16.667%) | 6 / 5 / 1 (16.667%) | 6 / 5 / 1 (16.667%) |
| java-checks-common/src/main/java/org/sonar/java/checks/methods/AbstractMethodDetection.java | 88 / 47 / 41 (46.591%) | 88 / 88 / 0 (0.000%) | 88 / 47 / 41 (46.591%) | 88 / 88 / 0 (0.000%) |
| java-checks-common/src/main/java/org/sonar/java/checks/methods/package-info.java | 6 / 5 / 1 (16.667%) | 6 / 5 / 1 (16.667%) | 6 / 5 / 1 (16.667%) | 6 / 5 / 1 (16.667%) |
| java-checks-test-sources/test-classpath-reader/src/main/java/org/sonar/java/test/classpath/TestClasspathUtils.java | 388 / 387 / 1 (0.258%) | 388 / 387 / 1 (0.258%) | 388 / 387 / 1 (0.258%) | 388 / 387 / 1 (0.258%) |
| java-checks-test-sources/test-classpath-reader/src/main/java/org/sonar/java/test/classpath/package-info.java | 6 / 5 / 1 (16.667%) | 6 / 5 / 1 (16.667%) | 6 / 5 / 1 (16.667%) | 6 / 5 / 1 (16.667%) |
| java-checks-testkit/src/main/java/org/sonar/java/checks/verifier/CheckVerifier.java | 106 / 90 / 16 (15.094%) | 106 / 97 / 9 (8.491%) | 106 / 90 / 16 (15.094%) | 106 / 97 / 9 (8.491%) |
| java-checks-testkit/src/main/java/org/sonar/java/checks/verifier/FilesUtils.java | 63 / 62 / 1 (1.587%) | 63 / 63 / 0 (0.000%) | 63 / 62 / 1 (1.587%) | 63 / 63 / 0 (0.000%) |
| java-checks-testkit/src/main/java/org/sonar/java/checks/verifier/TestCheckRegistrarContext.java | 205 / 132 / 73 (35.610%) | 205 / 171 / 34 (16.585%) | 205 / 132 / 73 (35.610%) | 205 / 171 / 34 (16.585%) |
| java-checks-testkit/src/main/java/org/sonar/java/checks/verifier/TestProfileRegistrarContext.java | 28 / 17 / 11 (39.286%) | 28 / 19 / 9 (32.143%) | 28 / 17 / 11 (39.286%) | 28 / 19 / 9 (32.143%) |
| java-checks-testkit/src/main/java/org/sonar/java/checks/verifier/TestUtils.java | 129 / 102 / 27 (20.930%) | 129 / 119 / 10 (7.752%) | 129 / 102 / 27 (20.930%) | 129 / 119 / 10 (7.752%) |
| java-checks-testkit/src/main/java/org/sonar/java/checks/verifier/internal/CacheEnabledSensorContext.java | 33 / 26 / 7 (21.212%) | 33 / 27 / 6 (18.182%) | 33 / 26 / 7 (21.212%) | 33 / 27 / 6 (18.182%) |
| java-checks-testkit/src/main/java/org/sonar/java/checks/verifier/internal/CheckVerifierUtils.java | 103 / 79 / 24 (23.301%) | 103 / 89 / 14 (13.592%) | 103 / 79 / 24 (23.301%) | 103 / 89 / 14 (13.592%) |
| java-checks-testkit/src/main/java/org/sonar/java/checks/verifier/internal/Expectations.java | 1661 / 1520 / 141 (8.489%) | 1661 / 1615 / 46 (2.769%) | 1661 / 1520 / 141 (8.489%) | 1661 / 1615 / 46 (2.769%) |
| java-checks-testkit/src/main/java/org/sonar/java/checks/verifier/internal/InternalCacheContext.java | 75 / 66 / 9 (12.000%) | 75 / 73 / 2 (2.667%) | 75 / 66 / 9 (12.000%) | 75 / 73 / 2 (2.667%) |
| java-checks-testkit/src/main/java/org/sonar/java/checks/verifier/internal/InternalCheckVerifier.java | 1572 / 1021 / 551 (35.051%) | 1572 / 1531 / 41 (2.608%) | 1572 / 1021 / 551 (35.051%) | 1572 / 1531 / 41 (2.608%) |
| java-checks-testkit/src/main/java/org/sonar/java/checks/verifier/internal/InternalConfiguration.java | 45 / 40 / 5 (11.111%) | 45 / 44 / 1 (2.222%) | 45 / 42 / 3 (6.667%) | 45 / 44 / 1 (2.222%) |
| java-checks-testkit/src/main/java/org/sonar/java/checks/verifier/internal/InternalFileSystem.java | 77 / 60 / 17 (22.078%) | 77 / 67 / 10 (12.987%) | 77 / 67 / 10 (12.987%) | 77 / 67 / 10 (12.987%) |
| java-checks-testkit/src/main/java/org/sonar/java/checks/verifier/internal/InternalInputFile.java | 255 / 217 / 38 (14.902%) | 255 / 226 / 29 (11.373%) | 255 / 219 / 36 (14.118%) | 255 / 226 / 29 (11.373%) |
| java-checks-testkit/src/main/java/org/sonar/java/checks/verifier/internal/InternalMockedSonarAPI.java | 37 / 37 / 0 (0.000%) | 37 / 37 / 0 (0.000%) | 37 / 37 / 0 (0.000%) | 37 / 37 / 0 (0.000%) |
| java-checks-testkit/src/main/java/org/sonar/java/checks/verifier/internal/InternalReadCache.java | 68 / 65 / 3 (4.412%) | 68 / 67 / 1 (1.471%) | 68 / 65 / 3 (4.412%) | 68 / 67 / 1 (1.471%) |
| java-checks-testkit/src/main/java/org/sonar/java/checks/verifier/internal/InternalSensorContext.java | 163 / 108 / 55 (33.742%) | 163 / 132 / 31 (19.018%) | 163 / 127 / 36 (22.086%) | 163 / 132 / 31 (19.018%) |
| java-checks-testkit/src/main/java/org/sonar/java/checks/verifier/internal/InternalSonarRuntime.java | 31 / 17 / 14 (45.161%) | 31 / 17 / 14 (45.161%) | 31 / 17 / 14 (45.161%) | 31 / 17 / 14 (45.161%) |
| java-checks-testkit/src/main/java/org/sonar/java/checks/verifier/internal/InternalTextPointer.java | 42 / 32 / 10 (23.810%) | 42 / 32 / 10 (23.810%) | 42 / 32 / 10 (23.810%) | 42 / 32 / 10 (23.810%) |
| java-checks-testkit/src/main/java/org/sonar/java/checks/verifier/internal/InternalTextRange.java | 49 / 37 / 12 (24.490%) | 49 / 41 / 8 (16.327%) | 49 / 39 / 10 (20.408%) | 49 / 41 / 8 (16.327%) |
| java-checks-testkit/src/main/java/org/sonar/java/checks/verifier/internal/InternalWriteCache.java | 65 / 61 / 4 (6.154%) | 65 / 61 / 4 (6.154%) | 65 / 61 / 4 (6.154%) | 65 / 61 / 4 (6.154%) |
| java-checks-testkit/src/main/java/org/sonar/java/checks/verifier/internal/JavaCheckVerifier.java | 829 / 464 / 365 (44.029%) | 829 / 752 / 77 (9.288%) | 829 / 464 / 365 (44.029%) | 829 / 752 / 77 (9.288%) |
| java-checks-testkit/src/main/java/org/sonar/java/checks/verifier/internal/package-info.java | 7 / 6 / 1 (14.286%) | 7 / 6 / 1 (14.286%) | 7 / 6 / 1 (14.286%) | 7 / 6 / 1 (14.286%) |
| java-checks-testkit/src/main/java/org/sonar/java/checks/verifier/package-info.java | 6 / 5 / 1 (16.667%) | 6 / 5 / 1 (16.667%) | 6 / 5 / 1 (16.667%) | 6 / 5 / 1 (16.667%) |
| java-checks/src/main/java/org/sonar/java/checks/AbsOnNegativeCheck.java | 155 / 58 / 97 (62.581%) | 155 / 152 / 3 (1.935%) | 155 / 58 / 97 (62.581%) | 155 / 152 / 3 (1.935%) |
| java-checks/src/main/java/org/sonar/java/checks/AbstractAccessibilityChangeChecker.java | 257 / 91 / 166 (64.591%) | 257 / 257 / 0 (0.000%) | 257 / 91 / 166 (64.591%) | 257 / 257 / 0 (0.000%) |
| java-checks/src/main/java/org/sonar/java/checks/AbstractBadFieldNameChecker.java | 85 / 53 / 32 (37.647%) | 85 / 85 / 0 (0.000%) | 85 / 53 / 32 (37.647%) | 85 / 85 / 0 (0.000%) |
| java-checks/src/main/java/org/sonar/java/checks/AbstractCallToDeprecatedCodeChecker.java | 249 / 115 / 134 (53.815%) | 249 / 247 / 2 (0.803%) | 249 / 115 / 134 (53.815%) | 249 / 247 / 2 (0.803%) |
| java-checks/src/main/java/org/sonar/java/checks/AbstractClassNoFieldShouldBeInterfaceCheck.java | 167 / 74 / 93 (55.689%) | 167 / 165 / 2 (1.198%) | 167 / 74 / 93 (55.689%) | 167 / 165 / 2 (1.198%) |
| java-checks/src/main/java/org/sonar/java/checks/AbstractClassWithoutAbstractMethodCheck.java | 119 / 71 / 48 (40.336%) | 119 / 117 / 2 (1.681%) | 119 / 71 / 48 (40.336%) | 119 / 117 / 2 (1.681%) |
| java-checks/src/main/java/org/sonar/java/checks/AbstractCreateTempFileChecker.java | 242 / 141 / 101 (41.736%) | 242 / 241 / 1 (0.413%) | 242 / 141 / 101 (41.736%) | 242 / 241 / 1 (0.413%) |
| java-checks/src/main/java/org/sonar/java/checks/AbstractForLoopRule.java | 372 / 214 / 158 (42.473%) | 372 / 364 / 8 (2.151%) | 372 / 214 / 158 (42.473%) | 372 / 364 / 8 (2.151%) |
| java-checks/src/main/java/org/sonar/java/checks/AbstractHardCodedCredentialChecker.java | 393 / 238 / 155 (39.440%) | 393 / 389 / 4 (1.018%) | 393 / 238 / 155 (39.440%) | 393 / 389 / 4 (1.018%) |
| java-checks/src/main/java/org/sonar/java/checks/AbstractInSynchronizeChecker.java | 107 / 53 / 54 (50.467%) | 107 / 107 / 0 (0.000%) | 107 / 53 / 54 (50.467%) | 107 / 107 / 0 (0.000%) |
| java-checks/src/main/java/org/sonar/java/checks/AbstractMissingDeprecatedChecker.java | 79 / 54 / 25 (31.646%) | 79 / 78 / 1 (1.266%) | 79 / 54 / 25 (31.646%) | 79 / 78 / 1 (1.266%) |
| java-checks/src/main/java/org/sonar/java/checks/AbstractPackageInfoChecker.java | 125 / 87 / 38 (30.400%) | 125 / 117 / 8 (6.400%) | 125 / 87 / 38 (30.400%) | 125 / 117 / 8 (6.400%) |
| java-checks/src/main/java/org/sonar/java/checks/AbstractPrintfChecker.java | 652 / 480 / 172 (26.380%) | 652 / 647 / 5 (0.767%) | 652 / 480 / 172 (26.380%) | 652 / 647 / 5 (0.767%) |
| java-checks/src/main/java/org/sonar/java/checks/AbstractSerializableInnerClassRule.java | 107 / 56 / 51 (47.664%) | 107 / 107 / 0 (0.000%) | 107 / 56 / 51 (47.664%) | 107 / 107 / 0 (0.000%) |
| java-checks/src/main/java/org/sonar/java/checks/AccessibilityChangeCheck.java | 23 / 13 / 10 (43.478%) | 23 / 21 / 2 (8.696%) | 23 / 13 / 10 (43.478%) | 23 / 21 / 2 (8.696%) |
| java-checks/src/main/java/org/sonar/java/checks/AccessibilityChangeOnRecordsCheck.java | 187 / 84 / 103 (55.080%) | 187 / 185 / 2 (1.070%) | 187 / 84 / 103 (55.080%) | 187 / 185 / 2 (1.070%) |
| java-checks/src/main/java/org/sonar/java/checks/AllBranchesAreIdenticalCheck.java | 114 / 43 / 71 (62.281%) | 114 / 112 / 2 (1.754%) | 114 / 43 / 71 (62.281%) | 114 / 112 / 2 (1.754%) |
| java-checks/src/main/java/org/sonar/java/checks/AlmostJavadocCheck.java | 356 / 192 / 164 (46.067%) | 356 / 354 / 2 (0.562%) | 356 / 192 / 164 (46.067%) | 356 / 354 / 2 (0.562%) |
| java-checks/src/main/java/org/sonar/java/checks/AnnotationDefaultArgumentCheck.java | 129 / 76 / 53 (41.085%) | 129 / 126 / 3 (2.326%) | 129 / 76 / 53 (41.085%) | 129 / 126 / 3 (2.326%) |
| java-checks/src/main/java/org/sonar/java/checks/AnonymousClassOnFunctionalInterfaceCheck.java | 23 / 14 / 9 (39.130%) | 23 / 21 / 2 (8.696%) | 23 / 14 / 9 (39.130%) | 23 / 21 / 2 (8.696%) |
| java-checks/src/main/java/org/sonar/java/checks/AnonymousClassShouldBeLambdaCheck.java | 23 / 14 / 9 (39.130%) | 23 / 21 / 2 (8.696%) | 23 / 14 / 9 (39.130%) | 23 / 21 / 2 (8.696%) |
| java-checks/src/main/java/org/sonar/java/checks/AnonymousClassesTooBigCheck.java | 71 / 47 / 24 (33.803%) | 71 / 65 / 6 (8.451%) | 71 / 47 / 24 (33.803%) | 71 / 65 / 6 (8.451%) |
| java-checks/src/main/java/org/sonar/java/checks/ArrayCopyLoopCheck.java | 677 / 331 / 346 (51.108%) | 677 / 665 / 12 (1.773%) | 677 / 331 / 346 (51.108%) | 677 / 665 / 12 (1.773%) |
| java-checks/src/main/java/org/sonar/java/checks/ArrayDesignatorAfterTypeCheck.java | 54 / 17 / 37 (68.519%) | 54 / 52 / 2 (3.704%) | 54 / 17 / 37 (68.519%) | 54 / 52 / 2 (3.704%) |
| java-checks/src/main/java/org/sonar/java/checks/ArrayDesignatorOnVariableCheck.java | 189 / 86 / 103 (54.497%) | 189 / 186 / 3 (1.587%) | 189 / 86 / 103 (54.497%) | 189 / 186 / 3 (1.587%) |
| java-checks/src/main/java/org/sonar/java/checks/ArrayForVarArgCheck.java | 205 / 104 / 101 (49.268%) | 205 / 203 / 2 (0.976%) | 205 / 104 / 101 (49.268%) | 205 / 203 / 2 (0.976%) |
| java-checks/src/main/java/org/sonar/java/checks/ArrayHashCodeAndToStringCheck.java | 117 / 53 / 64 (54.701%) | 117 / 115 / 2 (1.709%) | 117 / 53 / 64 (54.701%) | 117 / 115 / 2 (1.709%) |
| java-checks/src/main/java/org/sonar/java/checks/ArraysAsListOfPrimitiveToStreamCheck.java | 106 / 42 / 64 (60.377%) | 106 / 104 / 2 (1.887%) | 106 / 42 / 64 (60.377%) | 106 / 104 / 2 (1.887%) |
| java-checks/src/main/java/org/sonar/java/checks/ArraysFillIncompatibleTypeCheck.java | 255 / 143 / 112 (43.922%) | 255 / 253 / 2 (0.784%) | 255 / 143 / 112 (43.922%) | 255 / 253 / 2 (0.784%) |
| java-checks/src/main/java/org/sonar/java/checks/AssertOnBooleanVariableCheck.java | 57 / 35 / 22 (38.596%) | 57 / 55 / 2 (3.509%) | 57 / 35 / 22 (38.596%) | 57 / 55 / 2 (3.509%) |
| java-checks/src/main/java/org/sonar/java/checks/AssertThrowsInsteadOfTryCatchFailCheck.java | 316 / 156 / 160 (50.633%) | 316 / 311 / 5 (1.582%) | 316 / 156 / 160 (50.633%) | 316 / 311 / 5 (1.582%) |
| java-checks/src/main/java/org/sonar/java/checks/AssertionsInProductionCodeCheck.java | 107 / 58 / 49 (45.794%) | 107 / 105 / 2 (1.869%) | 107 / 58 / 49 (45.794%) | 107 / 105 / 2 (1.869%) |
| java-checks/src/main/java/org/sonar/java/checks/AssertsOnParametersOfPublicMethodCheck.java | 91 / 40 / 51 (56.044%) | 91 / 89 / 2 (2.198%) | 91 / 40 / 51 (56.044%) | 91 / 89 / 2 (2.198%) |
| java-checks/src/main/java/org/sonar/java/checks/AssignmentInSubExpressionCheck.java | 206 / 108 / 98 (47.573%) | 206 / 200 / 6 (2.913%) | 206 / 108 / 98 (47.573%) | 206 / 200 / 6 (2.913%) |
| java-checks/src/main/java/org/sonar/java/checks/AtLeastOneConstructorCheck.java | 136 / 80 / 56 (41.176%) | 136 / 134 / 2 (1.471%) | 136 / 80 / 56 (41.176%) | 136 / 134 / 2 (1.471%) |
| java-checks/src/main/java/org/sonar/java/checks/AvoidHighFrameratesOnMobileCheck.java | 75 / 20 / 55 (73.333%) | 75 / 73 / 2 (2.667%) | 75 / 20 / 55 (73.333%) | 75 / 73 / 2 (2.667%) |
| java-checks/src/main/java/org/sonar/java/checks/BareDotRegexpCheck.java | 122 / 56 / 66 (54.098%) | 122 / 120 / 2 (1.639%) | 122 / 56 / 66 (54.098%) | 122 / 120 / 2 (1.639%) |
| java-checks/src/main/java/org/sonar/java/checks/BasicAuthCheck.java | 109 / 45 / 64 (58.716%) | 109 / 107 / 2 (1.835%) | 109 / 45 / 64 (58.716%) | 109 / 107 / 2 (1.835%) |
| java-checks/src/main/java/org/sonar/java/checks/BatchSQLStatementsCheck.java | 128 / 55 / 73 (57.031%) | 128 / 126 / 2 (1.563%) | 128 / 55 / 73 (57.031%) | 128 / 126 / 2 (1.563%) |
| java-checks/src/main/java/org/sonar/java/checks/BeanValidationConstraintOnStaticFieldCheck.java | 62 / 36 / 26 (41.935%) | 62 / 60 / 2 (3.226%) | 62 / 36 / 26 (41.935%) | 62 / 60 / 2 (3.226%) |
| java-checks/src/main/java/org/sonar/java/checks/BigDecimalDoubleConstructorCheck.java | 130 / 62 / 68 (52.308%) | 130 / 128 / 2 (1.538%) | 130 / 62 / 68 (52.308%) | 130 / 128 / 2 (1.538%) |
| java-checks/src/main/java/org/sonar/java/checks/BigDecimalEqualsCheck.java | 211 / 108 / 103 (48.815%) | 211 / 209 / 2 (0.948%) | 211 / 108 / 103 (48.815%) | 211 / 209 / 2 (0.948%) |
| java-checks/src/main/java/org/sonar/java/checks/BitwiseAndWithZeroCheck.java | 65 / 36 / 29 (44.615%) | 65 / 63 / 2 (3.077%) | 65 / 36 / 29 (44.615%) | 65 / 63 / 2 (3.077%) |
| java-checks/src/main/java/org/sonar/java/checks/BlockingOperationsInVirtualThreadsCheck.java | 168 / 73 / 95 (56.548%) | 168 / 166 / 2 (1.190%) | 168 / 73 / 95 (56.548%) | 168 / 166 / 2 (1.190%) |
| java-checks/src/main/java/org/sonar/java/checks/BluetoothLowPowerModeCheck.java | 76 / 38 / 38 (50.000%) | 76 / 74 / 2 (2.632%) | 76 / 38 / 38 (50.000%) | 76 / 74 / 2 (2.632%) |
| java-checks/src/main/java/org/sonar/java/checks/BooleanInversionCheck.java | 74 / 26 / 48 (64.865%) | 74 / 63 / 11 (14.865%) | 74 / 26 / 48 (64.865%) | 74 / 63 / 11 (14.865%) |
| java-checks/src/main/java/org/sonar/java/checks/BooleanLiteralCheck.java | 698 / 399 / 299 (42.837%) | 698 / 695 / 3 (0.430%) | 698 / 399 / 299 (42.837%) | 698 / 695 / 3 (0.430%) |
| java-checks/src/main/java/org/sonar/java/checks/BooleanMethodReturnCheck.java | 68 / 36 / 32 (47.059%) | 68 / 66 / 2 (2.941%) | 68 / 36 / 32 (47.059%) | 68 / 66 / 2 (2.941%) |
| java-checks/src/main/java/org/sonar/java/checks/BoxedBooleanExpressionsCheck.java | 535 / 255 / 280 (52.336%) | 535 / 532 / 3 (0.561%) | 535 / 255 / 280 (52.336%) | 535 / 532 / 3 (0.561%) |
| java-checks/src/main/java/org/sonar/java/checks/BufferedReaderBoilerplateCheck.java | 77 / 35 / 42 (54.545%) | 77 / 75 / 2 (2.597%) | 77 / 35 / 42 (54.545%) | 77 / 75 / 2 (2.597%) |
| java-checks/src/main/java/org/sonar/java/checks/CORSCheck.java | 292 / 132 / 160 (54.795%) | 292 / 288 / 4 (1.370%) | 292 / 132 / 160 (54.795%) | 292 / 288 / 4 (1.370%) |
| java-checks/src/main/java/org/sonar/java/checks/CallOuterPrivateMethodCheck.java | 290 / 137 / 153 (52.759%) | 290 / 287 / 3 (1.034%) | 290 / 137 / 153 (52.759%) | 290 / 287 / 3 (1.034%) |
| java-checks/src/main/java/org/sonar/java/checks/CallSuperMethodFromInnerClassCheck.java | 162 / 86 / 76 (46.914%) | 162 / 160 / 2 (1.235%) | 162 / 86 / 76 (46.914%) | 162 / 160 / 2 (1.235%) |
| java-checks/src/main/java/org/sonar/java/checks/CallToDeprecatedCodeMarkedForRemovalCheck.java | 54 / 31 / 23 (42.593%) | 54 / 52 / 2 (3.704%) | 54 / 31 / 23 (42.593%) | 54 / 52 / 2 (3.704%) |
| java-checks/src/main/java/org/sonar/java/checks/CallToDeprecatedMethodCheck.java | 90 / 43 / 47 (52.222%) | 90 / 85 / 5 (5.556%) | 90 / 43 / 47 (52.222%) | 90 / 85 / 5 (5.556%) |
| java-checks/src/main/java/org/sonar/java/checks/CallToFileDeleteOnExitMethodCheck.java | 28 / 11 / 17 (60.714%) | 28 / 23 / 5 (17.857%) | 28 / 11 / 17 (60.714%) | 28 / 23 / 5 (17.857%) |
| java-checks/src/main/java/org/sonar/java/checks/CaseInsensitiveComparisonCheck.java | 88 / 45 / 43 (48.864%) | 88 / 86 / 2 (2.273%) | 88 / 45 / 43 (48.864%) | 88 / 86 / 2 (2.273%) |
| java-checks/src/main/java/org/sonar/java/checks/CastArithmeticOperandCheck.java | 350 / 176 / 174 (49.714%) | 350 / 340 / 10 (2.857%) | 350 / 176 / 174 (49.714%) | 350 / 340 / 10 (2.857%) |
| java-checks/src/main/java/org/sonar/java/checks/CastDoubleToFloatCheck.java | 146 / 88 / 58 (39.726%) | 146 / 144 / 2 (1.370%) | 146 / 88 / 58 (39.726%) | 146 / 144 / 2 (1.370%) |
| java-checks/src/main/java/org/sonar/java/checks/CatchExceptionCheck.java | 138 / 87 / 51 (36.957%) | 138 / 135 / 3 (2.174%) | 138 / 87 / 51 (36.957%) | 138 / 135 / 3 (2.174%) |
| java-checks/src/main/java/org/sonar/java/checks/CatchIllegalMonitorStateExceptionCheck.java | 54 / 29 / 25 (46.296%) | 54 / 52 / 2 (3.704%) | 54 / 29 / 25 (46.296%) | 54 / 52 / 2 (3.704%) |
| java-checks/src/main/java/org/sonar/java/checks/CatchNPECheck.java | 119 / 66 / 53 (44.538%) | 119 / 117 / 2 (1.681%) | 119 / 66 / 53 (44.538%) | 119 / 117 / 2 (1.681%) |
| java-checks/src/main/java/org/sonar/java/checks/CatchOfThrowableOrErrorCheck.java | 219 / 131 / 88 (40.183%) | 219 / 217 / 2 (0.913%) | 219 / 131 / 88 (40.183%) | 219 / 217 / 2 (0.913%) |
| java-checks/src/main/java/org/sonar/java/checks/CatchRethrowingCheck.java | 67 / 28 / 39 (58.209%) | 67 / 65 / 2 (2.985%) | 67 / 28 / 39 (58.209%) | 67 / 65 / 2 (2.985%) |
| java-checks/src/main/java/org/sonar/java/checks/CatchUsesExceptionWithContextCheck.java | 677 / 379 / 298 (44.018%) | 677 / 665 / 12 (1.773%) | 677 / 379 / 298 (44.018%) | 677 / 665 / 12 (1.773%) |
| java-checks/src/main/java/org/sonar/java/checks/ChangeMethodContractCheck.java | 241 / 146 / 95 (39.419%) | 241 / 239 / 2 (0.830%) | 241 / 146 / 95 (39.419%) | 241 / 239 / 2 (0.830%) |
| java-checks/src/main/java/org/sonar/java/checks/ChildClassShadowFieldCheck.java | 123 / 57 / 66 (53.659%) | 123 / 120 / 3 (2.439%) | 123 / 57 / 66 (53.659%) | 123 / 120 / 3 (2.439%) |
| java-checks/src/main/java/org/sonar/java/checks/ClassBuilderWithMethodCheck.java | 103 / 41 / 62 (60.194%) | 103 / 101 / 2 (1.942%) | 103 / 41 / 62 (60.194%) | 103 / 101 / 2 (1.942%) |
| java-checks/src/main/java/org/sonar/java/checks/ClassComparedByNameCheck.java | 99 / 48 / 51 (51.515%) | 99 / 97 / 2 (2.020%) | 99 / 48 / 51 (51.515%) | 99 / 97 / 2 (2.020%) |
| java-checks/src/main/java/org/sonar/java/checks/ClassFieldCountCheck.java | 100 / 53 / 47 (47.000%) | 100 / 90 / 10 (10.000%) | 100 / 53 / 47 (47.000%) | 100 / 90 / 10 (10.000%) |
| java-checks/src/main/java/org/sonar/java/checks/ClassNameInClassTransformCheck.java | 225 / 77 / 148 (65.778%) | 225 / 223 / 2 (0.889%) | 225 / 77 / 148 (65.778%) | 225 / 223 / 2 (0.889%) |
| java-checks/src/main/java/org/sonar/java/checks/ClassVariableVisibilityCheck.java | 100 / 58 / 42 (42.000%) | 100 / 95 / 5 (5.000%) | 100 / 58 / 42 (42.000%) | 100 / 95 / 5 (5.000%) |
| java-checks/src/main/java/org/sonar/java/checks/ClassWithOnlyStaticMethodsInstantiationCheck.java | 165 / 78 / 87 (52.727%) | 165 / 162 / 3 (1.818%) | 165 / 78 / 87 (52.727%) | 165 / 162 / 3 (1.818%) |
| java-checks/src/main/java/org/sonar/java/checks/ClassWithoutHashCodeInHashStructureCheck.java | 95 / 40 / 55 (57.895%) | 95 / 93 / 2 (2.105%) | 95 / 40 / 55 (57.895%) | 95 / 93 / 2 (2.105%) |
| java-checks/src/main/java/org/sonar/java/checks/CloneMethodCallsSuperCloneCheck.java | 103 / 59 / 44 (42.718%) | 103 / 101 / 2 (1.942%) | 103 / 59 / 44 (42.718%) | 103 / 101 / 2 (1.942%) |
| java-checks/src/main/java/org/sonar/java/checks/CloneOverrideCheck.java | 72 / 36 / 36 (50.000%) | 72 / 70 / 2 (2.778%) | 72 / 36 / 36 (50.000%) | 72 / 70 / 2 (2.778%) |
| java-checks/src/main/java/org/sonar/java/checks/CloneableImplementingCloneCheck.java | 70 / 30 / 40 (57.143%) | 70 / 68 / 2 (2.857%) | 70 / 30 / 40 (57.143%) | 70 / 68 / 2 (2.857%) |
| java-checks/src/main/java/org/sonar/java/checks/CognitiveComplexityMethodCheck.java | 77 / 43 / 34 (44.156%) | 77 / 71 / 6 (7.792%) | 77 / 43 / 34 (44.156%) | 77 / 71 / 6 (7.792%) |
| java-checks/src/main/java/org/sonar/java/checks/CollapsibleIfCandidateCheck.java | 151 / 70 / 81 (53.642%) | 151 / 149 / 2 (1.325%) | 151 / 70 / 81 (53.642%) | 151 / 149 / 2 (1.325%) |
| java-checks/src/main/java/org/sonar/java/checks/CollectInsteadOfForeachCheck.java | 194 / 97 / 97 (50.000%) | 194 / 191 / 3 (1.546%) | 194 / 97 / 97 (50.000%) | 194 / 191 / 3 (1.546%) |
| java-checks/src/main/java/org/sonar/java/checks/CollectionCallingItselfCheck.java | 111 / 58 / 53 (47.748%) | 111 / 109 / 2 (1.802%) | 111 / 58 / 53 (47.748%) | 111 / 109 / 2 (1.802%) |
| java-checks/src/main/java/org/sonar/java/checks/CollectionConstructorReferenceCheck.java | 49 / 25 / 24 (48.980%) | 49 / 47 / 2 (4.082%) | 49 / 25 / 24 (48.980%) | 49 / 47 / 2 (4.082%) |
| java-checks/src/main/java/org/sonar/java/checks/CollectionImplementationReferencedCheck.java | 534 / 344 / 190 (35.581%) | 534 / 486 / 48 (8.989%) | 534 / 344 / 190 (35.581%) | 534 / 486 / 48 (8.989%) |
| java-checks/src/main/java/org/sonar/java/checks/CollectionInappropriateCallsCheck.java | 505 / 376 / 129 (25.545%) | 505 / 503 / 2 (0.396%) | 505 / 376 / 129 (25.545%) | 505 / 503 / 2 (0.396%) |
| java-checks/src/main/java/org/sonar/java/checks/CollectionIsEmptyCheck.java | 373 / 197 / 176 (47.185%) | 373 / 369 / 4 (1.072%) | 373 / 197 / 176 (47.185%) | 373 / 369 / 4 (1.072%) |
| java-checks/src/main/java/org/sonar/java/checks/CollectionMethodsWithLinearComplexityCheck.java | 341 / 167 / 174 (51.026%) | 341 / 321 / 20 (5.865%) | 341 / 167 / 174 (51.026%) | 341 / 321 / 20 (5.865%) |
| java-checks/src/main/java/org/sonar/java/checks/CollectionSizeAndArrayLengthCheck.java | 216 / 109 / 107 (49.537%) | 216 / 214 / 2 (0.926%) | 216 / 109 / 107 (49.537%) | 216 / 214 / 2 (0.926%) |
| java-checks/src/main/java/org/sonar/java/checks/CollectionsEmptyConstantsCheck.java | 69 / 43 / 26 (37.681%) | 69 / 61 / 8 (11.594%) | 69 / 43 / 26 (37.681%) | 69 / 61 / 8 (11.594%) |
| java-checks/src/main/java/org/sonar/java/checks/CollectionsSortCheck.java | 126 / 49 / 77 (61.111%) | 126 / 124 / 2 (1.587%) | 126 / 49 / 77 (61.111%) | 126 / 124 / 2 (1.587%) |
| java-checks/src/main/java/org/sonar/java/checks/CollectorsToListCheck.java | 255 / 116 / 139 (54.510%) | 255 / 252 / 3 (1.176%) | 255 / 116 / 139 (54.510%) | 255 / 252 / 3 (1.176%) |
| java-checks/src/main/java/org/sonar/java/checks/CombineCatchCheck.java | 252 / 117 / 135 (53.571%) | 252 / 250 / 2 (0.794%) | 252 / 117 / 135 (53.571%) | 252 / 250 / 2 (0.794%) |
| java-checks/src/main/java/org/sonar/java/checks/CommentContainsPatternChecker.java | 98 / 82 / 16 (16.327%) | 98 / 89 / 9 (9.184%) | 98 / 82 / 16 (16.327%) | 98 / 89 / 9 (9.184%) |
| java-checks/src/main/java/org/sonar/java/checks/CommentRegularExpressionCheck.java | 77 / 51 / 26 (33.766%) | 77 / 65 / 12 (15.584%) | 77 / 51 / 26 (33.766%) | 77 / 65 / 12 (15.584%) |
| java-checks/src/main/java/org/sonar/java/checks/CommentedOutCodeLineCheck.java | 273 / 180 / 93 (34.066%) | 273 / 258 / 15 (5.495%) | 273 / 180 / 93 (34.066%) | 273 / 258 / 15 (5.495%) |
| java-checks/src/main/java/org/sonar/java/checks/CommentsMustStartWithCorrectNumberOfSlashesCheck.java | 215 / 147 / 68 (31.628%) | 215 / 213 / 2 (0.930%) | 215 / 147 / 68 (31.628%) | 215 / 213 / 2 (0.930%) |
| java-checks/src/main/java/org/sonar/java/checks/CompareObjectWithEqualsCheck.java | 247 / 150 / 97 (39.271%) | 247 / 245 / 2 (0.810%) | 247 / 150 / 97 (39.271%) | 247 / 245 / 2 (0.810%) |
| java-checks/src/main/java/org/sonar/java/checks/CompareStringsBoxedTypesWithEqualsCheck.java | 234 / 103 / 131 (55.983%) | 234 / 232 / 2 (0.855%) | 234 / 103 / 131 (55.983%) | 234 / 232 / 2 (0.855%) |
| java-checks/src/main/java/org/sonar/java/checks/CompareToNotOverloadedCheck.java | 76 / 28 / 48 (63.158%) | 76 / 74 / 2 (2.632%) | 76 / 28 / 48 (63.158%) | 76 / 74 / 2 (2.632%) |
| java-checks/src/main/java/org/sonar/java/checks/CompareToResultTestCheck.java | 300 / 155 / 145 (48.333%) | 300 / 298 / 2 (0.667%) | 300 / 155 / 145 (48.333%) | 300 / 298 / 2 (0.667%) |
| java-checks/src/main/java/org/sonar/java/checks/CompareToReturnValueCheck.java | 113 / 64 / 49 (43.363%) | 113 / 111 / 2 (1.770%) | 113 / 64 / 49 (43.363%) | 113 / 111 / 2 (1.770%) |
| java-checks/src/main/java/org/sonar/java/checks/CompareWithEqualsVisitor.java | 94 / 58 / 36 (38.298%) | 94 / 94 / 0 (0.000%) | 94 / 58 / 36 (38.298%) | 94 / 94 / 0 (0.000%) |
| java-checks/src/main/java/org/sonar/java/checks/ConcatenationWithStringValueOfCheck.java | 180 / 84 / 96 (53.333%) | 180 / 178 / 2 (1.111%) | 180 / 84 / 96 (53.333%) | 180 / 178 / 2 (1.111%) |
| java-checks/src/main/java/org/sonar/java/checks/ConditionalOnNewLineCheck.java | 79 / 45 / 34 (43.038%) | 79 / 77 / 2 (2.532%) | 79 / 45 / 34 (43.038%) | 79 / 77 / 2 (2.532%) |
| java-checks/src/main/java/org/sonar/java/checks/ConditionalRuleCacheUtils.java | 268 / 267 / 1 (0.373%) | 268 / 267 / 1 (0.373%) | 268 / 267 / 1 (0.373%) | 268 / 267 / 1 (0.373%) |
| java-checks/src/main/java/org/sonar/java/checks/ConfigurationBeanNamesCheck.java | 54 / 30 / 24 (44.444%) | 54 / 52 / 2 (3.704%) | 54 / 30 / 24 (44.444%) | 54 / 52 / 2 (3.704%) |
| java-checks/src/main/java/org/sonar/java/checks/ConfusingOverloadCheck.java | 250 / 140 / 110 (44.000%) | 250 / 246 / 4 (1.600%) | 250 / 140 / 110 (44.000%) | 250 / 246 / 4 (1.600%) |
| java-checks/src/main/java/org/sonar/java/checks/ConfusingVarargCheck.java | 195 / 86 / 109 (55.897%) | 195 / 193 / 2 (1.026%) | 195 / 86 / 109 (55.897%) | 195 / 193 / 2 (1.026%) |
| java-checks/src/main/java/org/sonar/java/checks/ConstantMathCheck.java | 368 / 187 / 181 (49.185%) | 368 / 365 / 3 (0.815%) | 368 / 187 / 181 (49.185%) | 368 / 365 / 3 (0.815%) |
| java-checks/src/main/java/org/sonar/java/checks/ConstantMethodCheck.java | 125 / 54 / 71 (56.800%) | 125 / 122 / 3 (2.400%) | 125 / 54 / 71 (56.800%) | 125 / 122 / 3 (2.400%) |
| java-checks/src/main/java/org/sonar/java/checks/ConstantsShouldBeStaticFinalCheck.java | 191 / 102 / 89 (46.597%) | 191 / 189 / 2 (1.047%) | 191 / 102 / 89 (46.597%) | 191 / 189 / 2 (1.047%) |
| java-checks/src/main/java/org/sonar/java/checks/ConstructorCallingOverridableCheck.java | 169 / 96 / 73 (43.195%) | 169 / 167 / 2 (1.183%) | 169 / 96 / 73 (43.195%) | 169 / 167 / 2 (1.183%) |
| java-checks/src/main/java/org/sonar/java/checks/ConstructorInjectionCheck.java | 82 / 27 / 55 (67.073%) | 82 / 80 / 2 (2.439%) | 82 / 27 / 55 (67.073%) | 82 / 80 / 2 (2.439%) |
| java-checks/src/main/java/org/sonar/java/checks/ConstructorsShouldNotAccessUninitializedValuesCheck.java | 328 / 171 / 157 (47.866%) | 328 / 326 / 2 (0.610%) | 328 / 171 / 157 (47.866%) | 328 / 326 / 2 (0.610%) |
| java-checks/src/main/java/org/sonar/java/checks/ContinueInLoopCheck.java | 24 / 13 / 11 (45.833%) | 24 / 22 / 2 (8.333%) | 24 / 13 / 11 (45.833%) | 24 / 22 / 2 (8.333%) |
| java-checks/src/main/java/org/sonar/java/checks/ControlCharacterInLiteralCheck.java | 92 / 61 / 31 (33.696%) | 92 / 86 / 6 (6.522%) | 92 / 61 / 31 (33.696%) | 92 / 86 / 6 (6.522%) |
| java-checks/src/main/java/org/sonar/java/checks/CopyConstructorMissesFieldCheck.java | 591 / 311 / 280 (47.377%) | 591 / 589 / 2 (0.338%) | 591 / 311 / 280 (47.377%) | 591 / 589 / 2 (0.338%) |
| java-checks/src/main/java/org/sonar/java/checks/CounterModeIVShouldNotBeReusedCheck.java | 223 / 109 / 114 (51.121%) | 223 / 221 / 2 (0.897%) | 223 / 109 / 114 (51.121%) | 223 / 221 / 2 (0.897%) |
| java-checks/src/main/java/org/sonar/java/checks/CredentialsProviderUnremovableCheck.java | 68 / 43 / 25 (36.765%) | 68 / 66 / 2 (2.941%) | 68 / 43 / 25 (36.765%) | 68 / 66 / 2 (2.941%) |
| java-checks/src/main/java/org/sonar/java/checks/CustomCryptographicAlgorithmCheck.java | 53 / 31 / 22 (41.509%) | 53 / 51 / 2 (3.774%) | 53 / 31 / 22 (41.509%) | 53 / 51 / 2 (3.774%) |
| java-checks/src/main/java/org/sonar/java/checks/DanglingElseStatementsCheck.java | 49 / 27 / 22 (44.898%) | 49 / 47 / 2 (4.082%) | 49 / 27 / 22 (44.898%) | 49 / 47 / 2 (4.082%) |
| java-checks/src/main/java/org/sonar/java/checks/DanglingJavadocCheck.java | 29 / 16 / 13 (44.828%) | 29 / 27 / 2 (6.897%) | 29 / 16 / 13 (44.828%) | 29 / 27 / 2 (6.897%) |
| java-checks/src/main/java/org/sonar/java/checks/DateAndTimesCheck.java | 129 / 80 / 49 (37.984%) | 129 / 127 / 2 (1.550%) | 129 / 80 / 49 (37.984%) | 129 / 127 / 2 (1.550%) |
| java-checks/src/main/java/org/sonar/java/checks/DateEnumsCheck.java | 803 / 453 / 350 (43.587%) | 803 / 758 / 45 (5.604%) | 803 / 453 / 350 (43.587%) | 803 / 758 / 45 (5.604%) |
| java-checks/src/main/java/org/sonar/java/checks/DateFormatWeekYearCheck.java | 183 / 111 / 72 (39.344%) | 183 / 179 / 4 (2.186%) | 183 / 111 / 72 (39.344%) | 183 / 179 / 4 (2.186%) |
| java-checks/src/main/java/org/sonar/java/checks/DateTimeConversionsCheck.java | 129 / 76 / 53 (41.085%) | 129 / 127 / 2 (1.550%) | 129 / 76 / 53 (41.085%) | 129 / 127 / 2 (1.550%) |
| java-checks/src/main/java/org/sonar/java/checks/DateTimeDurationCheck.java | 114 / 69 / 45 (39.474%) | 114 / 112 / 2 (1.754%) | 114 / 69 / 45 (39.474%) | 114 / 112 / 2 (1.754%) |
| java-checks/src/main/java/org/sonar/java/checks/DateTimeFormatterMismatchCheck.java | 268 / 168 / 100 (37.313%) | 268 / 266 / 2 (0.746%) | 268 / 168 / 100 (37.313%) | 268 / 266 / 2 (0.746%) |
| java-checks/src/main/java/org/sonar/java/checks/DateUtilsTruncateCheck.java | 51 / 18 / 33 (64.706%) | 51 / 47 / 4 (7.843%) | 51 / 18 / 33 (64.706%) | 51 / 47 / 4 (7.843%) |
| java-checks/src/main/java/org/sonar/java/checks/DeadStoreCheck.java | 600 / 310 / 290 (48.333%) | 600 / 598 / 2 (0.333%) | 600 / 310 / 290 (48.333%) | 600 / 598 / 2 (0.333%) |
| java-checks/src/main/java/org/sonar/java/checks/DefaultEncodingUsageCheck.java | 517 / 293 / 224 (43.327%) | 517 / 515 / 2 (0.387%) | 517 / 293 / 224 (43.327%) | 517 / 515 / 2 (0.387%) |
| java-checks/src/main/java/org/sonar/java/checks/DefaultFinisherInGathererFactoryCheck.java | 97 / 34 / 63 (64.948%) | 97 / 95 / 2 (2.062%) | 97 / 34 / 63 (64.948%) | 97 / 95 / 2 (2.062%) |
| java-checks/src/main/java/org/sonar/java/checks/DefaultInitializedFieldCheck.java | 162 / 62 / 100 (61.728%) | 162 / 160 / 2 (1.235%) | 162 / 62 / 100 (61.728%) | 162 / 160 / 2 (1.235%) |
| java-checks/src/main/java/org/sonar/java/checks/DefaultPackageCheck.java | 43 / 23 / 20 (46.512%) | 43 / 41 / 2 (4.651%) | 43 / 23 / 20 (46.512%) | 43 / 41 / 2 (4.651%) |
| java-checks/src/main/java/org/sonar/java/checks/DeprecatedArgumentsCheck.java | 35 / 22 / 13 (37.143%) | 35 / 32 / 3 (8.571%) | 35 / 22 / 13 (37.143%) | 35 / 32 / 3 (8.571%) |
| java-checks/src/main/java/org/sonar/java/checks/DeprecatedTagPresenceCheck.java | 50 / 33 / 17 (34.000%) | 50 / 48 / 2 (4.000%) | 50 / 33 / 17 (34.000%) | 50 / 48 / 2 (4.000%) |
| java-checks/src/main/java/org/sonar/java/checks/DepthOfInheritanceTreeCheck.java | 156 / 92 / 64 (41.026%) | 156 / 133 / 23 (14.744%) | 156 / 92 / 64 (41.026%) | 156 / 133 / 23 (14.744%) |
| java-checks/src/main/java/org/sonar/java/checks/DiamondOperatorCheck.java | 367 / 184 / 183 (49.864%) | 367 / 360 / 7 (1.907%) | 367 / 184 / 183 (49.864%) | 367 / 360 / 7 (1.907%) |
| java-checks/src/main/java/org/sonar/java/checks/DisallowedClassCheck.java | 237 / 146 / 91 (38.397%) | 237 / 231 / 6 (2.532%) | 237 / 146 / 91 (38.397%) | 237 / 231 / 6 (2.532%) |
| java-checks/src/main/java/org/sonar/java/checks/DisallowedConstructorCheck.java | 96 / 61 / 35 (36.458%) | 96 / 80 / 16 (16.667%) | 96 / 61 / 35 (36.458%) | 96 / 80 / 16 (16.667%) |
| java-checks/src/main/java/org/sonar/java/checks/DisallowedMethodCheck.java | 124 / 78 / 46 (37.097%) | 124 / 103 / 21 (16.935%) | 124 / 78 / 46 (37.097%) | 124 / 103 / 21 (16.935%) |
| java-checks/src/main/java/org/sonar/java/checks/DisallowedThreadGroupCheck.java | 123 / 84 / 39 (31.707%) | 123 / 121 / 2 (1.626%) | 123 / 84 / 39 (31.707%) | 123 / 121 / 2 (1.626%) |
| java-checks/src/main/java/org/sonar/java/checks/DoubleBraceInitializationCheck.java | 43 / 20 / 23 (53.488%) | 43 / 41 / 2 (4.651%) | 43 / 20 / 23 (53.488%) | 43 / 41 / 2 (4.651%) |
| java-checks/src/main/java/org/sonar/java/checks/DoubleCheckedLockingAssignmentCheck.java | 301 / 148 / 153 (50.831%) | 301 / 295 / 6 (1.993%) | 301 / 148 / 153 (50.831%) | 301 / 295 / 6 (1.993%) |
| java-checks/src/main/java/org/sonar/java/checks/DoublePrefixOperatorCheck.java | 116 / 47 / 69 (59.483%) | 116 / 114 / 2 (1.724%) | 116 / 47 / 69 (59.483%) | 116 / 114 / 2 (1.724%) |
| java-checks/src/main/java/org/sonar/java/checks/DuplicateConditionIfElseIfCheck.java | 120 / 64 / 56 (46.667%) | 120 / 118 / 2 (1.667%) | 120 / 64 / 56 (46.667%) | 120 / 118 / 2 (1.667%) |
| java-checks/src/main/java/org/sonar/java/checks/DuplicateImmutableCollectionArgumentsCheck.java | 359 / 188 / 171 (47.632%) | 359 / 357 / 2 (0.557%) | 359 / 188 / 171 (47.632%) | 359 / 357 / 2 (0.557%) |
| java-checks/src/main/java/org/sonar/java/checks/DurationGetTemporalUnitCheck.java | 108 / 66 / 42 (38.889%) | 108 / 106 / 2 (1.852%) | 108 / 66 / 42 (38.889%) | 108 / 106 / 2 (1.852%) |
| java-checks/src/main/java/org/sonar/java/checks/DurationTimeUnitAgreementCheck.java | 277 / 127 / 150 (54.152%) | 277 / 275 / 2 (0.722%) | 277 / 127 / 150 (54.152%) | 277 / 275 / 2 (0.722%) |
| java-checks/src/main/java/org/sonar/java/checks/DynamicClassLoadCheck.java | 42 / 15 / 27 (64.286%) | 42 / 40 / 2 (4.762%) | 42 / 15 / 27 (64.286%) | 42 / 40 / 2 (4.762%) |
| java-checks/src/main/java/org/sonar/java/checks/EmptyArchiveEntryCheck.java | 406 / 204 / 202 (49.754%) | 406 / 400 / 6 (1.478%) | 406 / 204 / 202 (49.754%) | 406 / 400 / 6 (1.478%) |
| java-checks/src/main/java/org/sonar/java/checks/EmptyBlockCheck.java | 130 / 51 / 79 (60.769%) | 130 / 125 / 5 (3.846%) | 130 / 51 / 79 (60.769%) | 130 / 125 / 5 (3.846%) |
| java-checks/src/main/java/org/sonar/java/checks/EmptyClassCheck.java | 68 / 31 / 37 (54.412%) | 68 / 66 / 2 (2.941%) | 68 / 31 / 37 (54.412%) | 68 / 66 / 2 (2.941%) |
| java-checks/src/main/java/org/sonar/java/checks/EmptyFileCheck.java | 31 / 16 / 15 (48.387%) | 31 / 26 / 5 (16.129%) | 31 / 16 / 15 (48.387%) | 31 / 26 / 5 (16.129%) |
| java-checks/src/main/java/org/sonar/java/checks/EmptyMethodsCheck.java | 287 / 129 / 158 (55.052%) | 287 / 285 / 2 (0.697%) | 287 / 129 / 158 (55.052%) | 287 / 285 / 2 (0.697%) |
| java-checks/src/main/java/org/sonar/java/checks/EmptyStatementUsageCheck.java | 130 / 54 / 76 (58.462%) | 130 / 125 / 5 (3.846%) | 130 / 54 / 76 (58.462%) | 130 / 125 / 5 (3.846%) |
| java-checks/src/main/java/org/sonar/java/checks/EntityManagerMergeUnusedResultCheck.java | 67 / 26 / 41 (61.194%) | 67 / 65 / 2 (2.985%) | 67 / 26 / 41 (61.194%) | 67 / 65 / 2 (2.985%) |
| java-checks/src/main/java/org/sonar/java/checks/EnumEqualCheck.java | 49 / 21 / 28 (57.143%) | 49 / 47 / 2 (4.082%) | 49 / 21 / 28 (57.143%) | 49 / 47 / 2 (4.082%) |
| java-checks/src/main/java/org/sonar/java/checks/EnumMapCheck.java | 172 / 90 / 82 (47.674%) | 172 / 170 / 2 (1.163%) | 172 / 90 / 82 (47.674%) | 172 / 170 / 2 (1.163%) |
| java-checks/src/main/java/org/sonar/java/checks/EnumMutableFieldCheck.java | 170 / 78 / 92 (54.118%) | 170 / 168 / 2 (1.176%) | 170 / 78 / 92 (54.118%) | 170 / 168 / 2 (1.176%) |
| java-checks/src/main/java/org/sonar/java/checks/EnumSetCheck.java | 118 / 45 / 73 (61.864%) | 118 / 116 / 2 (1.695%) | 118 / 45 / 73 (61.864%) | 118 / 116 / 2 (1.695%) |
| java-checks/src/main/java/org/sonar/java/checks/EqualsArgumentTypeCheck.java | 311 / 174 / 137 (44.051%) | 311 / 309 / 2 (0.643%) | 311 / 174 / 137 (44.051%) | 311 / 309 / 2 (0.643%) |
| java-checks/src/main/java/org/sonar/java/checks/EqualsMismatchedMembersCheck.java | 642 / 435 / 207 (32.243%) | 642 / 640 / 2 (0.312%) | 642 / 435 / 207 (32.243%) | 642 / 640 / 2 (0.312%) |
| java-checks/src/main/java/org/sonar/java/checks/EqualsNotOverriddenInSubclassCheck.java | 179 / 92 / 87 (48.603%) | 179 / 177 / 2 (1.117%) | 179 / 92 / 87 (48.603%) | 179 / 177 / 2 (1.117%) |
| java-checks/src/main/java/org/sonar/java/checks/EqualsNotOverriddenWithCompareToCheck.java | 104 / 57 / 47 (45.192%) | 104 / 102 / 2 (1.923%) | 104 / 57 / 47 (45.192%) | 104 / 102 / 2 (1.923%) |
| java-checks/src/main/java/org/sonar/java/checks/EqualsOnAtomicClassCheck.java | 25 / 11 / 14 (56.000%) | 25 / 23 / 2 (8.000%) | 25 / 11 / 14 (56.000%) | 25 / 23 / 2 (8.000%) |
| java-checks/src/main/java/org/sonar/java/checks/EqualsOverriddenWithHashCodeCheck.java | 131 / 66 / 65 (49.618%) | 131 / 129 / 2 (1.527%) | 131 / 66 / 65 (49.618%) | 131 / 129 / 2 (1.527%) |
| java-checks/src/main/java/org/sonar/java/checks/EqualsParametersMarkedNonNullCheck.java | 82 / 32 / 50 (60.976%) | 82 / 80 / 2 (2.439%) | 82 / 32 / 50 (60.976%) | 82 / 80 / 2 (2.439%) |
| java-checks/src/main/java/org/sonar/java/checks/ErrorClassExtendedCheck.java | 33 / 17 / 16 (48.485%) | 33 / 31 / 2 (6.061%) | 33 / 17 / 16 (48.485%) | 33 / 31 / 2 (6.061%) |
| java-checks/src/main/java/org/sonar/java/checks/EscapedUnicodeCharactersCheck.java | 111 / 89 / 22 (19.820%) | 111 / 107 / 4 (3.604%) | 111 / 89 / 22 (19.820%) | 111 / 107 / 4 (3.604%) |
| java-checks/src/main/java/org/sonar/java/checks/ExceptionsShouldBeImmutableCheck.java | 67 / 32 / 35 (52.239%) | 67 / 65 / 2 (2.985%) | 67 / 32 / 35 (52.239%) | 67 / 65 / 2 (2.985%) |
| java-checks/src/main/java/org/sonar/java/checks/ExpressionComplexityCheck.java | 330 / 106 / 224 (67.879%) | 330 / 323 / 7 (2.121%) | 330 / 106 / 224 (67.879%) | 330 / 323 / 7 (2.121%) |
| java-checks/src/main/java/org/sonar/java/checks/FieldModifierCheck.java | 107 / 37 / 70 (65.421%) | 107 / 105 / 2 (1.869%) | 107 / 37 / 70 (65.421%) | 107 / 105 / 2 (1.869%) |
| java-checks/src/main/java/org/sonar/java/checks/FileHeaderCheck.java | 133 / 111 / 22 (16.541%) | 133 / 122 / 11 (8.271%) | 133 / 111 / 22 (16.541%) | 133 / 122 / 11 (8.271%) |
| java-checks/src/main/java/org/sonar/java/checks/FilesExistsJDK8Check.java | 83 / 51 / 32 (38.554%) | 83 / 74 / 9 (10.843%) | 83 / 51 / 32 (38.554%) | 83 / 74 / 9 (10.843%) |
| java-checks/src/main/java/org/sonar/java/checks/FinalClassCheck.java | 125 / 73 / 52 (41.600%) | 125 / 122 / 3 (2.400%) | 125 / 73 / 52 (41.600%) | 125 / 122 / 3 (2.400%) |
| java-checks/src/main/java/org/sonar/java/checks/FinalizeFieldsSetCheck.java | 129 / 71 / 58 (44.961%) | 129 / 127 / 2 (1.550%) | 129 / 71 / 58 (44.961%) | 129 / 127 / 2 (1.550%) |
| java-checks/src/main/java/org/sonar/java/checks/FinalizerAttackCheck.java | 531 / 254 / 277 (52.166%) | 531 / 529 / 2 (0.377%) | 531 / 254 / 277 (52.166%) | 531 / 529 / 2 (0.377%) |
| java-checks/src/main/java/org/sonar/java/checks/FixmeTagPresenceCheck.java | 35 / 22 / 13 (37.143%) | 35 / 33 / 2 (5.714%) | 35 / 22 / 13 (37.143%) | 35 / 33 / 2 (5.714%) |
| java-checks/src/main/java/org/sonar/java/checks/FlexibleConstructorBodyValidationCheck.java | 195 / 107 / 88 (45.128%) | 195 / 193 / 2 (1.026%) | 195 / 107 / 88 (45.128%) | 195 / 193 / 2 (1.026%) |
| java-checks/src/main/java/org/sonar/java/checks/FlexibleConstructorVisitor.java | 85 / 50 / 35 (41.176%) | 85 / 85 / 0 (0.000%) | 85 / 50 / 35 (41.176%) | 85 / 85 / 0 (0.000%) |
| java-checks/src/main/java/org/sonar/java/checks/FloatEqualityCheck.java | 207 / 76 / 131 (63.285%) | 207 / 205 / 2 (0.966%) | 207 / 76 / 131 (63.285%) | 207 / 205 / 2 (0.966%) |
| java-checks/src/main/java/org/sonar/java/checks/FloatingPointComparisonCheck.java | 364 / 154 / 210 (57.692%) | 364 / 362 / 2 (0.549%) | 364 / 154 / 210 (57.692%) | 364 / 362 / 2 (0.549%) |
| java-checks/src/main/java/org/sonar/java/checks/ForLoopCounterChangedCheck.java | 149 / 74 / 75 (50.336%) | 149 / 144 / 5 (3.356%) | 149 / 74 / 75 (50.336%) | 149 / 144 / 5 (3.356%) |
| java-checks/src/main/java/org/sonar/java/checks/ForLoopFalseConditionCheck.java | 170 / 105 / 65 (38.235%) | 170 / 167 / 3 (1.765%) | 170 / 105 / 65 (38.235%) | 170 / 167 / 3 (1.765%) |
| java-checks/src/main/java/org/sonar/java/checks/ForLoopIncrementAndUpdateCheck.java | 401 / 191 / 210 (52.369%) | 401 / 399 / 2 (0.499%) | 401 / 191 / 210 (52.369%) | 401 / 399 / 2 (0.499%) |
| java-checks/src/main/java/org/sonar/java/checks/ForLoopIncrementSignCheck.java | 113 / 63 / 50 (44.248%) | 113 / 111 / 2 (1.770%) | 113 / 63 / 50 (44.248%) | 113 / 111 / 2 (1.770%) |
| java-checks/src/main/java/org/sonar/java/checks/ForLoopStreamSuggestionCheck.java | 416 / 191 / 225 (54.087%) | 416 / 414 / 2 (0.481%) | 416 / 191 / 225 (54.087%) | 416 / 414 / 2 (0.481%) |
| java-checks/src/main/java/org/sonar/java/checks/ForLoopTerminationConditionCheck.java | 223 / 147 / 76 (34.081%) | 223 / 221 / 2 (0.897%) | 223 / 147 / 76 (34.081%) | 223 / 221 / 2 (0.897%) |
| java-checks/src/main/java/org/sonar/java/checks/ForLoopUsedAsWhileLoopCheck.java | 39 / 19 / 20 (51.282%) | 39 / 37 / 2 (5.128%) | 39 / 19 / 20 (51.282%) | 39 / 37 / 2 (5.128%) |
| java-checks/src/main/java/org/sonar/java/checks/ForLoopVariableTypeCheck.java | 170 / 88 / 82 (48.235%) | 170 / 167 / 3 (1.765%) | 170 / 88 / 82 (48.235%) | 170 / 167 / 3 (1.765%) |
| java-checks/src/main/java/org/sonar/java/checks/ForStatelessGatherersOmitInitializerCheck.java | 236 / 138 / 98 (41.525%) | 236 / 234 / 2 (0.847%) | 236 / 138 / 98 (41.525%) | 236 / 234 / 2 (0.847%) |
| java-checks/src/main/java/org/sonar/java/checks/GarbageCollectorCalledCheck.java | 41 / 18 / 23 (56.098%) | 41 / 39 / 2 (4.878%) | 41 / 18 / 23 (56.098%) | 41 / 39 / 2 (4.878%) |
| java-checks/src/main/java/org/sonar/java/checks/GetClassLoaderCheck.java | 25 / 11 / 14 (56.000%) | 25 / 23 / 2 (8.000%) | 25 / 11 / 14 (56.000%) | 25 / 23 / 2 (8.000%) |
| java-checks/src/main/java/org/sonar/java/checks/GetRequestedSessionIdCheck.java | 25 / 11 / 14 (56.000%) | 25 / 23 / 2 (8.000%) | 25 / 11 / 14 (56.000%) | 25 / 23 / 2 (8.000%) |
| java-checks/src/main/java/org/sonar/java/checks/GettersSettersOnRightFieldCheck.java | 237 / 70 / 167 (70.464%) | 237 / 235 / 2 (0.844%) | 237 / 70 / 167 (70.464%) | 237 / 235 / 2 (0.844%) |
| java-checks/src/main/java/org/sonar/java/checks/HardCodedPasswordCheck.java | 209 / 123 / 86 (41.148%) | 209 / 203 / 6 (2.871%) | 209 / 123 / 86 (41.148%) | 209 / 203 / 6 (2.871%) |
| java-checks/src/main/java/org/sonar/java/checks/HardCodedSecretCheck.java | 147 / 82 / 65 (44.218%) | 147 / 137 / 10 (6.803%) | 147 / 82 / 65 (44.218%) | 147 / 137 / 10 (6.803%) |
| java-checks/src/main/java/org/sonar/java/checks/HardcodedIpCheck.java | 256 / 237 / 19 (7.422%) | 256 / 254 / 2 (0.781%) | 256 / 237 / 19 (7.422%) | 256 / 254 / 2 (0.781%) |
| java-checks/src/main/java/org/sonar/java/checks/HardcodedMathConstantCheck.java | 198 / 181 / 17 (8.586%) | 198 / 196 / 2 (1.010%) | 198 / 181 / 17 (8.586%) | 198 / 196 / 2 (1.010%) |
| java-checks/src/main/java/org/sonar/java/checks/HardcodedURICheck.java | 450 / 292 / 158 (35.111%) | 450 / 445 / 5 (1.111%) | 450 / 292 / 158 (35.111%) | 450 / 445 / 5 (1.111%) |
| java-checks/src/main/java/org/sonar/java/checks/HasNextCallingNextCheck.java | 131 / 80 / 51 (38.931%) | 131 / 129 / 2 (1.527%) | 131 / 80 / 51 (38.931%) | 131 / 129 / 2 (1.527%) |
| java-checks/src/main/java/org/sonar/java/checks/HashCodeMismatchedFieldsCheck.java | 542 / 325 / 217 (40.037%) | 542 / 540 / 2 (0.369%) | 542 / 325 / 217 (40.037%) | 542 / 540 / 2 (0.369%) |
| java-checks/src/main/java/org/sonar/java/checks/HiddenFieldCheck.java | 347 / 169 / 178 (51.297%) | 347 / 332 / 15 (4.323%) | 347 / 169 / 178 (51.297%) | 347 / 332 / 15 (4.323%) |
| java-checks/src/main/java/org/sonar/java/checks/IdenticalCasesInSwitchCheck.java | 416 / 209 / 207 (49.760%) | 416 / 414 / 2 (0.481%) | 416 / 209 / 207 (49.760%) | 416 / 414 / 2 (0.481%) |
| java-checks/src/main/java/org/sonar/java/checks/IdenticalOperandOnBinaryExpressionCheck.java | 338 / 126 / 212 (62.722%) | 338 / 333 / 5 (1.479%) | 338 / 126 / 212 (62.722%) | 338 / 333 / 5 (1.479%) |
| java-checks/src/main/java/org/sonar/java/checks/IdentityHashMapBoxedKeyCheck.java | 88 / 47 / 41 (46.591%) | 88 / 86 / 2 (2.273%) | 88 / 47 / 41 (46.591%) | 88 / 86 / 2 (2.273%) |
| java-checks/src/main/java/org/sonar/java/checks/IfElseIfStatementEndsWithElseCheck.java | 47 / 23 / 24 (51.064%) | 47 / 45 / 2 (4.255%) | 47 / 23 / 24 (51.064%) | 47 / 45 / 2 (4.255%) |
| java-checks/src/main/java/org/sonar/java/checks/IgnoredOperationStatusCheck.java | 151 / 33 / 118 (78.146%) | 151 / 149 / 2 (1.325%) | 151 / 33 / 118 (78.146%) | 151 / 149 / 2 (1.325%) |
| java-checks/src/main/java/org/sonar/java/checks/IgnoredReturnValueCheck.java | 331 / 150 / 181 (54.683%) | 331 / 329 / 2 (0.604%) | 331 / 150 / 181 (54.683%) | 331 / 329 / 2 (0.604%) |
| java-checks/src/main/java/org/sonar/java/checks/IgnoredStreamReturnValueCheck.java | 62 / 22 / 40 (64.516%) | 62 / 60 / 2 (3.226%) | 62 / 22 / 40 (64.516%) | 62 / 60 / 2 (3.226%) |
| java-checks/src/main/java/org/sonar/java/checks/ImmediateReverseBoxingCheck.java | 551 / 271 / 280 (50.817%) | 551 / 538 / 13 (2.359%) | 551 / 271 / 280 (50.817%) | 551 / 538 / 13 (2.359%) |
| java-checks/src/main/java/org/sonar/java/checks/ImmediatelyReturnedVariableCheck.java | 182 / 85 / 97 (53.297%) | 182 / 171 / 11 (6.044%) | 182 / 85 / 97 (53.297%) | 182 / 171 / 11 (6.044%) |
| java-checks/src/main/java/org/sonar/java/checks/ImplementsEnumerationCheck.java | 75 / 32 / 43 (57.333%) | 75 / 72 / 3 (4.000%) | 75 / 32 / 43 (57.333%) | 75 / 72 / 3 (4.000%) |
| java-checks/src/main/java/org/sonar/java/checks/ImportDeclarationOrderCheck.java | 276 / 180 / 96 (34.783%) | 276 / 274 / 2 (0.725%) | 276 / 180 / 96 (34.783%) | 276 / 274 / 2 (0.725%) |
| java-checks/src/main/java/org/sonar/java/checks/InappropriateRegexpCheck.java | 66 / 31 / 35 (53.030%) | 66 / 64 / 2 (3.030%) | 66 / 31 / 35 (53.030%) | 66 / 64 / 2 (3.030%) |
| java-checks/src/main/java/org/sonar/java/checks/IncDecOnFloatingPointCheck.java | 81 / 24 / 57 (70.370%) | 81 / 79 / 2 (2.469%) | 81 / 24 / 57 (70.370%) | 81 / 79 / 2 (2.469%) |
| java-checks/src/main/java/org/sonar/java/checks/IncompatibleBitMaskCheck.java | 286 / 184 / 102 (35.664%) | 286 / 281 / 5 (1.748%) | 286 / 184 / 102 (35.664%) | 286 / 281 / 5 (1.748%) |
| java-checks/src/main/java/org/sonar/java/checks/IncorrectOrderOfMembersCheck.java | 93 / 55 / 38 (40.860%) | 93 / 91 / 2 (2.151%) | 93 / 55 / 38 (40.860%) | 93 / 91 / 2 (2.151%) |
| java-checks/src/main/java/org/sonar/java/checks/IncrementDecrementInSubExpressionCheck.java | 97 / 58 / 39 (40.206%) | 97 / 95 / 2 (2.062%) | 97 / 58 / 39 (40.206%) | 97 / 95 / 2 (2.062%) |
| java-checks/src/main/java/org/sonar/java/checks/IndentationAfterConditionalCheck.java | 198 / 104 / 94 (47.475%) | 198 / 196 / 2 (1.010%) | 198 / 104 / 94 (47.475%) | 198 / 196 / 2 (1.010%) |
| java-checks/src/main/java/org/sonar/java/checks/IndentationCheck.java | 543 / 347 / 196 (36.096%) | 543 / 528 / 15 (2.762%) | 543 / 347 / 196 (36.096%) | 543 / 528 / 15 (2.762%) |
| java-checks/src/main/java/org/sonar/java/checks/IndexOfWithPositiveNumberCheck.java | 126 / 60 / 66 (52.381%) | 126 / 123 / 3 (2.381%) | 126 / 60 / 66 (52.381%) | 126 / 123 / 3 (2.381%) |
| java-checks/src/main/java/org/sonar/java/checks/InitializeSubclassFieldsBeforeSuperCheck.java | 344 / 196 / 148 (43.023%) | 344 / 340 / 4 (1.163%) | 344 / 196 / 148 (43.023%) | 344 / 340 / 4 (1.163%) |
| java-checks/src/main/java/org/sonar/java/checks/InnerClassInInterfaceCheck.java | 45 / 20 / 25 (55.556%) | 45 / 43 / 2 (4.444%) | 45 / 20 / 25 (55.556%) | 45 / 43 / 2 (4.444%) |
| java-checks/src/main/java/org/sonar/java/checks/InnerClassOfNonSerializableCheck.java | 14 / 9 / 5 (35.714%) | 14 / 12 / 2 (14.286%) | 14 / 9 / 5 (35.714%) | 14 / 12 / 2 (14.286%) |
| java-checks/src/main/java/org/sonar/java/checks/InnerClassOfSerializableCheck.java | 14 / 9 / 5 (35.714%) | 14 / 12 / 2 (14.286%) | 14 / 9 / 5 (35.714%) | 14 / 12 / 2 (14.286%) |
| java-checks/src/main/java/org/sonar/java/checks/InnerClassShadowFieldCheck.java | 146 / 85 / 61 (41.781%) | 146 / 144 / 2 (1.370%) | 146 / 85 / 61 (41.781%) | 146 / 144 / 2 (1.370%) |
| java-checks/src/main/java/org/sonar/java/checks/InnerClassTooManyLinesCheck.java | 65 / 34 / 31 (47.692%) | 65 / 59 / 6 (9.231%) | 65 / 34 / 31 (47.692%) | 65 / 59 / 6 (9.231%) |
| java-checks/src/main/java/org/sonar/java/checks/InnerStaticClassesCheck.java | 291 / 171 / 120 (41.237%) | 291 / 289 / 2 (0.687%) | 291 / 171 / 120 (41.237%) | 291 / 289 / 2 (0.687%) |
| java-checks/src/main/java/org/sonar/java/checks/InputStreamOverrideReadCheck.java | 109 / 43 / 66 (60.550%) | 109 / 107 / 2 (1.835%) | 109 / 43 / 66 (60.550%) | 109 / 107 / 2 (1.835%) |
| java-checks/src/main/java/org/sonar/java/checks/InputStreamReadCheck.java | 65 / 34 / 31 (47.692%) | 65 / 63 / 2 (3.077%) | 65 / 34 / 31 (47.692%) | 65 / 63 / 2 (3.077%) |
| java-checks/src/main/java/org/sonar/java/checks/InsecureCreateTempFileCheck.java | 14 / 8 / 6 (42.857%) | 14 / 12 / 2 (14.286%) | 14 / 8 / 6 (42.857%) | 14 / 12 / 2 (14.286%) |
| java-checks/src/main/java/org/sonar/java/checks/InstanceOfPatternMatchingCheck.java | 348 / 192 / 156 (44.828%) | 348 / 344 / 4 (1.149%) | 348 / 192 / 156 (44.828%) | 348 / 344 / 4 (1.149%) |
| java-checks/src/main/java/org/sonar/java/checks/InstanceofUsedOnExceptionCheck.java | 165 / 64 / 101 (61.212%) | 165 / 163 / 2 (1.212%) | 165 / 64 / 101 (61.212%) | 165 / 163 / 2 (1.212%) |
| java-checks/src/main/java/org/sonar/java/checks/IntegerOverflowCheck.java | 600 / 277 / 323 (53.833%) | 600 / 596 / 4 (0.667%) | 600 / 277 / 323 (53.833%) | 600 / 596 / 4 (0.667%) |
| java-checks/src/main/java/org/sonar/java/checks/IntegerSubtractionInComparisonCheck.java | 217 / 105 / 112 (51.613%) | 217 / 215 / 2 (0.922%) | 217 / 105 / 112 (51.613%) | 217 / 215 / 2 (0.922%) |
| java-checks/src/main/java/org/sonar/java/checks/IntegerToLongTimestampCastCheck.java | 163 / 55 / 108 (66.258%) | 163 / 161 / 2 (1.227%) | 163 / 55 / 108 (66.258%) | 163 / 161 / 2 (1.227%) |
| java-checks/src/main/java/org/sonar/java/checks/InterfaceAsConstantContainerCheck.java | 79 / 34 / 45 (56.962%) | 79 / 77 / 2 (2.532%) | 79 / 34 / 45 (56.962%) | 79 / 77 / 2 (2.532%) |
| java-checks/src/main/java/org/sonar/java/checks/InterfaceOrSuperclassShadowingCheck.java | 109 / 53 / 56 (51.376%) | 109 / 106 / 3 (2.752%) | 109 / 53 / 56 (51.376%) | 109 / 106 / 3 (2.752%) |
| java-checks/src/main/java/org/sonar/java/checks/InterruptedExceptionCheck.java | 341 / 203 / 138 (40.469%) | 341 / 338 / 3 (0.880%) | 341 / 203 / 138 (40.469%) | 341 / 338 / 3 (0.880%) |
| java-checks/src/main/java/org/sonar/java/checks/InvalidComparatorMethodReferenceCheck.java | 80 / 42 / 38 (47.500%) | 80 / 78 / 2 (2.500%) | 80 / 42 / 38 (47.500%) | 80 / 78 / 2 (2.500%) |
| java-checks/src/main/java/org/sonar/java/checks/InvalidDateValuesCheck.java | 470 / 310 / 160 (34.043%) | 470 / 465 / 5 (1.064%) | 470 / 310 / 160 (34.043%) | 470 / 465 / 5 (1.064%) |
| java-checks/src/main/java/org/sonar/java/checks/IsInstanceMethodCheck.java | 113 / 55 / 58 (51.327%) | 113 / 111 / 2 (1.770%) | 113 / 55 / 58 (51.327%) | 113 / 111 / 2 (1.770%) |
| java-checks/src/main/java/org/sonar/java/checks/IterableIteratorCheck.java | 111 / 45 / 66 (59.459%) | 111 / 109 / 2 (1.802%) | 111 / 45 / 66 (59.459%) | 111 / 109 / 2 (1.802%) |
| java-checks/src/main/java/org/sonar/java/checks/IteratorNextExceptionCheck.java | 169 / 88 / 81 (47.929%) | 169 / 167 / 2 (1.183%) | 169 / 88 / 81 (47.929%) | 169 / 167 / 2 (1.183%) |
| java-checks/src/main/java/org/sonar/java/checks/JEEThreadCheck.java | 195 / 104 / 91 (46.667%) | 195 / 193 / 2 (1.026%) | 195 / 104 / 91 (46.667%) | 195 / 193 / 2 (1.026%) |
| java-checks/src/main/java/org/sonar/java/checks/JacksonDeserializationCheck.java | 172 / 78 / 94 (54.651%) | 172 / 170 / 2 (1.163%) | 172 / 78 / 94 (54.651%) | 172 / 170 / 2 (1.163%) |
| java-checks/src/main/java/org/sonar/java/checks/JavaFootprint.java | 34 / 17 / 17 (50.000%) | 34 / 17 / 17 (50.000%) | 34 / 17 / 17 (50.000%) | 34 / 17 / 17 (50.000%) |
| java-checks/src/main/java/org/sonar/java/checks/JdbcDriverExplicitLoadingCheck.java | 50 / 19 / 31 (62.000%) | 50 / 46 / 4 (8.000%) | 50 / 19 / 31 (62.000%) | 50 / 46 / 4 (8.000%) |
| java-checks/src/main/java/org/sonar/java/checks/JpaEagerFetchTypeCheck.java | 93 / 46 / 47 (50.538%) | 93 / 90 / 3 (3.226%) | 93 / 46 / 47 (50.538%) | 93 / 90 / 3 (3.226%) |
| java-checks/src/main/java/org/sonar/java/checks/JpaEntityFinalCheck.java | 101 / 52 / 49 (48.515%) | 101 / 99 / 2 (1.980%) | 101 / 52 / 49 (48.515%) | 101 / 99 / 2 (1.980%) |
| java-checks/src/main/java/org/sonar/java/checks/KeySetInsteadOfEntrySetCheck.java | 193 / 92 / 101 (52.332%) | 193 / 187 / 6 (3.109%) | 193 / 92 / 101 (52.332%) | 193 / 187 / 6 (3.109%) |
| java-checks/src/main/java/org/sonar/java/checks/KnownCapacityHashBasedCollectionCheck.java | 96 / 50 / 46 (47.917%) | 96 / 94 / 2 (2.083%) | 96 / 50 / 46 (47.917%) | 96 / 94 / 2 (2.083%) |
| java-checks/src/main/java/org/sonar/java/checks/LabelsShouldNotBeUsedCheck.java | 29 / 13 / 16 (55.172%) | 29 / 24 / 5 (17.241%) | 29 / 13 / 16 (55.172%) | 29 / 24 / 5 (17.241%) |
| java-checks/src/main/java/org/sonar/java/checks/LambdaOptionalParenthesisCheck.java | 63 / 30 / 33 (52.381%) | 63 / 61 / 2 (3.175%) | 63 / 30 / 33 (52.381%) | 63 / 61 / 2 (3.175%) |
| java-checks/src/main/java/org/sonar/java/checks/LambdaSingleExpressionCheck.java | 210 / 96 / 114 (54.286%) | 210 / 208 / 2 (0.952%) | 210 / 96 / 114 (54.286%) | 210 / 208 / 2 (0.952%) |
| java-checks/src/main/java/org/sonar/java/checks/LambdaTooBigCheck.java | 73 / 46 / 27 (36.986%) | 73 / 67 / 6 (8.219%) | 73 / 46 / 27 (36.986%) | 73 / 67 / 6 (8.219%) |
| java-checks/src/main/java/org/sonar/java/checks/LambdaTypeParameterCheck.java | 72 / 28 / 44 (61.111%) | 72 / 68 / 4 (5.556%) | 72 / 28 / 44 (61.111%) | 72 / 68 / 4 (5.556%) |
| java-checks/src/main/java/org/sonar/java/checks/LazyArgEvaluationCheck.java | 492 / 279 / 213 (43.293%) | 492 / 490 / 2 (0.407%) | 492 / 279 / 213 (43.293%) | 492 / 490 / 2 (0.407%) |
| java-checks/src/main/java/org/sonar/java/checks/LeastSpecificTypeCheck.java | 560 / 259 / 301 (53.750%) | 560 / 558 / 2 (0.357%) | 560 / 259 / 301 (53.750%) | 560 / 558 / 2 (0.357%) |
| java-checks/src/main/java/org/sonar/java/checks/LeftCurlyBraceBaseTreeVisitor.java | 424 / 250 / 174 (41.038%) | 424 / 417 / 7 (1.651%) | 424 / 250 / 174 (41.038%) | 424 / 417 / 7 (1.651%) |
| java-checks/src/main/java/org/sonar/java/checks/LeftCurlyBraceEndLineCheck.java | 26 / 13 / 13 (50.000%) | 26 / 21 / 5 (19.231%) | 26 / 13 / 13 (50.000%) | 26 / 21 / 5 (19.231%) |
| java-checks/src/main/java/org/sonar/java/checks/LeftCurlyBraceStartLineCheck.java | 26 / 13 / 13 (50.000%) | 26 / 21 / 5 (19.231%) | 26 / 13 / 13 (50.000%) | 26 / 21 / 5 (19.231%) |
| java-checks/src/main/java/org/sonar/java/checks/LocalVariablesShouldNotSpanSwitchCaseGroupsCheck.java | 152 / 92 / 60 (39.474%) | 152 / 150 / 2 (1.316%) | 152 / 92 / 60 (39.474%) | 152 / 150 / 2 (1.316%) |
| java-checks/src/main/java/org/sonar/java/checks/LoggedRethrownExceptionsCheck.java | 201 / 107 / 94 (46.766%) | 201 / 199 / 2 (0.995%) | 201 / 107 / 94 (46.766%) | 201 / 199 / 2 (0.995%) |
| java-checks/src/main/java/org/sonar/java/checks/LoggerClassCheck.java | 183 / 65 / 118 (64.481%) | 183 / 180 / 3 (1.639%) | 183 / 65 / 118 (64.481%) | 183 / 180 / 3 (1.639%) |
| java-checks/src/main/java/org/sonar/java/checks/LoggersDeclarationCheck.java | 164 / 104 / 60 (36.585%) | 164 / 158 / 6 (3.659%) | 164 / 104 / 60 (36.585%) | 164 / 158 / 6 (3.659%) |
| java-checks/src/main/java/org/sonar/java/checks/LongBitsToDoubleOnIntCheck.java | 37 / 16 / 21 (56.757%) | 37 / 35 / 2 (5.405%) | 37 / 16 / 21 (56.757%) | 37 / 35 / 2 (5.405%) |
| java-checks/src/main/java/org/sonar/java/checks/LoopExecutingAtMostOnceCheck.java | 348 / 145 / 203 (58.333%) | 348 / 346 / 2 (0.575%) | 348 / 145 / 203 (58.333%) | 348 / 346 / 2 (0.575%) |
| java-checks/src/main/java/org/sonar/java/checks/LoopsOnSameSetCheck.java | 164 / 95 / 69 (42.073%) | 164 / 161 / 3 (1.829%) | 164 / 95 / 69 (42.073%) | 164 / 161 / 3 (1.829%) |
| java-checks/src/main/java/org/sonar/java/checks/MagicNumberCheck.java | 159 / 107 / 52 (32.704%) | 159 / 153 / 6 (3.774%) | 159 / 107 / 52 (32.704%) | 159 / 153 / 6 (3.774%) |
| java-checks/src/main/java/org/sonar/java/checks/MainMethodSignatureCheck.java | 41 / 21 / 20 (48.780%) | 41 / 39 / 2 (4.878%) | 41 / 21 / 20 (48.780%) | 41 / 39 / 2 (4.878%) |
| java-checks/src/main/java/org/sonar/java/checks/MainMethodThrowsExceptionCheck.java | 37 / 17 / 20 (54.054%) | 37 / 35 / 2 (5.405%) | 37 / 17 / 20 (54.054%) | 37 / 35 / 2 (5.405%) |
| java-checks/src/main/java/org/sonar/java/checks/MapKeyNotComparableCheck.java | 81 / 38 / 43 (53.086%) | 81 / 79 / 2 (2.469%) | 81 / 38 / 43 (53.086%) | 81 / 79 / 2 (2.469%) |
| java-checks/src/main/java/org/sonar/java/checks/MapperWithoutDaoFactoryCheck.java | 127 / 63 / 64 (50.394%) | 127 / 125 / 2 (1.575%) | 127 / 63 / 64 (50.394%) | 127 / 125 / 2 (1.575%) |
| java-checks/src/main/java/org/sonar/java/checks/MarkdownJavadocSyntaxCheck.java | 256 / 196 / 60 (23.438%) | 256 / 254 / 2 (0.781%) | 256 / 196 / 60 (23.438%) | 256 / 254 / 2 (0.781%) |
| java-checks/src/main/java/org/sonar/java/checks/MathClampMethodsCheck.java | 292 / 164 / 128 (43.836%) | 292 / 289 / 3 (1.027%) | 292 / 164 / 128 (43.836%) | 292 / 289 / 3 (1.027%) |
| java-checks/src/main/java/org/sonar/java/checks/MathClampRangeCheck.java | 225 / 150 / 75 (33.333%) | 225 / 223 / 2 (0.889%) | 225 / 150 / 75 (33.333%) | 225 / 223 / 2 (0.889%) |
| java-checks/src/main/java/org/sonar/java/checks/MathOnFloatCheck.java | 69 / 32 / 37 (53.623%) | 69 / 67 / 2 (2.899%) | 69 / 32 / 37 (53.623%) | 69 / 67 / 2 (2.899%) |
| java-checks/src/main/java/org/sonar/java/checks/MembersDifferOnlyByCapitalizationCheck.java | 556 / 319 / 237 (42.626%) | 556 / 554 / 2 (0.360%) | 556 / 319 / 237 (42.626%) | 556 / 554 / 2 (0.360%) |
| java-checks/src/main/java/org/sonar/java/checks/MethodComplexityCheck.java | 94 / 46 / 48 (51.064%) | 94 / 85 / 9 (9.574%) | 94 / 46 / 48 (51.064%) | 94 / 85 / 9 (9.574%) |
| java-checks/src/main/java/org/sonar/java/checks/MethodIdenticalImplementationsCheck.java | 210 / 127 / 83 (39.524%) | 210 / 208 / 2 (0.952%) | 210 / 127 / 83 (39.524%) | 210 / 208 / 2 (0.952%) |
| java-checks/src/main/java/org/sonar/java/checks/MethodOnlyCallsSuperCheck.java | 313 / 146 / 167 (53.355%) | 313 / 311 / 2 (0.639%) | 313 / 146 / 167 (53.355%) | 313 / 311 / 2 (0.639%) |
| java-checks/src/main/java/org/sonar/java/checks/MethodParametersOrderCheck.java | 296 / 157 / 139 (46.959%) | 296 / 294 / 2 (0.676%) | 296 / 157 / 139 (46.959%) | 296 / 294 / 2 (0.676%) |
| java-checks/src/main/java/org/sonar/java/checks/MethodTooBigCheck.java | 56 / 30 / 26 (46.429%) | 56 / 51 / 5 (8.929%) | 56 / 30 / 26 (46.429%) | 56 / 51 / 5 (8.929%) |
| java-checks/src/main/java/org/sonar/java/checks/MethodWithExcessiveReturnsCheck.java | 163 / 80 / 83 (50.920%) | 163 / 158 / 5 (3.067%) | 163 / 80 / 83 (50.920%) | 163 / 158 / 5 (3.067%) |
| java-checks/src/main/java/org/sonar/java/checks/MismatchPackageDirectoryCheck.java | 128 / 107 / 21 (16.406%) | 128 / 124 / 4 (3.125%) | 128 / 107 / 21 (16.406%) | 128 / 124 / 4 (3.125%) |
| java-checks/src/main/java/org/sonar/java/checks/MissingBeanValidationCheck.java | 288 / 155 / 133 (46.181%) | 288 / 286 / 2 (0.694%) | 288 / 155 / 133 (46.181%) | 288 / 286 / 2 (0.694%) |
| java-checks/src/main/java/org/sonar/java/checks/MissingCurlyBracesCheck.java | 150 / 68 / 82 (54.667%) | 150 / 145 / 5 (3.333%) | 150 / 68 / 82 (54.667%) | 150 / 145 / 5 (3.333%) |
| java-checks/src/main/java/org/sonar/java/checks/MissingDeprecatedCheck.java | 52 / 26 / 26 (50.000%) | 52 / 46 / 6 (11.538%) | 52 / 26 / 26 (50.000%) | 52 / 46 / 6 (11.538%) |
| java-checks/src/main/java/org/sonar/java/checks/MissingNewLineAtEndOfFileCheck.java | 33 / 24 / 9 (27.273%) | 33 / 28 / 5 (15.152%) | 33 / 24 / 9 (27.273%) | 33 / 28 / 5 (15.152%) |
| java-checks/src/main/java/org/sonar/java/checks/MissingOverridesInRecordWithArrayComponentCheck.java | 175 / 98 / 77 (44.000%) | 175 / 173 / 2 (1.143%) | 175 / 98 / 77 (44.000%) | 175 / 173 / 2 (1.143%) |
| java-checks/src/main/java/org/sonar/java/checks/MissingPackageInfoCheck.java | 55 / 45 / 10 (18.182%) | 55 / 51 / 4 (7.273%) | 55 / 45 / 10 (18.182%) | 55 / 51 / 4 (7.273%) |
| java-checks/src/main/java/org/sonar/java/checks/ModifiersOrderCheck.java | 283 / 130 / 153 (54.064%) | 283 / 278 / 5 (1.767%) | 283 / 130 / 153 (54.064%) | 283 / 278 / 5 (1.767%) |
| java-checks/src/main/java/org/sonar/java/checks/ModulusEqualityCheck.java | 224 / 129 / 95 (42.411%) | 224 / 222 / 2 (0.893%) | 224 / 129 / 95 (42.411%) | 224 / 222 / 2 (0.893%) |
| java-checks/src/main/java/org/sonar/java/checks/MultilineBlocksCurlyBracesCheck.java | 186 / 115 / 71 (38.172%) | 186 / 184 / 2 (1.075%) | 186 / 115 / 71 (38.172%) | 186 / 184 / 2 (1.075%) |
| java-checks/src/main/java/org/sonar/java/checks/MultipleMainInstancesCheck.java | 138 / 64 / 74 (53.623%) | 138 / 136 / 2 (1.449%) | 138 / 64 / 74 (53.623%) | 138 / 136 / 2 (1.449%) |
| java-checks/src/main/java/org/sonar/java/checks/MutableMembersUsageCheck.java | 901 / 610 / 291 (32.297%) | 901 / 898 / 3 (0.333%) | 901 / 610 / 291 (32.297%) | 901 / 898 / 3 (0.333%) |
| java-checks/src/main/java/org/sonar/java/checks/NPEThrowCheck.java | 74 / 34 / 40 (54.054%) | 74 / 72 / 2 (2.703%) | 74 / 34 / 40 (54.054%) | 74 / 72 / 2 (2.703%) |
| java-checks/src/main/java/org/sonar/java/checks/NanEqualityCheck.java | 49 / 26 / 23 (46.939%) | 49 / 47 / 2 (4.082%) | 49 / 26 / 23 (46.939%) | 49 / 47 / 2 (4.082%) |
| java-checks/src/main/java/org/sonar/java/checks/NestedBlocksCheck.java | 88 / 48 / 40 (45.455%) | 88 / 86 / 2 (2.273%) | 88 / 48 / 40 (45.455%) | 88 / 86 / 2 (2.273%) |
| java-checks/src/main/java/org/sonar/java/checks/NestedEnumStaticCheck.java | 48 / 23 / 25 (52.083%) | 48 / 46 / 2 (4.167%) | 48 / 23 / 25 (52.083%) | 48 / 46 / 2 (4.167%) |
| java-checks/src/main/java/org/sonar/java/checks/NestedIfStatementsCheck.java | 236 / 143 / 93 (39.407%) | 236 / 231 / 5 (2.119%) | 236 / 143 / 93 (39.407%) | 236 / 231 / 5 (2.119%) |
| java-checks/src/main/java/org/sonar/java/checks/NestedSwitchCheck.java | 64 / 36 / 28 (43.750%) | 64 / 62 / 2 (3.125%) | 64 / 36 / 28 (43.750%) | 64 / 62 / 2 (3.125%) |
| java-checks/src/main/java/org/sonar/java/checks/NestedTernaryOperatorsCheck.java | 51 / 26 / 25 (49.020%) | 51 / 49 / 2 (3.922%) | 51 / 26 / 25 (49.020%) | 51 / 49 / 2 (3.922%) |
| java-checks/src/main/java/org/sonar/java/checks/NestedTryCatchCheck.java | 107 / 55 / 52 (48.598%) | 107 / 105 / 2 (1.869%) | 107 / 55 / 52 (48.598%) | 107 / 105 / 2 (1.869%) |
| java-checks/src/main/java/org/sonar/java/checks/NioFileDeleteCheck.java | 32 / 15 / 17 (53.125%) | 32 / 30 / 2 (6.250%) | 32 / 15 / 17 (53.125%) | 32 / 30 / 2 (6.250%) |
| java-checks/src/main/java/org/sonar/java/checks/NoCheckstyleTagPresenceCheck.java | 35 / 22 / 13 (37.143%) | 35 / 33 / 2 (5.714%) | 35 / 22 / 13 (37.143%) | 35 / 33 / 2 (5.714%) |
| java-checks/src/main/java/org/sonar/java/checks/NoPmdTagPresenceCheck.java | 35 / 22 / 13 (37.143%) | 35 / 33 / 2 (5.714%) | 35 / 22 / 13 (37.143%) | 35 / 33 / 2 (5.714%) |
| java-checks/src/main/java/org/sonar/java/checks/NoSonarCheck.java | 35 / 22 / 13 (37.143%) | 35 / 33 / 2 (5.714%) | 35 / 22 / 13 (37.143%) | 35 / 33 / 2 (5.714%) |
| java-checks/src/main/java/org/sonar/java/checks/NonShortCircuitLogicCheck.java | 91 / 58 / 33 (36.264%) | 91 / 84 / 7 (7.692%) | 91 / 58 / 33 (36.264%) | 91 / 84 / 7 (7.692%) |
| java-checks/src/main/java/org/sonar/java/checks/NonStaticClassInitializerCheck.java | 48 / 22 / 26 (54.167%) | 48 / 46 / 2 (4.167%) | 48 / 22 / 26 (54.167%) | 48 / 46 / 2 (4.167%) |
| java-checks/src/main/java/org/sonar/java/checks/NotifyCheck.java | 43 / 14 / 29 (67.442%) | 43 / 41 / 2 (4.651%) | 43 / 14 / 29 (67.442%) | 43 / 41 / 2 (4.651%) |
| java-checks/src/main/java/org/sonar/java/checks/NowWithoutParametersCheck.java | 45 / 25 / 20 (44.444%) | 45 / 43 / 2 (4.444%) | 45 / 25 / 20 (44.444%) | 45 / 43 / 2 (4.444%) |
| java-checks/src/main/java/org/sonar/java/checks/NullCheckWithInstanceofCheck.java | 214 / 97 / 117 (54.673%) | 214 / 209 / 5 (2.336%) | 214 / 97 / 117 (54.673%) | 214 / 209 / 5 (2.336%) |
| java-checks/src/main/java/org/sonar/java/checks/NullReturnedOnComputeIfPresentOrAbsentCheck.java | 87 / 35 / 52 (59.770%) | 87 / 85 / 2 (2.299%) | 87 / 35 / 52 (59.770%) | 87 / 85 / 2 (2.299%) |
| java-checks/src/main/java/org/sonar/java/checks/NullShouldNotBeUsedWithOptionalCheck.java | 252 / 154 / 98 (38.889%) | 252 / 248 / 4 (1.587%) | 252 / 154 / 98 (38.889%) | 252 / 248 / 4 (1.587%) |
| java-checks/src/main/java/org/sonar/java/checks/OSCommandsPathCheck.java | 315 / 170 / 145 (46.032%) | 315 / 313 / 2 (0.635%) | 315 / 170 / 145 (46.032%) | 315 / 313 / 2 (0.635%) |
| java-checks/src/main/java/org/sonar/java/checks/ObjectCreatedOnlyToCallGetClassCheck.java | 156 / 82 / 74 (47.436%) | 156 / 152 / 4 (2.564%) | 156 / 82 / 74 (47.436%) | 156 / 152 / 4 (2.564%) |
| java-checks/src/main/java/org/sonar/java/checks/ObjectFinalizeCheck.java | 93 / 45 / 48 (51.613%) | 93 / 88 / 5 (5.376%) | 93 / 45 / 48 (51.613%) | 93 / 88 / 5 (5.376%) |
| java-checks/src/main/java/org/sonar/java/checks/ObjectFinalizeOverloadedCheck.java | 70 / 39 / 31 (44.286%) | 70 / 68 / 2 (2.857%) | 70 / 39 / 31 (44.286%) | 70 / 68 / 2 (2.857%) |
| java-checks/src/main/java/org/sonar/java/checks/ObjectFinalizeOverriddenCallsSuperFinalizeCheck.java | 211 / 102 / 109 (51.659%) | 211 / 201 / 10 (4.739%) | 211 / 102 / 109 (51.659%) | 211 / 201 / 10 (4.739%) |
| java-checks/src/main/java/org/sonar/java/checks/ObjectFinalizeOverriddenCheck.java | 60 / 26 / 34 (56.667%) | 60 / 55 / 5 (8.333%) | 60 / 26 / 34 (56.667%) | 60 / 55 / 5 (8.333%) |
| java-checks/src/main/java/org/sonar/java/checks/ObjectFinalizeOverriddenNotPublicCheck.java | 52 / 27 / 25 (48.077%) | 52 / 50 / 2 (3.846%) | 52 / 27 / 25 (48.077%) | 52 / 50 / 2 (3.846%) |
| java-checks/src/main/java/org/sonar/java/checks/ObjectsEqualsCheck.java | 243 / 123 / 120 (49.383%) | 243 / 241 / 2 (0.823%) | 243 / 123 / 120 (49.383%) | 243 / 241 / 2 (0.823%) |
| java-checks/src/main/java/org/sonar/java/checks/OctalEscapeSequenceFollowedByDigitCheck.java | 122 / 101 / 21 (17.213%) | 122 / 120 / 2 (1.639%) | 122 / 101 / 21 (17.213%) | 122 / 120 / 2 (1.639%) |
| java-checks/src/main/java/org/sonar/java/checks/OctalValuesCheck.java | 62 / 47 / 15 (24.194%) | 62 / 60 / 2 (3.226%) | 62 / 47 / 15 (24.194%) | 62 / 60 / 2 (3.226%) |
| java-checks/src/main/java/org/sonar/java/checks/OmitPermittedTypesCheck.java | 118 / 50 / 68 (57.627%) | 118 / 116 / 2 (1.695%) | 118 / 50 / 68 (57.627%) | 118 / 116 / 2 (1.695%) |
| java-checks/src/main/java/org/sonar/java/checks/OneClassInterfacePerFileCheck.java | 32 / 19 / 13 (40.625%) | 32 / 30 / 2 (6.250%) | 32 / 19 / 13 (40.625%) | 32 / 30 / 2 (6.250%) |
| java-checks/src/main/java/org/sonar/java/checks/OneDeclarationPerLineCheck.java | 296 / 156 / 140 (47.297%) | 296 / 292 / 4 (1.351%) | 296 / 156 / 140 (47.297%) | 296 / 292 / 4 (1.351%) |
| java-checks/src/main/java/org/sonar/java/checks/OneToManyMappingCheck.java | 151 / 61 / 90 (59.603%) | 151 / 149 / 2 (1.325%) | 151 / 61 / 90 (59.603%) | 151 / 149 / 2 (1.325%) |
| java-checks/src/main/java/org/sonar/java/checks/OperatorPrecedenceCheck.java | 532 / 255 / 277 (52.068%) | 532 / 520 / 12 (2.256%) | 532 / 255 / 277 (52.068%) | 532 / 520 / 12 (2.256%) |
| java-checks/src/main/java/org/sonar/java/checks/OptionalAsParameterCheck.java | 119 / 75 / 44 (36.975%) | 119 / 117 / 2 (1.681%) | 119 / 75 / 44 (36.975%) | 119 / 117 / 2 (1.681%) |
| java-checks/src/main/java/org/sonar/java/checks/OptionalWrappingContainerCheck.java | 87 / 51 / 36 (41.379%) | 87 / 84 / 3 (3.448%) | 87 / 51 / 36 (41.379%) | 87 / 84 / 3 (3.448%) |
| java-checks/src/main/java/org/sonar/java/checks/OutputStreamOverrideWriteCheck.java | 114 / 49 / 65 (57.018%) | 114 / 112 / 2 (1.754%) | 114 / 49 / 65 (57.018%) | 114 / 112 / 2 (1.754%) |
| java-checks/src/main/java/org/sonar/java/checks/OverrideAnnotationCheck.java | 149 / 68 / 81 (54.362%) | 149 / 147 / 2 (1.342%) | 149 / 68 / 81 (54.362%) | 149 / 147 / 2 (1.342%) |
| java-checks/src/main/java/org/sonar/java/checks/OverwrittenKeyCheck.java | 386 / 217 / 169 (43.782%) | 386 / 379 / 7 (1.813%) | 386 / 217 / 169 (43.782%) | 386 / 379 / 7 (1.813%) |
| java-checks/src/main/java/org/sonar/java/checks/ParameterReassignedToCheck.java | 221 / 117 / 104 (47.059%) | 221 / 219 / 2 (0.905%) | 221 / 117 / 104 (47.059%) | 221 / 219 / 2 (0.905%) |
| java-checks/src/main/java/org/sonar/java/checks/ParsingErrorCheck.java | 46 / 33 / 13 (28.261%) | 46 / 39 / 7 (15.217%) | 46 / 33 / 13 (28.261%) | 46 / 39 / 7 (15.217%) |
| java-checks/src/main/java/org/sonar/java/checks/PatternMatchUsingIfCheck.java | 626 / 417 / 209 (33.387%) | 626 / 619 / 7 (1.118%) | 626 / 417 / 209 (33.387%) | 626 / 619 / 7 (1.118%) |
| java-checks/src/main/java/org/sonar/java/checks/PatternUtils.java | 33 / 25 / 8 (24.242%) | 33 / 25 / 8 (24.242%) | 33 / 25 / 8 (24.242%) | 33 / 25 / 8 (24.242%) |
| java-checks/src/main/java/org/sonar/java/checks/PersistenceAnnotationsMixedCheck.java | 156 / 61 / 95 (60.897%) | 156 / 154 / 2 (1.282%) | 156 / 61 / 95 (60.897%) | 156 / 154 / 2 (1.282%) |
| java-checks/src/main/java/org/sonar/java/checks/PopulateBeansCheck.java | 34 / 13 / 21 (61.765%) | 34 / 32 / 2 (5.882%) | 34 / 13 / 21 (61.765%) | 34 / 32 / 2 (5.882%) |
| java-checks/src/main/java/org/sonar/java/checks/PredictableSeedCheck.java | 143 / 52 / 91 (63.636%) | 143 / 141 / 2 (1.399%) | 143 / 52 / 91 (63.636%) | 143 / 141 / 2 (1.399%) |
| java-checks/src/main/java/org/sonar/java/checks/PreferStreamAnyMatchCheck.java | 209 / 76 / 133 (63.636%) | 209 / 207 / 2 (0.957%) | 209 / 76 / 133 (63.636%) | 209 / 207 / 2 (0.957%) |
| java-checks/src/main/java/org/sonar/java/checks/PreparedStatementAndResultSetCheck.java | 279 / 148 / 131 (46.953%) | 279 / 271 / 8 (2.867%) | 279 / 148 / 131 (46.953%) | 279 / 271 / 8 (2.867%) |
| java-checks/src/main/java/org/sonar/java/checks/PreparedStatementInsideLoopCheck.java | 243 / 131 / 112 (46.091%) | 243 / 241 / 2 (0.823%) | 243 / 131 / 112 (46.091%) | 243 / 241 / 2 (0.823%) |
| java-checks/src/main/java/org/sonar/java/checks/PreparedStatementLoopInvariantCheck.java | 289 / 183 / 106 (36.678%) | 289 / 287 / 2 (0.692%) | 289 / 183 / 106 (36.678%) | 289 / 287 / 2 (0.692%) |
| java-checks/src/main/java/org/sonar/java/checks/PresuperLogicBloatsConstructorCheck.java | 47 / 27 / 20 (42.553%) | 47 / 41 / 6 (12.766%) | 47 / 27 / 20 (42.553%) | 47 / 41 / 6 (12.766%) |
| java-checks/src/main/java/org/sonar/java/checks/PrimitiveTypeBoxingWithToStringCheck.java | 104 / 54 / 50 (48.077%) | 104 / 102 / 2 (1.923%) | 104 / 54 / 50 (48.077%) | 104 / 102 / 2 (1.923%) |
| java-checks/src/main/java/org/sonar/java/checks/PrimitiveWrappersInTernaryOperatorCheck.java | 52 / 29 / 23 (44.231%) | 52 / 50 / 2 (3.846%) | 52 / 29 / 23 (44.231%) | 52 / 50 / 2 (3.846%) |
| java-checks/src/main/java/org/sonar/java/checks/PrimitivesMarkedNullableCheck.java | 86 / 33 / 53 (61.628%) | 86 / 84 / 2 (2.326%) | 86 / 33 / 53 (61.628%) | 86 / 84 / 2 (2.326%) |
| java-checks/src/main/java/org/sonar/java/checks/PrintfFailCheck.java | 123 / 84 / 39 (31.707%) | 123 / 121 / 2 (1.626%) | 123 / 84 / 39 (31.707%) | 123 / 121 / 2 (1.626%) |
| java-checks/src/main/java/org/sonar/java/checks/PrintfMisuseCheck.java | 852 / 464 / 388 (45.540%) | 852 / 849 / 3 (0.352%) | 852 / 464 / 388 (45.540%) | 852 / 849 / 3 (0.352%) |
| java-checks/src/main/java/org/sonar/java/checks/PrivateFieldUsedLocallyCheck.java | 393 / 185 / 208 (52.926%) | 393 / 390 / 3 (0.763%) | 393 / 185 / 208 (52.926%) | 393 / 390 / 3 (0.763%) |
| java-checks/src/main/java/org/sonar/java/checks/ProtectedMemberInFinalClassCheck.java | 111 / 56 / 55 (49.550%) | 111 / 109 / 2 (1.802%) | 111 / 56 / 55 (49.550%) | 111 / 109 / 2 (1.802%) |
| java-checks/src/main/java/org/sonar/java/checks/PseudoRandomCheck.java | 228 / 136 / 92 (40.351%) | 228 / 222 / 6 (2.632%) | 228 / 136 / 92 (40.351%) | 228 / 222 / 6 (2.632%) |
| java-checks/src/main/java/org/sonar/java/checks/PublicConstructorInAbstractClassCheck.java | 99 / 24 / 75 (75.758%) | 99 / 97 / 2 (2.020%) | 99 / 24 / 75 (75.758%) | 99 / 97 / 2 (2.020%) |
| java-checks/src/main/java/org/sonar/java/checks/PublicStaticFieldShouldBeFinalCheck.java | 89 / 47 / 42 (47.191%) | 89 / 87 / 2 (2.247%) | 89 / 47 / 42 (47.191%) | 89 / 87 / 2 (2.247%) |
| java-checks/src/main/java/org/sonar/java/checks/PublicStaticMutableMembersCheck.java | 472 / 216 / 256 (54.237%) | 472 / 469 / 3 (0.636%) | 472 / 216 / 256 (54.237%) | 472 / 469 / 3 (0.636%) |
| java-checks/src/main/java/org/sonar/java/checks/QuarkusCacheResultOnVoidMethodCheck.java | 50 / 27 / 23 (46.000%) | 50 / 48 / 2 (4.000%) | 50 / 27 / 23 (46.000%) | 50 / 48 / 2 (4.000%) |
| java-checks/src/main/java/org/sonar/java/checks/QueryOnlyRequiredFieldsCheck.java | 106 / 40 / 66 (62.264%) | 106 / 104 / 2 (1.887%) | 106 / 40 / 66 (62.264%) | 106 / 104 / 2 (1.887%) |
| java-checks/src/main/java/org/sonar/java/checks/RandomFloatToIntCheck.java | 121 / 60 / 61 (50.413%) | 121 / 119 / 2 (1.653%) | 121 / 60 / 61 (50.413%) | 121 / 119 / 2 (1.653%) |
| java-checks/src/main/java/org/sonar/java/checks/RawByteBitwiseOperationsCheck.java | 117 / 61 / 56 (47.863%) | 117 / 115 / 2 (1.709%) | 117 / 61 / 56 (47.863%) | 117 / 115 / 2 (1.709%) |
| java-checks/src/main/java/org/sonar/java/checks/RawExceptionCheck.java | 215 / 134 / 81 (37.674%) | 215 / 210 / 5 (2.326%) | 215 / 134 / 81 (37.674%) | 215 / 210 / 5 (2.326%) |
| java-checks/src/main/java/org/sonar/java/checks/RawTypeCheck.java | 125 / 76 / 49 (39.200%) | 125 / 122 / 3 (2.400%) | 125 / 76 / 49 (39.200%) | 125 / 122 / 3 (2.400%) |
| java-checks/src/main/java/org/sonar/java/checks/ReadObjectSynchronizedCheck.java | 80 / 38 / 42 (52.500%) | 80 / 78 / 2 (2.500%) | 80 / 38 / 42 (52.500%) | 80 / 78 / 2 (2.500%) |
| java-checks/src/main/java/org/sonar/java/checks/ReadlnWithPromptCheck.java | 95 / 50 / 45 (47.368%) | 95 / 93 / 2 (2.105%) | 95 / 50 / 45 (47.368%) | 95 / 93 / 2 (2.105%) |
| java-checks/src/main/java/org/sonar/java/checks/RecordDuplicatedGetterCheck.java | 337 / 141 / 196 (58.160%) | 337 / 335 / 2 (0.593%) | 337 / 141 / 196 (58.160%) | 337 / 335 / 2 (0.593%) |
| java-checks/src/main/java/org/sonar/java/checks/RecordInsteadOfClassCheck.java | 664 / 359 / 305 (45.934%) | 664 / 662 / 2 (0.301%) | 664 / 359 / 305 (45.934%) | 664 / 662 / 2 (0.301%) |
| java-checks/src/main/java/org/sonar/java/checks/RecordPatternInsteadOfFieldAccessCheck.java | 196 / 101 / 95 (48.469%) | 196 / 194 / 2 (1.020%) | 196 / 101 / 95 (48.469%) | 196 / 194 / 2 (1.020%) |
| java-checks/src/main/java/org/sonar/java/checks/RedundantAbstractMethodCheck.java | 251 / 137 / 114 (45.418%) | 251 / 249 / 2 (0.797%) | 251 / 137 / 114 (45.418%) | 251 / 249 / 2 (0.797%) |
| java-checks/src/main/java/org/sonar/java/checks/RedundantCloseCheck.java | 136 / 58 / 78 (57.353%) | 136 / 134 / 2 (1.471%) | 136 / 58 / 78 (57.353%) | 136 / 134 / 2 (1.471%) |
| java-checks/src/main/java/org/sonar/java/checks/RedundantJumpCheck.java | 155 / 71 / 84 (54.194%) | 155 / 153 / 2 (1.290%) | 155 / 71 / 84 (54.194%) | 155 / 153 / 2 (1.290%) |
| java-checks/src/main/java/org/sonar/java/checks/RedundantModifierCheck.java | 197 / 91 / 106 (53.807%) | 197 / 195 / 2 (1.015%) | 197 / 91 / 106 (53.807%) | 197 / 195 / 2 (1.015%) |
| java-checks/src/main/java/org/sonar/java/checks/RedundantNullabilityAnnotationsCheck.java | 294 / 163 / 131 (44.558%) | 294 / 292 / 2 (0.680%) | 294 / 163 / 131 (44.558%) | 294 / 292 / 2 (0.680%) |
| java-checks/src/main/java/org/sonar/java/checks/RedundantRangeCheckCheck.java | 502 / 363 / 139 (27.689%) | 502 / 496 / 6 (1.195%) | 502 / 363 / 139 (27.689%) | 502 / 496 / 6 (1.195%) |
| java-checks/src/main/java/org/sonar/java/checks/RedundantRecordMethodsCheck.java | 476 / 311 / 165 (34.664%) | 476 / 474 / 2 (0.420%) | 476 / 311 / 165 (34.664%) | 476 / 474 / 2 (0.420%) |
| java-checks/src/main/java/org/sonar/java/checks/RedundantStreamCollectCheck.java | 137 / 48 / 89 (64.964%) | 137 / 124 / 13 (9.489%) | 137 / 48 / 89 (64.964%) | 137 / 124 / 13 (9.489%) |
| java-checks/src/main/java/org/sonar/java/checks/RedundantStringFormatCheck.java | 296 / 134 / 162 (54.730%) | 296 / 294 / 2 (0.676%) | 296 / 134 / 162 (54.730%) | 296 / 294 / 2 (0.676%) |
| java-checks/src/main/java/org/sonar/java/checks/RedundantThrowsDeclarationCheck.java | 688 / 353 / 335 (48.692%) | 688 / 679 / 9 (1.308%) | 688 / 353 / 335 (48.692%) | 688 / 679 / 9 (1.308%) |
| java-checks/src/main/java/org/sonar/java/checks/RedundantTypeCastCheck.java | 167 / 68 / 99 (59.281%) | 167 / 165 / 2 (1.198%) | 167 / 68 / 99 (59.281%) | 167 / 165 / 2 (1.198%) |
| java-checks/src/main/java/org/sonar/java/checks/ReflectionOnNonRuntimeAnnotationCheck.java | 85 / 44 / 41 (48.235%) | 85 / 82 / 3 (3.529%) | 85 / 44 / 41 (48.235%) | 85 / 82 / 3 (3.529%) |
| java-checks/src/main/java/org/sonar/java/checks/RegexPatternsNeedlesslyCheck.java | 168 / 96 / 72 (42.857%) | 168 / 164 / 4 (2.381%) | 168 / 96 / 72 (42.857%) | 168 / 164 / 4 (2.381%) |
| java-checks/src/main/java/org/sonar/java/checks/ReleaseSensorsCheck.java | 230 / 166 / 64 (27.826%) | 230 / 228 / 2 (0.870%) | 230 / 166 / 64 (27.826%) | 230 / 228 / 2 (0.870%) |
| java-checks/src/main/java/org/sonar/java/checks/RemoveTypeFromUnusedPatternCheck.java | 79 / 33 / 46 (58.228%) | 79 / 77 / 2 (2.532%) | 79 / 33 / 46 (58.228%) | 79 / 77 / 2 (2.532%) |
| java-checks/src/main/java/org/sonar/java/checks/RepeatAnnotationCheck.java | 164 / 86 / 78 (47.561%) | 164 / 162 / 2 (1.220%) | 164 / 86 / 78 (47.561%) | 164 / 162 / 2 (1.220%) |
| java-checks/src/main/java/org/sonar/java/checks/ReplaceGuavaWithJavaCheck.java | 195 / 100 / 95 (48.718%) | 195 / 180 / 15 (7.692%) | 195 / 100 / 95 (48.718%) | 195 / 180 / 15 (7.692%) |
| java-checks/src/main/java/org/sonar/java/checks/ReplaceLambdaByMethodRefCheck.java | 968 / 431 / 537 (55.475%) | 968 / 961 / 7 (0.723%) | 968 / 431 / 537 (55.475%) | 968 / 961 / 7 (0.723%) |
| java-checks/src/main/java/org/sonar/java/checks/ReplaceUnusedExceptionParameterWithUnnamedPatternCheck.java | 90 / 39 / 51 (56.667%) | 90 / 88 / 2 (2.222%) | 90 / 39 / 51 (56.667%) | 90 / 88 / 2 (2.222%) |
| java-checks/src/main/java/org/sonar/java/checks/RestDataPanacheResourceImplementationCheck.java | 67 / 42 / 25 (37.313%) | 67 / 65 / 2 (2.985%) | 67 / 42 / 25 (37.313%) | 67 / 65 / 2 (2.985%) |
| java-checks/src/main/java/org/sonar/java/checks/RestrictedIdentifiersUsageCheck.java | 72 / 42 / 30 (41.667%) | 72 / 68 / 4 (5.556%) | 72 / 42 / 30 (41.667%) | 72 / 68 / 4 (5.556%) |
| java-checks/src/main/java/org/sonar/java/checks/ResultSetIsLastCheck.java | 25 / 11 / 14 (56.000%) | 25 / 23 / 2 (8.000%) | 25 / 11 / 14 (56.000%) | 25 / 23 / 2 (8.000%) |
| java-checks/src/main/java/org/sonar/java/checks/ReturnEmptyArrayNotNullCheck.java | 422 / 289 / 133 (31.517%) | 422 / 418 / 4 (0.948%) | 422 / 289 / 133 (31.517%) | 422 / 418 / 4 (0.948%) |
| java-checks/src/main/java/org/sonar/java/checks/ReturnInFinallyCheck.java | 273 / 141 / 132 (48.352%) | 273 / 271 / 2 (0.733%) | 273 / 141 / 132 (48.352%) | 273 / 271 / 2 (0.733%) |
| java-checks/src/main/java/org/sonar/java/checks/ReturnOfBooleanExpressionsCheck.java | 288 / 148 / 140 (48.611%) | 288 / 284 / 4 (1.389%) | 288 / 148 / 140 (48.611%) | 288 / 284 / 4 (1.389%) |
| java-checks/src/main/java/org/sonar/java/checks/ReuseRandomCheck.java | 110 / 53 / 57 (51.818%) | 110 / 108 / 2 (1.818%) | 110 / 53 / 57 (51.818%) | 110 / 108 / 2 (1.818%) |
| java-checks/src/main/java/org/sonar/java/checks/ReverseSequencedCollectionCheck.java | 397 / 182 / 215 (54.156%) | 397 / 391 / 6 (1.511%) | 397 / 182 / 215 (54.156%) | 397 / 391 / 6 (1.511%) |
| java-checks/src/main/java/org/sonar/java/checks/ReversedMethodSequencedCollectionCheck.java | 110 / 55 / 55 (50.000%) | 110 / 108 / 2 (1.818%) | 110 / 55 / 55 (50.000%) | 110 / 108 / 2 (1.818%) |
| java-checks/src/main/java/org/sonar/java/checks/RightCurlyBraceDifferentLineAsNextBlockCheck.java | 28 / 13 / 15 (53.571%) | 28 / 23 / 5 (17.857%) | 28 / 13 / 15 (53.571%) | 28 / 23 / 5 (17.857%) |
| java-checks/src/main/java/org/sonar/java/checks/RightCurlyBraceSameLineAsNextBlockCheck.java | 45 / 14 / 31 (68.889%) | 45 / 40 / 5 (11.111%) | 45 / 14 / 31 (68.889%) | 45 / 40 / 5 (11.111%) |
| java-checks/src/main/java/org/sonar/java/checks/RightCurlyBraceStartLineCheck.java | 106 / 38 / 68 (64.151%) | 106 / 101 / 5 (4.717%) | 106 / 38 / 68 (64.151%) | 106 / 101 / 5 (4.717%) |
| java-checks/src/main/java/org/sonar/java/checks/RightCurlyBraceToNextBlockAbstractVisitor.java | 80 / 42 / 38 (47.500%) | 80 / 80 / 0 (0.000%) | 80 / 42 / 38 (47.500%) | 80 / 80 / 0 (0.000%) |
| java-checks/src/main/java/org/sonar/java/checks/RunFinalizersCheck.java | 38 / 17 / 21 (55.263%) | 38 / 36 / 2 (5.263%) | 38 / 17 / 21 (55.263%) | 38 / 36 / 2 (5.263%) |
| java-checks/src/main/java/org/sonar/java/checks/S9395Check.java | 310 / 178 / 132 (42.581%) | 310 / 307 / 3 (0.968%) | 310 / 178 / 132 (42.581%) | 310 / 307 / 3 (0.968%) |
| java-checks/src/main/java/org/sonar/java/checks/S9411Check.java | 436 / 225 / 211 (48.394%) | 436 / 430 / 6 (1.376%) | 436 / 225 / 211 (48.394%) | 436 / 430 / 6 (1.376%) |
| java-checks/src/main/java/org/sonar/java/checks/SQLInjectionCheck.java | 443 / 187 / 256 (57.788%) | 443 / 440 / 3 (0.677%) | 443 / 187 / 256 (57.788%) | 443 / 440 / 3 (0.677%) |
| java-checks/src/main/java/org/sonar/java/checks/ScheduledThreadPoolExecutorMaximumPoolSizeCheck.java | 25 / 11 / 14 (56.000%) | 25 / 23 / 2 (8.000%) | 25 / 11 / 14 (56.000%) | 25 / 23 / 2 (8.000%) |
| java-checks/src/main/java/org/sonar/java/checks/ScheduledThreadPoolExecutorZeroCheck.java | 57 / 25 / 32 (56.140%) | 57 / 55 / 2 (3.509%) | 57 / 25 / 32 (56.140%) | 57 / 55 / 2 (3.509%) |
| java-checks/src/main/java/org/sonar/java/checks/ScopedValueStableReferenceCheck.java | 101 / 54 / 47 (46.535%) | 101 / 99 / 2 (1.980%) | 101 / 54 / 47 (46.535%) | 101 / 99 / 2 (1.980%) |
| java-checks/src/main/java/org/sonar/java/checks/SelectorMethodArgumentCheck.java | 146 / 85 / 61 (41.781%) | 146 / 144 / 2 (1.370%) | 146 / 85 / 61 (41.781%) | 146 / 144 / 2 (1.370%) |
| java-checks/src/main/java/org/sonar/java/checks/SelfAssignmentCheck.java | 259 / 79 / 180 (69.498%) | 259 / 257 / 2 (0.772%) | 259 / 79 / 180 (69.498%) | 259 / 257 / 2 (0.772%) |
| java-checks/src/main/java/org/sonar/java/checks/ServletInstanceFieldCheck.java | 209 / 103 / 106 (50.718%) | 209 / 206 / 3 (1.435%) | 209 / 103 / 106 (50.718%) | 209 / 206 / 3 (1.435%) |
| java-checks/src/main/java/org/sonar/java/checks/ServletMethodsExceptionsThrownCheck.java | 281 / 136 / 145 (51.601%) | 281 / 279 / 2 (0.712%) | 281 / 136 / 145 (51.601%) | 281 / 279 / 2 (0.712%) |
| java-checks/src/main/java/org/sonar/java/checks/SeveralBreakOrContinuePerLoopCheck.java | 174 / 115 / 59 (33.908%) | 174 / 172 / 2 (1.149%) | 174 / 115 / 59 (33.908%) | 174 / 172 / 2 (1.149%) |
| java-checks/src/main/java/org/sonar/java/checks/ShiftOnIntOrLongCheck.java | 289 / 191 / 98 (33.910%) | 289 / 284 / 5 (1.730%) | 289 / 191 / 98 (33.910%) | 289 / 284 / 5 (1.730%) |
| java-checks/src/main/java/org/sonar/java/checks/SillyEqualsCheck.java | 208 / 111 / 97 (46.635%) | 208 / 204 / 4 (1.923%) | 208 / 111 / 97 (46.635%) | 208 / 204 / 4 (1.923%) |
| java-checks/src/main/java/org/sonar/java/checks/SillyStringOperationsCheck.java | 262 / 140 / 122 (46.565%) | 262 / 258 / 4 (1.527%) | 262 / 140 / 122 (46.565%) | 262 / 258 / 4 (1.527%) |
| java-checks/src/main/java/org/sonar/java/checks/SimpleClassNameCheck.java | 127 / 52 / 75 (59.055%) | 127 / 125 / 2 (1.575%) | 127 / 52 / 75 (59.055%) | 127 / 125 / 2 (1.575%) |
| java-checks/src/main/java/org/sonar/java/checks/SimpleStringLiteralForSingleLineStringsCheck.java | 38 / 24 / 14 (36.842%) | 38 / 36 / 2 (5.263%) | 38 / 24 / 14 (36.842%) | 38 / 36 / 2 (5.263%) |
| java-checks/src/main/java/org/sonar/java/checks/SimpleTemporalInstantiationCheck.java | 131 / 52 / 79 (60.305%) | 131 / 129 / 2 (1.527%) | 131 / 52 / 79 (60.305%) | 131 / 129 / 2 (1.527%) |
| java-checks/src/main/java/org/sonar/java/checks/SingleIfInsteadOfPatternMatchGuardCheck.java | 189 / 99 / 90 (47.619%) | 189 / 187 / 2 (1.058%) | 189 / 99 / 90 (47.619%) | 189 / 187 / 2 (1.058%) |
| java-checks/src/main/java/org/sonar/java/checks/SortedCollectionWithNonComparableTypeCheck.java | 372 / 194 / 178 (47.849%) | 372 / 370 / 2 (0.538%) | 372 / 194 / 178 (47.849%) | 372 / 370 / 2 (0.538%) |
| java-checks/src/main/java/org/sonar/java/checks/SpecializedFunctionalInterfacesCheck.java | 538 / 377 / 161 (29.926%) | 538 / 534 / 4 (0.743%) | 538 / 377 / 161 (29.926%) | 538 / 534 / 4 (0.743%) |
| java-checks/src/main/java/org/sonar/java/checks/StandardCharsetsConstantsCheck.java | 876 / 474 / 402 (45.890%) | 876 / 866 / 10 (1.142%) | 876 / 474 / 402 (45.890%) | 876 / 866 / 10 (1.142%) |
| java-checks/src/main/java/org/sonar/java/checks/StandardFunctionalInterfaceCheck.java | 483 / 360 / 123 (25.466%) | 483 / 480 / 3 (0.621%) | 483 / 360 / 123 (25.466%) | 483 / 480 / 3 (0.621%) |
| java-checks/src/main/java/org/sonar/java/checks/StartupAnnotationCheck.java | 102 / 42 / 60 (58.824%) | 102 / 100 / 2 (1.961%) | 102 / 42 / 60 (58.824%) | 102 / 100 / 2 (1.961%) |
| java-checks/src/main/java/org/sonar/java/checks/StatelessBeanInstanceFieldCheck.java | 88 / 50 / 38 (43.182%) | 88 / 86 / 2 (2.273%) | 88 / 50 / 38 (43.182%) | 88 / 86 / 2 (2.273%) |
| java-checks/src/main/java/org/sonar/java/checks/StaticFieldInitializationCheck.java | 219 / 121 / 98 (44.749%) | 219 / 217 / 2 (0.913%) | 219 / 121 / 98 (44.749%) | 219 / 217 / 2 (0.913%) |
| java-checks/src/main/java/org/sonar/java/checks/StaticFieldUpateCheck.java | 178 / 91 / 87 (48.876%) | 178 / 176 / 2 (1.124%) | 178 / 91 / 87 (48.876%) | 178 / 176 / 2 (1.124%) |
| java-checks/src/main/java/org/sonar/java/checks/StaticFieldUpdateInConstructorCheck.java | 160 / 73 / 87 (54.375%) | 160 / 157 / 3 (1.875%) | 160 / 73 / 87 (54.375%) | 160 / 157 / 3 (1.875%) |
| java-checks/src/main/java/org/sonar/java/checks/StaticImportCountCheck.java | 84 / 40 / 44 (52.381%) | 84 / 78 / 6 (7.143%) | 84 / 40 / 44 (52.381%) | 84 / 78 / 6 (7.143%) |
| java-checks/src/main/java/org/sonar/java/checks/StaticMemberAccessCheck.java | 176 / 88 / 88 (50.000%) | 176 / 174 / 2 (1.136%) | 176 / 88 / 88 (50.000%) | 176 / 174 / 2 (1.136%) |
| java-checks/src/main/java/org/sonar/java/checks/StaticMembersAccessCheck.java | 137 / 60 / 77 (56.204%) | 137 / 135 / 2 (1.460%) | 137 / 60 / 77 (56.204%) | 137 / 135 / 2 (1.460%) |
| java-checks/src/main/java/org/sonar/java/checks/StaticMethodCheck.java | 517 / 275 / 242 (46.809%) | 517 / 514 / 3 (0.580%) | 517 / 275 / 242 (46.809%) | 517 / 514 / 3 (0.580%) |
| java-checks/src/main/java/org/sonar/java/checks/StaticMethodHidingCheck.java | 193 / 98 / 95 (49.223%) | 193 / 191 / 2 (1.036%) | 193 / 98 / 95 (49.223%) | 193 / 191 / 2 (1.036%) |
| java-checks/src/main/java/org/sonar/java/checks/StaticMultithreadedUnsafeFieldsCheck.java | 162 / 81 / 81 (50.000%) | 162 / 159 / 3 (1.852%) | 162 / 81 / 81 (50.000%) | 162 / 159 / 3 (1.852%) |
| java-checks/src/main/java/org/sonar/java/checks/StreamForeachCheck.java | 95 / 38 / 57 (60.000%) | 95 / 93 / 2 (2.105%) | 95 / 38 / 57 (60.000%) | 95 / 93 / 2 (2.105%) |
| java-checks/src/main/java/org/sonar/java/checks/StreamPeekCheck.java | 25 / 11 / 14 (56.000%) | 25 / 23 / 2 (8.000%) | 25 / 11 / 14 (56.000%) | 25 / 23 / 2 (8.000%) |
| java-checks/src/main/java/org/sonar/java/checks/StreamReadResultCastCheck.java | 67 / 25 / 42 (62.687%) | 67 / 65 / 2 (2.985%) | 67 / 25 / 42 (62.687%) | 67 / 65 / 2 (2.985%) |
| java-checks/src/main/java/org/sonar/java/checks/StringBufferAndBuilderConcatenationCheck.java | 195 / 92 / 103 (52.821%) | 195 / 192 / 3 (1.538%) | 195 / 92 / 103 (52.821%) | 195 / 192 / 3 (1.538%) |
| java-checks/src/main/java/org/sonar/java/checks/StringBufferAndBuilderWithCharCheck.java | 84 / 43 / 41 (48.810%) | 84 / 80 / 4 (4.762%) | 84 / 43 / 41 (48.810%) | 84 / 80 / 4 (4.762%) |
| java-checks/src/main/java/org/sonar/java/checks/StringCallsBeyondBoundsCheck.java | 364 / 114 / 250 (68.681%) | 364 / 360 / 4 (1.099%) | 364 / 114 / 250 (68.681%) | 364 / 360 / 4 (1.099%) |
| java-checks/src/main/java/org/sonar/java/checks/StringConcatToTextBlockCheck.java | 127 / 90 / 37 (29.134%) | 127 / 125 / 2 (1.575%) | 127 / 90 / 37 (29.134%) | 127 / 125 / 2 (1.575%) |
| java-checks/src/main/java/org/sonar/java/checks/StringConcatenationInLoopCheck.java | 249 / 131 / 118 (47.390%) | 249 / 246 / 3 (1.205%) | 249 / 131 / 118 (47.390%) | 249 / 246 / 3 (1.205%) |
| java-checks/src/main/java/org/sonar/java/checks/StringEqualsCharSequenceCheck.java | 68 / 25 / 43 (63.235%) | 68 / 66 / 2 (2.941%) | 68 / 25 / 43 (63.235%) | 68 / 66 / 2 (2.941%) |
| java-checks/src/main/java/org/sonar/java/checks/StringFormatCheck.java | 227 / 174 / 53 (23.348%) | 227 / 225 / 2 (0.881%) | 227 / 174 / 53 (23.348%) | 227 / 225 / 2 (0.881%) |
| java-checks/src/main/java/org/sonar/java/checks/StringIndexOfRangesCheck.java | 306 / 202 / 104 (33.987%) | 306 / 304 / 2 (0.654%) | 306 / 202 / 104 (33.987%) | 306 / 304 / 2 (0.654%) |
| java-checks/src/main/java/org/sonar/java/checks/StringIsEmptyCheck.java | 310 / 168 / 142 (45.806%) | 310 / 307 / 3 (0.968%) | 310 / 168 / 142 (45.806%) | 310 / 307 / 3 (0.968%) |
| java-checks/src/main/java/org/sonar/java/checks/StringLiteralDuplicatedCheck.java | 435 / 174 / 261 (60.000%) | 435 / 416 / 19 (4.368%) | 435 / 174 / 261 (60.000%) | 435 / 416 / 19 (4.368%) |
| java-checks/src/main/java/org/sonar/java/checks/StringLiteralInsideEqualsCheck.java | 135 / 69 / 66 (48.889%) | 135 / 133 / 2 (1.481%) | 135 / 69 / 66 (48.889%) | 135 / 133 / 2 (1.481%) |
| java-checks/src/main/java/org/sonar/java/checks/StringMethodsWithLocaleCheck.java | 127 / 79 / 48 (37.795%) | 127 / 125 / 2 (1.575%) | 127 / 79 / 48 (37.795%) | 127 / 125 / 2 (1.575%) |
| java-checks/src/main/java/org/sonar/java/checks/StringOffsetMethodsCheck.java | 82 / 34 / 48 (58.537%) | 82 / 80 / 2 (2.439%) | 82 / 34 / 48 (58.537%) | 82 / 80 / 2 (2.439%) |
| java-checks/src/main/java/org/sonar/java/checks/StringPrimitiveConstructorCheck.java | 239 / 149 / 90 (37.657%) | 239 / 237 / 2 (0.837%) | 239 / 149 / 90 (37.657%) | 239 / 237 / 2 (0.837%) |
| java-checks/src/main/java/org/sonar/java/checks/StringToPrimitiveConversionCheck.java | 274 / 156 / 118 (43.066%) | 274 / 270 / 4 (1.460%) | 274 / 156 / 118 (43.066%) | 274 / 270 / 4 (1.460%) |
| java-checks/src/main/java/org/sonar/java/checks/StringToStringCheck.java | 197 / 95 / 102 (51.777%) | 197 / 193 / 4 (2.030%) | 197 / 95 / 102 (51.777%) | 197 / 193 / 4 (2.030%) |
| java-checks/src/main/java/org/sonar/java/checks/StrongCipherAlgorithmCheck.java | 95 / 62 / 33 (34.737%) | 95 / 93 / 2 (2.105%) | 95 / 62 / 33 (34.737%) | 95 / 93 / 2 (2.105%) |
| java-checks/src/main/java/org/sonar/java/checks/SubClassStaticReferenceCheck.java | 187 / 91 / 96 (51.337%) | 187 / 185 / 2 (1.070%) | 187 / 91 / 96 (51.337%) | 187 / 185 / 2 (1.070%) |
| java-checks/src/main/java/org/sonar/java/checks/SunPackagesUsedCheck.java | 99 / 65 / 34 (34.343%) | 99 / 93 / 6 (6.061%) | 99 / 65 / 34 (34.343%) | 99 / 93 / 6 (6.061%) |
| java-checks/src/main/java/org/sonar/java/checks/SuppressWarningsCheck.java | 177 / 125 / 52 (29.379%) | 177 / 167 / 10 (5.650%) | 177 / 125 / 52 (29.379%) | 177 / 167 / 10 (5.650%) |
| java-checks/src/main/java/org/sonar/java/checks/SuspiciousListRemoveCheck.java | 218 / 113 / 105 (48.165%) | 218 / 215 / 3 (1.376%) | 218 / 113 / 105 (48.165%) | 218 / 215 / 3 (1.376%) |
| java-checks/src/main/java/org/sonar/java/checks/SwitchAtLeastThreeCasesCheck.java | 122 / 58 / 64 (52.459%) | 122 / 120 / 2 (1.639%) | 122 / 58 / 64 (52.459%) | 122 / 120 / 2 (1.639%) |
| java-checks/src/main/java/org/sonar/java/checks/SwitchCaseTooBigCheck.java | 63 / 20 / 43 (68.254%) | 63 / 58 / 5 (7.937%) | 63 / 20 / 43 (68.254%) | 63 / 58 / 5 (7.937%) |
| java-checks/src/main/java/org/sonar/java/checks/SwitchCaseWithoutBreakCheck.java | 257 / 108 / 149 (57.977%) | 257 / 254 / 3 (1.167%) | 257 / 108 / 149 (57.977%) | 257 / 254 / 3 (1.167%) |
| java-checks/src/main/java/org/sonar/java/checks/SwitchCasesShouldBeCommaSeparatedCheck.java | 110 / 48 / 62 (56.364%) | 110 / 108 / 2 (1.818%) | 110 / 48 / 62 (56.364%) | 110 / 108 / 2 (1.818%) |
| java-checks/src/main/java/org/sonar/java/checks/SwitchDefaultLastCaseCheck.java | 95 / 53 / 42 (44.211%) | 95 / 93 / 2 (2.105%) | 95 / 53 / 42 (44.211%) | 95 / 93 / 2 (2.105%) |
| java-checks/src/main/java/org/sonar/java/checks/SwitchInsteadOfIfSequenceCheck.java | 229 / 117 / 112 (48.908%) | 229 / 227 / 2 (0.873%) | 229 / 117 / 112 (48.908%) | 229 / 227 / 2 (0.873%) |
| java-checks/src/main/java/org/sonar/java/checks/SwitchLastCaseIsDefaultCheck.java | 160 / 69 / 91 (56.875%) | 160 / 155 / 5 (3.125%) | 160 / 69 / 91 (56.875%) | 160 / 155 / 5 (3.125%) |
| java-checks/src/main/java/org/sonar/java/checks/SwitchRedundantKeywordCheck.java | 163 / 80 / 83 (50.920%) | 163 / 161 / 2 (1.227%) | 163 / 80 / 83 (50.920%) | 163 / 161 / 2 (1.227%) |
| java-checks/src/main/java/org/sonar/java/checks/SwitchWithLabelsCheck.java | 72 / 36 / 36 (50.000%) | 72 / 70 / 2 (2.778%) | 72 / 36 / 36 (50.000%) | 72 / 70 / 2 (2.778%) |
| java-checks/src/main/java/org/sonar/java/checks/SwitchWithTooManyCasesCheck.java | 91 / 40 / 51 (56.044%) | 91 / 85 / 6 (6.593%) | 91 / 40 / 51 (56.044%) | 91 / 85 / 6 (6.593%) |
| java-checks/src/main/java/org/sonar/java/checks/SymmetricEqualsCheck.java | 145 / 75 / 70 (48.276%) | 145 / 143 / 2 (1.379%) | 145 / 75 / 70 (48.276%) | 145 / 143 / 2 (1.379%) |
| java-checks/src/main/java/org/sonar/java/checks/SyncGetterAndSetterCheck.java | 222 / 111 / 111 (50.000%) | 222 / 220 / 2 (0.901%) | 222 / 111 / 111 (50.000%) | 222 / 220 / 2 (0.901%) |
| java-checks/src/main/java/org/sonar/java/checks/SynchronizationOnStringOrBoxedCheck.java | 121 / 54 / 67 (55.372%) | 121 / 117 / 4 (3.306%) | 121 / 54 / 67 (55.372%) | 121 / 117 / 4 (3.306%) |
| java-checks/src/main/java/org/sonar/java/checks/SynchronizedClassUsageCheck.java | 341 / 207 / 134 (39.296%) | 341 / 330 / 11 (3.226%) | 341 / 207 / 134 (39.296%) | 341 / 330 / 11 (3.226%) |
| java-checks/src/main/java/org/sonar/java/checks/SynchronizedFieldAssignmentCheck.java | 203 / 100 / 103 (50.739%) | 203 / 199 / 4 (1.970%) | 203 / 100 / 103 (50.739%) | 203 / 199 / 4 (1.970%) |
| java-checks/src/main/java/org/sonar/java/checks/SynchronizedOnConcurrentObjectCheck.java | 76 / 53 / 23 (30.263%) | 76 / 74 / 2 (2.632%) | 76 / 53 / 23 (30.263%) | 76 / 74 / 2 (2.632%) |
| java-checks/src/main/java/org/sonar/java/checks/SynchronizedOverrideCheck.java | 72 / 38 / 34 (47.222%) | 72 / 70 / 2 (2.778%) | 72 / 38 / 34 (47.222%) | 72 / 70 / 2 (2.778%) |
| java-checks/src/main/java/org/sonar/java/checks/SystemExitCalledCheck.java | 137 / 84 / 53 (38.686%) | 137 / 135 / 2 (1.460%) | 137 / 84 / 53 (38.686%) | 137 / 135 / 2 (1.460%) |
| java-checks/src/main/java/org/sonar/java/checks/SystemOutOrErrUsageCheck.java | 123 / 64 / 59 (47.967%) | 123 / 121 / 2 (1.626%) | 123 / 64 / 59 (47.967%) | 123 / 121 / 2 (1.626%) |
| java-checks/src/main/java/org/sonar/java/checks/TabCharacterCheck.java | 36 / 23 / 13 (36.111%) | 36 / 31 / 5 (13.889%) | 36 / 23 / 13 (36.111%) | 36 / 31 / 5 (13.889%) |
| java-checks/src/main/java/org/sonar/java/checks/TernaryOperatorCheck.java | 26 / 13 / 13 (50.000%) | 26 / 24 / 2 (7.692%) | 26 / 13 / 13 (50.000%) | 26 / 24 / 2 (7.692%) |
| java-checks/src/main/java/org/sonar/java/checks/TernaryOperatorSameOperationCheck.java | 266 / 143 / 123 (46.241%) | 266 / 264 / 2 (0.752%) | 266 / 143 / 123 (46.241%) | 266 / 264 / 2 (0.752%) |
| java-checks/src/main/java/org/sonar/java/checks/TestsInSeparateFolderCheck.java | 31 / 16 / 15 (48.387%) | 31 / 29 / 2 (6.452%) | 31 / 16 / 15 (48.387%) | 31 / 29 / 2 (6.452%) |
| java-checks/src/main/java/org/sonar/java/checks/TextBlockTabsAndSpacesCheck.java | 84 / 66 / 18 (21.429%) | 84 / 82 / 2 (2.381%) | 84 / 66 / 18 (21.429%) | 84 / 82 / 2 (2.381%) |
| java-checks/src/main/java/org/sonar/java/checks/TextBlocksInComplexExpressionsCheck.java | 94 / 56 / 38 (40.426%) | 94 / 88 / 6 (6.383%) | 94 / 56 / 38 (40.426%) | 94 / 88 / 6 (6.383%) |
| java-checks/src/main/java/org/sonar/java/checks/ThisExposedFromConstructorCheck.java | 116 / 58 / 58 (50.000%) | 116 / 114 / 2 (1.724%) | 116 / 58 / 58 (50.000%) | 116 / 114 / 2 (1.724%) |
| java-checks/src/main/java/org/sonar/java/checks/ThreadAsRunnableArgumentCheck.java | 289 / 159 / 130 (44.983%) | 289 / 287 / 2 (0.692%) | 289 / 159 / 130 (44.983%) | 289 / 287 / 2 (0.692%) |
| java-checks/src/main/java/org/sonar/java/checks/ThreadLocalCleanupCheck.java | 141 / 45 / 96 (68.085%) | 141 / 139 / 2 (1.418%) | 141 / 45 / 96 (68.085%) | 141 / 139 / 2 (1.418%) |
| java-checks/src/main/java/org/sonar/java/checks/ThreadLocalWithInitialCheck.java | 75 / 24 / 51 (68.000%) | 75 / 73 / 2 (2.667%) | 75 / 24 / 51 (68.000%) | 75 / 73 / 2 (2.667%) |
| java-checks/src/main/java/org/sonar/java/checks/ThreadOverridesRunCheck.java | 175 / 92 / 83 (47.429%) | 175 / 173 / 2 (1.143%) | 175 / 92 / 83 (47.429%) | 175 / 173 / 2 (1.143%) |
| java-checks/src/main/java/org/sonar/java/checks/ThreadRunCheck.java | 70 / 28 / 42 (60.000%) | 70 / 68 / 2 (2.857%) | 70 / 28 / 42 (60.000%) | 70 / 68 / 2 (2.857%) |
| java-checks/src/main/java/org/sonar/java/checks/ThreadSleepCheck.java | 26 / 11 / 15 (57.692%) | 26 / 24 / 2 (7.692%) | 26 / 11 / 15 (57.692%) | 26 / 24 / 2 (7.692%) |
| java-checks/src/main/java/org/sonar/java/checks/ThreadStartedInConstructorCheck.java | 103 / 40 / 63 (61.165%) | 103 / 101 / 2 (1.942%) | 103 / 40 / 63 (61.165%) | 103 / 101 / 2 (1.942%) |
| java-checks/src/main/java/org/sonar/java/checks/ThreadWaitCallCheck.java | 34 / 11 / 23 (67.647%) | 34 / 32 / 2 (5.882%) | 34 / 11 / 23 (67.647%) | 34 / 32 / 2 (5.882%) |
| java-checks/src/main/java/org/sonar/java/checks/ThrowCheckedExceptionCheck.java | 105 / 55 / 50 (47.619%) | 105 / 103 / 2 (1.905%) | 105 / 55 / 50 (47.619%) | 105 / 103 / 2 (1.905%) |
| java-checks/src/main/java/org/sonar/java/checks/ThrowsFromFinallyCheck.java | 66 / 44 / 22 (33.333%) | 66 / 64 / 2 (3.030%) | 66 / 44 / 22 (33.333%) | 66 / 64 / 2 (3.030%) |
| java-checks/src/main/java/org/sonar/java/checks/ThrowsSeveralCheckedExceptionCheck.java | 107 / 60 / 47 (43.925%) | 107 / 105 / 2 (1.869%) | 107 / 60 / 47 (43.925%) | 107 / 105 / 2 (1.869%) |
| java-checks/src/main/java/org/sonar/java/checks/TimeZoneIdCheck.java | 128 / 81 / 47 (36.719%) | 128 / 126 / 2 (1.563%) | 128 / 81 / 47 (36.719%) | 128 / 126 / 2 (1.563%) |
| java-checks/src/main/java/org/sonar/java/checks/ToArrayCheck.java | 128 / 54 / 74 (57.813%) | 128 / 126 / 2 (1.563%) | 128 / 54 / 74 (57.813%) | 128 / 126 / 2 (1.563%) |
| java-checks/src/main/java/org/sonar/java/checks/ToStringReturningNullCheck.java | 123 / 60 / 63 (51.220%) | 123 / 121 / 2 (1.626%) | 123 / 60 / 63 (51.220%) | 123 / 121 / 2 (1.626%) |
| java-checks/src/main/java/org/sonar/java/checks/ToStringUsingBoxingCheck.java | 265 / 118 / 147 (55.472%) | 265 / 263 / 2 (0.755%) | 265 / 118 / 147 (55.472%) | 265 / 263 / 2 (0.755%) |
| java-checks/src/main/java/org/sonar/java/checks/TodoTagPresenceCheck.java | 35 / 22 / 13 (37.143%) | 35 / 33 / 2 (5.714%) | 35 / 22 / 13 (37.143%) | 35 / 33 / 2 (5.714%) |
| java-checks/src/main/java/org/sonar/java/checks/TooLongLineCheck.java | 139 / 95 / 44 (31.655%) | 139 / 130 / 9 (6.475%) | 139 / 95 / 44 (31.655%) | 139 / 130 / 9 (6.475%) |
| java-checks/src/main/java/org/sonar/java/checks/TooManyLinesOfCodeInFileCheck.java | 48 / 26 / 22 (45.833%) | 48 / 39 / 9 (18.750%) | 48 / 26 / 22 (45.833%) | 48 / 39 / 9 (18.750%) |
| java-checks/src/main/java/org/sonar/java/checks/TooManyMethodsCheck.java | 160 / 71 / 89 (55.625%) | 160 / 150 / 10 (6.250%) | 160 / 71 / 89 (55.625%) | 160 / 150 / 10 (6.250%) |
| java-checks/src/main/java/org/sonar/java/checks/TooManyParametersCheck.java | 173 / 81 / 92 (53.179%) | 173 / 160 / 13 (7.514%) | 173 / 81 / 92 (53.179%) | 173 / 160 / 13 (7.514%) |
| java-checks/src/main/java/org/sonar/java/checks/TooManyStatementsPerLineCheck.java | 366 / 211 / 155 (42.350%) | 366 / 361 / 5 (1.366%) | 366 / 211 / 155 (42.350%) | 366 / 361 / 5 (1.366%) |
| java-checks/src/main/java/org/sonar/java/checks/TrailingCommentCheck.java | 131 / 73 / 58 (44.275%) | 131 / 119 / 12 (9.160%) | 131 / 73 / 58 (44.275%) | 131 / 119 / 12 (9.160%) |
| java-checks/src/main/java/org/sonar/java/checks/TransientFieldInNonSerializableCheck.java | 72 / 35 / 37 (51.389%) | 72 / 70 / 2 (2.778%) | 72 / 35 / 37 (51.389%) | 72 / 70 / 2 (2.778%) |
| java-checks/src/main/java/org/sonar/java/checks/TryWithResourcesCheck.java | 284 / 114 / 170 (59.859%) | 284 / 282 / 2 (0.704%) | 284 / 114 / 170 (59.859%) | 284 / 282 / 2 (0.704%) |
| java-checks/src/main/java/org/sonar/java/checks/TypeParametersShadowingCheck.java | 138 / 73 / 65 (47.101%) | 138 / 136 / 2 (1.449%) | 138 / 73 / 65 (47.101%) | 138 / 136 / 2 (1.449%) |
| java-checks/src/main/java/org/sonar/java/checks/TypeUpperBoundNotFinalCheck.java | 154 / 71 / 83 (53.896%) | 154 / 152 / 2 (1.299%) | 154 / 71 / 83 (53.896%) | 154 / 152 / 2 (1.299%) |
| java-checks/src/main/java/org/sonar/java/checks/URLHashCodeAndEqualsCheck.java | 102 / 47 / 55 (53.922%) | 102 / 99 / 3 (2.941%) | 102 / 47 / 55 (53.922%) | 102 / 99 / 3 (2.941%) |
| java-checks/src/main/java/org/sonar/java/checks/UnderscoreMisplacedOnNumberCheck.java | 106 / 89 / 17 (16.038%) | 106 / 104 / 2 (1.887%) | 106 / 89 / 17 (16.038%) | 106 / 104 / 2 (1.887%) |
| java-checks/src/main/java/org/sonar/java/checks/UnderscoreOnNumberCheck.java | 142 / 110 / 32 (22.535%) | 142 / 140 / 2 (1.408%) | 142 / 110 / 32 (22.535%) | 142 / 140 / 2 (1.408%) |
| java-checks/src/main/java/org/sonar/java/checks/UndocumentedApiCheck.java | 454 / 267 / 187 (41.189%) | 454 / 428 / 26 (5.727%) | 454 / 267 / 187 (41.189%) | 454 / 428 / 26 (5.727%) |
| java-checks/src/main/java/org/sonar/java/checks/UnnamedVariableShouldUseVarCheck.java | 83 / 39 / 44 (53.012%) | 83 / 81 / 2 (2.410%) | 83 / 39 / 44 (53.012%) | 83 / 81 / 2 (2.410%) |
| java-checks/src/main/java/org/sonar/java/checks/UnnecessaryBitOperationCheck.java | 84 / 39 / 45 (53.571%) | 84 / 82 / 2 (2.381%) | 84 / 39 / 45 (53.571%) | 84 / 82 / 2 (2.381%) |
| java-checks/src/main/java/org/sonar/java/checks/UnnecessaryEscapeSequencesInTextBlockCheck.java | 79 / 59 / 20 (25.316%) | 79 / 75 / 4 (5.063%) | 79 / 59 / 20 (25.316%) | 79 / 75 / 4 (5.063%) |
| java-checks/src/main/java/org/sonar/java/checks/UnnecessarySemicolonCheck.java | 40 / 18 / 22 (55.000%) | 40 / 36 / 4 (10.000%) | 40 / 18 / 22 (55.000%) | 40 / 36 / 4 (10.000%) |
| java-checks/src/main/java/org/sonar/java/checks/UnreachableCatchCheck.java | 310 / 135 / 175 (56.452%) | 310 / 308 / 2 (0.645%) | 310 / 135 / 175 (56.452%) | 310 / 308 / 2 (0.645%) |
| java-checks/src/main/java/org/sonar/java/checks/UnsupportedChronoUnitWithInstantCheck.java | 123 / 70 / 53 (43.089%) | 123 / 119 / 4 (3.252%) | 123 / 70 / 53 (43.089%) | 123 / 119 / 4 (3.252%) |
| java-checks/src/main/java/org/sonar/java/checks/UnusedScopedValueWhereResultCheck.java | 261 / 149 / 112 (42.912%) | 261 / 259 / 2 (0.766%) | 261 / 149 / 112 (42.912%) | 261 / 259 / 2 (0.766%) |
| java-checks/src/main/java/org/sonar/java/checks/UppercaseSuffixesCheck.java | 54 / 31 / 23 (42.593%) | 54 / 48 / 6 (11.111%) | 54 / 31 / 23 (42.593%) | 54 / 48 / 6 (11.111%) |
| java-checks/src/main/java/org/sonar/java/checks/UseIsEmptyToTestEmptinessOfStringBuilderCheck.java | 217 / 94 / 123 (56.682%) | 217 / 215 / 2 (0.922%) | 217 / 94 / 123 (56.682%) | 217 / 215 / 2 (0.922%) |
| java-checks/src/main/java/org/sonar/java/checks/UseMotionSensorWithoutGyroscopeCheck.java | 41 / 17 / 24 (58.537%) | 41 / 39 / 2 (4.878%) | 41 / 17 / 24 (58.537%) | 41 / 39 / 2 (4.878%) |
| java-checks/src/main/java/org/sonar/java/checks/UseOfSequentialForSequentialGathererCheck.java | 160 / 98 / 62 (38.750%) | 160 / 158 / 2 (1.250%) | 160 / 98 / 62 (38.750%) | 160 / 158 / 2 (1.250%) |
| java-checks/src/main/java/org/sonar/java/checks/UseSwitchExpressionCheck.java | 224 / 101 / 123 (54.911%) | 224 / 218 / 6 (2.679%) | 224 / 101 / 123 (54.911%) | 224 / 218 / 6 (2.679%) |
| java-checks/src/main/java/org/sonar/java/checks/UseTransformClassInsteadOfBuildCheck.java | 85 / 37 / 48 (56.471%) | 85 / 83 / 2 (2.353%) | 85 / 37 / 48 (56.471%) | 85 / 83 / 2 (2.353%) |
| java-checks/src/main/java/org/sonar/java/checks/UselessExtendsCheck.java | 239 / 126 / 113 (47.280%) | 239 / 237 / 2 (0.837%) | 239 / 126 / 113 (47.280%) | 239 / 237 / 2 (0.837%) |
| java-checks/src/main/java/org/sonar/java/checks/UselessImportCheck.java | 401 / 249 / 152 (37.905%) | 401 / 396 / 5 (1.247%) | 401 / 249 / 152 (37.905%) | 401 / 396 / 5 (1.247%) |
| java-checks/src/main/java/org/sonar/java/checks/UselessIncrementCheck.java | 162 / 69 / 93 (57.407%) | 162 / 160 / 2 (1.235%) | 162 / 69 / 93 (57.407%) | 162 / 160 / 2 (1.235%) |
| java-checks/src/main/java/org/sonar/java/checks/UselessMathematicalComparisonCheck.java | 297 / 199 / 98 (32.997%) | 297 / 290 / 7 (2.357%) | 297 / 199 / 98 (32.997%) | 297 / 290 / 7 (2.357%) |
| java-checks/src/main/java/org/sonar/java/checks/UselessPackageInfoCheck.java | 84 / 68 / 16 (19.048%) | 84 / 80 / 4 (4.762%) | 84 / 68 / 16 (19.048%) | 84 / 80 / 4 (4.762%) |
| java-checks/src/main/java/org/sonar/java/checks/UselessParenthesesCheck.java | 43 / 19 / 24 (55.814%) | 43 / 38 / 5 (11.628%) | 43 / 19 / 24 (55.814%) | 43 / 38 / 5 (11.628%) |
| java-checks/src/main/java/org/sonar/java/checks/UtilityClassWithPublicConstructorCheck.java | 316 / 156 / 160 (50.633%) | 316 / 308 / 8 (2.532%) | 316 / 156 / 160 (50.633%) | 316 / 308 / 8 (2.532%) |
| java-checks/src/main/java/org/sonar/java/checks/ValueBasedObjectIdentityCheck.java | 82 / 40 / 42 (51.220%) | 82 / 80 / 2 (2.439%) | 82 / 40 / 42 (51.220%) | 82 / 80 / 2 (2.439%) |
| java-checks/src/main/java/org/sonar/java/checks/ValueBasedObjectsShouldNotBeSerializedCheck.java | 166 / 63 / 103 (62.048%) | 166 / 164 / 2 (1.205%) | 166 / 63 / 103 (62.048%) | 166 / 164 / 2 (1.205%) |
| java-checks/src/main/java/org/sonar/java/checks/VarArgCheck.java | 50 / 23 / 27 (54.000%) | 50 / 48 / 2 (4.000%) | 50 / 23 / 27 (54.000%) | 50 / 48 / 2 (4.000%) |
| java-checks/src/main/java/org/sonar/java/checks/VarCanBeUsedCheck.java | 267 / 142 / 125 (46.816%) | 267 / 265 / 2 (0.749%) | 267 / 142 / 125 (46.816%) | 267 / 265 / 2 (0.749%) |
| java-checks/src/main/java/org/sonar/java/checks/VariableDeclarationScopeCheck.java | 153 / 99 / 54 (35.294%) | 153 / 151 / 2 (1.307%) | 153 / 99 / 54 (35.294%) | 153 / 151 / 2 (1.307%) |
| java-checks/src/main/java/org/sonar/java/checks/VirtualThreadNotSynchronizedCheck.java | 298 / 164 / 134 (44.966%) | 298 / 296 / 2 (0.671%) | 298 / 164 / 134 (44.966%) | 298 / 296 / 2 (0.671%) |
| java-checks/src/main/java/org/sonar/java/checks/VirtualThreadUnsupportedMethodsCheck.java | 138 / 72 / 66 (47.826%) | 138 / 132 / 6 (4.348%) | 138 / 72 / 66 (47.826%) | 138 / 132 / 6 (4.348%) |
| java-checks/src/main/java/org/sonar/java/checks/VisibleForTestingUsageCheck.java | 150 / 81 / 69 (46.000%) | 150 / 148 / 2 (1.333%) | 150 / 81 / 69 (46.000%) | 150 / 148 / 2 (1.333%) |
| java-checks/src/main/java/org/sonar/java/checks/VolatileNonPrimitiveFieldCheck.java | 154 / 60 / 94 (61.039%) | 154 / 152 / 2 (1.299%) | 154 / 60 / 94 (61.039%) | 154 / 152 / 2 (1.299%) |
| java-checks/src/main/java/org/sonar/java/checks/VolatileVariablesOperationsCheck.java | 248 / 130 / 118 (47.581%) | 248 / 243 / 5 (2.016%) | 248 / 130 / 118 (47.581%) | 248 / 243 / 5 (2.016%) |
| java-checks/src/main/java/org/sonar/java/checks/WaitInSynchronizeCheck.java | 59 / 23 / 36 (61.017%) | 59 / 57 / 2 (3.390%) | 59 / 23 / 36 (61.017%) | 59 / 57 / 2 (3.390%) |
| java-checks/src/main/java/org/sonar/java/checks/WaitInWhileLoopCheck.java | 136 / 61 / 75 (55.147%) | 136 / 134 / 2 (1.471%) | 136 / 61 / 75 (55.147%) | 136 / 134 / 2 (1.471%) |
| java-checks/src/main/java/org/sonar/java/checks/WaitOnConditionCheck.java | 34 / 15 / 19 (55.882%) | 34 / 32 / 2 (5.882%) | 34 / 15 / 19 (55.882%) | 34 / 32 / 2 (5.882%) |
| java-checks/src/main/java/org/sonar/java/checks/WeakSSLContextCheck.java | 242 / 122 / 120 (49.587%) | 242 / 240 / 2 (0.826%) | 242 / 122 / 120 (49.587%) | 242 / 240 / 2 (0.826%) |
| java-checks/src/main/java/org/sonar/java/checks/WildcardImportsShouldNotBeUsedCheck.java | 39 / 21 / 18 (46.154%) | 39 / 37 / 2 (5.128%) | 39 / 21 / 18 (46.154%) | 39 / 37 / 2 (5.128%) |
| java-checks/src/main/java/org/sonar/java/checks/WildcardReturnParameterTypeCheck.java | 102 / 52 / 50 (49.020%) | 102 / 100 / 2 (1.961%) | 102 / 52 / 50 (49.020%) | 102 / 100 / 2 (1.961%) |
| java-checks/src/main/java/org/sonar/java/checks/WrongAssignmentOperatorCheck.java | 105 / 61 / 44 (41.905%) | 105 / 101 / 4 (3.810%) | 105 / 61 / 44 (41.905%) | 105 / 101 / 4 (3.810%) |
| java-checks/src/main/java/org/sonar/java/checks/design/AbstractCouplingChecker.java | 193 / 129 / 64 (33.161%) | 193 / 191 / 2 (1.036%) | 193 / 129 / 64 (33.161%) | 193 / 191 / 2 (1.036%) |
| java-checks/src/main/java/org/sonar/java/checks/design/BrainMethodCheck.java | 208 / 147 / 61 (29.327%) | 208 / 190 / 18 (8.654%) | 208 / 147 / 61 (29.327%) | 208 / 190 / 18 (8.654%) |
| java-checks/src/main/java/org/sonar/java/checks/design/ClassCouplingCheck.java | 102 / 48 / 54 (52.941%) | 102 / 94 / 8 (7.843%) | 102 / 48 / 54 (52.941%) | 102 / 94 / 8 (7.843%) |
| java-checks/src/main/java/org/sonar/java/checks/design/ClassImportCouplingCheck.java | 216 / 99 / 117 (54.167%) | 216 / 207 / 9 (4.167%) | 216 / 99 / 117 (54.167%) | 216 / 207 / 9 (4.167%) |
| java-checks/src/main/java/org/sonar/java/checks/design/SingletonUsageCheck.java | 346 / 127 / 219 (63.295%) | 346 / 342 / 4 (1.156%) | 346 / 127 / 219 (63.295%) | 346 / 342 / 4 (1.156%) |
| java-checks/src/main/java/org/sonar/java/checks/design/package-info.java | 7 / 5 / 2 (28.571%) | 7 / 6 / 1 (14.286%) | 7 / 5 / 2 (28.571%) | 7 / 6 / 1 (14.286%) |
| java-checks/src/main/java/org/sonar/java/checks/helpers/AbstractAssertionVisitor.java | 86 / 60 / 26 (30.233%) | 86 / 84 / 2 (2.326%) | 86 / 60 / 26 (30.233%) | 86 / 84 / 2 (2.326%) |
| java-checks/src/main/java/org/sonar/java/checks/helpers/AnnotationsHelper.java | 26 / 18 / 8 (30.769%) | 26 / 26 / 0 (0.000%) | 26 / 18 / 8 (30.769%) | 26 / 26 / 0 (0.000%) |
| java-checks/src/main/java/org/sonar/java/checks/helpers/AnonymousClassToLambdaUtils.java | 322 / 162 / 160 (49.689%) | 322 / 322 / 0 (0.000%) | 322 / 162 / 160 (49.689%) | 322 / 322 / 0 (0.000%) |
| java-checks/src/main/java/org/sonar/java/checks/helpers/ClassPatternsUtils.java | 168 / 74 / 94 (55.952%) | 168 / 168 / 0 (0.000%) | 168 / 74 / 94 (55.952%) | 168 / 168 / 0 (0.000%) |
| java-checks/src/main/java/org/sonar/java/checks/helpers/ComparisonMethodUtils.java | 101 / 45 / 56 (55.446%) | 101 / 101 / 0 (0.000%) | 101 / 45 / 56 (55.446%) | 101 / 101 / 0 (0.000%) |
| java-checks/src/main/java/org/sonar/java/checks/helpers/DeprecatedCheckerHelper.java | 240 / 144 / 96 (40.000%) | 240 / 235 / 5 (2.083%) | 240 / 144 / 96 (40.000%) | 240 / 235 / 5 (2.083%) |
| java-checks/src/main/java/org/sonar/java/checks/helpers/GeneratedStringLiteralRecognizer.java | 87 / 57 / 30 (34.483%) | 87 / 87 / 0 (0.000%) | 87 / 57 / 30 (34.483%) | 87 / 87 / 0 (0.000%) |
| java-checks/src/main/java/org/sonar/java/checks/helpers/InjectionHelper.java | 24 / 20 / 4 (16.667%) | 24 / 24 / 0 (0.000%) | 24 / 20 / 4 (16.667%) | 24 / 24 / 0 (0.000%) |
| java-checks/src/main/java/org/sonar/java/checks/helpers/IntegerOverflowRange.java | 608 / 445 / 163 (26.809%) | 608 / 598 / 10 (1.645%) | 608 / 445 / 163 (26.809%) | 608 / 598 / 10 (1.645%) |
| java-checks/src/main/java/org/sonar/java/checks/helpers/JavaPropertiesHelper.java | 89 / 43 / 46 (51.685%) | 89 / 86 / 3 (3.371%) | 89 / 43 / 46 (51.685%) | 89 / 86 / 3 (3.371%) |
| java-checks/src/main/java/org/sonar/java/checks/helpers/Javadoc.java | 588 / 523 / 65 (11.054%) | 588 / 578 / 10 (1.701%) | 588 / 523 / 65 (11.054%) | 588 / 578 / 10 (1.701%) |
| java-checks/src/main/java/org/sonar/java/checks/helpers/LatinAlphabetLanguagesHelper.java | 125 / 123 / 2 (1.600%) | 125 / 123 / 2 (1.600%) | 125 / 123 / 2 (1.600%) | 125 / 123 / 2 (1.600%) |
| java-checks/src/main/java/org/sonar/java/checks/helpers/LoggingMatchers.java | 46 / 14 / 32 (69.565%) | 46 / 46 / 0 (0.000%) | 46 / 14 / 32 (69.565%) | 46 / 46 / 0 (0.000%) |
| java-checks/src/main/java/org/sonar/java/checks/helpers/MethodTreeUtils.java | 515 / 326 / 189 (36.699%) | 515 / 510 / 5 (0.971%) | 515 / 326 / 189 (36.699%) | 515 / 510 / 5 (0.971%) |
| java-checks/src/main/java/org/sonar/java/checks/helpers/NullabilityDataUtils.java | 96 / 54 / 42 (43.750%) | 96 / 96 / 0 (0.000%) | 96 / 54 / 42 (43.750%) | 96 / 96 / 0 (0.000%) |
| java-checks/src/main/java/org/sonar/java/checks/helpers/QuickFixHelper.java | 652 / 407 / 245 (37.577%) | 652 / 645 / 7 (1.074%) | 652 / 407 / 245 (37.577%) | 652 / 645 / 7 (1.074%) |
| java-checks/src/main/java/org/sonar/java/checks/helpers/RandomnessDetector.java | 59 / 53 / 6 (10.169%) | 59 / 59 / 0 (0.000%) | 59 / 53 / 6 (10.169%) | 59 / 59 / 0 (0.000%) |
| java-checks/src/main/java/org/sonar/java/checks/helpers/ShannonEntropy.java | 49 / 48 / 1 (2.041%) | 49 / 48 / 1 (2.041%) | 49 / 48 / 1 (2.041%) | 49 / 48 / 1 (2.041%) |
| java-checks/src/main/java/org/sonar/java/checks/helpers/TernaryValue.java | 31 / 30 / 1 (3.226%) | 31 / 30 / 1 (3.226%) | 31 / 30 / 1 (3.226%) | 31 / 30 / 1 (3.226%) |
| java-checks/src/main/java/org/sonar/java/checks/helpers/TryCatchUtils.java | 32 / 17 / 15 (46.875%) | 32 / 32 / 0 (0.000%) | 32 / 17 / 15 (46.875%) | 32 / 32 / 0 (0.000%) |
| java-checks/src/main/java/org/sonar/java/checks/helpers/UnresolvedIdentifiersVisitor.java | 91 / 62 / 29 (31.868%) | 91 / 91 / 0 (0.000%) | 91 / 62 / 29 (31.868%) | 91 / 91 / 0 (0.000%) |
| java-checks/src/main/java/org/sonar/java/checks/helpers/ValueBasedUtils.java | 43 / 34 / 9 (20.930%) | 43 / 41 / 2 (4.651%) | 43 / 34 / 9 (20.930%) | 43 / 41 / 2 (4.651%) |
| java-checks/src/main/java/org/sonar/java/checks/helpers/package-info.java | 6 / 5 / 1 (16.667%) | 6 / 5 / 1 (16.667%) | 6 / 5 / 1 (16.667%) | 6 / 5 / 1 (16.667%) |
| java-checks/src/main/java/org/sonar/java/checks/naming/BadAbstractClassNameCheck.java | 89 / 56 / 33 (37.079%) | 89 / 80 / 9 (10.112%) | 89 / 56 / 33 (37.079%) | 89 / 80 / 9 (10.112%) |
| java-checks/src/main/java/org/sonar/java/checks/naming/BadClassNameCheck.java | 91 / 56 / 35 (38.462%) | 91 / 82 / 9 (9.890%) | 91 / 56 / 35 (38.462%) | 91 / 82 / 9 (9.890%) |
| java-checks/src/main/java/org/sonar/java/checks/naming/BadConstantNameCheck.java | 146 / 77 / 69 (47.260%) | 146 / 137 / 9 (6.164%) | 146 / 77 / 69 (47.260%) | 146 / 137 / 9 (6.164%) |
| java-checks/src/main/java/org/sonar/java/checks/naming/BadFieldNameCheck.java | 35 / 16 / 19 (54.286%) | 35 / 26 / 9 (25.714%) | 35 / 16 / 19 (54.286%) | 35 / 26 / 9 (25.714%) |
| java-checks/src/main/java/org/sonar/java/checks/naming/BadFieldNameStaticNonFinalCheck.java | 37 / 17 / 20 (54.054%) | 37 / 31 / 6 (16.216%) | 37 / 17 / 20 (54.054%) | 37 / 31 / 6 (16.216%) |
| java-checks/src/main/java/org/sonar/java/checks/naming/BadInterfaceNameCheck.java | 68 / 42 / 26 (38.235%) | 68 / 59 / 9 (13.235%) | 68 / 42 / 26 (38.235%) | 68 / 59 / 9 (13.235%) |
| java-checks/src/main/java/org/sonar/java/checks/naming/BadLocalConstantNameCheck.java | 110 / 52 / 58 (52.727%) | 110 / 103 / 7 (6.364%) | 110 / 52 / 58 (52.727%) | 110 / 103 / 7 (6.364%) |
| java-checks/src/main/java/org/sonar/java/checks/naming/BadLocalVariableNameCheck.java | 187 / 120 / 67 (35.829%) | 187 / 178 / 9 (4.813%) | 187 / 120 / 67 (35.829%) | 187 / 178 / 9 (4.813%) |
| java-checks/src/main/java/org/sonar/java/checks/naming/BadMethodNameCheck.java | 92 / 53 / 39 (42.391%) | 92 / 83 / 9 (9.783%) | 92 / 53 / 39 (42.391%) | 92 / 83 / 9 (9.783%) |
| java-checks/src/main/java/org/sonar/java/checks/naming/BadPackageNameCheck.java | 141 / 102 / 39 (27.660%) | 141 / 125 / 16 (11.348%) | 141 / 102 / 39 (27.660%) | 141 / 125 / 16 (11.348%) |
| java-checks/src/main/java/org/sonar/java/checks/naming/BadTestClassNameCheck.java | 82 / 55 / 27 (32.927%) | 82 / 75 / 7 (8.537%) | 82 / 55 / 27 (32.927%) | 82 / 75 / 7 (8.537%) |
| java-checks/src/main/java/org/sonar/java/checks/naming/BadTestMethodNameCheck.java | 75 / 49 / 26 (34.667%) | 75 / 69 / 6 (8.000%) | 75 / 49 / 26 (34.667%) | 75 / 69 / 6 (8.000%) |
| java-checks/src/main/java/org/sonar/java/checks/naming/BadTypeParameterNameCheck.java | 65 / 40 / 25 (38.462%) | 65 / 56 / 9 (13.846%) | 65 / 40 / 25 (38.462%) | 65 / 56 / 9 (13.846%) |
| java-checks/src/main/java/org/sonar/java/checks/naming/BooleanMethodNameCheck.java | 68 / 42 / 26 (38.235%) | 68 / 66 / 2 (2.941%) | 68 / 42 / 26 (38.235%) | 68 / 66 / 2 (2.941%) |
| java-checks/src/main/java/org/sonar/java/checks/naming/ClassNamedLikeExceptionCheck.java | 96 / 63 / 33 (34.375%) | 96 / 94 / 2 (2.083%) | 96 / 63 / 33 (34.375%) | 96 / 94 / 2 (2.083%) |
| java-checks/src/main/java/org/sonar/java/checks/naming/FieldNameMatchingTypeNameCheck.java | 111 / 68 / 43 (38.739%) | 111 / 109 / 2 (1.802%) | 111 / 68 / 43 (38.739%) | 111 / 109 / 2 (1.802%) |
| java-checks/src/main/java/org/sonar/java/checks/naming/KeywordAsIdentifierCheck.java | 47 / 31 / 16 (34.043%) | 47 / 45 / 2 (4.255%) | 47 / 31 / 16 (34.043%) | 47 / 45 / 2 (4.255%) |
| java-checks/src/main/java/org/sonar/java/checks/naming/MethodNameSameAsClassCheck.java | 60 / 36 / 24 (40.000%) | 60 / 58 / 2 (3.333%) | 60 / 36 / 24 (40.000%) | 60 / 58 / 2 (3.333%) |
| java-checks/src/main/java/org/sonar/java/checks/naming/MethodNamedEqualsCheck.java | 73 / 38 / 35 (47.945%) | 73 / 71 / 2 (2.740%) | 73 / 38 / 35 (47.945%) | 73 / 71 / 2 (2.740%) |
| java-checks/src/main/java/org/sonar/java/checks/naming/MethodNamedHashcodeOrEqualCheck.java | 58 / 38 / 20 (34.483%) | 58 / 56 / 2 (3.448%) | 58 / 38 / 20 (34.483%) | 58 / 56 / 2 (3.448%) |
| java-checks/src/main/java/org/sonar/java/checks/naming/package-info.java | 6 / 5 / 1 (16.667%) | 6 / 5 / 1 (16.667%) | 6 / 5 / 1 (16.667%) | 6 / 5 / 1 (16.667%) |
| java-checks/src/main/java/org/sonar/java/checks/package-info.java | 5 / 4 / 1 (20.000%) | 5 / 4 / 1 (20.000%) | 5 / 4 / 1 (20.000%) | 5 / 4 / 1 (20.000%) |
| java-checks/src/main/java/org/sonar/java/checks/quarkus/CacheKeyGeneratorInstantiableCheck.java | 168 / 89 / 79 (47.024%) | 168 / 166 / 2 (1.190%) | 168 / 89 / 79 (47.024%) | 168 / 166 / 2 (1.190%) |
| java-checks/src/main/java/org/sonar/java/checks/quarkus/SingletonInsteadOfApplicationScopedCheck.java | 155 / 66 / 89 (57.419%) | 155 / 153 / 2 (1.290%) | 155 / 66 / 89 (57.419%) | 155 / 153 / 2 (1.290%) |
| java-checks/src/main/java/org/sonar/java/checks/quarkus/package-info.java | 6 / 5 / 1 (16.667%) | 6 / 5 / 1 (16.667%) | 6 / 5 / 1 (16.667%) | 6 / 5 / 1 (16.667%) |
| java-checks/src/main/java/org/sonar/java/checks/regex/AbstractRedosCheck.java | 545 / 358 / 187 (34.312%) | 545 / 368 / 177 (32.477%) | 545 / 358 / 187 (34.312%) | 545 / 368 / 177 (32.477%) |
| java-checks/src/main/java/org/sonar/java/checks/regex/AbstractRegexCheck.java | 646 / 341 / 305 (47.214%) | 646 / 603 / 43 (6.656%) | 646 / 341 / 305 (47.214%) | 646 / 603 / 43 (6.656%) |
| java-checks/src/main/java/org/sonar/java/checks/regex/AbstractRegexCheckTrackingMatchType.java | 111 / 47 / 64 (57.658%) | 111 / 95 / 16 (14.414%) | 111 / 47 / 64 (57.658%) | 111 / 95 / 16 (14.414%) |
| java-checks/src/main/java/org/sonar/java/checks/regex/AbstractRegexCheckTrackingMatchers.java | 566 / 251 / 315 (55.654%) | 566 / 492 / 74 (13.074%) | 566 / 251 / 315 (55.654%) | 566 / 492 / 74 (13.074%) |
| java-checks/src/main/java/org/sonar/java/checks/regex/AnchorPrecedenceCheck.java | 20 / 12 / 8 (40.000%) | 20 / 15 / 5 (25.000%) | 20 / 12 / 8 (40.000%) | 20 / 15 / 5 (25.000%) |
| java-checks/src/main/java/org/sonar/java/checks/regex/CanonEqFlagInRegexCheck.java | 152 / 76 / 76 (50.000%) | 152 / 133 / 19 (12.500%) | 152 / 76 / 76 (50.000%) | 152 / 133 / 19 (12.500%) |
| java-checks/src/main/java/org/sonar/java/checks/regex/DuplicatesInCharacterClassCheck.java | 20 / 12 / 8 (40.000%) | 20 / 15 / 5 (25.000%) | 20 / 12 / 8 (40.000%) | 20 / 15 / 5 (25.000%) |
| java-checks/src/main/java/org/sonar/java/checks/regex/EmptyLineRegexCheck.java | 356 / 149 / 207 (58.146%) | 356 / 302 / 54 (15.169%) | 356 / 149 / 207 (58.146%) | 356 / 302 / 54 (15.169%) |
| java-checks/src/main/java/org/sonar/java/checks/regex/EmptyRegexGroupCheck.java | 20 / 12 / 8 (40.000%) | 20 / 15 / 5 (25.000%) | 20 / 12 / 8 (40.000%) | 20 / 15 / 5 (25.000%) |
| java-checks/src/main/java/org/sonar/java/checks/regex/EmptyStringRepetitionCheck.java | 20 / 12 / 8 (40.000%) | 20 / 15 / 5 (25.000%) | 20 / 12 / 8 (40.000%) | 20 / 15 / 5 (25.000%) |
| java-checks/src/main/java/org/sonar/java/checks/regex/EscapeSequenceControlCharacterCheck.java | 40 / 29 / 11 (27.500%) | 40 / 32 / 8 (20.000%) | 40 / 29 / 11 (27.500%) | 40 / 32 / 8 (20.000%) |
| java-checks/src/main/java/org/sonar/java/checks/regex/GraphemeClustersInClassesCheck.java | 20 / 12 / 8 (40.000%) | 20 / 15 / 5 (25.000%) | 20 / 12 / 8 (40.000%) | 20 / 15 / 5 (25.000%) |
| java-checks/src/main/java/org/sonar/java/checks/regex/ImpossibleBackReferenceCheck.java | 20 / 12 / 8 (40.000%) | 20 / 15 / 5 (25.000%) | 20 / 12 / 8 (40.000%) | 20 / 15 / 5 (25.000%) |
| java-checks/src/main/java/org/sonar/java/checks/regex/ImpossibleBoundariesCheck.java | 20 / 12 / 8 (40.000%) | 20 / 15 / 5 (25.000%) | 20 / 12 / 8 (40.000%) | 20 / 15 / 5 (25.000%) |
| java-checks/src/main/java/org/sonar/java/checks/regex/InvalidRegexCheck.java | 69 / 38 / 31 (44.928%) | 69 / 46 / 23 (33.333%) | 69 / 38 / 31 (44.928%) | 69 / 46 / 23 (33.333%) |
| java-checks/src/main/java/org/sonar/java/checks/regex/MultipleWhitespaceCheck.java | 20 / 12 / 8 (40.000%) | 20 / 15 / 5 (25.000%) | 20 / 12 / 8 (40.000%) | 20 / 15 / 5 (25.000%) |
| java-checks/src/main/java/org/sonar/java/checks/regex/PossessiveQuantifierContinuationCheck.java | 22 / 13 / 9 (40.909%) | 22 / 16 / 6 (27.273%) | 22 / 13 / 9 (40.909%) | 22 / 16 / 6 (27.273%) |
| java-checks/src/main/java/org/sonar/java/checks/regex/RedosCheck.java | 33 / 24 / 9 (27.273%) | 33 / 31 / 2 (6.061%) | 33 / 24 / 9 (27.273%) | 33 / 31 / 2 (6.061%) |
| java-checks/src/main/java/org/sonar/java/checks/regex/RedundantRegexAlternativesCheck.java | 20 / 12 / 8 (40.000%) | 20 / 15 / 5 (25.000%) | 20 / 12 / 8 (40.000%) | 20 / 15 / 5 (25.000%) |
| java-checks/src/main/java/org/sonar/java/checks/regex/RegexComplexityCheck.java | 276 / 179 / 97 (35.145%) | 276 / 260 / 16 (5.797%) | 276 / 179 / 97 (35.145%) | 276 / 260 / 16 (5.797%) |
| java-checks/src/main/java/org/sonar/java/checks/regex/RegexLookaheadCheck.java | 25 / 15 / 10 (40.000%) | 25 / 18 / 7 (28.000%) | 25 / 15 / 10 (40.000%) | 25 / 18 / 7 (28.000%) |
| java-checks/src/main/java/org/sonar/java/checks/regex/RegexStackOverflowCheck.java | 411 / 268 / 143 (34.793%) | 411 / 273 / 138 (33.577%) | 411 / 268 / 143 (34.793%) | 411 / 273 / 138 (33.577%) |
| java-checks/src/main/java/org/sonar/java/checks/regex/ReluctantQuantifierCheck.java | 20 / 12 / 8 (40.000%) | 20 / 15 / 5 (25.000%) | 20 / 12 / 8 (40.000%) | 20 / 15 / 5 (25.000%) |
| java-checks/src/main/java/org/sonar/java/checks/regex/ReluctantQuantifierWithEmptyContinuationCheck.java | 23 / 14 / 9 (39.130%) | 23 / 17 / 6 (26.087%) | 23 / 14 / 9 (39.130%) | 23 / 17 / 6 (26.087%) |
| java-checks/src/main/java/org/sonar/java/checks/regex/SingleCharCharacterClassCheck.java | 20 / 12 / 8 (40.000%) | 20 / 15 / 5 (25.000%) | 20 / 12 / 8 (40.000%) | 20 / 15 / 5 (25.000%) |
| java-checks/src/main/java/org/sonar/java/checks/regex/SingleCharacterAlternationCheck.java | 20 / 12 / 8 (40.000%) | 20 / 15 / 5 (25.000%) | 20 / 12 / 8 (40.000%) | 20 / 15 / 5 (25.000%) |
| java-checks/src/main/java/org/sonar/java/checks/regex/StringReplaceCheck.java | 70 / 31 / 39 (55.714%) | 70 / 45 / 25 (35.714%) | 70 / 31 / 39 (55.714%) | 70 / 45 / 25 (35.714%) |
| java-checks/src/main/java/org/sonar/java/checks/regex/SuperLinearRegexCheck.java | 39 / 28 / 11 (28.205%) | 39 / 37 / 2 (5.128%) | 39 / 28 / 11 (28.205%) | 39 / 37 / 2 (5.128%) |
| java-checks/src/main/java/org/sonar/java/checks/regex/SuperfluousCurlyBraceCheck.java | 20 / 12 / 8 (40.000%) | 20 / 15 / 5 (25.000%) | 20 / 12 / 8 (40.000%) | 20 / 15 / 5 (25.000%) |
| java-checks/src/main/java/org/sonar/java/checks/regex/UnicodeAwareCharClassesCheck.java | 46 / 12 / 34 (73.913%) | 46 / 15 / 31 (67.391%) | 46 / 12 / 34 (73.913%) | 46 / 15 / 31 (67.391%) |
| java-checks/src/main/java/org/sonar/java/checks/regex/UnicodeCaseCheck.java | 111 / 74 / 37 (33.333%) | 111 / 92 / 19 (17.117%) | 111 / 74 / 37 (33.333%) | 111 / 92 / 19 (17.117%) |
| java-checks/src/main/java/org/sonar/java/checks/regex/UnquantifiedNonCapturingGroupCheck.java | 20 / 12 / 8 (40.000%) | 20 / 15 / 5 (25.000%) | 20 / 12 / 8 (40.000%) | 20 / 15 / 5 (25.000%) |
| java-checks/src/main/java/org/sonar/java/checks/regex/UnusedGroupNamesCheck.java | 367 / 214 / 153 (41.689%) | 367 / 298 / 69 (18.801%) | 367 / 214 / 153 (41.689%) | 367 / 298 / 69 (18.801%) |
| java-checks/src/main/java/org/sonar/java/checks/regex/VerboseRegexCheck.java | 20 / 12 / 8 (40.000%) | 20 / 15 / 5 (25.000%) | 20 / 12 / 8 (40.000%) | 20 / 15 / 5 (25.000%) |
| java-checks/src/main/java/org/sonar/java/checks/regex/package-info.java | 6 / 5 / 1 (16.667%) | 6 / 5 / 1 (16.667%) | 6 / 5 / 1 (16.667%) | 6 / 5 / 1 (16.667%) |
| java-checks/src/main/java/org/sonar/java/checks/security/AndroidBiometricAuthWithoutCryptoCheck.java | 34 / 12 / 22 (64.706%) | 34 / 32 / 2 (5.882%) | 34 / 12 / 22 (64.706%) | 34 / 32 / 2 (5.882%) |
| java-checks/src/main/java/org/sonar/java/checks/security/AndroidBroadcastingCheck.java | 161 / 79 / 82 (50.932%) | 161 / 159 / 2 (1.242%) | 161 / 79 / 82 (50.932%) | 161 / 159 / 2 (1.242%) |
| java-checks/src/main/java/org/sonar/java/checks/security/AndroidExternalStorageCheck.java | 32 / 12 / 20 (62.500%) | 32 / 30 / 2 (6.250%) | 32 / 12 / 20 (62.500%) | 32 / 30 / 2 (6.250%) |
| java-checks/src/main/java/org/sonar/java/checks/security/AndroidMobileDatabaseEncryptionKeysCheck.java | 197 / 84 / 113 (57.360%) | 197 / 194 / 3 (1.523%) | 197 / 84 / 113 (57.360%) | 197 / 194 / 3 (1.523%) |
| java-checks/src/main/java/org/sonar/java/checks/security/AndroidNonAuthenticatedUsersCheck.java | 190 / 88 / 102 (53.684%) | 190 / 188 / 2 (1.053%) | 190 / 88 / 102 (53.684%) | 190 / 188 / 2 (1.053%) |
| java-checks/src/main/java/org/sonar/java/checks/security/AndroidPersistentUniqueIdentifierCheck.java | 184 / 64 / 120 (65.217%) | 184 / 182 / 2 (1.087%) | 184 / 64 / 120 (65.217%) | 184 / 182 / 2 (1.087%) |
| java-checks/src/main/java/org/sonar/java/checks/security/AuthorizationsStrongDecisionsCheck.java | 187 / 114 / 73 (39.037%) | 187 / 185 / 2 (1.070%) | 187 / 114 / 73 (39.037%) | 187 / 185 / 2 (1.070%) |
| java-checks/src/main/java/org/sonar/java/checks/security/CipherBlockChainingCheck.java | 598 / 359 / 239 (39.967%) | 598 / 591 / 7 (1.171%) | 598 / 359 / 239 (39.967%) | 598 / 591 / 7 (1.171%) |
| java-checks/src/main/java/org/sonar/java/checks/security/ClearTextProtocolCheck.java | 185 / 113 / 72 (38.919%) | 185 / 183 / 2 (1.081%) | 185 / 113 / 72 (38.919%) | 185 / 183 / 2 (1.081%) |
| java-checks/src/main/java/org/sonar/java/checks/security/CookieHttpOnlyCheck.java | 873 / 480 / 393 (45.017%) | 873 / 869 / 4 (0.458%) | 873 / 480 / 393 (45.017%) | 873 / 869 / 4 (0.458%) |
| java-checks/src/main/java/org/sonar/java/checks/security/CryptographicKeySizeCheck.java | 200 / 115 / 85 (42.500%) | 200 / 178 / 22 (11.000%) | 200 / 115 / 85 (42.500%) | 200 / 178 / 22 (11.000%) |
| java-checks/src/main/java/org/sonar/java/checks/security/DataHashingCheck.java | 873 / 530 / 343 (39.290%) | 873 / 850 / 23 (2.635%) | 873 / 530 / 343 (39.290%) | 873 / 850 / 23 (2.635%) |
| java-checks/src/main/java/org/sonar/java/checks/security/DebugFeatureEnabledCheck.java | 202 / 85 / 117 (57.921%) | 202 / 199 / 3 (1.485%) | 202 / 85 / 117 (57.921%) | 202 / 199 / 3 (1.485%) |
| java-checks/src/main/java/org/sonar/java/checks/security/DisableAutoEscapingCheck.java | 208 / 106 / 102 (49.038%) | 208 / 206 / 2 (0.962%) | 208 / 106 / 102 (49.038%) | 208 / 206 / 2 (0.962%) |
| java-checks/src/main/java/org/sonar/java/checks/security/DisclosingTechnologyFingerprintsCheck.java | 82 / 30 / 52 (63.415%) | 82 / 80 / 2 (2.439%) | 82 / 30 / 52 (63.415%) | 82 / 80 / 2 (2.439%) |
| java-checks/src/main/java/org/sonar/java/checks/security/EmptyDatabasePasswordCheck.java | 105 / 63 / 42 (40.000%) | 105 / 103 / 2 (1.905%) | 105 / 63 / 42 (40.000%) | 105 / 103 / 2 (1.905%) |
| java-checks/src/main/java/org/sonar/java/checks/security/EncryptionAlgorithmCheck.java | 137 / 96 / 41 (29.927%) | 137 / 135 / 2 (1.460%) | 137 / 96 / 41 (29.927%) | 137 / 135 / 2 (1.460%) |
| java-checks/src/main/java/org/sonar/java/checks/security/ExcessiveContentRequestCheck.java | 583 / 378 / 205 (35.163%) | 583 / 563 / 20 (3.431%) | 583 / 378 / 205 (35.163%) | 583 / 563 / 20 (3.431%) |
| java-checks/src/main/java/org/sonar/java/checks/security/FilePermissionsCheck.java | 253 / 149 / 104 (41.107%) | 253 / 251 / 2 (0.791%) | 253 / 149 / 104 (41.107%) | 253 / 251 / 2 (0.791%) |
| java-checks/src/main/java/org/sonar/java/checks/security/IntegerToHexStringCheck.java | 105 / 29 / 76 (72.381%) | 105 / 103 / 2 (1.905%) | 105 / 29 / 76 (72.381%) | 105 / 103 / 2 (1.905%) |
| java-checks/src/main/java/org/sonar/java/checks/security/JWTWithStrongCipherCheck.java | 238 / 108 / 130 (54.622%) | 238 / 236 / 2 (0.840%) | 238 / 108 / 130 (54.622%) | 238 / 236 / 2 (0.840%) |
| java-checks/src/main/java/org/sonar/java/checks/security/LDAPAuthenticatedConnectionCheck.java | 76 / 36 / 40 (52.632%) | 76 / 74 / 2 (2.632%) | 76 / 36 / 40 (52.632%) | 76 / 74 / 2 (2.632%) |
| java-checks/src/main/java/org/sonar/java/checks/security/LDAPDeserializationCheck.java | 81 / 46 / 35 (43.210%) | 81 / 79 / 2 (2.469%) | 81 / 46 / 35 (43.210%) | 81 / 79 / 2 (2.469%) |
| java-checks/src/main/java/org/sonar/java/checks/security/OpenSAML2AuthenticationBypassCheck.java | 38 / 18 / 20 (52.632%) | 38 / 36 / 2 (5.263%) | 38 / 18 / 20 (52.632%) | 38 / 36 / 2 (5.263%) |
| java-checks/src/main/java/org/sonar/java/checks/security/PasswordEncoderCheck.java | 410 / 169 / 241 (58.780%) | 410 / 408 / 2 (0.488%) | 410 / 169 / 241 (58.780%) | 410 / 408 / 2 (0.488%) |
| java-checks/src/main/java/org/sonar/java/checks/security/PubliclyWritableDirectoriesCheck.java | 264 / 112 / 152 (57.576%) | 264 / 260 / 4 (1.515%) | 264 / 112 / 152 (57.576%) | 264 / 260 / 4 (1.515%) |
| java-checks/src/main/java/org/sonar/java/checks/security/ReceivingIntentsCheck.java | 33 / 14 / 19 (57.576%) | 33 / 31 / 2 (6.061%) | 33 / 14 / 19 (57.576%) | 33 / 31 / 2 (6.061%) |
| java-checks/src/main/java/org/sonar/java/checks/security/SecureCookieCheck.java | 880 / 457 / 423 (48.068%) | 880 / 877 / 3 (0.341%) | 880 / 457 / 423 (48.068%) | 880 / 877 / 3 (0.341%) |
| java-checks/src/main/java/org/sonar/java/checks/security/ServerCertificatesCheck.java | 124 / 73 / 51 (41.129%) | 124 / 122 / 2 (1.613%) | 124 / 73 / 51 (41.129%) | 124 / 122 / 2 (1.613%) |
| java-checks/src/main/java/org/sonar/java/checks/security/UnpredictableSaltCheck.java | 188 / 71 / 117 (62.234%) | 188 / 186 / 2 (1.064%) | 188 / 71 / 117 (62.234%) | 188 / 186 / 2 (1.064%) |
| java-checks/src/main/java/org/sonar/java/checks/security/UserEnumerationCheck.java | 199 / 113 / 86 (43.216%) | 199 / 197 / 2 (1.005%) | 199 / 113 / 86 (43.216%) | 199 / 197 / 2 (1.005%) |
| java-checks/src/main/java/org/sonar/java/checks/security/VerifiedServerHostnamesCheck.java | 367 / 187 / 180 (49.046%) | 367 / 363 / 4 (1.090%) | 367 / 187 / 180 (49.046%) | 367 / 363 / 4 (1.090%) |
| java-checks/src/main/java/org/sonar/java/checks/security/WebViewJavaScriptInterfaceCheck.java | 37 / 19 / 18 (48.649%) | 37 / 35 / 2 (5.405%) | 37 / 19 / 18 (48.649%) | 37 / 35 / 2 (5.405%) |
| java-checks/src/main/java/org/sonar/java/checks/security/WebViewJavaScriptSupportCheck.java | 45 / 22 / 23 (51.111%) | 45 / 43 / 2 (4.444%) | 45 / 22 / 23 (51.111%) | 45 / 43 / 2 (4.444%) |
| java-checks/src/main/java/org/sonar/java/checks/security/WebViewsFileAccessCheck.java | 45 / 22 / 23 (51.111%) | 45 / 43 / 2 (4.444%) | 45 / 22 / 23 (51.111%) | 45 / 43 / 2 (4.444%) |
| java-checks/src/main/java/org/sonar/java/checks/security/XmlRpcExtensionsCheck.java | 35 / 18 / 17 (48.571%) | 35 / 33 / 2 (5.714%) | 35 / 18 / 17 (48.571%) | 35 / 33 / 2 (5.714%) |
| java-checks/src/main/java/org/sonar/java/checks/security/XxeActiveMQCheck.java | 144 / 81 / 63 (43.750%) | 144 / 141 / 3 (2.083%) | 144 / 81 / 63 (43.750%) | 144 / 141 / 3 (2.083%) |
| java-checks/src/main/java/org/sonar/java/checks/security/ZipEntryCheck.java | 121 / 52 / 69 (57.025%) | 121 / 119 / 2 (1.653%) | 121 / 52 / 69 (57.025%) | 121 / 119 / 2 (1.653%) |
| java-checks/src/main/java/org/sonar/java/checks/security/package-info.java | 6 / 5 / 1 (16.667%) | 6 / 5 / 1 (16.667%) | 6 / 5 / 1 (16.667%) | 6 / 5 / 1 (16.667%) |
| java-checks/src/main/java/org/sonar/java/checks/serialization/BlindSerialVersionUidCheck.java | 86 / 33 / 53 (61.628%) | 86 / 84 / 2 (2.326%) | 86 / 33 / 53 (61.628%) | 86 / 84 / 2 (2.326%) |
| java-checks/src/main/java/org/sonar/java/checks/serialization/CustomSerializationMethodCheck.java | 162 / 96 / 66 (40.741%) | 162 / 160 / 2 (1.235%) | 162 / 96 / 66 (40.741%) | 162 / 160 / 2 (1.235%) |
| java-checks/src/main/java/org/sonar/java/checks/serialization/ExternalizableClassConstructorCheck.java | 83 / 43 / 40 (48.193%) | 83 / 81 / 2 (2.410%) | 83 / 43 / 40 (48.193%) | 83 / 81 / 2 (2.410%) |
| java-checks/src/main/java/org/sonar/java/checks/serialization/NonSerializableWriteCheck.java | 116 / 53 / 63 (54.310%) | 116 / 114 / 2 (1.724%) | 116 / 53 / 63 (54.310%) | 116 / 114 / 2 (1.724%) |
| java-checks/src/main/java/org/sonar/java/checks/serialization/PrivateReadResolveCheck.java | 65 / 33 / 32 (49.231%) | 65 / 63 / 2 (3.077%) | 65 / 33 / 32 (49.231%) | 65 / 63 / 2 (3.077%) |
| java-checks/src/main/java/org/sonar/java/checks/serialization/RecordSerializationIgnoredMembersCheck.java | 133 / 72 / 61 (45.865%) | 133 / 131 / 2 (1.504%) | 133 / 72 / 61 (45.865%) | 133 / 131 / 2 (1.504%) |
| java-checks/src/main/java/org/sonar/java/checks/serialization/SerialVersionUidCheck.java | 156 / 95 / 61 (39.103%) | 156 / 154 / 2 (1.282%) | 156 / 95 / 61 (39.103%) | 156 / 154 / 2 (1.282%) |
| java-checks/src/main/java/org/sonar/java/checks/serialization/SerialVersionUidInRecordCheck.java | 93 / 57 / 36 (38.710%) | 93 / 91 / 2 (2.151%) | 93 / 57 / 36 (38.710%) | 93 / 91 / 2 (2.151%) |
| java-checks/src/main/java/org/sonar/java/checks/serialization/SerializableComparatorCheck.java | 49 / 26 / 23 (46.939%) | 49 / 47 / 2 (4.082%) | 49 / 26 / 23 (46.939%) | 49 / 47 / 2 (4.082%) |
| java-checks/src/main/java/org/sonar/java/checks/serialization/SerializableContract.java | 117 / 71 / 46 (39.316%) | 117 / 115 / 2 (1.709%) | 117 / 71 / 46 (39.316%) | 117 / 115 / 2 (1.709%) |
| java-checks/src/main/java/org/sonar/java/checks/serialization/SerializableFieldInSerializableClassCheck.java | 436 / 209 / 227 (52.064%) | 436 / 432 / 4 (0.917%) | 436 / 209 / 227 (52.064%) | 436 / 432 / 4 (0.917%) |
| java-checks/src/main/java/org/sonar/java/checks/serialization/SerializableObjectInSessionCheck.java | 48 / 26 / 22 (45.833%) | 48 / 46 / 2 (4.167%) | 48 / 26 / 22 (45.833%) | 48 / 46 / 2 (4.167%) |
| java-checks/src/main/java/org/sonar/java/checks/serialization/SerializableSuperConstructorCheck.java | 163 / 74 / 89 (54.601%) | 163 / 160 / 3 (1.840%) | 163 / 74 / 89 (54.601%) | 163 / 160 / 3 (1.840%) |
| java-checks/src/main/java/org/sonar/java/checks/serialization/package-info.java | 6 / 5 / 1 (16.667%) | 6 / 5 / 1 (16.667%) | 6 / 5 / 1 (16.667%) | 6 / 5 / 1 (16.667%) |
| java-checks/src/main/java/org/sonar/java/checks/spring/AmbiguousDependencyCheck.java | 292 / 220 / 72 (24.658%) | 292 / 289 / 3 (1.027%) | 292 / 220 / 72 (24.658%) | 292 / 289 / 3 (1.027%) |
| java-checks/src/main/java/org/sonar/java/checks/spring/AsyncMethodsCalledViaThisCheck.java | 69 / 40 / 29 (42.029%) | 69 / 67 / 2 (2.899%) | 69 / 40 / 29 (42.029%) | 69 / 67 / 2 (2.899%) |
| java-checks/src/main/java/org/sonar/java/checks/spring/AsyncMethodsOnConfigurationClassCheck.java | 88 / 21 / 67 (76.136%) | 88 / 86 / 2 (2.273%) | 88 / 21 / 67 (76.136%) | 88 / 86 / 2 (2.273%) |
| java-checks/src/main/java/org/sonar/java/checks/spring/AsyncMethodsReturnTypeCheck.java | 48 / 24 / 24 (50.000%) | 48 / 46 / 2 (4.167%) | 48 / 24 / 24 (50.000%) | 48 / 46 / 2 (4.167%) |
| java-checks/src/main/java/org/sonar/java/checks/spring/AutowiredOnConstructorWhenMultipleConstructorsCheck.java | 95 / 31 / 64 (67.368%) | 95 / 93 / 2 (2.105%) | 95 / 31 / 64 (67.368%) | 95 / 93 / 2 (2.105%) |
| java-checks/src/main/java/org/sonar/java/checks/spring/AutowiredOnMultipleConstructorsCheck.java | 115 / 46 / 69 (60.000%) | 115 / 113 / 2 (1.739%) | 115 / 46 / 69 (60.000%) | 115 / 113 / 2 (1.739%) |
| java-checks/src/main/java/org/sonar/java/checks/spring/AvoidQualifierOnBeanMethodsCheck.java | 153 / 79 / 74 (48.366%) | 153 / 151 / 2 (1.307%) | 153 / 79 / 74 (48.366%) | 153 / 151 / 2 (1.307%) |
| java-checks/src/main/java/org/sonar/java/checks/spring/BeforeAndAfterTransactionContractCheck.java | 161 / 93 / 68 (42.236%) | 161 / 159 / 2 (1.242%) | 161 / 93 / 68 (42.236%) | 161 / 159 / 2 (1.242%) |
| java-checks/src/main/java/org/sonar/java/checks/spring/CacheAnnotationsShouldOnlyBeAppliedToConcreteClassesCheck.java | 106 / 33 / 73 (68.868%) | 106 / 104 / 2 (1.887%) | 106 / 33 / 73 (68.868%) | 106 / 104 / 2 (1.887%) |
| java-checks/src/main/java/org/sonar/java/checks/spring/ConfigurationClassShouldNotBeFinalCheck.java | 110 / 46 / 64 (58.182%) | 110 / 108 / 2 (1.818%) | 110 / 46 / 64 (58.182%) | 110 / 108 / 2 (1.818%) |
| java-checks/src/main/java/org/sonar/java/checks/spring/ControllerWithRestControllerReplacementCheck.java | 176 / 78 / 98 (55.682%) | 176 / 174 / 2 (1.136%) | 176 / 78 / 98 (55.682%) | 176 / 174 / 2 (1.136%) |
| java-checks/src/main/java/org/sonar/java/checks/spring/ControllerWithSessionAttributesCheck.java | 83 / 38 / 45 (54.217%) | 83 / 81 / 2 (2.410%) | 83 / 38 / 45 (54.217%) | 83 / 81 / 2 (2.410%) |
| java-checks/src/main/java/org/sonar/java/checks/spring/DirectBeanMethodInvocationWithoutProxyCheck.java | 185 / 82 / 103 (55.676%) | 185 / 183 / 2 (1.081%) | 185 / 82 / 103 (55.676%) | 185 / 183 / 2 (1.081%) |
| java-checks/src/main/java/org/sonar/java/checks/spring/DirtyContextShouldUseCorrectControlModeCheck.java | 89 / 55 / 34 (38.202%) | 89 / 87 / 2 (2.247%) | 89 / 55 / 34 (38.202%) | 89 / 87 / 2 (2.247%) |
| java-checks/src/main/java/org/sonar/java/checks/spring/EventListenerMethodOneArgCheck.java | 51 / 24 / 27 (52.941%) | 51 / 49 / 2 (3.922%) | 51 / 24 / 27 (52.941%) | 51 / 49 / 2 (3.922%) |
| java-checks/src/main/java/org/sonar/java/checks/spring/FieldDependencyInjectionCheck.java | 58 / 17 / 41 (70.690%) | 58 / 56 / 2 (3.448%) | 58 / 17 / 41 (70.690%) | 58 / 56 / 2 (3.448%) |
| java-checks/src/main/java/org/sonar/java/checks/spring/InitBinderMethodsMustBeVoidCheck.java | 55 / 27 / 28 (50.909%) | 55 / 53 / 2 (3.636%) | 55 / 27 / 28 (50.909%) | 55 / 53 / 2 (3.636%) |
| java-checks/src/main/java/org/sonar/java/checks/spring/MissingPathVariableAnnotationCheck.java | 906 / 656 / 250 (27.594%) | 906 / 902 / 4 (0.442%) | 906 / 656 / 250 (27.594%) | 906 / 902 / 4 (0.442%) |
| java-checks/src/main/java/org/sonar/java/checks/spring/ModelAttributeNamingConventionForSpELCheck.java | 159 / 84 / 75 (47.170%) | 159 / 157 / 2 (1.258%) | 159 / 84 / 75 (47.170%) | 159 / 157 / 2 (1.258%) |
| java-checks/src/main/java/org/sonar/java/checks/spring/NonSingletonAutowiredInSingletonCheck.java | 360 / 156 / 204 (56.667%) | 360 / 357 / 3 (0.833%) | 360 / 156 / 204 (56.667%) | 360 / 357 / 3 (0.833%) |
| java-checks/src/main/java/org/sonar/java/checks/spring/NullableInjectedFieldsHaveDefaultValueCheck.java | 399 / 190 / 209 (52.381%) | 399 / 395 / 4 (1.003%) | 399 / 190 / 209 (52.381%) | 399 / 395 / 4 (1.003%) |
| java-checks/src/main/java/org/sonar/java/checks/spring/OptionalRestParametersShouldBeObjectsCheck.java | 140 / 53 / 87 (62.143%) | 140 / 138 / 2 (1.429%) | 140 / 53 / 87 (62.143%) | 140 / 138 / 2 (1.429%) |
| java-checks/src/main/java/org/sonar/java/checks/spring/PersistentEntityUsedAsRequestParameterCheck.java | 121 / 66 / 55 (45.455%) | 121 / 119 / 2 (1.653%) | 121 / 66 / 55 (45.455%) | 121 / 119 / 2 (1.653%) |
| java-checks/src/main/java/org/sonar/java/checks/spring/RedundantSpringAnnotationCheck.java | 320 / 196 / 124 (38.750%) | 320 / 318 / 2 (0.625%) | 320 / 196 / 124 (38.750%) | 320 / 318 / 2 (0.625%) |
| java-checks/src/main/java/org/sonar/java/checks/spring/RequestMappingMethodPublicCheck.java | 76 / 45 / 31 (40.789%) | 76 / 74 / 2 (2.632%) | 76 / 45 / 31 (40.789%) | 76 / 74 / 2 (2.632%) |
| java-checks/src/main/java/org/sonar/java/checks/spring/ScheduledOnlyOnNoArgMethodCheck.java | 73 / 33 / 40 (54.795%) | 73 / 71 / 2 (2.740%) | 73 / 33 / 40 (54.795%) | 73 / 71 / 2 (2.740%) |
| java-checks/src/main/java/org/sonar/java/checks/spring/SpelExpressionCheck.java | 569 / 473 / 96 (16.872%) | 569 / 562 / 7 (1.230%) | 569 / 473 / 96 (16.872%) | 569 / 562 / 7 (1.230%) |
| java-checks/src/main/java/org/sonar/java/checks/spring/SpringAntMatcherOrderCheck.java | 235 / 182 / 53 (22.553%) | 235 / 231 / 4 (1.702%) | 235 / 182 / 53 (22.553%) | 235 / 231 / 4 (1.702%) |
| java-checks/src/main/java/org/sonar/java/checks/spring/SpringAutoConfigurationCheck.java | 104 / 52 / 52 (50.000%) | 104 / 102 / 2 (1.923%) | 104 / 52 / 52 (50.000%) | 104 / 102 / 2 (1.923%) |
| java-checks/src/main/java/org/sonar/java/checks/spring/SpringBeanNamingConventionCheck.java | 128 / 64 / 64 (50.000%) | 128 / 124 / 4 (3.125%) | 128 / 64 / 64 (50.000%) | 128 / 124 / 4 (3.125%) |
| java-checks/src/main/java/org/sonar/java/checks/spring/SpringBeansShouldBeAccessibleCheck.java | 403 / 248 / 155 (38.462%) | 403 / 390 / 13 (3.226%) | 403 / 248 / 155 (38.462%) | 403 / 390 / 13 (3.226%) |
| java-checks/src/main/java/org/sonar/java/checks/spring/SpringCacheableWithCachePutCheck.java | 184 / 106 / 78 (42.391%) | 184 / 182 / 2 (1.087%) | 184 / 106 / 78 (42.391%) | 184 / 182 / 2 (1.087%) |
| java-checks/src/main/java/org/sonar/java/checks/spring/SpringComponentSpecializationCheck.java | 256 / 154 / 102 (39.844%) | 256 / 253 / 3 (1.172%) | 256 / 154 / 102 (39.844%) | 256 / 253 / 3 (1.172%) |
| java-checks/src/main/java/org/sonar/java/checks/spring/SpringComponentWithNonAutowiredMembersCheck.java | 276 / 111 / 165 (59.783%) | 276 / 270 / 6 (2.174%) | 276 / 111 / 165 (59.783%) | 276 / 270 / 6 (2.174%) |
| java-checks/src/main/java/org/sonar/java/checks/spring/SpringComponentWithWrongScopeCheck.java | 75 / 31 / 44 (58.667%) | 75 / 73 / 2 (2.667%) | 75 / 31 / 44 (58.667%) | 75 / 73 / 2 (2.667%) |
| java-checks/src/main/java/org/sonar/java/checks/spring/SpringComposedRequestMappingCheck.java | 174 / 96 / 78 (44.828%) | 174 / 172 / 2 (1.149%) | 174 / 96 / 78 (44.828%) | 174 / 172 / 2 (1.149%) |
| java-checks/src/main/java/org/sonar/java/checks/spring/SpringConfigurationWithAutowiredFieldsCheck.java | 230 / 88 / 142 (61.739%) | 230 / 228 / 2 (0.870%) | 230 / 88 / 142 (61.739%) | 230 / 228 / 2 (0.870%) |
| java-checks/src/main/java/org/sonar/java/checks/spring/SpringConstructorInjectionCheck.java | 192 / 74 / 118 (61.458%) | 192 / 190 / 2 (1.042%) | 192 / 74 / 118 (61.458%) | 192 / 190 / 2 (1.042%) |
| java-checks/src/main/java/org/sonar/java/checks/spring/SpringContextCheck.java | 11 / 8 / 3 (27.273%) | 11 / 11 / 0 (0.000%) | 11 / 8 / 3 (27.273%) | 11 / 11 / 0 (0.000%) |
| java-checks/src/main/java/org/sonar/java/checks/spring/SpringContextChecks.java | 13 / 10 / 3 (23.077%) | 13 / 13 / 0 (0.000%) | 13 / 10 / 3 (23.077%) | 13 / 13 / 0 (0.000%) |
| java-checks/src/main/java/org/sonar/java/checks/spring/SpringContextIssue.java | 10 / 9 / 1 (10.000%) | 10 / 10 / 0 (0.000%) | 10 / 9 / 1 (10.000%) | 10 / 10 / 0 (0.000%) |
| java-checks/src/main/java/org/sonar/java/checks/spring/SpringIncompatibleTransactionalCheck.java | 439 / 301 / 138 (31.435%) | 439 / 435 / 4 (0.911%) | 439 / 301 / 138 (31.435%) | 439 / 435 / 4 (0.911%) |
| java-checks/src/main/java/org/sonar/java/checks/spring/SpringRequestMappingMethodCheck.java | 158 / 54 / 104 (65.823%) | 158 / 156 / 2 (1.266%) | 158 / 54 / 104 (65.823%) | 158 / 156 / 2 (1.266%) |
| java-checks/src/main/java/org/sonar/java/checks/spring/SpringScanDefaultPackageCheck.java | 269 / 140 / 129 (47.955%) | 269 / 267 / 2 (0.743%) | 269 / 140 / 129 (47.955%) | 269 / 267 / 2 (0.743%) |
| java-checks/src/main/java/org/sonar/java/checks/spring/SpringSecurityDisableCSRFCheck.java | 67 / 34 / 33 (49.254%) | 67 / 65 / 2 (2.985%) | 67 / 34 / 33 (49.254%) | 67 / 65 / 2 (2.985%) |
| java-checks/src/main/java/org/sonar/java/checks/spring/SpringSessionFixationCheck.java | 37 / 20 / 17 (45.946%) | 37 / 35 / 2 (5.405%) | 37 / 20 / 17 (45.946%) | 37 / 35 / 2 (5.405%) |
| java-checks/src/main/java/org/sonar/java/checks/spring/StaticFieldInjectionNotSupportedCheck.java | 159 / 55 / 104 (65.409%) | 159 / 157 / 2 (1.258%) | 159 / 55 / 104 (65.409%) | 159 / 157 / 2 (1.258%) |
| java-checks/src/main/java/org/sonar/java/checks/spring/StatusCodesOnResponseCheck.java | 287 / 170 / 117 (40.767%) | 287 / 285 / 2 (0.697%) | 287 / 170 / 117 (40.767%) | 287 / 285 / 2 (0.697%) |
| java-checks/src/main/java/org/sonar/java/checks/spring/SuperfluousResponseBodyAnnotationCheck.java | 61 / 17 / 44 (72.131%) | 61 / 59 / 2 (3.279%) | 61 / 17 / 44 (72.131%) | 61 / 59 / 2 (3.279%) |
| java-checks/src/main/java/org/sonar/java/checks/spring/TransactionalMethodCheckedExceptionCheck.java | 554 / 256 / 298 (53.791%) | 554 / 552 / 2 (0.361%) | 554 / 256 / 298 (53.791%) | 554 / 552 / 2 (0.361%) |
| java-checks/src/main/java/org/sonar/java/checks/spring/TransactionalMethodVisibilityCheck.java | 120 / 69 / 51 (42.500%) | 120 / 118 / 2 (1.667%) | 120 / 69 / 51 (42.500%) | 120 / 118 / 2 (1.667%) |
| java-checks/src/main/java/org/sonar/java/checks/spring/UsePageableParameterForPagedQueryCheck.java | 111 / 63 / 48 (43.243%) | 111 / 109 / 2 (1.802%) | 111 / 63 / 48 (43.243%) | 111 / 109 / 2 (1.802%) |
| java-checks/src/main/java/org/sonar/java/checks/spring/ValueAnnotationShouldInjectPropertyOrSpELCheck.java | 143 / 73 / 70 (48.951%) | 143 / 141 / 2 (1.399%) | 143 / 73 / 70 (48.951%) | 143 / 141 / 2 (1.399%) |
| java-checks/src/main/java/org/sonar/java/checks/spring/package-info.java | 6 / 5 / 1 (16.667%) | 6 / 5 / 1 (16.667%) | 6 / 5 / 1 (16.667%) | 6 / 5 / 1 (16.667%) |
| java-checks/src/main/java/org/sonar/java/checks/sustainability/AndroidExactAlarmCheck.java | 92 / 63 / 29 (31.522%) | 92 / 90 / 2 (2.174%) | 92 / 63 / 29 (31.522%) | 92 / 90 / 2 (2.174%) |
| java-checks/src/main/java/org/sonar/java/checks/sustainability/AndroidFusedLocationProviderClientCheck.java | 39 / 20 / 19 (48.718%) | 39 / 37 / 2 (5.128%) | 39 / 20 / 19 (48.718%) | 39 / 37 / 2 (5.128%) |
| java-checks/src/main/java/org/sonar/java/checks/sustainability/package-info.java | 6 / 5 / 1 (16.667%) | 6 / 5 / 1 (16.667%) | 6 / 5 / 1 (16.667%) | 6 / 5 / 1 (16.667%) |
| java-checks/src/main/java/org/sonar/java/checks/synchronization/DoubleCheckedLockingCheck.java | 329 / 199 / 130 (39.514%) | 329 / 327 / 2 (0.608%) | 329 / 199 / 130 (39.514%) | 329 / 327 / 2 (0.608%) |
| java-checks/src/main/java/org/sonar/java/checks/synchronization/S9399Check.java | 274 / 136 / 138 (50.365%) | 274 / 272 / 2 (0.730%) | 274 / 136 / 138 (50.365%) | 274 / 272 / 2 (0.730%) |
| java-checks/src/main/java/org/sonar/java/checks/synchronization/SynchronizationOnGetClassCheck.java | 110 / 52 / 58 (52.727%) | 110 / 108 / 2 (1.818%) | 110 / 52 / 58 (52.727%) | 110 / 108 / 2 (1.818%) |
| java-checks/src/main/java/org/sonar/java/checks/synchronization/TwoLocksWaitCheck.java | 223 / 131 / 92 (41.256%) | 223 / 221 / 2 (0.897%) | 223 / 131 / 92 (41.256%) | 223 / 221 / 2 (0.897%) |
| java-checks/src/main/java/org/sonar/java/checks/synchronization/ValueBasedObjectUsedForLockCheck.java | 57 / 31 / 26 (45.614%) | 57 / 55 / 2 (3.509%) | 57 / 31 / 26 (45.614%) | 57 / 55 / 2 (3.509%) |
| java-checks/src/main/java/org/sonar/java/checks/synchronization/WriteObjectTheOnlySynchronizedMethodCheck.java | 96 / 56 / 40 (41.667%) | 96 / 94 / 2 (2.083%) | 96 / 56 / 40 (41.667%) | 96 / 94 / 2 (2.083%) |
| java-checks/src/main/java/org/sonar/java/checks/synchronization/package-info.java | 6 / 5 / 1 (16.667%) | 6 / 5 / 1 (16.667%) | 6 / 5 / 1 (16.667%) | 6 / 5 / 1 (16.667%) |
| java-checks/src/main/java/org/sonar/java/checks/tests/AbstractMockitoArgumentChecker.java | 102 / 31 / 71 (69.608%) | 102 / 102 / 0 (0.000%) | 102 / 31 / 71 (69.608%) | 102 / 102 / 0 (0.000%) |
| java-checks/src/main/java/org/sonar/java/checks/tests/AbstractOneExpectedExceptionRule.java | 331 / 136 / 195 (58.912%) | 331 / 331 / 0 (0.000%) | 331 / 136 / 195 (58.912%) | 331 / 331 / 0 (0.000%) |
| java-checks/src/main/java/org/sonar/java/checks/tests/AssertJApplyConfigurationCheck.java | 74 / 35 / 39 (52.703%) | 74 / 72 / 2 (2.703%) | 74 / 35 / 39 (52.703%) | 74 / 72 / 2 (2.703%) |
| java-checks/src/main/java/org/sonar/java/checks/tests/AssertJAssertionsInConsumerCheck.java | 208 / 105 / 103 (49.519%) | 208 / 205 / 3 (1.442%) | 208 / 105 / 103 (49.519%) | 208 / 205 / 3 (1.442%) |
| java-checks/src/main/java/org/sonar/java/checks/tests/AssertJChainSimplificationCheck.java | 209 / 70 / 139 (66.507%) | 209 / 207 / 2 (0.957%) | 209 / 70 / 139 (66.507%) | 209 / 207 / 2 (0.957%) |
| java-checks/src/main/java/org/sonar/java/checks/tests/AssertJChainSimplificationHelper.java | 73 / 42 / 31 (42.466%) | 73 / 73 / 0 (0.000%) | 73 / 42 / 31 (42.466%) | 73 / 73 / 0 (0.000%) |
| java-checks/src/main/java/org/sonar/java/checks/tests/AssertJChainSimplificationIndex.java | 1893 / 1219 / 674 (35.605%) | 1893 / 1863 / 30 (1.585%) | 1893 / 1219 / 674 (35.605%) | 1893 / 1863 / 30 (1.585%) |
| java-checks/src/main/java/org/sonar/java/checks/tests/AssertJChainSimplificationQuickFix.java | 314 / 180 / 134 (42.675%) | 314 / 314 / 0 (0.000%) | 314 / 180 / 134 (42.675%) | 314 / 314 / 0 (0.000%) |
| java-checks/src/main/java/org/sonar/java/checks/tests/AssertJConsecutiveAssertionCheck.java | 284 / 161 / 123 (43.310%) | 284 / 281 / 3 (1.056%) | 284 / 161 / 123 (43.310%) | 284 / 281 / 3 (1.056%) |
| java-checks/src/main/java/org/sonar/java/checks/tests/AssertJContextBeforeAssertionCheck.java | 56 / 20 / 36 (64.286%) | 56 / 54 / 2 (3.571%) | 56 / 20 / 36 (64.286%) | 56 / 54 / 2 (3.571%) |
| java-checks/src/main/java/org/sonar/java/checks/tests/AssertJTestForEmptinessCheck.java | 220 / 77 / 143 (65.000%) | 220 / 218 / 2 (0.909%) | 220 / 77 / 143 (65.000%) | 220 / 218 / 2 (0.909%) |
| java-checks/src/main/java/org/sonar/java/checks/tests/AssertThatThrownByAloneCheck.java | 32 / 13 / 19 (59.375%) | 32 / 30 / 2 (6.250%) | 32 / 13 / 19 (59.375%) | 32 / 30 / 2 (6.250%) |
| java-checks/src/main/java/org/sonar/java/checks/tests/AssertTrueInsteadOfDedicatedAssertCheck.java | 255 / 167 / 88 (34.510%) | 255 / 244 / 11 (4.314%) | 255 / 167 / 88 (34.510%) | 255 / 244 / 11 (4.314%) |
| java-checks/src/main/java/org/sonar/java/checks/tests/AssertionArgumentOrderCheck.java | 481 / 235 / 246 (51.143%) | 481 / 479 / 2 (0.416%) | 481 / 235 / 246 (51.143%) | 481 / 479 / 2 (0.416%) |
| java-checks/src/main/java/org/sonar/java/checks/tests/AssertionCompareToSelfCheck.java | 273 / 107 / 166 (60.806%) | 273 / 269 / 4 (1.465%) | 273 / 107 / 166 (60.806%) | 273 / 269 / 4 (1.465%) |
| java-checks/src/main/java/org/sonar/java/checks/tests/AssertionFailInCatchBlockCheck.java | 41 / 23 / 18 (43.902%) | 41 / 39 / 2 (4.878%) | 41 / 23 / 18 (43.902%) | 41 / 39 / 2 (4.878%) |
| java-checks/src/main/java/org/sonar/java/checks/tests/AssertionInThreadRunCheck.java | 68 / 36 / 32 (47.059%) | 68 / 66 / 2 (2.941%) | 68 / 36 / 32 (47.059%) | 68 / 66 / 2 (2.941%) |
| java-checks/src/main/java/org/sonar/java/checks/tests/AssertionInTryCatchCheck.java | 108 / 44 / 64 (59.259%) | 108 / 106 / 2 (1.852%) | 108 / 44 / 64 (59.259%) | 108 / 106 / 2 (1.852%) |
| java-checks/src/main/java/org/sonar/java/checks/tests/AssertionTypesCheck.java | 793 / 423 / 370 (46.658%) | 793 / 791 / 2 (0.252%) | 793 / 423 / 370 (46.658%) | 793 / 791 / 2 (0.252%) |
| java-checks/src/main/java/org/sonar/java/checks/tests/AssertionsCompletenessCheck.java | 505 / 254 / 251 (49.703%) | 505 / 503 / 2 (0.396%) | 505 / 254 / 251 (49.703%) | 505 / 503 / 2 (0.396%) |
| java-checks/src/main/java/org/sonar/java/checks/tests/AssertionsInTestsCheck.java | 378 / 221 / 157 (41.534%) | 378 / 366 / 12 (3.175%) | 378 / 221 / 157 (41.534%) | 378 / 366 / 12 (3.175%) |
| java-checks/src/main/java/org/sonar/java/checks/tests/AssertionsWithoutMessageCheck.java | 435 / 236 / 199 (45.747%) | 435 / 421 / 14 (3.218%) | 435 / 236 / 199 (45.747%) | 435 / 421 / 14 (3.218%) |
| java-checks/src/main/java/org/sonar/java/checks/tests/BooleanOrNullLiteralInAssertionsCheck.java | 337 / 173 / 164 (48.665%) | 337 / 335 / 2 (0.593%) | 337 / 173 / 164 (48.665%) | 337 / 335 / 2 (0.593%) |
| java-checks/src/main/java/org/sonar/java/checks/tests/CallSuperInTestCaseCheck.java | 194 / 105 / 89 (45.876%) | 194 / 191 / 3 (1.546%) | 194 / 105 / 89 (45.876%) | 194 / 191 / 3 (1.546%) |
| java-checks/src/main/java/org/sonar/java/checks/tests/DataProviderNameUniquenessCheck.java | 166 / 91 / 75 (45.181%) | 166 / 162 / 4 (2.410%) | 166 / 91 / 75 (45.181%) | 166 / 162 / 4 (2.410%) |
| java-checks/src/main/java/org/sonar/java/checks/tests/ExpectedExceptionCheck.java | 97 / 58 / 39 (40.206%) | 97 / 95 / 2 (2.062%) | 97 / 58 / 39 (40.206%) | 97 / 95 / 2 (2.062%) |
| java-checks/src/main/java/org/sonar/java/checks/tests/IgnoredTestsCheck.java | 188 / 73 / 115 (61.170%) | 188 / 186 / 2 (1.064%) | 188 / 73 / 115 (61.170%) | 188 / 186 / 2 (1.064%) |
| java-checks/src/main/java/org/sonar/java/checks/tests/JUnit45MethodAnnotationCheck.java | 243 / 166 / 77 (31.687%) | 243 / 234 / 9 (3.704%) | 243 / 166 / 77 (31.687%) | 243 / 234 / 9 (3.704%) |
| java-checks/src/main/java/org/sonar/java/checks/tests/JUnit4AnnotationsCheck.java | 58 / 30 / 28 (48.276%) | 58 / 43 / 15 (25.862%) | 58 / 30 / 28 (48.276%) | 58 / 43 / 15 (25.862%) |
| java-checks/src/main/java/org/sonar/java/checks/tests/JUnit5DefaultPackageClassAndMethodCheck.java | 187 / 77 / 110 (58.824%) | 187 / 185 / 2 (1.070%) | 187 / 77 / 110 (58.824%) | 187 / 185 / 2 (1.070%) |
| java-checks/src/main/java/org/sonar/java/checks/tests/JUnit5SilentlyIgnoreClassAndMethodCheck.java | 239 / 112 / 127 (53.138%) | 239 / 237 / 2 (0.837%) | 239 / 112 / 127 (53.138%) | 239 / 237 / 2 (0.837%) |
| java-checks/src/main/java/org/sonar/java/checks/tests/JUnitCompatibleAnnotationsCheck.java | 64 / 25 / 39 (60.938%) | 64 / 60 / 4 (6.250%) | 64 / 25 / 39 (60.938%) | 64 / 60 / 4 (6.250%) |
| java-checks/src/main/java/org/sonar/java/checks/tests/JunitNestedAnnotationCheck.java | 88 / 42 / 46 (52.273%) | 88 / 86 / 2 (2.273%) | 88 / 42 / 46 (52.273%) | 88 / 86 / 2 (2.273%) |
| java-checks/src/main/java/org/sonar/java/checks/tests/MockFieldShouldUseMockAnnotationCheck.java | 80 / 31 / 49 (61.250%) | 80 / 78 / 2 (2.500%) | 80 / 31 / 49 (61.250%) | 80 / 78 / 2 (2.500%) |
| java-checks/src/main/java/org/sonar/java/checks/tests/MockingAllMethodsCheck.java | 203 / 85 / 118 (58.128%) | 203 / 201 / 2 (0.985%) | 203 / 85 / 118 (58.128%) | 203 / 201 / 2 (0.985%) |
| java-checks/src/main/java/org/sonar/java/checks/tests/MockitoAnnotatedObjectsShouldBeInitializedCheck.java | 387 / 204 / 183 (47.287%) | 387 / 385 / 2 (0.517%) | 387 / 204 / 183 (47.287%) | 387 / 385 / 2 (0.517%) |
| java-checks/src/main/java/org/sonar/java/checks/tests/MockitoArgumentMatchersUsedOnAllParametersCheck.java | 262 / 137 / 125 (47.710%) | 262 / 260 / 2 (0.763%) | 262 / 137 / 125 (47.710%) | 262 / 260 / 2 (0.763%) |
| java-checks/src/main/java/org/sonar/java/checks/tests/MockitoEqSimplificationCheck.java | 65 / 27 / 38 (58.462%) | 65 / 63 / 2 (3.077%) | 65 / 27 / 38 (58.462%) | 65 / 63 / 2 (3.077%) |
| java-checks/src/main/java/org/sonar/java/checks/tests/MockitoInjectMocksShouldBeUsedCheck.java | 339 / 177 / 162 (47.788%) | 339 / 337 / 2 (0.590%) | 339 / 177 / 162 (47.788%) | 339 / 337 / 2 (0.590%) |
| java-checks/src/main/java/org/sonar/java/checks/tests/MockitoInlineMockInThenReturnCheck.java | 70 / 35 / 35 (50.000%) | 70 / 68 / 2 (2.857%) | 70 / 35 / 35 (50.000%) | 70 / 68 / 2 (2.857%) |
| java-checks/src/main/java/org/sonar/java/checks/tests/MockitoManagedClassHelper.java | 130 / 88 / 42 (32.308%) | 130 / 130 / 0 (0.000%) | 130 / 88 / 42 (32.308%) | 130 / 130 / 0 (0.000%) |
| java-checks/src/main/java/org/sonar/java/checks/tests/MockitoStaticImportCheck.java | 225 / 129 / 96 (42.667%) | 225 / 221 / 4 (1.778%) | 225 / 129 / 96 (42.667%) | 225 / 221 / 4 (1.778%) |
| java-checks/src/main/java/org/sonar/java/checks/tests/MockitoStubbingChainCheck.java | 129 / 77 / 52 (40.310%) | 129 / 127 / 2 (1.550%) | 129 / 77 / 52 (40.310%) | 129 / 127 / 2 (1.550%) |
| java-checks/src/main/java/org/sonar/java/checks/tests/NoJUnit4AssertionsInJUnit5TestsCheck.java | 83 / 29 / 54 (65.060%) | 83 / 81 / 2 (2.410%) | 83 / 29 / 54 (65.060%) | 83 / 81 / 2 (2.410%) |
| java-checks/src/main/java/org/sonar/java/checks/tests/NoTestInTestClassCheck.java | 597 / 345 / 252 (42.211%) | 597 / 588 / 9 (1.508%) | 597 / 345 / 252 (42.211%) | 597 / 588 / 9 (1.508%) |
| java-checks/src/main/java/org/sonar/java/checks/tests/OneExpectedCheckedExceptionCheck.java | 74 / 30 / 44 (59.459%) | 74 / 72 / 2 (2.703%) | 74 / 30 / 44 (59.459%) | 74 / 72 / 2 (2.703%) |
| java-checks/src/main/java/org/sonar/java/checks/tests/OneExpectedRuntimeExceptionCheck.java | 125 / 40 / 85 (68.000%) | 125 / 123 / 2 (1.600%) | 125 / 40 / 85 (68.000%) | 125 / 123 / 2 (1.600%) |
| java-checks/src/main/java/org/sonar/java/checks/tests/OneTestLifecycleAnnotationCheck.java | 131 / 67 / 64 (48.855%) | 131 / 129 / 2 (1.527%) | 131 / 67 / 64 (48.855%) | 131 / 129 / 2 (1.527%) |
| java-checks/src/main/java/org/sonar/java/checks/tests/ParameterizedTestCheck.java | 277 / 150 / 127 (45.848%) | 277 / 273 / 4 (1.444%) | 277 / 150 / 127 (45.848%) | 277 / 273 / 4 (1.444%) |
| java-checks/src/main/java/org/sonar/java/checks/tests/RandomizedTestDataCheck.java | 142 / 67 / 75 (52.817%) | 142 / 140 / 2 (1.408%) | 142 / 67 / 75 (52.817%) | 142 / 140 / 2 (1.408%) |
| java-checks/src/main/java/org/sonar/java/checks/tests/SpringAssertionsSimplificationCheck.java | 172 / 54 / 118 (68.605%) | 172 / 170 / 2 (1.163%) | 172 / 54 / 118 (68.605%) | 172 / 170 / 2 (1.163%) |
| java-checks/src/main/java/org/sonar/java/checks/tests/SystemClockCheck.java | 58 / 22 / 36 (62.069%) | 58 / 56 / 2 (3.448%) | 58 / 22 / 36 (62.069%) | 58 / 56 / 2 (3.448%) |
| java-checks/src/main/java/org/sonar/java/checks/tests/TestAnnotationWithExpectedExceptionCheck.java | 103 / 43 / 60 (58.252%) | 103 / 101 / 2 (1.942%) | 103 / 43 / 60 (58.252%) | 103 / 101 / 2 (1.942%) |
| java-checks/src/main/java/org/sonar/java/checks/tests/TestNGDataProviderReturnTypeCheck.java | 100 / 57 / 43 (43.000%) | 100 / 98 / 2 (2.000%) | 100 / 57 / 43 (43.000%) | 100 / 98 / 2 (2.000%) |
| java-checks/src/main/java/org/sonar/java/checks/tests/TestNGJavadocTagsCheck.java | 178 / 121 / 57 (32.022%) | 178 / 160 / 18 (10.112%) | 178 / 121 / 57 (32.022%) | 178 / 160 / 18 (10.112%) |
| java-checks/src/main/java/org/sonar/java/checks/tests/TestsStabilityCheck.java | 81 / 34 / 47 (58.025%) | 81 / 79 / 2 (2.469%) | 81 / 34 / 47 (58.025%) | 81 / 79 / 2 (2.469%) |
| java-checks/src/main/java/org/sonar/java/checks/tests/ThreadSleepInTestsCheck.java | 46 / 20 / 26 (56.522%) | 46 / 44 / 2 (4.348%) | 46 / 20 / 26 (56.522%) | 46 / 44 / 2 (4.348%) |
| java-checks/src/main/java/org/sonar/java/checks/tests/TooManyAssertionsCheck.java | 201 / 110 / 91 (45.274%) | 201 / 195 / 6 (2.985%) | 201 / 110 / 91 (45.274%) | 201 / 195 / 6 (2.985%) |
| java-checks/src/main/java/org/sonar/java/checks/tests/package-info.java | 6 / 5 / 1 (16.667%) | 6 / 5 / 1 (16.667%) | 6 / 5 / 1 (16.667%) | 6 / 5 / 1 (16.667%) |
| java-checks/src/main/java/org/sonar/java/checks/unused/UnusedCollectionCheck.java | 102 / 44 / 58 (56.863%) | 102 / 99 / 3 (2.941%) | 102 / 44 / 58 (56.863%) | 102 / 99 / 3 (2.941%) |
| java-checks/src/main/java/org/sonar/java/checks/unused/UnusedLabelCheck.java | 32 / 15 / 17 (53.125%) | 32 / 30 / 2 (6.250%) | 32 / 15 / 17 (53.125%) | 32 / 30 / 2 (6.250%) |
| java-checks/src/main/java/org/sonar/java/checks/unused/UnusedLocalVariableCheck.java | 460 / 233 / 227 (49.348%) | 460 / 458 / 2 (0.435%) | 460 / 233 / 227 (49.348%) | 460 / 458 / 2 (0.435%) |
| java-checks/src/main/java/org/sonar/java/checks/unused/UnusedMethodParameterCheck.java | 481 / 232 / 249 (51.767%) | 481 / 477 / 4 (0.832%) | 481 / 232 / 249 (51.767%) | 481 / 477 / 4 (0.832%) |
| java-checks/src/main/java/org/sonar/java/checks/unused/UnusedPrivateClassCheck.java | 66 / 30 / 36 (54.545%) | 66 / 62 / 4 (6.061%) | 66 / 30 / 36 (54.545%) | 66 / 62 / 4 (6.061%) |
| java-checks/src/main/java/org/sonar/java/checks/unused/UnusedPrivateFieldCheck.java | 580 / 288 / 292 (50.345%) | 580 / 575 / 5 (0.862%) | 580 / 288 / 292 (50.345%) | 580 / 575 / 5 (0.862%) |
| java-checks/src/main/java/org/sonar/java/checks/unused/UnusedPrivateMethodCheck.java | 583 / 309 / 274 (46.998%) | 583 / 578 / 5 (0.858%) | 583 / 309 / 274 (46.998%) | 583 / 578 / 5 (0.858%) |
| java-checks/src/main/java/org/sonar/java/checks/unused/UnusedReturnedDataCheck.java | 153 / 66 / 87 (56.863%) | 153 / 150 / 3 (1.961%) | 153 / 66 / 87 (56.863%) | 153 / 150 / 3 (1.961%) |
| java-checks/src/main/java/org/sonar/java/checks/unused/UnusedStringBuilderCheck.java | 125 / 72 / 53 (42.400%) | 125 / 120 / 5 (4.000%) | 125 / 72 / 53 (42.400%) | 125 / 120 / 5 (4.000%) |
| java-checks/src/main/java/org/sonar/java/checks/unused/UnusedTestRuleCheck.java | 136 / 68 / 68 (50.000%) | 136 / 132 / 4 (2.941%) | 136 / 68 / 68 (50.000%) | 136 / 132 / 4 (2.941%) |
| java-checks/src/main/java/org/sonar/java/checks/unused/UnusedThrowableCheck.java | 65 / 21 / 44 (67.692%) | 65 / 63 / 2 (3.077%) | 65 / 21 / 44 (67.692%) | 65 / 63 / 2 (3.077%) |
| java-checks/src/main/java/org/sonar/java/checks/unused/UnusedTypeParameterCheck.java | 76 / 36 / 40 (52.632%) | 76 / 74 / 2 (2.632%) | 76 / 36 / 40 (52.632%) | 76 / 74 / 2 (2.632%) |
| java-checks/src/main/java/org/sonar/java/checks/unused/package-info.java | 6 / 5 / 1 (16.667%) | 6 / 5 / 1 (16.667%) | 6 / 5 / 1 (16.667%) | 6 / 5 / 1 (16.667%) |
| java-checks/src/main/java/org/sonar/java/checks/unused/utils/AnnotationFieldReferenceFinder.java | 178 / 125 / 53 (29.775%) | 178 / 172 / 6 (3.371%) | 178 / 125 / 53 (29.775%) | 178 / 172 / 6 (3.371%) |
| java-checks/src/main/java/org/sonar/java/checks/unused/utils/package-info.java | 7 / 6 / 1 (14.286%) | 7 / 6 / 1 (14.286%) | 7 / 6 / 1 (14.286%) | 7 / 6 / 1 (14.286%) |
| java-checks/src/main/java/org/sonar/java/filters/AnyRuleIssueFilter.java | 96 / 65 / 31 (32.292%) | 96 / 91 / 5 (5.208%) | 96 / 65 / 31 (32.292%) | 96 / 91 / 5 (5.208%) |
| java-checks/src/main/java/org/sonar/java/filters/BaseTreeVisitorIssueFilter.java | 228 / 156 / 72 (31.579%) | 228 / 209 / 19 (8.333%) | 228 / 156 / 72 (31.579%) | 228 / 209 / 19 (8.333%) |
| java-checks/src/main/java/org/sonar/java/filters/EclipseI18NFilter.java | 30 / 16 / 14 (46.667%) | 30 / 30 / 0 (0.000%) | 30 / 16 / 14 (46.667%) | 30 / 30 / 0 (0.000%) |
| java-checks/src/main/java/org/sonar/java/filters/ExpectedExceptionFilter.java | 407 / 230 / 177 (43.489%) | 407 / 407 / 0 (0.000%) | 407 / 230 / 177 (43.489%) | 407 / 407 / 0 (0.000%) |
| java-checks/src/main/java/org/sonar/java/filters/GeneratedCodeFilter.java | 63 / 37 / 26 (41.270%) | 63 / 63 / 0 (0.000%) | 63 / 37 / 26 (41.270%) | 63 / 63 / 0 (0.000%) |
| java-checks/src/main/java/org/sonar/java/filters/GoogleAutoFilter.java | 59 / 36 / 23 (38.983%) | 59 / 59 / 0 (0.000%) | 59 / 36 / 23 (38.983%) | 59 / 59 / 0 (0.000%) |
| java-checks/src/main/java/org/sonar/java/filters/JavaIssueFilter.java | 13 / 8 / 5 (38.462%) | 13 / 12 / 1 (7.692%) | 13 / 8 / 5 (38.462%) | 13 / 12 / 1 (7.692%) |
| java-checks/src/main/java/org/sonar/java/filters/LombokFilter.java | 493 / 253 / 240 (48.682%) | 493 / 492 / 1 (0.203%) | 493 / 253 / 240 (48.682%) | 493 / 492 / 1 (0.203%) |
| java-checks/src/main/java/org/sonar/java/filters/PostAnalysisIssueFilter.java | 54 / 25 / 29 (53.704%) | 54 / 51 / 3 (5.556%) | 54 / 25 / 29 (53.704%) | 54 / 51 / 3 (5.556%) |
| java-checks/src/main/java/org/sonar/java/filters/SpringFilter.java | 238 / 107 / 131 (55.042%) | 238 / 238 / 0 (0.000%) | 238 / 107 / 131 (55.042%) | 238 / 238 / 0 (0.000%) |
| java-checks/src/main/java/org/sonar/java/filters/SuppressWarningFilter.java | 581 / 286 / 295 (50.775%) | 581 / 364 / 217 (37.349%) | 581 / 286 / 295 (50.775%) | 581 / 364 / 217 (37.349%) |
| java-checks/src/main/java/org/sonar/java/filters/package-info.java | 5 / 4 / 1 (20.000%) | 5 / 4 / 1 (20.000%) | 5 / 4 / 1 (20.000%) | 5 / 4 / 1 (20.000%) |
| java-frontend/src/main/java/org/eclipse/jdt/core/dom/ASTUtils.java | 179 / 117 / 62 (34.637%) | 179 / 117 / 62 (34.637%) | 179 / 117 / 62 (34.637%) | 179 / 117 / 62 (34.637%) |
| java-frontend/src/main/java/org/eclipse/jdt/core/dom/package-info.java | 5 / 5 / 0 (0.000%) | 5 / 5 / 0 (0.000%) | 5 / 5 / 0 (0.000%) | 5 / 5 / 0 (0.000%) |
| java-frontend/src/main/java/org/sonar/java/AnalysisException.java | 18 / 18 / 0 (0.000%) | 18 / 18 / 0 (0.000%) | 18 / 18 / 0 (0.000%) | 18 / 18 / 0 (0.000%) |
| java-frontend/src/main/java/org/sonar/java/AnalysisProgress.java | 43 / 43 / 0 (0.000%) | 43 / 43 / 0 (0.000%) | 43 / 43 / 0 (0.000%) | 43 / 43 / 0 (0.000%) |
| java-frontend/src/main/java/org/sonar/java/AnalysisWarningsWrapper.java | 28 / 22 / 6 (21.429%) | 28 / 22 / 6 (21.429%) | 28 / 22 / 6 (21.429%) | 28 / 22 / 6 (21.429%) |
| java-frontend/src/main/java/org/sonar/java/BatchGenerator.java | 78 / 52 / 26 (33.333%) | 78 / 52 / 26 (33.333%) | 78 / 52 / 26 (33.333%) | 78 / 52 / 26 (33.333%) |
| java-frontend/src/main/java/org/sonar/java/CheckFailureException.java | 13 / 13 / 0 (0.000%) | 13 / 13 / 0 (0.000%) | 13 / 13 / 0 (0.000%) | 13 / 13 / 0 (0.000%) |
| java-frontend/src/main/java/org/sonar/java/DefaultJavaResourceLocator.java | 142 / 107 / 35 (24.648%) | 142 / 129 / 13 (9.155%) | 142 / 108 / 34 (23.944%) | 142 / 129 / 13 (9.155%) |
| java-frontend/src/main/java/org/sonar/java/DefaultModuleMetadata.java | 53 / 35 / 18 (33.962%) | 53 / 43 / 10 (18.868%) | 53 / 37 / 16 (30.189%) | 53 / 43 / 10 (18.868%) |
| java-frontend/src/main/java/org/sonar/java/ExceptionHandler.java | 10 / 9 / 1 (10.000%) | 10 / 9 / 1 (10.000%) | 10 / 9 / 1 (10.000%) | 10 / 9 / 1 (10.000%) |
| java-frontend/src/main/java/org/sonar/java/ExecutionTimeReport.java | 180 / 165 / 15 (8.333%) | 180 / 168 / 12 (6.667%) | 180 / 166 / 14 (7.778%) | 180 / 168 / 12 (6.667%) |
| java-frontend/src/main/java/org/sonar/java/IllegalRuleParameterException.java | 13 / 13 / 0 (0.000%) | 13 / 13 / 0 (0.000%) | 13 / 13 / 0 (0.000%) | 13 / 13 / 0 (0.000%) |
| java-frontend/src/main/java/org/sonar/java/JavaConstants.java | 7 / 7 / 0 (0.000%) | 7 / 7 / 0 (0.000%) | 7 / 7 / 0 (0.000%) | 7 / 7 / 0 (0.000%) |
| java-frontend/src/main/java/org/sonar/java/JavaFilesCache.java | 151 / 116 / 35 (23.179%) | 151 / 149 / 2 (1.325%) | 151 / 119 / 32 (21.192%) | 151 / 149 / 2 (1.325%) |
| java-frontend/src/main/java/org/sonar/java/JavaFrontend.java | 677 / 428 / 249 (36.780%) | 677 / 579 / 98 (14.476%) | 677 / 494 / 183 (27.031%) | 677 / 579 / 98 (14.476%) |
| java-frontend/src/main/java/org/sonar/java/Measurer.java | 238 / 121 / 117 (49.160%) | 238 / 205 / 33 (13.866%) | 238 / 138 / 100 (42.017%) | 238 / 205 / 33 (13.866%) |
| java-frontend/src/main/java/org/sonar/java/Preconditions.java | 54 / 54 / 0 (0.000%) | 54 / 54 / 0 (0.000%) | 54 / 54 / 0 (0.000%) | 54 / 54 / 0 (0.000%) |
| java-frontend/src/main/java/org/sonar/java/ProgressMonitor.java | 167 / 153 / 14 (8.383%) | 167 / 160 / 7 (4.192%) | 167 / 160 / 7 (4.192%) | 167 / 160 / 7 (4.192%) |
| java-frontend/src/main/java/org/sonar/java/SemanticReportScanner.java | 1117 / 805 / 312 (27.932%) | 1117 / 1046 / 71 (6.356%) | 1117 / 822 / 295 (26.410%) | 1117 / 1046 / 71 (6.356%) |
| java-frontend/src/main/java/org/sonar/java/SonarComponents.java | 1147 / 693 / 454 (39.582%) | 1147 / 916 / 231 (20.139%) | 1147 / 769 / 378 (32.956%) | 1147 / 916 / 231 (20.139%) |
| java-frontend/src/main/java/org/sonar/java/annotations/Beta.java | 20 / 20 / 0 (0.000%) | 20 / 20 / 0 (0.000%) | 20 / 20 / 0 (0.000%) | 20 / 20 / 0 (0.000%) |
| java-frontend/src/main/java/org/sonar/java/annotations/VisibleForTesting.java | 14 / 14 / 0 (0.000%) | 14 / 14 / 0 (0.000%) | 14 / 14 / 0 (0.000%) | 14 / 14 / 0 (0.000%) |
| java-frontend/src/main/java/org/sonar/java/annotations/package-info.java | 7 / 6 / 1 (14.286%) | 7 / 6 / 1 (14.286%) | 7 / 6 / 1 (14.286%) | 7 / 6 / 1 (14.286%) |
| java-frontend/src/main/java/org/sonar/java/ast/JavaAstScanner.java | 417 / 255 / 162 (38.849%) | 417 / 348 / 69 (16.547%) | 417 / 281 / 136 (32.614%) | 417 / 348 / 69 (16.547%) |
| java-frontend/src/main/java/org/sonar/java/ast/api/JavaKeyword.java | 145 / 144 / 1 (0.690%) | 145 / 144 / 1 (0.690%) | 145 / 144 / 1 (0.690%) | 145 / 144 / 1 (0.690%) |
| java-frontend/src/main/java/org/sonar/java/ast/api/JavaPunctuator.java | 125 / 124 / 1 (0.800%) | 125 / 124 / 1 (0.800%) | 125 / 124 / 1 (0.800%) | 125 / 124 / 1 (0.800%) |
| java-frontend/src/main/java/org/sonar/java/ast/api/JavaRestrictedKeyword.java | 64 / 62 / 2 (3.125%) | 64 / 63 / 1 (1.563%) | 64 / 63 / 1 (1.563%) | 64 / 63 / 1 (1.563%) |
| java-frontend/src/main/java/org/sonar/java/ast/api/package-info.java | 6 / 5 / 1 (16.667%) | 6 / 5 / 1 (16.667%) | 6 / 5 / 1 (16.667%) | 6 / 5 / 1 (16.667%) |
| java-frontend/src/main/java/org/sonar/java/ast/package-info.java | 5 / 4 / 1 (20.000%) | 5 / 4 / 1 (20.000%) | 5 / 4 / 1 (20.000%) | 5 / 4 / 1 (20.000%) |
| java-frontend/src/main/java/org/sonar/java/ast/parser/ArgumentListTreeImpl.java | 82 / 52 / 30 (36.585%) | 82 / 74 / 8 (9.756%) | 82 / 63 / 19 (23.171%) | 82 / 74 / 8 (9.756%) |
| java-frontend/src/main/java/org/sonar/java/ast/parser/FormalParametersListTreeImpl.java | 37 / 22 / 15 (40.541%) | 37 / 31 / 6 (16.216%) | 37 / 28 / 9 (24.324%) | 37 / 31 / 6 (16.216%) |
| java-frontend/src/main/java/org/sonar/java/ast/parser/InitializerListTreeImpl.java | 23 / 16 / 7 (30.435%) | 23 / 23 / 0 (0.000%) | 23 / 21 / 2 (8.696%) | 23 / 23 / 0 (0.000%) |
| java-frontend/src/main/java/org/sonar/java/ast/parser/ListTreeImpl.java | 231 / 163 / 68 (29.437%) | 231 / 229 / 2 (0.866%) | 231 / 171 / 60 (25.974%) | 231 / 229 / 2 (0.866%) |
| java-frontend/src/main/java/org/sonar/java/ast/parser/ModuleNameListTreeImpl.java | 23 / 16 / 7 (30.435%) | 23 / 23 / 0 (0.000%) | 23 / 21 / 2 (8.696%) | 23 / 23 / 0 (0.000%) |
| java-frontend/src/main/java/org/sonar/java/ast/parser/ModuleNameTreeImpl.java | 21 / 15 / 6 (28.571%) | 21 / 21 / 0 (0.000%) | 21 / 18 / 3 (14.286%) | 21 / 21 / 0 (0.000%) |
| java-frontend/src/main/java/org/sonar/java/ast/parser/QualifiedIdentifierListTreeImpl.java | 23 / 16 / 7 (30.435%) | 23 / 23 / 0 (0.000%) | 23 / 21 / 2 (8.696%) | 23 / 23 / 0 (0.000%) |
| java-frontend/src/main/java/org/sonar/java/ast/parser/ResourceListTreeImpl.java | 23 / 16 / 7 (30.435%) | 23 / 23 / 0 (0.000%) | 23 / 18 / 5 (21.739%) | 23 / 23 / 0 (0.000%) |
| java-frontend/src/main/java/org/sonar/java/ast/parser/StatementListTreeImpl.java | 23 / 16 / 7 (30.435%) | 23 / 23 / 0 (0.000%) | 23 / 21 / 2 (8.696%) | 23 / 23 / 0 (0.000%) |
| java-frontend/src/main/java/org/sonar/java/ast/parser/TypeParameterListTreeImpl.java | 75 / 50 / 25 (33.333%) | 75 / 69 / 6 (8.000%) | 75 / 58 / 17 (22.667%) | 75 / 69 / 6 (8.000%) |
| java-frontend/src/main/java/org/sonar/java/ast/parser/package-info.java | 6 / 5 / 1 (16.667%) | 6 / 5 / 1 (16.667%) | 6 / 5 / 1 (16.667%) | 6 / 5 / 1 (16.667%) |
| java-frontend/src/main/java/org/sonar/java/ast/visitors/AccessorsUtils.java | 317 / 161 / 156 (49.211%) | 317 / 313 / 4 (1.262%) | 317 / 208 / 109 (34.385%) | 317 / 313 / 4 (1.262%) |
| java-frontend/src/main/java/org/sonar/java/ast/visitors/CognitiveComplexityVisitor.java | 439 / 296 / 143 (32.574%) | 439 / 439 / 0 (0.000%) | 439 / 323 / 116 (26.424%) | 439 / 439 / 0 (0.000%) |
| java-frontend/src/main/java/org/sonar/java/ast/visitors/CommentLinesVisitor.java | 131 / 99 / 32 (24.427%) | 131 / 130 / 1 (0.763%) | 131 / 122 / 9 (6.870%) | 131 / 130 / 1 (0.763%) |
| java-frontend/src/main/java/org/sonar/java/ast/visitors/ComplexityVisitor.java | 189 / 105 / 84 (44.444%) | 189 / 189 / 0 (0.000%) | 189 / 115 / 74 (39.153%) | 189 / 189 / 0 (0.000%) |
| java-frontend/src/main/java/org/sonar/java/ast/visitors/FileLinesVisitor.java | 295 / 143 / 152 (51.525%) | 295 / 285 / 10 (3.390%) | 295 / 166 / 129 (43.729%) | 295 / 285 / 10 (3.390%) |
| java-frontend/src/main/java/org/sonar/java/ast/visitors/Java25FeaturesTelemetryVisitor.java | 93 / 48 / 45 (48.387%) | 93 / 93 / 0 (0.000%) | 93 / 64 / 29 (31.183%) | 93 / 93 / 0 (0.000%) |
| java-frontend/src/main/java/org/sonar/java/ast/visitors/LinesOfCodeVisitor.java | 43 / 28 / 15 (34.884%) | 43 / 43 / 0 (0.000%) | 43 / 36 / 7 (16.279%) | 43 / 43 / 0 (0.000%) |
| java-frontend/src/main/java/org/sonar/java/ast/visitors/MethodNestingLevelVisitor.java | 130 / 91 / 39 (30.000%) | 130 / 130 / 0 (0.000%) | 130 / 101 / 29 (22.308%) | 130 / 130 / 0 (0.000%) |
| java-frontend/src/main/java/org/sonar/java/ast/visitors/NumberOfDefinedVariablesVisitor.java | 20 / 16 / 4 (20.000%) | 20 / 20 / 0 (0.000%) | 20 / 16 / 4 (20.000%) | 20 / 20 / 0 (0.000%) |
| java-frontend/src/main/java/org/sonar/java/ast/visitors/PublicApiChecker.java | 289 / 134 / 155 (53.633%) | 289 / 284 / 5 (1.730%) | 289 / 145 / 144 (49.827%) | 289 / 284 / 5 (1.730%) |
| java-frontend/src/main/java/org/sonar/java/ast/visitors/SonarSymbolTableVisitor.java | 210 / 118 / 92 (43.810%) | 210 / 204 / 6 (2.857%) | 210 / 154 / 56 (26.667%) | 210 / 204 / 6 (2.857%) |
| java-frontend/src/main/java/org/sonar/java/ast/visitors/StatementVisitor.java | 220 / 147 / 73 (33.182%) | 220 / 220 / 0 (0.000%) | 220 / 158 / 62 (28.182%) | 220 / 220 / 0 (0.000%) |
| java-frontend/src/main/java/org/sonar/java/ast/visitors/SubscriptionVisitor.java | 148 / 99 / 49 (33.108%) | 148 / 148 / 0 (0.000%) | 148 / 106 / 42 (28.378%) | 148 / 148 / 0 (0.000%) |
| java-frontend/src/main/java/org/sonar/java/ast/visitors/SyntaxHighlighterVisitor.java | 444 / 180 / 264 (59.459%) | 444 / 380 / 64 (14.414%) | 444 / 226 / 218 (49.099%) | 444 / 380 / 64 (14.414%) |
| java-frontend/src/main/java/org/sonar/java/ast/visitors/package-info.java | 6 / 5 / 1 (16.667%) | 6 / 5 / 1 (16.667%) | 6 / 5 / 1 (16.667%) | 6 / 5 / 1 (16.667%) |
| java-frontend/src/main/java/org/sonar/java/caching/CacheContextImpl.java | 125 / 85 / 40 (32.000%) | 125 / 101 / 24 (19.200%) | 125 / 89 / 36 (28.800%) | 125 / 101 / 24 (19.200%) |
| java-frontend/src/main/java/org/sonar/java/caching/CacheReadException.java | 14 / 14 / 0 (0.000%) | 14 / 14 / 0 (0.000%) | 14 / 14 / 0 (0.000%) | 14 / 14 / 0 (0.000%) |
| java-frontend/src/main/java/org/sonar/java/caching/ContentHashCache.java | 167 / 113 / 54 (32.335%) | 167 / 125 / 42 (25.150%) | 167 / 113 / 54 (32.335%) | 167 / 125 / 42 (25.150%) |
| java-frontend/src/main/java/org/sonar/java/caching/DummyCache.java | 42 / 39 / 3 (7.143%) | 42 / 41 / 1 (2.381%) | 42 / 40 / 2 (4.762%) | 42 / 41 / 1 (2.381%) |
| java-frontend/src/main/java/org/sonar/java/caching/FileCachingCheck.java | 113 / 85 / 28 (24.779%) | 113 / 106 / 7 (6.195%) | 113 / 85 / 28 (24.779%) | 113 / 106 / 7 (6.195%) |
| java-frontend/src/main/java/org/sonar/java/caching/FileHashingUtils.java | 54 / 51 / 3 (5.556%) | 54 / 51 / 3 (5.556%) | 54 / 51 / 3 (5.556%) | 54 / 51 / 3 (5.556%) |
| java-frontend/src/main/java/org/sonar/java/caching/JavaReadCacheImpl.java | 97 / 75 / 22 (22.680%) | 97 / 77 / 20 (20.619%) | 97 / 76 / 21 (21.649%) | 97 / 77 / 20 (20.619%) |
| java-frontend/src/main/java/org/sonar/java/caching/JavaWriteCacheImpl.java | 88 / 67 / 21 (23.864%) | 88 / 68 / 20 (22.727%) | 88 / 68 / 20 (22.727%) | 88 / 68 / 20 (22.727%) |
| java-frontend/src/main/java/org/sonar/java/caching/package-info.java | 5 / 4 / 1 (20.000%) | 5 / 4 / 1 (20.000%) | 5 / 4 / 1 (20.000%) | 5 / 4 / 1 (20.000%) |
| java-frontend/src/main/java/org/sonar/java/cfg/CFG.java | 2337 / 1703 / 634 (27.129%) | 2337 / 2302 / 35 (1.498%) | 2337 / 1911 / 426 (18.228%) | 2337 / 2302 / 35 (1.498%) |
| java-frontend/src/main/java/org/sonar/java/cfg/CFGLoop.java | 315 / 183 / 132 (41.905%) | 315 / 314 / 1 (0.317%) | 315 / 186 / 129 (40.952%) | 315 / 314 / 1 (0.317%) |
| java-frontend/src/main/java/org/sonar/java/cfg/CFGUtils.java | 243 / 119 / 124 (51.029%) | 243 / 243 / 0 (0.000%) | 243 / 136 / 107 (44.033%) | 243 / 243 / 0 (0.000%) |
| java-frontend/src/main/java/org/sonar/java/cfg/LiveVariables.java | 480 / 255 / 225 (46.875%) | 480 / 474 / 6 (1.250%) | 480 / 277 / 203 (42.292%) | 480 / 474 / 6 (1.250%) |
| java-frontend/src/main/java/org/sonar/java/cfg/VariableReadExtractor.java | 110 / 63 / 47 (42.727%) | 110 / 110 / 0 (0.000%) | 110 / 72 / 38 (34.545%) | 110 / 110 / 0 (0.000%) |
| java-frontend/src/main/java/org/sonar/java/cfg/package-info.java | 5 / 4 / 1 (20.000%) | 5 / 4 / 1 (20.000%) | 5 / 4 / 1 (20.000%) | 5 / 4 / 1 (20.000%) |
| java-frontend/src/main/java/org/sonar/java/classpath/AbstractClasspath.java | 753 / 686 / 67 (8.898%) | 753 / 700 / 53 (7.039%) | 753 / 687 / 66 (8.765%) | 753 / 700 / 53 (7.039%) |
| java-frontend/src/main/java/org/sonar/java/classpath/ClasspathForMain.java | 32 / 18 / 14 (43.750%) | 32 / 25 / 7 (21.875%) | 32 / 18 / 14 (43.750%) | 32 / 25 / 7 (21.875%) |
| java-frontend/src/main/java/org/sonar/java/classpath/ClasspathForTest.java | 32 / 18 / 14 (43.750%) | 32 / 25 / 7 (21.875%) | 32 / 18 / 14 (43.750%) | 32 / 25 / 7 (21.875%) |
| java-frontend/src/main/java/org/sonar/java/classpath/ClasspathProperties.java | 70 / 32 / 38 (54.286%) | 70 / 32 / 38 (54.286%) | 70 / 32 / 38 (54.286%) | 70 / 32 / 38 (54.286%) |
| java-frontend/src/main/java/org/sonar/java/classpath/DependencyVersionInference.java | 62 / 45 / 17 (27.419%) | 62 / 62 / 0 (0.000%) | 62 / 58 / 4 (6.452%) | 62 / 62 / 0 (0.000%) |
| java-frontend/src/main/java/org/sonar/java/classpath/JavaSdkUtil.java | 299 / 298 / 1 (0.334%) | 299 / 299 / 0 (0.000%) | 299 / 299 / 0 (0.000%) | 299 / 299 / 0 (0.000%) |
| java-frontend/src/main/java/org/sonar/java/classpath/VersionImpl.java | 191 / 169 / 22 (11.518%) | 191 / 189 / 2 (1.047%) | 191 / 189 / 2 (1.047%) | 191 / 189 / 2 (1.047%) |
| java-frontend/src/main/java/org/sonar/java/classpath/package-info.java | 7 / 6 / 1 (14.286%) | 7 / 6 / 1 (14.286%) | 7 / 6 / 1 (14.286%) | 7 / 6 / 1 (14.286%) |
| java-frontend/src/main/java/org/sonar/java/collections/CollectionUtils.java | 48 / 46 / 2 (4.167%) | 48 / 46 / 2 (4.167%) | 48 / 46 / 2 (4.167%) | 48 / 46 / 2 (4.167%) |
| java-frontend/src/main/java/org/sonar/java/collections/package-info.java | 5 / 4 / 1 (20.000%) | 5 / 5 / 0 (0.000%) | 5 / 4 / 1 (20.000%) | 5 / 5 / 0 (0.000%) |
| java-frontend/src/main/java/org/sonar/java/exceptions/ApiMismatchException.java | 11 / 11 / 0 (0.000%) | 11 / 11 / 0 (0.000%) | 11 / 11 / 0 (0.000%) | 11 / 11 / 0 (0.000%) |
| java-frontend/src/main/java/org/sonar/java/exceptions/ThrowableUtils.java | 35 / 35 / 0 (0.000%) | 35 / 35 / 0 (0.000%) | 35 / 35 / 0 (0.000%) | 35 / 35 / 0 (0.000%) |
| java-frontend/src/main/java/org/sonar/java/exceptions/package-info.java | 7 / 6 / 1 (14.286%) | 7 / 6 / 1 (14.286%) | 7 / 6 / 1 (14.286%) | 7 / 6 / 1 (14.286%) |
| java-frontend/src/main/java/org/sonar/java/filters/SonarJavaIssueFilter.java | 7 / 5 / 2 (28.571%) | 7 / 6 / 1 (14.286%) | 7 / 6 / 1 (14.286%) | 7 / 6 / 1 (14.286%) |
| java-frontend/src/main/java/org/sonar/java/filters/package-info.java | 7 / 6 / 1 (14.286%) | 7 / 6 / 1 (14.286%) | 7 / 6 / 1 (14.286%) | 7 / 6 / 1 (14.286%) |
| java-frontend/src/main/java/org/sonar/java/matcher/MethodMatcherFactory.java | 137 / 114 / 23 (16.788%) | 137 / 137 / 0 (0.000%) | 137 / 136 / 1 (0.730%) | 137 / 137 / 0 (0.000%) |
| java-frontend/src/main/java/org/sonar/java/matcher/MethodMatchersBuilder.java | 490 / 331 / 159 (32.449%) | 490 / 476 / 14 (2.857%) | 490 / 371 / 119 (24.286%) | 490 / 476 / 14 (2.857%) |
| java-frontend/src/main/java/org/sonar/java/matcher/MethodMatchersList.java | 72 / 31 / 41 (56.944%) | 72 / 72 / 0 (0.000%) | 72 / 67 / 5 (6.944%) | 72 / 72 / 0 (0.000%) |
| java-frontend/src/main/java/org/sonar/java/matcher/NoneMethodMatchers.java | 35 / 29 / 6 (17.143%) | 35 / 35 / 0 (0.000%) | 35 / 30 / 5 (14.286%) | 35 / 35 / 0 (0.000%) |
| java-frontend/src/main/java/org/sonar/java/matcher/TreeMatcher.java | 366 / 203 / 163 (44.536%) | 366 / 366 / 0 (0.000%) | 366 / 206 / 160 (43.716%) | 366 / 366 / 0 (0.000%) |
| java-frontend/src/main/java/org/sonar/java/matcher/package-info.java | 6 / 4 / 2 (33.333%) | 6 / 5 / 1 (16.667%) | 6 / 4 / 2 (33.333%) | 6 / 5 / 1 (16.667%) |
| java-frontend/src/main/java/org/sonar/java/metrics/MetricsComputer.java | 238 / 141 / 97 (40.756%) | 238 / 238 / 0 (0.000%) | 238 / 169 / 69 (28.992%) | 238 / 238 / 0 (0.000%) |
| java-frontend/src/main/java/org/sonar/java/metrics/MetricsScannerContext.java | 7 / 6 / 1 (14.286%) | 7 / 7 / 0 (0.000%) | 7 / 7 / 0 (0.000%) | 7 / 7 / 0 (0.000%) |
| java-frontend/src/main/java/org/sonar/java/metrics/package-info.java | 6 / 4 / 2 (33.333%) | 6 / 5 / 1 (16.667%) | 6 / 4 / 2 (33.333%) | 6 / 5 / 1 (16.667%) |
| java-frontend/src/main/java/org/sonar/java/model/AbstractTypedTree.java | 18 / 9 / 9 (50.000%) | 18 / 16 / 2 (11.111%) | 18 / 9 / 9 (50.000%) | 18 / 16 / 2 (11.111%) |
| java-frontend/src/main/java/org/sonar/java/model/AnnotationValueImpl.java | 30 / 28 / 2 (6.667%) | 30 / 30 / 0 (0.000%) | 30 / 28 / 2 (6.667%) | 30 / 30 / 0 (0.000%) |
| java-frontend/src/main/java/org/sonar/java/model/ArrayDimensionTreeImpl.java | 103 / 68 / 35 (33.981%) | 103 / 100 / 3 (2.913%) | 103 / 86 / 17 (16.505%) | 103 / 100 / 3 (2.913%) |
| java-frontend/src/main/java/org/sonar/java/model/DefaultInputFileScannerContext.java | 71 / 53 / 18 (25.352%) | 71 / 64 / 7 (9.859%) | 71 / 58 / 13 (18.310%) | 71 / 64 / 7 (9.859%) |
| java-frontend/src/main/java/org/sonar/java/model/DefaultJavaFileScannerContext.java | 545 / 334 / 211 (38.716%) | 545 / 523 / 22 (4.037%) | 545 / 360 / 185 (33.945%) | 545 / 523 / 22 (4.037%) |
| java-frontend/src/main/java/org/sonar/java/model/DefaultModuleScannerContext.java | 120 / 84 / 36 (30.000%) | 120 / 111 / 9 (7.500%) | 120 / 93 / 27 (22.500%) | 120 / 111 / 9 (7.500%) |
| java-frontend/src/main/java/org/sonar/java/model/ExpressionUtils.java | 892 / 520 / 372 (41.704%) | 892 / 872 / 20 (2.242%) | 892 / 632 / 260 (29.148%) | 892 / 872 / 20 (2.242%) |
| java-frontend/src/main/java/org/sonar/java/model/GeneratedFile.java | 325 / 251 / 74 (22.769%) | 325 / 306 / 19 (5.846%) | 325 / 257 / 68 (20.923%) | 325 / 306 / 19 (5.846%) |
| java-frontend/src/main/java/org/sonar/java/model/InputFileUtils.java | 81 / 71 / 10 (12.346%) | 81 / 71 / 10 (12.346%) | 81 / 71 / 10 (12.346%) | 81 / 71 / 10 (12.346%) |
| java-frontend/src/main/java/org/sonar/java/model/InternalSyntaxToken.java | 124 / 92 / 32 (25.806%) | 124 / 124 / 0 (0.000%) | 124 / 114 / 10 (8.065%) | 124 / 124 / 0 (0.000%) |
| java-frontend/src/main/java/org/sonar/java/model/InternalSyntaxTrivia.java | 151 / 108 / 43 (28.477%) | 151 / 149 / 2 (1.325%) | 151 / 138 / 13 (8.609%) | 151 / 149 / 2 (1.325%) |
| java-frontend/src/main/java/org/sonar/java/model/JInitializerBlockSymbol.java | 133 / 104 / 29 (21.805%) | 133 / 133 / 0 (0.000%) | 133 / 106 / 27 (20.301%) | 133 / 133 / 0 (0.000%) |
| java-frontend/src/main/java/org/sonar/java/model/JLabelSymbol.java | 52 / 36 / 16 (30.769%) | 52 / 51 / 1 (1.923%) | 52 / 42 / 10 (19.231%) | 52 / 51 / 1 (1.923%) |
| java-frontend/src/main/java/org/sonar/java/model/JMethodSymbol.java | 280 / 150 / 130 (46.429%) | 280 / 231 / 49 (17.500%) | 280 / 151 / 129 (46.071%) | 280 / 231 / 49 (17.500%) |
| java-frontend/src/main/java/org/sonar/java/model/JPackageSymbol.java | 14 / 10 / 4 (28.571%) | 14 / 13 / 1 (7.143%) | 14 / 10 / 4 (28.571%) | 14 / 13 / 1 (7.143%) |
| java-frontend/src/main/java/org/sonar/java/model/JParser.java | 7102 / 3618 / 3484 (49.057%) | 7102 / 4874 / 2228 (31.371%) | 7102 / 3993 / 3109 (43.776%) | 7102 / 4874 / 2228 (31.371%) |
| java-frontend/src/main/java/org/sonar/java/model/JParserConfig.java | 546 / 383 / 163 (29.853%) | 546 / 437 / 109 (19.963%) | 546 / 403 / 143 (26.190%) | 546 / 437 / 109 (19.963%) |
| java-frontend/src/main/java/org/sonar/java/model/JProblem.java | 198 / 148 / 50 (25.253%) | 198 / 174 / 24 (12.121%) | 198 / 148 / 50 (25.253%) | 198 / 174 / 24 (12.121%) |
| java-frontend/src/main/java/org/sonar/java/model/JSema.java | 286 / 141 / 145 (50.699%) | 286 / 208 / 78 (27.273%) | 286 / 148 / 138 (48.252%) | 286 / 208 / 78 (27.273%) |
| java-frontend/src/main/java/org/sonar/java/model/JSymbol.java | 659 / 360 / 299 (45.372%) | 659 / 475 / 184 (27.921%) | 659 / 364 / 295 (44.765%) | 659 / 475 / 184 (27.921%) |
| java-frontend/src/main/java/org/sonar/java/model/JSymbolMetadata.java | 774 / 472 / 302 (39.018%) | 774 / 701 / 73 (9.432%) | 774 / 481 / 293 (37.855%) | 774 / 701 / 73 (9.432%) |
| java-frontend/src/main/java/org/sonar/java/model/JSymbolMetadataNullabilityHelper.java | 769 / 492 / 277 (36.021%) | 769 / 763 / 6 (0.780%) | 769 / 492 / 277 (36.021%) | 769 / 763 / 6 (0.780%) |
| java-frontend/src/main/java/org/sonar/java/model/JType.java | 454 / 320 / 134 (29.515%) | 454 / 370 / 84 (18.502%) | 454 / 320 / 134 (29.515%) | 454 / 370 / 84 (18.502%) |
| java-frontend/src/main/java/org/sonar/java/model/JTypeSymbol.java | 261 / 154 / 107 (40.996%) | 261 / 238 / 23 (8.812%) | 261 / 156 / 105 (40.230%) | 261 / 238 / 23 (8.812%) |
| java-frontend/src/main/java/org/sonar/java/model/JUtils.java | 400 / 202 / 198 (49.500%) | 400 / 354 / 46 (11.500%) | 400 / 202 / 198 (49.500%) | 400 / 354 / 46 (11.500%) |
| java-frontend/src/main/java/org/sonar/java/model/JVariableSymbol.java | 179 / 132 / 47 (26.257%) | 179 / 168 / 11 (6.145%) | 179 / 134 / 45 (25.140%) | 179 / 168 / 11 (6.145%) |
| java-frontend/src/main/java/org/sonar/java/model/JWarning.java | 308 / 198 / 110 (35.714%) | 308 / 291 / 17 (5.519%) | 308 / 238 / 70 (22.727%) | 308 / 291 / 17 (5.519%) |
| java-frontend/src/main/java/org/sonar/java/model/JavaTree.java | 1054 / 687 / 367 (34.820%) | 1054 / 1008 / 46 (4.364%) | 1054 / 849 / 205 (19.450%) | 1054 / 1008 / 46 (4.364%) |
| java-frontend/src/main/java/org/sonar/java/model/JavaVersionImpl.java | 283 / 257 / 26 (9.187%) | 283 / 273 / 10 (3.534%) | 283 / 273 / 10 (3.534%) | 283 / 273 / 10 (3.534%) |
| java-frontend/src/main/java/org/sonar/java/model/KeywordSuper.java | 33 / 16 / 17 (51.515%) | 33 / 30 / 3 (9.091%) | 33 / 17 / 16 (48.485%) | 33 / 30 / 3 (9.091%) |
| java-frontend/src/main/java/org/sonar/java/model/KeywordThis.java | 28 / 13 / 15 (53.571%) | 28 / 25 / 3 (10.714%) | 28 / 14 / 14 (50.000%) | 28 / 25 / 3 (10.714%) |
| java-frontend/src/main/java/org/sonar/java/model/LineColumnConverter.java | 75 / 71 / 4 (5.333%) | 75 / 75 / 0 (0.000%) | 75 / 75 / 0 (0.000%) | 75 / 75 / 0 (0.000%) |
| java-frontend/src/main/java/org/sonar/java/model/LineUtils.java | 87 / 67 / 20 (22.989%) | 87 / 87 / 0 (0.000%) | 87 / 83 / 4 (4.598%) | 87 / 87 / 0 (0.000%) |
| java-frontend/src/main/java/org/sonar/java/model/LiteralUtils.java | 399 / 269 / 130 (32.581%) | 399 / 392 / 7 (1.754%) | 399 / 315 / 84 (21.053%) | 399 / 392 / 7 (1.754%) |
| java-frontend/src/main/java/org/sonar/java/model/ModifiersUtils.java | 111 / 59 / 52 (46.847%) | 111 / 110 / 1 (0.901%) | 111 / 92 / 19 (17.117%) | 111 / 110 / 1 (0.901%) |
| java-frontend/src/main/java/org/sonar/java/model/SmapFile.java | 408 / 387 / 21 (5.147%) | 408 / 387 / 21 (5.147%) | 408 / 387 / 21 (5.147%) | 408 / 387 / 21 (5.147%) |
| java-frontend/src/main/java/org/sonar/java/model/Symbols.java | 323 / 230 / 93 (28.793%) | 323 / 319 / 4 (1.238%) | 323 / 233 / 90 (27.864%) | 323 / 319 / 4 (1.238%) |
| java-frontend/src/main/java/org/sonar/java/model/SyntacticEquivalence.java | 300 / 159 / 141 (47.000%) | 300 / 294 / 6 (2.000%) | 300 / 167 / 133 (44.333%) | 300 / 294 / 6 (2.000%) |
| java-frontend/src/main/java/org/sonar/java/model/TypeParameterTreeImpl.java | 97 / 59 / 38 (39.175%) | 97 / 92 / 5 (5.155%) | 97 / 71 / 26 (26.804%) | 97 / 92 / 5 (5.155%) |
| java-frontend/src/main/java/org/sonar/java/model/TypeUtils.java | 37 / 30 / 7 (18.919%) | 37 / 37 / 0 (0.000%) | 37 / 30 / 7 (18.919%) | 37 / 37 / 0 (0.000%) |
| java-frontend/src/main/java/org/sonar/java/model/VisitorsBridge.java | 849 / 497 / 352 (41.461%) | 849 / 775 / 74 (8.716%) | 849 / 677 / 172 (20.259%) | 849 / 775 / 74 (8.716%) |
| java-frontend/src/main/java/org/sonar/java/model/declaration/AnnotationTreeImpl.java | 63 / 45 / 18 (28.571%) | 63 / 63 / 0 (0.000%) | 63 / 53 / 10 (15.873%) | 63 / 63 / 0 (0.000%) |
| java-frontend/src/main/java/org/sonar/java/model/declaration/ClassTreeImpl.java | 381 / 251 / 130 (34.121%) | 381 / 358 / 23 (6.037%) | 381 / 305 / 76 (19.948%) | 381 / 358 / 23 (6.037%) |
| java-frontend/src/main/java/org/sonar/java/model/declaration/EnumConstantTreeImpl.java | 76 / 45 / 31 (40.789%) | 76 / 73 / 3 (3.947%) | 76 / 52 / 24 (31.579%) | 76 / 73 / 3 (3.947%) |
| java-frontend/src/main/java/org/sonar/java/model/declaration/ExportsDirectiveTreeImpl.java | 54 / 30 / 24 (44.444%) | 54 / 52 / 2 (3.704%) | 54 / 42 / 12 (22.222%) | 54 / 52 / 2 (3.704%) |
| java-frontend/src/main/java/org/sonar/java/model/declaration/MethodTreeImpl.java | 476 / 295 / 181 (38.025%) | 476 / 451 / 25 (5.252%) | 476 / 370 / 106 (22.269%) | 476 / 451 / 25 (5.252%) |
| java-frontend/src/main/java/org/sonar/java/model/declaration/ModifierKeywordTreeImpl.java | 28 / 20 / 8 (28.571%) | 28 / 28 / 0 (0.000%) | 28 / 25 / 3 (10.714%) | 28 / 28 / 0 (0.000%) |
| java-frontend/src/main/java/org/sonar/java/model/declaration/ModifiersTreeImpl.java | 81 / 50 / 31 (38.272%) | 81 / 81 / 0 (0.000%) | 81 / 72 / 9 (11.111%) | 81 / 81 / 0 (0.000%) |
| java-frontend/src/main/java/org/sonar/java/model/declaration/ModuleDeclarationTreeImpl.java | 139 / 90 / 49 (35.252%) | 139 / 137 / 2 (1.439%) | 139 / 120 / 19 (13.669%) | 139 / 137 / 2 (1.439%) |
| java-frontend/src/main/java/org/sonar/java/model/declaration/ModuleDirectiveTreeImpl.java | 31 / 23 / 8 (25.806%) | 31 / 31 / 0 (0.000%) | 31 / 30 / 1 (3.226%) | 31 / 31 / 0 (0.000%) |
| java-frontend/src/main/java/org/sonar/java/model/declaration/OpensDirectiveTreeImpl.java | 54 / 30 / 24 (44.444%) | 54 / 52 / 2 (3.704%) | 54 / 42 / 12 (22.222%) | 54 / 52 / 2 (3.704%) |
| java-frontend/src/main/java/org/sonar/java/model/declaration/ProvidesDirectiveTreeImpl.java | 78 / 51 / 27 (34.615%) | 78 / 78 / 0 (0.000%) | 78 / 67 / 11 (14.103%) | 78 / 78 / 0 (0.000%) |
| java-frontend/src/main/java/org/sonar/java/model/declaration/RequiresDirectiveTreeImpl.java | 63 / 42 / 21 (33.333%) | 63 / 63 / 0 (0.000%) | 63 / 52 / 11 (17.460%) | 63 / 63 / 0 (0.000%) |
| java-frontend/src/main/java/org/sonar/java/model/declaration/SimpleModuleDirectiveTreeImpl.java | 67 / 42 / 25 (37.313%) | 67 / 65 / 2 (2.985%) | 67 / 52 / 15 (22.388%) | 67 / 65 / 2 (2.985%) |
| java-frontend/src/main/java/org/sonar/java/model/declaration/UsesDirectiveTreeImpl.java | 51 / 33 / 18 (35.294%) | 51 / 51 / 0 (0.000%) | 51 / 40 / 11 (21.569%) | 51 / 51 / 0 (0.000%) |
| java-frontend/src/main/java/org/sonar/java/model/declaration/VariableTreeImpl.java | 205 / 145 / 60 (29.268%) | 205 / 195 / 10 (4.878%) | 205 / 176 / 29 (14.146%) | 205 / 195 / 10 (4.878%) |
| java-frontend/src/main/java/org/sonar/java/model/declaration/package-info.java | 9 / 7 / 2 (22.222%) | 9 / 8 / 1 (11.111%) | 9 / 7 / 2 (22.222%) | 9 / 8 / 1 (11.111%) |
| java-frontend/src/main/java/org/sonar/java/model/expression/ArrayAccessExpressionTreeImpl.java | 51 / 36 / 15 (29.412%) | 51 / 51 / 0 (0.000%) | 51 / 41 / 10 (19.608%) | 51 / 51 / 0 (0.000%) |
| java-frontend/src/main/java/org/sonar/java/model/expression/AssessableExpressionTree.java | 32 / 28 / 4 (12.500%) | 32 / 32 / 0 (0.000%) | 32 / 29 / 3 (9.375%) | 32 / 32 / 0 (0.000%) |
| java-frontend/src/main/java/org/sonar/java/model/expression/AssignmentExpressionTreeImpl.java | 71 / 53 / 18 (25.352%) | 71 / 71 / 0 (0.000%) | 71 / 64 / 7 (9.859%) | 71 / 71 / 0 (0.000%) |
| java-frontend/src/main/java/org/sonar/java/model/expression/BinaryExpressionTreeImpl.java | 75 / 57 / 18 (24.000%) | 75 / 75 / 0 (0.000%) | 75 / 68 / 7 (9.333%) | 75 / 75 / 0 (0.000%) |
| java-frontend/src/main/java/org/sonar/java/model/expression/ConditionalExpressionTreeImpl.java | 87 / 63 / 24 (27.586%) | 87 / 87 / 0 (0.000%) | 87 / 80 / 7 (8.046%) | 87 / 87 / 0 (0.000%) |
| java-frontend/src/main/java/org/sonar/java/model/expression/IdentifierTreeImpl.java | 133 / 79 / 54 (40.602%) | 133 / 117 / 16 (12.030%) | 133 / 92 / 41 (30.827%) | 133 / 117 / 16 (12.030%) |
| java-frontend/src/main/java/org/sonar/java/model/expression/InstanceOfTreeImpl.java | 139 / 85 / 54 (38.849%) | 139 / 135 / 4 (2.878%) | 139 / 108 / 31 (22.302%) | 139 / 135 / 4 (2.878%) |
| java-frontend/src/main/java/org/sonar/java/model/expression/InternalPostfixUnaryExpression.java | 26 / 17 / 9 (34.615%) | 26 / 26 / 0 (0.000%) | 26 / 19 / 7 (26.923%) | 26 / 26 / 0 (0.000%) |
| java-frontend/src/main/java/org/sonar/java/model/expression/InternalPrefixUnaryExpression.java | 26 / 17 / 9 (34.615%) | 26 / 26 / 0 (0.000%) | 26 / 19 / 7 (26.923%) | 26 / 26 / 0 (0.000%) |
| java-frontend/src/main/java/org/sonar/java/model/expression/InternalUnaryExpression.java | 53 / 40 / 13 (24.528%) | 53 / 53 / 0 (0.000%) | 53 / 48 / 5 (9.434%) | 53 / 53 / 0 (0.000%) |
| java-frontend/src/main/java/org/sonar/java/model/expression/LambdaExpressionTreeImpl.java | 148 / 91 / 57 (38.514%) | 148 / 138 / 10 (6.757%) | 148 / 104 / 44 (29.730%) | 148 / 138 / 10 (6.757%) |
| java-frontend/src/main/java/org/sonar/java/model/expression/LiteralTreeImpl.java | 53 / 39 / 14 (26.415%) | 53 / 53 / 0 (0.000%) | 53 / 45 / 8 (15.094%) | 53 / 53 / 0 (0.000%) |
| java-frontend/src/main/java/org/sonar/java/model/expression/MemberSelectExpressionTreeImpl.java | 92 / 64 / 28 (30.435%) | 92 / 90 / 2 (2.174%) | 92 / 81 / 11 (11.957%) | 92 / 90 / 2 (2.174%) |
| java-frontend/src/main/java/org/sonar/java/model/expression/MethodInvocationTreeImpl.java | 119 / 73 / 46 (38.655%) | 119 / 112 / 7 (5.882%) | 119 / 86 / 33 (27.731%) | 119 / 112 / 7 (5.882%) |
| java-frontend/src/main/java/org/sonar/java/model/expression/MethodReferenceTreeImpl.java | 86 / 60 / 26 (30.233%) | 86 / 82 / 4 (4.651%) | 86 / 70 / 16 (18.605%) | 86 / 82 / 4 (4.651%) |
| java-frontend/src/main/java/org/sonar/java/model/expression/NewArrayTreeImpl.java | 177 / 120 / 57 (32.203%) | 177 / 170 / 7 (3.955%) | 177 / 142 / 35 (19.774%) | 177 / 170 / 7 (3.955%) |
| java-frontend/src/main/java/org/sonar/java/model/expression/NewClassTreeImpl.java | 225 / 141 / 84 (37.333%) | 225 / 213 / 12 (5.333%) | 225 / 167 / 58 (25.778%) | 225 / 213 / 12 (5.333%) |
| java-frontend/src/main/java/org/sonar/java/model/expression/ParenthesizedTreeImpl.java | 65 / 47 / 18 (27.692%) | 65 / 65 / 0 (0.000%) | 65 / 58 / 7 (10.769%) | 65 / 65 / 0 (0.000%) |
| java-frontend/src/main/java/org/sonar/java/model/expression/TypeArgumentListTreeImpl.java | 61 / 41 / 20 (32.787%) | 61 / 59 / 2 (3.279%) | 61 / 50 / 11 (18.033%) | 61 / 59 / 2 (3.279%) |
| java-frontend/src/main/java/org/sonar/java/model/expression/TypeCastExpressionTreeImpl.java | 131 / 89 / 42 (32.061%) | 131 / 126 / 5 (3.817%) | 131 / 117 / 14 (10.687%) | 131 / 126 / 5 (3.817%) |
| java-frontend/src/main/java/org/sonar/java/model/expression/VarTypeTreeImpl.java | 65 / 43 / 22 (33.846%) | 65 / 65 / 0 (0.000%) | 65 / 54 / 11 (16.923%) | 65 / 65 / 0 (0.000%) |
| java-frontend/src/main/java/org/sonar/java/model/expression/package-info.java | 9 / 7 / 2 (22.222%) | 9 / 8 / 1 (11.111%) | 9 / 7 / 2 (22.222%) | 9 / 8 / 1 (11.111%) |
| java-frontend/src/main/java/org/sonar/java/model/location/InternalPosition.java | 114 / 100 / 14 (12.281%) | 114 / 113 / 1 (0.877%) | 114 / 113 / 1 (0.877%) | 114 / 113 / 1 (0.877%) |
| java-frontend/src/main/java/org/sonar/java/model/location/InternalRange.java | 103 / 83 / 20 (19.417%) | 103 / 103 / 0 (0.000%) | 103 / 103 / 0 (0.000%) | 103 / 103 / 0 (0.000%) |
| java-frontend/src/main/java/org/sonar/java/model/location/package-info.java | 9 / 7 / 2 (22.222%) | 9 / 8 / 1 (11.111%) | 9 / 7 / 2 (22.222%) | 9 / 8 / 1 (11.111%) |
| java-frontend/src/main/java/org/sonar/java/model/package-info.java | 8 / 6 / 2 (25.000%) | 8 / 7 / 1 (12.500%) | 8 / 6 / 2 (25.000%) | 8 / 7 / 1 (12.500%) |
| java-frontend/src/main/java/org/sonar/java/model/pattern/AbstractPatternTree.java | 58 / 40 / 18 (31.034%) | 58 / 54 / 4 (6.897%) | 58 / 40 / 18 (31.034%) | 58 / 54 / 4 (6.897%) |
| java-frontend/src/main/java/org/sonar/java/model/pattern/DefaultPatternTreeImpl.java | 42 / 27 / 15 (35.714%) | 42 / 40 / 2 (4.762%) | 42 / 32 / 10 (23.810%) | 42 / 40 / 2 (4.762%) |
| java-frontend/src/main/java/org/sonar/java/model/pattern/GuardedPatternTreeImpl.java | 66 / 45 / 21 (31.818%) | 66 / 64 / 2 (3.030%) | 66 / 56 / 10 (15.152%) | 66 / 64 / 2 (3.030%) |
| java-frontend/src/main/java/org/sonar/java/model/pattern/NullPatternTreeImpl.java | 60 / 43 / 17 (28.333%) | 60 / 60 / 0 (0.000%) | 60 / 51 / 9 (15.000%) | 60 / 60 / 0 (0.000%) |
| java-frontend/src/main/java/org/sonar/java/model/pattern/RecordPatternTreeImpl.java | 90 / 59 / 31 (34.444%) | 90 / 88 / 2 (2.222%) | 90 / 77 / 13 (14.444%) | 90 / 88 / 2 (2.222%) |
| java-frontend/src/main/java/org/sonar/java/model/pattern/TypePatternTreeImpl.java | 42 / 27 / 15 (35.714%) | 42 / 40 / 2 (4.762%) | 42 / 29 / 13 (30.952%) | 42 / 40 / 2 (4.762%) |
| java-frontend/src/main/java/org/sonar/java/model/pattern/package-info.java | 9 / 7 / 2 (22.222%) | 9 / 8 / 1 (11.111%) | 9 / 7 / 2 (22.222%) | 9 / 8 / 1 (11.111%) |
| java-frontend/src/main/java/org/sonar/java/model/springcontext/BeanDefinitionGatherer.java | 416 / 185 / 231 (55.529%) | 416 / 331 / 85 (20.433%) | 416 / 220 / 196 (47.115%) | 416 / 331 / 85 (20.433%) |
| java-frontend/src/main/java/org/sonar/java/model/springcontext/BeanDefinitionHolder.java | 287 / 247 / 40 (13.937%) | 287 / 281 / 6 (2.091%) | 287 / 262 / 25 (8.711%) | 287 / 281 / 6 (2.091%) |
| java-frontend/src/main/java/org/sonar/java/model/springcontext/BeanDefinitionRegistry.java | 77 / 49 / 28 (36.364%) | 77 / 77 / 0 (0.000%) | 77 / 50 / 27 (35.065%) | 77 / 77 / 0 (0.000%) |
| java-frontend/src/main/java/org/sonar/java/model/springcontext/BeanLocation.java | 24 / 17 / 7 (29.167%) | 24 / 23 / 1 (4.167%) | 24 / 18 / 6 (25.000%) | 24 / 23 / 1 (4.167%) |
| java-frontend/src/main/java/org/sonar/java/model/springcontext/ComponentScanPackageGatherer.java | 328 / 232 / 96 (29.268%) | 328 / 322 / 6 (1.829%) | 328 / 234 / 94 (28.659%) | 328 / 322 / 6 (1.829%) |
| java-frontend/src/main/java/org/sonar/java/model/springcontext/EntityClassToPropertiesIndex.java | 87 / 78 / 9 (10.345%) | 87 / 87 / 0 (0.000%) | 87 / 79 / 8 (9.195%) | 87 / 87 / 0 (0.000%) |
| java-frontend/src/main/java/org/sonar/java/model/springcontext/InjectionPoint.java | 42 / 31 / 11 (26.190%) | 42 / 42 / 0 (0.000%) | 42 / 33 / 9 (21.429%) | 42 / 42 / 0 (0.000%) |
| java-frontend/src/main/java/org/sonar/java/model/springcontext/ProfileExpression.java | 417 / 405 / 12 (2.878%) | 417 / 417 / 0 (0.000%) | 417 / 406 / 11 (2.638%) | 417 / 417 / 0 (0.000%) |
| java-frontend/src/main/java/org/sonar/java/model/springcontext/ProfileExpressionParser.java | 147 / 124 / 23 (15.646%) | 147 / 147 / 0 (0.000%) | 147 / 147 / 0 (0.000%) | 147 / 147 / 0 (0.000%) |
| java-frontend/src/main/java/org/sonar/java/model/springcontext/ProjectPackageScan.java | 92 / 85 / 7 (7.609%) | 92 / 92 / 0 (0.000%) | 92 / 86 / 6 (6.522%) | 92 / 92 / 0 (0.000%) |
| java-frontend/src/main/java/org/sonar/java/model/springcontext/SpringContextCacheHelper.java | 94 / 48 / 46 (48.936%) | 94 / 84 / 10 (10.638%) | 94 / 48 / 46 (48.936%) | 94 / 84 / 10 (10.638%) |
| java-frontend/src/main/java/org/sonar/java/model/springcontext/SpringContextModel.java | 61 / 36 / 25 (40.984%) | 61 / 59 / 2 (3.279%) | 61 / 52 / 9 (14.754%) | 61 / 59 / 2 (3.279%) |
| java-frontend/src/main/java/org/sonar/java/model/springcontext/SpringContextModelGatherer.java | 130 / 95 / 35 (26.923%) | 130 / 130 / 0 (0.000%) | 130 / 114 / 16 (12.308%) | 130 / 130 / 0 (0.000%) |
| java-frontend/src/main/java/org/sonar/java/model/springcontext/SpringContextModelGatherers.java | 18 / 13 / 5 (27.778%) | 18 / 18 / 0 (0.000%) | 18 / 15 / 3 (16.667%) | 18 / 18 / 0 (0.000%) |
| java-frontend/src/main/java/org/sonar/java/model/springcontext/SpringContextModelMetrics.java | 36 / 26 / 10 (27.778%) | 36 / 36 / 0 (0.000%) | 36 / 26 / 10 (27.778%) | 36 / 36 / 0 (0.000%) |
| java-frontend/src/main/java/org/sonar/java/model/springcontext/TypeToBeansIndex.java | 153 / 140 / 13 (8.497%) | 153 / 153 / 0 (0.000%) | 153 / 142 / 11 (7.190%) | 153 / 153 / 0 (0.000%) |
| java-frontend/src/main/java/org/sonar/java/model/springcontext/TypeToDependenciesIndex.java | 88 / 59 / 29 (32.955%) | 88 / 88 / 0 (0.000%) | 88 / 81 / 7 (7.955%) | 88 / 88 / 0 (0.000%) |
| java-frontend/src/main/java/org/sonar/java/model/springcontext/package-info.java | 7 / 5 / 2 (28.571%) | 7 / 6 / 1 (14.286%) | 7 / 5 / 2 (28.571%) | 7 / 6 / 1 (14.286%) |
| java-frontend/src/main/java/org/sonar/java/model/statement/AssertStatementTreeImpl.java | 108 / 77 / 31 (28.704%) | 108 / 102 / 6 (5.556%) | 108 / 94 / 14 (12.963%) | 108 / 102 / 6 (5.556%) |
| java-frontend/src/main/java/org/sonar/java/model/statement/BlockTreeImpl.java | 92 / 63 / 29 (31.522%) | 92 / 90 / 2 (2.174%) | 92 / 81 / 11 (11.957%) | 92 / 90 / 2 (2.174%) |
| java-frontend/src/main/java/org/sonar/java/model/statement/BreakStatementTreeImpl.java | 80 / 54 / 26 (32.500%) | 80 / 75 / 5 (6.250%) | 80 / 66 / 14 (17.500%) | 80 / 75 / 5 (6.250%) |
| java-frontend/src/main/java/org/sonar/java/model/statement/CaseGroupTreeImpl.java | 62 / 40 / 22 (35.484%) | 62 / 60 / 2 (3.226%) | 62 / 52 / 10 (16.129%) | 62 / 60 / 2 (3.226%) |
| java-frontend/src/main/java/org/sonar/java/model/statement/CaseLabelTreeImpl.java | 82 / 54 / 28 (34.146%) | 82 / 80 / 2 (2.439%) | 82 / 70 / 12 (14.634%) | 82 / 80 / 2 (2.439%) |
| java-frontend/src/main/java/org/sonar/java/model/statement/CatchTreeImpl.java | 91 / 67 / 24 (26.374%) | 91 / 91 / 0 (0.000%) | 91 / 81 / 10 (10.989%) | 91 / 91 / 0 (0.000%) |
| java-frontend/src/main/java/org/sonar/java/model/statement/ContinueStatementTreeImpl.java | 76 / 52 / 24 (31.579%) | 76 / 71 / 5 (6.579%) | 76 / 62 / 14 (18.421%) | 76 / 71 / 5 (6.579%) |
| java-frontend/src/main/java/org/sonar/java/model/statement/DoWhileStatementTreeImpl.java | 115 / 85 / 30 (26.087%) | 115 / 115 / 0 (0.000%) | 115 / 108 / 7 (6.087%) | 115 / 115 / 0 (0.000%) |
| java-frontend/src/main/java/org/sonar/java/model/statement/EmptyStatementTreeImpl.java | 40 / 27 / 13 (32.500%) | 40 / 40 / 0 (0.000%) | 40 / 32 / 8 (20.000%) | 40 / 40 / 0 (0.000%) |
| java-frontend/src/main/java/org/sonar/java/model/statement/ExpressionStatementTreeImpl.java | 62 / 43 / 19 (30.645%) | 62 / 59 / 3 (4.839%) | 62 / 51 / 11 (17.742%) | 62 / 59 / 3 (4.839%) |
| java-frontend/src/main/java/org/sonar/java/model/statement/ForEachStatementImpl.java | 117 / 87 / 30 (25.641%) | 117 / 117 / 0 (0.000%) | 117 / 107 / 10 (8.547%) | 117 / 117 / 0 (0.000%) |
| java-frontend/src/main/java/org/sonar/java/model/statement/ForStatementTreeImpl.java | 174 / 118 / 56 (32.184%) | 174 / 171 / 3 (1.724%) | 174 / 153 / 21 (12.069%) | 174 / 171 / 3 (1.724%) |
| java-frontend/src/main/java/org/sonar/java/model/statement/IfStatementTreeImpl.java | 125 / 86 / 39 (31.200%) | 125 / 117 / 8 (6.400%) | 125 / 109 / 16 (12.800%) | 125 / 117 / 8 (6.400%) |
| java-frontend/src/main/java/org/sonar/java/model/statement/LabeledStatementTreeImpl.java | 74 / 53 / 21 (28.378%) | 74 / 74 / 0 (0.000%) | 74 / 64 / 10 (13.514%) | 74 / 74 / 0 (0.000%) |
| java-frontend/src/main/java/org/sonar/java/model/statement/ReturnStatementTreeImpl.java | 78 / 52 / 26 (33.333%) | 78 / 75 / 3 (3.846%) | 78 / 63 / 15 (19.231%) | 78 / 75 / 3 (3.846%) |
| java-frontend/src/main/java/org/sonar/java/model/statement/StaticInitializerTreeImpl.java | 51 / 30 / 21 (41.176%) | 51 / 51 / 0 (0.000%) | 51 / 37 / 14 (27.451%) | 51 / 51 / 0 (0.000%) |
| java-frontend/src/main/java/org/sonar/java/model/statement/SwitchExpressionTreeImpl.java | 44 / 28 / 16 (36.364%) | 44 / 44 / 0 (0.000%) | 44 / 36 / 8 (18.182%) | 44 / 44 / 0 (0.000%) |
| java-frontend/src/main/java/org/sonar/java/model/statement/SwitchStatementTreeImpl.java | 45 / 28 / 17 (37.778%) | 45 / 45 / 0 (0.000%) | 45 / 36 / 9 (20.000%) | 45 / 45 / 0 (0.000%) |
| java-frontend/src/main/java/org/sonar/java/model/statement/SwitchTreeImpl.java | 112 / 82 / 30 (26.786%) | 112 / 110 / 2 (1.786%) | 112 / 105 / 7 (6.250%) | 112 / 110 / 2 (1.786%) |
| java-frontend/src/main/java/org/sonar/java/model/statement/SynchronizedStatementTreeImpl.java | 91 / 67 / 24 (26.374%) | 91 / 91 / 0 (0.000%) | 91 / 83 / 8 (8.791%) | 91 / 91 / 0 (0.000%) |
| java-frontend/src/main/java/org/sonar/java/model/statement/ThrowStatementTreeImpl.java | 65 / 47 / 18 (27.692%) | 65 / 65 / 0 (0.000%) | 65 / 58 / 7 (10.769%) | 65 / 65 / 0 (0.000%) |
| java-frontend/src/main/java/org/sonar/java/model/statement/TryStatementTreeImpl.java | 162 / 102 / 60 (37.037%) | 162 / 150 / 12 (7.407%) | 162 / 126 / 36 (22.222%) | 162 / 150 / 12 (7.407%) |
| java-frontend/src/main/java/org/sonar/java/model/statement/WhileStatementTreeImpl.java | 91 / 67 / 24 (26.374%) | 91 / 91 / 0 (0.000%) | 91 / 84 / 7 (7.692%) | 91 / 91 / 0 (0.000%) |
| java-frontend/src/main/java/org/sonar/java/model/statement/YieldStatementTreeImpl.java | 71 / 50 / 21 (29.577%) | 71 / 68 / 3 (4.225%) | 71 / 61 / 10 (14.085%) | 71 / 68 / 3 (4.225%) |
| java-frontend/src/main/java/org/sonar/java/model/statement/package-info.java | 8 / 7 / 1 (12.500%) | 8 / 7 / 1 (12.500%) | 8 / 7 / 1 (12.500%) | 8 / 7 / 1 (12.500%) |
| java-frontend/src/main/java/org/sonar/java/package-info.java | 4 / 3 / 1 (25.000%) | 4 / 3 / 1 (25.000%) | 4 / 3 / 1 (25.000%) | 4 / 3 / 1 (25.000%) |
| java-frontend/src/main/java/org/sonar/java/regex/JavaAnalyzerRegexSource.java | 395 / 270 / 125 (31.646%) | 395 / 381 / 14 (3.544%) | 395 / 298 / 97 (24.557%) | 395 / 381 / 14 (3.544%) |
| java-frontend/src/main/java/org/sonar/java/regex/RegexCache.java | 38 / 14 / 24 (63.158%) | 38 / 17 / 21 (55.263%) | 38 / 17 / 21 (55.263%) | 38 / 17 / 21 (55.263%) |
| java-frontend/src/main/java/org/sonar/java/regex/RegexCheck.java | 159 / 99 / 60 (37.736%) | 159 / 132 / 27 (16.981%) | 159 / 100 / 59 (37.107%) | 159 / 132 / 27 (16.981%) |
| java-frontend/src/main/java/org/sonar/java/regex/RegexScannerContext.java | 39 / 24 / 15 (38.462%) | 39 / 34 / 5 (12.821%) | 39 / 25 / 14 (35.897%) | 39 / 34 / 5 (12.821%) |
| java-frontend/src/main/java/org/sonar/java/regex/package-info.java | 5 / 4 / 1 (20.000%) | 5 / 4 / 1 (20.000%) | 5 / 4 / 1 (20.000%) | 5 / 4 / 1 (20.000%) |
| java-frontend/src/main/java/org/sonar/java/reporting/AnalyzerMessage.java | 315 / 269 / 46 (14.603%) | 315 / 306 / 9 (2.857%) | 315 / 293 / 22 (6.984%) | 315 / 306 / 9 (2.857%) |
| java-frontend/src/main/java/org/sonar/java/reporting/FluentReporting.java | 72 / 53 / 19 (26.389%) | 72 / 72 / 0 (0.000%) | 72 / 60 / 12 (16.667%) | 72 / 72 / 0 (0.000%) |
| java-frontend/src/main/java/org/sonar/java/reporting/InternalJavaIssueBuilder.java | 587 / 391 / 196 (33.390%) | 587 / 527 / 60 (10.221%) | 587 / 449 / 138 (23.509%) | 587 / 527 / 60 (10.221%) |
| java-frontend/src/main/java/org/sonar/java/reporting/JavaIssue.java | 173 / 103 / 70 (40.462%) | 173 / 127 / 46 (26.590%) | 173 / 103 / 70 (40.462%) | 173 / 127 / 46 (26.590%) |
| java-frontend/src/main/java/org/sonar/java/reporting/JavaQuickFix.java | 143 / 111 / 32 (22.378%) | 143 / 143 / 0 (0.000%) | 143 / 131 / 12 (8.392%) | 143 / 143 / 0 (0.000%) |
| java-frontend/src/main/java/org/sonar/java/reporting/JavaTextEdit.java | 178 / 138 / 40 (22.472%) | 178 / 178 / 0 (0.000%) | 178 / 150 / 28 (15.730%) | 178 / 178 / 0 (0.000%) |
| java-frontend/src/main/java/org/sonar/java/reporting/package-info.java | 5 / 4 / 1 (20.000%) | 5 / 4 / 1 (20.000%) | 5 / 4 / 1 (20.000%) | 5 / 4 / 1 (20.000%) |
| java-frontend/src/main/java/org/sonar/java/serialization/BeanDefinitionHolderTypeAdapter.java | 347 / 171 / 176 (50.720%) | 347 / 296 / 51 (14.697%) | 347 / 215 / 132 (38.040%) | 347 / 296 / 51 (14.697%) |
| java-frontend/src/main/java/org/sonar/java/serialization/InjectionPointTypeAdapter.java | 97 / 49 / 48 (49.485%) | 97 / 81 / 16 (16.495%) | 97 / 60 / 37 (38.144%) | 97 / 81 / 16 (16.495%) |
| java-frontend/src/main/java/org/sonar/java/serialization/JsonUtils.java | 294 / 230 / 64 (21.769%) | 294 / 230 / 64 (21.769%) | 294 / 230 / 64 (21.769%) | 294 / 230 / 64 (21.769%) |
| java-frontend/src/main/java/org/sonar/java/serialization/TextSpanTypeAdapter.java | 132 / 80 / 52 (39.394%) | 132 / 114 / 18 (13.636%) | 132 / 80 / 52 (39.394%) | 132 / 114 / 18 (13.636%) |
| java-frontend/src/main/java/org/sonar/java/serialization/package-info.java | 5 / 4 / 1 (20.000%) | 5 / 4 / 1 (20.000%) | 5 / 4 / 1 (20.000%) | 5 / 4 / 1 (20.000%) |
| java-frontend/src/main/java/org/sonar/java/telemetry/AlphaNumericComparator.java | 87 / 85 / 2 (2.299%) | 87 / 85 / 2 (2.299%) | 87 / 85 / 2 (2.299%) | 87 / 85 / 2 (2.299%) |
| java-frontend/src/main/java/org/sonar/java/telemetry/DefaultTelemetry.java | 157 / 95 / 62 (39.490%) | 157 / 157 / 0 (0.000%) | 157 / 152 / 5 (3.185%) | 157 / 157 / 0 (0.000%) |
| java-frontend/src/main/java/org/sonar/java/telemetry/NoOpTelemetry.java | 33 / 28 / 5 (15.152%) | 33 / 33 / 0 (0.000%) | 33 / 32 / 1 (3.030%) | 33 / 33 / 0 (0.000%) |
| java-frontend/src/main/java/org/sonar/java/telemetry/SizeEstimable.java | 8 / 7 / 1 (12.500%) | 8 / 8 / 0 (0.000%) | 8 / 7 / 1 (12.500%) | 8 / 8 / 0 (0.000%) |
| java-frontend/src/main/java/org/sonar/java/telemetry/SizeEstimator.java | 151 / 142 / 9 (5.960%) | 151 / 148 / 3 (1.987%) | 151 / 148 / 3 (1.987%) | 151 / 148 / 3 (1.987%) |
| java-frontend/src/main/java/org/sonar/java/telemetry/Telemetry.java | 28 / 21 / 7 (25.000%) | 28 / 25 / 3 (10.714%) | 28 / 25 / 3 (10.714%) | 28 / 25 / 3 (10.714%) |
| java-frontend/src/main/java/org/sonar/java/telemetry/TelemetryKey.java | 157 / 157 / 0 (0.000%) | 157 / 157 / 0 (0.000%) | 157 / 157 / 0 (0.000%) | 157 / 157 / 0 (0.000%) |
| java-frontend/src/main/java/org/sonar/java/telemetry/package-info.java | 5 / 4 / 1 (20.000%) | 5 / 4 / 1 (20.000%) | 5 / 4 / 1 (20.000%) | 5 / 4 / 1 (20.000%) |
| java-frontend/src/main/java/org/sonar/java/testing/JavaFileScannerContextForTests.java | 268 / 158 / 110 (41.045%) | 268 / 257 / 11 (4.104%) | 268 / 187 / 81 (30.224%) | 268 / 257 / 11 (4.104%) |
| java-frontend/src/main/java/org/sonar/java/testing/JavaIssueBuilderForTests.java | 178 / 64 / 114 (64.045%) | 178 / 167 / 11 (6.180%) | 178 / 74 / 104 (58.427%) | 178 / 167 / 11 (6.180%) |
| java-frontend/src/main/java/org/sonar/java/testing/VisitorsBridgeForTests.java | 190 / 144 / 46 (24.211%) | 190 / 185 / 5 (2.632%) | 190 / 158 / 32 (16.842%) | 190 / 185 / 5 (2.632%) |
| java-frontend/src/main/java/org/sonar/java/testing/package-info.java | 5 / 4 / 1 (20.000%) | 5 / 4 / 1 (20.000%) | 5 / 4 / 1 (20.000%) | 5 / 4 / 1 (20.000%) |
| java-frontend/src/main/java/org/sonar/java/utils/JavaFileTypeClassifier.java | 126 / 68 / 58 (46.032%) | 126 / 108 / 18 (14.286%) | 126 / 68 / 58 (46.032%) | 126 / 108 / 18 (14.286%) |
| java-frontend/src/main/java/org/sonar/java/utils/ModuleMetadataUtils.java | 88 / 63 / 25 (28.409%) | 88 / 63 / 25 (28.409%) | 88 / 63 / 25 (28.409%) | 88 / 63 / 25 (28.409%) |
| java-frontend/src/main/java/org/sonar/java/utils/PackageUtils.java | 73 / 52 / 21 (28.767%) | 73 / 72 / 1 (1.370%) | 73 / 62 / 11 (15.068%) | 73 / 72 / 1 (1.370%) |
| java-frontend/src/main/java/org/sonar/java/utils/SpringUtils.java | 616 / 318 / 298 (48.377%) | 616 / 614 / 2 (0.325%) | 616 / 358 / 258 (41.883%) | 616 / 614 / 2 (0.325%) |
| java-frontend/src/main/java/org/sonar/java/utils/StringUtils.java | 177 / 174 / 3 (1.695%) | 177 / 174 / 3 (1.695%) | 177 / 174 / 3 (1.695%) | 177 / 174 / 3 (1.695%) |
| java-frontend/src/main/java/org/sonar/java/utils/UnitTestUtils.java | 547 / 253 / 294 (53.748%) | 547 / 546 / 1 (0.183%) | 547 / 380 / 167 (30.530%) | 547 / 546 / 1 (0.183%) |
| java-frontend/src/main/java/org/sonar/java/utils/package-info.java | 5 / 4 / 1 (20.000%) | 5 / 4 / 1 (20.000%) | 5 / 4 / 1 (20.000%) | 5 / 4 / 1 (20.000%) |
| java-frontend/src/main/java/org/sonar/plugins/java/api/CheckRegistrar.java | 153 / 106 / 47 (30.719%) | 153 / 134 / 19 (12.418%) | 153 / 134 / 19 (12.418%) | 153 / 134 / 19 (12.418%) |
| java-frontend/src/main/java/org/sonar/plugins/java/api/DependencyVersionAware.java | 12 / 9 / 3 (25.000%) | 12 / 12 / 0 (0.000%) | 12 / 12 / 0 (0.000%) | 12 / 12 / 0 (0.000%) |
| java-frontend/src/main/java/org/sonar/plugins/java/api/InputFileScannerContext.java | 29 / 23 / 6 (20.690%) | 29 / 27 / 2 (6.897%) | 29 / 26 / 3 (10.345%) | 29 / 27 / 2 (6.897%) |
| java-frontend/src/main/java/org/sonar/plugins/java/api/IssuableSubscriptionVisitor.java | 114 / 82 / 32 (28.070%) | 114 / 112 / 2 (1.754%) | 114 / 90 / 24 (21.053%) | 114 / 112 / 2 (1.754%) |
| java-frontend/src/main/java/org/sonar/plugins/java/api/JavaCheck.java | 6 / 6 / 0 (0.000%) | 6 / 6 / 0 (0.000%) | 6 / 6 / 0 (0.000%) | 6 / 6 / 0 (0.000%) |
| java-frontend/src/main/java/org/sonar/plugins/java/api/JavaFileScanner.java | 15 / 10 / 5 (33.333%) | 15 / 15 / 0 (0.000%) | 15 / 13 / 2 (13.333%) | 15 / 15 / 0 (0.000%) |
| java-frontend/src/main/java/org/sonar/plugins/java/api/JavaFileScannerContext.java | 135 / 109 / 26 (19.259%) | 135 / 131 / 4 (2.963%) | 135 / 115 / 20 (14.815%) | 135 / 131 / 4 (2.963%) |
| java-frontend/src/main/java/org/sonar/plugins/java/api/JavaResourceLocator.java | 30 / 24 / 6 (20.000%) | 30 / 26 / 4 (13.333%) | 30 / 26 / 4 (13.333%) | 30 / 26 / 4 (13.333%) |
| java-frontend/src/main/java/org/sonar/plugins/java/api/JavaVersion.java | 42 / 41 / 1 (2.381%) | 42 / 42 / 0 (0.000%) | 42 / 42 / 0 (0.000%) | 42 / 42 / 0 (0.000%) |
| java-frontend/src/main/java/org/sonar/plugins/java/api/JavaVersionAwareVisitor.java | 10 / 8 / 2 (20.000%) | 10 / 10 / 0 (0.000%) | 10 / 10 / 0 (0.000%) | 10 / 10 / 0 (0.000%) |
| java-frontend/src/main/java/org/sonar/plugins/java/api/JspCodeVisitor.java | 8 / 6 / 2 (25.000%) | 8 / 8 / 0 (0.000%) | 8 / 8 / 0 (0.000%) | 8 / 8 / 0 (0.000%) |
| java-frontend/src/main/java/org/sonar/plugins/java/api/ModuleScannerContext.java | 34 / 27 / 7 (20.588%) | 34 / 30 / 4 (11.765%) | 34 / 30 / 4 (11.765%) | 34 / 30 / 4 (11.765%) |
| java-frontend/src/main/java/org/sonar/plugins/java/api/ProfileRegistrar.java | 25 / 18 / 7 (28.000%) | 25 / 19 / 6 (24.000%) | 25 / 19 / 6 (24.000%) | 25 / 19 / 6 (24.000%) |
| java-frontend/src/main/java/org/sonar/plugins/java/api/SourceMap.java | 17 / 14 / 3 (17.647%) | 17 / 16 / 1 (5.882%) | 17 / 15 / 2 (11.765%) | 17 / 16 / 1 (5.882%) |
| java-frontend/src/main/java/org/sonar/plugins/java/api/Version.java | 38 / 38 / 0 (0.000%) | 38 / 38 / 0 (0.000%) | 38 / 38 / 0 (0.000%) | 38 / 38 / 0 (0.000%) |
| java-frontend/src/main/java/org/sonar/plugins/java/api/caching/CacheContext.java | 12 / 10 / 2 (16.667%) | 12 / 12 / 0 (0.000%) | 12 / 11 / 1 (8.333%) | 12 / 12 / 0 (0.000%) |
| java-frontend/src/main/java/org/sonar/plugins/java/api/caching/JavaReadCache.java | 18 / 17 / 1 (5.556%) | 18 / 17 / 1 (5.556%) | 18 / 17 / 1 (5.556%) | 18 / 17 / 1 (5.556%) |
| java-frontend/src/main/java/org/sonar/plugins/java/api/caching/JavaWriteCache.java | 19 / 19 / 0 (0.000%) | 19 / 19 / 0 (0.000%) | 19 / 19 / 0 (0.000%) | 19 / 19 / 0 (0.000%) |
| java-frontend/src/main/java/org/sonar/plugins/java/api/caching/SonarLintCache.java | 67 / 63 / 4 (5.970%) | 67 / 63 / 4 (5.970%) | 67 / 63 / 4 (5.970%) | 67 / 63 / 4 (5.970%) |
| java-frontend/src/main/java/org/sonar/plugins/java/api/caching/package-info.java | 7 / 6 / 1 (14.286%) | 7 / 6 / 1 (14.286%) | 7 / 6 / 1 (14.286%) | 7 / 6 / 1 (14.286%) |
| java-frontend/src/main/java/org/sonar/plugins/java/api/cfg/ControlFlowGraph.java | 51 / 42 / 9 (17.647%) | 51 / 49 / 2 (3.922%) | 51 / 44 / 7 (13.725%) | 51 / 49 / 2 (3.922%) |
| java-frontend/src/main/java/org/sonar/plugins/java/api/cfg/package-info.java | 10 / 8 / 2 (20.000%) | 10 / 9 / 1 (10.000%) | 10 / 8 / 2 (20.000%) | 10 / 9 / 1 (10.000%) |
| java-frontend/src/main/java/org/sonar/plugins/java/api/internal/EndOfAnalysis.java | 11 / 9 / 2 (18.182%) | 11 / 11 / 0 (0.000%) | 11 / 10 / 1 (9.091%) | 11 / 11 / 0 (0.000%) |
| java-frontend/src/main/java/org/sonar/plugins/java/api/internal/ModuleMetadata.java | 14 / 11 / 3 (21.429%) | 14 / 13 / 1 (7.143%) | 14 / 13 / 1 (7.143%) | 14 / 13 / 1 (7.143%) |
| java-frontend/src/main/java/org/sonar/plugins/java/api/internal/package-info.java | 7 / 6 / 1 (14.286%) | 7 / 6 / 1 (14.286%) | 7 / 6 / 1 (14.286%) | 7 / 6 / 1 (14.286%) |
| java-frontend/src/main/java/org/sonar/plugins/java/api/location/Position.java | 84 / 64 / 20 (23.810%) | 84 / 84 / 0 (0.000%) | 84 / 76 / 8 (9.524%) | 84 / 84 / 0 (0.000%) |
| java-frontend/src/main/java/org/sonar/plugins/java/api/location/Range.java | 58 / 40 / 18 (31.034%) | 58 / 58 / 0 (0.000%) | 58 / 58 / 0 (0.000%) | 58 / 58 / 0 (0.000%) |
| java-frontend/src/main/java/org/sonar/plugins/java/api/location/package-info.java | 10 / 8 / 2 (20.000%) | 10 / 9 / 1 (10.000%) | 10 / 8 / 2 (20.000%) | 10 / 9 / 1 (10.000%) |
| java-frontend/src/main/java/org/sonar/plugins/java/api/package-info.java | 6 / 5 / 1 (16.667%) | 6 / 5 / 1 (16.667%) | 6 / 5 / 1 (16.667%) | 6 / 5 / 1 (16.667%) |
| java-frontend/src/main/java/org/sonar/plugins/java/api/semantic/MethodMatchers.java | 109 / 94 / 15 (13.761%) | 109 / 109 / 0 (0.000%) | 109 / 98 / 11 (10.092%) | 109 / 109 / 0 (0.000%) |
| java-frontend/src/main/java/org/sonar/plugins/java/api/semantic/Sema.java | 11 / 10 / 1 (9.091%) | 11 / 11 / 0 (0.000%) | 11 / 10 / 1 (9.091%) | 11 / 11 / 0 (0.000%) |
| java-frontend/src/main/java/org/sonar/plugins/java/api/semantic/Symbol.java | 133 / 98 / 35 (26.316%) | 133 / 126 / 7 (5.263%) | 133 / 103 / 30 (22.556%) | 133 / 126 / 7 (5.263%) |
| java-frontend/src/main/java/org/sonar/plugins/java/api/semantic/SymbolMetadata.java | 106 / 99 / 7 (6.604%) | 106 / 102 / 4 (3.774%) | 106 / 100 / 6 (5.660%) | 106 / 102 / 4 (3.774%) |
| java-frontend/src/main/java/org/sonar/plugins/java/api/semantic/Type.java | 78 / 72 / 6 (7.692%) | 78 / 76 / 2 (2.564%) | 78 / 72 / 6 (7.692%) | 78 / 76 / 2 (2.564%) |
| java-frontend/src/main/java/org/sonar/plugins/java/api/semantic/package-info.java | 10 / 8 / 2 (20.000%) | 10 / 9 / 1 (10.000%) | 10 / 8 / 2 (20.000%) | 10 / 9 / 1 (10.000%) |
| java-frontend/src/main/java/org/sonar/plugins/java/api/tree/AnnotationTree.java | 16 / 10 / 6 (37.500%) | 16 / 16 / 0 (0.000%) | 16 / 15 / 1 (6.250%) | 16 / 16 / 0 (0.000%) |
| java-frontend/src/main/java/org/sonar/plugins/java/api/tree/Arguments.java | 15 / 9 / 6 (40.000%) | 15 / 13 / 2 (13.333%) | 15 / 13 / 2 (13.333%) | 15 / 13 / 2 (13.333%) |
| java-frontend/src/main/java/org/sonar/plugins/java/api/tree/ArrayAccessExpressionTree.java | 13 / 9 / 4 (30.769%) | 13 / 13 / 0 (0.000%) | 13 / 12 / 1 (7.692%) | 13 / 13 / 0 (0.000%) |
| java-frontend/src/main/java/org/sonar/plugins/java/api/tree/ArrayDimensionTree.java | 19 / 11 / 8 (42.105%) | 19 / 18 / 1 (5.263%) | 19 / 17 / 2 (10.526%) | 19 / 18 / 1 (5.263%) |
| java-frontend/src/main/java/org/sonar/plugins/java/api/tree/ArrayTypeTree.java | 21 / 11 / 10 (47.619%) | 21 / 18 / 3 (14.286%) | 21 / 18 / 3 (14.286%) | 21 / 18 / 3 (14.286%) |
| java-frontend/src/main/java/org/sonar/plugins/java/api/tree/AssertStatementTree.java | 21 / 12 / 9 (42.857%) | 21 / 19 / 2 (9.524%) | 21 / 19 / 2 (9.524%) | 21 / 19 / 2 (9.524%) |
| java-frontend/src/main/java/org/sonar/plugins/java/api/tree/AssignmentExpressionTree.java | 15 / 10 / 5 (33.333%) | 15 / 15 / 0 (0.000%) | 15 / 15 / 0 (0.000%) | 15 / 15 / 0 (0.000%) |
| java-frontend/src/main/java/org/sonar/plugins/java/api/tree/BaseTreeVisitor.java | 747 / 518 / 229 (30.656%) | 747 / 745 / 2 (0.268%) | 747 / 624 / 123 (16.466%) | 747 / 745 / 2 (0.268%) |
| java-frontend/src/main/java/org/sonar/plugins/java/api/tree/BinaryExpressionTree.java | 15 / 10 / 5 (33.333%) | 15 / 15 / 0 (0.000%) | 15 / 15 / 0 (0.000%) | 15 / 15 / 0 (0.000%) |
| java-frontend/src/main/java/org/sonar/plugins/java/api/tree/BlockTree.java | 16 / 10 / 6 (37.500%) | 16 / 16 / 0 (0.000%) | 16 / 16 / 0 (0.000%) | 16 / 16 / 0 (0.000%) |
| java-frontend/src/main/java/org/sonar/plugins/java/api/tree/BreakStatementTree.java | 16 / 10 / 6 (37.500%) | 16 / 15 / 1 (6.250%) | 16 / 15 / 1 (6.250%) | 16 / 15 / 1 (6.250%) |
| java-frontend/src/main/java/org/sonar/plugins/java/api/tree/CaseGroupTree.java | 15 / 9 / 6 (40.000%) | 15 / 15 / 0 (0.000%) | 15 / 14 / 1 (6.667%) | 15 / 15 / 0 (0.000%) |
| java-frontend/src/main/java/org/sonar/plugins/java/api/tree/CaseLabelTree.java | 17 / 11 / 6 (35.294%) | 17 / 17 / 0 (0.000%) | 17 / 16 / 1 (5.882%) | 17 / 17 / 0 (0.000%) |
| java-frontend/src/main/java/org/sonar/plugins/java/api/tree/CatchTree.java | 19 / 12 / 7 (36.842%) | 19 / 19 / 0 (0.000%) | 19 / 17 / 2 (10.526%) | 19 / 19 / 0 (0.000%) |
| java-frontend/src/main/java/org/sonar/plugins/java/api/tree/ClassTree.java | 50 / 22 / 28 (56.000%) | 50 / 44 / 6 (12.000%) | 50 / 37 / 13 (26.000%) | 50 / 44 / 6 (12.000%) |
| java-frontend/src/main/java/org/sonar/plugins/java/api/tree/CompilationUnitTree.java | 23 / 12 / 11 (47.826%) | 23 / 21 / 2 (8.696%) | 23 / 17 / 6 (26.087%) | 23 / 21 / 2 (8.696%) |
| java-frontend/src/main/java/org/sonar/plugins/java/api/tree/ConditionalExpressionTree.java | 19 / 12 / 7 (36.842%) | 19 / 19 / 0 (0.000%) | 19 / 19 / 0 (0.000%) | 19 / 19 / 0 (0.000%) |
| java-frontend/src/main/java/org/sonar/plugins/java/api/tree/ContinueStatementTree.java | 16 / 10 / 6 (37.500%) | 16 / 15 / 1 (6.250%) | 16 / 15 / 1 (6.250%) | 16 / 15 / 1 (6.250%) |
| java-frontend/src/main/java/org/sonar/plugins/java/api/tree/DefaultPatternTree.java | 11 / 8 / 3 (27.273%) | 11 / 11 / 0 (0.000%) | 11 / 10 / 1 (9.091%) | 11 / 11 / 0 (0.000%) |
| java-frontend/src/main/java/org/sonar/plugins/java/api/tree/DoWhileStatementTree.java | 23 / 14 / 9 (39.130%) | 23 / 23 / 0 (0.000%) | 23 / 23 / 0 (0.000%) | 23 / 23 / 0 (0.000%) |
| java-frontend/src/main/java/org/sonar/plugins/java/api/tree/EmptyStatementTree.java | 12 / 8 / 4 (33.333%) | 12 / 12 / 0 (0.000%) | 12 / 12 / 0 (0.000%) | 12 / 12 / 0 (0.000%) |
| java-frontend/src/main/java/org/sonar/plugins/java/api/tree/EnumConstantTree.java | 18 / 11 / 7 (38.889%) | 18 / 17 / 1 (5.556%) | 18 / 15 / 3 (16.667%) | 18 / 17 / 1 (5.556%) |
| java-frontend/src/main/java/org/sonar/plugins/java/api/tree/ExportsDirectiveTree.java | 17 / 10 / 7 (41.176%) | 17 / 16 / 1 (5.882%) | 17 / 16 / 1 (5.882%) | 17 / 16 / 1 (5.882%) |
| java-frontend/src/main/java/org/sonar/plugins/java/api/tree/ExpressionStatementTree.java | 13 / 9 / 4 (30.769%) | 13 / 13 / 0 (0.000%) | 13 / 13 / 0 (0.000%) | 13 / 13 / 0 (0.000%) |
| java-frontend/src/main/java/org/sonar/plugins/java/api/tree/ExpressionTree.java | 21 / 18 / 3 (14.286%) | 21 / 21 / 0 (0.000%) | 21 / 19 / 2 (9.524%) | 21 / 21 / 0 (0.000%) |
| java-frontend/src/main/java/org/sonar/plugins/java/api/tree/ForEachStatement.java | 23 / 14 / 9 (39.130%) | 23 / 23 / 0 (0.000%) | 23 / 22 / 1 (4.348%) | 23 / 23 / 0 (0.000%) |
| java-frontend/src/main/java/org/sonar/plugins/java/api/tree/ForStatementTree.java | 30 / 16 / 14 (46.667%) | 30 / 29 / 1 (3.333%) | 30 / 29 / 1 (3.333%) | 30 / 29 / 1 (3.333%) |
| java-frontend/src/main/java/org/sonar/plugins/java/api/tree/GuardedPatternTree.java | 15 / 10 / 5 (33.333%) | 15 / 15 / 0 (0.000%) | 15 / 14 / 1 (6.667%) | 15 / 15 / 0 (0.000%) |
| java-frontend/src/main/java/org/sonar/plugins/java/api/tree/IdentifierTree.java | 17 / 12 / 5 (29.412%) | 17 / 17 / 0 (0.000%) | 17 / 16 / 1 (5.882%) | 17 / 17 / 0 (0.000%) |
| java-frontend/src/main/java/org/sonar/plugins/java/api/tree/IfStatementTree.java | 25 / 14 / 11 (44.000%) | 25 / 23 / 2 (8.000%) | 25 / 23 / 2 (8.000%) | 25 / 23 / 2 (8.000%) |
| java-frontend/src/main/java/org/sonar/plugins/java/api/tree/ImportClauseTree.java | 9 / 7 / 2 (22.222%) | 9 / 9 / 0 (0.000%) | 9 / 8 / 1 (11.111%) | 9 / 9 / 0 (0.000%) |
| java-frontend/src/main/java/org/sonar/plugins/java/api/tree/ImportTree.java | 26 / 15 / 11 (42.308%) | 26 / 23 / 3 (11.538%) | 26 / 21 / 5 (19.231%) | 26 / 23 / 3 (11.538%) |
| java-frontend/src/main/java/org/sonar/plugins/java/api/tree/InferedTypeTree.java | 39 / 25 / 14 (35.897%) | 39 / 37 / 2 (5.128%) | 39 / 31 / 8 (20.513%) | 39 / 37 / 2 (5.128%) |
| java-frontend/src/main/java/org/sonar/plugins/java/api/tree/InstanceOfTree.java | 15 / 10 / 5 (33.333%) | 15 / 15 / 0 (0.000%) | 15 / 15 / 0 (0.000%) | 15 / 15 / 0 (0.000%) |
| java-frontend/src/main/java/org/sonar/plugins/java/api/tree/LabeledStatementTree.java | 18 / 11 / 7 (38.889%) | 18 / 18 / 0 (0.000%) | 18 / 16 / 2 (11.111%) | 18 / 18 / 0 (0.000%) |
| java-frontend/src/main/java/org/sonar/plugins/java/api/tree/LambdaExpressionTree.java | 27 / 14 / 13 (48.148%) | 27 / 25 / 2 (7.407%) | 27 / 19 / 8 (29.630%) | 27 / 25 / 2 (7.407%) |
| java-frontend/src/main/java/org/sonar/plugins/java/api/tree/ListTree.java | 15 / 9 / 6 (40.000%) | 15 / 15 / 0 (0.000%) | 15 / 11 / 4 (26.667%) | 15 / 15 / 0 (0.000%) |
| java-frontend/src/main/java/org/sonar/plugins/java/api/tree/LiteralTree.java | 13 / 10 / 3 (23.077%) | 13 / 13 / 0 (0.000%) | 13 / 13 / 0 (0.000%) | 13 / 13 / 0 (0.000%) |
| java-frontend/src/main/java/org/sonar/plugins/java/api/tree/MemberSelectExpressionTree.java | 16 / 10 / 6 (37.500%) | 16 / 16 / 0 (0.000%) | 16 / 16 / 0 (0.000%) | 16 / 16 / 0 (0.000%) |
| java-frontend/src/main/java/org/sonar/plugins/java/api/tree/MethodInvocationTree.java | 24 / 15 / 9 (37.500%) | 24 / 23 / 1 (4.167%) | 24 / 19 / 5 (20.833%) | 24 / 23 / 1 (4.167%) |
| java-frontend/src/main/java/org/sonar/plugins/java/api/tree/MethodReferenceTree.java | 18 / 11 / 7 (38.889%) | 18 / 17 / 1 (5.556%) | 18 / 16 / 2 (11.111%) | 18 / 17 / 1 (5.556%) |
| java-frontend/src/main/java/org/sonar/plugins/java/api/tree/MethodTree.java | 53 / 24 / 29 (54.717%) | 53 / 44 / 9 (16.981%) | 53 / 37 / 16 (30.189%) | 53 / 44 / 9 (16.981%) |
| java-frontend/src/main/java/org/sonar/plugins/java/api/tree/MethodsAreNonnullByDefault.java | 18 / 16 / 2 (11.111%) | 18 / 16 / 2 (11.111%) | 18 / 16 / 2 (11.111%) | 18 / 16 / 2 (11.111%) |
| java-frontend/src/main/java/org/sonar/plugins/java/api/tree/Modifier.java | 38 / 37 / 1 (2.632%) | 38 / 38 / 0 (0.000%) | 38 / 38 / 0 (0.000%) | 38 / 38 / 0 (0.000%) |
| java-frontend/src/main/java/org/sonar/plugins/java/api/tree/ModifierKeywordTree.java | 12 / 9 / 3 (25.000%) | 12 / 12 / 0 (0.000%) | 12 / 12 / 0 (0.000%) | 12 / 12 / 0 (0.000%) |
| java-frontend/src/main/java/org/sonar/plugins/java/api/tree/ModifierTree.java | 8 / 7 / 1 (12.500%) | 8 / 8 / 0 (0.000%) | 8 / 7 / 1 (12.500%) | 8 / 8 / 0 (0.000%) |
| java-frontend/src/main/java/org/sonar/plugins/java/api/tree/ModifiersTree.java | 16 / 9 / 7 (43.750%) | 16 / 16 / 0 (0.000%) | 16 / 16 / 0 (0.000%) | 16 / 16 / 0 (0.000%) |
| java-frontend/src/main/java/org/sonar/plugins/java/api/tree/ModuleDeclarationTree.java | 26 / 14 / 12 (46.154%) | 26 / 25 / 1 (3.846%) | 26 / 24 / 2 (7.692%) | 26 / 25 / 1 (3.846%) |
| java-frontend/src/main/java/org/sonar/plugins/java/api/tree/ModuleDirectiveTree.java | 13 / 9 / 4 (30.769%) | 13 / 13 / 0 (0.000%) | 13 / 12 / 1 (7.692%) | 13 / 13 / 0 (0.000%) |
| java-frontend/src/main/java/org/sonar/plugins/java/api/tree/ModuleNameTree.java | 10 / 7 / 3 (30.000%) | 10 / 10 / 0 (0.000%) | 10 / 10 / 0 (0.000%) | 10 / 10 / 0 (0.000%) |
| java-frontend/src/main/java/org/sonar/plugins/java/api/tree/NewArrayTree.java | 27 / 13 / 14 (51.852%) | 27 / 23 / 4 (14.815%) | 27 / 21 / 6 (22.222%) | 27 / 23 / 4 (14.815%) |
| java-frontend/src/main/java/org/sonar/plugins/java/api/tree/NewClassTree.java | 36 / 19 / 17 (47.222%) | 36 / 31 / 5 (13.889%) | 36 / 26 / 10 (27.778%) | 36 / 31 / 5 (13.889%) |
| java-frontend/src/main/java/org/sonar/plugins/java/api/tree/NullPatternTree.java | 11 / 8 / 3 (27.273%) | 11 / 11 / 0 (0.000%) | 11 / 10 / 1 (9.091%) | 11 / 11 / 0 (0.000%) |
| java-frontend/src/main/java/org/sonar/plugins/java/api/tree/OpensDirectiveTree.java | 17 / 10 / 7 (41.176%) | 17 / 16 / 1 (5.882%) | 17 / 16 / 1 (5.882%) | 17 / 16 / 1 (5.882%) |
| java-frontend/src/main/java/org/sonar/plugins/java/api/tree/PackageDeclarationTree.java | 18 / 11 / 7 (38.889%) | 18 / 18 / 0 (0.000%) | 18 / 17 / 1 (5.556%) | 18 / 18 / 0 (0.000%) |
| java-frontend/src/main/java/org/sonar/plugins/java/api/tree/ParameterizedTypeTree.java | 13 / 9 / 4 (30.769%) | 13 / 13 / 0 (0.000%) | 13 / 13 / 0 (0.000%) | 13 / 13 / 0 (0.000%) |
| java-frontend/src/main/java/org/sonar/plugins/java/api/tree/ParenthesizedTree.java | 15 / 10 / 5 (33.333%) | 15 / 15 / 0 (0.000%) | 15 / 15 / 0 (0.000%) | 15 / 15 / 0 (0.000%) |
| java-frontend/src/main/java/org/sonar/plugins/java/api/tree/PatternInstanceOfTree.java | 21 / 14 / 7 (33.333%) | 21 / 20 / 1 (4.762%) | 21 / 19 / 2 (9.524%) | 21 / 20 / 1 (4.762%) |
| java-frontend/src/main/java/org/sonar/plugins/java/api/tree/PatternTree.java | 9 / 7 / 2 (22.222%) | 9 / 9 / 0 (0.000%) | 9 / 9 / 0 (0.000%) | 9 / 9 / 0 (0.000%) |
| java-frontend/src/main/java/org/sonar/plugins/java/api/tree/PrimitiveTypeTree.java | 12 / 8 / 4 (33.333%) | 12 / 12 / 0 (0.000%) | 12 / 12 / 0 (0.000%) | 12 / 12 / 0 (0.000%) |
| java-frontend/src/main/java/org/sonar/plugins/java/api/tree/ProvidesDirectiveTree.java | 16 / 10 / 6 (37.500%) | 16 / 16 / 0 (0.000%) | 16 / 16 / 0 (0.000%) | 16 / 16 / 0 (0.000%) |
| java-frontend/src/main/java/org/sonar/plugins/java/api/tree/RecordPatternTree.java | 18 / 11 / 7 (38.889%) | 18 / 18 / 0 (0.000%) | 18 / 17 / 1 (5.556%) | 18 / 18 / 0 (0.000%) |
| java-frontend/src/main/java/org/sonar/plugins/java/api/tree/RequiresDirectiveTree.java | 13 / 9 / 4 (30.769%) | 13 / 13 / 0 (0.000%) | 13 / 13 / 0 (0.000%) | 13 / 13 / 0 (0.000%) |
| java-frontend/src/main/java/org/sonar/plugins/java/api/tree/ReturnStatementTree.java | 16 / 10 / 6 (37.500%) | 16 / 15 / 1 (6.250%) | 16 / 15 / 1 (6.250%) | 16 / 15 / 1 (6.250%) |
| java-frontend/src/main/java/org/sonar/plugins/java/api/tree/StatementTree.java | 9 / 7 / 2 (22.222%) | 9 / 9 / 0 (0.000%) | 9 / 8 / 1 (11.111%) | 9 / 9 / 0 (0.000%) |
| java-frontend/src/main/java/org/sonar/plugins/java/api/tree/StaticInitializerTree.java | 11 / 8 / 3 (27.273%) | 11 / 11 / 0 (0.000%) | 11 / 10 / 1 (9.091%) | 11 / 11 / 0 (0.000%) |
| java-frontend/src/main/java/org/sonar/plugins/java/api/tree/SwitchExpressionTree.java | 10 / 7 / 3 (30.000%) | 10 / 10 / 0 (0.000%) | 10 / 10 / 0 (0.000%) | 10 / 10 / 0 (0.000%) |
| java-frontend/src/main/java/org/sonar/plugins/java/api/tree/SwitchStatementTree.java | 10 / 7 / 3 (30.000%) | 10 / 10 / 0 (0.000%) | 10 / 10 / 0 (0.000%) | 10 / 10 / 0 (0.000%) |
| java-frontend/src/main/java/org/sonar/plugins/java/api/tree/SwitchTree.java | 23 / 14 / 9 (39.130%) | 23 / 23 / 0 (0.000%) | 23 / 22 / 1 (4.348%) | 23 / 23 / 0 (0.000%) |
| java-frontend/src/main/java/org/sonar/plugins/java/api/tree/SynchronizedStatementTree.java | 19 / 12 / 7 (36.842%) | 19 / 19 / 0 (0.000%) | 19 / 19 / 0 (0.000%) | 19 / 19 / 0 (0.000%) |
| java-frontend/src/main/java/org/sonar/plugins/java/api/tree/SyntaxToken.java | 24 / 19 / 5 (20.833%) | 24 / 24 / 0 (0.000%) | 24 / 23 / 1 (4.167%) | 24 / 24 / 0 (0.000%) |
| java-frontend/src/main/java/org/sonar/plugins/java/api/tree/SyntaxTrivia.java | 40 / 37 / 3 (7.500%) | 40 / 40 / 0 (0.000%) | 40 / 39 / 1 (2.500%) | 40 / 40 / 0 (0.000%) |
| java-frontend/src/main/java/org/sonar/plugins/java/api/tree/ThrowStatementTree.java | 15 / 10 / 5 (33.333%) | 15 / 15 / 0 (0.000%) | 15 / 15 / 0 (0.000%) | 15 / 15 / 0 (0.000%) |
| java-frontend/src/main/java/org/sonar/plugins/java/api/tree/Tree.java | 456 / 320 / 136 (29.825%) | 456 / 452 / 4 (0.877%) | 456 / 413 / 43 (9.430%) | 456 / 452 / 4 (0.877%) |
| java-frontend/src/main/java/org/sonar/plugins/java/api/tree/TreeVisitor.java | 218 / 147 / 71 (32.569%) | 218 / 218 / 0 (0.000%) | 218 / 188 / 30 (13.761%) | 218 / 218 / 0 (0.000%) |
| java-frontend/src/main/java/org/sonar/plugins/java/api/tree/TryStatementTree.java | 31 / 15 / 16 (51.613%) | 31 / 27 / 4 (12.903%) | 31 / 25 / 6 (19.355%) | 31 / 27 / 4 (12.903%) |
| java-frontend/src/main/java/org/sonar/plugins/java/api/tree/TypeArguments.java | 13 / 9 / 4 (30.769%) | 13 / 13 / 0 (0.000%) | 13 / 13 / 0 (0.000%) | 13 / 13 / 0 (0.000%) |
| java-frontend/src/main/java/org/sonar/plugins/java/api/tree/TypeCastTree.java | 23 / 13 / 10 (43.478%) | 23 / 22 / 1 (4.348%) | 23 / 22 / 1 (4.348%) | 23 / 22 / 1 (4.348%) |
| java-frontend/src/main/java/org/sonar/plugins/java/api/tree/TypeParameterTree.java | 19 / 11 / 8 (42.105%) | 19 / 18 / 1 (5.263%) | 19 / 16 / 3 (15.789%) | 19 / 18 / 1 (5.263%) |
| java-frontend/src/main/java/org/sonar/plugins/java/api/tree/TypeParameters.java | 15 / 9 / 6 (40.000%) | 15 / 13 / 2 (13.333%) | 15 / 11 / 4 (26.667%) | 15 / 13 / 2 (13.333%) |
| java-frontend/src/main/java/org/sonar/plugins/java/api/tree/TypePatternTree.java | 11 / 8 / 3 (27.273%) | 11 / 11 / 0 (0.000%) | 11 / 9 / 2 (18.182%) | 11 / 11 / 0 (0.000%) |
| java-frontend/src/main/java/org/sonar/plugins/java/api/tree/TypeTree.java | 13 / 9 / 4 (30.769%) | 13 / 13 / 0 (0.000%) | 13 / 11 / 2 (15.385%) | 13 / 13 / 0 (0.000%) |
| java-frontend/src/main/java/org/sonar/plugins/java/api/tree/UnaryExpressionTree.java | 13 / 9 / 4 (30.769%) | 13 / 13 / 0 (0.000%) | 13 / 13 / 0 (0.000%) | 13 / 13 / 0 (0.000%) |
| java-frontend/src/main/java/org/sonar/plugins/java/api/tree/UnionTypeTree.java | 12 / 8 / 4 (33.333%) | 12 / 12 / 0 (0.000%) | 12 / 12 / 0 (0.000%) | 12 / 12 / 0 (0.000%) |
| java-frontend/src/main/java/org/sonar/plugins/java/api/tree/UsesDirectiveTree.java | 11 / 8 / 3 (27.273%) | 11 / 11 / 0 (0.000%) | 11 / 11 / 0 (0.000%) | 11 / 11 / 0 (0.000%) |
| java-frontend/src/main/java/org/sonar/plugins/java/api/tree/VarTypeTree.java | 11 / 8 / 3 (27.273%) | 11 / 11 / 0 (0.000%) | 11 / 11 / 0 (0.000%) | 11 / 11 / 0 (0.000%) |
| java-frontend/src/main/java/org/sonar/plugins/java/api/tree/VariableTree.java | 26 / 14 / 12 (46.154%) | 26 / 23 / 3 (11.538%) | 26 / 22 / 4 (15.385%) | 26 / 23 / 3 (11.538%) |
| java-frontend/src/main/java/org/sonar/plugins/java/api/tree/WhileStatementTree.java | 19 / 12 / 7 (36.842%) | 19 / 19 / 0 (0.000%) | 19 / 19 / 0 (0.000%) | 19 / 19 / 0 (0.000%) |
| java-frontend/src/main/java/org/sonar/plugins/java/api/tree/WildcardTree.java | 21 / 12 / 9 (42.857%) | 21 / 19 / 2 (9.524%) | 21 / 19 / 2 (9.524%) | 21 / 19 / 2 (9.524%) |
| java-frontend/src/main/java/org/sonar/plugins/java/api/tree/YieldStatementTree.java | 16 / 10 / 6 (37.500%) | 16 / 15 / 1 (6.250%) | 16 / 15 / 1 (6.250%) | 16 / 15 / 1 (6.250%) |
| java-frontend/src/main/java/org/sonar/plugins/java/api/tree/package-info.java | 10 / 8 / 2 (20.000%) | 10 / 9 / 1 (10.000%) | 10 / 8 / 2 (20.000%) | 10 / 9 / 1 (10.000%) |
| java-jsp/src/main/java/org/sonar/java/jsp/Jasper.java | 492 / 364 / 128 (26.016%) | 492 / 382 / 110 (22.358%) | 492 / 364 / 128 (26.016%) | 492 / 382 / 110 (22.358%) |
| java-jsp/src/main/java/org/sonar/java/jsp/JasperOptions.java | 162 / 136 / 26 (16.049%) | 162 / 137 / 25 (15.432%) | 162 / 136 / 26 (16.049%) | 162 / 137 / 25 (15.432%) |
| java-jsp/src/main/java/org/sonar/java/jsp/package-info.java | 5 / 4 / 1 (20.000%) | 5 / 4 / 1 (20.000%) | 5 / 4 / 1 (20.000%) | 5 / 4 / 1 (20.000%) |
| java-surefire/src/main/java/org/sonar/plugins/surefire/StaxParser.java | 73 / 49 / 24 (32.877%) | 73 / 53 / 20 (27.397%) | 73 / 50 / 23 (31.507%) | 73 / 53 / 20 (27.397%) |
| java-surefire/src/main/java/org/sonar/plugins/surefire/SurefireExtensions.java | 31 / 13 / 18 (58.065%) | 31 / 19 / 12 (38.710%) | 31 / 13 / 18 (58.065%) | 31 / 19 / 12 (38.710%) |
| java-surefire/src/main/java/org/sonar/plugins/surefire/SurefireJavaParser.java | 330 / 214 / 116 (35.152%) | 330 / 273 / 57 (17.273%) | 330 / 222 / 108 (32.727%) | 330 / 273 / 57 (17.273%) |
| java-surefire/src/main/java/org/sonar/plugins/surefire/SurefireSensor.java | 84 / 60 / 24 (28.571%) | 84 / 67 / 17 (20.238%) | 84 / 60 / 24 (28.571%) | 84 / 67 / 17 (20.238%) |
| java-surefire/src/main/java/org/sonar/plugins/surefire/api/SurefireUtils.java | 89 / 60 / 29 (32.584%) | 89 / 60 / 29 (32.584%) | 89 / 60 / 29 (32.584%) | 89 / 60 / 29 (32.584%) |
| java-surefire/src/main/java/org/sonar/plugins/surefire/api/package-info.java | 6 / 5 / 1 (16.667%) | 6 / 5 / 1 (16.667%) | 6 / 5 / 1 (16.667%) | 6 / 5 / 1 (16.667%) |
| java-surefire/src/main/java/org/sonar/plugins/surefire/data/SurefireStaxHandler.java | 253 / 180 / 73 (28.854%) | 253 / 204 / 49 (19.368%) | 253 / 201 / 52 (20.553%) | 253 / 204 / 49 (19.368%) |
| java-surefire/src/main/java/org/sonar/plugins/surefire/data/UnitTestClassReport.java | 100 / 68 / 32 (32.000%) | 100 / 97 / 3 (3.000%) | 100 / 97 / 3 (3.000%) | 100 / 97 / 3 (3.000%) |
| java-surefire/src/main/java/org/sonar/plugins/surefire/data/UnitTestIndex.java | 73 / 53 / 20 (27.397%) | 73 / 73 / 0 (0.000%) | 73 / 53 / 20 (27.397%) | 73 / 73 / 0 (0.000%) |
| java-surefire/src/main/java/org/sonar/plugins/surefire/data/UnitTestResult.java | 105 / 105 / 0 (0.000%) | 105 / 105 / 0 (0.000%) | 105 / 105 / 0 (0.000%) | 105 / 105 / 0 (0.000%) |
| java-surefire/src/main/java/org/sonar/plugins/surefire/data/package-info.java | 6 / 5 / 1 (16.667%) | 6 / 5 / 1 (16.667%) | 6 / 5 / 1 (16.667%) | 6 / 5 / 1 (16.667%) |
| java-surefire/src/main/java/org/sonar/plugins/surefire/package-info.java | 5 / 4 / 1 (20.000%) | 5 / 4 / 1 (20.000%) | 5 / 4 / 1 (20.000%) | 5 / 4 / 1 (20.000%) |
| sonar-java-plugin/src/main/java/org/sonar/plugins/java/BuiltInJavaQualityProfile.java | 235 / 145 / 90 (38.298%) | 235 / 167 / 68 (28.936%) | 235 / 145 / 90 (38.298%) | 235 / 167 / 68 (28.936%) |
| sonar-java-plugin/src/main/java/org/sonar/plugins/java/DroppedPropertiesSensor.java | 105 / 79 / 26 (24.762%) | 105 / 79 / 26 (24.762%) | 105 / 79 / 26 (24.762%) | 105 / 79 / 26 (24.762%) |
| sonar-java-plugin/src/main/java/org/sonar/plugins/java/ExternalReportExtensions.java | 129 / 49 / 80 (62.016%) | 129 / 78 / 51 (39.535%) | 129 / 49 / 80 (62.016%) | 129 / 78 / 51 (39.535%) |
| sonar-java-plugin/src/main/java/org/sonar/plugins/java/Java.java | 49 / 37 / 12 (24.490%) | 49 / 37 / 12 (24.490%) | 49 / 37 / 12 (24.490%) | 49 / 37 / 12 (24.490%) |
| sonar-java-plugin/src/main/java/org/sonar/plugins/java/JavaAgenticAIProfile.java | 30 / 24 / 6 (20.000%) | 30 / 28 / 2 (6.667%) | 30 / 24 / 6 (20.000%) | 30 / 28 / 2 (6.667%) |
| sonar-java-plugin/src/main/java/org/sonar/plugins/java/JavaPlugin.java | 159 / 59 / 100 (62.893%) | 159 / 100 / 59 (37.107%) | 159 / 59 / 100 (62.893%) | 159 / 100 / 59 (37.107%) |
| sonar-java-plugin/src/main/java/org/sonar/plugins/java/JavaRulesDefinition.java | 135 / 73 / 62 (45.926%) | 135 / 83 / 52 (38.519%) | 135 / 73 / 62 (45.926%) | 135 / 83 / 52 (38.519%) |
| sonar-java-plugin/src/main/java/org/sonar/plugins/java/JavaSensor.java | 367 / 172 / 195 (53.134%) | 367 / 247 / 120 (32.698%) | 367 / 172 / 195 (53.134%) | 367 / 247 / 120 (32.698%) |
| sonar-java-plugin/src/main/java/org/sonar/plugins/java/JavaSonarWayProfile.java | 35 / 26 / 9 (25.714%) | 35 / 31 / 4 (11.429%) | 35 / 26 / 9 (25.714%) | 35 / 31 / 4 (11.429%) |
| sonar-java-plugin/src/main/java/org/sonar/plugins/java/ProjectEndOfAnalysisSensor.java | 128 / 48 / 80 (62.500%) | 128 / 98 / 30 (23.438%) | 128 / 48 / 80 (62.500%) | 128 / 98 / 30 (23.438%) |
| sonar-java-plugin/src/main/java/org/sonar/plugins/java/SpringContextModelSensor.java | 127 / 64 / 63 (49.606%) | 127 / 95 / 32 (25.197%) | 127 / 64 / 63 (49.606%) | 127 / 95 / 32 (25.197%) |
| sonar-java-plugin/src/main/java/org/sonar/plugins/java/package-info.java | 5 / 4 / 1 (20.000%) | 5 / 4 / 1 (20.000%) | 5 / 4 / 1 (20.000%) | 5 / 4 / 1 (20.000%) |

## Rules with findings

| Rule | Baseline | Source paths | Bytecode | Combined |
|---|---:|---:|---:|---:|
| java:S1066 | 1 | 1 | 1 | 1 |
| java:S107 | 1 | 7 | 1 | 7 |
| java:S1075 | 3 | 3 | 3 | 3 |
| java:S110 | 0 | 5 | 0 | 5 |
| java:S112 | 1 | 1 | 1 | 1 |
| java:S1121 | 2 | 2 | 2 | 2 |
| java:S1133 | 10 | 10 | 10 | 10 |
| java:S1134 | 5 | 5 | 5 | 5 |
| java:S1135 | 12 | 12 | 12 | 12 |
| java:S1141 | 1 | 1 | 1 | 1 |
| java:S1144 | 1 | 0 | 0 | 0 |
| java:S1185 | 0 | 1 | 1 | 1 |
| java:S1192 | 6 | 6 | 6 | 6 |
| java:S125 | 10 | 10 | 10 | 10 |
| java:S131 | 0 | 3 | 0 | 3 |
| java:S135 | 2 | 2 | 2 | 2 |
| java:S1450 | 1 | 0 | 1 | 0 |
| java:S1452 | 6 | 6 | 6 | 6 |
| java:S1845 | 0 | 1 | 0 | 1 |
| java:S2097 | 1 | 1 | 1 | 1 |
| java:S2160 | 2 | 2 | 2 | 2 |
| java:S2234 | 0 | 1 | 0 | 1 |
| java:S3398 | 1 | 0 | 1 | 0 |
| java:S3457 | 1 | 2 | 1 | 2 |
| java:S3776 | 2 | 2 | 2 | 2 |
| java:S4790 | 2 | 2 | 2 | 2 |
| java:S5411 | 2 | 2 | 2 | 2 |
| java:S5803 | 0 | 1 | 0 | 1 |
| java:S6204 | 0 | 1 | 0 | 1 |
| java:S6485 | 0 | 1 | 0 | 1 |
| java:S6539 | 0 | 2 | 2 | 2 |
| java:S6878 | 1 | 1 | 1 | 1 |
| java:S6880 | 5 | 5 | 5 | 5 |
| java:S7158 | 1 | 1 | 1 | 1 |
| java:S8491 | 2 | 2 | 2 | 2 |
| java:S9358 | 2 | 0 | 0 | 0 |
| java:S9395 | 2 | 2 | 2 | 2 |
| java:S9398 | 3 | 3 | 3 | 3 |

561 configured rules had no findings in any mode and are omitted.

## Detailed differences

<details>
<summary>Source paths versus Baseline</summary>

### Largest semantic improvements

Top five by absolute change in unknown percentage.

| File | Second mode unknown % | First mode unknown % | Change (pp) |
|---|---:|---:|---:|
| java-checks/src/main/java/org/sonar/java/checks/IgnoredOperationStatusCheck.java | 78.146% | 1.325% | -76.821 |
| java-checks/src/main/java/org/sonar/java/checks/spring/AsyncMethodsOnConfigurationClassCheck.java | 76.136% | 2.273% | -73.864 |
| java-checks/src/main/java/org/sonar/java/checks/PublicConstructorInAbstractClassCheck.java | 75.758% | 2.020% | -73.737 |
| java-checks/src/main/java/org/sonar/java/checks/AvoidHighFrameratesOnMobileCheck.java | 73.333% | 2.667% | -70.667 |
| java-checks/src/main/java/org/sonar/java/checks/security/IntegerToHexStringCheck.java | 72.381% | 1.905% | -70.476 |

### Largest semantic regressions

Top five by absolute change in unknown percentage.

None.

| Unknown occurrences | Count |
|---|---:|
| Still unknown in both modes | 10458 |
| No longer reported unknown | 82565 |
| Newly reported unknown | 4 |

### No longer reported unknown

| File | Range | Identifier | AST context |
|---|---|---|---|
| external-reports/src/main/java/org/sonar/java/externalreport/CheckstyleSensor.java | (70:35)-(70:48) | importIfExist | METHOD_INVOCATION |
| external-reports/src/main/java/org/sonar/java/externalreport/CheckstyleSensor.java | (76:33)-(76:37) | read | MEMBER_SELECT |
| external-reports/src/main/java/org/sonar/java/externalreport/CheckstyleSensor.java | (76:7)-(76:32) | CheckstyleXmlReportReader | MEMBER_SELECT |
| external-reports/src/main/java/org/sonar/java/externalreport/CheckstyleSensor.java | (83:24)-(83:33) | saveIssue | MEMBER_SELECT |
| external-reports/src/main/java/org/sonar/java/externalreport/CheckstyleSensor.java | (83:5)-(83:23) | ExternalIssueUtils | MEMBER_SELECT |
| external-reports/src/main/java/org/sonar/java/externalreport/PmdSensor.java | (68:35)-(68:48) | importIfExist | METHOD_INVOCATION |
| external-reports/src/main/java/org/sonar/java/externalreport/PmdSensor.java | (74:26)-(74:30) | read | MEMBER_SELECT |
| external-reports/src/main/java/org/sonar/java/externalreport/PmdSensor.java | (74:7)-(74:25) | PmdXmlReportReader | MEMBER_SELECT |
| external-reports/src/main/java/org/sonar/java/externalreport/PmdXmlReportReader.java | (110:19)-(110:28) | PmdSensor | MEMBER_SELECT |
| external-reports/src/main/java/org/sonar/java/externalreport/PmdXmlReportReader.java | (110:29)-(110:39) | LINTER_KEY | MEMBER_SELECT |
| external-reports/src/main/java/org/sonar/java/externalreport/SpotBugsSensor.java | (102:35)-(102:48) | importIfExist | METHOD_INVOCATION |
| external-reports/src/main/java/org/sonar/java/externalreport/SpotBugsSensor.java | (112:31)-(112:35) | read | MEMBER_SELECT |
| external-reports/src/main/java/org/sonar/java/externalreport/SpotBugsSensor.java | (112:7)-(112:30) | SpotBugsXmlReportReader | MEMBER_SELECT |
| external-reports/src/main/java/org/sonar/java/externalreport/SpotBugsXmlReportReader.java | (164:23)-(164:37) | SpotBugsSensor | MEMBER_SELECT |
| external-reports/src/main/java/org/sonar/java/externalreport/SpotBugsXmlReportReader.java | (164:38)-(164:50) | SPOTBUGS_KEY | MEMBER_SELECT |
| external-reports/src/main/java/org/sonar/java/externalreport/SpotBugsXmlReportReader.java | (172:24)-(172:33) | saveIssue | MEMBER_SELECT |
| external-reports/src/main/java/org/sonar/java/externalreport/SpotBugsXmlReportReader.java | (172:5)-(172:23) | ExternalIssueUtils | MEMBER_SELECT |
| java-checks-aws/src/main/java/org/sonar/java/checks/aws/AbstractAwsMethodVisitor.java | (29:56)-(29:83) | IssuableSubscriptionVisitor | CLASS |
| java-checks-aws/src/main/java/org/sonar/java/checks/aws/AbstractAwsMethodVisitor.java | (30:24)-(30:28) | List | PARAMETERIZED_TYPE |
| java-checks-aws/src/main/java/org/sonar/java/checks/aws/AbstractAwsMethodVisitor.java | (30:29)-(30:33) | Tree | MEMBER_SELECT |

82545 additional occurrences omitted.


### Newly reported unknown

| File | Range | Identifier | AST context |
|---|---|---|---|
| java-checks/src/main/java/org/sonar/java/checks/regex/AbstractRegexCheckTrackingMatchers.java | (193:22)-(193:30) | getRegex | METHOD_REFERENCE |
| java-checks/src/main/java/org/sonar/java/checks/regex/AbstractRegexCheckTrackingMatchers.java | (219:18)-(219:26) | getRegex | METHOD_REFERENCE |
| java-frontend/src/main/java/org/sonar/java/model/JSymbolMetadata.java | (110:16)-(110:31) | JSymbolMetadata | NEW_CLASS |
| java-frontend/src/main/java/org/sonar/java/model/JSymbolMetadata.java | (118:16)-(118:31) | JSymbolMetadata | NEW_CLASS |


Semantic graph location differences unavailable: traversal is DISABLED, PARTIAL, or UNKNOWN in at least one compared mode. Primary identifier and finding comparisons remain valid.

### Findings only in Baseline

| Rule | File | Line | Message |
|---|---|---:|---|
| java:S1144 | java-frontend/src/main/java/org/sonar/java/model/JParserConfig.java | 227 | Remove this unused private "FileByFile" constructor. |
| java:S1450 | java-checks/src/main/java/org/sonar/java/checks/TypeParametersShadowingCheck.java | 37 | Remove the "context" field and declare it as a local variable in the relevant methods. |
| java:S3398 | java-frontend/src/main/java/org/sonar/java/SemanticReportScanner.java | 338 | Move this method into "ModuleReferences". |
| java:S9358 | java-frontend/src/main/java/org/sonar/java/model/InternalSyntaxToken.java | 45 | Move the conditional expression inside this operation. |
| java:S9358 | java-frontend/src/main/java/org/sonar/java/model/InternalSyntaxTrivia.java | 41 | Move the conditional expression inside this operation. |

### Findings only in Source paths

| Rule | File | Line | Message |
|---|---|---:|---|
| java:S107 | java-frontend/src/main/java/org/sonar/java/SonarComponents.java | 169 | Constructor has 8 parameters, which is greater than 7 authorized. |
| java:S107 | java-frontend/src/main/java/org/sonar/java/SonarComponents.java | 188 | Constructor has 8 parameters, which is greater than 7 authorized. |
| java:S107 | java-frontend/src/main/java/org/sonar/java/SonarComponents.java | 213 | Constructor has 9 parameters, which is greater than 7 authorized. |
| java:S107 | java-frontend/src/main/java/org/sonar/java/model/DefaultJavaFileScannerContext.java | 62 | Constructor has 8 parameters, which is greater than 7 authorized. |
| java:S107 | java-frontend/src/main/java/org/sonar/java/model/statement/ForStatementTreeImpl.java | 47 | Constructor has 9 parameters, which is greater than 7 authorized. |
| java:S107 | java-frontend/src/main/java/org/sonar/java/model/statement/TryStatementTreeImpl.java | 51 | Constructor has 8 parameters, which is greater than 7 authorized. |
| java:S110 | java-checks/src/main/java/org/sonar/java/checks/regex/AbstractRedosCheck.java | 51 | This class has 6 parents which is greater than 5 authorized. |
| java:S110 | java-checks/src/main/java/org/sonar/java/checks/regex/RedosCheck.java | 23 | This class has 7 parents which is greater than 5 authorized. |
| java:S110 | java-checks/src/main/java/org/sonar/java/checks/regex/RegexLookaheadCheck.java | 26 | This class has 6 parents which is greater than 5 authorized. |
| java:S110 | java-checks/src/main/java/org/sonar/java/checks/regex/ReluctantQuantifierWithEmptyContinuationCheck.java | 26 | This class has 6 parents which is greater than 5 authorized. |
| java:S110 | java-checks/src/main/java/org/sonar/java/checks/regex/SuperLinearRegexCheck.java | 23 | This class has 7 parents which is greater than 5 authorized. |
| java:S1185 | java-frontend/src/main/java/org/sonar/plugins/java/api/IssuableSubscriptionVisitor.java | 36 | Remove this method to simply inherit it. |
| java:S131 | java-checks/src/main/java/org/sonar/java/checks/BlockingOperationsInVirtualThreadsCheck.java | 96 | Complete cases by adding the missing enum constants or add a default case to this switch. |
| java:S131 | java-checks/src/main/java/org/sonar/java/checks/ThreadAsRunnableArgumentCheck.java | 55 | Complete cases by adding the missing enum constants or add a default case to this switch. |
| java:S131 | java-checks/src/main/java/org/sonar/java/checks/VolatileVariablesOperationsCheck.java | 125 | Complete cases by adding the missing enum constants or add a default case to this switch. |
| java:S1845 | java-checks-testkit/src/main/java/org/sonar/java/checks/verifier/TestCheckRegistrarContext.java | 39 | Rename field "testCheckClasses" to prevent any misunderstanding/clash with method "testCheckClasses" defined in superclass "org.sonar.plugins.java.api.CheckRegistrar$RegistrarContext". |
| java:S2234 | java-checks/src/main/java/org/sonar/java/checks/MembersDifferOnlyByCapitalizationCheck.java | 184 | Parameters to variableAndMethod have the same names but not the same order as the method arguments. |
| java:S3457 | java-checks/src/main/java/org/sonar/java/checks/OneDeclarationPerLineCheck.java | 123 | %n should be used in place of \n to produce the platform-specific line separator. |
| java:S5803 | java-checks-testkit/src/main/java/org/sonar/java/checks/verifier/internal/JavaCheckVerifier.java | 121 | Remove this usage of "scanForTesting", it is annotated with @VisibleForTesting and should not be accessed from production code. |
| java:S6204 | java-checks/src/main/java/org/sonar/java/checks/spring/SpringCacheableWithCachePutCheck.java | 89 | Replace this usage of 'Stream.collect(Collectors.toList())' with 'Stream.toList()' and ensure that the list is unmodified. |
| java:S6485 | java-checks/src/main/java/org/sonar/java/checks/unused/utils/AnnotationFieldReferenceFinder.java | 75 | Replace this call to the constructor with the better suited static method HashMap.newHashMap(int numMappings) |
| java:S6539 | java-frontend/src/main/java/org/sonar/java/model/JParser.java | 256 | Split this “Monster Class” into smaller and more specialized ones to reduce its dependencies on other classes from 59 to the maximum authorized 20 or less. |
| java:S6539 | java-frontend/src/main/java/org/sonar/plugins/java/api/tree/BaseTreeVisitor.java | 27 | Split this “Monster Class” into smaller and more specialized ones to reduce its dependencies on other classes from 73 to the maximum authorized 20 or less. |

</details>

<details>
<summary>Bytecode versus Baseline</summary>

### Largest semantic improvements

Top five by absolute change in unknown percentage.

| File | Second mode unknown % | First mode unknown % | Change (pp) |
|---|---:|---:|---:|
| java-frontend/src/main/java/org/sonar/java/matcher/MethodMatchersList.java | 56.944% | 6.944% | -50.000 |
| java-frontend/src/main/java/org/sonar/plugins/java/api/tree/ModifiersTree.java | 43.750% | 0.000% | -43.750 |
| java-frontend/src/main/java/org/sonar/plugins/java/api/tree/ForStatementTree.java | 46.667% | 3.333% | -43.333 |
| java-frontend/src/main/java/org/sonar/plugins/java/api/tree/DoWhileStatementTree.java | 39.130% | 0.000% | -39.130 |
| java-frontend/src/main/java/org/sonar/plugins/java/api/tree/TypeCastTree.java | 43.478% | 4.348% | -39.130 |

### Largest semantic regressions

Top five by absolute change in unknown percentage.

None.

| Unknown occurrences | Count |
|---|---:|
| Still unknown in both modes | 88274 |
| No longer reported unknown | 4749 |
| Newly reported unknown | 58 |

### No longer reported unknown

| File | Range | Identifier | AST context |
|---|---|---|---|
| java-checks-testkit/src/main/java/org/sonar/java/checks/verifier/internal/InternalConfiguration.java | (25:43)-(25:65) | InternalMockedSonarAPI | CLASS |
| java-checks-testkit/src/main/java/org/sonar/java/checks/verifier/internal/InternalConfiguration.java | (44:11)-(44:32) | notSupportedException | METHOD_INVOCATION |
| java-checks-testkit/src/main/java/org/sonar/java/checks/verifier/internal/InternalFileSystem.java | (31:40)-(31:62) | InternalMockedSonarAPI | CLASS |
| java-checks-testkit/src/main/java/org/sonar/java/checks/verifier/internal/InternalFileSystem.java | (62:11)-(62:32) | notSupportedException | METHOD_INVOCATION |
| java-checks-testkit/src/main/java/org/sonar/java/checks/verifier/internal/InternalFileSystem.java | (67:11)-(67:32) | notSupportedException | METHOD_INVOCATION |
| java-checks-testkit/src/main/java/org/sonar/java/checks/verifier/internal/InternalFileSystem.java | (72:11)-(72:32) | notSupportedException | METHOD_INVOCATION |
| java-checks-testkit/src/main/java/org/sonar/java/checks/verifier/internal/InternalFileSystem.java | (77:11)-(77:32) | notSupportedException | METHOD_INVOCATION |
| java-checks-testkit/src/main/java/org/sonar/java/checks/verifier/internal/InternalFileSystem.java | (82:11)-(82:32) | notSupportedException | METHOD_INVOCATION |
| java-checks-testkit/src/main/java/org/sonar/java/checks/verifier/internal/InternalFileSystem.java | (87:11)-(87:32) | notSupportedException | METHOD_INVOCATION |
| java-checks-testkit/src/main/java/org/sonar/java/checks/verifier/internal/InternalInputFile.java | (186:11)-(186:32) | notSupportedException | METHOD_INVOCATION |
| java-checks-testkit/src/main/java/org/sonar/java/checks/verifier/internal/InternalInputFile.java | (34:46)-(34:68) | InternalMockedSonarAPI | CLASS |
| java-checks-testkit/src/main/java/org/sonar/java/checks/verifier/internal/InternalSensorContext.java | (114:11)-(114:32) | notSupportedException | METHOD_INVOCATION |
| java-checks-testkit/src/main/java/org/sonar/java/checks/verifier/internal/InternalSensorContext.java | (119:11)-(119:32) | notSupportedException | METHOD_INVOCATION |
| java-checks-testkit/src/main/java/org/sonar/java/checks/verifier/internal/InternalSensorContext.java | (124:11)-(124:32) | notSupportedException | METHOD_INVOCATION |
| java-checks-testkit/src/main/java/org/sonar/java/checks/verifier/internal/InternalSensorContext.java | (129:11)-(129:32) | notSupportedException | METHOD_INVOCATION |
| java-checks-testkit/src/main/java/org/sonar/java/checks/verifier/internal/InternalSensorContext.java | (149:11)-(149:32) | notSupportedException | METHOD_INVOCATION |
| java-checks-testkit/src/main/java/org/sonar/java/checks/verifier/internal/InternalSensorContext.java | (154:11)-(154:32) | notSupportedException | METHOD_INVOCATION |
| java-checks-testkit/src/main/java/org/sonar/java/checks/verifier/internal/InternalSensorContext.java | (159:11)-(159:32) | notSupportedException | METHOD_INVOCATION |
| java-checks-testkit/src/main/java/org/sonar/java/checks/verifier/internal/InternalSensorContext.java | (164:11)-(164:32) | notSupportedException | METHOD_INVOCATION |
| java-checks-testkit/src/main/java/org/sonar/java/checks/verifier/internal/InternalSensorContext.java | (169:11)-(169:32) | notSupportedException | METHOD_INVOCATION |

4729 additional occurrences omitted.


### Newly reported unknown

| File | Range | Identifier | AST context |
|---|---|---|---|
| java-frontend/src/main/java/org/sonar/java/matcher/TreeMatcher.java | (105:16)-(105:27) | TreeMatcher | PARAMETERIZED_TYPE |
| java-frontend/src/main/java/org/sonar/java/matcher/TreeMatcher.java | (106:25)-(106:38) | statementTree | PATTERN_INSTANCE_OF |
| java-frontend/src/main/java/org/sonar/java/matcher/TreeMatcher.java | (106:7)-(106:20) | statementTree | VARIABLE |
| java-frontend/src/main/java/org/sonar/java/matcher/TreeMatcher.java | (106:74)-(106:87) | exprStatement | VARIABLE |
| java-frontend/src/main/java/org/sonar/java/matcher/TreeMatcher.java | (107:12)-(107:18) | isCall | METHOD_INVOCATION |
| java-frontend/src/main/java/org/sonar/java/matcher/TreeMatcher.java | (107:19)-(107:42) | methodInvocationMatcher | ARGUMENTS |
| java-frontend/src/main/java/org/sonar/java/matcher/TreeMatcher.java | (107:50)-(107:63) | exprStatement | MEMBER_SELECT |
| java-frontend/src/main/java/org/sonar/java/matcher/TreeMatcher.java | (111:112)-(111:116) | name | ARGUMENTS |
| java-frontend/src/main/java/org/sonar/java/matcher/TreeMatcher.java | (111:16)-(111:27) | TreeMatcher | PARAMETERIZED_TYPE |
| java-frontend/src/main/java/org/sonar/java/matcher/TreeMatcher.java | (111:30)-(111:44) | expressionTree | VARIABLE |
| java-frontend/src/main/java/org/sonar/java/matcher/TreeMatcher.java | (111:48)-(111:62) | expressionTree | PATTERN_INSTANCE_OF |
| java-frontend/src/main/java/org/sonar/java/matcher/TreeMatcher.java | (111:89)-(111:91) | id | VARIABLE |
| java-frontend/src/main/java/org/sonar/java/matcher/TreeMatcher.java | (111:95)-(111:97) | id | MEMBER_SELECT |
| java-frontend/src/main/java/org/sonar/java/matcher/TreeMatcher.java | (115:114)-(115:120) | symbol | ARGUMENTS |
| java-frontend/src/main/java/org/sonar/java/matcher/TreeMatcher.java | (115:16)-(115:27) | TreeMatcher | PARAMETERIZED_TYPE |
| java-frontend/src/main/java/org/sonar/java/matcher/TreeMatcher.java | (115:30)-(115:44) | expressionTree | VARIABLE |
| java-frontend/src/main/java/org/sonar/java/matcher/TreeMatcher.java | (115:48)-(115:62) | expressionTree | PATTERN_INSTANCE_OF |
| java-frontend/src/main/java/org/sonar/java/matcher/TreeMatcher.java | (115:89)-(115:91) | id | VARIABLE |
| java-frontend/src/main/java/org/sonar/java/matcher/TreeMatcher.java | (115:95)-(115:97) | id | MEMBER_SELECT |
| java-frontend/src/main/java/org/sonar/java/matcher/TreeMatcher.java | (123:105)-(123:118) | lambdaMatcher | MEMBER_SELECT |

38 additional occurrences omitted.


Semantic graph location differences unavailable: traversal is DISABLED, PARTIAL, or UNKNOWN in at least one compared mode. Primary identifier and finding comparisons remain valid.

### Findings only in Baseline

| Rule | File | Line | Message |
|---|---|---:|---|
| java:S1144 | java-frontend/src/main/java/org/sonar/java/model/JParserConfig.java | 227 | Remove this unused private "FileByFile" constructor. |
| java:S9358 | java-frontend/src/main/java/org/sonar/java/model/InternalSyntaxToken.java | 45 | Move the conditional expression inside this operation. |
| java:S9358 | java-frontend/src/main/java/org/sonar/java/model/InternalSyntaxTrivia.java | 41 | Move the conditional expression inside this operation. |

### Findings only in Bytecode

| Rule | File | Line | Message |
|---|---|---:|---|
| java:S1185 | java-frontend/src/main/java/org/sonar/plugins/java/api/IssuableSubscriptionVisitor.java | 36 | Remove this method to simply inherit it. |
| java:S6539 | java-frontend/src/main/java/org/sonar/java/model/JParser.java | 256 | Split this “Monster Class” into smaller and more specialized ones to reduce its dependencies on other classes from 24 to the maximum authorized 20 or less. |
| java:S6539 | java-frontend/src/main/java/org/sonar/plugins/java/api/tree/BaseTreeVisitor.java | 27 | Split this “Monster Class” into smaller and more specialized ones to reduce its dependencies on other classes from 43 to the maximum authorized 20 or less. |

</details>

<details>
<summary>Combined versus Baseline</summary>

### Largest semantic improvements

Top five by absolute change in unknown percentage.

| File | Second mode unknown % | First mode unknown % | Change (pp) |
|---|---:|---:|---:|
| java-checks/src/main/java/org/sonar/java/checks/IgnoredOperationStatusCheck.java | 78.146% | 1.325% | -76.821 |
| java-checks/src/main/java/org/sonar/java/checks/spring/AsyncMethodsOnConfigurationClassCheck.java | 76.136% | 2.273% | -73.864 |
| java-checks/src/main/java/org/sonar/java/checks/PublicConstructorInAbstractClassCheck.java | 75.758% | 2.020% | -73.737 |
| java-checks/src/main/java/org/sonar/java/checks/AvoidHighFrameratesOnMobileCheck.java | 73.333% | 2.667% | -70.667 |
| java-checks/src/main/java/org/sonar/java/checks/security/IntegerToHexStringCheck.java | 72.381% | 1.905% | -70.476 |

### Largest semantic regressions

Top five by absolute change in unknown percentage.

None.

| Unknown occurrences | Count |
|---|---:|
| Still unknown in both modes | 10458 |
| No longer reported unknown | 82565 |
| Newly reported unknown | 4 |

### No longer reported unknown

| File | Range | Identifier | AST context |
|---|---|---|---|
| external-reports/src/main/java/org/sonar/java/externalreport/CheckstyleSensor.java | (70:35)-(70:48) | importIfExist | METHOD_INVOCATION |
| external-reports/src/main/java/org/sonar/java/externalreport/CheckstyleSensor.java | (76:33)-(76:37) | read | MEMBER_SELECT |
| external-reports/src/main/java/org/sonar/java/externalreport/CheckstyleSensor.java | (76:7)-(76:32) | CheckstyleXmlReportReader | MEMBER_SELECT |
| external-reports/src/main/java/org/sonar/java/externalreport/CheckstyleSensor.java | (83:24)-(83:33) | saveIssue | MEMBER_SELECT |
| external-reports/src/main/java/org/sonar/java/externalreport/CheckstyleSensor.java | (83:5)-(83:23) | ExternalIssueUtils | MEMBER_SELECT |
| external-reports/src/main/java/org/sonar/java/externalreport/PmdSensor.java | (68:35)-(68:48) | importIfExist | METHOD_INVOCATION |
| external-reports/src/main/java/org/sonar/java/externalreport/PmdSensor.java | (74:26)-(74:30) | read | MEMBER_SELECT |
| external-reports/src/main/java/org/sonar/java/externalreport/PmdSensor.java | (74:7)-(74:25) | PmdXmlReportReader | MEMBER_SELECT |
| external-reports/src/main/java/org/sonar/java/externalreport/PmdXmlReportReader.java | (110:19)-(110:28) | PmdSensor | MEMBER_SELECT |
| external-reports/src/main/java/org/sonar/java/externalreport/PmdXmlReportReader.java | (110:29)-(110:39) | LINTER_KEY | MEMBER_SELECT |
| external-reports/src/main/java/org/sonar/java/externalreport/SpotBugsSensor.java | (102:35)-(102:48) | importIfExist | METHOD_INVOCATION |
| external-reports/src/main/java/org/sonar/java/externalreport/SpotBugsSensor.java | (112:31)-(112:35) | read | MEMBER_SELECT |
| external-reports/src/main/java/org/sonar/java/externalreport/SpotBugsSensor.java | (112:7)-(112:30) | SpotBugsXmlReportReader | MEMBER_SELECT |
| external-reports/src/main/java/org/sonar/java/externalreport/SpotBugsXmlReportReader.java | (164:23)-(164:37) | SpotBugsSensor | MEMBER_SELECT |
| external-reports/src/main/java/org/sonar/java/externalreport/SpotBugsXmlReportReader.java | (164:38)-(164:50) | SPOTBUGS_KEY | MEMBER_SELECT |
| external-reports/src/main/java/org/sonar/java/externalreport/SpotBugsXmlReportReader.java | (172:24)-(172:33) | saveIssue | MEMBER_SELECT |
| external-reports/src/main/java/org/sonar/java/externalreport/SpotBugsXmlReportReader.java | (172:5)-(172:23) | ExternalIssueUtils | MEMBER_SELECT |
| java-checks-aws/src/main/java/org/sonar/java/checks/aws/AbstractAwsMethodVisitor.java | (29:56)-(29:83) | IssuableSubscriptionVisitor | CLASS |
| java-checks-aws/src/main/java/org/sonar/java/checks/aws/AbstractAwsMethodVisitor.java | (30:24)-(30:28) | List | PARAMETERIZED_TYPE |
| java-checks-aws/src/main/java/org/sonar/java/checks/aws/AbstractAwsMethodVisitor.java | (30:29)-(30:33) | Tree | MEMBER_SELECT |

82545 additional occurrences omitted.


### Newly reported unknown

| File | Range | Identifier | AST context |
|---|---|---|---|
| java-checks/src/main/java/org/sonar/java/checks/regex/AbstractRegexCheckTrackingMatchers.java | (193:22)-(193:30) | getRegex | METHOD_REFERENCE |
| java-checks/src/main/java/org/sonar/java/checks/regex/AbstractRegexCheckTrackingMatchers.java | (219:18)-(219:26) | getRegex | METHOD_REFERENCE |
| java-frontend/src/main/java/org/sonar/java/model/JSymbolMetadata.java | (110:16)-(110:31) | JSymbolMetadata | NEW_CLASS |
| java-frontend/src/main/java/org/sonar/java/model/JSymbolMetadata.java | (118:16)-(118:31) | JSymbolMetadata | NEW_CLASS |


Semantic graph location differences unavailable: traversal is DISABLED, PARTIAL, or UNKNOWN in at least one compared mode. Primary identifier and finding comparisons remain valid.

### Findings only in Baseline

| Rule | File | Line | Message |
|---|---|---:|---|
| java:S1144 | java-frontend/src/main/java/org/sonar/java/model/JParserConfig.java | 227 | Remove this unused private "FileByFile" constructor. |
| java:S1450 | java-checks/src/main/java/org/sonar/java/checks/TypeParametersShadowingCheck.java | 37 | Remove the "context" field and declare it as a local variable in the relevant methods. |
| java:S3398 | java-frontend/src/main/java/org/sonar/java/SemanticReportScanner.java | 338 | Move this method into "ModuleReferences". |
| java:S9358 | java-frontend/src/main/java/org/sonar/java/model/InternalSyntaxToken.java | 45 | Move the conditional expression inside this operation. |
| java:S9358 | java-frontend/src/main/java/org/sonar/java/model/InternalSyntaxTrivia.java | 41 | Move the conditional expression inside this operation. |

### Findings only in Combined

| Rule | File | Line | Message |
|---|---|---:|---|
| java:S107 | java-frontend/src/main/java/org/sonar/java/SonarComponents.java | 169 | Constructor has 8 parameters, which is greater than 7 authorized. |
| java:S107 | java-frontend/src/main/java/org/sonar/java/SonarComponents.java | 188 | Constructor has 8 parameters, which is greater than 7 authorized. |
| java:S107 | java-frontend/src/main/java/org/sonar/java/SonarComponents.java | 213 | Constructor has 9 parameters, which is greater than 7 authorized. |
| java:S107 | java-frontend/src/main/java/org/sonar/java/model/DefaultJavaFileScannerContext.java | 62 | Constructor has 8 parameters, which is greater than 7 authorized. |
| java:S107 | java-frontend/src/main/java/org/sonar/java/model/statement/ForStatementTreeImpl.java | 47 | Constructor has 9 parameters, which is greater than 7 authorized. |
| java:S107 | java-frontend/src/main/java/org/sonar/java/model/statement/TryStatementTreeImpl.java | 51 | Constructor has 8 parameters, which is greater than 7 authorized. |
| java:S110 | java-checks/src/main/java/org/sonar/java/checks/regex/AbstractRedosCheck.java | 51 | This class has 6 parents which is greater than 5 authorized. |
| java:S110 | java-checks/src/main/java/org/sonar/java/checks/regex/RedosCheck.java | 23 | This class has 7 parents which is greater than 5 authorized. |
| java:S110 | java-checks/src/main/java/org/sonar/java/checks/regex/RegexLookaheadCheck.java | 26 | This class has 6 parents which is greater than 5 authorized. |
| java:S110 | java-checks/src/main/java/org/sonar/java/checks/regex/ReluctantQuantifierWithEmptyContinuationCheck.java | 26 | This class has 6 parents which is greater than 5 authorized. |
| java:S110 | java-checks/src/main/java/org/sonar/java/checks/regex/SuperLinearRegexCheck.java | 23 | This class has 7 parents which is greater than 5 authorized. |
| java:S1185 | java-frontend/src/main/java/org/sonar/plugins/java/api/IssuableSubscriptionVisitor.java | 36 | Remove this method to simply inherit it. |
| java:S131 | java-checks/src/main/java/org/sonar/java/checks/BlockingOperationsInVirtualThreadsCheck.java | 96 | Complete cases by adding the missing enum constants or add a default case to this switch. |
| java:S131 | java-checks/src/main/java/org/sonar/java/checks/ThreadAsRunnableArgumentCheck.java | 55 | Complete cases by adding the missing enum constants or add a default case to this switch. |
| java:S131 | java-checks/src/main/java/org/sonar/java/checks/VolatileVariablesOperationsCheck.java | 125 | Complete cases by adding the missing enum constants or add a default case to this switch. |
| java:S1845 | java-checks-testkit/src/main/java/org/sonar/java/checks/verifier/TestCheckRegistrarContext.java | 39 | Rename field "testCheckClasses" to prevent any misunderstanding/clash with method "testCheckClasses" defined in superclass "org.sonar.plugins.java.api.CheckRegistrar$RegistrarContext". |
| java:S2234 | java-checks/src/main/java/org/sonar/java/checks/MembersDifferOnlyByCapitalizationCheck.java | 184 | Parameters to variableAndMethod have the same names but not the same order as the method arguments. |
| java:S3457 | java-checks/src/main/java/org/sonar/java/checks/OneDeclarationPerLineCheck.java | 123 | %n should be used in place of \n to produce the platform-specific line separator. |
| java:S5803 | java-checks-testkit/src/main/java/org/sonar/java/checks/verifier/internal/JavaCheckVerifier.java | 121 | Remove this usage of "scanForTesting", it is annotated with @VisibleForTesting and should not be accessed from production code. |
| java:S6204 | java-checks/src/main/java/org/sonar/java/checks/spring/SpringCacheableWithCachePutCheck.java | 89 | Replace this usage of 'Stream.collect(Collectors.toList())' with 'Stream.toList()' and ensure that the list is unmodified. |
| java:S6485 | java-checks/src/main/java/org/sonar/java/checks/unused/utils/AnnotationFieldReferenceFinder.java | 75 | Replace this call to the constructor with the better suited static method HashMap.newHashMap(int numMappings) |
| java:S6539 | java-frontend/src/main/java/org/sonar/java/model/JParser.java | 256 | Split this “Monster Class” into smaller and more specialized ones to reduce its dependencies on other classes from 59 to the maximum authorized 20 or less. |
| java:S6539 | java-frontend/src/main/java/org/sonar/plugins/java/api/tree/BaseTreeVisitor.java | 27 | Split this “Monster Class” into smaller and more specialized ones to reduce its dependencies on other classes from 73 to the maximum authorized 20 or less. |

</details>

<details>
<summary>Combined versus Source paths</summary>

### Largest semantic improvements

Top five by absolute change in unknown percentage.

None.

### Largest semantic regressions

Top five by absolute change in unknown percentage.

None.

| Unknown occurrences | Count |
|---|---:|
| Still unknown in both modes | 10462 |
| No longer reported unknown | 0 |
| Newly reported unknown | 0 |

### No longer reported unknown

None.


### Newly reported unknown

None.


Semantic graph location differences unavailable: traversal is DISABLED, PARTIAL, or UNKNOWN in at least one compared mode. Primary identifier and finding comparisons remain valid.

### Findings only in Source paths

None.

### Findings only in Combined

None.

</details>

<details>
<summary>Combined versus Bytecode</summary>

### Largest semantic improvements

Top five by absolute change in unknown percentage.

| File | Second mode unknown % | First mode unknown % | Change (pp) |
|---|---:|---:|---:|
| java-checks/src/main/java/org/sonar/java/checks/IgnoredOperationStatusCheck.java | 78.146% | 1.325% | -76.821 |
| java-checks/src/main/java/org/sonar/java/checks/spring/AsyncMethodsOnConfigurationClassCheck.java | 76.136% | 2.273% | -73.864 |
| java-checks/src/main/java/org/sonar/java/checks/PublicConstructorInAbstractClassCheck.java | 75.758% | 2.020% | -73.737 |
| java-checks/src/main/java/org/sonar/java/checks/AvoidHighFrameratesOnMobileCheck.java | 73.333% | 2.667% | -70.667 |
| java-checks/src/main/java/org/sonar/java/checks/security/IntegerToHexStringCheck.java | 72.381% | 1.905% | -70.476 |

### Largest semantic regressions

Top five by absolute change in unknown percentage.

None.

| Unknown occurrences | Count |
|---|---:|
| Still unknown in both modes | 10458 |
| No longer reported unknown | 77874 |
| Newly reported unknown | 4 |

### No longer reported unknown

| File | Range | Identifier | AST context |
|---|---|---|---|
| external-reports/src/main/java/org/sonar/java/externalreport/CheckstyleSensor.java | (70:35)-(70:48) | importIfExist | METHOD_INVOCATION |
| external-reports/src/main/java/org/sonar/java/externalreport/CheckstyleSensor.java | (76:33)-(76:37) | read | MEMBER_SELECT |
| external-reports/src/main/java/org/sonar/java/externalreport/CheckstyleSensor.java | (76:7)-(76:32) | CheckstyleXmlReportReader | MEMBER_SELECT |
| external-reports/src/main/java/org/sonar/java/externalreport/CheckstyleSensor.java | (83:24)-(83:33) | saveIssue | MEMBER_SELECT |
| external-reports/src/main/java/org/sonar/java/externalreport/CheckstyleSensor.java | (83:5)-(83:23) | ExternalIssueUtils | MEMBER_SELECT |
| external-reports/src/main/java/org/sonar/java/externalreport/PmdSensor.java | (68:35)-(68:48) | importIfExist | METHOD_INVOCATION |
| external-reports/src/main/java/org/sonar/java/externalreport/PmdSensor.java | (74:26)-(74:30) | read | MEMBER_SELECT |
| external-reports/src/main/java/org/sonar/java/externalreport/PmdSensor.java | (74:7)-(74:25) | PmdXmlReportReader | MEMBER_SELECT |
| external-reports/src/main/java/org/sonar/java/externalreport/PmdXmlReportReader.java | (110:19)-(110:28) | PmdSensor | MEMBER_SELECT |
| external-reports/src/main/java/org/sonar/java/externalreport/PmdXmlReportReader.java | (110:29)-(110:39) | LINTER_KEY | MEMBER_SELECT |
| external-reports/src/main/java/org/sonar/java/externalreport/SpotBugsSensor.java | (102:35)-(102:48) | importIfExist | METHOD_INVOCATION |
| external-reports/src/main/java/org/sonar/java/externalreport/SpotBugsSensor.java | (112:31)-(112:35) | read | MEMBER_SELECT |
| external-reports/src/main/java/org/sonar/java/externalreport/SpotBugsSensor.java | (112:7)-(112:30) | SpotBugsXmlReportReader | MEMBER_SELECT |
| external-reports/src/main/java/org/sonar/java/externalreport/SpotBugsXmlReportReader.java | (164:23)-(164:37) | SpotBugsSensor | MEMBER_SELECT |
| external-reports/src/main/java/org/sonar/java/externalreport/SpotBugsXmlReportReader.java | (164:38)-(164:50) | SPOTBUGS_KEY | MEMBER_SELECT |
| external-reports/src/main/java/org/sonar/java/externalreport/SpotBugsXmlReportReader.java | (172:24)-(172:33) | saveIssue | MEMBER_SELECT |
| external-reports/src/main/java/org/sonar/java/externalreport/SpotBugsXmlReportReader.java | (172:5)-(172:23) | ExternalIssueUtils | MEMBER_SELECT |
| java-checks-aws/src/main/java/org/sonar/java/checks/aws/AbstractAwsMethodVisitor.java | (29:56)-(29:83) | IssuableSubscriptionVisitor | CLASS |
| java-checks-aws/src/main/java/org/sonar/java/checks/aws/AbstractAwsMethodVisitor.java | (30:24)-(30:28) | List | PARAMETERIZED_TYPE |
| java-checks-aws/src/main/java/org/sonar/java/checks/aws/AbstractAwsMethodVisitor.java | (30:29)-(30:33) | Tree | MEMBER_SELECT |

77854 additional occurrences omitted.


### Newly reported unknown

| File | Range | Identifier | AST context |
|---|---|---|---|
| java-checks/src/main/java/org/sonar/java/checks/regex/AbstractRegexCheckTrackingMatchers.java | (193:22)-(193:30) | getRegex | METHOD_REFERENCE |
| java-checks/src/main/java/org/sonar/java/checks/regex/AbstractRegexCheckTrackingMatchers.java | (219:18)-(219:26) | getRegex | METHOD_REFERENCE |
| java-frontend/src/main/java/org/sonar/java/model/JSymbolMetadata.java | (110:16)-(110:31) | JSymbolMetadata | NEW_CLASS |
| java-frontend/src/main/java/org/sonar/java/model/JSymbolMetadata.java | (118:16)-(118:31) | JSymbolMetadata | NEW_CLASS |


Semantic graph location differences unavailable: traversal is DISABLED, PARTIAL, or UNKNOWN in at least one compared mode. Primary identifier and finding comparisons remain valid.

### Findings only in Bytecode

| Rule | File | Line | Message |
|---|---|---:|---|
| java:S1450 | java-checks/src/main/java/org/sonar/java/checks/TypeParametersShadowingCheck.java | 37 | Remove the "context" field and declare it as a local variable in the relevant methods. |
| java:S3398 | java-frontend/src/main/java/org/sonar/java/SemanticReportScanner.java | 338 | Move this method into "ModuleReferences". |

### Findings only in Combined

| Rule | File | Line | Message |
|---|---|---:|---|
| java:S107 | java-frontend/src/main/java/org/sonar/java/SonarComponents.java | 169 | Constructor has 8 parameters, which is greater than 7 authorized. |
| java:S107 | java-frontend/src/main/java/org/sonar/java/SonarComponents.java | 188 | Constructor has 8 parameters, which is greater than 7 authorized. |
| java:S107 | java-frontend/src/main/java/org/sonar/java/SonarComponents.java | 213 | Constructor has 9 parameters, which is greater than 7 authorized. |
| java:S107 | java-frontend/src/main/java/org/sonar/java/model/DefaultJavaFileScannerContext.java | 62 | Constructor has 8 parameters, which is greater than 7 authorized. |
| java:S107 | java-frontend/src/main/java/org/sonar/java/model/statement/ForStatementTreeImpl.java | 47 | Constructor has 9 parameters, which is greater than 7 authorized. |
| java:S107 | java-frontend/src/main/java/org/sonar/java/model/statement/TryStatementTreeImpl.java | 51 | Constructor has 8 parameters, which is greater than 7 authorized. |
| java:S110 | java-checks/src/main/java/org/sonar/java/checks/regex/AbstractRedosCheck.java | 51 | This class has 6 parents which is greater than 5 authorized. |
| java:S110 | java-checks/src/main/java/org/sonar/java/checks/regex/RedosCheck.java | 23 | This class has 7 parents which is greater than 5 authorized. |
| java:S110 | java-checks/src/main/java/org/sonar/java/checks/regex/RegexLookaheadCheck.java | 26 | This class has 6 parents which is greater than 5 authorized. |
| java:S110 | java-checks/src/main/java/org/sonar/java/checks/regex/ReluctantQuantifierWithEmptyContinuationCheck.java | 26 | This class has 6 parents which is greater than 5 authorized. |
| java:S110 | java-checks/src/main/java/org/sonar/java/checks/regex/SuperLinearRegexCheck.java | 23 | This class has 7 parents which is greater than 5 authorized. |
| java:S131 | java-checks/src/main/java/org/sonar/java/checks/BlockingOperationsInVirtualThreadsCheck.java | 96 | Complete cases by adding the missing enum constants or add a default case to this switch. |
| java:S131 | java-checks/src/main/java/org/sonar/java/checks/ThreadAsRunnableArgumentCheck.java | 55 | Complete cases by adding the missing enum constants or add a default case to this switch. |
| java:S131 | java-checks/src/main/java/org/sonar/java/checks/VolatileVariablesOperationsCheck.java | 125 | Complete cases by adding the missing enum constants or add a default case to this switch. |
| java:S1845 | java-checks-testkit/src/main/java/org/sonar/java/checks/verifier/TestCheckRegistrarContext.java | 39 | Rename field "testCheckClasses" to prevent any misunderstanding/clash with method "testCheckClasses" defined in superclass "org.sonar.plugins.java.api.CheckRegistrar$RegistrarContext". |
| java:S2234 | java-checks/src/main/java/org/sonar/java/checks/MembersDifferOnlyByCapitalizationCheck.java | 184 | Parameters to variableAndMethod have the same names but not the same order as the method arguments. |
| java:S3457 | java-checks/src/main/java/org/sonar/java/checks/OneDeclarationPerLineCheck.java | 123 | %n should be used in place of \n to produce the platform-specific line separator. |
| java:S5803 | java-checks-testkit/src/main/java/org/sonar/java/checks/verifier/internal/JavaCheckVerifier.java | 121 | Remove this usage of "scanForTesting", it is annotated with @VisibleForTesting and should not be accessed from production code. |
| java:S6204 | java-checks/src/main/java/org/sonar/java/checks/spring/SpringCacheableWithCachePutCheck.java | 89 | Replace this usage of 'Stream.collect(Collectors.toList())' with 'Stream.toList()' and ensure that the list is unmodified. |
| java:S6485 | java-checks/src/main/java/org/sonar/java/checks/unused/utils/AnnotationFieldReferenceFinder.java | 75 | Replace this call to the constructor with the better suited static method HashMap.newHashMap(int numMappings) |

</details>


## Measured timings

Warm-ups are excluded. JavaSensor time includes compilation.

| Sample | Mode | Maven/server wall (ms) | JavaSensor (ms) | Compilation (ms) | Compilation outcome | Generated classes |
|---|---|---:|---:|---:|---|---:|
| 1 | Baseline | 151272 | 73111.0 | 0.0 | DISABLED | 0 |
| 1 | Source paths | 284094 | 205191.0 | 0.0 | DISABLED | 0 |
| 1 | Bytecode | 160142 | 79763.0 | 1452.0 | FAILED | 192 |
| 1 | Combined | 291581 | 212191.0 | 1379.0 | FAILED | 192 |

## Run metadata

| Setting | Value |
|---|---|
| Baseline properties | {sonar.java.compileToByteCode=false, sonar.java.experimental.batchModeSizeInKB=500, sonar.java.fileByFile=true, sonar.java.sourcepath=, sonar.java.test.sourcepath=} |
| Bytecode properties | {sonar.java.compileToByteCode=true, sonar.java.experimental.batchModeSizeInKB=500, sonar.java.fileByFile=true, sonar.java.sourcepath=, sonar.java.test.sourcepath=} |
| Combined properties | {sonar.java.compileToByteCode=true, sonar.java.experimental.batchModeSizeInKB=500, sonar.java.fileByFile=true, sonar.java.sourcepath=/var/folders/q0/kmr6_fp118n0633zlbj5ydtw0000gq/T/junit-1607271975678351325/sonar-java-file-by-file-no-libraries/combined/java-frontend/src/main/java,/var/folders/q0/kmr6_fp118n0633zlbj5ydtw0000gq/T/junit-1607271975678351325/sonar-java-file-by-file-no-libraries/combined/java-checks-testkit/src/main/java,/var/folders/q0/kmr6_fp118n0633zlbj5ydtw0000gq/T/junit-1607271975678351325/sonar-java-file-by-file-no-libraries/combined/java-checks/src/main/java,/var/folders/q0/kmr6_fp118n0633zlbj5ydtw0000gq/T/junit-1607271975678351325/sonar-java-file-by-file-no-libraries/combined/java-checks-aws/src/main/java,/var/folders/q0/kmr6_fp118n0633zlbj5ydtw0000gq/T/junit-1607271975678351325/sonar-java-file-by-file-no-libraries/combined/check-list/src/main/java,/var/folders/q0/kmr6_fp118n0633zlbj5ydtw0000gq/T/junit-1607271975678351325/sonar-java-file-by-file-no-libraries/combined/external-reports/src/main/java,/var/folders/q0/kmr6_fp118n0633zlbj5ydtw0000gq/T/junit-1607271975678351325/sonar-java-file-by-file-no-libraries/combined/sonar-java-plugin/src/main/java,/var/folders/q0/kmr6_fp118n0633zlbj5ydtw0000gq/T/junit-1607271975678351325/sonar-java-file-by-file-no-libraries/combined/java-surefire/src/main/java,/var/folders/q0/kmr6_fp118n0633zlbj5ydtw0000gq/T/junit-1607271975678351325/sonar-java-file-by-file-no-libraries/combined/java-jsp/src/main/java,/var/folders/q0/kmr6_fp118n0633zlbj5ydtw0000gq/T/junit-1607271975678351325/sonar-java-file-by-file-no-libraries/combined/java-checks-common/src/main/java,/var/folders/q0/kmr6_fp118n0633zlbj5ydtw0000gq/T/junit-1607271975678351325/sonar-java-file-by-file-no-libraries/combined/java-checks-test-sources/test-classpath-reader/src/main/java, sonar.java.test.sourcepath=} |
| External library count | 0 |
| Graph diagnostic stability | stable |
| Module check-list external classpath | [] |
| Module external-reports external classpath | [] |
| Module java-checks external classpath | [] |
| Module java-checks-aws external classpath | [] |
| Module java-checks-common external classpath | [] |
| Module java-checks-test-sources/test-classpath-reader external classpath | [] |
| Module java-checks-testkit external classpath | [] |
| Module java-frontend external classpath | [] |
| Module java-jsp external classpath | [] |
| Module java-surefire external classpath | [] |
| Module sonar-java-plugin external classpath | [] |
| Repeated result stability | stable |
| Scenario | Each production file is parsed separately; no external dependency JARs are supplied; native Maven module scope |
| Shared scenario properties | {sonar.java.experimental.batchModeSizeInKB=500, sonar.java.fileByFile=true} |
| Source paths properties | {sonar.java.compileToByteCode=false, sonar.java.experimental.batchModeSizeInKB=500, sonar.java.fileByFile=true, sonar.java.sourcepath=/var/folders/q0/kmr6_fp118n0633zlbj5ydtw0000gq/T/junit-1607271975678351325/sonar-java-file-by-file-no-libraries/sourcepaths/java-frontend/src/main/java,/var/folders/q0/kmr6_fp118n0633zlbj5ydtw0000gq/T/junit-1607271975678351325/sonar-java-file-by-file-no-libraries/sourcepaths/java-checks-testkit/src/main/java,/var/folders/q0/kmr6_fp118n0633zlbj5ydtw0000gq/T/junit-1607271975678351325/sonar-java-file-by-file-no-libraries/sourcepaths/java-checks/src/main/java,/var/folders/q0/kmr6_fp118n0633zlbj5ydtw0000gq/T/junit-1607271975678351325/sonar-java-file-by-file-no-libraries/sourcepaths/java-checks-aws/src/main/java,/var/folders/q0/kmr6_fp118n0633zlbj5ydtw0000gq/T/junit-1607271975678351325/sonar-java-file-by-file-no-libraries/sourcepaths/check-list/src/main/java,/var/folders/q0/kmr6_fp118n0633zlbj5ydtw0000gq/T/junit-1607271975678351325/sonar-java-file-by-file-no-libraries/sourcepaths/external-reports/src/main/java,/var/folders/q0/kmr6_fp118n0633zlbj5ydtw0000gq/T/junit-1607271975678351325/sonar-java-file-by-file-no-libraries/sourcepaths/sonar-java-plugin/src/main/java,/var/folders/q0/kmr6_fp118n0633zlbj5ydtw0000gq/T/junit-1607271975678351325/sonar-java-file-by-file-no-libraries/sourcepaths/java-surefire/src/main/java,/var/folders/q0/kmr6_fp118n0633zlbj5ydtw0000gq/T/junit-1607271975678351325/sonar-java-file-by-file-no-libraries/sourcepaths/java-jsp/src/main/java,/var/folders/q0/kmr6_fp118n0633zlbj5ydtw0000gq/T/junit-1607271975678351325/sonar-java-file-by-file-no-libraries/sourcepaths/java-checks-common/src/main/java,/var/folders/q0/kmr6_fp118n0633zlbj5ydtw0000gq/T/junit-1607271975678351325/sonar-java-file-by-file-no-libraries/sourcepaths/java-checks-test-sources/test-classpath-reader/src/main/java, sonar.java.test.sourcepath=} |

</details>

<details>
<summary>sonar-java / file-by-file / dependencies</summary>

Each production file is parsed separately; every mode receives the same compile-scope external JARs; native Maven module scope

## Summary

| Metric | Baseline | Source paths | Bytecode | Combined |
|---|---:|---:|---:|---:|
| Source paths enabled | NO | YES | NO | YES |
| Internal compilation enabled | NO | NO | YES | YES |
| Scan status | SUCCESS | SUCCESS | SUCCESS | SUCCESS |
| Java files analyzed | 1320 | 1320 | 1320 | 1320 |
| Identifiers (total) | 219976 | 219976 | 219976 | 219976 |
| Known identifiers | 136674 | 219928 | 150907 | 219928 |
| Unknown identifiers | 83302 | 48 | 69069 | 48 |
| Unknown identifiers (%) | 37.869% | 0.022% | 31.398% | 0.022% |
| Semantic graph resolved symbols | N/A | N/A | N/A | N/A |
| Semantic graph unknown symbols | N/A | N/A | N/A | N/A |
| Semantic graph resolved types | N/A | N/A | N/A | N/A |
| Semantic graph unknown types | N/A | N/A | N/A | N/A |
| Semantic graph traversal | DISABLED | DISABLED | DISABLED | DISABLED |
| Graph expansion limit per module | 0 | 0 | 0 | 0 |
| Graph expansions executed (representative sample) | 0 | 0 | 0 | 0 |
| Per-file semantic observations | AVAILABLE | AVAILABLE | AVAILABLE | AVAILABLE |
| Files with no unknown identifiers | 102 / 1320 | 1313 / 1320 | 450 / 1320 | 1313 / 1320 |
| Findings | 99 | 117 | 104 | 117 |
| Median Maven/server time (ms) | 158672.0 | 289678.0 | 165487.0 | 301603.0 |
| Median JavaSensor time, including compilation (ms) | 80280.0 | 213817.0 | 86662.0 | 222564.0 |
| Compilation outcome | DISABLED | DISABLED | FAILED | FAILED |
| Median internal compilation time (ms) | 0.0 | 0.0 | 1734.0 | 1558.0 |
| Compilation source files | 0 | 0 | 1320 | 1320 |
| Generated class files before cleanup | 0 | 0 | 574 | 574 |
| Temporary bytecode cleaned up | YES | YES | YES | YES |
| Source characters analyzed | 5688552 | 5688552 | 5688552 | 5688552 |
| Undefined-type errors | 17160 | 14 | 12626 | 14 |
| Source characters with parse errors | N/A | N/A | N/A | N/A |
| Source characters with analysis exceptions | N/A | N/A | N/A | N/A |

## Semantics per module

Each cell shows **total / known / unknown (unknown %)** from canonical module counts. Empty aggregator modules retain their actual zero counts.

| Module | Baseline | Source paths | Bytecode | Combined |
|---|---:|---:|---:|---:|
| check-list | 331 / 331 / 0 (0.000%) | 331 / 331 / 0 (0.000%) | 331 / 331 / 0 (0.000%) | 331 / 331 / 0 (0.000%) |
| external-reports | 1270 / 1249 / 21 (1.654%) | 1270 / 1270 / 0 (0.000%) | 1270 / 1270 / 0 (0.000%) | 1270 / 1270 / 0 (0.000%) |
| java-checks | 152148 / 85624 / 66524 (43.723%) | 152148 / 152148 / 0 (0.000%) | 152148 / 85707 / 66441 (43.669%) | 152148 / 152148 / 0 (0.000%) |
| java-checks-aws | 1269 / 688 / 581 (45.784%) | 1269 / 1269 / 0 (0.000%) | 1269 / 688 / 581 (45.784%) | 1269 / 1269 / 0 (0.000%) |
| java-checks-common | 1426 / 801 / 625 (43.829%) | 1426 / 1426 / 0 (0.000%) | 1426 / 801 / 625 (43.829%) | 1426 / 1426 / 0 (0.000%) |
| java-checks-test-sources/test-classpath-reader | 394 / 394 / 0 (0.000%) | 394 / 394 / 0 (0.000%) | 394 / 394 / 0 (0.000%) | 394 / 394 / 0 (0.000%) |
| java-checks-testkit | 5649 / 4587 / 1062 (18.800%) | 5649 / 5646 / 3 (0.053%) | 5649 / 4633 / 1016 (17.985%) | 5649 / 5646 / 3 (0.053%) |
| java-frontend | 54171 / 40263 / 13908 (25.674%) | 54171 / 54171 / 0 (0.000%) | 54171 / 54171 / 0 (0.000%) | 54171 / 54171 / 0 (0.000%) |
| java-jsp | 659 / 633 / 26 (3.945%) | 659 / 659 / 0 (0.000%) | 659 / 633 / 26 (3.945%) | 659 / 659 / 0 (0.000%) |
| java-surefire | 1155 / 984 / 171 (14.805%) | 1155 / 1155 / 0 (0.000%) | 1155 / 1139 / 16 (1.385%) | 1155 / 1155 / 0 (0.000%) |
| sonar-java-plugin | 1504 / 1120 / 384 (25.532%) | 1504 / 1459 / 45 (2.992%) | 1504 / 1140 / 364 (24.202%) | 1504 / 1459 / 45 (2.992%) |

### Feature comparisons

Changes are the first named mode minus the second.

| Comparison | Status | Unknown count change | Unknown % change (pp) | Files improved / unchanged / regressed | Shared findings | Second mode only | First mode only | Retention |
|---|---|---:|---:|---:|---:|---:|---:|---:|
| Source paths versus Baseline | VALID | -83254 | -37.847 | 1218 / 102 / 0 | 94 | 5 | 23 | 94.949% |
| Bytecode versus Baseline | VALID | -14233 | -6.470 | 367 / 953 / 0 | 95 | 4 | 9 | 95.960% |
| Combined versus Baseline | VALID | -83254 | -37.847 | 1218 / 102 / 0 | 94 | 5 | 23 | 94.949% |
| Combined versus Source paths | VALID | +0 | +0.000 | 0 / 1320 / 0 | 117 | 0 | 0 | 100.000% |
| Combined versus Bytecode | VALID | -69021 | -31.377 | 870 / 450 / 0 | 103 | 1 | 14 | 99.038% |

## Unknown occurrences by AST context

Context is the identifier's AST parent kind. Modes without occurrence details are UNAVAILABLE.

| AST context | Baseline | Source paths | Bytecode | Combined |
|---|---:|---:|---:|---:|
| AND | 1 | 0 | 1 | 0 |
| ANNOTATION | 202 | 0 | 49 | 0 |
| ARGUMENTS | 1719 | 2 | 1398 | 2 |
| ARRAY_TYPE | 68 | 0 | 37 | 0 |
| ASSIGNMENT | 19 | 0 | 17 | 0 |
| CASE_LABEL | 443 | 0 | 278 | 0 |
| CLASS | 1074 | 0 | 956 | 0 |
| CONDITIONAL_AND | 2 | 0 | 2 | 0 |
| CONDITIONAL_EXPRESSION | 11 | 0 | 5 | 0 |
| CONDITIONAL_OR | 5 | 0 | 5 | 0 |
| EQUAL_TO | 27 | 0 | 18 | 0 |
| EXTENDS_WILDCARD | 97 | 0 | 54 | 0 |
| FOR_EACH_STATEMENT | 3 | 0 | 1 | 0 |
| GREATER_THAN | 21 | 0 | 21 | 0 |
| GREATER_THAN_OR_EQUAL_TO | 4 | 0 | 4 | 0 |
| IF_STATEMENT | 2 | 0 | 2 | 0 |
| INSTANCE_OF | 66 | 0 | 59 | 0 |
| LAMBDA_EXPRESSION | 3 | 0 | 2 | 0 |
| LESS_THAN | 7 | 0 | 7 | 0 |
| LIST | 415 | 0 | 170 | 0 |
| LOGICAL_COMPLEMENT | 3 | 0 | 3 | 0 |
| MEMBER_SELECT | 53314 | 38 | 46357 | 38 |
| METHOD | 1368 | 0 | 461 | 0 |
| METHOD_INVOCATION | 2002 | 2 | 1734 | 2 |
| METHOD_REFERENCE | 834 | 0 | 726 | 0 |
| MULTIPLY | 2 | 0 | 2 | 0 |
| NEW_ARRAY | 13 | 0 | 7 | 0 |
| NEW_CLASS | 438 | 0 | 133 | 0 |
| NOT_EQUAL_TO | 44 | 0 | 39 | 0 |
| PARAMETERIZED_TYPE | 3300 | 0 | 2309 | 0 |
| PATTERN_INSTANCE_OF | 8 | 0 | 5 | 0 |
| PLUS | 33 | 0 | 28 | 0 |
| REMAINDER | 1 | 0 | 1 | 0 |
| RETURN_STATEMENT | 25 | 0 | 4 | 0 |
| SUPER_WILDCARD | 1 | 0 | 1 | 0 |
| SWITCH_EXPRESSION | 2 | 0 | 2 | 0 |
| SWITCH_STATEMENT | 1 | 0 | 1 | 0 |
| TYPE_ARGUMENTS | 2222 | 0 | 1466 | 0 |
| TYPE_CAST | 2449 | 0 | 2177 | 0 |
| TYPE_PARAMETER | 8 | 0 | 1 | 0 |
| UNARY_MINUS | 1 | 0 | 1 | 0 |
| VARIABLE | 13044 | 6 | 10525 | 6 |

## Top files contributing unknown identifiers

Top five by the largest unknown count in any mode. Cells show **unknown count (share of project unknowns)**.

| File | Baseline | Source paths | Bytecode | Combined |
|---|---:|---:|---:|---:|
| java-frontend/src/main/java/org/sonar/java/model/JParser.java | 1276 (1.532%) | 0 (0.000%) | 0 (0.000%) | 0 (0.000%) |
| java-checks/src/main/java/org/sonar/java/checks/tests/AssertJChainSimplificationIndex.java | 670 (0.804%) | 0 (0.000%) | 670 (0.970%) | 0 (0.000%) |
| java-frontend/src/main/java/org/sonar/java/cfg/CFG.java | 603 (0.724%) | 0 (0.000%) | 0 (0.000%) | 0 (0.000%) |
| java-checks/src/main/java/org/sonar/java/checks/ReplaceLambdaByMethodRefCheck.java | 530 (0.636%) | 0 (0.000%) | 530 (0.767%) | 0 (0.000%) |
| java-checks-testkit/src/main/java/org/sonar/java/checks/verifier/internal/InternalCheckVerifier.java | 520 (0.624%) | 1 (2.083%) | 520 (0.753%) | 1 (2.083%) |

## Semantics per file

Each cell shows **total / known / unknown (unknown %)**. Absent observations are UNAVAILABLE; missing coverage in an observed mode is N/A.

| File | Baseline | Source paths | Bytecode | Combined |
|---|---:|---:|---:|---:|
| check-list/src/main/java/org/sonar/java/CheckListGenerator.java | 327 / 327 / 0 (0.000%) | 327 / 327 / 0 (0.000%) | 327 / 327 / 0 (0.000%) | 327 / 327 / 0 (0.000%) |
| check-list/src/main/java/org/sonar/java/package-info.java | 4 / 4 / 0 (0.000%) | 4 / 4 / 0 (0.000%) | 4 / 4 / 0 (0.000%) | 4 / 4 / 0 (0.000%) |
| external-reports/src/main/java/org/sonar/java/externalreport/CheckstyleSensor.java | 115 / 110 / 5 (4.348%) | 115 / 115 / 0 (0.000%) | 115 / 115 / 0 (0.000%) | 115 / 115 / 0 (0.000%) |
| external-reports/src/main/java/org/sonar/java/externalreport/CheckstyleXmlReportReader.java | 218 / 218 / 0 (0.000%) | 218 / 218 / 0 (0.000%) | 218 / 218 / 0 (0.000%) | 218 / 218 / 0 (0.000%) |
| external-reports/src/main/java/org/sonar/java/externalreport/ExternalIssueUtils.java | 91 / 91 / 0 (0.000%) | 91 / 91 / 0 (0.000%) | 91 / 91 / 0 (0.000%) | 91 / 91 / 0 (0.000%) |
| external-reports/src/main/java/org/sonar/java/externalreport/ExternalRulesDefinition.java | 32 / 32 / 0 (0.000%) | 32 / 32 / 0 (0.000%) | 32 / 32 / 0 (0.000%) | 32 / 32 / 0 (0.000%) |
| external-reports/src/main/java/org/sonar/java/externalreport/PmdSensor.java | 89 / 86 / 3 (3.371%) | 89 / 89 / 0 (0.000%) | 89 / 89 / 0 (0.000%) | 89 / 89 / 0 (0.000%) |
| external-reports/src/main/java/org/sonar/java/externalreport/PmdXmlReportReader.java | 283 / 277 / 6 (2.120%) | 283 / 283 / 0 (0.000%) | 283 / 283 / 0 (0.000%) | 283 / 283 / 0 (0.000%) |
| external-reports/src/main/java/org/sonar/java/externalreport/SpotBugsSensor.java | 143 / 140 / 3 (2.098%) | 143 / 143 / 0 (0.000%) | 143 / 143 / 0 (0.000%) | 143 / 143 / 0 (0.000%) |
| external-reports/src/main/java/org/sonar/java/externalreport/SpotBugsXmlReportReader.java | 294 / 290 / 4 (1.361%) | 294 / 294 / 0 (0.000%) | 294 / 294 / 0 (0.000%) | 294 / 294 / 0 (0.000%) |
| external-reports/src/main/java/org/sonar/java/externalreport/package-info.java | 5 / 5 / 0 (0.000%) | 5 / 5 / 0 (0.000%) | 5 / 5 / 0 (0.000%) | 5 / 5 / 0 (0.000%) |
| java-checks-aws/src/main/java/org/sonar/java/checks/aws/AbstractAwsMethodVisitor.java | 66 / 30 / 36 (54.545%) | 66 / 66 / 0 (0.000%) | 66 / 30 / 36 (54.545%) | 66 / 66 / 0 (0.000%) |
| java-checks-aws/src/main/java/org/sonar/java/checks/aws/AwsBuilderMethodFinder.java | 300 / 152 / 148 (49.333%) | 300 / 300 / 0 (0.000%) | 300 / 152 / 148 (49.333%) | 300 / 300 / 0 (0.000%) |
| java-checks-aws/src/main/java/org/sonar/java/checks/aws/AwsConsumerBuilderUsageCheck.java | 216 / 109 / 107 (49.537%) | 216 / 216 / 0 (0.000%) | 216 / 109 / 107 (49.537%) | 216 / 216 / 0 (0.000%) |
| java-checks-aws/src/main/java/org/sonar/java/checks/aws/AwsCredentialsShouldBeSetExplicitlyCheck.java | 25 / 15 / 10 (40.000%) | 25 / 25 / 0 (0.000%) | 25 / 15 / 10 (40.000%) | 25 / 25 / 0 (0.000%) |
| java-checks-aws/src/main/java/org/sonar/java/checks/aws/AwsLambdaSyncCallCheck.java | 250 / 129 / 121 (48.400%) | 250 / 250 / 0 (0.000%) | 250 / 129 / 121 (48.400%) | 250 / 250 / 0 (0.000%) |
| java-checks-aws/src/main/java/org/sonar/java/checks/aws/AwsLongTermAccessKeysCheck.java | 28 / 17 / 11 (39.286%) | 28 / 28 / 0 (0.000%) | 28 / 17 / 11 (39.286%) | 28 / 28 / 0 (0.000%) |
| java-checks-aws/src/main/java/org/sonar/java/checks/aws/AwsRegionSetterCheck.java | 43 / 25 / 18 (41.860%) | 43 / 43 / 0 (0.000%) | 43 / 25 / 18 (41.860%) | 43 / 43 / 0 (0.000%) |
| java-checks-aws/src/main/java/org/sonar/java/checks/aws/AwsRegionShouldBeSetExplicitlyCheck.java | 25 / 15 / 10 (40.000%) | 25 / 25 / 0 (0.000%) | 25 / 15 / 10 (40.000%) | 25 / 25 / 0 (0.000%) |
| java-checks-aws/src/main/java/org/sonar/java/checks/aws/AwsReusableResourcesInitializedOnceCheck.java | 140 / 78 / 62 (44.286%) | 140 / 140 / 0 (0.000%) | 140 / 78 / 62 (44.286%) | 140 / 140 / 0 (0.000%) |
| java-checks-aws/src/main/java/org/sonar/java/checks/aws/package-info.java | 6 / 6 / 0 (0.000%) | 6 / 6 / 0 (0.000%) | 6 / 6 / 0 (0.000%) | 6 / 6 / 0 (0.000%) |
| java-checks-aws/src/main/java/org/sonar/java/checks/security/HardCodedCredentialsShouldNotBeUsedCheck.java | 164 / 106 / 58 (35.366%) | 164 / 164 / 0 (0.000%) | 164 / 106 / 58 (35.366%) | 164 / 164 / 0 (0.000%) |
| java-checks-aws/src/main/java/org/sonar/java/checks/security/package-info.java | 6 / 6 / 0 (0.000%) | 6 / 6 / 0 (0.000%) | 6 / 6 / 0 (0.000%) | 6 / 6 / 0 (0.000%) |
| java-checks-common/src/main/java/org/sonar/java/checks/helpers/CredentialMethod.java | 96 / 83 / 13 (13.542%) | 96 / 96 / 0 (0.000%) | 96 / 83 / 13 (13.542%) | 96 / 96 / 0 (0.000%) |
| java-checks-common/src/main/java/org/sonar/java/checks/helpers/CredentialMethodsLoader.java | 48 / 39 / 9 (18.750%) | 48 / 48 / 0 (0.000%) | 48 / 39 / 9 (18.750%) | 48 / 48 / 0 (0.000%) |
| java-checks-common/src/main/java/org/sonar/java/checks/helpers/ExpressionsHelper.java | 514 / 291 / 223 (43.385%) | 514 / 514 / 0 (0.000%) | 514 / 291 / 223 (43.385%) | 514 / 514 / 0 (0.000%) |
| java-checks-common/src/main/java/org/sonar/java/checks/helpers/HardcodedStringExpressionChecker.java | 397 / 181 / 216 (54.408%) | 397 / 397 / 0 (0.000%) | 397 / 181 / 216 (54.408%) | 397 / 397 / 0 (0.000%) |
| java-checks-common/src/main/java/org/sonar/java/checks/helpers/ReassignmentFinder.java | 166 / 91 / 75 (45.181%) | 166 / 166 / 0 (0.000%) | 166 / 91 / 75 (45.181%) | 166 / 166 / 0 (0.000%) |
| java-checks-common/src/main/java/org/sonar/java/checks/helpers/TreeHelper.java | 105 / 57 / 48 (45.714%) | 105 / 105 / 0 (0.000%) | 105 / 57 / 48 (45.714%) | 105 / 105 / 0 (0.000%) |
| java-checks-common/src/main/java/org/sonar/java/checks/helpers/package-info.java | 6 / 6 / 0 (0.000%) | 6 / 6 / 0 (0.000%) | 6 / 6 / 0 (0.000%) | 6 / 6 / 0 (0.000%) |
| java-checks-common/src/main/java/org/sonar/java/checks/methods/AbstractMethodDetection.java | 88 / 47 / 41 (46.591%) | 88 / 88 / 0 (0.000%) | 88 / 47 / 41 (46.591%) | 88 / 88 / 0 (0.000%) |
| java-checks-common/src/main/java/org/sonar/java/checks/methods/package-info.java | 6 / 6 / 0 (0.000%) | 6 / 6 / 0 (0.000%) | 6 / 6 / 0 (0.000%) | 6 / 6 / 0 (0.000%) |
| java-checks-test-sources/test-classpath-reader/src/main/java/org/sonar/java/test/classpath/TestClasspathUtils.java | 388 / 388 / 0 (0.000%) | 388 / 388 / 0 (0.000%) | 388 / 388 / 0 (0.000%) | 388 / 388 / 0 (0.000%) |
| java-checks-test-sources/test-classpath-reader/src/main/java/org/sonar/java/test/classpath/package-info.java | 6 / 6 / 0 (0.000%) | 6 / 6 / 0 (0.000%) | 6 / 6 / 0 (0.000%) | 6 / 6 / 0 (0.000%) |
| java-checks-testkit/src/main/java/org/sonar/java/checks/verifier/CheckVerifier.java | 106 / 98 / 8 (7.547%) | 106 / 105 / 1 (0.943%) | 106 / 98 / 8 (7.547%) | 106 / 105 / 1 (0.943%) |
| java-checks-testkit/src/main/java/org/sonar/java/checks/verifier/FilesUtils.java | 63 / 62 / 1 (1.587%) | 63 / 63 / 0 (0.000%) | 63 / 62 / 1 (1.587%) | 63 / 63 / 0 (0.000%) |
| java-checks-testkit/src/main/java/org/sonar/java/checks/verifier/TestCheckRegistrarContext.java | 205 / 166 / 39 (19.024%) | 205 / 205 / 0 (0.000%) | 205 / 166 / 39 (19.024%) | 205 / 205 / 0 (0.000%) |
| java-checks-testkit/src/main/java/org/sonar/java/checks/verifier/TestProfileRegistrarContext.java | 28 / 26 / 2 (7.143%) | 28 / 28 / 0 (0.000%) | 28 / 26 / 2 (7.143%) | 28 / 28 / 0 (0.000%) |
| java-checks-testkit/src/main/java/org/sonar/java/checks/verifier/TestUtils.java | 129 / 112 / 17 (13.178%) | 129 / 129 / 0 (0.000%) | 129 / 112 / 17 (13.178%) | 129 / 129 / 0 (0.000%) |
| java-checks-testkit/src/main/java/org/sonar/java/checks/verifier/internal/CacheEnabledSensorContext.java | 33 / 32 / 1 (3.030%) | 33 / 33 / 0 (0.000%) | 33 / 33 / 0 (0.000%) | 33 / 33 / 0 (0.000%) |
| java-checks-testkit/src/main/java/org/sonar/java/checks/verifier/internal/CheckVerifierUtils.java | 103 / 93 / 10 (9.709%) | 103 / 103 / 0 (0.000%) | 103 / 95 / 8 (7.767%) | 103 / 103 / 0 (0.000%) |
| java-checks-testkit/src/main/java/org/sonar/java/checks/verifier/internal/Expectations.java | 1661 / 1566 / 95 (5.719%) | 1661 / 1661 / 0 (0.000%) | 1661 / 1566 / 95 (5.719%) | 1661 / 1661 / 0 (0.000%) |
| java-checks-testkit/src/main/java/org/sonar/java/checks/verifier/internal/InternalCacheContext.java | 75 / 68 / 7 (9.333%) | 75 / 75 / 0 (0.000%) | 75 / 68 / 7 (9.333%) | 75 / 75 / 0 (0.000%) |
| java-checks-testkit/src/main/java/org/sonar/java/checks/verifier/internal/InternalCheckVerifier.java | 1572 / 1052 / 520 (33.079%) | 1572 / 1571 / 1 (0.064%) | 1572 / 1052 / 520 (33.079%) | 1572 / 1571 / 1 (0.064%) |
| java-checks-testkit/src/main/java/org/sonar/java/checks/verifier/internal/InternalConfiguration.java | 45 / 41 / 4 (8.889%) | 45 / 45 / 0 (0.000%) | 45 / 43 / 2 (4.444%) | 45 / 45 / 0 (0.000%) |
| java-checks-testkit/src/main/java/org/sonar/java/checks/verifier/internal/InternalFileSystem.java | 77 / 70 / 7 (9.091%) | 77 / 77 / 0 (0.000%) | 77 / 77 / 0 (0.000%) | 77 / 77 / 0 (0.000%) |
| java-checks-testkit/src/main/java/org/sonar/java/checks/verifier/internal/InternalInputFile.java | 255 / 246 / 9 (3.529%) | 255 / 255 / 0 (0.000%) | 255 / 251 / 4 (1.569%) | 255 / 255 / 0 (0.000%) |
| java-checks-testkit/src/main/java/org/sonar/java/checks/verifier/internal/InternalMockedSonarAPI.java | 37 / 37 / 0 (0.000%) | 37 / 37 / 0 (0.000%) | 37 / 37 / 0 (0.000%) | 37 / 37 / 0 (0.000%) |
| java-checks-testkit/src/main/java/org/sonar/java/checks/verifier/internal/InternalReadCache.java | 68 / 66 / 2 (2.941%) | 68 / 68 / 0 (0.000%) | 68 / 68 / 0 (0.000%) | 68 / 68 / 0 (0.000%) |
| java-checks-testkit/src/main/java/org/sonar/java/checks/verifier/internal/InternalSensorContext.java | 163 / 139 / 24 (14.724%) | 163 / 163 / 0 (0.000%) | 163 / 162 / 1 (0.613%) | 163 / 163 / 0 (0.000%) |
| java-checks-testkit/src/main/java/org/sonar/java/checks/verifier/internal/InternalSonarRuntime.java | 31 / 31 / 0 (0.000%) | 31 / 31 / 0 (0.000%) | 31 / 31 / 0 (0.000%) | 31 / 31 / 0 (0.000%) |
| java-checks-testkit/src/main/java/org/sonar/java/checks/verifier/internal/InternalTextPointer.java | 42 / 42 / 0 (0.000%) | 42 / 42 / 0 (0.000%) | 42 / 42 / 0 (0.000%) | 42 / 42 / 0 (0.000%) |
| java-checks-testkit/src/main/java/org/sonar/java/checks/verifier/internal/InternalTextRange.java | 49 / 45 / 4 (8.163%) | 49 / 49 / 0 (0.000%) | 49 / 49 / 0 (0.000%) | 49 / 49 / 0 (0.000%) |
| java-checks-testkit/src/main/java/org/sonar/java/checks/verifier/internal/InternalWriteCache.java | 65 / 65 / 0 (0.000%) | 65 / 65 / 0 (0.000%) | 65 / 65 / 0 (0.000%) | 65 / 65 / 0 (0.000%) |
| java-checks-testkit/src/main/java/org/sonar/java/checks/verifier/internal/JavaCheckVerifier.java | 829 / 517 / 312 (37.636%) | 829 / 828 / 1 (0.121%) | 829 / 517 / 312 (37.636%) | 829 / 828 / 1 (0.121%) |
| java-checks-testkit/src/main/java/org/sonar/java/checks/verifier/internal/package-info.java | 7 / 7 / 0 (0.000%) | 7 / 7 / 0 (0.000%) | 7 / 7 / 0 (0.000%) | 7 / 7 / 0 (0.000%) |
| java-checks-testkit/src/main/java/org/sonar/java/checks/verifier/package-info.java | 6 / 6 / 0 (0.000%) | 6 / 6 / 0 (0.000%) | 6 / 6 / 0 (0.000%) | 6 / 6 / 0 (0.000%) |
| java-checks/src/main/java/org/sonar/java/checks/AbsOnNegativeCheck.java | 155 / 61 / 94 (60.645%) | 155 / 155 / 0 (0.000%) | 155 / 61 / 94 (60.645%) | 155 / 155 / 0 (0.000%) |
| java-checks/src/main/java/org/sonar/java/checks/AbstractAccessibilityChangeChecker.java | 257 / 91 / 166 (64.591%) | 257 / 257 / 0 (0.000%) | 257 / 91 / 166 (64.591%) | 257 / 257 / 0 (0.000%) |
| java-checks/src/main/java/org/sonar/java/checks/AbstractBadFieldNameChecker.java | 85 / 53 / 32 (37.647%) | 85 / 85 / 0 (0.000%) | 85 / 53 / 32 (37.647%) | 85 / 85 / 0 (0.000%) |
| java-checks/src/main/java/org/sonar/java/checks/AbstractCallToDeprecatedCodeChecker.java | 249 / 117 / 132 (53.012%) | 249 / 249 / 0 (0.000%) | 249 / 117 / 132 (53.012%) | 249 / 249 / 0 (0.000%) |
| java-checks/src/main/java/org/sonar/java/checks/AbstractClassNoFieldShouldBeInterfaceCheck.java | 167 / 76 / 91 (54.491%) | 167 / 167 / 0 (0.000%) | 167 / 76 / 91 (54.491%) | 167 / 167 / 0 (0.000%) |
| java-checks/src/main/java/org/sonar/java/checks/AbstractClassWithoutAbstractMethodCheck.java | 119 / 73 / 46 (38.655%) | 119 / 119 / 0 (0.000%) | 119 / 73 / 46 (38.655%) | 119 / 119 / 0 (0.000%) |
| java-checks/src/main/java/org/sonar/java/checks/AbstractCreateTempFileChecker.java | 242 / 142 / 100 (41.322%) | 242 / 242 / 0 (0.000%) | 242 / 142 / 100 (41.322%) | 242 / 242 / 0 (0.000%) |
| java-checks/src/main/java/org/sonar/java/checks/AbstractForLoopRule.java | 372 / 222 / 150 (40.323%) | 372 / 372 / 0 (0.000%) | 372 / 222 / 150 (40.323%) | 372 / 372 / 0 (0.000%) |
| java-checks/src/main/java/org/sonar/java/checks/AbstractHardCodedCredentialChecker.java | 393 / 242 / 151 (38.422%) | 393 / 393 / 0 (0.000%) | 393 / 242 / 151 (38.422%) | 393 / 393 / 0 (0.000%) |
| java-checks/src/main/java/org/sonar/java/checks/AbstractInSynchronizeChecker.java | 107 / 53 / 54 (50.467%) | 107 / 107 / 0 (0.000%) | 107 / 53 / 54 (50.467%) | 107 / 107 / 0 (0.000%) |
| java-checks/src/main/java/org/sonar/java/checks/AbstractMissingDeprecatedChecker.java | 79 / 55 / 24 (30.380%) | 79 / 79 / 0 (0.000%) | 79 / 55 / 24 (30.380%) | 79 / 79 / 0 (0.000%) |
| java-checks/src/main/java/org/sonar/java/checks/AbstractPackageInfoChecker.java | 125 / 94 / 31 (24.800%) | 125 / 125 / 0 (0.000%) | 125 / 94 / 31 (24.800%) | 125 / 125 / 0 (0.000%) |
| java-checks/src/main/java/org/sonar/java/checks/AbstractPrintfChecker.java | 652 / 485 / 167 (25.613%) | 652 / 652 / 0 (0.000%) | 652 / 485 / 167 (25.613%) | 652 / 652 / 0 (0.000%) |
| java-checks/src/main/java/org/sonar/java/checks/AbstractSerializableInnerClassRule.java | 107 / 56 / 51 (47.664%) | 107 / 107 / 0 (0.000%) | 107 / 56 / 51 (47.664%) | 107 / 107 / 0 (0.000%) |
| java-checks/src/main/java/org/sonar/java/checks/AccessibilityChangeCheck.java | 23 / 15 / 8 (34.783%) | 23 / 23 / 0 (0.000%) | 23 / 15 / 8 (34.783%) | 23 / 23 / 0 (0.000%) |
| java-checks/src/main/java/org/sonar/java/checks/AccessibilityChangeOnRecordsCheck.java | 187 / 86 / 101 (54.011%) | 187 / 187 / 0 (0.000%) | 187 / 86 / 101 (54.011%) | 187 / 187 / 0 (0.000%) |
| java-checks/src/main/java/org/sonar/java/checks/AllBranchesAreIdenticalCheck.java | 114 / 45 / 69 (60.526%) | 114 / 114 / 0 (0.000%) | 114 / 45 / 69 (60.526%) | 114 / 114 / 0 (0.000%) |
| java-checks/src/main/java/org/sonar/java/checks/AlmostJavadocCheck.java | 356 / 194 / 162 (45.506%) | 356 / 356 / 0 (0.000%) | 356 / 194 / 162 (45.506%) | 356 / 356 / 0 (0.000%) |
| java-checks/src/main/java/org/sonar/java/checks/AnnotationDefaultArgumentCheck.java | 129 / 79 / 50 (38.760%) | 129 / 129 / 0 (0.000%) | 129 / 79 / 50 (38.760%) | 129 / 129 / 0 (0.000%) |
| java-checks/src/main/java/org/sonar/java/checks/AnonymousClassOnFunctionalInterfaceCheck.java | 23 / 16 / 7 (30.435%) | 23 / 23 / 0 (0.000%) | 23 / 16 / 7 (30.435%) | 23 / 23 / 0 (0.000%) |
| java-checks/src/main/java/org/sonar/java/checks/AnonymousClassShouldBeLambdaCheck.java | 23 / 16 / 7 (30.435%) | 23 / 23 / 0 (0.000%) | 23 / 16 / 7 (30.435%) | 23 / 23 / 0 (0.000%) |
| java-checks/src/main/java/org/sonar/java/checks/AnonymousClassesTooBigCheck.java | 71 / 53 / 18 (25.352%) | 71 / 71 / 0 (0.000%) | 71 / 53 / 18 (25.352%) | 71 / 71 / 0 (0.000%) |
| java-checks/src/main/java/org/sonar/java/checks/ArrayCopyLoopCheck.java | 677 / 343 / 334 (49.335%) | 677 / 677 / 0 (0.000%) | 677 / 343 / 334 (49.335%) | 677 / 677 / 0 (0.000%) |
| java-checks/src/main/java/org/sonar/java/checks/ArrayDesignatorAfterTypeCheck.java | 54 / 19 / 35 (64.815%) | 54 / 54 / 0 (0.000%) | 54 / 19 / 35 (64.815%) | 54 / 54 / 0 (0.000%) |
| java-checks/src/main/java/org/sonar/java/checks/ArrayDesignatorOnVariableCheck.java | 189 / 89 / 100 (52.910%) | 189 / 189 / 0 (0.000%) | 189 / 89 / 100 (52.910%) | 189 / 189 / 0 (0.000%) |
| java-checks/src/main/java/org/sonar/java/checks/ArrayForVarArgCheck.java | 205 / 106 / 99 (48.293%) | 205 / 205 / 0 (0.000%) | 205 / 106 / 99 (48.293%) | 205 / 205 / 0 (0.000%) |
| java-checks/src/main/java/org/sonar/java/checks/ArrayHashCodeAndToStringCheck.java | 117 / 55 / 62 (52.991%) | 117 / 117 / 0 (0.000%) | 117 / 55 / 62 (52.991%) | 117 / 117 / 0 (0.000%) |
| java-checks/src/main/java/org/sonar/java/checks/ArraysAsListOfPrimitiveToStreamCheck.java | 106 / 44 / 62 (58.491%) | 106 / 106 / 0 (0.000%) | 106 / 44 / 62 (58.491%) | 106 / 106 / 0 (0.000%) |
| java-checks/src/main/java/org/sonar/java/checks/ArraysFillIncompatibleTypeCheck.java | 255 / 145 / 110 (43.137%) | 255 / 255 / 0 (0.000%) | 255 / 145 / 110 (43.137%) | 255 / 255 / 0 (0.000%) |
| java-checks/src/main/java/org/sonar/java/checks/AssertOnBooleanVariableCheck.java | 57 / 37 / 20 (35.088%) | 57 / 57 / 0 (0.000%) | 57 / 37 / 20 (35.088%) | 57 / 57 / 0 (0.000%) |
| java-checks/src/main/java/org/sonar/java/checks/AssertThrowsInsteadOfTryCatchFailCheck.java | 316 / 161 / 155 (49.051%) | 316 / 316 / 0 (0.000%) | 316 / 161 / 155 (49.051%) | 316 / 316 / 0 (0.000%) |
| java-checks/src/main/java/org/sonar/java/checks/AssertionsInProductionCodeCheck.java | 107 / 60 / 47 (43.925%) | 107 / 107 / 0 (0.000%) | 107 / 60 / 47 (43.925%) | 107 / 107 / 0 (0.000%) |
| java-checks/src/main/java/org/sonar/java/checks/AssertsOnParametersOfPublicMethodCheck.java | 91 / 42 / 49 (53.846%) | 91 / 91 / 0 (0.000%) | 91 / 42 / 49 (53.846%) | 91 / 91 / 0 (0.000%) |
| java-checks/src/main/java/org/sonar/java/checks/AssignmentInSubExpressionCheck.java | 206 / 114 / 92 (44.660%) | 206 / 206 / 0 (0.000%) | 206 / 114 / 92 (44.660%) | 206 / 206 / 0 (0.000%) |
| java-checks/src/main/java/org/sonar/java/checks/AtLeastOneConstructorCheck.java | 136 / 82 / 54 (39.706%) | 136 / 136 / 0 (0.000%) | 136 / 82 / 54 (39.706%) | 136 / 136 / 0 (0.000%) |
| java-checks/src/main/java/org/sonar/java/checks/AvoidHighFrameratesOnMobileCheck.java | 75 / 22 / 53 (70.667%) | 75 / 75 / 0 (0.000%) | 75 / 22 / 53 (70.667%) | 75 / 75 / 0 (0.000%) |
| java-checks/src/main/java/org/sonar/java/checks/BareDotRegexpCheck.java | 122 / 58 / 64 (52.459%) | 122 / 122 / 0 (0.000%) | 122 / 58 / 64 (52.459%) | 122 / 122 / 0 (0.000%) |
| java-checks/src/main/java/org/sonar/java/checks/BasicAuthCheck.java | 109 / 47 / 62 (56.881%) | 109 / 109 / 0 (0.000%) | 109 / 47 / 62 (56.881%) | 109 / 109 / 0 (0.000%) |
| java-checks/src/main/java/org/sonar/java/checks/BatchSQLStatementsCheck.java | 128 / 57 / 71 (55.469%) | 128 / 128 / 0 (0.000%) | 128 / 57 / 71 (55.469%) | 128 / 128 / 0 (0.000%) |
| java-checks/src/main/java/org/sonar/java/checks/BeanValidationConstraintOnStaticFieldCheck.java | 62 / 38 / 24 (38.710%) | 62 / 62 / 0 (0.000%) | 62 / 38 / 24 (38.710%) | 62 / 62 / 0 (0.000%) |
| java-checks/src/main/java/org/sonar/java/checks/BigDecimalDoubleConstructorCheck.java | 130 / 64 / 66 (50.769%) | 130 / 130 / 0 (0.000%) | 130 / 64 / 66 (50.769%) | 130 / 130 / 0 (0.000%) |
| java-checks/src/main/java/org/sonar/java/checks/BigDecimalEqualsCheck.java | 211 / 110 / 101 (47.867%) | 211 / 211 / 0 (0.000%) | 211 / 110 / 101 (47.867%) | 211 / 211 / 0 (0.000%) |
| java-checks/src/main/java/org/sonar/java/checks/BitwiseAndWithZeroCheck.java | 65 / 38 / 27 (41.538%) | 65 / 65 / 0 (0.000%) | 65 / 38 / 27 (41.538%) | 65 / 65 / 0 (0.000%) |
| java-checks/src/main/java/org/sonar/java/checks/BlockingOperationsInVirtualThreadsCheck.java | 168 / 75 / 93 (55.357%) | 168 / 168 / 0 (0.000%) | 168 / 75 / 93 (55.357%) | 168 / 168 / 0 (0.000%) |
| java-checks/src/main/java/org/sonar/java/checks/BluetoothLowPowerModeCheck.java | 76 / 40 / 36 (47.368%) | 76 / 76 / 0 (0.000%) | 76 / 40 / 36 (47.368%) | 76 / 76 / 0 (0.000%) |
| java-checks/src/main/java/org/sonar/java/checks/BooleanInversionCheck.java | 74 / 37 / 37 (50.000%) | 74 / 74 / 0 (0.000%) | 74 / 37 / 37 (50.000%) | 74 / 74 / 0 (0.000%) |
| java-checks/src/main/java/org/sonar/java/checks/BooleanLiteralCheck.java | 698 / 402 / 296 (42.407%) | 698 / 698 / 0 (0.000%) | 698 / 402 / 296 (42.407%) | 698 / 698 / 0 (0.000%) |
| java-checks/src/main/java/org/sonar/java/checks/BooleanMethodReturnCheck.java | 68 / 38 / 30 (44.118%) | 68 / 68 / 0 (0.000%) | 68 / 38 / 30 (44.118%) | 68 / 68 / 0 (0.000%) |
| java-checks/src/main/java/org/sonar/java/checks/BoxedBooleanExpressionsCheck.java | 535 / 258 / 277 (51.776%) | 535 / 535 / 0 (0.000%) | 535 / 258 / 277 (51.776%) | 535 / 535 / 0 (0.000%) |
| java-checks/src/main/java/org/sonar/java/checks/BufferedReaderBoilerplateCheck.java | 77 / 37 / 40 (51.948%) | 77 / 77 / 0 (0.000%) | 77 / 37 / 40 (51.948%) | 77 / 77 / 0 (0.000%) |
| java-checks/src/main/java/org/sonar/java/checks/CORSCheck.java | 292 / 136 / 156 (53.425%) | 292 / 292 / 0 (0.000%) | 292 / 136 / 156 (53.425%) | 292 / 292 / 0 (0.000%) |
| java-checks/src/main/java/org/sonar/java/checks/CallOuterPrivateMethodCheck.java | 290 / 140 / 150 (51.724%) | 290 / 290 / 0 (0.000%) | 290 / 140 / 150 (51.724%) | 290 / 290 / 0 (0.000%) |
| java-checks/src/main/java/org/sonar/java/checks/CallSuperMethodFromInnerClassCheck.java | 162 / 88 / 74 (45.679%) | 162 / 162 / 0 (0.000%) | 162 / 88 / 74 (45.679%) | 162 / 162 / 0 (0.000%) |
| java-checks/src/main/java/org/sonar/java/checks/CallToDeprecatedCodeMarkedForRemovalCheck.java | 54 / 33 / 21 (38.889%) | 54 / 54 / 0 (0.000%) | 54 / 33 / 21 (38.889%) | 54 / 54 / 0 (0.000%) |
| java-checks/src/main/java/org/sonar/java/checks/CallToDeprecatedMethodCheck.java | 90 / 48 / 42 (46.667%) | 90 / 90 / 0 (0.000%) | 90 / 48 / 42 (46.667%) | 90 / 90 / 0 (0.000%) |
| java-checks/src/main/java/org/sonar/java/checks/CallToFileDeleteOnExitMethodCheck.java | 28 / 16 / 12 (42.857%) | 28 / 28 / 0 (0.000%) | 28 / 16 / 12 (42.857%) | 28 / 28 / 0 (0.000%) |
| java-checks/src/main/java/org/sonar/java/checks/CaseInsensitiveComparisonCheck.java | 88 / 47 / 41 (46.591%) | 88 / 88 / 0 (0.000%) | 88 / 47 / 41 (46.591%) | 88 / 88 / 0 (0.000%) |
| java-checks/src/main/java/org/sonar/java/checks/CastArithmeticOperandCheck.java | 350 / 181 / 169 (48.286%) | 350 / 350 / 0 (0.000%) | 350 / 181 / 169 (48.286%) | 350 / 350 / 0 (0.000%) |
| java-checks/src/main/java/org/sonar/java/checks/CastDoubleToFloatCheck.java | 146 / 90 / 56 (38.356%) | 146 / 146 / 0 (0.000%) | 146 / 90 / 56 (38.356%) | 146 / 146 / 0 (0.000%) |
| java-checks/src/main/java/org/sonar/java/checks/CatchExceptionCheck.java | 138 / 90 / 48 (34.783%) | 138 / 138 / 0 (0.000%) | 138 / 90 / 48 (34.783%) | 138 / 138 / 0 (0.000%) |
| java-checks/src/main/java/org/sonar/java/checks/CatchIllegalMonitorStateExceptionCheck.java | 54 / 31 / 23 (42.593%) | 54 / 54 / 0 (0.000%) | 54 / 31 / 23 (42.593%) | 54 / 54 / 0 (0.000%) |
| java-checks/src/main/java/org/sonar/java/checks/CatchNPECheck.java | 119 / 68 / 51 (42.857%) | 119 / 119 / 0 (0.000%) | 119 / 68 / 51 (42.857%) | 119 / 119 / 0 (0.000%) |
| java-checks/src/main/java/org/sonar/java/checks/CatchOfThrowableOrErrorCheck.java | 219 / 133 / 86 (39.269%) | 219 / 219 / 0 (0.000%) | 219 / 133 / 86 (39.269%) | 219 / 219 / 0 (0.000%) |
| java-checks/src/main/java/org/sonar/java/checks/CatchRethrowingCheck.java | 67 / 30 / 37 (55.224%) | 67 / 67 / 0 (0.000%) | 67 / 30 / 37 (55.224%) | 67 / 67 / 0 (0.000%) |
| java-checks/src/main/java/org/sonar/java/checks/CatchUsesExceptionWithContextCheck.java | 677 / 391 / 286 (42.245%) | 677 / 677 / 0 (0.000%) | 677 / 391 / 286 (42.245%) | 677 / 677 / 0 (0.000%) |
| java-checks/src/main/java/org/sonar/java/checks/ChangeMethodContractCheck.java | 241 / 148 / 93 (38.589%) | 241 / 241 / 0 (0.000%) | 241 / 148 / 93 (38.589%) | 241 / 241 / 0 (0.000%) |
| java-checks/src/main/java/org/sonar/java/checks/ChildClassShadowFieldCheck.java | 123 / 60 / 63 (51.220%) | 123 / 123 / 0 (0.000%) | 123 / 60 / 63 (51.220%) | 123 / 123 / 0 (0.000%) |
| java-checks/src/main/java/org/sonar/java/checks/ClassBuilderWithMethodCheck.java | 103 / 43 / 60 (58.252%) | 103 / 103 / 0 (0.000%) | 103 / 43 / 60 (58.252%) | 103 / 103 / 0 (0.000%) |
| java-checks/src/main/java/org/sonar/java/checks/ClassComparedByNameCheck.java | 99 / 50 / 49 (49.495%) | 99 / 99 / 0 (0.000%) | 99 / 50 / 49 (49.495%) | 99 / 99 / 0 (0.000%) |
| java-checks/src/main/java/org/sonar/java/checks/ClassFieldCountCheck.java | 100 / 63 / 37 (37.000%) | 100 / 100 / 0 (0.000%) | 100 / 63 / 37 (37.000%) | 100 / 100 / 0 (0.000%) |
| java-checks/src/main/java/org/sonar/java/checks/ClassNameInClassTransformCheck.java | 225 / 79 / 146 (64.889%) | 225 / 225 / 0 (0.000%) | 225 / 79 / 146 (64.889%) | 225 / 225 / 0 (0.000%) |
| java-checks/src/main/java/org/sonar/java/checks/ClassVariableVisibilityCheck.java | 100 / 63 / 37 (37.000%) | 100 / 100 / 0 (0.000%) | 100 / 63 / 37 (37.000%) | 100 / 100 / 0 (0.000%) |
| java-checks/src/main/java/org/sonar/java/checks/ClassWithOnlyStaticMethodsInstantiationCheck.java | 165 / 81 / 84 (50.909%) | 165 / 165 / 0 (0.000%) | 165 / 81 / 84 (50.909%) | 165 / 165 / 0 (0.000%) |
| java-checks/src/main/java/org/sonar/java/checks/ClassWithoutHashCodeInHashStructureCheck.java | 95 / 42 / 53 (55.789%) | 95 / 95 / 0 (0.000%) | 95 / 42 / 53 (55.789%) | 95 / 95 / 0 (0.000%) |
| java-checks/src/main/java/org/sonar/java/checks/CloneMethodCallsSuperCloneCheck.java | 103 / 61 / 42 (40.777%) | 103 / 103 / 0 (0.000%) | 103 / 61 / 42 (40.777%) | 103 / 103 / 0 (0.000%) |
| java-checks/src/main/java/org/sonar/java/checks/CloneOverrideCheck.java | 72 / 38 / 34 (47.222%) | 72 / 72 / 0 (0.000%) | 72 / 38 / 34 (47.222%) | 72 / 72 / 0 (0.000%) |
| java-checks/src/main/java/org/sonar/java/checks/CloneableImplementingCloneCheck.java | 70 / 32 / 38 (54.286%) | 70 / 70 / 0 (0.000%) | 70 / 32 / 38 (54.286%) | 70 / 70 / 0 (0.000%) |
| java-checks/src/main/java/org/sonar/java/checks/CognitiveComplexityMethodCheck.java | 77 / 49 / 28 (36.364%) | 77 / 77 / 0 (0.000%) | 77 / 49 / 28 (36.364%) | 77 / 77 / 0 (0.000%) |
| java-checks/src/main/java/org/sonar/java/checks/CollapsibleIfCandidateCheck.java | 151 / 72 / 79 (52.318%) | 151 / 151 / 0 (0.000%) | 151 / 72 / 79 (52.318%) | 151 / 151 / 0 (0.000%) |
| java-checks/src/main/java/org/sonar/java/checks/CollectInsteadOfForeachCheck.java | 194 / 100 / 94 (48.454%) | 194 / 194 / 0 (0.000%) | 194 / 100 / 94 (48.454%) | 194 / 194 / 0 (0.000%) |
| java-checks/src/main/java/org/sonar/java/checks/CollectionCallingItselfCheck.java | 111 / 60 / 51 (45.946%) | 111 / 111 / 0 (0.000%) | 111 / 60 / 51 (45.946%) | 111 / 111 / 0 (0.000%) |
| java-checks/src/main/java/org/sonar/java/checks/CollectionConstructorReferenceCheck.java | 49 / 27 / 22 (44.898%) | 49 / 49 / 0 (0.000%) | 49 / 27 / 22 (44.898%) | 49 / 49 / 0 (0.000%) |
| java-checks/src/main/java/org/sonar/java/checks/CollectionImplementationReferencedCheck.java | 534 / 392 / 142 (26.592%) | 534 / 534 / 0 (0.000%) | 534 / 392 / 142 (26.592%) | 534 / 534 / 0 (0.000%) |
| java-checks/src/main/java/org/sonar/java/checks/CollectionInappropriateCallsCheck.java | 505 / 378 / 127 (25.149%) | 505 / 505 / 0 (0.000%) | 505 / 378 / 127 (25.149%) | 505 / 505 / 0 (0.000%) |
| java-checks/src/main/java/org/sonar/java/checks/CollectionIsEmptyCheck.java | 373 / 201 / 172 (46.113%) | 373 / 373 / 0 (0.000%) | 373 / 201 / 172 (46.113%) | 373 / 373 / 0 (0.000%) |
| java-checks/src/main/java/org/sonar/java/checks/CollectionMethodsWithLinearComplexityCheck.java | 341 / 180 / 161 (47.214%) | 341 / 341 / 0 (0.000%) | 341 / 180 / 161 (47.214%) | 341 / 341 / 0 (0.000%) |
| java-checks/src/main/java/org/sonar/java/checks/CollectionSizeAndArrayLengthCheck.java | 216 / 111 / 105 (48.611%) | 216 / 216 / 0 (0.000%) | 216 / 111 / 105 (48.611%) | 216 / 216 / 0 (0.000%) |
| java-checks/src/main/java/org/sonar/java/checks/CollectionsEmptyConstantsCheck.java | 69 / 51 / 18 (26.087%) | 69 / 69 / 0 (0.000%) | 69 / 51 / 18 (26.087%) | 69 / 69 / 0 (0.000%) |
| java-checks/src/main/java/org/sonar/java/checks/CollectionsSortCheck.java | 126 / 51 / 75 (59.524%) | 126 / 126 / 0 (0.000%) | 126 / 51 / 75 (59.524%) | 126 / 126 / 0 (0.000%) |
| java-checks/src/main/java/org/sonar/java/checks/CollectorsToListCheck.java | 255 / 119 / 136 (53.333%) | 255 / 255 / 0 (0.000%) | 255 / 119 / 136 (53.333%) | 255 / 255 / 0 (0.000%) |
| java-checks/src/main/java/org/sonar/java/checks/CombineCatchCheck.java | 252 / 119 / 133 (52.778%) | 252 / 252 / 0 (0.000%) | 252 / 119 / 133 (52.778%) | 252 / 252 / 0 (0.000%) |
| java-checks/src/main/java/org/sonar/java/checks/CommentContainsPatternChecker.java | 98 / 91 / 7 (7.143%) | 98 / 98 / 0 (0.000%) | 98 / 91 / 7 (7.143%) | 98 / 98 / 0 (0.000%) |
| java-checks/src/main/java/org/sonar/java/checks/CommentRegularExpressionCheck.java | 77 / 63 / 14 (18.182%) | 77 / 77 / 0 (0.000%) | 77 / 63 / 14 (18.182%) | 77 / 77 / 0 (0.000%) |
| java-checks/src/main/java/org/sonar/java/checks/CommentedOutCodeLineCheck.java | 273 / 195 / 78 (28.571%) | 273 / 273 / 0 (0.000%) | 273 / 196 / 77 (28.205%) | 273 / 273 / 0 (0.000%) |
| java-checks/src/main/java/org/sonar/java/checks/CommentsMustStartWithCorrectNumberOfSlashesCheck.java | 215 / 149 / 66 (30.698%) | 215 / 215 / 0 (0.000%) | 215 / 149 / 66 (30.698%) | 215 / 215 / 0 (0.000%) |
| java-checks/src/main/java/org/sonar/java/checks/CompareObjectWithEqualsCheck.java | 247 / 152 / 95 (38.462%) | 247 / 247 / 0 (0.000%) | 247 / 152 / 95 (38.462%) | 247 / 247 / 0 (0.000%) |
| java-checks/src/main/java/org/sonar/java/checks/CompareStringsBoxedTypesWithEqualsCheck.java | 234 / 105 / 129 (55.128%) | 234 / 234 / 0 (0.000%) | 234 / 105 / 129 (55.128%) | 234 / 234 / 0 (0.000%) |
| java-checks/src/main/java/org/sonar/java/checks/CompareToNotOverloadedCheck.java | 76 / 30 / 46 (60.526%) | 76 / 76 / 0 (0.000%) | 76 / 30 / 46 (60.526%) | 76 / 76 / 0 (0.000%) |
| java-checks/src/main/java/org/sonar/java/checks/CompareToResultTestCheck.java | 300 / 157 / 143 (47.667%) | 300 / 300 / 0 (0.000%) | 300 / 157 / 143 (47.667%) | 300 / 300 / 0 (0.000%) |
| java-checks/src/main/java/org/sonar/java/checks/CompareToReturnValueCheck.java | 113 / 66 / 47 (41.593%) | 113 / 113 / 0 (0.000%) | 113 / 66 / 47 (41.593%) | 113 / 113 / 0 (0.000%) |
| java-checks/src/main/java/org/sonar/java/checks/CompareWithEqualsVisitor.java | 94 / 58 / 36 (38.298%) | 94 / 94 / 0 (0.000%) | 94 / 58 / 36 (38.298%) | 94 / 94 / 0 (0.000%) |
| java-checks/src/main/java/org/sonar/java/checks/ConcatenationWithStringValueOfCheck.java | 180 / 86 / 94 (52.222%) | 180 / 180 / 0 (0.000%) | 180 / 86 / 94 (52.222%) | 180 / 180 / 0 (0.000%) |
| java-checks/src/main/java/org/sonar/java/checks/ConditionalOnNewLineCheck.java | 79 / 47 / 32 (40.506%) | 79 / 79 / 0 (0.000%) | 79 / 47 / 32 (40.506%) | 79 / 79 / 0 (0.000%) |
| java-checks/src/main/java/org/sonar/java/checks/ConditionalRuleCacheUtils.java | 268 / 268 / 0 (0.000%) | 268 / 268 / 0 (0.000%) | 268 / 268 / 0 (0.000%) | 268 / 268 / 0 (0.000%) |
| java-checks/src/main/java/org/sonar/java/checks/ConfigurationBeanNamesCheck.java | 54 / 32 / 22 (40.741%) | 54 / 54 / 0 (0.000%) | 54 / 32 / 22 (40.741%) | 54 / 54 / 0 (0.000%) |
| java-checks/src/main/java/org/sonar/java/checks/ConfusingOverloadCheck.java | 250 / 144 / 106 (42.400%) | 250 / 250 / 0 (0.000%) | 250 / 144 / 106 (42.400%) | 250 / 250 / 0 (0.000%) |
| java-checks/src/main/java/org/sonar/java/checks/ConfusingVarargCheck.java | 195 / 88 / 107 (54.872%) | 195 / 195 / 0 (0.000%) | 195 / 88 / 107 (54.872%) | 195 / 195 / 0 (0.000%) |
| java-checks/src/main/java/org/sonar/java/checks/ConstantMathCheck.java | 368 / 190 / 178 (48.370%) | 368 / 368 / 0 (0.000%) | 368 / 190 / 178 (48.370%) | 368 / 368 / 0 (0.000%) |
| java-checks/src/main/java/org/sonar/java/checks/ConstantMethodCheck.java | 125 / 57 / 68 (54.400%) | 125 / 125 / 0 (0.000%) | 125 / 57 / 68 (54.400%) | 125 / 125 / 0 (0.000%) |
| java-checks/src/main/java/org/sonar/java/checks/ConstantsShouldBeStaticFinalCheck.java | 191 / 104 / 87 (45.550%) | 191 / 191 / 0 (0.000%) | 191 / 104 / 87 (45.550%) | 191 / 191 / 0 (0.000%) |
| java-checks/src/main/java/org/sonar/java/checks/ConstructorCallingOverridableCheck.java | 169 / 98 / 71 (42.012%) | 169 / 169 / 0 (0.000%) | 169 / 98 / 71 (42.012%) | 169 / 169 / 0 (0.000%) |
| java-checks/src/main/java/org/sonar/java/checks/ConstructorInjectionCheck.java | 82 / 29 / 53 (64.634%) | 82 / 82 / 0 (0.000%) | 82 / 29 / 53 (64.634%) | 82 / 82 / 0 (0.000%) |
| java-checks/src/main/java/org/sonar/java/checks/ConstructorsShouldNotAccessUninitializedValuesCheck.java | 328 / 173 / 155 (47.256%) | 328 / 328 / 0 (0.000%) | 328 / 173 / 155 (47.256%) | 328 / 328 / 0 (0.000%) |
| java-checks/src/main/java/org/sonar/java/checks/ContinueInLoopCheck.java | 24 / 15 / 9 (37.500%) | 24 / 24 / 0 (0.000%) | 24 / 15 / 9 (37.500%) | 24 / 24 / 0 (0.000%) |
| java-checks/src/main/java/org/sonar/java/checks/ControlCharacterInLiteralCheck.java | 92 / 67 / 25 (27.174%) | 92 / 92 / 0 (0.000%) | 92 / 67 / 25 (27.174%) | 92 / 92 / 0 (0.000%) |
| java-checks/src/main/java/org/sonar/java/checks/CopyConstructorMissesFieldCheck.java | 591 / 313 / 278 (47.039%) | 591 / 591 / 0 (0.000%) | 591 / 313 / 278 (47.039%) | 591 / 591 / 0 (0.000%) |
| java-checks/src/main/java/org/sonar/java/checks/CounterModeIVShouldNotBeReusedCheck.java | 223 / 111 / 112 (50.224%) | 223 / 223 / 0 (0.000%) | 223 / 111 / 112 (50.224%) | 223 / 223 / 0 (0.000%) |
| java-checks/src/main/java/org/sonar/java/checks/CredentialsProviderUnremovableCheck.java | 68 / 45 / 23 (33.824%) | 68 / 68 / 0 (0.000%) | 68 / 45 / 23 (33.824%) | 68 / 68 / 0 (0.000%) |
| java-checks/src/main/java/org/sonar/java/checks/CustomCryptographicAlgorithmCheck.java | 53 / 33 / 20 (37.736%) | 53 / 53 / 0 (0.000%) | 53 / 33 / 20 (37.736%) | 53 / 53 / 0 (0.000%) |
| java-checks/src/main/java/org/sonar/java/checks/DanglingElseStatementsCheck.java | 49 / 29 / 20 (40.816%) | 49 / 49 / 0 (0.000%) | 49 / 29 / 20 (40.816%) | 49 / 49 / 0 (0.000%) |
| java-checks/src/main/java/org/sonar/java/checks/DanglingJavadocCheck.java | 29 / 18 / 11 (37.931%) | 29 / 29 / 0 (0.000%) | 29 / 18 / 11 (37.931%) | 29 / 29 / 0 (0.000%) |
| java-checks/src/main/java/org/sonar/java/checks/DateAndTimesCheck.java | 129 / 82 / 47 (36.434%) | 129 / 129 / 0 (0.000%) | 129 / 82 / 47 (36.434%) | 129 / 129 / 0 (0.000%) |
| java-checks/src/main/java/org/sonar/java/checks/DateEnumsCheck.java | 803 / 463 / 340 (42.341%) | 803 / 803 / 0 (0.000%) | 803 / 530 / 273 (33.998%) | 803 / 803 / 0 (0.000%) |
| java-checks/src/main/java/org/sonar/java/checks/DateFormatWeekYearCheck.java | 183 / 115 / 68 (37.158%) | 183 / 183 / 0 (0.000%) | 183 / 115 / 68 (37.158%) | 183 / 183 / 0 (0.000%) |
| java-checks/src/main/java/org/sonar/java/checks/DateTimeConversionsCheck.java | 129 / 78 / 51 (39.535%) | 129 / 129 / 0 (0.000%) | 129 / 78 / 51 (39.535%) | 129 / 129 / 0 (0.000%) |
| java-checks/src/main/java/org/sonar/java/checks/DateTimeDurationCheck.java | 114 / 71 / 43 (37.719%) | 114 / 114 / 0 (0.000%) | 114 / 71 / 43 (37.719%) | 114 / 114 / 0 (0.000%) |
| java-checks/src/main/java/org/sonar/java/checks/DateTimeFormatterMismatchCheck.java | 268 / 170 / 98 (36.567%) | 268 / 268 / 0 (0.000%) | 268 / 170 / 98 (36.567%) | 268 / 268 / 0 (0.000%) |
| java-checks/src/main/java/org/sonar/java/checks/DateUtilsTruncateCheck.java | 51 / 22 / 29 (56.863%) | 51 / 51 / 0 (0.000%) | 51 / 22 / 29 (56.863%) | 51 / 51 / 0 (0.000%) |
| java-checks/src/main/java/org/sonar/java/checks/DeadStoreCheck.java | 600 / 312 / 288 (48.000%) | 600 / 600 / 0 (0.000%) | 600 / 312 / 288 (48.000%) | 600 / 600 / 0 (0.000%) |
| java-checks/src/main/java/org/sonar/java/checks/DefaultEncodingUsageCheck.java | 517 / 295 / 222 (42.940%) | 517 / 517 / 0 (0.000%) | 517 / 295 / 222 (42.940%) | 517 / 517 / 0 (0.000%) |
| java-checks/src/main/java/org/sonar/java/checks/DefaultFinisherInGathererFactoryCheck.java | 97 / 36 / 61 (62.887%) | 97 / 97 / 0 (0.000%) | 97 / 36 / 61 (62.887%) | 97 / 97 / 0 (0.000%) |
| java-checks/src/main/java/org/sonar/java/checks/DefaultInitializedFieldCheck.java | 162 / 64 / 98 (60.494%) | 162 / 162 / 0 (0.000%) | 162 / 64 / 98 (60.494%) | 162 / 162 / 0 (0.000%) |
| java-checks/src/main/java/org/sonar/java/checks/DefaultPackageCheck.java | 43 / 25 / 18 (41.860%) | 43 / 43 / 0 (0.000%) | 43 / 25 / 18 (41.860%) | 43 / 43 / 0 (0.000%) |
| java-checks/src/main/java/org/sonar/java/checks/DeprecatedArgumentsCheck.java | 35 / 25 / 10 (28.571%) | 35 / 35 / 0 (0.000%) | 35 / 25 / 10 (28.571%) | 35 / 35 / 0 (0.000%) |
| java-checks/src/main/java/org/sonar/java/checks/DeprecatedTagPresenceCheck.java | 50 / 35 / 15 (30.000%) | 50 / 50 / 0 (0.000%) | 50 / 35 / 15 (30.000%) | 50 / 50 / 0 (0.000%) |
| java-checks/src/main/java/org/sonar/java/checks/DepthOfInheritanceTreeCheck.java | 156 / 115 / 41 (26.282%) | 156 / 156 / 0 (0.000%) | 156 / 117 / 39 (25.000%) | 156 / 156 / 0 (0.000%) |
| java-checks/src/main/java/org/sonar/java/checks/DiamondOperatorCheck.java | 367 / 191 / 176 (47.956%) | 367 / 367 / 0 (0.000%) | 367 / 191 / 176 (47.956%) | 367 / 367 / 0 (0.000%) |
| java-checks/src/main/java/org/sonar/java/checks/DisallowedClassCheck.java | 237 / 152 / 85 (35.865%) | 237 / 237 / 0 (0.000%) | 237 / 152 / 85 (35.865%) | 237 / 237 / 0 (0.000%) |
| java-checks/src/main/java/org/sonar/java/checks/DisallowedConstructorCheck.java | 96 / 77 / 19 (19.792%) | 96 / 96 / 0 (0.000%) | 96 / 77 / 19 (19.792%) | 96 / 96 / 0 (0.000%) |
| java-checks/src/main/java/org/sonar/java/checks/DisallowedMethodCheck.java | 124 / 99 / 25 (20.161%) | 124 / 124 / 0 (0.000%) | 124 / 99 / 25 (20.161%) | 124 / 124 / 0 (0.000%) |
| java-checks/src/main/java/org/sonar/java/checks/DisallowedThreadGroupCheck.java | 123 / 86 / 37 (30.081%) | 123 / 123 / 0 (0.000%) | 123 / 86 / 37 (30.081%) | 123 / 123 / 0 (0.000%) |
| java-checks/src/main/java/org/sonar/java/checks/DoubleBraceInitializationCheck.java | 43 / 22 / 21 (48.837%) | 43 / 43 / 0 (0.000%) | 43 / 22 / 21 (48.837%) | 43 / 43 / 0 (0.000%) |
| java-checks/src/main/java/org/sonar/java/checks/DoubleCheckedLockingAssignmentCheck.java | 301 / 154 / 147 (48.837%) | 301 / 301 / 0 (0.000%) | 301 / 154 / 147 (48.837%) | 301 / 301 / 0 (0.000%) |
| java-checks/src/main/java/org/sonar/java/checks/DoublePrefixOperatorCheck.java | 116 / 49 / 67 (57.759%) | 116 / 116 / 0 (0.000%) | 116 / 49 / 67 (57.759%) | 116 / 116 / 0 (0.000%) |
| java-checks/src/main/java/org/sonar/java/checks/DuplicateConditionIfElseIfCheck.java | 120 / 66 / 54 (45.000%) | 120 / 120 / 0 (0.000%) | 120 / 66 / 54 (45.000%) | 120 / 120 / 0 (0.000%) |
| java-checks/src/main/java/org/sonar/java/checks/DuplicateImmutableCollectionArgumentsCheck.java | 359 / 190 / 169 (47.075%) | 359 / 359 / 0 (0.000%) | 359 / 190 / 169 (47.075%) | 359 / 359 / 0 (0.000%) |
| java-checks/src/main/java/org/sonar/java/checks/DurationGetTemporalUnitCheck.java | 108 / 68 / 40 (37.037%) | 108 / 108 / 0 (0.000%) | 108 / 68 / 40 (37.037%) | 108 / 108 / 0 (0.000%) |
| java-checks/src/main/java/org/sonar/java/checks/DurationTimeUnitAgreementCheck.java | 277 / 129 / 148 (53.430%) | 277 / 277 / 0 (0.000%) | 277 / 129 / 148 (53.430%) | 277 / 277 / 0 (0.000%) |
| java-checks/src/main/java/org/sonar/java/checks/DynamicClassLoadCheck.java | 42 / 17 / 25 (59.524%) | 42 / 42 / 0 (0.000%) | 42 / 17 / 25 (59.524%) | 42 / 42 / 0 (0.000%) |
| java-checks/src/main/java/org/sonar/java/checks/EmptyArchiveEntryCheck.java | 406 / 210 / 196 (48.276%) | 406 / 406 / 0 (0.000%) | 406 / 210 / 196 (48.276%) | 406 / 406 / 0 (0.000%) |
| java-checks/src/main/java/org/sonar/java/checks/EmptyBlockCheck.java | 130 / 56 / 74 (56.923%) | 130 / 130 / 0 (0.000%) | 130 / 56 / 74 (56.923%) | 130 / 130 / 0 (0.000%) |
| java-checks/src/main/java/org/sonar/java/checks/EmptyClassCheck.java | 68 / 33 / 35 (51.471%) | 68 / 68 / 0 (0.000%) | 68 / 33 / 35 (51.471%) | 68 / 68 / 0 (0.000%) |
| java-checks/src/main/java/org/sonar/java/checks/EmptyFileCheck.java | 31 / 21 / 10 (32.258%) | 31 / 31 / 0 (0.000%) | 31 / 21 / 10 (32.258%) | 31 / 31 / 0 (0.000%) |
| java-checks/src/main/java/org/sonar/java/checks/EmptyMethodsCheck.java | 287 / 131 / 156 (54.355%) | 287 / 287 / 0 (0.000%) | 287 / 131 / 156 (54.355%) | 287 / 287 / 0 (0.000%) |
| java-checks/src/main/java/org/sonar/java/checks/EmptyStatementUsageCheck.java | 130 / 59 / 71 (54.615%) | 130 / 130 / 0 (0.000%) | 130 / 59 / 71 (54.615%) | 130 / 130 / 0 (0.000%) |
| java-checks/src/main/java/org/sonar/java/checks/EntityManagerMergeUnusedResultCheck.java | 67 / 28 / 39 (58.209%) | 67 / 67 / 0 (0.000%) | 67 / 28 / 39 (58.209%) | 67 / 67 / 0 (0.000%) |
| java-checks/src/main/java/org/sonar/java/checks/EnumEqualCheck.java | 49 / 23 / 26 (53.061%) | 49 / 49 / 0 (0.000%) | 49 / 23 / 26 (53.061%) | 49 / 49 / 0 (0.000%) |
| java-checks/src/main/java/org/sonar/java/checks/EnumMapCheck.java | 172 / 92 / 80 (46.512%) | 172 / 172 / 0 (0.000%) | 172 / 92 / 80 (46.512%) | 172 / 172 / 0 (0.000%) |
| java-checks/src/main/java/org/sonar/java/checks/EnumMutableFieldCheck.java | 170 / 80 / 90 (52.941%) | 170 / 170 / 0 (0.000%) | 170 / 80 / 90 (52.941%) | 170 / 170 / 0 (0.000%) |
| java-checks/src/main/java/org/sonar/java/checks/EnumSetCheck.java | 118 / 47 / 71 (60.169%) | 118 / 118 / 0 (0.000%) | 118 / 47 / 71 (60.169%) | 118 / 118 / 0 (0.000%) |
| java-checks/src/main/java/org/sonar/java/checks/EqualsArgumentTypeCheck.java | 311 / 176 / 135 (43.408%) | 311 / 311 / 0 (0.000%) | 311 / 176 / 135 (43.408%) | 311 / 311 / 0 (0.000%) |
| java-checks/src/main/java/org/sonar/java/checks/EqualsMismatchedMembersCheck.java | 642 / 437 / 205 (31.931%) | 642 / 642 / 0 (0.000%) | 642 / 437 / 205 (31.931%) | 642 / 642 / 0 (0.000%) |
| java-checks/src/main/java/org/sonar/java/checks/EqualsNotOverriddenInSubclassCheck.java | 179 / 94 / 85 (47.486%) | 179 / 179 / 0 (0.000%) | 179 / 94 / 85 (47.486%) | 179 / 179 / 0 (0.000%) |
| java-checks/src/main/java/org/sonar/java/checks/EqualsNotOverriddenWithCompareToCheck.java | 104 / 59 / 45 (43.269%) | 104 / 104 / 0 (0.000%) | 104 / 59 / 45 (43.269%) | 104 / 104 / 0 (0.000%) |
| java-checks/src/main/java/org/sonar/java/checks/EqualsOnAtomicClassCheck.java | 25 / 13 / 12 (48.000%) | 25 / 25 / 0 (0.000%) | 25 / 13 / 12 (48.000%) | 25 / 25 / 0 (0.000%) |
| java-checks/src/main/java/org/sonar/java/checks/EqualsOverriddenWithHashCodeCheck.java | 131 / 68 / 63 (48.092%) | 131 / 131 / 0 (0.000%) | 131 / 68 / 63 (48.092%) | 131 / 131 / 0 (0.000%) |
| java-checks/src/main/java/org/sonar/java/checks/EqualsParametersMarkedNonNullCheck.java | 82 / 34 / 48 (58.537%) | 82 / 82 / 0 (0.000%) | 82 / 34 / 48 (58.537%) | 82 / 82 / 0 (0.000%) |
| java-checks/src/main/java/org/sonar/java/checks/ErrorClassExtendedCheck.java | 33 / 19 / 14 (42.424%) | 33 / 33 / 0 (0.000%) | 33 / 19 / 14 (42.424%) | 33 / 33 / 0 (0.000%) |
| java-checks/src/main/java/org/sonar/java/checks/EscapedUnicodeCharactersCheck.java | 111 / 93 / 18 (16.216%) | 111 / 111 / 0 (0.000%) | 111 / 93 / 18 (16.216%) | 111 / 111 / 0 (0.000%) |
| java-checks/src/main/java/org/sonar/java/checks/ExceptionsShouldBeImmutableCheck.java | 67 / 34 / 33 (49.254%) | 67 / 67 / 0 (0.000%) | 67 / 34 / 33 (49.254%) | 67 / 67 / 0 (0.000%) |
| java-checks/src/main/java/org/sonar/java/checks/ExpressionComplexityCheck.java | 330 / 113 / 217 (65.758%) | 330 / 330 / 0 (0.000%) | 330 / 113 / 217 (65.758%) | 330 / 330 / 0 (0.000%) |
| java-checks/src/main/java/org/sonar/java/checks/FieldModifierCheck.java | 107 / 39 / 68 (63.551%) | 107 / 107 / 0 (0.000%) | 107 / 39 / 68 (63.551%) | 107 / 107 / 0 (0.000%) |
| java-checks/src/main/java/org/sonar/java/checks/FileHeaderCheck.java | 133 / 122 / 11 (8.271%) | 133 / 133 / 0 (0.000%) | 133 / 122 / 11 (8.271%) | 133 / 133 / 0 (0.000%) |
| java-checks/src/main/java/org/sonar/java/checks/FilesExistsJDK8Check.java | 83 / 60 / 23 (27.711%) | 83 / 83 / 0 (0.000%) | 83 / 60 / 23 (27.711%) | 83 / 83 / 0 (0.000%) |
| java-checks/src/main/java/org/sonar/java/checks/FinalClassCheck.java | 125 / 76 / 49 (39.200%) | 125 / 125 / 0 (0.000%) | 125 / 76 / 49 (39.200%) | 125 / 125 / 0 (0.000%) |
| java-checks/src/main/java/org/sonar/java/checks/FinalizeFieldsSetCheck.java | 129 / 73 / 56 (43.411%) | 129 / 129 / 0 (0.000%) | 129 / 73 / 56 (43.411%) | 129 / 129 / 0 (0.000%) |
| java-checks/src/main/java/org/sonar/java/checks/FinalizerAttackCheck.java | 531 / 256 / 275 (51.789%) | 531 / 531 / 0 (0.000%) | 531 / 256 / 275 (51.789%) | 531 / 531 / 0 (0.000%) |
| java-checks/src/main/java/org/sonar/java/checks/FixmeTagPresenceCheck.java | 35 / 24 / 11 (31.429%) | 35 / 35 / 0 (0.000%) | 35 / 24 / 11 (31.429%) | 35 / 35 / 0 (0.000%) |
| java-checks/src/main/java/org/sonar/java/checks/FlexibleConstructorBodyValidationCheck.java | 195 / 109 / 86 (44.103%) | 195 / 195 / 0 (0.000%) | 195 / 109 / 86 (44.103%) | 195 / 195 / 0 (0.000%) |
| java-checks/src/main/java/org/sonar/java/checks/FlexibleConstructorVisitor.java | 85 / 50 / 35 (41.176%) | 85 / 85 / 0 (0.000%) | 85 / 50 / 35 (41.176%) | 85 / 85 / 0 (0.000%) |
| java-checks/src/main/java/org/sonar/java/checks/FloatEqualityCheck.java | 207 / 78 / 129 (62.319%) | 207 / 207 / 0 (0.000%) | 207 / 78 / 129 (62.319%) | 207 / 207 / 0 (0.000%) |
| java-checks/src/main/java/org/sonar/java/checks/FloatingPointComparisonCheck.java | 364 / 156 / 208 (57.143%) | 364 / 364 / 0 (0.000%) | 364 / 156 / 208 (57.143%) | 364 / 364 / 0 (0.000%) |
| java-checks/src/main/java/org/sonar/java/checks/ForLoopCounterChangedCheck.java | 149 / 79 / 70 (46.980%) | 149 / 149 / 0 (0.000%) | 149 / 79 / 70 (46.980%) | 149 / 149 / 0 (0.000%) |
| java-checks/src/main/java/org/sonar/java/checks/ForLoopFalseConditionCheck.java | 170 / 108 / 62 (36.471%) | 170 / 170 / 0 (0.000%) | 170 / 108 / 62 (36.471%) | 170 / 170 / 0 (0.000%) |
| java-checks/src/main/java/org/sonar/java/checks/ForLoopIncrementAndUpdateCheck.java | 401 / 193 / 208 (51.870%) | 401 / 401 / 0 (0.000%) | 401 / 193 / 208 (51.870%) | 401 / 401 / 0 (0.000%) |
| java-checks/src/main/java/org/sonar/java/checks/ForLoopIncrementSignCheck.java | 113 / 65 / 48 (42.478%) | 113 / 113 / 0 (0.000%) | 113 / 65 / 48 (42.478%) | 113 / 113 / 0 (0.000%) |
| java-checks/src/main/java/org/sonar/java/checks/ForLoopStreamSuggestionCheck.java | 416 / 193 / 223 (53.606%) | 416 / 416 / 0 (0.000%) | 416 / 193 / 223 (53.606%) | 416 / 416 / 0 (0.000%) |
| java-checks/src/main/java/org/sonar/java/checks/ForLoopTerminationConditionCheck.java | 223 / 149 / 74 (33.184%) | 223 / 223 / 0 (0.000%) | 223 / 149 / 74 (33.184%) | 223 / 223 / 0 (0.000%) |
| java-checks/src/main/java/org/sonar/java/checks/ForLoopUsedAsWhileLoopCheck.java | 39 / 21 / 18 (46.154%) | 39 / 39 / 0 (0.000%) | 39 / 21 / 18 (46.154%) | 39 / 39 / 0 (0.000%) |
| java-checks/src/main/java/org/sonar/java/checks/ForLoopVariableTypeCheck.java | 170 / 91 / 79 (46.471%) | 170 / 170 / 0 (0.000%) | 170 / 91 / 79 (46.471%) | 170 / 170 / 0 (0.000%) |
| java-checks/src/main/java/org/sonar/java/checks/ForStatelessGatherersOmitInitializerCheck.java | 236 / 140 / 96 (40.678%) | 236 / 236 / 0 (0.000%) | 236 / 140 / 96 (40.678%) | 236 / 236 / 0 (0.000%) |
| java-checks/src/main/java/org/sonar/java/checks/GarbageCollectorCalledCheck.java | 41 / 20 / 21 (51.220%) | 41 / 41 / 0 (0.000%) | 41 / 20 / 21 (51.220%) | 41 / 41 / 0 (0.000%) |
| java-checks/src/main/java/org/sonar/java/checks/GetClassLoaderCheck.java | 25 / 13 / 12 (48.000%) | 25 / 25 / 0 (0.000%) | 25 / 13 / 12 (48.000%) | 25 / 25 / 0 (0.000%) |
| java-checks/src/main/java/org/sonar/java/checks/GetRequestedSessionIdCheck.java | 25 / 13 / 12 (48.000%) | 25 / 25 / 0 (0.000%) | 25 / 13 / 12 (48.000%) | 25 / 25 / 0 (0.000%) |
| java-checks/src/main/java/org/sonar/java/checks/GettersSettersOnRightFieldCheck.java | 237 / 72 / 165 (69.620%) | 237 / 237 / 0 (0.000%) | 237 / 72 / 165 (69.620%) | 237 / 237 / 0 (0.000%) |
| java-checks/src/main/java/org/sonar/java/checks/HardCodedPasswordCheck.java | 209 / 129 / 80 (38.278%) | 209 / 209 / 0 (0.000%) | 209 / 129 / 80 (38.278%) | 209 / 209 / 0 (0.000%) |
| java-checks/src/main/java/org/sonar/java/checks/HardCodedSecretCheck.java | 147 / 92 / 55 (37.415%) | 147 / 147 / 0 (0.000%) | 147 / 92 / 55 (37.415%) | 147 / 147 / 0 (0.000%) |
| java-checks/src/main/java/org/sonar/java/checks/HardcodedIpCheck.java | 256 / 239 / 17 (6.641%) | 256 / 256 / 0 (0.000%) | 256 / 239 / 17 (6.641%) | 256 / 256 / 0 (0.000%) |
| java-checks/src/main/java/org/sonar/java/checks/HardcodedMathConstantCheck.java | 198 / 183 / 15 (7.576%) | 198 / 198 / 0 (0.000%) | 198 / 183 / 15 (7.576%) | 198 / 198 / 0 (0.000%) |
| java-checks/src/main/java/org/sonar/java/checks/HardcodedURICheck.java | 450 / 297 / 153 (34.000%) | 450 / 450 / 0 (0.000%) | 450 / 297 / 153 (34.000%) | 450 / 450 / 0 (0.000%) |
| java-checks/src/main/java/org/sonar/java/checks/HasNextCallingNextCheck.java | 131 / 82 / 49 (37.405%) | 131 / 131 / 0 (0.000%) | 131 / 82 / 49 (37.405%) | 131 / 131 / 0 (0.000%) |
| java-checks/src/main/java/org/sonar/java/checks/HashCodeMismatchedFieldsCheck.java | 542 / 327 / 215 (39.668%) | 542 / 542 / 0 (0.000%) | 542 / 327 / 215 (39.668%) | 542 / 542 / 0 (0.000%) |
| java-checks/src/main/java/org/sonar/java/checks/HiddenFieldCheck.java | 347 / 181 / 166 (47.839%) | 347 / 347 / 0 (0.000%) | 347 / 181 / 166 (47.839%) | 347 / 347 / 0 (0.000%) |
| java-checks/src/main/java/org/sonar/java/checks/IdenticalCasesInSwitchCheck.java | 416 / 211 / 205 (49.279%) | 416 / 416 / 0 (0.000%) | 416 / 211 / 205 (49.279%) | 416 / 416 / 0 (0.000%) |
| java-checks/src/main/java/org/sonar/java/checks/IdenticalOperandOnBinaryExpressionCheck.java | 338 / 131 / 207 (61.243%) | 338 / 338 / 0 (0.000%) | 338 / 131 / 207 (61.243%) | 338 / 338 / 0 (0.000%) |
| java-checks/src/main/java/org/sonar/java/checks/IdentityHashMapBoxedKeyCheck.java | 88 / 49 / 39 (44.318%) | 88 / 88 / 0 (0.000%) | 88 / 49 / 39 (44.318%) | 88 / 88 / 0 (0.000%) |
| java-checks/src/main/java/org/sonar/java/checks/IfElseIfStatementEndsWithElseCheck.java | 47 / 25 / 22 (46.809%) | 47 / 47 / 0 (0.000%) | 47 / 25 / 22 (46.809%) | 47 / 47 / 0 (0.000%) |
| java-checks/src/main/java/org/sonar/java/checks/IgnoredOperationStatusCheck.java | 151 / 35 / 116 (76.821%) | 151 / 151 / 0 (0.000%) | 151 / 35 / 116 (76.821%) | 151 / 151 / 0 (0.000%) |
| java-checks/src/main/java/org/sonar/java/checks/IgnoredReturnValueCheck.java | 331 / 152 / 179 (54.079%) | 331 / 331 / 0 (0.000%) | 331 / 152 / 179 (54.079%) | 331 / 331 / 0 (0.000%) |
| java-checks/src/main/java/org/sonar/java/checks/IgnoredStreamReturnValueCheck.java | 62 / 24 / 38 (61.290%) | 62 / 62 / 0 (0.000%) | 62 / 24 / 38 (61.290%) | 62 / 62 / 0 (0.000%) |
| java-checks/src/main/java/org/sonar/java/checks/ImmediateReverseBoxingCheck.java | 551 / 284 / 267 (48.457%) | 551 / 551 / 0 (0.000%) | 551 / 284 / 267 (48.457%) | 551 / 551 / 0 (0.000%) |
| java-checks/src/main/java/org/sonar/java/checks/ImmediatelyReturnedVariableCheck.java | 182 / 93 / 89 (48.901%) | 182 / 182 / 0 (0.000%) | 182 / 93 / 89 (48.901%) | 182 / 182 / 0 (0.000%) |
| java-checks/src/main/java/org/sonar/java/checks/ImplementsEnumerationCheck.java | 75 / 35 / 40 (53.333%) | 75 / 75 / 0 (0.000%) | 75 / 35 / 40 (53.333%) | 75 / 75 / 0 (0.000%) |
| java-checks/src/main/java/org/sonar/java/checks/ImportDeclarationOrderCheck.java | 276 / 182 / 94 (34.058%) | 276 / 276 / 0 (0.000%) | 276 / 182 / 94 (34.058%) | 276 / 276 / 0 (0.000%) |
| java-checks/src/main/java/org/sonar/java/checks/InappropriateRegexpCheck.java | 66 / 33 / 33 (50.000%) | 66 / 66 / 0 (0.000%) | 66 / 33 / 33 (50.000%) | 66 / 66 / 0 (0.000%) |
| java-checks/src/main/java/org/sonar/java/checks/IncDecOnFloatingPointCheck.java | 81 / 26 / 55 (67.901%) | 81 / 81 / 0 (0.000%) | 81 / 26 / 55 (67.901%) | 81 / 81 / 0 (0.000%) |
| java-checks/src/main/java/org/sonar/java/checks/IncompatibleBitMaskCheck.java | 286 / 189 / 97 (33.916%) | 286 / 286 / 0 (0.000%) | 286 / 189 / 97 (33.916%) | 286 / 286 / 0 (0.000%) |
| java-checks/src/main/java/org/sonar/java/checks/IncorrectOrderOfMembersCheck.java | 93 / 57 / 36 (38.710%) | 93 / 93 / 0 (0.000%) | 93 / 57 / 36 (38.710%) | 93 / 93 / 0 (0.000%) |
| java-checks/src/main/java/org/sonar/java/checks/IncrementDecrementInSubExpressionCheck.java | 97 / 60 / 37 (38.144%) | 97 / 97 / 0 (0.000%) | 97 / 60 / 37 (38.144%) | 97 / 97 / 0 (0.000%) |
| java-checks/src/main/java/org/sonar/java/checks/IndentationAfterConditionalCheck.java | 198 / 106 / 92 (46.465%) | 198 / 198 / 0 (0.000%) | 198 / 106 / 92 (46.465%) | 198 / 198 / 0 (0.000%) |
| java-checks/src/main/java/org/sonar/java/checks/IndentationCheck.java | 543 / 360 / 183 (33.702%) | 543 / 543 / 0 (0.000%) | 543 / 360 / 183 (33.702%) | 543 / 543 / 0 (0.000%) |
| java-checks/src/main/java/org/sonar/java/checks/IndexOfWithPositiveNumberCheck.java | 126 / 63 / 63 (50.000%) | 126 / 126 / 0 (0.000%) | 126 / 63 / 63 (50.000%) | 126 / 126 / 0 (0.000%) |
| java-checks/src/main/java/org/sonar/java/checks/InitializeSubclassFieldsBeforeSuperCheck.java | 344 / 200 / 144 (41.860%) | 344 / 344 / 0 (0.000%) | 344 / 200 / 144 (41.860%) | 344 / 344 / 0 (0.000%) |
| java-checks/src/main/java/org/sonar/java/checks/InnerClassInInterfaceCheck.java | 45 / 22 / 23 (51.111%) | 45 / 45 / 0 (0.000%) | 45 / 22 / 23 (51.111%) | 45 / 45 / 0 (0.000%) |
| java-checks/src/main/java/org/sonar/java/checks/InnerClassOfNonSerializableCheck.java | 14 / 11 / 3 (21.429%) | 14 / 14 / 0 (0.000%) | 14 / 11 / 3 (21.429%) | 14 / 14 / 0 (0.000%) |
| java-checks/src/main/java/org/sonar/java/checks/InnerClassOfSerializableCheck.java | 14 / 11 / 3 (21.429%) | 14 / 14 / 0 (0.000%) | 14 / 11 / 3 (21.429%) | 14 / 14 / 0 (0.000%) |
| java-checks/src/main/java/org/sonar/java/checks/InnerClassShadowFieldCheck.java | 146 / 87 / 59 (40.411%) | 146 / 146 / 0 (0.000%) | 146 / 87 / 59 (40.411%) | 146 / 146 / 0 (0.000%) |
| java-checks/src/main/java/org/sonar/java/checks/InnerClassTooManyLinesCheck.java | 65 / 40 / 25 (38.462%) | 65 / 65 / 0 (0.000%) | 65 / 40 / 25 (38.462%) | 65 / 65 / 0 (0.000%) |
| java-checks/src/main/java/org/sonar/java/checks/InnerStaticClassesCheck.java | 291 / 173 / 118 (40.550%) | 291 / 291 / 0 (0.000%) | 291 / 173 / 118 (40.550%) | 291 / 291 / 0 (0.000%) |
| java-checks/src/main/java/org/sonar/java/checks/InputStreamOverrideReadCheck.java | 109 / 45 / 64 (58.716%) | 109 / 109 / 0 (0.000%) | 109 / 45 / 64 (58.716%) | 109 / 109 / 0 (0.000%) |
| java-checks/src/main/java/org/sonar/java/checks/InputStreamReadCheck.java | 65 / 36 / 29 (44.615%) | 65 / 65 / 0 (0.000%) | 65 / 36 / 29 (44.615%) | 65 / 65 / 0 (0.000%) |
| java-checks/src/main/java/org/sonar/java/checks/InsecureCreateTempFileCheck.java | 14 / 10 / 4 (28.571%) | 14 / 14 / 0 (0.000%) | 14 / 10 / 4 (28.571%) | 14 / 14 / 0 (0.000%) |
| java-checks/src/main/java/org/sonar/java/checks/InstanceOfPatternMatchingCheck.java | 348 / 196 / 152 (43.678%) | 348 / 348 / 0 (0.000%) | 348 / 196 / 152 (43.678%) | 348 / 348 / 0 (0.000%) |
| java-checks/src/main/java/org/sonar/java/checks/InstanceofUsedOnExceptionCheck.java | 165 / 66 / 99 (60.000%) | 165 / 165 / 0 (0.000%) | 165 / 66 / 99 (60.000%) | 165 / 165 / 0 (0.000%) |
| java-checks/src/main/java/org/sonar/java/checks/IntegerOverflowCheck.java | 600 / 281 / 319 (53.167%) | 600 / 600 / 0 (0.000%) | 600 / 281 / 319 (53.167%) | 600 / 600 / 0 (0.000%) |
| java-checks/src/main/java/org/sonar/java/checks/IntegerSubtractionInComparisonCheck.java | 217 / 107 / 110 (50.691%) | 217 / 217 / 0 (0.000%) | 217 / 107 / 110 (50.691%) | 217 / 217 / 0 (0.000%) |
| java-checks/src/main/java/org/sonar/java/checks/IntegerToLongTimestampCastCheck.java | 163 / 57 / 106 (65.031%) | 163 / 163 / 0 (0.000%) | 163 / 57 / 106 (65.031%) | 163 / 163 / 0 (0.000%) |
| java-checks/src/main/java/org/sonar/java/checks/InterfaceAsConstantContainerCheck.java | 79 / 36 / 43 (54.430%) | 79 / 79 / 0 (0.000%) | 79 / 36 / 43 (54.430%) | 79 / 79 / 0 (0.000%) |
| java-checks/src/main/java/org/sonar/java/checks/InterfaceOrSuperclassShadowingCheck.java | 109 / 56 / 53 (48.624%) | 109 / 109 / 0 (0.000%) | 109 / 56 / 53 (48.624%) | 109 / 109 / 0 (0.000%) |
| java-checks/src/main/java/org/sonar/java/checks/InterruptedExceptionCheck.java | 341 / 206 / 135 (39.589%) | 341 / 341 / 0 (0.000%) | 341 / 206 / 135 (39.589%) | 341 / 341 / 0 (0.000%) |
| java-checks/src/main/java/org/sonar/java/checks/InvalidComparatorMethodReferenceCheck.java | 80 / 44 / 36 (45.000%) | 80 / 80 / 0 (0.000%) | 80 / 44 / 36 (45.000%) | 80 / 80 / 0 (0.000%) |
| java-checks/src/main/java/org/sonar/java/checks/InvalidDateValuesCheck.java | 470 / 315 / 155 (32.979%) | 470 / 470 / 0 (0.000%) | 470 / 315 / 155 (32.979%) | 470 / 470 / 0 (0.000%) |
| java-checks/src/main/java/org/sonar/java/checks/IsInstanceMethodCheck.java | 113 / 57 / 56 (49.558%) | 113 / 113 / 0 (0.000%) | 113 / 57 / 56 (49.558%) | 113 / 113 / 0 (0.000%) |
| java-checks/src/main/java/org/sonar/java/checks/IterableIteratorCheck.java | 111 / 47 / 64 (57.658%) | 111 / 111 / 0 (0.000%) | 111 / 47 / 64 (57.658%) | 111 / 111 / 0 (0.000%) |
| java-checks/src/main/java/org/sonar/java/checks/IteratorNextExceptionCheck.java | 169 / 90 / 79 (46.746%) | 169 / 169 / 0 (0.000%) | 169 / 90 / 79 (46.746%) | 169 / 169 / 0 (0.000%) |
| java-checks/src/main/java/org/sonar/java/checks/JEEThreadCheck.java | 195 / 106 / 89 (45.641%) | 195 / 195 / 0 (0.000%) | 195 / 106 / 89 (45.641%) | 195 / 195 / 0 (0.000%) |
| java-checks/src/main/java/org/sonar/java/checks/JacksonDeserializationCheck.java | 172 / 80 / 92 (53.488%) | 172 / 172 / 0 (0.000%) | 172 / 80 / 92 (53.488%) | 172 / 172 / 0 (0.000%) |
| java-checks/src/main/java/org/sonar/java/checks/JavaFootprint.java | 34 / 34 / 0 (0.000%) | 34 / 34 / 0 (0.000%) | 34 / 34 / 0 (0.000%) | 34 / 34 / 0 (0.000%) |
| java-checks/src/main/java/org/sonar/java/checks/JdbcDriverExplicitLoadingCheck.java | 50 / 23 / 27 (54.000%) | 50 / 50 / 0 (0.000%) | 50 / 23 / 27 (54.000%) | 50 / 50 / 0 (0.000%) |
| java-checks/src/main/java/org/sonar/java/checks/JpaEagerFetchTypeCheck.java | 93 / 49 / 44 (47.312%) | 93 / 93 / 0 (0.000%) | 93 / 49 / 44 (47.312%) | 93 / 93 / 0 (0.000%) |
| java-checks/src/main/java/org/sonar/java/checks/JpaEntityFinalCheck.java | 101 / 54 / 47 (46.535%) | 101 / 101 / 0 (0.000%) | 101 / 54 / 47 (46.535%) | 101 / 101 / 0 (0.000%) |
| java-checks/src/main/java/org/sonar/java/checks/KeySetInsteadOfEntrySetCheck.java | 193 / 98 / 95 (49.223%) | 193 / 193 / 0 (0.000%) | 193 / 98 / 95 (49.223%) | 193 / 193 / 0 (0.000%) |
| java-checks/src/main/java/org/sonar/java/checks/KnownCapacityHashBasedCollectionCheck.java | 96 / 52 / 44 (45.833%) | 96 / 96 / 0 (0.000%) | 96 / 52 / 44 (45.833%) | 96 / 96 / 0 (0.000%) |
| java-checks/src/main/java/org/sonar/java/checks/LabelsShouldNotBeUsedCheck.java | 29 / 18 / 11 (37.931%) | 29 / 29 / 0 (0.000%) | 29 / 18 / 11 (37.931%) | 29 / 29 / 0 (0.000%) |
| java-checks/src/main/java/org/sonar/java/checks/LambdaOptionalParenthesisCheck.java | 63 / 32 / 31 (49.206%) | 63 / 63 / 0 (0.000%) | 63 / 32 / 31 (49.206%) | 63 / 63 / 0 (0.000%) |
| java-checks/src/main/java/org/sonar/java/checks/LambdaSingleExpressionCheck.java | 210 / 98 / 112 (53.333%) | 210 / 210 / 0 (0.000%) | 210 / 98 / 112 (53.333%) | 210 / 210 / 0 (0.000%) |
| java-checks/src/main/java/org/sonar/java/checks/LambdaTooBigCheck.java | 73 / 52 / 21 (28.767%) | 73 / 73 / 0 (0.000%) | 73 / 52 / 21 (28.767%) | 73 / 73 / 0 (0.000%) |
| java-checks/src/main/java/org/sonar/java/checks/LambdaTypeParameterCheck.java | 72 / 32 / 40 (55.556%) | 72 / 72 / 0 (0.000%) | 72 / 32 / 40 (55.556%) | 72 / 72 / 0 (0.000%) |
| java-checks/src/main/java/org/sonar/java/checks/LazyArgEvaluationCheck.java | 492 / 281 / 211 (42.886%) | 492 / 492 / 0 (0.000%) | 492 / 281 / 211 (42.886%) | 492 / 492 / 0 (0.000%) |
| java-checks/src/main/java/org/sonar/java/checks/LeastSpecificTypeCheck.java | 560 / 261 / 299 (53.393%) | 560 / 560 / 0 (0.000%) | 560 / 261 / 299 (53.393%) | 560 / 560 / 0 (0.000%) |
| java-checks/src/main/java/org/sonar/java/checks/LeftCurlyBraceBaseTreeVisitor.java | 424 / 257 / 167 (39.387%) | 424 / 424 / 0 (0.000%) | 424 / 257 / 167 (39.387%) | 424 / 424 / 0 (0.000%) |
| java-checks/src/main/java/org/sonar/java/checks/LeftCurlyBraceEndLineCheck.java | 26 / 18 / 8 (30.769%) | 26 / 26 / 0 (0.000%) | 26 / 18 / 8 (30.769%) | 26 / 26 / 0 (0.000%) |
| java-checks/src/main/java/org/sonar/java/checks/LeftCurlyBraceStartLineCheck.java | 26 / 18 / 8 (30.769%) | 26 / 26 / 0 (0.000%) | 26 / 18 / 8 (30.769%) | 26 / 26 / 0 (0.000%) |
| java-checks/src/main/java/org/sonar/java/checks/LocalVariablesShouldNotSpanSwitchCaseGroupsCheck.java | 152 / 94 / 58 (38.158%) | 152 / 152 / 0 (0.000%) | 152 / 94 / 58 (38.158%) | 152 / 152 / 0 (0.000%) |
| java-checks/src/main/java/org/sonar/java/checks/LoggedRethrownExceptionsCheck.java | 201 / 109 / 92 (45.771%) | 201 / 201 / 0 (0.000%) | 201 / 109 / 92 (45.771%) | 201 / 201 / 0 (0.000%) |
| java-checks/src/main/java/org/sonar/java/checks/LoggerClassCheck.java | 183 / 68 / 115 (62.842%) | 183 / 183 / 0 (0.000%) | 183 / 68 / 115 (62.842%) | 183 / 183 / 0 (0.000%) |
| java-checks/src/main/java/org/sonar/java/checks/LoggersDeclarationCheck.java | 164 / 110 / 54 (32.927%) | 164 / 164 / 0 (0.000%) | 164 / 110 / 54 (32.927%) | 164 / 164 / 0 (0.000%) |
| java-checks/src/main/java/org/sonar/java/checks/LongBitsToDoubleOnIntCheck.java | 37 / 18 / 19 (51.351%) | 37 / 37 / 0 (0.000%) | 37 / 18 / 19 (51.351%) | 37 / 37 / 0 (0.000%) |
| java-checks/src/main/java/org/sonar/java/checks/LoopExecutingAtMostOnceCheck.java | 348 / 147 / 201 (57.759%) | 348 / 348 / 0 (0.000%) | 348 / 147 / 201 (57.759%) | 348 / 348 / 0 (0.000%) |
| java-checks/src/main/java/org/sonar/java/checks/LoopsOnSameSetCheck.java | 164 / 98 / 66 (40.244%) | 164 / 164 / 0 (0.000%) | 164 / 98 / 66 (40.244%) | 164 / 164 / 0 (0.000%) |
| java-checks/src/main/java/org/sonar/java/checks/MagicNumberCheck.java | 159 / 113 / 46 (28.931%) | 159 / 159 / 0 (0.000%) | 159 / 113 / 46 (28.931%) | 159 / 159 / 0 (0.000%) |
| java-checks/src/main/java/org/sonar/java/checks/MainMethodSignatureCheck.java | 41 / 23 / 18 (43.902%) | 41 / 41 / 0 (0.000%) | 41 / 23 / 18 (43.902%) | 41 / 41 / 0 (0.000%) |
| java-checks/src/main/java/org/sonar/java/checks/MainMethodThrowsExceptionCheck.java | 37 / 19 / 18 (48.649%) | 37 / 37 / 0 (0.000%) | 37 / 19 / 18 (48.649%) | 37 / 37 / 0 (0.000%) |
| java-checks/src/main/java/org/sonar/java/checks/MapKeyNotComparableCheck.java | 81 / 40 / 41 (50.617%) | 81 / 81 / 0 (0.000%) | 81 / 40 / 41 (50.617%) | 81 / 81 / 0 (0.000%) |
| java-checks/src/main/java/org/sonar/java/checks/MapperWithoutDaoFactoryCheck.java | 127 / 65 / 62 (48.819%) | 127 / 127 / 0 (0.000%) | 127 / 65 / 62 (48.819%) | 127 / 127 / 0 (0.000%) |
| java-checks/src/main/java/org/sonar/java/checks/MarkdownJavadocSyntaxCheck.java | 256 / 198 / 58 (22.656%) | 256 / 256 / 0 (0.000%) | 256 / 198 / 58 (22.656%) | 256 / 256 / 0 (0.000%) |
| java-checks/src/main/java/org/sonar/java/checks/MathClampMethodsCheck.java | 292 / 167 / 125 (42.808%) | 292 / 292 / 0 (0.000%) | 292 / 167 / 125 (42.808%) | 292 / 292 / 0 (0.000%) |
| java-checks/src/main/java/org/sonar/java/checks/MathClampRangeCheck.java | 225 / 152 / 73 (32.444%) | 225 / 225 / 0 (0.000%) | 225 / 152 / 73 (32.444%) | 225 / 225 / 0 (0.000%) |
| java-checks/src/main/java/org/sonar/java/checks/MathOnFloatCheck.java | 69 / 34 / 35 (50.725%) | 69 / 69 / 0 (0.000%) | 69 / 34 / 35 (50.725%) | 69 / 69 / 0 (0.000%) |
| java-checks/src/main/java/org/sonar/java/checks/MembersDifferOnlyByCapitalizationCheck.java | 556 / 321 / 235 (42.266%) | 556 / 556 / 0 (0.000%) | 556 / 321 / 235 (42.266%) | 556 / 556 / 0 (0.000%) |
| java-checks/src/main/java/org/sonar/java/checks/MethodComplexityCheck.java | 94 / 55 / 39 (41.489%) | 94 / 94 / 0 (0.000%) | 94 / 55 / 39 (41.489%) | 94 / 94 / 0 (0.000%) |
| java-checks/src/main/java/org/sonar/java/checks/MethodIdenticalImplementationsCheck.java | 210 / 129 / 81 (38.571%) | 210 / 210 / 0 (0.000%) | 210 / 129 / 81 (38.571%) | 210 / 210 / 0 (0.000%) |
| java-checks/src/main/java/org/sonar/java/checks/MethodOnlyCallsSuperCheck.java | 313 / 148 / 165 (52.716%) | 313 / 313 / 0 (0.000%) | 313 / 148 / 165 (52.716%) | 313 / 313 / 0 (0.000%) |
| java-checks/src/main/java/org/sonar/java/checks/MethodParametersOrderCheck.java | 296 / 159 / 137 (46.284%) | 296 / 296 / 0 (0.000%) | 296 / 159 / 137 (46.284%) | 296 / 296 / 0 (0.000%) |
| java-checks/src/main/java/org/sonar/java/checks/MethodTooBigCheck.java | 56 / 35 / 21 (37.500%) | 56 / 56 / 0 (0.000%) | 56 / 35 / 21 (37.500%) | 56 / 56 / 0 (0.000%) |
| java-checks/src/main/java/org/sonar/java/checks/MethodWithExcessiveReturnsCheck.java | 163 / 85 / 78 (47.853%) | 163 / 163 / 0 (0.000%) | 163 / 85 / 78 (47.853%) | 163 / 163 / 0 (0.000%) |
| java-checks/src/main/java/org/sonar/java/checks/MismatchPackageDirectoryCheck.java | 128 / 109 / 19 (14.844%) | 128 / 128 / 0 (0.000%) | 128 / 109 / 19 (14.844%) | 128 / 128 / 0 (0.000%) |
| java-checks/src/main/java/org/sonar/java/checks/MissingBeanValidationCheck.java | 288 / 157 / 131 (45.486%) | 288 / 288 / 0 (0.000%) | 288 / 157 / 131 (45.486%) | 288 / 288 / 0 (0.000%) |
| java-checks/src/main/java/org/sonar/java/checks/MissingCurlyBracesCheck.java | 150 / 73 / 77 (51.333%) | 150 / 150 / 0 (0.000%) | 150 / 73 / 77 (51.333%) | 150 / 150 / 0 (0.000%) |
| java-checks/src/main/java/org/sonar/java/checks/MissingDeprecatedCheck.java | 52 / 32 / 20 (38.462%) | 52 / 52 / 0 (0.000%) | 52 / 32 / 20 (38.462%) | 52 / 52 / 0 (0.000%) |
| java-checks/src/main/java/org/sonar/java/checks/MissingNewLineAtEndOfFileCheck.java | 33 / 29 / 4 (12.121%) | 33 / 33 / 0 (0.000%) | 33 / 29 / 4 (12.121%) | 33 / 33 / 0 (0.000%) |
| java-checks/src/main/java/org/sonar/java/checks/MissingOverridesInRecordWithArrayComponentCheck.java | 175 / 100 / 75 (42.857%) | 175 / 175 / 0 (0.000%) | 175 / 100 / 75 (42.857%) | 175 / 175 / 0 (0.000%) |
| java-checks/src/main/java/org/sonar/java/checks/MissingPackageInfoCheck.java | 55 / 47 / 8 (14.545%) | 55 / 55 / 0 (0.000%) | 55 / 47 / 8 (14.545%) | 55 / 55 / 0 (0.000%) |
| java-checks/src/main/java/org/sonar/java/checks/ModifiersOrderCheck.java | 283 / 135 / 148 (52.297%) | 283 / 283 / 0 (0.000%) | 283 / 135 / 148 (52.297%) | 283 / 283 / 0 (0.000%) |
| java-checks/src/main/java/org/sonar/java/checks/ModulusEqualityCheck.java | 224 / 131 / 93 (41.518%) | 224 / 224 / 0 (0.000%) | 224 / 131 / 93 (41.518%) | 224 / 224 / 0 (0.000%) |
| java-checks/src/main/java/org/sonar/java/checks/MultilineBlocksCurlyBracesCheck.java | 186 / 117 / 69 (37.097%) | 186 / 186 / 0 (0.000%) | 186 / 117 / 69 (37.097%) | 186 / 186 / 0 (0.000%) |
| java-checks/src/main/java/org/sonar/java/checks/MultipleMainInstancesCheck.java | 138 / 66 / 72 (52.174%) | 138 / 138 / 0 (0.000%) | 138 / 66 / 72 (52.174%) | 138 / 138 / 0 (0.000%) |
| java-checks/src/main/java/org/sonar/java/checks/MutableMembersUsageCheck.java | 901 / 613 / 288 (31.964%) | 901 / 901 / 0 (0.000%) | 901 / 613 / 288 (31.964%) | 901 / 901 / 0 (0.000%) |
| java-checks/src/main/java/org/sonar/java/checks/NPEThrowCheck.java | 74 / 36 / 38 (51.351%) | 74 / 74 / 0 (0.000%) | 74 / 36 / 38 (51.351%) | 74 / 74 / 0 (0.000%) |
| java-checks/src/main/java/org/sonar/java/checks/NanEqualityCheck.java | 49 / 28 / 21 (42.857%) | 49 / 49 / 0 (0.000%) | 49 / 28 / 21 (42.857%) | 49 / 49 / 0 (0.000%) |
| java-checks/src/main/java/org/sonar/java/checks/NestedBlocksCheck.java | 88 / 50 / 38 (43.182%) | 88 / 88 / 0 (0.000%) | 88 / 50 / 38 (43.182%) | 88 / 88 / 0 (0.000%) |
| java-checks/src/main/java/org/sonar/java/checks/NestedEnumStaticCheck.java | 48 / 25 / 23 (47.917%) | 48 / 48 / 0 (0.000%) | 48 / 25 / 23 (47.917%) | 48 / 48 / 0 (0.000%) |
| java-checks/src/main/java/org/sonar/java/checks/NestedIfStatementsCheck.java | 236 / 148 / 88 (37.288%) | 236 / 236 / 0 (0.000%) | 236 / 148 / 88 (37.288%) | 236 / 236 / 0 (0.000%) |
| java-checks/src/main/java/org/sonar/java/checks/NestedSwitchCheck.java | 64 / 38 / 26 (40.625%) | 64 / 64 / 0 (0.000%) | 64 / 38 / 26 (40.625%) | 64 / 64 / 0 (0.000%) |
| java-checks/src/main/java/org/sonar/java/checks/NestedTernaryOperatorsCheck.java | 51 / 28 / 23 (45.098%) | 51 / 51 / 0 (0.000%) | 51 / 28 / 23 (45.098%) | 51 / 51 / 0 (0.000%) |
| java-checks/src/main/java/org/sonar/java/checks/NestedTryCatchCheck.java | 107 / 57 / 50 (46.729%) | 107 / 107 / 0 (0.000%) | 107 / 57 / 50 (46.729%) | 107 / 107 / 0 (0.000%) |
| java-checks/src/main/java/org/sonar/java/checks/NioFileDeleteCheck.java | 32 / 17 / 15 (46.875%) | 32 / 32 / 0 (0.000%) | 32 / 17 / 15 (46.875%) | 32 / 32 / 0 (0.000%) |
| java-checks/src/main/java/org/sonar/java/checks/NoCheckstyleTagPresenceCheck.java | 35 / 24 / 11 (31.429%) | 35 / 35 / 0 (0.000%) | 35 / 24 / 11 (31.429%) | 35 / 35 / 0 (0.000%) |
| java-checks/src/main/java/org/sonar/java/checks/NoPmdTagPresenceCheck.java | 35 / 24 / 11 (31.429%) | 35 / 35 / 0 (0.000%) | 35 / 24 / 11 (31.429%) | 35 / 35 / 0 (0.000%) |
| java-checks/src/main/java/org/sonar/java/checks/NoSonarCheck.java | 35 / 24 / 11 (31.429%) | 35 / 35 / 0 (0.000%) | 35 / 24 / 11 (31.429%) | 35 / 35 / 0 (0.000%) |
| java-checks/src/main/java/org/sonar/java/checks/NonShortCircuitLogicCheck.java | 91 / 65 / 26 (28.571%) | 91 / 91 / 0 (0.000%) | 91 / 65 / 26 (28.571%) | 91 / 91 / 0 (0.000%) |
| java-checks/src/main/java/org/sonar/java/checks/NonStaticClassInitializerCheck.java | 48 / 24 / 24 (50.000%) | 48 / 48 / 0 (0.000%) | 48 / 24 / 24 (50.000%) | 48 / 48 / 0 (0.000%) |
| java-checks/src/main/java/org/sonar/java/checks/NotifyCheck.java | 43 / 16 / 27 (62.791%) | 43 / 43 / 0 (0.000%) | 43 / 16 / 27 (62.791%) | 43 / 43 / 0 (0.000%) |
| java-checks/src/main/java/org/sonar/java/checks/NowWithoutParametersCheck.java | 45 / 27 / 18 (40.000%) | 45 / 45 / 0 (0.000%) | 45 / 27 / 18 (40.000%) | 45 / 45 / 0 (0.000%) |
| java-checks/src/main/java/org/sonar/java/checks/NullCheckWithInstanceofCheck.java | 214 / 102 / 112 (52.336%) | 214 / 214 / 0 (0.000%) | 214 / 102 / 112 (52.336%) | 214 / 214 / 0 (0.000%) |
| java-checks/src/main/java/org/sonar/java/checks/NullReturnedOnComputeIfPresentOrAbsentCheck.java | 87 / 37 / 50 (57.471%) | 87 / 87 / 0 (0.000%) | 87 / 37 / 50 (57.471%) | 87 / 87 / 0 (0.000%) |
| java-checks/src/main/java/org/sonar/java/checks/NullShouldNotBeUsedWithOptionalCheck.java | 252 / 158 / 94 (37.302%) | 252 / 252 / 0 (0.000%) | 252 / 158 / 94 (37.302%) | 252 / 252 / 0 (0.000%) |
| java-checks/src/main/java/org/sonar/java/checks/OSCommandsPathCheck.java | 315 / 172 / 143 (45.397%) | 315 / 315 / 0 (0.000%) | 315 / 172 / 143 (45.397%) | 315 / 315 / 0 (0.000%) |
| java-checks/src/main/java/org/sonar/java/checks/ObjectCreatedOnlyToCallGetClassCheck.java | 156 / 86 / 70 (44.872%) | 156 / 156 / 0 (0.000%) | 156 / 86 / 70 (44.872%) | 156 / 156 / 0 (0.000%) |
| java-checks/src/main/java/org/sonar/java/checks/ObjectFinalizeCheck.java | 93 / 50 / 43 (46.237%) | 93 / 93 / 0 (0.000%) | 93 / 50 / 43 (46.237%) | 93 / 93 / 0 (0.000%) |
| java-checks/src/main/java/org/sonar/java/checks/ObjectFinalizeOverloadedCheck.java | 70 / 41 / 29 (41.429%) | 70 / 70 / 0 (0.000%) | 70 / 41 / 29 (41.429%) | 70 / 70 / 0 (0.000%) |
| java-checks/src/main/java/org/sonar/java/checks/ObjectFinalizeOverriddenCallsSuperFinalizeCheck.java | 211 / 112 / 99 (46.919%) | 211 / 211 / 0 (0.000%) | 211 / 112 / 99 (46.919%) | 211 / 211 / 0 (0.000%) |
| java-checks/src/main/java/org/sonar/java/checks/ObjectFinalizeOverriddenCheck.java | 60 / 31 / 29 (48.333%) | 60 / 60 / 0 (0.000%) | 60 / 31 / 29 (48.333%) | 60 / 60 / 0 (0.000%) |
| java-checks/src/main/java/org/sonar/java/checks/ObjectFinalizeOverriddenNotPublicCheck.java | 52 / 29 / 23 (44.231%) | 52 / 52 / 0 (0.000%) | 52 / 29 / 23 (44.231%) | 52 / 52 / 0 (0.000%) |
| java-checks/src/main/java/org/sonar/java/checks/ObjectsEqualsCheck.java | 243 / 125 / 118 (48.560%) | 243 / 243 / 0 (0.000%) | 243 / 125 / 118 (48.560%) | 243 / 243 / 0 (0.000%) |
| java-checks/src/main/java/org/sonar/java/checks/OctalEscapeSequenceFollowedByDigitCheck.java | 122 / 103 / 19 (15.574%) | 122 / 122 / 0 (0.000%) | 122 / 103 / 19 (15.574%) | 122 / 122 / 0 (0.000%) |
| java-checks/src/main/java/org/sonar/java/checks/OctalValuesCheck.java | 62 / 49 / 13 (20.968%) | 62 / 62 / 0 (0.000%) | 62 / 49 / 13 (20.968%) | 62 / 62 / 0 (0.000%) |
| java-checks/src/main/java/org/sonar/java/checks/OmitPermittedTypesCheck.java | 118 / 52 / 66 (55.932%) | 118 / 118 / 0 (0.000%) | 118 / 52 / 66 (55.932%) | 118 / 118 / 0 (0.000%) |
| java-checks/src/main/java/org/sonar/java/checks/OneClassInterfacePerFileCheck.java | 32 / 21 / 11 (34.375%) | 32 / 32 / 0 (0.000%) | 32 / 21 / 11 (34.375%) | 32 / 32 / 0 (0.000%) |
| java-checks/src/main/java/org/sonar/java/checks/OneDeclarationPerLineCheck.java | 296 / 160 / 136 (45.946%) | 296 / 296 / 0 (0.000%) | 296 / 160 / 136 (45.946%) | 296 / 296 / 0 (0.000%) |
| java-checks/src/main/java/org/sonar/java/checks/OneToManyMappingCheck.java | 151 / 63 / 88 (58.278%) | 151 / 151 / 0 (0.000%) | 151 / 63 / 88 (58.278%) | 151 / 151 / 0 (0.000%) |
| java-checks/src/main/java/org/sonar/java/checks/OperatorPrecedenceCheck.java | 532 / 267 / 265 (49.812%) | 532 / 532 / 0 (0.000%) | 532 / 267 / 265 (49.812%) | 532 / 532 / 0 (0.000%) |
| java-checks/src/main/java/org/sonar/java/checks/OptionalAsParameterCheck.java | 119 / 77 / 42 (35.294%) | 119 / 119 / 0 (0.000%) | 119 / 77 / 42 (35.294%) | 119 / 119 / 0 (0.000%) |
| java-checks/src/main/java/org/sonar/java/checks/OptionalWrappingContainerCheck.java | 87 / 54 / 33 (37.931%) | 87 / 87 / 0 (0.000%) | 87 / 54 / 33 (37.931%) | 87 / 87 / 0 (0.000%) |
| java-checks/src/main/java/org/sonar/java/checks/OutputStreamOverrideWriteCheck.java | 114 / 51 / 63 (55.263%) | 114 / 114 / 0 (0.000%) | 114 / 51 / 63 (55.263%) | 114 / 114 / 0 (0.000%) |
| java-checks/src/main/java/org/sonar/java/checks/OverrideAnnotationCheck.java | 149 / 70 / 79 (53.020%) | 149 / 149 / 0 (0.000%) | 149 / 70 / 79 (53.020%) | 149 / 149 / 0 (0.000%) |
| java-checks/src/main/java/org/sonar/java/checks/OverwrittenKeyCheck.java | 386 / 224 / 162 (41.969%) | 386 / 386 / 0 (0.000%) | 386 / 224 / 162 (41.969%) | 386 / 386 / 0 (0.000%) |
| java-checks/src/main/java/org/sonar/java/checks/ParameterReassignedToCheck.java | 221 / 119 / 102 (46.154%) | 221 / 221 / 0 (0.000%) | 221 / 119 / 102 (46.154%) | 221 / 221 / 0 (0.000%) |
| java-checks/src/main/java/org/sonar/java/checks/ParsingErrorCheck.java | 46 / 40 / 6 (13.043%) | 46 / 46 / 0 (0.000%) | 46 / 40 / 6 (13.043%) | 46 / 46 / 0 (0.000%) |
| java-checks/src/main/java/org/sonar/java/checks/PatternMatchUsingIfCheck.java | 626 / 424 / 202 (32.268%) | 626 / 626 / 0 (0.000%) | 626 / 424 / 202 (32.268%) | 626 / 626 / 0 (0.000%) |
| java-checks/src/main/java/org/sonar/java/checks/PatternUtils.java | 33 / 33 / 0 (0.000%) | 33 / 33 / 0 (0.000%) | 33 / 33 / 0 (0.000%) | 33 / 33 / 0 (0.000%) |
| java-checks/src/main/java/org/sonar/java/checks/PersistenceAnnotationsMixedCheck.java | 156 / 63 / 93 (59.615%) | 156 / 156 / 0 (0.000%) | 156 / 63 / 93 (59.615%) | 156 / 156 / 0 (0.000%) |
| java-checks/src/main/java/org/sonar/java/checks/PopulateBeansCheck.java | 34 / 15 / 19 (55.882%) | 34 / 34 / 0 (0.000%) | 34 / 15 / 19 (55.882%) | 34 / 34 / 0 (0.000%) |
| java-checks/src/main/java/org/sonar/java/checks/PredictableSeedCheck.java | 143 / 54 / 89 (62.238%) | 143 / 143 / 0 (0.000%) | 143 / 54 / 89 (62.238%) | 143 / 143 / 0 (0.000%) |
| java-checks/src/main/java/org/sonar/java/checks/PreferStreamAnyMatchCheck.java | 209 / 78 / 131 (62.679%) | 209 / 209 / 0 (0.000%) | 209 / 78 / 131 (62.679%) | 209 / 209 / 0 (0.000%) |
| java-checks/src/main/java/org/sonar/java/checks/PreparedStatementAndResultSetCheck.java | 279 / 156 / 123 (44.086%) | 279 / 279 / 0 (0.000%) | 279 / 156 / 123 (44.086%) | 279 / 279 / 0 (0.000%) |
| java-checks/src/main/java/org/sonar/java/checks/PreparedStatementInsideLoopCheck.java | 243 / 133 / 110 (45.267%) | 243 / 243 / 0 (0.000%) | 243 / 133 / 110 (45.267%) | 243 / 243 / 0 (0.000%) |
| java-checks/src/main/java/org/sonar/java/checks/PreparedStatementLoopInvariantCheck.java | 289 / 185 / 104 (35.986%) | 289 / 289 / 0 (0.000%) | 289 / 185 / 104 (35.986%) | 289 / 289 / 0 (0.000%) |
| java-checks/src/main/java/org/sonar/java/checks/PresuperLogicBloatsConstructorCheck.java | 47 / 33 / 14 (29.787%) | 47 / 47 / 0 (0.000%) | 47 / 33 / 14 (29.787%) | 47 / 47 / 0 (0.000%) |
| java-checks/src/main/java/org/sonar/java/checks/PrimitiveTypeBoxingWithToStringCheck.java | 104 / 56 / 48 (46.154%) | 104 / 104 / 0 (0.000%) | 104 / 56 / 48 (46.154%) | 104 / 104 / 0 (0.000%) |
| java-checks/src/main/java/org/sonar/java/checks/PrimitiveWrappersInTernaryOperatorCheck.java | 52 / 31 / 21 (40.385%) | 52 / 52 / 0 (0.000%) | 52 / 31 / 21 (40.385%) | 52 / 52 / 0 (0.000%) |
| java-checks/src/main/java/org/sonar/java/checks/PrimitivesMarkedNullableCheck.java | 86 / 35 / 51 (59.302%) | 86 / 86 / 0 (0.000%) | 86 / 35 / 51 (59.302%) | 86 / 86 / 0 (0.000%) |
| java-checks/src/main/java/org/sonar/java/checks/PrintfFailCheck.java | 123 / 86 / 37 (30.081%) | 123 / 123 / 0 (0.000%) | 123 / 86 / 37 (30.081%) | 123 / 123 / 0 (0.000%) |
| java-checks/src/main/java/org/sonar/java/checks/PrintfMisuseCheck.java | 852 / 467 / 385 (45.188%) | 852 / 852 / 0 (0.000%) | 852 / 467 / 385 (45.188%) | 852 / 852 / 0 (0.000%) |
| java-checks/src/main/java/org/sonar/java/checks/PrivateFieldUsedLocallyCheck.java | 393 / 188 / 205 (52.163%) | 393 / 393 / 0 (0.000%) | 393 / 188 / 205 (52.163%) | 393 / 393 / 0 (0.000%) |
| java-checks/src/main/java/org/sonar/java/checks/ProtectedMemberInFinalClassCheck.java | 111 / 58 / 53 (47.748%) | 111 / 111 / 0 (0.000%) | 111 / 58 / 53 (47.748%) | 111 / 111 / 0 (0.000%) |
| java-checks/src/main/java/org/sonar/java/checks/PseudoRandomCheck.java | 228 / 142 / 86 (37.719%) | 228 / 228 / 0 (0.000%) | 228 / 142 / 86 (37.719%) | 228 / 228 / 0 (0.000%) |
| java-checks/src/main/java/org/sonar/java/checks/PublicConstructorInAbstractClassCheck.java | 99 / 26 / 73 (73.737%) | 99 / 99 / 0 (0.000%) | 99 / 26 / 73 (73.737%) | 99 / 99 / 0 (0.000%) |
| java-checks/src/main/java/org/sonar/java/checks/PublicStaticFieldShouldBeFinalCheck.java | 89 / 49 / 40 (44.944%) | 89 / 89 / 0 (0.000%) | 89 / 49 / 40 (44.944%) | 89 / 89 / 0 (0.000%) |
| java-checks/src/main/java/org/sonar/java/checks/PublicStaticMutableMembersCheck.java | 472 / 219 / 253 (53.602%) | 472 / 472 / 0 (0.000%) | 472 / 219 / 253 (53.602%) | 472 / 472 / 0 (0.000%) |
| java-checks/src/main/java/org/sonar/java/checks/QuarkusCacheResultOnVoidMethodCheck.java | 50 / 29 / 21 (42.000%) | 50 / 50 / 0 (0.000%) | 50 / 29 / 21 (42.000%) | 50 / 50 / 0 (0.000%) |
| java-checks/src/main/java/org/sonar/java/checks/QueryOnlyRequiredFieldsCheck.java | 106 / 42 / 64 (60.377%) | 106 / 106 / 0 (0.000%) | 106 / 42 / 64 (60.377%) | 106 / 106 / 0 (0.000%) |
| java-checks/src/main/java/org/sonar/java/checks/RandomFloatToIntCheck.java | 121 / 62 / 59 (48.760%) | 121 / 121 / 0 (0.000%) | 121 / 62 / 59 (48.760%) | 121 / 121 / 0 (0.000%) |
| java-checks/src/main/java/org/sonar/java/checks/RawByteBitwiseOperationsCheck.java | 117 / 63 / 54 (46.154%) | 117 / 117 / 0 (0.000%) | 117 / 63 / 54 (46.154%) | 117 / 117 / 0 (0.000%) |
| java-checks/src/main/java/org/sonar/java/checks/RawExceptionCheck.java | 215 / 139 / 76 (35.349%) | 215 / 215 / 0 (0.000%) | 215 / 139 / 76 (35.349%) | 215 / 215 / 0 (0.000%) |
| java-checks/src/main/java/org/sonar/java/checks/RawTypeCheck.java | 125 / 79 / 46 (36.800%) | 125 / 125 / 0 (0.000%) | 125 / 79 / 46 (36.800%) | 125 / 125 / 0 (0.000%) |
| java-checks/src/main/java/org/sonar/java/checks/ReadObjectSynchronizedCheck.java | 80 / 40 / 40 (50.000%) | 80 / 80 / 0 (0.000%) | 80 / 40 / 40 (50.000%) | 80 / 80 / 0 (0.000%) |
| java-checks/src/main/java/org/sonar/java/checks/ReadlnWithPromptCheck.java | 95 / 52 / 43 (45.263%) | 95 / 95 / 0 (0.000%) | 95 / 52 / 43 (45.263%) | 95 / 95 / 0 (0.000%) |
| java-checks/src/main/java/org/sonar/java/checks/RecordDuplicatedGetterCheck.java | 337 / 143 / 194 (57.567%) | 337 / 337 / 0 (0.000%) | 337 / 143 / 194 (57.567%) | 337 / 337 / 0 (0.000%) |
| java-checks/src/main/java/org/sonar/java/checks/RecordInsteadOfClassCheck.java | 664 / 361 / 303 (45.633%) | 664 / 664 / 0 (0.000%) | 664 / 361 / 303 (45.633%) | 664 / 664 / 0 (0.000%) |
| java-checks/src/main/java/org/sonar/java/checks/RecordPatternInsteadOfFieldAccessCheck.java | 196 / 103 / 93 (47.449%) | 196 / 196 / 0 (0.000%) | 196 / 103 / 93 (47.449%) | 196 / 196 / 0 (0.000%) |
| java-checks/src/main/java/org/sonar/java/checks/RedundantAbstractMethodCheck.java | 251 / 139 / 112 (44.622%) | 251 / 251 / 0 (0.000%) | 251 / 139 / 112 (44.622%) | 251 / 251 / 0 (0.000%) |
| java-checks/src/main/java/org/sonar/java/checks/RedundantCloseCheck.java | 136 / 60 / 76 (55.882%) | 136 / 136 / 0 (0.000%) | 136 / 60 / 76 (55.882%) | 136 / 136 / 0 (0.000%) |
| java-checks/src/main/java/org/sonar/java/checks/RedundantJumpCheck.java | 155 / 73 / 82 (52.903%) | 155 / 155 / 0 (0.000%) | 155 / 73 / 82 (52.903%) | 155 / 155 / 0 (0.000%) |
| java-checks/src/main/java/org/sonar/java/checks/RedundantModifierCheck.java | 197 / 93 / 104 (52.792%) | 197 / 197 / 0 (0.000%) | 197 / 93 / 104 (52.792%) | 197 / 197 / 0 (0.000%) |
| java-checks/src/main/java/org/sonar/java/checks/RedundantNullabilityAnnotationsCheck.java | 294 / 165 / 129 (43.878%) | 294 / 294 / 0 (0.000%) | 294 / 165 / 129 (43.878%) | 294 / 294 / 0 (0.000%) |
| java-checks/src/main/java/org/sonar/java/checks/RedundantRangeCheckCheck.java | 502 / 369 / 133 (26.494%) | 502 / 502 / 0 (0.000%) | 502 / 369 / 133 (26.494%) | 502 / 502 / 0 (0.000%) |
| java-checks/src/main/java/org/sonar/java/checks/RedundantRecordMethodsCheck.java | 476 / 313 / 163 (34.244%) | 476 / 476 / 0 (0.000%) | 476 / 313 / 163 (34.244%) | 476 / 476 / 0 (0.000%) |
| java-checks/src/main/java/org/sonar/java/checks/RedundantStreamCollectCheck.java | 137 / 52 / 85 (62.044%) | 137 / 137 / 0 (0.000%) | 137 / 52 / 85 (62.044%) | 137 / 137 / 0 (0.000%) |
| java-checks/src/main/java/org/sonar/java/checks/RedundantStringFormatCheck.java | 296 / 136 / 160 (54.054%) | 296 / 296 / 0 (0.000%) | 296 / 136 / 160 (54.054%) | 296 / 296 / 0 (0.000%) |
| java-checks/src/main/java/org/sonar/java/checks/RedundantThrowsDeclarationCheck.java | 688 / 362 / 326 (47.384%) | 688 / 688 / 0 (0.000%) | 688 / 362 / 326 (47.384%) | 688 / 688 / 0 (0.000%) |
| java-checks/src/main/java/org/sonar/java/checks/RedundantTypeCastCheck.java | 167 / 70 / 97 (58.084%) | 167 / 167 / 0 (0.000%) | 167 / 70 / 97 (58.084%) | 167 / 167 / 0 (0.000%) |
| java-checks/src/main/java/org/sonar/java/checks/ReflectionOnNonRuntimeAnnotationCheck.java | 85 / 47 / 38 (44.706%) | 85 / 85 / 0 (0.000%) | 85 / 47 / 38 (44.706%) | 85 / 85 / 0 (0.000%) |
| java-checks/src/main/java/org/sonar/java/checks/RegexPatternsNeedlesslyCheck.java | 168 / 100 / 68 (40.476%) | 168 / 168 / 0 (0.000%) | 168 / 100 / 68 (40.476%) | 168 / 168 / 0 (0.000%) |
| java-checks/src/main/java/org/sonar/java/checks/ReleaseSensorsCheck.java | 230 / 168 / 62 (26.957%) | 230 / 230 / 0 (0.000%) | 230 / 168 / 62 (26.957%) | 230 / 230 / 0 (0.000%) |
| java-checks/src/main/java/org/sonar/java/checks/RemoveTypeFromUnusedPatternCheck.java | 79 / 35 / 44 (55.696%) | 79 / 79 / 0 (0.000%) | 79 / 35 / 44 (55.696%) | 79 / 79 / 0 (0.000%) |
| java-checks/src/main/java/org/sonar/java/checks/RepeatAnnotationCheck.java | 164 / 88 / 76 (46.341%) | 164 / 164 / 0 (0.000%) | 164 / 88 / 76 (46.341%) | 164 / 164 / 0 (0.000%) |
| java-checks/src/main/java/org/sonar/java/checks/ReplaceGuavaWithJavaCheck.java | 195 / 115 / 80 (41.026%) | 195 / 195 / 0 (0.000%) | 195 / 115 / 80 (41.026%) | 195 / 195 / 0 (0.000%) |
| java-checks/src/main/java/org/sonar/java/checks/ReplaceLambdaByMethodRefCheck.java | 968 / 438 / 530 (54.752%) | 968 / 968 / 0 (0.000%) | 968 / 438 / 530 (54.752%) | 968 / 968 / 0 (0.000%) |
| java-checks/src/main/java/org/sonar/java/checks/ReplaceUnusedExceptionParameterWithUnnamedPatternCheck.java | 90 / 41 / 49 (54.444%) | 90 / 90 / 0 (0.000%) | 90 / 41 / 49 (54.444%) | 90 / 90 / 0 (0.000%) |
| java-checks/src/main/java/org/sonar/java/checks/RestDataPanacheResourceImplementationCheck.java | 67 / 44 / 23 (34.328%) | 67 / 67 / 0 (0.000%) | 67 / 44 / 23 (34.328%) | 67 / 67 / 0 (0.000%) |
| java-checks/src/main/java/org/sonar/java/checks/RestrictedIdentifiersUsageCheck.java | 72 / 46 / 26 (36.111%) | 72 / 72 / 0 (0.000%) | 72 / 46 / 26 (36.111%) | 72 / 72 / 0 (0.000%) |
| java-checks/src/main/java/org/sonar/java/checks/ResultSetIsLastCheck.java | 25 / 13 / 12 (48.000%) | 25 / 25 / 0 (0.000%) | 25 / 13 / 12 (48.000%) | 25 / 25 / 0 (0.000%) |
| java-checks/src/main/java/org/sonar/java/checks/ReturnEmptyArrayNotNullCheck.java | 422 / 293 / 129 (30.569%) | 422 / 422 / 0 (0.000%) | 422 / 293 / 129 (30.569%) | 422 / 422 / 0 (0.000%) |
| java-checks/src/main/java/org/sonar/java/checks/ReturnInFinallyCheck.java | 273 / 143 / 130 (47.619%) | 273 / 273 / 0 (0.000%) | 273 / 143 / 130 (47.619%) | 273 / 273 / 0 (0.000%) |
| java-checks/src/main/java/org/sonar/java/checks/ReturnOfBooleanExpressionsCheck.java | 288 / 152 / 136 (47.222%) | 288 / 288 / 0 (0.000%) | 288 / 152 / 136 (47.222%) | 288 / 288 / 0 (0.000%) |
| java-checks/src/main/java/org/sonar/java/checks/ReuseRandomCheck.java | 110 / 55 / 55 (50.000%) | 110 / 110 / 0 (0.000%) | 110 / 55 / 55 (50.000%) | 110 / 110 / 0 (0.000%) |
| java-checks/src/main/java/org/sonar/java/checks/ReverseSequencedCollectionCheck.java | 397 / 188 / 209 (52.645%) | 397 / 397 / 0 (0.000%) | 397 / 188 / 209 (52.645%) | 397 / 397 / 0 (0.000%) |
| java-checks/src/main/java/org/sonar/java/checks/ReversedMethodSequencedCollectionCheck.java | 110 / 57 / 53 (48.182%) | 110 / 110 / 0 (0.000%) | 110 / 57 / 53 (48.182%) | 110 / 110 / 0 (0.000%) |
| java-checks/src/main/java/org/sonar/java/checks/RightCurlyBraceDifferentLineAsNextBlockCheck.java | 28 / 18 / 10 (35.714%) | 28 / 28 / 0 (0.000%) | 28 / 18 / 10 (35.714%) | 28 / 28 / 0 (0.000%) |
| java-checks/src/main/java/org/sonar/java/checks/RightCurlyBraceSameLineAsNextBlockCheck.java | 45 / 19 / 26 (57.778%) | 45 / 45 / 0 (0.000%) | 45 / 19 / 26 (57.778%) | 45 / 45 / 0 (0.000%) |
| java-checks/src/main/java/org/sonar/java/checks/RightCurlyBraceStartLineCheck.java | 106 / 43 / 63 (59.434%) | 106 / 106 / 0 (0.000%) | 106 / 43 / 63 (59.434%) | 106 / 106 / 0 (0.000%) |
| java-checks/src/main/java/org/sonar/java/checks/RightCurlyBraceToNextBlockAbstractVisitor.java | 80 / 42 / 38 (47.500%) | 80 / 80 / 0 (0.000%) | 80 / 42 / 38 (47.500%) | 80 / 80 / 0 (0.000%) |
| java-checks/src/main/java/org/sonar/java/checks/RunFinalizersCheck.java | 38 / 19 / 19 (50.000%) | 38 / 38 / 0 (0.000%) | 38 / 19 / 19 (50.000%) | 38 / 38 / 0 (0.000%) |
| java-checks/src/main/java/org/sonar/java/checks/S9395Check.java | 310 / 181 / 129 (41.613%) | 310 / 310 / 0 (0.000%) | 310 / 181 / 129 (41.613%) | 310 / 310 / 0 (0.000%) |
| java-checks/src/main/java/org/sonar/java/checks/S9411Check.java | 436 / 231 / 205 (47.018%) | 436 / 436 / 0 (0.000%) | 436 / 231 / 205 (47.018%) | 436 / 436 / 0 (0.000%) |
| java-checks/src/main/java/org/sonar/java/checks/SQLInjectionCheck.java | 443 / 190 / 253 (57.111%) | 443 / 443 / 0 (0.000%) | 443 / 190 / 253 (57.111%) | 443 / 443 / 0 (0.000%) |
| java-checks/src/main/java/org/sonar/java/checks/ScheduledThreadPoolExecutorMaximumPoolSizeCheck.java | 25 / 13 / 12 (48.000%) | 25 / 25 / 0 (0.000%) | 25 / 13 / 12 (48.000%) | 25 / 25 / 0 (0.000%) |
| java-checks/src/main/java/org/sonar/java/checks/ScheduledThreadPoolExecutorZeroCheck.java | 57 / 27 / 30 (52.632%) | 57 / 57 / 0 (0.000%) | 57 / 27 / 30 (52.632%) | 57 / 57 / 0 (0.000%) |
| java-checks/src/main/java/org/sonar/java/checks/ScopedValueStableReferenceCheck.java | 101 / 56 / 45 (44.554%) | 101 / 101 / 0 (0.000%) | 101 / 56 / 45 (44.554%) | 101 / 101 / 0 (0.000%) |
| java-checks/src/main/java/org/sonar/java/checks/SelectorMethodArgumentCheck.java | 146 / 87 / 59 (40.411%) | 146 / 146 / 0 (0.000%) | 146 / 87 / 59 (40.411%) | 146 / 146 / 0 (0.000%) |
| java-checks/src/main/java/org/sonar/java/checks/SelfAssignmentCheck.java | 259 / 81 / 178 (68.726%) | 259 / 259 / 0 (0.000%) | 259 / 81 / 178 (68.726%) | 259 / 259 / 0 (0.000%) |
| java-checks/src/main/java/org/sonar/java/checks/ServletInstanceFieldCheck.java | 209 / 106 / 103 (49.282%) | 209 / 209 / 0 (0.000%) | 209 / 106 / 103 (49.282%) | 209 / 209 / 0 (0.000%) |
| java-checks/src/main/java/org/sonar/java/checks/ServletMethodsExceptionsThrownCheck.java | 281 / 138 / 143 (50.890%) | 281 / 281 / 0 (0.000%) | 281 / 138 / 143 (50.890%) | 281 / 281 / 0 (0.000%) |
| java-checks/src/main/java/org/sonar/java/checks/SeveralBreakOrContinuePerLoopCheck.java | 174 / 117 / 57 (32.759%) | 174 / 174 / 0 (0.000%) | 174 / 117 / 57 (32.759%) | 174 / 174 / 0 (0.000%) |
| java-checks/src/main/java/org/sonar/java/checks/ShiftOnIntOrLongCheck.java | 289 / 196 / 93 (32.180%) | 289 / 289 / 0 (0.000%) | 289 / 196 / 93 (32.180%) | 289 / 289 / 0 (0.000%) |
| java-checks/src/main/java/org/sonar/java/checks/SillyEqualsCheck.java | 208 / 115 / 93 (44.712%) | 208 / 208 / 0 (0.000%) | 208 / 115 / 93 (44.712%) | 208 / 208 / 0 (0.000%) |
| java-checks/src/main/java/org/sonar/java/checks/SillyStringOperationsCheck.java | 262 / 144 / 118 (45.038%) | 262 / 262 / 0 (0.000%) | 262 / 144 / 118 (45.038%) | 262 / 262 / 0 (0.000%) |
| java-checks/src/main/java/org/sonar/java/checks/SimpleClassNameCheck.java | 127 / 54 / 73 (57.480%) | 127 / 127 / 0 (0.000%) | 127 / 54 / 73 (57.480%) | 127 / 127 / 0 (0.000%) |
| java-checks/src/main/java/org/sonar/java/checks/SimpleStringLiteralForSingleLineStringsCheck.java | 38 / 26 / 12 (31.579%) | 38 / 38 / 0 (0.000%) | 38 / 26 / 12 (31.579%) | 38 / 38 / 0 (0.000%) |
| java-checks/src/main/java/org/sonar/java/checks/SimpleTemporalInstantiationCheck.java | 131 / 54 / 77 (58.779%) | 131 / 131 / 0 (0.000%) | 131 / 54 / 77 (58.779%) | 131 / 131 / 0 (0.000%) |
| java-checks/src/main/java/org/sonar/java/checks/SingleIfInsteadOfPatternMatchGuardCheck.java | 189 / 101 / 88 (46.561%) | 189 / 189 / 0 (0.000%) | 189 / 101 / 88 (46.561%) | 189 / 189 / 0 (0.000%) |
| java-checks/src/main/java/org/sonar/java/checks/SortedCollectionWithNonComparableTypeCheck.java | 372 / 196 / 176 (47.312%) | 372 / 372 / 0 (0.000%) | 372 / 196 / 176 (47.312%) | 372 / 372 / 0 (0.000%) |
| java-checks/src/main/java/org/sonar/java/checks/SpecializedFunctionalInterfacesCheck.java | 538 / 381 / 157 (29.182%) | 538 / 538 / 0 (0.000%) | 538 / 381 / 157 (29.182%) | 538 / 538 / 0 (0.000%) |
| java-checks/src/main/java/org/sonar/java/checks/StandardCharsetsConstantsCheck.java | 876 / 484 / 392 (44.749%) | 876 / 876 / 0 (0.000%) | 876 / 484 / 392 (44.749%) | 876 / 876 / 0 (0.000%) |
| java-checks/src/main/java/org/sonar/java/checks/StandardFunctionalInterfaceCheck.java | 483 / 363 / 120 (24.845%) | 483 / 483 / 0 (0.000%) | 483 / 363 / 120 (24.845%) | 483 / 483 / 0 (0.000%) |
| java-checks/src/main/java/org/sonar/java/checks/StartupAnnotationCheck.java | 102 / 44 / 58 (56.863%) | 102 / 102 / 0 (0.000%) | 102 / 44 / 58 (56.863%) | 102 / 102 / 0 (0.000%) |
| java-checks/src/main/java/org/sonar/java/checks/StatelessBeanInstanceFieldCheck.java | 88 / 52 / 36 (40.909%) | 88 / 88 / 0 (0.000%) | 88 / 52 / 36 (40.909%) | 88 / 88 / 0 (0.000%) |
| java-checks/src/main/java/org/sonar/java/checks/StaticFieldInitializationCheck.java | 219 / 123 / 96 (43.836%) | 219 / 219 / 0 (0.000%) | 219 / 123 / 96 (43.836%) | 219 / 219 / 0 (0.000%) |
| java-checks/src/main/java/org/sonar/java/checks/StaticFieldUpateCheck.java | 178 / 93 / 85 (47.753%) | 178 / 178 / 0 (0.000%) | 178 / 93 / 85 (47.753%) | 178 / 178 / 0 (0.000%) |
| java-checks/src/main/java/org/sonar/java/checks/StaticFieldUpdateInConstructorCheck.java | 160 / 76 / 84 (52.500%) | 160 / 160 / 0 (0.000%) | 160 / 76 / 84 (52.500%) | 160 / 160 / 0 (0.000%) |
| java-checks/src/main/java/org/sonar/java/checks/StaticImportCountCheck.java | 84 / 46 / 38 (45.238%) | 84 / 84 / 0 (0.000%) | 84 / 46 / 38 (45.238%) | 84 / 84 / 0 (0.000%) |
| java-checks/src/main/java/org/sonar/java/checks/StaticMemberAccessCheck.java | 176 / 90 / 86 (48.864%) | 176 / 176 / 0 (0.000%) | 176 / 90 / 86 (48.864%) | 176 / 176 / 0 (0.000%) |
| java-checks/src/main/java/org/sonar/java/checks/StaticMembersAccessCheck.java | 137 / 62 / 75 (54.745%) | 137 / 137 / 0 (0.000%) | 137 / 62 / 75 (54.745%) | 137 / 137 / 0 (0.000%) |
| java-checks/src/main/java/org/sonar/java/checks/StaticMethodCheck.java | 517 / 278 / 239 (46.228%) | 517 / 517 / 0 (0.000%) | 517 / 278 / 239 (46.228%) | 517 / 517 / 0 (0.000%) |
| java-checks/src/main/java/org/sonar/java/checks/StaticMethodHidingCheck.java | 193 / 100 / 93 (48.187%) | 193 / 193 / 0 (0.000%) | 193 / 100 / 93 (48.187%) | 193 / 193 / 0 (0.000%) |
| java-checks/src/main/java/org/sonar/java/checks/StaticMultithreadedUnsafeFieldsCheck.java | 162 / 84 / 78 (48.148%) | 162 / 162 / 0 (0.000%) | 162 / 84 / 78 (48.148%) | 162 / 162 / 0 (0.000%) |
| java-checks/src/main/java/org/sonar/java/checks/StreamForeachCheck.java | 95 / 40 / 55 (57.895%) | 95 / 95 / 0 (0.000%) | 95 / 40 / 55 (57.895%) | 95 / 95 / 0 (0.000%) |
| java-checks/src/main/java/org/sonar/java/checks/StreamPeekCheck.java | 25 / 13 / 12 (48.000%) | 25 / 25 / 0 (0.000%) | 25 / 13 / 12 (48.000%) | 25 / 25 / 0 (0.000%) |
| java-checks/src/main/java/org/sonar/java/checks/StreamReadResultCastCheck.java | 67 / 27 / 40 (59.701%) | 67 / 67 / 0 (0.000%) | 67 / 27 / 40 (59.701%) | 67 / 67 / 0 (0.000%) |
| java-checks/src/main/java/org/sonar/java/checks/StringBufferAndBuilderConcatenationCheck.java | 195 / 95 / 100 (51.282%) | 195 / 195 / 0 (0.000%) | 195 / 95 / 100 (51.282%) | 195 / 195 / 0 (0.000%) |
| java-checks/src/main/java/org/sonar/java/checks/StringBufferAndBuilderWithCharCheck.java | 84 / 47 / 37 (44.048%) | 84 / 84 / 0 (0.000%) | 84 / 47 / 37 (44.048%) | 84 / 84 / 0 (0.000%) |
| java-checks/src/main/java/org/sonar/java/checks/StringCallsBeyondBoundsCheck.java | 364 / 118 / 246 (67.582%) | 364 / 364 / 0 (0.000%) | 364 / 118 / 246 (67.582%) | 364 / 364 / 0 (0.000%) |
| java-checks/src/main/java/org/sonar/java/checks/StringConcatToTextBlockCheck.java | 127 / 92 / 35 (27.559%) | 127 / 127 / 0 (0.000%) | 127 / 92 / 35 (27.559%) | 127 / 127 / 0 (0.000%) |
| java-checks/src/main/java/org/sonar/java/checks/StringConcatenationInLoopCheck.java | 249 / 134 / 115 (46.185%) | 249 / 249 / 0 (0.000%) | 249 / 134 / 115 (46.185%) | 249 / 249 / 0 (0.000%) |
| java-checks/src/main/java/org/sonar/java/checks/StringEqualsCharSequenceCheck.java | 68 / 27 / 41 (60.294%) | 68 / 68 / 0 (0.000%) | 68 / 27 / 41 (60.294%) | 68 / 68 / 0 (0.000%) |
| java-checks/src/main/java/org/sonar/java/checks/StringFormatCheck.java | 227 / 176 / 51 (22.467%) | 227 / 227 / 0 (0.000%) | 227 / 176 / 51 (22.467%) | 227 / 227 / 0 (0.000%) |
| java-checks/src/main/java/org/sonar/java/checks/StringIndexOfRangesCheck.java | 306 / 204 / 102 (33.333%) | 306 / 306 / 0 (0.000%) | 306 / 204 / 102 (33.333%) | 306 / 306 / 0 (0.000%) |
| java-checks/src/main/java/org/sonar/java/checks/StringIsEmptyCheck.java | 310 / 171 / 139 (44.839%) | 310 / 310 / 0 (0.000%) | 310 / 171 / 139 (44.839%) | 310 / 310 / 0 (0.000%) |
| java-checks/src/main/java/org/sonar/java/checks/StringLiteralDuplicatedCheck.java | 435 / 193 / 242 (55.632%) | 435 / 435 / 0 (0.000%) | 435 / 193 / 242 (55.632%) | 435 / 435 / 0 (0.000%) |
| java-checks/src/main/java/org/sonar/java/checks/StringLiteralInsideEqualsCheck.java | 135 / 71 / 64 (47.407%) | 135 / 135 / 0 (0.000%) | 135 / 71 / 64 (47.407%) | 135 / 135 / 0 (0.000%) |
| java-checks/src/main/java/org/sonar/java/checks/StringMethodsWithLocaleCheck.java | 127 / 81 / 46 (36.220%) | 127 / 127 / 0 (0.000%) | 127 / 81 / 46 (36.220%) | 127 / 127 / 0 (0.000%) |
| java-checks/src/main/java/org/sonar/java/checks/StringOffsetMethodsCheck.java | 82 / 36 / 46 (56.098%) | 82 / 82 / 0 (0.000%) | 82 / 36 / 46 (56.098%) | 82 / 82 / 0 (0.000%) |
| java-checks/src/main/java/org/sonar/java/checks/StringPrimitiveConstructorCheck.java | 239 / 151 / 88 (36.820%) | 239 / 239 / 0 (0.000%) | 239 / 151 / 88 (36.820%) | 239 / 239 / 0 (0.000%) |
| java-checks/src/main/java/org/sonar/java/checks/StringToPrimitiveConversionCheck.java | 274 / 160 / 114 (41.606%) | 274 / 274 / 0 (0.000%) | 274 / 160 / 114 (41.606%) | 274 / 274 / 0 (0.000%) |
| java-checks/src/main/java/org/sonar/java/checks/StringToStringCheck.java | 197 / 99 / 98 (49.746%) | 197 / 197 / 0 (0.000%) | 197 / 99 / 98 (49.746%) | 197 / 197 / 0 (0.000%) |
| java-checks/src/main/java/org/sonar/java/checks/StrongCipherAlgorithmCheck.java | 95 / 64 / 31 (32.632%) | 95 / 95 / 0 (0.000%) | 95 / 64 / 31 (32.632%) | 95 / 95 / 0 (0.000%) |
| java-checks/src/main/java/org/sonar/java/checks/SubClassStaticReferenceCheck.java | 187 / 93 / 94 (50.267%) | 187 / 187 / 0 (0.000%) | 187 / 93 / 94 (50.267%) | 187 / 187 / 0 (0.000%) |
| java-checks/src/main/java/org/sonar/java/checks/SunPackagesUsedCheck.java | 99 / 71 / 28 (28.283%) | 99 / 99 / 0 (0.000%) | 99 / 71 / 28 (28.283%) | 99 / 99 / 0 (0.000%) |
| java-checks/src/main/java/org/sonar/java/checks/SuppressWarningsCheck.java | 177 / 135 / 42 (23.729%) | 177 / 177 / 0 (0.000%) | 177 / 135 / 42 (23.729%) | 177 / 177 / 0 (0.000%) |
| java-checks/src/main/java/org/sonar/java/checks/SuspiciousListRemoveCheck.java | 218 / 116 / 102 (46.789%) | 218 / 218 / 0 (0.000%) | 218 / 116 / 102 (46.789%) | 218 / 218 / 0 (0.000%) |
| java-checks/src/main/java/org/sonar/java/checks/SwitchAtLeastThreeCasesCheck.java | 122 / 60 / 62 (50.820%) | 122 / 122 / 0 (0.000%) | 122 / 60 / 62 (50.820%) | 122 / 122 / 0 (0.000%) |
| java-checks/src/main/java/org/sonar/java/checks/SwitchCaseTooBigCheck.java | 63 / 25 / 38 (60.317%) | 63 / 63 / 0 (0.000%) | 63 / 25 / 38 (60.317%) | 63 / 63 / 0 (0.000%) |
| java-checks/src/main/java/org/sonar/java/checks/SwitchCaseWithoutBreakCheck.java | 257 / 111 / 146 (56.809%) | 257 / 257 / 0 (0.000%) | 257 / 111 / 146 (56.809%) | 257 / 257 / 0 (0.000%) |
| java-checks/src/main/java/org/sonar/java/checks/SwitchCasesShouldBeCommaSeparatedCheck.java | 110 / 50 / 60 (54.545%) | 110 / 110 / 0 (0.000%) | 110 / 50 / 60 (54.545%) | 110 / 110 / 0 (0.000%) |
| java-checks/src/main/java/org/sonar/java/checks/SwitchDefaultLastCaseCheck.java | 95 / 55 / 40 (42.105%) | 95 / 95 / 0 (0.000%) | 95 / 55 / 40 (42.105%) | 95 / 95 / 0 (0.000%) |
| java-checks/src/main/java/org/sonar/java/checks/SwitchInsteadOfIfSequenceCheck.java | 229 / 119 / 110 (48.035%) | 229 / 229 / 0 (0.000%) | 229 / 119 / 110 (48.035%) | 229 / 229 / 0 (0.000%) |
| java-checks/src/main/java/org/sonar/java/checks/SwitchLastCaseIsDefaultCheck.java | 160 / 74 / 86 (53.750%) | 160 / 160 / 0 (0.000%) | 160 / 74 / 86 (53.750%) | 160 / 160 / 0 (0.000%) |
| java-checks/src/main/java/org/sonar/java/checks/SwitchRedundantKeywordCheck.java | 163 / 82 / 81 (49.693%) | 163 / 163 / 0 (0.000%) | 163 / 82 / 81 (49.693%) | 163 / 163 / 0 (0.000%) |
| java-checks/src/main/java/org/sonar/java/checks/SwitchWithLabelsCheck.java | 72 / 38 / 34 (47.222%) | 72 / 72 / 0 (0.000%) | 72 / 38 / 34 (47.222%) | 72 / 72 / 0 (0.000%) |
| java-checks/src/main/java/org/sonar/java/checks/SwitchWithTooManyCasesCheck.java | 91 / 46 / 45 (49.451%) | 91 / 91 / 0 (0.000%) | 91 / 51 / 40 (43.956%) | 91 / 91 / 0 (0.000%) |
| java-checks/src/main/java/org/sonar/java/checks/SymmetricEqualsCheck.java | 145 / 77 / 68 (46.897%) | 145 / 145 / 0 (0.000%) | 145 / 77 / 68 (46.897%) | 145 / 145 / 0 (0.000%) |
| java-checks/src/main/java/org/sonar/java/checks/SyncGetterAndSetterCheck.java | 222 / 113 / 109 (49.099%) | 222 / 222 / 0 (0.000%) | 222 / 113 / 109 (49.099%) | 222 / 222 / 0 (0.000%) |
| java-checks/src/main/java/org/sonar/java/checks/SynchronizationOnStringOrBoxedCheck.java | 121 / 58 / 63 (52.066%) | 121 / 121 / 0 (0.000%) | 121 / 58 / 63 (52.066%) | 121 / 121 / 0 (0.000%) |
| java-checks/src/main/java/org/sonar/java/checks/SynchronizedClassUsageCheck.java | 341 / 218 / 123 (36.070%) | 341 / 341 / 0 (0.000%) | 341 / 218 / 123 (36.070%) | 341 / 341 / 0 (0.000%) |
| java-checks/src/main/java/org/sonar/java/checks/SynchronizedFieldAssignmentCheck.java | 203 / 104 / 99 (48.768%) | 203 / 203 / 0 (0.000%) | 203 / 104 / 99 (48.768%) | 203 / 203 / 0 (0.000%) |
| java-checks/src/main/java/org/sonar/java/checks/SynchronizedOnConcurrentObjectCheck.java | 76 / 55 / 21 (27.632%) | 76 / 76 / 0 (0.000%) | 76 / 55 / 21 (27.632%) | 76 / 76 / 0 (0.000%) |
| java-checks/src/main/java/org/sonar/java/checks/SynchronizedOverrideCheck.java | 72 / 40 / 32 (44.444%) | 72 / 72 / 0 (0.000%) | 72 / 40 / 32 (44.444%) | 72 / 72 / 0 (0.000%) |
| java-checks/src/main/java/org/sonar/java/checks/SystemExitCalledCheck.java | 137 / 86 / 51 (37.226%) | 137 / 137 / 0 (0.000%) | 137 / 86 / 51 (37.226%) | 137 / 137 / 0 (0.000%) |
| java-checks/src/main/java/org/sonar/java/checks/SystemOutOrErrUsageCheck.java | 123 / 66 / 57 (46.341%) | 123 / 123 / 0 (0.000%) | 123 / 66 / 57 (46.341%) | 123 / 123 / 0 (0.000%) |
| java-checks/src/main/java/org/sonar/java/checks/TabCharacterCheck.java | 36 / 28 / 8 (22.222%) | 36 / 36 / 0 (0.000%) | 36 / 28 / 8 (22.222%) | 36 / 36 / 0 (0.000%) |
| java-checks/src/main/java/org/sonar/java/checks/TernaryOperatorCheck.java | 26 / 15 / 11 (42.308%) | 26 / 26 / 0 (0.000%) | 26 / 15 / 11 (42.308%) | 26 / 26 / 0 (0.000%) |
| java-checks/src/main/java/org/sonar/java/checks/TernaryOperatorSameOperationCheck.java | 266 / 145 / 121 (45.489%) | 266 / 266 / 0 (0.000%) | 266 / 145 / 121 (45.489%) | 266 / 266 / 0 (0.000%) |
| java-checks/src/main/java/org/sonar/java/checks/TestsInSeparateFolderCheck.java | 31 / 18 / 13 (41.935%) | 31 / 31 / 0 (0.000%) | 31 / 18 / 13 (41.935%) | 31 / 31 / 0 (0.000%) |
| java-checks/src/main/java/org/sonar/java/checks/TextBlockTabsAndSpacesCheck.java | 84 / 68 / 16 (19.048%) | 84 / 84 / 0 (0.000%) | 84 / 68 / 16 (19.048%) | 84 / 84 / 0 (0.000%) |
| java-checks/src/main/java/org/sonar/java/checks/TextBlocksInComplexExpressionsCheck.java | 94 / 62 / 32 (34.043%) | 94 / 94 / 0 (0.000%) | 94 / 62 / 32 (34.043%) | 94 / 94 / 0 (0.000%) |
| java-checks/src/main/java/org/sonar/java/checks/ThisExposedFromConstructorCheck.java | 116 / 60 / 56 (48.276%) | 116 / 116 / 0 (0.000%) | 116 / 60 / 56 (48.276%) | 116 / 116 / 0 (0.000%) |
| java-checks/src/main/java/org/sonar/java/checks/ThreadAsRunnableArgumentCheck.java | 289 / 161 / 128 (44.291%) | 289 / 289 / 0 (0.000%) | 289 / 161 / 128 (44.291%) | 289 / 289 / 0 (0.000%) |
| java-checks/src/main/java/org/sonar/java/checks/ThreadLocalCleanupCheck.java | 141 / 47 / 94 (66.667%) | 141 / 141 / 0 (0.000%) | 141 / 47 / 94 (66.667%) | 141 / 141 / 0 (0.000%) |
| java-checks/src/main/java/org/sonar/java/checks/ThreadLocalWithInitialCheck.java | 75 / 26 / 49 (65.333%) | 75 / 75 / 0 (0.000%) | 75 / 26 / 49 (65.333%) | 75 / 75 / 0 (0.000%) |
| java-checks/src/main/java/org/sonar/java/checks/ThreadOverridesRunCheck.java | 175 / 94 / 81 (46.286%) | 175 / 175 / 0 (0.000%) | 175 / 94 / 81 (46.286%) | 175 / 175 / 0 (0.000%) |
| java-checks/src/main/java/org/sonar/java/checks/ThreadRunCheck.java | 70 / 30 / 40 (57.143%) | 70 / 70 / 0 (0.000%) | 70 / 30 / 40 (57.143%) | 70 / 70 / 0 (0.000%) |
| java-checks/src/main/java/org/sonar/java/checks/ThreadSleepCheck.java | 26 / 13 / 13 (50.000%) | 26 / 26 / 0 (0.000%) | 26 / 13 / 13 (50.000%) | 26 / 26 / 0 (0.000%) |
| java-checks/src/main/java/org/sonar/java/checks/ThreadStartedInConstructorCheck.java | 103 / 42 / 61 (59.223%) | 103 / 103 / 0 (0.000%) | 103 / 42 / 61 (59.223%) | 103 / 103 / 0 (0.000%) |
| java-checks/src/main/java/org/sonar/java/checks/ThreadWaitCallCheck.java | 34 / 13 / 21 (61.765%) | 34 / 34 / 0 (0.000%) | 34 / 13 / 21 (61.765%) | 34 / 34 / 0 (0.000%) |
| java-checks/src/main/java/org/sonar/java/checks/ThrowCheckedExceptionCheck.java | 105 / 57 / 48 (45.714%) | 105 / 105 / 0 (0.000%) | 105 / 57 / 48 (45.714%) | 105 / 105 / 0 (0.000%) |
| java-checks/src/main/java/org/sonar/java/checks/ThrowsFromFinallyCheck.java | 66 / 46 / 20 (30.303%) | 66 / 66 / 0 (0.000%) | 66 / 46 / 20 (30.303%) | 66 / 66 / 0 (0.000%) |
| java-checks/src/main/java/org/sonar/java/checks/ThrowsSeveralCheckedExceptionCheck.java | 107 / 62 / 45 (42.056%) | 107 / 107 / 0 (0.000%) | 107 / 62 / 45 (42.056%) | 107 / 107 / 0 (0.000%) |
| java-checks/src/main/java/org/sonar/java/checks/TimeZoneIdCheck.java | 128 / 83 / 45 (35.156%) | 128 / 128 / 0 (0.000%) | 128 / 83 / 45 (35.156%) | 128 / 128 / 0 (0.000%) |
| java-checks/src/main/java/org/sonar/java/checks/ToArrayCheck.java | 128 / 56 / 72 (56.250%) | 128 / 128 / 0 (0.000%) | 128 / 56 / 72 (56.250%) | 128 / 128 / 0 (0.000%) |
| java-checks/src/main/java/org/sonar/java/checks/ToStringReturningNullCheck.java | 123 / 62 / 61 (49.593%) | 123 / 123 / 0 (0.000%) | 123 / 62 / 61 (49.593%) | 123 / 123 / 0 (0.000%) |
| java-checks/src/main/java/org/sonar/java/checks/ToStringUsingBoxingCheck.java | 265 / 120 / 145 (54.717%) | 265 / 265 / 0 (0.000%) | 265 / 120 / 145 (54.717%) | 265 / 265 / 0 (0.000%) |
| java-checks/src/main/java/org/sonar/java/checks/TodoTagPresenceCheck.java | 35 / 24 / 11 (31.429%) | 35 / 35 / 0 (0.000%) | 35 / 24 / 11 (31.429%) | 35 / 35 / 0 (0.000%) |
| java-checks/src/main/java/org/sonar/java/checks/TooLongLineCheck.java | 139 / 104 / 35 (25.180%) | 139 / 139 / 0 (0.000%) | 139 / 104 / 35 (25.180%) | 139 / 139 / 0 (0.000%) |
| java-checks/src/main/java/org/sonar/java/checks/TooManyLinesOfCodeInFileCheck.java | 48 / 35 / 13 (27.083%) | 48 / 48 / 0 (0.000%) | 48 / 35 / 13 (27.083%) | 48 / 48 / 0 (0.000%) |
| java-checks/src/main/java/org/sonar/java/checks/TooManyMethodsCheck.java | 160 / 81 / 79 (49.375%) | 160 / 160 / 0 (0.000%) | 160 / 81 / 79 (49.375%) | 160 / 160 / 0 (0.000%) |
| java-checks/src/main/java/org/sonar/java/checks/TooManyParametersCheck.java | 173 / 94 / 79 (45.665%) | 173 / 173 / 0 (0.000%) | 173 / 94 / 79 (45.665%) | 173 / 173 / 0 (0.000%) |
| java-checks/src/main/java/org/sonar/java/checks/TooManyStatementsPerLineCheck.java | 366 / 216 / 150 (40.984%) | 366 / 366 / 0 (0.000%) | 366 / 216 / 150 (40.984%) | 366 / 366 / 0 (0.000%) |
| java-checks/src/main/java/org/sonar/java/checks/TrailingCommentCheck.java | 131 / 85 / 46 (35.115%) | 131 / 131 / 0 (0.000%) | 131 / 85 / 46 (35.115%) | 131 / 131 / 0 (0.000%) |
| java-checks/src/main/java/org/sonar/java/checks/TransientFieldInNonSerializableCheck.java | 72 / 37 / 35 (48.611%) | 72 / 72 / 0 (0.000%) | 72 / 37 / 35 (48.611%) | 72 / 72 / 0 (0.000%) |
| java-checks/src/main/java/org/sonar/java/checks/TryWithResourcesCheck.java | 284 / 116 / 168 (59.155%) | 284 / 284 / 0 (0.000%) | 284 / 116 / 168 (59.155%) | 284 / 284 / 0 (0.000%) |
| java-checks/src/main/java/org/sonar/java/checks/TypeParametersShadowingCheck.java | 138 / 75 / 63 (45.652%) | 138 / 138 / 0 (0.000%) | 138 / 75 / 63 (45.652%) | 138 / 138 / 0 (0.000%) |
| java-checks/src/main/java/org/sonar/java/checks/TypeUpperBoundNotFinalCheck.java | 154 / 73 / 81 (52.597%) | 154 / 154 / 0 (0.000%) | 154 / 73 / 81 (52.597%) | 154 / 154 / 0 (0.000%) |
| java-checks/src/main/java/org/sonar/java/checks/URLHashCodeAndEqualsCheck.java | 102 / 50 / 52 (50.980%) | 102 / 102 / 0 (0.000%) | 102 / 50 / 52 (50.980%) | 102 / 102 / 0 (0.000%) |
| java-checks/src/main/java/org/sonar/java/checks/UnderscoreMisplacedOnNumberCheck.java | 106 / 91 / 15 (14.151%) | 106 / 106 / 0 (0.000%) | 106 / 91 / 15 (14.151%) | 106 / 106 / 0 (0.000%) |
| java-checks/src/main/java/org/sonar/java/checks/UnderscoreOnNumberCheck.java | 142 / 112 / 30 (21.127%) | 142 / 142 / 0 (0.000%) | 142 / 112 / 30 (21.127%) | 142 / 142 / 0 (0.000%) |
| java-checks/src/main/java/org/sonar/java/checks/UndocumentedApiCheck.java | 454 / 293 / 161 (35.463%) | 454 / 454 / 0 (0.000%) | 454 / 297 / 157 (34.581%) | 454 / 454 / 0 (0.000%) |
| java-checks/src/main/java/org/sonar/java/checks/UnnamedVariableShouldUseVarCheck.java | 83 / 41 / 42 (50.602%) | 83 / 83 / 0 (0.000%) | 83 / 41 / 42 (50.602%) | 83 / 83 / 0 (0.000%) |
| java-checks/src/main/java/org/sonar/java/checks/UnnecessaryBitOperationCheck.java | 84 / 41 / 43 (51.190%) | 84 / 84 / 0 (0.000%) | 84 / 41 / 43 (51.190%) | 84 / 84 / 0 (0.000%) |
| java-checks/src/main/java/org/sonar/java/checks/UnnecessaryEscapeSequencesInTextBlockCheck.java | 79 / 63 / 16 (20.253%) | 79 / 79 / 0 (0.000%) | 79 / 63 / 16 (20.253%) | 79 / 79 / 0 (0.000%) |
| java-checks/src/main/java/org/sonar/java/checks/UnnecessarySemicolonCheck.java | 40 / 22 / 18 (45.000%) | 40 / 40 / 0 (0.000%) | 40 / 22 / 18 (45.000%) | 40 / 40 / 0 (0.000%) |
| java-checks/src/main/java/org/sonar/java/checks/UnreachableCatchCheck.java | 310 / 137 / 173 (55.806%) | 310 / 310 / 0 (0.000%) | 310 / 137 / 173 (55.806%) | 310 / 310 / 0 (0.000%) |
| java-checks/src/main/java/org/sonar/java/checks/UnsupportedChronoUnitWithInstantCheck.java | 123 / 74 / 49 (39.837%) | 123 / 123 / 0 (0.000%) | 123 / 74 / 49 (39.837%) | 123 / 123 / 0 (0.000%) |
| java-checks/src/main/java/org/sonar/java/checks/UnusedScopedValueWhereResultCheck.java | 261 / 151 / 110 (42.146%) | 261 / 261 / 0 (0.000%) | 261 / 151 / 110 (42.146%) | 261 / 261 / 0 (0.000%) |
| java-checks/src/main/java/org/sonar/java/checks/UppercaseSuffixesCheck.java | 54 / 37 / 17 (31.481%) | 54 / 54 / 0 (0.000%) | 54 / 37 / 17 (31.481%) | 54 / 54 / 0 (0.000%) |
| java-checks/src/main/java/org/sonar/java/checks/UseIsEmptyToTestEmptinessOfStringBuilderCheck.java | 217 / 96 / 121 (55.760%) | 217 / 217 / 0 (0.000%) | 217 / 96 / 121 (55.760%) | 217 / 217 / 0 (0.000%) |
| java-checks/src/main/java/org/sonar/java/checks/UseMotionSensorWithoutGyroscopeCheck.java | 41 / 19 / 22 (53.659%) | 41 / 41 / 0 (0.000%) | 41 / 19 / 22 (53.659%) | 41 / 41 / 0 (0.000%) |
| java-checks/src/main/java/org/sonar/java/checks/UseOfSequentialForSequentialGathererCheck.java | 160 / 100 / 60 (37.500%) | 160 / 160 / 0 (0.000%) | 160 / 100 / 60 (37.500%) | 160 / 160 / 0 (0.000%) |
| java-checks/src/main/java/org/sonar/java/checks/UseSwitchExpressionCheck.java | 224 / 107 / 117 (52.232%) | 224 / 224 / 0 (0.000%) | 224 / 107 / 117 (52.232%) | 224 / 224 / 0 (0.000%) |
| java-checks/src/main/java/org/sonar/java/checks/UseTransformClassInsteadOfBuildCheck.java | 85 / 39 / 46 (54.118%) | 85 / 85 / 0 (0.000%) | 85 / 39 / 46 (54.118%) | 85 / 85 / 0 (0.000%) |
| java-checks/src/main/java/org/sonar/java/checks/UselessExtendsCheck.java | 239 / 128 / 111 (46.444%) | 239 / 239 / 0 (0.000%) | 239 / 128 / 111 (46.444%) | 239 / 239 / 0 (0.000%) |
| java-checks/src/main/java/org/sonar/java/checks/UselessImportCheck.java | 401 / 254 / 147 (36.658%) | 401 / 401 / 0 (0.000%) | 401 / 254 / 147 (36.658%) | 401 / 401 / 0 (0.000%) |
| java-checks/src/main/java/org/sonar/java/checks/UselessIncrementCheck.java | 162 / 71 / 91 (56.173%) | 162 / 162 / 0 (0.000%) | 162 / 71 / 91 (56.173%) | 162 / 162 / 0 (0.000%) |
| java-checks/src/main/java/org/sonar/java/checks/UselessMathematicalComparisonCheck.java | 297 / 206 / 91 (30.640%) | 297 / 297 / 0 (0.000%) | 297 / 206 / 91 (30.640%) | 297 / 297 / 0 (0.000%) |
| java-checks/src/main/java/org/sonar/java/checks/UselessPackageInfoCheck.java | 84 / 70 / 14 (16.667%) | 84 / 84 / 0 (0.000%) | 84 / 70 / 14 (16.667%) | 84 / 84 / 0 (0.000%) |
| java-checks/src/main/java/org/sonar/java/checks/UselessParenthesesCheck.java | 43 / 24 / 19 (44.186%) | 43 / 43 / 0 (0.000%) | 43 / 24 / 19 (44.186%) | 43 / 43 / 0 (0.000%) |
| java-checks/src/main/java/org/sonar/java/checks/UtilityClassWithPublicConstructorCheck.java | 316 / 164 / 152 (48.101%) | 316 / 316 / 0 (0.000%) | 316 / 164 / 152 (48.101%) | 316 / 316 / 0 (0.000%) |
| java-checks/src/main/java/org/sonar/java/checks/ValueBasedObjectIdentityCheck.java | 82 / 42 / 40 (48.780%) | 82 / 82 / 0 (0.000%) | 82 / 42 / 40 (48.780%) | 82 / 82 / 0 (0.000%) |
| java-checks/src/main/java/org/sonar/java/checks/ValueBasedObjectsShouldNotBeSerializedCheck.java | 166 / 65 / 101 (60.843%) | 166 / 166 / 0 (0.000%) | 166 / 65 / 101 (60.843%) | 166 / 166 / 0 (0.000%) |
| java-checks/src/main/java/org/sonar/java/checks/VarArgCheck.java | 50 / 25 / 25 (50.000%) | 50 / 50 / 0 (0.000%) | 50 / 25 / 25 (50.000%) | 50 / 50 / 0 (0.000%) |
| java-checks/src/main/java/org/sonar/java/checks/VarCanBeUsedCheck.java | 267 / 144 / 123 (46.067%) | 267 / 267 / 0 (0.000%) | 267 / 144 / 123 (46.067%) | 267 / 267 / 0 (0.000%) |
| java-checks/src/main/java/org/sonar/java/checks/VariableDeclarationScopeCheck.java | 153 / 101 / 52 (33.987%) | 153 / 153 / 0 (0.000%) | 153 / 101 / 52 (33.987%) | 153 / 153 / 0 (0.000%) |
| java-checks/src/main/java/org/sonar/java/checks/VirtualThreadNotSynchronizedCheck.java | 298 / 166 / 132 (44.295%) | 298 / 298 / 0 (0.000%) | 298 / 166 / 132 (44.295%) | 298 / 298 / 0 (0.000%) |
| java-checks/src/main/java/org/sonar/java/checks/VirtualThreadUnsupportedMethodsCheck.java | 138 / 78 / 60 (43.478%) | 138 / 138 / 0 (0.000%) | 138 / 78 / 60 (43.478%) | 138 / 138 / 0 (0.000%) |
| java-checks/src/main/java/org/sonar/java/checks/VisibleForTestingUsageCheck.java | 150 / 83 / 67 (44.667%) | 150 / 150 / 0 (0.000%) | 150 / 83 / 67 (44.667%) | 150 / 150 / 0 (0.000%) |
| java-checks/src/main/java/org/sonar/java/checks/VolatileNonPrimitiveFieldCheck.java | 154 / 62 / 92 (59.740%) | 154 / 154 / 0 (0.000%) | 154 / 62 / 92 (59.740%) | 154 / 154 / 0 (0.000%) |
| java-checks/src/main/java/org/sonar/java/checks/VolatileVariablesOperationsCheck.java | 248 / 135 / 113 (45.565%) | 248 / 248 / 0 (0.000%) | 248 / 135 / 113 (45.565%) | 248 / 248 / 0 (0.000%) |
| java-checks/src/main/java/org/sonar/java/checks/WaitInSynchronizeCheck.java | 59 / 25 / 34 (57.627%) | 59 / 59 / 0 (0.000%) | 59 / 25 / 34 (57.627%) | 59 / 59 / 0 (0.000%) |
| java-checks/src/main/java/org/sonar/java/checks/WaitInWhileLoopCheck.java | 136 / 63 / 73 (53.676%) | 136 / 136 / 0 (0.000%) | 136 / 63 / 73 (53.676%) | 136 / 136 / 0 (0.000%) |
| java-checks/src/main/java/org/sonar/java/checks/WaitOnConditionCheck.java | 34 / 17 / 17 (50.000%) | 34 / 34 / 0 (0.000%) | 34 / 17 / 17 (50.000%) | 34 / 34 / 0 (0.000%) |
| java-checks/src/main/java/org/sonar/java/checks/WeakSSLContextCheck.java | 242 / 124 / 118 (48.760%) | 242 / 242 / 0 (0.000%) | 242 / 124 / 118 (48.760%) | 242 / 242 / 0 (0.000%) |
| java-checks/src/main/java/org/sonar/java/checks/WildcardImportsShouldNotBeUsedCheck.java | 39 / 23 / 16 (41.026%) | 39 / 39 / 0 (0.000%) | 39 / 23 / 16 (41.026%) | 39 / 39 / 0 (0.000%) |
| java-checks/src/main/java/org/sonar/java/checks/WildcardReturnParameterTypeCheck.java | 102 / 54 / 48 (47.059%) | 102 / 102 / 0 (0.000%) | 102 / 54 / 48 (47.059%) | 102 / 102 / 0 (0.000%) |
| java-checks/src/main/java/org/sonar/java/checks/WrongAssignmentOperatorCheck.java | 105 / 65 / 40 (38.095%) | 105 / 105 / 0 (0.000%) | 105 / 65 / 40 (38.095%) | 105 / 105 / 0 (0.000%) |
| java-checks/src/main/java/org/sonar/java/checks/design/AbstractCouplingChecker.java | 193 / 131 / 62 (32.124%) | 193 / 193 / 0 (0.000%) | 193 / 131 / 62 (32.124%) | 193 / 193 / 0 (0.000%) |
| java-checks/src/main/java/org/sonar/java/checks/design/BrainMethodCheck.java | 208 / 165 / 43 (20.673%) | 208 / 208 / 0 (0.000%) | 208 / 165 / 43 (20.673%) | 208 / 208 / 0 (0.000%) |
| java-checks/src/main/java/org/sonar/java/checks/design/ClassCouplingCheck.java | 102 / 56 / 46 (45.098%) | 102 / 102 / 0 (0.000%) | 102 / 56 / 46 (45.098%) | 102 / 102 / 0 (0.000%) |
| java-checks/src/main/java/org/sonar/java/checks/design/ClassImportCouplingCheck.java | 216 / 107 / 109 (50.463%) | 216 / 216 / 0 (0.000%) | 216 / 107 / 109 (50.463%) | 216 / 216 / 0 (0.000%) |
| java-checks/src/main/java/org/sonar/java/checks/design/SingletonUsageCheck.java | 346 / 131 / 215 (62.139%) | 346 / 346 / 0 (0.000%) | 346 / 131 / 215 (62.139%) | 346 / 346 / 0 (0.000%) |
| java-checks/src/main/java/org/sonar/java/checks/design/package-info.java | 7 / 6 / 1 (14.286%) | 7 / 7 / 0 (0.000%) | 7 / 6 / 1 (14.286%) | 7 / 7 / 0 (0.000%) |
| java-checks/src/main/java/org/sonar/java/checks/helpers/AbstractAssertionVisitor.java | 86 / 62 / 24 (27.907%) | 86 / 86 / 0 (0.000%) | 86 / 62 / 24 (27.907%) | 86 / 86 / 0 (0.000%) |
| java-checks/src/main/java/org/sonar/java/checks/helpers/AnnotationsHelper.java | 26 / 18 / 8 (30.769%) | 26 / 26 / 0 (0.000%) | 26 / 18 / 8 (30.769%) | 26 / 26 / 0 (0.000%) |
| java-checks/src/main/java/org/sonar/java/checks/helpers/AnonymousClassToLambdaUtils.java | 322 / 162 / 160 (49.689%) | 322 / 322 / 0 (0.000%) | 322 / 162 / 160 (49.689%) | 322 / 322 / 0 (0.000%) |
| java-checks/src/main/java/org/sonar/java/checks/helpers/ClassPatternsUtils.java | 168 / 74 / 94 (55.952%) | 168 / 168 / 0 (0.000%) | 168 / 74 / 94 (55.952%) | 168 / 168 / 0 (0.000%) |
| java-checks/src/main/java/org/sonar/java/checks/helpers/ComparisonMethodUtils.java | 101 / 45 / 56 (55.446%) | 101 / 101 / 0 (0.000%) | 101 / 45 / 56 (55.446%) | 101 / 101 / 0 (0.000%) |
| java-checks/src/main/java/org/sonar/java/checks/helpers/DeprecatedCheckerHelper.java | 240 / 149 / 91 (37.917%) | 240 / 240 / 0 (0.000%) | 240 / 149 / 91 (37.917%) | 240 / 240 / 0 (0.000%) |
| java-checks/src/main/java/org/sonar/java/checks/helpers/GeneratedStringLiteralRecognizer.java | 87 / 57 / 30 (34.483%) | 87 / 87 / 0 (0.000%) | 87 / 57 / 30 (34.483%) | 87 / 87 / 0 (0.000%) |
| java-checks/src/main/java/org/sonar/java/checks/helpers/InjectionHelper.java | 24 / 20 / 4 (16.667%) | 24 / 24 / 0 (0.000%) | 24 / 20 / 4 (16.667%) | 24 / 24 / 0 (0.000%) |
| java-checks/src/main/java/org/sonar/java/checks/helpers/IntegerOverflowRange.java | 608 / 455 / 153 (25.164%) | 608 / 608 / 0 (0.000%) | 608 / 455 / 153 (25.164%) | 608 / 608 / 0 (0.000%) |
| java-checks/src/main/java/org/sonar/java/checks/helpers/JavaPropertiesHelper.java | 89 / 46 / 43 (48.315%) | 89 / 89 / 0 (0.000%) | 89 / 46 / 43 (48.315%) | 89 / 89 / 0 (0.000%) |
| java-checks/src/main/java/org/sonar/java/checks/helpers/Javadoc.java | 588 / 533 / 55 (9.354%) | 588 / 588 / 0 (0.000%) | 588 / 533 / 55 (9.354%) | 588 / 588 / 0 (0.000%) |
| java-checks/src/main/java/org/sonar/java/checks/helpers/LatinAlphabetLanguagesHelper.java | 125 / 125 / 0 (0.000%) | 125 / 125 / 0 (0.000%) | 125 / 125 / 0 (0.000%) | 125 / 125 / 0 (0.000%) |
| java-checks/src/main/java/org/sonar/java/checks/helpers/LoggingMatchers.java | 46 / 14 / 32 (69.565%) | 46 / 46 / 0 (0.000%) | 46 / 14 / 32 (69.565%) | 46 / 46 / 0 (0.000%) |
| java-checks/src/main/java/org/sonar/java/checks/helpers/MethodTreeUtils.java | 515 / 331 / 184 (35.728%) | 515 / 515 / 0 (0.000%) | 515 / 331 / 184 (35.728%) | 515 / 515 / 0 (0.000%) |
| java-checks/src/main/java/org/sonar/java/checks/helpers/NullabilityDataUtils.java | 96 / 54 / 42 (43.750%) | 96 / 96 / 0 (0.000%) | 96 / 54 / 42 (43.750%) | 96 / 96 / 0 (0.000%) |
| java-checks/src/main/java/org/sonar/java/checks/helpers/QuickFixHelper.java | 652 / 414 / 238 (36.503%) | 652 / 652 / 0 (0.000%) | 652 / 414 / 238 (36.503%) | 652 / 652 / 0 (0.000%) |
| java-checks/src/main/java/org/sonar/java/checks/helpers/RandomnessDetector.java | 59 / 53 / 6 (10.169%) | 59 / 59 / 0 (0.000%) | 59 / 57 / 2 (3.390%) | 59 / 59 / 0 (0.000%) |
| java-checks/src/main/java/org/sonar/java/checks/helpers/ShannonEntropy.java | 49 / 49 / 0 (0.000%) | 49 / 49 / 0 (0.000%) | 49 / 49 / 0 (0.000%) | 49 / 49 / 0 (0.000%) |
| java-checks/src/main/java/org/sonar/java/checks/helpers/TernaryValue.java | 31 / 31 / 0 (0.000%) | 31 / 31 / 0 (0.000%) | 31 / 31 / 0 (0.000%) | 31 / 31 / 0 (0.000%) |
| java-checks/src/main/java/org/sonar/java/checks/helpers/TryCatchUtils.java | 32 / 17 / 15 (46.875%) | 32 / 32 / 0 (0.000%) | 32 / 17 / 15 (46.875%) | 32 / 32 / 0 (0.000%) |
| java-checks/src/main/java/org/sonar/java/checks/helpers/UnresolvedIdentifiersVisitor.java | 91 / 62 / 29 (31.868%) | 91 / 91 / 0 (0.000%) | 91 / 62 / 29 (31.868%) | 91 / 91 / 0 (0.000%) |
| java-checks/src/main/java/org/sonar/java/checks/helpers/ValueBasedUtils.java | 43 / 36 / 7 (16.279%) | 43 / 43 / 0 (0.000%) | 43 / 36 / 7 (16.279%) | 43 / 43 / 0 (0.000%) |
| java-checks/src/main/java/org/sonar/java/checks/helpers/package-info.java | 6 / 6 / 0 (0.000%) | 6 / 6 / 0 (0.000%) | 6 / 6 / 0 (0.000%) | 6 / 6 / 0 (0.000%) |
| java-checks/src/main/java/org/sonar/java/checks/naming/BadAbstractClassNameCheck.java | 89 / 65 / 24 (26.966%) | 89 / 89 / 0 (0.000%) | 89 / 65 / 24 (26.966%) | 89 / 89 / 0 (0.000%) |
| java-checks/src/main/java/org/sonar/java/checks/naming/BadClassNameCheck.java | 91 / 65 / 26 (28.571%) | 91 / 91 / 0 (0.000%) | 91 / 65 / 26 (28.571%) | 91 / 91 / 0 (0.000%) |
| java-checks/src/main/java/org/sonar/java/checks/naming/BadConstantNameCheck.java | 146 / 86 / 60 (41.096%) | 146 / 146 / 0 (0.000%) | 146 / 86 / 60 (41.096%) | 146 / 146 / 0 (0.000%) |
| java-checks/src/main/java/org/sonar/java/checks/naming/BadFieldNameCheck.java | 35 / 25 / 10 (28.571%) | 35 / 35 / 0 (0.000%) | 35 / 25 / 10 (28.571%) | 35 / 35 / 0 (0.000%) |
| java-checks/src/main/java/org/sonar/java/checks/naming/BadFieldNameStaticNonFinalCheck.java | 37 / 23 / 14 (37.838%) | 37 / 37 / 0 (0.000%) | 37 / 23 / 14 (37.838%) | 37 / 37 / 0 (0.000%) |
| java-checks/src/main/java/org/sonar/java/checks/naming/BadInterfaceNameCheck.java | 68 / 51 / 17 (25.000%) | 68 / 68 / 0 (0.000%) | 68 / 51 / 17 (25.000%) | 68 / 68 / 0 (0.000%) |
| java-checks/src/main/java/org/sonar/java/checks/naming/BadLocalConstantNameCheck.java | 110 / 59 / 51 (46.364%) | 110 / 110 / 0 (0.000%) | 110 / 59 / 51 (46.364%) | 110 / 110 / 0 (0.000%) |
| java-checks/src/main/java/org/sonar/java/checks/naming/BadLocalVariableNameCheck.java | 187 / 129 / 58 (31.016%) | 187 / 187 / 0 (0.000%) | 187 / 129 / 58 (31.016%) | 187 / 187 / 0 (0.000%) |
| java-checks/src/main/java/org/sonar/java/checks/naming/BadMethodNameCheck.java | 92 / 62 / 30 (32.609%) | 92 / 92 / 0 (0.000%) | 92 / 62 / 30 (32.609%) | 92 / 92 / 0 (0.000%) |
| java-checks/src/main/java/org/sonar/java/checks/naming/BadPackageNameCheck.java | 141 / 115 / 26 (18.440%) | 141 / 141 / 0 (0.000%) | 141 / 115 / 26 (18.440%) | 141 / 141 / 0 (0.000%) |
| java-checks/src/main/java/org/sonar/java/checks/naming/BadTestClassNameCheck.java | 82 / 62 / 20 (24.390%) | 82 / 82 / 0 (0.000%) | 82 / 62 / 20 (24.390%) | 82 / 82 / 0 (0.000%) |
| java-checks/src/main/java/org/sonar/java/checks/naming/BadTestMethodNameCheck.java | 75 / 55 / 20 (26.667%) | 75 / 75 / 0 (0.000%) | 75 / 55 / 20 (26.667%) | 75 / 75 / 0 (0.000%) |
| java-checks/src/main/java/org/sonar/java/checks/naming/BadTypeParameterNameCheck.java | 65 / 49 / 16 (24.615%) | 65 / 65 / 0 (0.000%) | 65 / 49 / 16 (24.615%) | 65 / 65 / 0 (0.000%) |
| java-checks/src/main/java/org/sonar/java/checks/naming/BooleanMethodNameCheck.java | 68 / 44 / 24 (35.294%) | 68 / 68 / 0 (0.000%) | 68 / 44 / 24 (35.294%) | 68 / 68 / 0 (0.000%) |
| java-checks/src/main/java/org/sonar/java/checks/naming/ClassNamedLikeExceptionCheck.java | 96 / 65 / 31 (32.292%) | 96 / 96 / 0 (0.000%) | 96 / 65 / 31 (32.292%) | 96 / 96 / 0 (0.000%) |
| java-checks/src/main/java/org/sonar/java/checks/naming/FieldNameMatchingTypeNameCheck.java | 111 / 70 / 41 (36.937%) | 111 / 111 / 0 (0.000%) | 111 / 70 / 41 (36.937%) | 111 / 111 / 0 (0.000%) |
| java-checks/src/main/java/org/sonar/java/checks/naming/KeywordAsIdentifierCheck.java | 47 / 33 / 14 (29.787%) | 47 / 47 / 0 (0.000%) | 47 / 33 / 14 (29.787%) | 47 / 47 / 0 (0.000%) |
| java-checks/src/main/java/org/sonar/java/checks/naming/MethodNameSameAsClassCheck.java | 60 / 38 / 22 (36.667%) | 60 / 60 / 0 (0.000%) | 60 / 38 / 22 (36.667%) | 60 / 60 / 0 (0.000%) |
| java-checks/src/main/java/org/sonar/java/checks/naming/MethodNamedEqualsCheck.java | 73 / 40 / 33 (45.205%) | 73 / 73 / 0 (0.000%) | 73 / 40 / 33 (45.205%) | 73 / 73 / 0 (0.000%) |
| java-checks/src/main/java/org/sonar/java/checks/naming/MethodNamedHashcodeOrEqualCheck.java | 58 / 40 / 18 (31.034%) | 58 / 58 / 0 (0.000%) | 58 / 40 / 18 (31.034%) | 58 / 58 / 0 (0.000%) |
| java-checks/src/main/java/org/sonar/java/checks/naming/package-info.java | 6 / 6 / 0 (0.000%) | 6 / 6 / 0 (0.000%) | 6 / 6 / 0 (0.000%) | 6 / 6 / 0 (0.000%) |
| java-checks/src/main/java/org/sonar/java/checks/package-info.java | 5 / 5 / 0 (0.000%) | 5 / 5 / 0 (0.000%) | 5 / 5 / 0 (0.000%) | 5 / 5 / 0 (0.000%) |
| java-checks/src/main/java/org/sonar/java/checks/quarkus/CacheKeyGeneratorInstantiableCheck.java | 168 / 91 / 77 (45.833%) | 168 / 168 / 0 (0.000%) | 168 / 91 / 77 (45.833%) | 168 / 168 / 0 (0.000%) |
| java-checks/src/main/java/org/sonar/java/checks/quarkus/SingletonInsteadOfApplicationScopedCheck.java | 155 / 68 / 87 (56.129%) | 155 / 155 / 0 (0.000%) | 155 / 68 / 87 (56.129%) | 155 / 155 / 0 (0.000%) |
| java-checks/src/main/java/org/sonar/java/checks/quarkus/package-info.java | 6 / 6 / 0 (0.000%) | 6 / 6 / 0 (0.000%) | 6 / 6 / 0 (0.000%) | 6 / 6 / 0 (0.000%) |
| java-checks/src/main/java/org/sonar/java/checks/regex/AbstractRedosCheck.java | 545 / 535 / 10 (1.835%) | 545 / 545 / 0 (0.000%) | 545 / 535 / 10 (1.835%) | 545 / 545 / 0 (0.000%) |
| java-checks/src/main/java/org/sonar/java/checks/regex/AbstractRegexCheck.java | 646 / 372 / 274 (42.415%) | 646 / 646 / 0 (0.000%) | 646 / 372 / 274 (42.415%) | 646 / 646 / 0 (0.000%) |
| java-checks/src/main/java/org/sonar/java/checks/regex/AbstractRegexCheckTrackingMatchType.java | 111 / 63 / 48 (43.243%) | 111 / 111 / 0 (0.000%) | 111 / 63 / 48 (43.243%) | 111 / 111 / 0 (0.000%) |
| java-checks/src/main/java/org/sonar/java/checks/regex/AbstractRegexCheckTrackingMatchers.java | 566 / 273 / 293 (51.767%) | 566 / 566 / 0 (0.000%) | 566 / 273 / 293 (51.767%) | 566 / 566 / 0 (0.000%) |
| java-checks/src/main/java/org/sonar/java/checks/regex/AnchorPrecedenceCheck.java | 20 / 17 / 3 (15.000%) | 20 / 20 / 0 (0.000%) | 20 / 17 / 3 (15.000%) | 20 / 20 / 0 (0.000%) |
| java-checks/src/main/java/org/sonar/java/checks/regex/CanonEqFlagInRegexCheck.java | 152 / 93 / 59 (38.816%) | 152 / 152 / 0 (0.000%) | 152 / 93 / 59 (38.816%) | 152 / 152 / 0 (0.000%) |
| java-checks/src/main/java/org/sonar/java/checks/regex/DuplicatesInCharacterClassCheck.java | 20 / 17 / 3 (15.000%) | 20 / 20 / 0 (0.000%) | 20 / 17 / 3 (15.000%) | 20 / 20 / 0 (0.000%) |
| java-checks/src/main/java/org/sonar/java/checks/regex/EmptyLineRegexCheck.java | 356 / 203 / 153 (42.978%) | 356 / 356 / 0 (0.000%) | 356 / 203 / 153 (42.978%) | 356 / 356 / 0 (0.000%) |
| java-checks/src/main/java/org/sonar/java/checks/regex/EmptyRegexGroupCheck.java | 20 / 17 / 3 (15.000%) | 20 / 20 / 0 (0.000%) | 20 / 17 / 3 (15.000%) | 20 / 20 / 0 (0.000%) |
| java-checks/src/main/java/org/sonar/java/checks/regex/EmptyStringRepetitionCheck.java | 20 / 17 / 3 (15.000%) | 20 / 20 / 0 (0.000%) | 20 / 17 / 3 (15.000%) | 20 / 20 / 0 (0.000%) |
| java-checks/src/main/java/org/sonar/java/checks/regex/EscapeSequenceControlCharacterCheck.java | 40 / 37 / 3 (7.500%) | 40 / 40 / 0 (0.000%) | 40 / 37 / 3 (7.500%) | 40 / 40 / 0 (0.000%) |
| java-checks/src/main/java/org/sonar/java/checks/regex/GraphemeClustersInClassesCheck.java | 20 / 17 / 3 (15.000%) | 20 / 20 / 0 (0.000%) | 20 / 17 / 3 (15.000%) | 20 / 20 / 0 (0.000%) |
| java-checks/src/main/java/org/sonar/java/checks/regex/ImpossibleBackReferenceCheck.java | 20 / 17 / 3 (15.000%) | 20 / 20 / 0 (0.000%) | 20 / 17 / 3 (15.000%) | 20 / 20 / 0 (0.000%) |
| java-checks/src/main/java/org/sonar/java/checks/regex/ImpossibleBoundariesCheck.java | 20 / 17 / 3 (15.000%) | 20 / 20 / 0 (0.000%) | 20 / 17 / 3 (15.000%) | 20 / 20 / 0 (0.000%) |
| java-checks/src/main/java/org/sonar/java/checks/regex/InvalidRegexCheck.java | 69 / 58 / 11 (15.942%) | 69 / 69 / 0 (0.000%) | 69 / 58 / 11 (15.942%) | 69 / 69 / 0 (0.000%) |
| java-checks/src/main/java/org/sonar/java/checks/regex/MultipleWhitespaceCheck.java | 20 / 17 / 3 (15.000%) | 20 / 20 / 0 (0.000%) | 20 / 17 / 3 (15.000%) | 20 / 20 / 0 (0.000%) |
| java-checks/src/main/java/org/sonar/java/checks/regex/PossessiveQuantifierContinuationCheck.java | 22 / 19 / 3 (13.636%) | 22 / 22 / 0 (0.000%) | 22 / 19 / 3 (13.636%) | 22 / 22 / 0 (0.000%) |
| java-checks/src/main/java/org/sonar/java/checks/regex/RedosCheck.java | 33 / 26 / 7 (21.212%) | 33 / 33 / 0 (0.000%) | 33 / 26 / 7 (21.212%) | 33 / 33 / 0 (0.000%) |
| java-checks/src/main/java/org/sonar/java/checks/regex/RedundantRegexAlternativesCheck.java | 20 / 17 / 3 (15.000%) | 20 / 20 / 0 (0.000%) | 20 / 17 / 3 (15.000%) | 20 / 20 / 0 (0.000%) |
| java-checks/src/main/java/org/sonar/java/checks/regex/RegexComplexityCheck.java | 276 / 195 / 81 (29.348%) | 276 / 276 / 0 (0.000%) | 276 / 195 / 81 (29.348%) | 276 / 276 / 0 (0.000%) |
| java-checks/src/main/java/org/sonar/java/checks/regex/RegexLookaheadCheck.java | 25 / 21 / 4 (16.000%) | 25 / 25 / 0 (0.000%) | 25 / 21 / 4 (16.000%) | 25 / 25 / 0 (0.000%) |
| java-checks/src/main/java/org/sonar/java/checks/regex/RegexStackOverflowCheck.java | 411 / 404 / 7 (1.703%) | 411 / 411 / 0 (0.000%) | 411 / 404 / 7 (1.703%) | 411 / 411 / 0 (0.000%) |
| java-checks/src/main/java/org/sonar/java/checks/regex/ReluctantQuantifierCheck.java | 20 / 17 / 3 (15.000%) | 20 / 20 / 0 (0.000%) | 20 / 17 / 3 (15.000%) | 20 / 20 / 0 (0.000%) |
| java-checks/src/main/java/org/sonar/java/checks/regex/ReluctantQuantifierWithEmptyContinuationCheck.java | 23 / 19 / 4 (17.391%) | 23 / 23 / 0 (0.000%) | 23 / 19 / 4 (17.391%) | 23 / 23 / 0 (0.000%) |
| java-checks/src/main/java/org/sonar/java/checks/regex/SingleCharCharacterClassCheck.java | 20 / 17 / 3 (15.000%) | 20 / 20 / 0 (0.000%) | 20 / 17 / 3 (15.000%) | 20 / 20 / 0 (0.000%) |
| java-checks/src/main/java/org/sonar/java/checks/regex/SingleCharacterAlternationCheck.java | 20 / 17 / 3 (15.000%) | 20 / 20 / 0 (0.000%) | 20 / 17 / 3 (15.000%) | 20 / 20 / 0 (0.000%) |
| java-checks/src/main/java/org/sonar/java/checks/regex/StringReplaceCheck.java | 70 / 56 / 14 (20.000%) | 70 / 70 / 0 (0.000%) | 70 / 56 / 14 (20.000%) | 70 / 70 / 0 (0.000%) |
| java-checks/src/main/java/org/sonar/java/checks/regex/SuperLinearRegexCheck.java | 39 / 30 / 9 (23.077%) | 39 / 39 / 0 (0.000%) | 39 / 30 / 9 (23.077%) | 39 / 39 / 0 (0.000%) |
| java-checks/src/main/java/org/sonar/java/checks/regex/SuperfluousCurlyBraceCheck.java | 20 / 17 / 3 (15.000%) | 20 / 20 / 0 (0.000%) | 20 / 17 / 3 (15.000%) | 20 / 20 / 0 (0.000%) |
| java-checks/src/main/java/org/sonar/java/checks/regex/UnicodeAwareCharClassesCheck.java | 46 / 33 / 13 (28.261%) | 46 / 46 / 0 (0.000%) | 46 / 33 / 13 (28.261%) | 46 / 46 / 0 (0.000%) |
| java-checks/src/main/java/org/sonar/java/checks/regex/UnicodeCaseCheck.java | 111 / 93 / 18 (16.216%) | 111 / 111 / 0 (0.000%) | 111 / 93 / 18 (16.216%) | 111 / 111 / 0 (0.000%) |
| java-checks/src/main/java/org/sonar/java/checks/regex/UnquantifiedNonCapturingGroupCheck.java | 20 / 17 / 3 (15.000%) | 20 / 20 / 0 (0.000%) | 20 / 17 / 3 (15.000%) | 20 / 20 / 0 (0.000%) |
| java-checks/src/main/java/org/sonar/java/checks/regex/UnusedGroupNamesCheck.java | 367 / 281 / 86 (23.433%) | 367 / 367 / 0 (0.000%) | 367 / 281 / 86 (23.433%) | 367 / 367 / 0 (0.000%) |
| java-checks/src/main/java/org/sonar/java/checks/regex/VerboseRegexCheck.java | 20 / 17 / 3 (15.000%) | 20 / 20 / 0 (0.000%) | 20 / 17 / 3 (15.000%) | 20 / 20 / 0 (0.000%) |
| java-checks/src/main/java/org/sonar/java/checks/regex/package-info.java | 6 / 6 / 0 (0.000%) | 6 / 6 / 0 (0.000%) | 6 / 6 / 0 (0.000%) | 6 / 6 / 0 (0.000%) |
| java-checks/src/main/java/org/sonar/java/checks/security/AndroidBiometricAuthWithoutCryptoCheck.java | 34 / 14 / 20 (58.824%) | 34 / 34 / 0 (0.000%) | 34 / 14 / 20 (58.824%) | 34 / 34 / 0 (0.000%) |
| java-checks/src/main/java/org/sonar/java/checks/security/AndroidBroadcastingCheck.java | 161 / 81 / 80 (49.689%) | 161 / 161 / 0 (0.000%) | 161 / 81 / 80 (49.689%) | 161 / 161 / 0 (0.000%) |
| java-checks/src/main/java/org/sonar/java/checks/security/AndroidExternalStorageCheck.java | 32 / 14 / 18 (56.250%) | 32 / 32 / 0 (0.000%) | 32 / 14 / 18 (56.250%) | 32 / 32 / 0 (0.000%) |
| java-checks/src/main/java/org/sonar/java/checks/security/AndroidMobileDatabaseEncryptionKeysCheck.java | 197 / 87 / 110 (55.838%) | 197 / 197 / 0 (0.000%) | 197 / 87 / 110 (55.838%) | 197 / 197 / 0 (0.000%) |
| java-checks/src/main/java/org/sonar/java/checks/security/AndroidNonAuthenticatedUsersCheck.java | 190 / 90 / 100 (52.632%) | 190 / 190 / 0 (0.000%) | 190 / 90 / 100 (52.632%) | 190 / 190 / 0 (0.000%) |
| java-checks/src/main/java/org/sonar/java/checks/security/AndroidPersistentUniqueIdentifierCheck.java | 184 / 66 / 118 (64.130%) | 184 / 184 / 0 (0.000%) | 184 / 66 / 118 (64.130%) | 184 / 184 / 0 (0.000%) |
| java-checks/src/main/java/org/sonar/java/checks/security/AuthorizationsStrongDecisionsCheck.java | 187 / 116 / 71 (37.968%) | 187 / 187 / 0 (0.000%) | 187 / 116 / 71 (37.968%) | 187 / 187 / 0 (0.000%) |
| java-checks/src/main/java/org/sonar/java/checks/security/CipherBlockChainingCheck.java | 598 / 366 / 232 (38.796%) | 598 / 598 / 0 (0.000%) | 598 / 366 / 232 (38.796%) | 598 / 598 / 0 (0.000%) |
| java-checks/src/main/java/org/sonar/java/checks/security/ClearTextProtocolCheck.java | 185 / 115 / 70 (37.838%) | 185 / 185 / 0 (0.000%) | 185 / 115 / 70 (37.838%) | 185 / 185 / 0 (0.000%) |
| java-checks/src/main/java/org/sonar/java/checks/security/CookieHttpOnlyCheck.java | 873 / 484 / 389 (44.559%) | 873 / 873 / 0 (0.000%) | 873 / 484 / 389 (44.559%) | 873 / 873 / 0 (0.000%) |
| java-checks/src/main/java/org/sonar/java/checks/security/CryptographicKeySizeCheck.java | 200 / 136 / 64 (32.000%) | 200 / 200 / 0 (0.000%) | 200 / 136 / 64 (32.000%) | 200 / 200 / 0 (0.000%) |
| java-checks/src/main/java/org/sonar/java/checks/security/DataHashingCheck.java | 873 / 553 / 320 (36.655%) | 873 / 873 / 0 (0.000%) | 873 / 553 / 320 (36.655%) | 873 / 873 / 0 (0.000%) |
| java-checks/src/main/java/org/sonar/java/checks/security/DebugFeatureEnabledCheck.java | 202 / 88 / 114 (56.436%) | 202 / 202 / 0 (0.000%) | 202 / 88 / 114 (56.436%) | 202 / 202 / 0 (0.000%) |
| java-checks/src/main/java/org/sonar/java/checks/security/DisableAutoEscapingCheck.java | 208 / 108 / 100 (48.077%) | 208 / 208 / 0 (0.000%) | 208 / 108 / 100 (48.077%) | 208 / 208 / 0 (0.000%) |
| java-checks/src/main/java/org/sonar/java/checks/security/DisclosingTechnologyFingerprintsCheck.java | 82 / 32 / 50 (60.976%) | 82 / 82 / 0 (0.000%) | 82 / 32 / 50 (60.976%) | 82 / 82 / 0 (0.000%) |
| java-checks/src/main/java/org/sonar/java/checks/security/EmptyDatabasePasswordCheck.java | 105 / 65 / 40 (38.095%) | 105 / 105 / 0 (0.000%) | 105 / 65 / 40 (38.095%) | 105 / 105 / 0 (0.000%) |
| java-checks/src/main/java/org/sonar/java/checks/security/EncryptionAlgorithmCheck.java | 137 / 98 / 39 (28.467%) | 137 / 137 / 0 (0.000%) | 137 / 98 / 39 (28.467%) | 137 / 137 / 0 (0.000%) |
| java-checks/src/main/java/org/sonar/java/checks/security/ExcessiveContentRequestCheck.java | 583 / 398 / 185 (31.732%) | 583 / 583 / 0 (0.000%) | 583 / 398 / 185 (31.732%) | 583 / 583 / 0 (0.000%) |
| java-checks/src/main/java/org/sonar/java/checks/security/FilePermissionsCheck.java | 253 / 151 / 102 (40.316%) | 253 / 253 / 0 (0.000%) | 253 / 151 / 102 (40.316%) | 253 / 253 / 0 (0.000%) |
| java-checks/src/main/java/org/sonar/java/checks/security/IntegerToHexStringCheck.java | 105 / 31 / 74 (70.476%) | 105 / 105 / 0 (0.000%) | 105 / 31 / 74 (70.476%) | 105 / 105 / 0 (0.000%) |
| java-checks/src/main/java/org/sonar/java/checks/security/JWTWithStrongCipherCheck.java | 238 / 110 / 128 (53.782%) | 238 / 238 / 0 (0.000%) | 238 / 110 / 128 (53.782%) | 238 / 238 / 0 (0.000%) |
| java-checks/src/main/java/org/sonar/java/checks/security/LDAPAuthenticatedConnectionCheck.java | 76 / 38 / 38 (50.000%) | 76 / 76 / 0 (0.000%) | 76 / 38 / 38 (50.000%) | 76 / 76 / 0 (0.000%) |
| java-checks/src/main/java/org/sonar/java/checks/security/LDAPDeserializationCheck.java | 81 / 48 / 33 (40.741%) | 81 / 81 / 0 (0.000%) | 81 / 48 / 33 (40.741%) | 81 / 81 / 0 (0.000%) |
| java-checks/src/main/java/org/sonar/java/checks/security/OpenSAML2AuthenticationBypassCheck.java | 38 / 20 / 18 (47.368%) | 38 / 38 / 0 (0.000%) | 38 / 20 / 18 (47.368%) | 38 / 38 / 0 (0.000%) |
| java-checks/src/main/java/org/sonar/java/checks/security/PasswordEncoderCheck.java | 410 / 171 / 239 (58.293%) | 410 / 410 / 0 (0.000%) | 410 / 171 / 239 (58.293%) | 410 / 410 / 0 (0.000%) |
| java-checks/src/main/java/org/sonar/java/checks/security/PubliclyWritableDirectoriesCheck.java | 264 / 116 / 148 (56.061%) | 264 / 264 / 0 (0.000%) | 264 / 116 / 148 (56.061%) | 264 / 264 / 0 (0.000%) |
| java-checks/src/main/java/org/sonar/java/checks/security/ReceivingIntentsCheck.java | 33 / 16 / 17 (51.515%) | 33 / 33 / 0 (0.000%) | 33 / 16 / 17 (51.515%) | 33 / 33 / 0 (0.000%) |
| java-checks/src/main/java/org/sonar/java/checks/security/SecureCookieCheck.java | 880 / 460 / 420 (47.727%) | 880 / 880 / 0 (0.000%) | 880 / 460 / 420 (47.727%) | 880 / 880 / 0 (0.000%) |
| java-checks/src/main/java/org/sonar/java/checks/security/ServerCertificatesCheck.java | 124 / 75 / 49 (39.516%) | 124 / 124 / 0 (0.000%) | 124 / 75 / 49 (39.516%) | 124 / 124 / 0 (0.000%) |
| java-checks/src/main/java/org/sonar/java/checks/security/UnpredictableSaltCheck.java | 188 / 73 / 115 (61.170%) | 188 / 188 / 0 (0.000%) | 188 / 73 / 115 (61.170%) | 188 / 188 / 0 (0.000%) |
| java-checks/src/main/java/org/sonar/java/checks/security/UserEnumerationCheck.java | 199 / 115 / 84 (42.211%) | 199 / 199 / 0 (0.000%) | 199 / 115 / 84 (42.211%) | 199 / 199 / 0 (0.000%) |
| java-checks/src/main/java/org/sonar/java/checks/security/VerifiedServerHostnamesCheck.java | 367 / 191 / 176 (47.956%) | 367 / 367 / 0 (0.000%) | 367 / 191 / 176 (47.956%) | 367 / 367 / 0 (0.000%) |
| java-checks/src/main/java/org/sonar/java/checks/security/WebViewJavaScriptInterfaceCheck.java | 37 / 21 / 16 (43.243%) | 37 / 37 / 0 (0.000%) | 37 / 21 / 16 (43.243%) | 37 / 37 / 0 (0.000%) |
| java-checks/src/main/java/org/sonar/java/checks/security/WebViewJavaScriptSupportCheck.java | 45 / 24 / 21 (46.667%) | 45 / 45 / 0 (0.000%) | 45 / 24 / 21 (46.667%) | 45 / 45 / 0 (0.000%) |
| java-checks/src/main/java/org/sonar/java/checks/security/WebViewsFileAccessCheck.java | 45 / 24 / 21 (46.667%) | 45 / 45 / 0 (0.000%) | 45 / 24 / 21 (46.667%) | 45 / 45 / 0 (0.000%) |
| java-checks/src/main/java/org/sonar/java/checks/security/XmlRpcExtensionsCheck.java | 35 / 20 / 15 (42.857%) | 35 / 35 / 0 (0.000%) | 35 / 20 / 15 (42.857%) | 35 / 35 / 0 (0.000%) |
| java-checks/src/main/java/org/sonar/java/checks/security/XxeActiveMQCheck.java | 144 / 84 / 60 (41.667%) | 144 / 144 / 0 (0.000%) | 144 / 84 / 60 (41.667%) | 144 / 144 / 0 (0.000%) |
| java-checks/src/main/java/org/sonar/java/checks/security/ZipEntryCheck.java | 121 / 54 / 67 (55.372%) | 121 / 121 / 0 (0.000%) | 121 / 54 / 67 (55.372%) | 121 / 121 / 0 (0.000%) |
| java-checks/src/main/java/org/sonar/java/checks/security/package-info.java | 6 / 6 / 0 (0.000%) | 6 / 6 / 0 (0.000%) | 6 / 6 / 0 (0.000%) | 6 / 6 / 0 (0.000%) |
| java-checks/src/main/java/org/sonar/java/checks/serialization/BlindSerialVersionUidCheck.java | 86 / 35 / 51 (59.302%) | 86 / 86 / 0 (0.000%) | 86 / 35 / 51 (59.302%) | 86 / 86 / 0 (0.000%) |
| java-checks/src/main/java/org/sonar/java/checks/serialization/CustomSerializationMethodCheck.java | 162 / 98 / 64 (39.506%) | 162 / 162 / 0 (0.000%) | 162 / 98 / 64 (39.506%) | 162 / 162 / 0 (0.000%) |
| java-checks/src/main/java/org/sonar/java/checks/serialization/ExternalizableClassConstructorCheck.java | 83 / 45 / 38 (45.783%) | 83 / 83 / 0 (0.000%) | 83 / 45 / 38 (45.783%) | 83 / 83 / 0 (0.000%) |
| java-checks/src/main/java/org/sonar/java/checks/serialization/NonSerializableWriteCheck.java | 116 / 55 / 61 (52.586%) | 116 / 116 / 0 (0.000%) | 116 / 55 / 61 (52.586%) | 116 / 116 / 0 (0.000%) |
| java-checks/src/main/java/org/sonar/java/checks/serialization/PrivateReadResolveCheck.java | 65 / 35 / 30 (46.154%) | 65 / 65 / 0 (0.000%) | 65 / 35 / 30 (46.154%) | 65 / 65 / 0 (0.000%) |
| java-checks/src/main/java/org/sonar/java/checks/serialization/RecordSerializationIgnoredMembersCheck.java | 133 / 74 / 59 (44.361%) | 133 / 133 / 0 (0.000%) | 133 / 74 / 59 (44.361%) | 133 / 133 / 0 (0.000%) |
| java-checks/src/main/java/org/sonar/java/checks/serialization/SerialVersionUidCheck.java | 156 / 97 / 59 (37.821%) | 156 / 156 / 0 (0.000%) | 156 / 97 / 59 (37.821%) | 156 / 156 / 0 (0.000%) |
| java-checks/src/main/java/org/sonar/java/checks/serialization/SerialVersionUidInRecordCheck.java | 93 / 59 / 34 (36.559%) | 93 / 93 / 0 (0.000%) | 93 / 59 / 34 (36.559%) | 93 / 93 / 0 (0.000%) |
| java-checks/src/main/java/org/sonar/java/checks/serialization/SerializableComparatorCheck.java | 49 / 28 / 21 (42.857%) | 49 / 49 / 0 (0.000%) | 49 / 28 / 21 (42.857%) | 49 / 49 / 0 (0.000%) |
| java-checks/src/main/java/org/sonar/java/checks/serialization/SerializableContract.java | 117 / 73 / 44 (37.607%) | 117 / 117 / 0 (0.000%) | 117 / 73 / 44 (37.607%) | 117 / 117 / 0 (0.000%) |
| java-checks/src/main/java/org/sonar/java/checks/serialization/SerializableFieldInSerializableClassCheck.java | 436 / 213 / 223 (51.147%) | 436 / 436 / 0 (0.000%) | 436 / 213 / 223 (51.147%) | 436 / 436 / 0 (0.000%) |
| java-checks/src/main/java/org/sonar/java/checks/serialization/SerializableObjectInSessionCheck.java | 48 / 28 / 20 (41.667%) | 48 / 48 / 0 (0.000%) | 48 / 28 / 20 (41.667%) | 48 / 48 / 0 (0.000%) |
| java-checks/src/main/java/org/sonar/java/checks/serialization/SerializableSuperConstructorCheck.java | 163 / 77 / 86 (52.761%) | 163 / 163 / 0 (0.000%) | 163 / 77 / 86 (52.761%) | 163 / 163 / 0 (0.000%) |
| java-checks/src/main/java/org/sonar/java/checks/serialization/package-info.java | 6 / 6 / 0 (0.000%) | 6 / 6 / 0 (0.000%) | 6 / 6 / 0 (0.000%) | 6 / 6 / 0 (0.000%) |
| java-checks/src/main/java/org/sonar/java/checks/spring/AmbiguousDependencyCheck.java | 292 / 223 / 69 (23.630%) | 292 / 292 / 0 (0.000%) | 292 / 223 / 69 (23.630%) | 292 / 292 / 0 (0.000%) |
| java-checks/src/main/java/org/sonar/java/checks/spring/AsyncMethodsCalledViaThisCheck.java | 69 / 42 / 27 (39.130%) | 69 / 69 / 0 (0.000%) | 69 / 42 / 27 (39.130%) | 69 / 69 / 0 (0.000%) |
| java-checks/src/main/java/org/sonar/java/checks/spring/AsyncMethodsOnConfigurationClassCheck.java | 88 / 23 / 65 (73.864%) | 88 / 88 / 0 (0.000%) | 88 / 23 / 65 (73.864%) | 88 / 88 / 0 (0.000%) |
| java-checks/src/main/java/org/sonar/java/checks/spring/AsyncMethodsReturnTypeCheck.java | 48 / 26 / 22 (45.833%) | 48 / 48 / 0 (0.000%) | 48 / 26 / 22 (45.833%) | 48 / 48 / 0 (0.000%) |
| java-checks/src/main/java/org/sonar/java/checks/spring/AutowiredOnConstructorWhenMultipleConstructorsCheck.java | 95 / 33 / 62 (65.263%) | 95 / 95 / 0 (0.000%) | 95 / 33 / 62 (65.263%) | 95 / 95 / 0 (0.000%) |
| java-checks/src/main/java/org/sonar/java/checks/spring/AutowiredOnMultipleConstructorsCheck.java | 115 / 48 / 67 (58.261%) | 115 / 115 / 0 (0.000%) | 115 / 48 / 67 (58.261%) | 115 / 115 / 0 (0.000%) |
| java-checks/src/main/java/org/sonar/java/checks/spring/AvoidQualifierOnBeanMethodsCheck.java | 153 / 81 / 72 (47.059%) | 153 / 153 / 0 (0.000%) | 153 / 81 / 72 (47.059%) | 153 / 153 / 0 (0.000%) |
| java-checks/src/main/java/org/sonar/java/checks/spring/BeforeAndAfterTransactionContractCheck.java | 161 / 95 / 66 (40.994%) | 161 / 161 / 0 (0.000%) | 161 / 95 / 66 (40.994%) | 161 / 161 / 0 (0.000%) |
| java-checks/src/main/java/org/sonar/java/checks/spring/CacheAnnotationsShouldOnlyBeAppliedToConcreteClassesCheck.java | 106 / 35 / 71 (66.981%) | 106 / 106 / 0 (0.000%) | 106 / 35 / 71 (66.981%) | 106 / 106 / 0 (0.000%) |
| java-checks/src/main/java/org/sonar/java/checks/spring/ConfigurationClassShouldNotBeFinalCheck.java | 110 / 48 / 62 (56.364%) | 110 / 110 / 0 (0.000%) | 110 / 48 / 62 (56.364%) | 110 / 110 / 0 (0.000%) |
| java-checks/src/main/java/org/sonar/java/checks/spring/ControllerWithRestControllerReplacementCheck.java | 176 / 80 / 96 (54.545%) | 176 / 176 / 0 (0.000%) | 176 / 80 / 96 (54.545%) | 176 / 176 / 0 (0.000%) |
| java-checks/src/main/java/org/sonar/java/checks/spring/ControllerWithSessionAttributesCheck.java | 83 / 40 / 43 (51.807%) | 83 / 83 / 0 (0.000%) | 83 / 40 / 43 (51.807%) | 83 / 83 / 0 (0.000%) |
| java-checks/src/main/java/org/sonar/java/checks/spring/DirectBeanMethodInvocationWithoutProxyCheck.java | 185 / 84 / 101 (54.595%) | 185 / 185 / 0 (0.000%) | 185 / 84 / 101 (54.595%) | 185 / 185 / 0 (0.000%) |
| java-checks/src/main/java/org/sonar/java/checks/spring/DirtyContextShouldUseCorrectControlModeCheck.java | 89 / 57 / 32 (35.955%) | 89 / 89 / 0 (0.000%) | 89 / 57 / 32 (35.955%) | 89 / 89 / 0 (0.000%) |
| java-checks/src/main/java/org/sonar/java/checks/spring/EventListenerMethodOneArgCheck.java | 51 / 26 / 25 (49.020%) | 51 / 51 / 0 (0.000%) | 51 / 26 / 25 (49.020%) | 51 / 51 / 0 (0.000%) |
| java-checks/src/main/java/org/sonar/java/checks/spring/FieldDependencyInjectionCheck.java | 58 / 19 / 39 (67.241%) | 58 / 58 / 0 (0.000%) | 58 / 19 / 39 (67.241%) | 58 / 58 / 0 (0.000%) |
| java-checks/src/main/java/org/sonar/java/checks/spring/InitBinderMethodsMustBeVoidCheck.java | 55 / 29 / 26 (47.273%) | 55 / 55 / 0 (0.000%) | 55 / 29 / 26 (47.273%) | 55 / 55 / 0 (0.000%) |
| java-checks/src/main/java/org/sonar/java/checks/spring/MissingPathVariableAnnotationCheck.java | 906 / 660 / 246 (27.152%) | 906 / 906 / 0 (0.000%) | 906 / 660 / 246 (27.152%) | 906 / 906 / 0 (0.000%) |
| java-checks/src/main/java/org/sonar/java/checks/spring/ModelAttributeNamingConventionForSpELCheck.java | 159 / 86 / 73 (45.912%) | 159 / 159 / 0 (0.000%) | 159 / 86 / 73 (45.912%) | 159 / 159 / 0 (0.000%) |
| java-checks/src/main/java/org/sonar/java/checks/spring/NonSingletonAutowiredInSingletonCheck.java | 360 / 159 / 201 (55.833%) | 360 / 360 / 0 (0.000%) | 360 / 159 / 201 (55.833%) | 360 / 360 / 0 (0.000%) |
| java-checks/src/main/java/org/sonar/java/checks/spring/NullableInjectedFieldsHaveDefaultValueCheck.java | 399 / 194 / 205 (51.378%) | 399 / 399 / 0 (0.000%) | 399 / 194 / 205 (51.378%) | 399 / 399 / 0 (0.000%) |
| java-checks/src/main/java/org/sonar/java/checks/spring/OptionalRestParametersShouldBeObjectsCheck.java | 140 / 55 / 85 (60.714%) | 140 / 140 / 0 (0.000%) | 140 / 55 / 85 (60.714%) | 140 / 140 / 0 (0.000%) |
| java-checks/src/main/java/org/sonar/java/checks/spring/PersistentEntityUsedAsRequestParameterCheck.java | 121 / 68 / 53 (43.802%) | 121 / 121 / 0 (0.000%) | 121 / 68 / 53 (43.802%) | 121 / 121 / 0 (0.000%) |
| java-checks/src/main/java/org/sonar/java/checks/spring/RedundantSpringAnnotationCheck.java | 320 / 198 / 122 (38.125%) | 320 / 320 / 0 (0.000%) | 320 / 198 / 122 (38.125%) | 320 / 320 / 0 (0.000%) |
| java-checks/src/main/java/org/sonar/java/checks/spring/RequestMappingMethodPublicCheck.java | 76 / 47 / 29 (38.158%) | 76 / 76 / 0 (0.000%) | 76 / 47 / 29 (38.158%) | 76 / 76 / 0 (0.000%) |
| java-checks/src/main/java/org/sonar/java/checks/spring/ScheduledOnlyOnNoArgMethodCheck.java | 73 / 35 / 38 (52.055%) | 73 / 73 / 0 (0.000%) | 73 / 35 / 38 (52.055%) | 73 / 73 / 0 (0.000%) |
| java-checks/src/main/java/org/sonar/java/checks/spring/SpelExpressionCheck.java | 569 / 480 / 89 (15.641%) | 569 / 569 / 0 (0.000%) | 569 / 480 / 89 (15.641%) | 569 / 569 / 0 (0.000%) |
| java-checks/src/main/java/org/sonar/java/checks/spring/SpringAntMatcherOrderCheck.java | 235 / 186 / 49 (20.851%) | 235 / 235 / 0 (0.000%) | 235 / 186 / 49 (20.851%) | 235 / 235 / 0 (0.000%) |
| java-checks/src/main/java/org/sonar/java/checks/spring/SpringAutoConfigurationCheck.java | 104 / 54 / 50 (48.077%) | 104 / 104 / 0 (0.000%) | 104 / 54 / 50 (48.077%) | 104 / 104 / 0 (0.000%) |
| java-checks/src/main/java/org/sonar/java/checks/spring/SpringBeanNamingConventionCheck.java | 128 / 68 / 60 (46.875%) | 128 / 128 / 0 (0.000%) | 128 / 68 / 60 (46.875%) | 128 / 128 / 0 (0.000%) |
| java-checks/src/main/java/org/sonar/java/checks/spring/SpringBeansShouldBeAccessibleCheck.java | 403 / 260 / 143 (35.484%) | 403 / 403 / 0 (0.000%) | 403 / 260 / 143 (35.484%) | 403 / 403 / 0 (0.000%) |
| java-checks/src/main/java/org/sonar/java/checks/spring/SpringCacheableWithCachePutCheck.java | 184 / 108 / 76 (41.304%) | 184 / 184 / 0 (0.000%) | 184 / 108 / 76 (41.304%) | 184 / 184 / 0 (0.000%) |
| java-checks/src/main/java/org/sonar/java/checks/spring/SpringComponentSpecializationCheck.java | 256 / 157 / 99 (38.672%) | 256 / 256 / 0 (0.000%) | 256 / 157 / 99 (38.672%) | 256 / 256 / 0 (0.000%) |
| java-checks/src/main/java/org/sonar/java/checks/spring/SpringComponentWithNonAutowiredMembersCheck.java | 276 / 117 / 159 (57.609%) | 276 / 276 / 0 (0.000%) | 276 / 117 / 159 (57.609%) | 276 / 276 / 0 (0.000%) |
| java-checks/src/main/java/org/sonar/java/checks/spring/SpringComponentWithWrongScopeCheck.java | 75 / 33 / 42 (56.000%) | 75 / 75 / 0 (0.000%) | 75 / 33 / 42 (56.000%) | 75 / 75 / 0 (0.000%) |
| java-checks/src/main/java/org/sonar/java/checks/spring/SpringComposedRequestMappingCheck.java | 174 / 98 / 76 (43.678%) | 174 / 174 / 0 (0.000%) | 174 / 98 / 76 (43.678%) | 174 / 174 / 0 (0.000%) |
| java-checks/src/main/java/org/sonar/java/checks/spring/SpringConfigurationWithAutowiredFieldsCheck.java | 230 / 90 / 140 (60.870%) | 230 / 230 / 0 (0.000%) | 230 / 90 / 140 (60.870%) | 230 / 230 / 0 (0.000%) |
| java-checks/src/main/java/org/sonar/java/checks/spring/SpringConstructorInjectionCheck.java | 192 / 76 / 116 (60.417%) | 192 / 192 / 0 (0.000%) | 192 / 76 / 116 (60.417%) | 192 / 192 / 0 (0.000%) |
| java-checks/src/main/java/org/sonar/java/checks/spring/SpringContextCheck.java | 11 / 8 / 3 (27.273%) | 11 / 11 / 0 (0.000%) | 11 / 8 / 3 (27.273%) | 11 / 11 / 0 (0.000%) |
| java-checks/src/main/java/org/sonar/java/checks/spring/SpringContextChecks.java | 13 / 10 / 3 (23.077%) | 13 / 13 / 0 (0.000%) | 13 / 10 / 3 (23.077%) | 13 / 13 / 0 (0.000%) |
| java-checks/src/main/java/org/sonar/java/checks/spring/SpringContextIssue.java | 10 / 9 / 1 (10.000%) | 10 / 10 / 0 (0.000%) | 10 / 9 / 1 (10.000%) | 10 / 10 / 0 (0.000%) |
| java-checks/src/main/java/org/sonar/java/checks/spring/SpringIncompatibleTransactionalCheck.java | 439 / 305 / 134 (30.524%) | 439 / 439 / 0 (0.000%) | 439 / 305 / 134 (30.524%) | 439 / 439 / 0 (0.000%) |
| java-checks/src/main/java/org/sonar/java/checks/spring/SpringRequestMappingMethodCheck.java | 158 / 56 / 102 (64.557%) | 158 / 158 / 0 (0.000%) | 158 / 56 / 102 (64.557%) | 158 / 158 / 0 (0.000%) |
| java-checks/src/main/java/org/sonar/java/checks/spring/SpringScanDefaultPackageCheck.java | 269 / 142 / 127 (47.212%) | 269 / 269 / 0 (0.000%) | 269 / 142 / 127 (47.212%) | 269 / 269 / 0 (0.000%) |
| java-checks/src/main/java/org/sonar/java/checks/spring/SpringSecurityDisableCSRFCheck.java | 67 / 36 / 31 (46.269%) | 67 / 67 / 0 (0.000%) | 67 / 36 / 31 (46.269%) | 67 / 67 / 0 (0.000%) |
| java-checks/src/main/java/org/sonar/java/checks/spring/SpringSessionFixationCheck.java | 37 / 22 / 15 (40.541%) | 37 / 37 / 0 (0.000%) | 37 / 22 / 15 (40.541%) | 37 / 37 / 0 (0.000%) |
| java-checks/src/main/java/org/sonar/java/checks/spring/StaticFieldInjectionNotSupportedCheck.java | 159 / 57 / 102 (64.151%) | 159 / 159 / 0 (0.000%) | 159 / 57 / 102 (64.151%) | 159 / 159 / 0 (0.000%) |
| java-checks/src/main/java/org/sonar/java/checks/spring/StatusCodesOnResponseCheck.java | 287 / 172 / 115 (40.070%) | 287 / 287 / 0 (0.000%) | 287 / 172 / 115 (40.070%) | 287 / 287 / 0 (0.000%) |
| java-checks/src/main/java/org/sonar/java/checks/spring/SuperfluousResponseBodyAnnotationCheck.java | 61 / 19 / 42 (68.852%) | 61 / 61 / 0 (0.000%) | 61 / 19 / 42 (68.852%) | 61 / 61 / 0 (0.000%) |
| java-checks/src/main/java/org/sonar/java/checks/spring/TransactionalMethodCheckedExceptionCheck.java | 554 / 258 / 296 (53.430%) | 554 / 554 / 0 (0.000%) | 554 / 258 / 296 (53.430%) | 554 / 554 / 0 (0.000%) |
| java-checks/src/main/java/org/sonar/java/checks/spring/TransactionalMethodVisibilityCheck.java | 120 / 71 / 49 (40.833%) | 120 / 120 / 0 (0.000%) | 120 / 71 / 49 (40.833%) | 120 / 120 / 0 (0.000%) |
| java-checks/src/main/java/org/sonar/java/checks/spring/UsePageableParameterForPagedQueryCheck.java | 111 / 65 / 46 (41.441%) | 111 / 111 / 0 (0.000%) | 111 / 65 / 46 (41.441%) | 111 / 111 / 0 (0.000%) |
| java-checks/src/main/java/org/sonar/java/checks/spring/ValueAnnotationShouldInjectPropertyOrSpELCheck.java | 143 / 75 / 68 (47.552%) | 143 / 143 / 0 (0.000%) | 143 / 75 / 68 (47.552%) | 143 / 143 / 0 (0.000%) |
| java-checks/src/main/java/org/sonar/java/checks/spring/package-info.java | 6 / 6 / 0 (0.000%) | 6 / 6 / 0 (0.000%) | 6 / 6 / 0 (0.000%) | 6 / 6 / 0 (0.000%) |
| java-checks/src/main/java/org/sonar/java/checks/sustainability/AndroidExactAlarmCheck.java | 92 / 65 / 27 (29.348%) | 92 / 92 / 0 (0.000%) | 92 / 65 / 27 (29.348%) | 92 / 92 / 0 (0.000%) |
| java-checks/src/main/java/org/sonar/java/checks/sustainability/AndroidFusedLocationProviderClientCheck.java | 39 / 22 / 17 (43.590%) | 39 / 39 / 0 (0.000%) | 39 / 22 / 17 (43.590%) | 39 / 39 / 0 (0.000%) |
| java-checks/src/main/java/org/sonar/java/checks/sustainability/package-info.java | 6 / 6 / 0 (0.000%) | 6 / 6 / 0 (0.000%) | 6 / 6 / 0 (0.000%) | 6 / 6 / 0 (0.000%) |
| java-checks/src/main/java/org/sonar/java/checks/synchronization/DoubleCheckedLockingCheck.java | 329 / 201 / 128 (38.906%) | 329 / 329 / 0 (0.000%) | 329 / 201 / 128 (38.906%) | 329 / 329 / 0 (0.000%) |
| java-checks/src/main/java/org/sonar/java/checks/synchronization/S9399Check.java | 274 / 138 / 136 (49.635%) | 274 / 274 / 0 (0.000%) | 274 / 138 / 136 (49.635%) | 274 / 274 / 0 (0.000%) |
| java-checks/src/main/java/org/sonar/java/checks/synchronization/SynchronizationOnGetClassCheck.java | 110 / 54 / 56 (50.909%) | 110 / 110 / 0 (0.000%) | 110 / 54 / 56 (50.909%) | 110 / 110 / 0 (0.000%) |
| java-checks/src/main/java/org/sonar/java/checks/synchronization/TwoLocksWaitCheck.java | 223 / 133 / 90 (40.359%) | 223 / 223 / 0 (0.000%) | 223 / 133 / 90 (40.359%) | 223 / 223 / 0 (0.000%) |
| java-checks/src/main/java/org/sonar/java/checks/synchronization/ValueBasedObjectUsedForLockCheck.java | 57 / 33 / 24 (42.105%) | 57 / 57 / 0 (0.000%) | 57 / 33 / 24 (42.105%) | 57 / 57 / 0 (0.000%) |
| java-checks/src/main/java/org/sonar/java/checks/synchronization/WriteObjectTheOnlySynchronizedMethodCheck.java | 96 / 58 / 38 (39.583%) | 96 / 96 / 0 (0.000%) | 96 / 58 / 38 (39.583%) | 96 / 96 / 0 (0.000%) |
| java-checks/src/main/java/org/sonar/java/checks/synchronization/package-info.java | 6 / 6 / 0 (0.000%) | 6 / 6 / 0 (0.000%) | 6 / 6 / 0 (0.000%) | 6 / 6 / 0 (0.000%) |
| java-checks/src/main/java/org/sonar/java/checks/tests/AbstractMockitoArgumentChecker.java | 102 / 31 / 71 (69.608%) | 102 / 102 / 0 (0.000%) | 102 / 31 / 71 (69.608%) | 102 / 102 / 0 (0.000%) |
| java-checks/src/main/java/org/sonar/java/checks/tests/AbstractOneExpectedExceptionRule.java | 331 / 136 / 195 (58.912%) | 331 / 331 / 0 (0.000%) | 331 / 136 / 195 (58.912%) | 331 / 331 / 0 (0.000%) |
| java-checks/src/main/java/org/sonar/java/checks/tests/AssertJApplyConfigurationCheck.java | 74 / 37 / 37 (50.000%) | 74 / 74 / 0 (0.000%) | 74 / 37 / 37 (50.000%) | 74 / 74 / 0 (0.000%) |
| java-checks/src/main/java/org/sonar/java/checks/tests/AssertJAssertionsInConsumerCheck.java | 208 / 108 / 100 (48.077%) | 208 / 208 / 0 (0.000%) | 208 / 108 / 100 (48.077%) | 208 / 208 / 0 (0.000%) |
| java-checks/src/main/java/org/sonar/java/checks/tests/AssertJChainSimplificationCheck.java | 209 / 72 / 137 (65.550%) | 209 / 209 / 0 (0.000%) | 209 / 72 / 137 (65.550%) | 209 / 209 / 0 (0.000%) |
| java-checks/src/main/java/org/sonar/java/checks/tests/AssertJChainSimplificationHelper.java | 73 / 42 / 31 (42.466%) | 73 / 73 / 0 (0.000%) | 73 / 42 / 31 (42.466%) | 73 / 73 / 0 (0.000%) |
| java-checks/src/main/java/org/sonar/java/checks/tests/AssertJChainSimplificationIndex.java | 1893 / 1223 / 670 (35.394%) | 1893 / 1893 / 0 (0.000%) | 1893 / 1223 / 670 (35.394%) | 1893 / 1893 / 0 (0.000%) |
| java-checks/src/main/java/org/sonar/java/checks/tests/AssertJChainSimplificationQuickFix.java | 314 / 180 / 134 (42.675%) | 314 / 314 / 0 (0.000%) | 314 / 180 / 134 (42.675%) | 314 / 314 / 0 (0.000%) |
| java-checks/src/main/java/org/sonar/java/checks/tests/AssertJConsecutiveAssertionCheck.java | 284 / 164 / 120 (42.254%) | 284 / 284 / 0 (0.000%) | 284 / 164 / 120 (42.254%) | 284 / 284 / 0 (0.000%) |
| java-checks/src/main/java/org/sonar/java/checks/tests/AssertJContextBeforeAssertionCheck.java | 56 / 22 / 34 (60.714%) | 56 / 56 / 0 (0.000%) | 56 / 22 / 34 (60.714%) | 56 / 56 / 0 (0.000%) |
| java-checks/src/main/java/org/sonar/java/checks/tests/AssertJTestForEmptinessCheck.java | 220 / 79 / 141 (64.091%) | 220 / 220 / 0 (0.000%) | 220 / 79 / 141 (64.091%) | 220 / 220 / 0 (0.000%) |
| java-checks/src/main/java/org/sonar/java/checks/tests/AssertThatThrownByAloneCheck.java | 32 / 15 / 17 (53.125%) | 32 / 32 / 0 (0.000%) | 32 / 15 / 17 (53.125%) | 32 / 32 / 0 (0.000%) |
| java-checks/src/main/java/org/sonar/java/checks/tests/AssertTrueInsteadOfDedicatedAssertCheck.java | 255 / 178 / 77 (30.196%) | 255 / 255 / 0 (0.000%) | 255 / 178 / 77 (30.196%) | 255 / 255 / 0 (0.000%) |
| java-checks/src/main/java/org/sonar/java/checks/tests/AssertionArgumentOrderCheck.java | 481 / 237 / 244 (50.728%) | 481 / 481 / 0 (0.000%) | 481 / 237 / 244 (50.728%) | 481 / 481 / 0 (0.000%) |
| java-checks/src/main/java/org/sonar/java/checks/tests/AssertionCompareToSelfCheck.java | 273 / 111 / 162 (59.341%) | 273 / 273 / 0 (0.000%) | 273 / 111 / 162 (59.341%) | 273 / 273 / 0 (0.000%) |
| java-checks/src/main/java/org/sonar/java/checks/tests/AssertionFailInCatchBlockCheck.java | 41 / 25 / 16 (39.024%) | 41 / 41 / 0 (0.000%) | 41 / 25 / 16 (39.024%) | 41 / 41 / 0 (0.000%) |
| java-checks/src/main/java/org/sonar/java/checks/tests/AssertionInThreadRunCheck.java | 68 / 38 / 30 (44.118%) | 68 / 68 / 0 (0.000%) | 68 / 38 / 30 (44.118%) | 68 / 68 / 0 (0.000%) |
| java-checks/src/main/java/org/sonar/java/checks/tests/AssertionInTryCatchCheck.java | 108 / 46 / 62 (57.407%) | 108 / 108 / 0 (0.000%) | 108 / 46 / 62 (57.407%) | 108 / 108 / 0 (0.000%) |
| java-checks/src/main/java/org/sonar/java/checks/tests/AssertionTypesCheck.java | 793 / 425 / 368 (46.406%) | 793 / 793 / 0 (0.000%) | 793 / 425 / 368 (46.406%) | 793 / 793 / 0 (0.000%) |
| java-checks/src/main/java/org/sonar/java/checks/tests/AssertionsCompletenessCheck.java | 505 / 256 / 249 (49.307%) | 505 / 505 / 0 (0.000%) | 505 / 256 / 249 (49.307%) | 505 / 505 / 0 (0.000%) |
| java-checks/src/main/java/org/sonar/java/checks/tests/AssertionsInTestsCheck.java | 378 / 233 / 145 (38.360%) | 378 / 378 / 0 (0.000%) | 378 / 233 / 145 (38.360%) | 378 / 378 / 0 (0.000%) |
| java-checks/src/main/java/org/sonar/java/checks/tests/AssertionsWithoutMessageCheck.java | 435 / 250 / 185 (42.529%) | 435 / 435 / 0 (0.000%) | 435 / 250 / 185 (42.529%) | 435 / 435 / 0 (0.000%) |
| java-checks/src/main/java/org/sonar/java/checks/tests/BooleanOrNullLiteralInAssertionsCheck.java | 337 / 175 / 162 (48.071%) | 337 / 337 / 0 (0.000%) | 337 / 175 / 162 (48.071%) | 337 / 337 / 0 (0.000%) |
| java-checks/src/main/java/org/sonar/java/checks/tests/CallSuperInTestCaseCheck.java | 194 / 108 / 86 (44.330%) | 194 / 194 / 0 (0.000%) | 194 / 108 / 86 (44.330%) | 194 / 194 / 0 (0.000%) |
| java-checks/src/main/java/org/sonar/java/checks/tests/DataProviderNameUniquenessCheck.java | 166 / 95 / 71 (42.771%) | 166 / 166 / 0 (0.000%) | 166 / 95 / 71 (42.771%) | 166 / 166 / 0 (0.000%) |
| java-checks/src/main/java/org/sonar/java/checks/tests/ExpectedExceptionCheck.java | 97 / 60 / 37 (38.144%) | 97 / 97 / 0 (0.000%) | 97 / 60 / 37 (38.144%) | 97 / 97 / 0 (0.000%) |
| java-checks/src/main/java/org/sonar/java/checks/tests/IgnoredTestsCheck.java | 188 / 75 / 113 (60.106%) | 188 / 188 / 0 (0.000%) | 188 / 75 / 113 (60.106%) | 188 / 188 / 0 (0.000%) |
| java-checks/src/main/java/org/sonar/java/checks/tests/JUnit45MethodAnnotationCheck.java | 243 / 175 / 68 (27.984%) | 243 / 243 / 0 (0.000%) | 243 / 175 / 68 (27.984%) | 243 / 243 / 0 (0.000%) |
| java-checks/src/main/java/org/sonar/java/checks/tests/JUnit4AnnotationsCheck.java | 58 / 45 / 13 (22.414%) | 58 / 58 / 0 (0.000%) | 58 / 45 / 13 (22.414%) | 58 / 58 / 0 (0.000%) |
| java-checks/src/main/java/org/sonar/java/checks/tests/JUnit5DefaultPackageClassAndMethodCheck.java | 187 / 79 / 108 (57.754%) | 187 / 187 / 0 (0.000%) | 187 / 79 / 108 (57.754%) | 187 / 187 / 0 (0.000%) |
| java-checks/src/main/java/org/sonar/java/checks/tests/JUnit5SilentlyIgnoreClassAndMethodCheck.java | 239 / 114 / 125 (52.301%) | 239 / 239 / 0 (0.000%) | 239 / 114 / 125 (52.301%) | 239 / 239 / 0 (0.000%) |
| java-checks/src/main/java/org/sonar/java/checks/tests/JUnitCompatibleAnnotationsCheck.java | 64 / 29 / 35 (54.688%) | 64 / 64 / 0 (0.000%) | 64 / 29 / 35 (54.688%) | 64 / 64 / 0 (0.000%) |
| java-checks/src/main/java/org/sonar/java/checks/tests/JunitNestedAnnotationCheck.java | 88 / 44 / 44 (50.000%) | 88 / 88 / 0 (0.000%) | 88 / 44 / 44 (50.000%) | 88 / 88 / 0 (0.000%) |
| java-checks/src/main/java/org/sonar/java/checks/tests/MockFieldShouldUseMockAnnotationCheck.java | 80 / 33 / 47 (58.750%) | 80 / 80 / 0 (0.000%) | 80 / 33 / 47 (58.750%) | 80 / 80 / 0 (0.000%) |
| java-checks/src/main/java/org/sonar/java/checks/tests/MockingAllMethodsCheck.java | 203 / 87 / 116 (57.143%) | 203 / 203 / 0 (0.000%) | 203 / 87 / 116 (57.143%) | 203 / 203 / 0 (0.000%) |
| java-checks/src/main/java/org/sonar/java/checks/tests/MockitoAnnotatedObjectsShouldBeInitializedCheck.java | 387 / 206 / 181 (46.770%) | 387 / 387 / 0 (0.000%) | 387 / 206 / 181 (46.770%) | 387 / 387 / 0 (0.000%) |
| java-checks/src/main/java/org/sonar/java/checks/tests/MockitoArgumentMatchersUsedOnAllParametersCheck.java | 262 / 139 / 123 (46.947%) | 262 / 262 / 0 (0.000%) | 262 / 139 / 123 (46.947%) | 262 / 262 / 0 (0.000%) |
| java-checks/src/main/java/org/sonar/java/checks/tests/MockitoEqSimplificationCheck.java | 65 / 29 / 36 (55.385%) | 65 / 65 / 0 (0.000%) | 65 / 29 / 36 (55.385%) | 65 / 65 / 0 (0.000%) |
| java-checks/src/main/java/org/sonar/java/checks/tests/MockitoInjectMocksShouldBeUsedCheck.java | 339 / 179 / 160 (47.198%) | 339 / 339 / 0 (0.000%) | 339 / 179 / 160 (47.198%) | 339 / 339 / 0 (0.000%) |
| java-checks/src/main/java/org/sonar/java/checks/tests/MockitoInlineMockInThenReturnCheck.java | 70 / 37 / 33 (47.143%) | 70 / 70 / 0 (0.000%) | 70 / 37 / 33 (47.143%) | 70 / 70 / 0 (0.000%) |
| java-checks/src/main/java/org/sonar/java/checks/tests/MockitoManagedClassHelper.java | 130 / 88 / 42 (32.308%) | 130 / 130 / 0 (0.000%) | 130 / 88 / 42 (32.308%) | 130 / 130 / 0 (0.000%) |
| java-checks/src/main/java/org/sonar/java/checks/tests/MockitoStaticImportCheck.java | 225 / 133 / 92 (40.889%) | 225 / 225 / 0 (0.000%) | 225 / 133 / 92 (40.889%) | 225 / 225 / 0 (0.000%) |
| java-checks/src/main/java/org/sonar/java/checks/tests/MockitoStubbingChainCheck.java | 129 / 79 / 50 (38.760%) | 129 / 129 / 0 (0.000%) | 129 / 79 / 50 (38.760%) | 129 / 129 / 0 (0.000%) |
| java-checks/src/main/java/org/sonar/java/checks/tests/NoJUnit4AssertionsInJUnit5TestsCheck.java | 83 / 31 / 52 (62.651%) | 83 / 83 / 0 (0.000%) | 83 / 31 / 52 (62.651%) | 83 / 83 / 0 (0.000%) |
| java-checks/src/main/java/org/sonar/java/checks/tests/NoTestInTestClassCheck.java | 597 / 354 / 243 (40.704%) | 597 / 597 / 0 (0.000%) | 597 / 354 / 243 (40.704%) | 597 / 597 / 0 (0.000%) |
| java-checks/src/main/java/org/sonar/java/checks/tests/OneExpectedCheckedExceptionCheck.java | 74 / 32 / 42 (56.757%) | 74 / 74 / 0 (0.000%) | 74 / 32 / 42 (56.757%) | 74 / 74 / 0 (0.000%) |
| java-checks/src/main/java/org/sonar/java/checks/tests/OneExpectedRuntimeExceptionCheck.java | 125 / 42 / 83 (66.400%) | 125 / 125 / 0 (0.000%) | 125 / 42 / 83 (66.400%) | 125 / 125 / 0 (0.000%) |
| java-checks/src/main/java/org/sonar/java/checks/tests/OneTestLifecycleAnnotationCheck.java | 131 / 69 / 62 (47.328%) | 131 / 131 / 0 (0.000%) | 131 / 69 / 62 (47.328%) | 131 / 131 / 0 (0.000%) |
| java-checks/src/main/java/org/sonar/java/checks/tests/ParameterizedTestCheck.java | 277 / 154 / 123 (44.404%) | 277 / 277 / 0 (0.000%) | 277 / 154 / 123 (44.404%) | 277 / 277 / 0 (0.000%) |
| java-checks/src/main/java/org/sonar/java/checks/tests/RandomizedTestDataCheck.java | 142 / 69 / 73 (51.408%) | 142 / 142 / 0 (0.000%) | 142 / 69 / 73 (51.408%) | 142 / 142 / 0 (0.000%) |
| java-checks/src/main/java/org/sonar/java/checks/tests/SpringAssertionsSimplificationCheck.java | 172 / 56 / 116 (67.442%) | 172 / 172 / 0 (0.000%) | 172 / 56 / 116 (67.442%) | 172 / 172 / 0 (0.000%) |
| java-checks/src/main/java/org/sonar/java/checks/tests/SystemClockCheck.java | 58 / 24 / 34 (58.621%) | 58 / 58 / 0 (0.000%) | 58 / 24 / 34 (58.621%) | 58 / 58 / 0 (0.000%) |
| java-checks/src/main/java/org/sonar/java/checks/tests/TestAnnotationWithExpectedExceptionCheck.java | 103 / 45 / 58 (56.311%) | 103 / 103 / 0 (0.000%) | 103 / 45 / 58 (56.311%) | 103 / 103 / 0 (0.000%) |
| java-checks/src/main/java/org/sonar/java/checks/tests/TestNGDataProviderReturnTypeCheck.java | 100 / 59 / 41 (41.000%) | 100 / 100 / 0 (0.000%) | 100 / 59 / 41 (41.000%) | 100 / 100 / 0 (0.000%) |
| java-checks/src/main/java/org/sonar/java/checks/tests/TestNGJavadocTagsCheck.java | 178 / 139 / 39 (21.910%) | 178 / 178 / 0 (0.000%) | 178 / 139 / 39 (21.910%) | 178 / 178 / 0 (0.000%) |
| java-checks/src/main/java/org/sonar/java/checks/tests/TestsStabilityCheck.java | 81 / 36 / 45 (55.556%) | 81 / 81 / 0 (0.000%) | 81 / 36 / 45 (55.556%) | 81 / 81 / 0 (0.000%) |
| java-checks/src/main/java/org/sonar/java/checks/tests/ThreadSleepInTestsCheck.java | 46 / 22 / 24 (52.174%) | 46 / 46 / 0 (0.000%) | 46 / 22 / 24 (52.174%) | 46 / 46 / 0 (0.000%) |
| java-checks/src/main/java/org/sonar/java/checks/tests/TooManyAssertionsCheck.java | 201 / 116 / 85 (42.289%) | 201 / 201 / 0 (0.000%) | 201 / 116 / 85 (42.289%) | 201 / 201 / 0 (0.000%) |
| java-checks/src/main/java/org/sonar/java/checks/tests/package-info.java | 6 / 6 / 0 (0.000%) | 6 / 6 / 0 (0.000%) | 6 / 6 / 0 (0.000%) | 6 / 6 / 0 (0.000%) |
| java-checks/src/main/java/org/sonar/java/checks/unused/UnusedCollectionCheck.java | 102 / 47 / 55 (53.922%) | 102 / 102 / 0 (0.000%) | 102 / 47 / 55 (53.922%) | 102 / 102 / 0 (0.000%) |
| java-checks/src/main/java/org/sonar/java/checks/unused/UnusedLabelCheck.java | 32 / 17 / 15 (46.875%) | 32 / 32 / 0 (0.000%) | 32 / 17 / 15 (46.875%) | 32 / 32 / 0 (0.000%) |
| java-checks/src/main/java/org/sonar/java/checks/unused/UnusedLocalVariableCheck.java | 460 / 235 / 225 (48.913%) | 460 / 460 / 0 (0.000%) | 460 / 235 / 225 (48.913%) | 460 / 460 / 0 (0.000%) |
| java-checks/src/main/java/org/sonar/java/checks/unused/UnusedMethodParameterCheck.java | 481 / 236 / 245 (50.936%) | 481 / 481 / 0 (0.000%) | 481 / 236 / 245 (50.936%) | 481 / 481 / 0 (0.000%) |
| java-checks/src/main/java/org/sonar/java/checks/unused/UnusedPrivateClassCheck.java | 66 / 34 / 32 (48.485%) | 66 / 66 / 0 (0.000%) | 66 / 34 / 32 (48.485%) | 66 / 66 / 0 (0.000%) |
| java-checks/src/main/java/org/sonar/java/checks/unused/UnusedPrivateFieldCheck.java | 580 / 293 / 287 (49.483%) | 580 / 580 / 0 (0.000%) | 580 / 293 / 287 (49.483%) | 580 / 580 / 0 (0.000%) |
| java-checks/src/main/java/org/sonar/java/checks/unused/UnusedPrivateMethodCheck.java | 583 / 314 / 269 (46.141%) | 583 / 583 / 0 (0.000%) | 583 / 314 / 269 (46.141%) | 583 / 583 / 0 (0.000%) |
| java-checks/src/main/java/org/sonar/java/checks/unused/UnusedReturnedDataCheck.java | 153 / 69 / 84 (54.902%) | 153 / 153 / 0 (0.000%) | 153 / 69 / 84 (54.902%) | 153 / 153 / 0 (0.000%) |
| java-checks/src/main/java/org/sonar/java/checks/unused/UnusedStringBuilderCheck.java | 125 / 77 / 48 (38.400%) | 125 / 125 / 0 (0.000%) | 125 / 77 / 48 (38.400%) | 125 / 125 / 0 (0.000%) |
| java-checks/src/main/java/org/sonar/java/checks/unused/UnusedTestRuleCheck.java | 136 / 72 / 64 (47.059%) | 136 / 136 / 0 (0.000%) | 136 / 72 / 64 (47.059%) | 136 / 136 / 0 (0.000%) |
| java-checks/src/main/java/org/sonar/java/checks/unused/UnusedThrowableCheck.java | 65 / 23 / 42 (64.615%) | 65 / 65 / 0 (0.000%) | 65 / 23 / 42 (64.615%) | 65 / 65 / 0 (0.000%) |
| java-checks/src/main/java/org/sonar/java/checks/unused/UnusedTypeParameterCheck.java | 76 / 38 / 38 (50.000%) | 76 / 76 / 0 (0.000%) | 76 / 38 / 38 (50.000%) | 76 / 76 / 0 (0.000%) |
| java-checks/src/main/java/org/sonar/java/checks/unused/package-info.java | 6 / 6 / 0 (0.000%) | 6 / 6 / 0 (0.000%) | 6 / 6 / 0 (0.000%) | 6 / 6 / 0 (0.000%) |
| java-checks/src/main/java/org/sonar/java/checks/unused/utils/AnnotationFieldReferenceFinder.java | 178 / 131 / 47 (26.404%) | 178 / 178 / 0 (0.000%) | 178 / 131 / 47 (26.404%) | 178 / 178 / 0 (0.000%) |
| java-checks/src/main/java/org/sonar/java/checks/unused/utils/package-info.java | 7 / 7 / 0 (0.000%) | 7 / 7 / 0 (0.000%) | 7 / 7 / 0 (0.000%) | 7 / 7 / 0 (0.000%) |
| java-checks/src/main/java/org/sonar/java/filters/AnyRuleIssueFilter.java | 96 / 69 / 27 (28.125%) | 96 / 96 / 0 (0.000%) | 96 / 69 / 27 (28.125%) | 96 / 96 / 0 (0.000%) |
| java-checks/src/main/java/org/sonar/java/filters/BaseTreeVisitorIssueFilter.java | 228 / 174 / 54 (23.684%) | 228 / 228 / 0 (0.000%) | 228 / 174 / 54 (23.684%) | 228 / 228 / 0 (0.000%) |
| java-checks/src/main/java/org/sonar/java/filters/EclipseI18NFilter.java | 30 / 16 / 14 (46.667%) | 30 / 30 / 0 (0.000%) | 30 / 16 / 14 (46.667%) | 30 / 30 / 0 (0.000%) |
| java-checks/src/main/java/org/sonar/java/filters/ExpectedExceptionFilter.java | 407 / 230 / 177 (43.489%) | 407 / 407 / 0 (0.000%) | 407 / 230 / 177 (43.489%) | 407 / 407 / 0 (0.000%) |
| java-checks/src/main/java/org/sonar/java/filters/GeneratedCodeFilter.java | 63 / 37 / 26 (41.270%) | 63 / 63 / 0 (0.000%) | 63 / 37 / 26 (41.270%) | 63 / 63 / 0 (0.000%) |
| java-checks/src/main/java/org/sonar/java/filters/GoogleAutoFilter.java | 59 / 36 / 23 (38.983%) | 59 / 59 / 0 (0.000%) | 59 / 36 / 23 (38.983%) | 59 / 59 / 0 (0.000%) |
| java-checks/src/main/java/org/sonar/java/filters/JavaIssueFilter.java | 13 / 9 / 4 (30.769%) | 13 / 13 / 0 (0.000%) | 13 / 9 / 4 (30.769%) | 13 / 13 / 0 (0.000%) |
| java-checks/src/main/java/org/sonar/java/filters/LombokFilter.java | 493 / 254 / 239 (48.479%) | 493 / 493 / 0 (0.000%) | 493 / 254 / 239 (48.479%) | 493 / 493 / 0 (0.000%) |
| java-checks/src/main/java/org/sonar/java/filters/PostAnalysisIssueFilter.java | 54 / 28 / 26 (48.148%) | 54 / 54 / 0 (0.000%) | 54 / 28 / 26 (48.148%) | 54 / 54 / 0 (0.000%) |
| java-checks/src/main/java/org/sonar/java/filters/SpringFilter.java | 238 / 107 / 131 (55.042%) | 238 / 238 / 0 (0.000%) | 238 / 107 / 131 (55.042%) | 238 / 238 / 0 (0.000%) |
| java-checks/src/main/java/org/sonar/java/filters/SuppressWarningFilter.java | 581 / 503 / 78 (13.425%) | 581 / 581 / 0 (0.000%) | 581 / 503 / 78 (13.425%) | 581 / 581 / 0 (0.000%) |
| java-checks/src/main/java/org/sonar/java/filters/package-info.java | 5 / 5 / 0 (0.000%) | 5 / 5 / 0 (0.000%) | 5 / 5 / 0 (0.000%) | 5 / 5 / 0 (0.000%) |
| java-frontend/src/main/java/org/eclipse/jdt/core/dom/ASTUtils.java | 179 / 179 / 0 (0.000%) | 179 / 179 / 0 (0.000%) | 179 / 179 / 0 (0.000%) | 179 / 179 / 0 (0.000%) |
| java-frontend/src/main/java/org/eclipse/jdt/core/dom/package-info.java | 5 / 5 / 0 (0.000%) | 5 / 5 / 0 (0.000%) | 5 / 5 / 0 (0.000%) | 5 / 5 / 0 (0.000%) |
| java-frontend/src/main/java/org/sonar/java/AnalysisException.java | 18 / 18 / 0 (0.000%) | 18 / 18 / 0 (0.000%) | 18 / 18 / 0 (0.000%) | 18 / 18 / 0 (0.000%) |
| java-frontend/src/main/java/org/sonar/java/AnalysisProgress.java | 43 / 43 / 0 (0.000%) | 43 / 43 / 0 (0.000%) | 43 / 43 / 0 (0.000%) | 43 / 43 / 0 (0.000%) |
| java-frontend/src/main/java/org/sonar/java/AnalysisWarningsWrapper.java | 28 / 28 / 0 (0.000%) | 28 / 28 / 0 (0.000%) | 28 / 28 / 0 (0.000%) | 28 / 28 / 0 (0.000%) |
| java-frontend/src/main/java/org/sonar/java/BatchGenerator.java | 78 / 78 / 0 (0.000%) | 78 / 78 / 0 (0.000%) | 78 / 78 / 0 (0.000%) | 78 / 78 / 0 (0.000%) |
| java-frontend/src/main/java/org/sonar/java/CheckFailureException.java | 13 / 13 / 0 (0.000%) | 13 / 13 / 0 (0.000%) | 13 / 13 / 0 (0.000%) | 13 / 13 / 0 (0.000%) |
| java-frontend/src/main/java/org/sonar/java/DefaultJavaResourceLocator.java | 142 / 119 / 23 (16.197%) | 142 / 142 / 0 (0.000%) | 142 / 142 / 0 (0.000%) | 142 / 142 / 0 (0.000%) |
| java-frontend/src/main/java/org/sonar/java/DefaultModuleMetadata.java | 53 / 43 / 10 (18.868%) | 53 / 53 / 0 (0.000%) | 53 / 53 / 0 (0.000%) | 53 / 53 / 0 (0.000%) |
| java-frontend/src/main/java/org/sonar/java/ExceptionHandler.java | 10 / 10 / 0 (0.000%) | 10 / 10 / 0 (0.000%) | 10 / 10 / 0 (0.000%) | 10 / 10 / 0 (0.000%) |
| java-frontend/src/main/java/org/sonar/java/ExecutionTimeReport.java | 180 / 177 / 3 (1.667%) | 180 / 180 / 0 (0.000%) | 180 / 180 / 0 (0.000%) | 180 / 180 / 0 (0.000%) |
| java-frontend/src/main/java/org/sonar/java/IllegalRuleParameterException.java | 13 / 13 / 0 (0.000%) | 13 / 13 / 0 (0.000%) | 13 / 13 / 0 (0.000%) | 13 / 13 / 0 (0.000%) |
| java-frontend/src/main/java/org/sonar/java/JavaConstants.java | 7 / 7 / 0 (0.000%) | 7 / 7 / 0 (0.000%) | 7 / 7 / 0 (0.000%) | 7 / 7 / 0 (0.000%) |
| java-frontend/src/main/java/org/sonar/java/JavaFilesCache.java | 151 / 118 / 33 (21.854%) | 151 / 151 / 0 (0.000%) | 151 / 151 / 0 (0.000%) | 151 / 151 / 0 (0.000%) |
| java-frontend/src/main/java/org/sonar/java/JavaFrontend.java | 677 / 512 / 165 (24.372%) | 677 / 677 / 0 (0.000%) | 677 / 677 / 0 (0.000%) | 677 / 677 / 0 (0.000%) |
| java-frontend/src/main/java/org/sonar/java/Measurer.java | 238 / 154 / 84 (35.294%) | 238 / 238 / 0 (0.000%) | 238 / 238 / 0 (0.000%) | 238 / 238 / 0 (0.000%) |
| java-frontend/src/main/java/org/sonar/java/Preconditions.java | 54 / 54 / 0 (0.000%) | 54 / 54 / 0 (0.000%) | 54 / 54 / 0 (0.000%) | 54 / 54 / 0 (0.000%) |
| java-frontend/src/main/java/org/sonar/java/ProgressMonitor.java | 167 / 160 / 7 (4.192%) | 167 / 167 / 0 (0.000%) | 167 / 167 / 0 (0.000%) | 167 / 167 / 0 (0.000%) |
| java-frontend/src/main/java/org/sonar/java/SemanticReportScanner.java | 1117 / 876 / 241 (21.576%) | 1117 / 1117 / 0 (0.000%) | 1117 / 1117 / 0 (0.000%) | 1117 / 1117 / 0 (0.000%) |
| java-frontend/src/main/java/org/sonar/java/SonarComponents.java | 1147 / 879 / 268 (23.365%) | 1147 / 1147 / 0 (0.000%) | 1147 / 1147 / 0 (0.000%) | 1147 / 1147 / 0 (0.000%) |
| java-frontend/src/main/java/org/sonar/java/annotations/Beta.java | 20 / 20 / 0 (0.000%) | 20 / 20 / 0 (0.000%) | 20 / 20 / 0 (0.000%) | 20 / 20 / 0 (0.000%) |
| java-frontend/src/main/java/org/sonar/java/annotations/VisibleForTesting.java | 14 / 14 / 0 (0.000%) | 14 / 14 / 0 (0.000%) | 14 / 14 / 0 (0.000%) | 14 / 14 / 0 (0.000%) |
| java-frontend/src/main/java/org/sonar/java/annotations/package-info.java | 7 / 7 / 0 (0.000%) | 7 / 7 / 0 (0.000%) | 7 / 7 / 0 (0.000%) | 7 / 7 / 0 (0.000%) |
| java-frontend/src/main/java/org/sonar/java/ast/JavaAstScanner.java | 417 / 312 / 105 (25.180%) | 417 / 417 / 0 (0.000%) | 417 / 417 / 0 (0.000%) | 417 / 417 / 0 (0.000%) |
| java-frontend/src/main/java/org/sonar/java/ast/api/JavaKeyword.java | 145 / 145 / 0 (0.000%) | 145 / 145 / 0 (0.000%) | 145 / 145 / 0 (0.000%) | 145 / 145 / 0 (0.000%) |
| java-frontend/src/main/java/org/sonar/java/ast/api/JavaPunctuator.java | 125 / 125 / 0 (0.000%) | 125 / 125 / 0 (0.000%) | 125 / 125 / 0 (0.000%) | 125 / 125 / 0 (0.000%) |
| java-frontend/src/main/java/org/sonar/java/ast/api/JavaRestrictedKeyword.java | 64 / 63 / 1 (1.563%) | 64 / 64 / 0 (0.000%) | 64 / 64 / 0 (0.000%) | 64 / 64 / 0 (0.000%) |
| java-frontend/src/main/java/org/sonar/java/ast/api/package-info.java | 6 / 6 / 0 (0.000%) | 6 / 6 / 0 (0.000%) | 6 / 6 / 0 (0.000%) | 6 / 6 / 0 (0.000%) |
| java-frontend/src/main/java/org/sonar/java/ast/package-info.java | 5 / 5 / 0 (0.000%) | 5 / 5 / 0 (0.000%) | 5 / 5 / 0 (0.000%) | 5 / 5 / 0 (0.000%) |
| java-frontend/src/main/java/org/sonar/java/ast/parser/ArgumentListTreeImpl.java | 82 / 60 / 22 (26.829%) | 82 / 82 / 0 (0.000%) | 82 / 82 / 0 (0.000%) | 82 / 82 / 0 (0.000%) |
| java-frontend/src/main/java/org/sonar/java/ast/parser/FormalParametersListTreeImpl.java | 37 / 28 / 9 (24.324%) | 37 / 37 / 0 (0.000%) | 37 / 37 / 0 (0.000%) | 37 / 37 / 0 (0.000%) |
| java-frontend/src/main/java/org/sonar/java/ast/parser/InitializerListTreeImpl.java | 23 / 16 / 7 (30.435%) | 23 / 23 / 0 (0.000%) | 23 / 23 / 0 (0.000%) | 23 / 23 / 0 (0.000%) |
| java-frontend/src/main/java/org/sonar/java/ast/parser/ListTreeImpl.java | 231 / 165 / 66 (28.571%) | 231 / 231 / 0 (0.000%) | 231 / 231 / 0 (0.000%) | 231 / 231 / 0 (0.000%) |
| java-frontend/src/main/java/org/sonar/java/ast/parser/ModuleNameListTreeImpl.java | 23 / 16 / 7 (30.435%) | 23 / 23 / 0 (0.000%) | 23 / 23 / 0 (0.000%) | 23 / 23 / 0 (0.000%) |
| java-frontend/src/main/java/org/sonar/java/ast/parser/ModuleNameTreeImpl.java | 21 / 15 / 6 (28.571%) | 21 / 21 / 0 (0.000%) | 21 / 21 / 0 (0.000%) | 21 / 21 / 0 (0.000%) |
| java-frontend/src/main/java/org/sonar/java/ast/parser/QualifiedIdentifierListTreeImpl.java | 23 / 16 / 7 (30.435%) | 23 / 23 / 0 (0.000%) | 23 / 23 / 0 (0.000%) | 23 / 23 / 0 (0.000%) |
| java-frontend/src/main/java/org/sonar/java/ast/parser/ResourceListTreeImpl.java | 23 / 16 / 7 (30.435%) | 23 / 23 / 0 (0.000%) | 23 / 23 / 0 (0.000%) | 23 / 23 / 0 (0.000%) |
| java-frontend/src/main/java/org/sonar/java/ast/parser/StatementListTreeImpl.java | 23 / 16 / 7 (30.435%) | 23 / 23 / 0 (0.000%) | 23 / 23 / 0 (0.000%) | 23 / 23 / 0 (0.000%) |
| java-frontend/src/main/java/org/sonar/java/ast/parser/TypeParameterListTreeImpl.java | 75 / 56 / 19 (25.333%) | 75 / 75 / 0 (0.000%) | 75 / 75 / 0 (0.000%) | 75 / 75 / 0 (0.000%) |
| java-frontend/src/main/java/org/sonar/java/ast/parser/package-info.java | 6 / 6 / 0 (0.000%) | 6 / 6 / 0 (0.000%) | 6 / 6 / 0 (0.000%) | 6 / 6 / 0 (0.000%) |
| java-frontend/src/main/java/org/sonar/java/ast/visitors/AccessorsUtils.java | 317 / 165 / 152 (47.950%) | 317 / 317 / 0 (0.000%) | 317 / 317 / 0 (0.000%) | 317 / 317 / 0 (0.000%) |
| java-frontend/src/main/java/org/sonar/java/ast/visitors/CognitiveComplexityVisitor.java | 439 / 296 / 143 (32.574%) | 439 / 439 / 0 (0.000%) | 439 / 439 / 0 (0.000%) | 439 / 439 / 0 (0.000%) |
| java-frontend/src/main/java/org/sonar/java/ast/visitors/CommentLinesVisitor.java | 131 / 99 / 32 (24.427%) | 131 / 131 / 0 (0.000%) | 131 / 131 / 0 (0.000%) | 131 / 131 / 0 (0.000%) |
| java-frontend/src/main/java/org/sonar/java/ast/visitors/ComplexityVisitor.java | 189 / 105 / 84 (44.444%) | 189 / 189 / 0 (0.000%) | 189 / 189 / 0 (0.000%) | 189 / 189 / 0 (0.000%) |
| java-frontend/src/main/java/org/sonar/java/ast/visitors/FileLinesVisitor.java | 295 / 153 / 142 (48.136%) | 295 / 295 / 0 (0.000%) | 295 / 295 / 0 (0.000%) | 295 / 295 / 0 (0.000%) |
| java-frontend/src/main/java/org/sonar/java/ast/visitors/Java25FeaturesTelemetryVisitor.java | 93 / 48 / 45 (48.387%) | 93 / 93 / 0 (0.000%) | 93 / 93 / 0 (0.000%) | 93 / 93 / 0 (0.000%) |
| java-frontend/src/main/java/org/sonar/java/ast/visitors/LinesOfCodeVisitor.java | 43 / 28 / 15 (34.884%) | 43 / 43 / 0 (0.000%) | 43 / 43 / 0 (0.000%) | 43 / 43 / 0 (0.000%) |
| java-frontend/src/main/java/org/sonar/java/ast/visitors/MethodNestingLevelVisitor.java | 130 / 91 / 39 (30.000%) | 130 / 130 / 0 (0.000%) | 130 / 130 / 0 (0.000%) | 130 / 130 / 0 (0.000%) |
| java-frontend/src/main/java/org/sonar/java/ast/visitors/NumberOfDefinedVariablesVisitor.java | 20 / 16 / 4 (20.000%) | 20 / 20 / 0 (0.000%) | 20 / 20 / 0 (0.000%) | 20 / 20 / 0 (0.000%) |
| java-frontend/src/main/java/org/sonar/java/ast/visitors/PublicApiChecker.java | 289 / 138 / 151 (52.249%) | 289 / 289 / 0 (0.000%) | 289 / 289 / 0 (0.000%) | 289 / 289 / 0 (0.000%) |
| java-frontend/src/main/java/org/sonar/java/ast/visitors/SonarSymbolTableVisitor.java | 210 / 124 / 86 (40.952%) | 210 / 210 / 0 (0.000%) | 210 / 210 / 0 (0.000%) | 210 / 210 / 0 (0.000%) |
| java-frontend/src/main/java/org/sonar/java/ast/visitors/StatementVisitor.java | 220 / 147 / 73 (33.182%) | 220 / 220 / 0 (0.000%) | 220 / 220 / 0 (0.000%) | 220 / 220 / 0 (0.000%) |
| java-frontend/src/main/java/org/sonar/java/ast/visitors/SubscriptionVisitor.java | 148 / 99 / 49 (33.108%) | 148 / 148 / 0 (0.000%) | 148 / 148 / 0 (0.000%) | 148 / 148 / 0 (0.000%) |
| java-frontend/src/main/java/org/sonar/java/ast/visitors/SyntaxHighlighterVisitor.java | 444 / 221 / 223 (50.225%) | 444 / 444 / 0 (0.000%) | 444 / 444 / 0 (0.000%) | 444 / 444 / 0 (0.000%) |
| java-frontend/src/main/java/org/sonar/java/ast/visitors/package-info.java | 6 / 6 / 0 (0.000%) | 6 / 6 / 0 (0.000%) | 6 / 6 / 0 (0.000%) | 6 / 6 / 0 (0.000%) |
| java-frontend/src/main/java/org/sonar/java/caching/CacheContextImpl.java | 125 / 109 / 16 (12.800%) | 125 / 125 / 0 (0.000%) | 125 / 125 / 0 (0.000%) | 125 / 125 / 0 (0.000%) |
| java-frontend/src/main/java/org/sonar/java/caching/CacheReadException.java | 14 / 14 / 0 (0.000%) | 14 / 14 / 0 (0.000%) | 14 / 14 / 0 (0.000%) | 14 / 14 / 0 (0.000%) |
| java-frontend/src/main/java/org/sonar/java/caching/ContentHashCache.java | 167 / 153 / 14 (8.383%) | 167 / 167 / 0 (0.000%) | 167 / 167 / 0 (0.000%) | 167 / 167 / 0 (0.000%) |
| java-frontend/src/main/java/org/sonar/java/caching/DummyCache.java | 42 / 40 / 2 (4.762%) | 42 / 42 / 0 (0.000%) | 42 / 42 / 0 (0.000%) | 42 / 42 / 0 (0.000%) |
| java-frontend/src/main/java/org/sonar/java/caching/FileCachingCheck.java | 113 / 91 / 22 (19.469%) | 113 / 113 / 0 (0.000%) | 113 / 113 / 0 (0.000%) | 113 / 113 / 0 (0.000%) |
| java-frontend/src/main/java/org/sonar/java/caching/FileHashingUtils.java | 54 / 54 / 0 (0.000%) | 54 / 54 / 0 (0.000%) | 54 / 54 / 0 (0.000%) | 54 / 54 / 0 (0.000%) |
| java-frontend/src/main/java/org/sonar/java/caching/JavaReadCacheImpl.java | 97 / 95 / 2 (2.062%) | 97 / 97 / 0 (0.000%) | 97 / 97 / 0 (0.000%) | 97 / 97 / 0 (0.000%) |
| java-frontend/src/main/java/org/sonar/java/caching/JavaWriteCacheImpl.java | 88 / 87 / 1 (1.136%) | 88 / 88 / 0 (0.000%) | 88 / 88 / 0 (0.000%) | 88 / 88 / 0 (0.000%) |
| java-frontend/src/main/java/org/sonar/java/caching/package-info.java | 5 / 5 / 0 (0.000%) | 5 / 5 / 0 (0.000%) | 5 / 5 / 0 (0.000%) | 5 / 5 / 0 (0.000%) |
| java-frontend/src/main/java/org/sonar/java/cfg/CFG.java | 2337 / 1734 / 603 (25.802%) | 2337 / 2337 / 0 (0.000%) | 2337 / 2337 / 0 (0.000%) | 2337 / 2337 / 0 (0.000%) |
| java-frontend/src/main/java/org/sonar/java/cfg/CFGLoop.java | 315 / 184 / 131 (41.587%) | 315 / 315 / 0 (0.000%) | 315 / 315 / 0 (0.000%) | 315 / 315 / 0 (0.000%) |
| java-frontend/src/main/java/org/sonar/java/cfg/CFGUtils.java | 243 / 119 / 124 (51.029%) | 243 / 243 / 0 (0.000%) | 243 / 243 / 0 (0.000%) | 243 / 243 / 0 (0.000%) |
| java-frontend/src/main/java/org/sonar/java/cfg/LiveVariables.java | 480 / 261 / 219 (45.625%) | 480 / 480 / 0 (0.000%) | 480 / 480 / 0 (0.000%) | 480 / 480 / 0 (0.000%) |
| java-frontend/src/main/java/org/sonar/java/cfg/VariableReadExtractor.java | 110 / 63 / 47 (42.727%) | 110 / 110 / 0 (0.000%) | 110 / 110 / 0 (0.000%) | 110 / 110 / 0 (0.000%) |
| java-frontend/src/main/java/org/sonar/java/cfg/package-info.java | 5 / 5 / 0 (0.000%) | 5 / 5 / 0 (0.000%) | 5 / 5 / 0 (0.000%) | 5 / 5 / 0 (0.000%) |
| java-frontend/src/main/java/org/sonar/java/classpath/AbstractClasspath.java | 753 / 733 / 20 (2.656%) | 753 / 753 / 0 (0.000%) | 753 / 753 / 0 (0.000%) | 753 / 753 / 0 (0.000%) |
| java-frontend/src/main/java/org/sonar/java/classpath/ClasspathForMain.java | 32 / 25 / 7 (21.875%) | 32 / 32 / 0 (0.000%) | 32 / 32 / 0 (0.000%) | 32 / 32 / 0 (0.000%) |
| java-frontend/src/main/java/org/sonar/java/classpath/ClasspathForTest.java | 32 / 25 / 7 (21.875%) | 32 / 32 / 0 (0.000%) | 32 / 32 / 0 (0.000%) | 32 / 32 / 0 (0.000%) |
| java-frontend/src/main/java/org/sonar/java/classpath/ClasspathProperties.java | 70 / 70 / 0 (0.000%) | 70 / 70 / 0 (0.000%) | 70 / 70 / 0 (0.000%) | 70 / 70 / 0 (0.000%) |
| java-frontend/src/main/java/org/sonar/java/classpath/DependencyVersionInference.java | 62 / 45 / 17 (27.419%) | 62 / 62 / 0 (0.000%) | 62 / 62 / 0 (0.000%) | 62 / 62 / 0 (0.000%) |
| java-frontend/src/main/java/org/sonar/java/classpath/JavaSdkUtil.java | 299 / 298 / 1 (0.334%) | 299 / 299 / 0 (0.000%) | 299 / 299 / 0 (0.000%) | 299 / 299 / 0 (0.000%) |
| java-frontend/src/main/java/org/sonar/java/classpath/VersionImpl.java | 191 / 171 / 20 (10.471%) | 191 / 191 / 0 (0.000%) | 191 / 191 / 0 (0.000%) | 191 / 191 / 0 (0.000%) |
| java-frontend/src/main/java/org/sonar/java/classpath/package-info.java | 7 / 7 / 0 (0.000%) | 7 / 7 / 0 (0.000%) | 7 / 7 / 0 (0.000%) | 7 / 7 / 0 (0.000%) |
| java-frontend/src/main/java/org/sonar/java/collections/CollectionUtils.java | 48 / 48 / 0 (0.000%) | 48 / 48 / 0 (0.000%) | 48 / 48 / 0 (0.000%) | 48 / 48 / 0 (0.000%) |
| java-frontend/src/main/java/org/sonar/java/collections/package-info.java | 5 / 4 / 1 (20.000%) | 5 / 5 / 0 (0.000%) | 5 / 5 / 0 (0.000%) | 5 / 5 / 0 (0.000%) |
| java-frontend/src/main/java/org/sonar/java/exceptions/ApiMismatchException.java | 11 / 11 / 0 (0.000%) | 11 / 11 / 0 (0.000%) | 11 / 11 / 0 (0.000%) | 11 / 11 / 0 (0.000%) |
| java-frontend/src/main/java/org/sonar/java/exceptions/ThrowableUtils.java | 35 / 35 / 0 (0.000%) | 35 / 35 / 0 (0.000%) | 35 / 35 / 0 (0.000%) | 35 / 35 / 0 (0.000%) |
| java-frontend/src/main/java/org/sonar/java/exceptions/package-info.java | 7 / 7 / 0 (0.000%) | 7 / 7 / 0 (0.000%) | 7 / 7 / 0 (0.000%) | 7 / 7 / 0 (0.000%) |
| java-frontend/src/main/java/org/sonar/java/filters/SonarJavaIssueFilter.java | 7 / 6 / 1 (14.286%) | 7 / 7 / 0 (0.000%) | 7 / 7 / 0 (0.000%) | 7 / 7 / 0 (0.000%) |
| java-frontend/src/main/java/org/sonar/java/filters/package-info.java | 7 / 7 / 0 (0.000%) | 7 / 7 / 0 (0.000%) | 7 / 7 / 0 (0.000%) | 7 / 7 / 0 (0.000%) |
| java-frontend/src/main/java/org/sonar/java/matcher/MethodMatcherFactory.java | 137 / 114 / 23 (16.788%) | 137 / 137 / 0 (0.000%) | 137 / 137 / 0 (0.000%) | 137 / 137 / 0 (0.000%) |
| java-frontend/src/main/java/org/sonar/java/matcher/MethodMatchersBuilder.java | 490 / 345 / 145 (29.592%) | 490 / 490 / 0 (0.000%) | 490 / 490 / 0 (0.000%) | 490 / 490 / 0 (0.000%) |
| java-frontend/src/main/java/org/sonar/java/matcher/MethodMatchersList.java | 72 / 31 / 41 (56.944%) | 72 / 72 / 0 (0.000%) | 72 / 72 / 0 (0.000%) | 72 / 72 / 0 (0.000%) |
| java-frontend/src/main/java/org/sonar/java/matcher/NoneMethodMatchers.java | 35 / 29 / 6 (17.143%) | 35 / 35 / 0 (0.000%) | 35 / 35 / 0 (0.000%) | 35 / 35 / 0 (0.000%) |
| java-frontend/src/main/java/org/sonar/java/matcher/TreeMatcher.java | 366 / 203 / 163 (44.536%) | 366 / 366 / 0 (0.000%) | 366 / 366 / 0 (0.000%) | 366 / 366 / 0 (0.000%) |
| java-frontend/src/main/java/org/sonar/java/matcher/package-info.java | 6 / 5 / 1 (16.667%) | 6 / 6 / 0 (0.000%) | 6 / 6 / 0 (0.000%) | 6 / 6 / 0 (0.000%) |
| java-frontend/src/main/java/org/sonar/java/metrics/MetricsComputer.java | 238 / 141 / 97 (40.756%) | 238 / 238 / 0 (0.000%) | 238 / 238 / 0 (0.000%) | 238 / 238 / 0 (0.000%) |
| java-frontend/src/main/java/org/sonar/java/metrics/MetricsScannerContext.java | 7 / 6 / 1 (14.286%) | 7 / 7 / 0 (0.000%) | 7 / 7 / 0 (0.000%) | 7 / 7 / 0 (0.000%) |
| java-frontend/src/main/java/org/sonar/java/metrics/package-info.java | 6 / 5 / 1 (16.667%) | 6 / 6 / 0 (0.000%) | 6 / 6 / 0 (0.000%) | 6 / 6 / 0 (0.000%) |
| java-frontend/src/main/java/org/sonar/java/model/AbstractTypedTree.java | 18 / 11 / 7 (38.889%) | 18 / 18 / 0 (0.000%) | 18 / 18 / 0 (0.000%) | 18 / 18 / 0 (0.000%) |
| java-frontend/src/main/java/org/sonar/java/model/AnnotationValueImpl.java | 30 / 28 / 2 (6.667%) | 30 / 30 / 0 (0.000%) | 30 / 30 / 0 (0.000%) | 30 / 30 / 0 (0.000%) |
| java-frontend/src/main/java/org/sonar/java/model/ArrayDimensionTreeImpl.java | 103 / 71 / 32 (31.068%) | 103 / 103 / 0 (0.000%) | 103 / 103 / 0 (0.000%) | 103 / 103 / 0 (0.000%) |
| java-frontend/src/main/java/org/sonar/java/model/DefaultInputFileScannerContext.java | 71 / 60 / 11 (15.493%) | 71 / 71 / 0 (0.000%) | 71 / 71 / 0 (0.000%) | 71 / 71 / 0 (0.000%) |
| java-frontend/src/main/java/org/sonar/java/model/DefaultJavaFileScannerContext.java | 545 / 356 / 189 (34.679%) | 545 / 545 / 0 (0.000%) | 545 / 545 / 0 (0.000%) | 545 / 545 / 0 (0.000%) |
| java-frontend/src/main/java/org/sonar/java/model/DefaultModuleScannerContext.java | 120 / 91 / 29 (24.167%) | 120 / 120 / 0 (0.000%) | 120 / 120 / 0 (0.000%) | 120 / 120 / 0 (0.000%) |
| java-frontend/src/main/java/org/sonar/java/model/ExpressionUtils.java | 892 / 540 / 352 (39.462%) | 892 / 892 / 0 (0.000%) | 892 / 892 / 0 (0.000%) | 892 / 892 / 0 (0.000%) |
| java-frontend/src/main/java/org/sonar/java/model/GeneratedFile.java | 325 / 265 / 60 (18.462%) | 325 / 325 / 0 (0.000%) | 325 / 325 / 0 (0.000%) | 325 / 325 / 0 (0.000%) |
| java-frontend/src/main/java/org/sonar/java/model/InputFileUtils.java | 81 / 81 / 0 (0.000%) | 81 / 81 / 0 (0.000%) | 81 / 81 / 0 (0.000%) | 81 / 81 / 0 (0.000%) |
| java-frontend/src/main/java/org/sonar/java/model/InternalSyntaxToken.java | 124 / 92 / 32 (25.806%) | 124 / 124 / 0 (0.000%) | 124 / 124 / 0 (0.000%) | 124 / 124 / 0 (0.000%) |
| java-frontend/src/main/java/org/sonar/java/model/InternalSyntaxTrivia.java | 151 / 110 / 41 (27.152%) | 151 / 151 / 0 (0.000%) | 151 / 151 / 0 (0.000%) | 151 / 151 / 0 (0.000%) |
| java-frontend/src/main/java/org/sonar/java/model/JInitializerBlockSymbol.java | 133 / 104 / 29 (21.805%) | 133 / 133 / 0 (0.000%) | 133 / 133 / 0 (0.000%) | 133 / 133 / 0 (0.000%) |
| java-frontend/src/main/java/org/sonar/java/model/JLabelSymbol.java | 52 / 37 / 15 (28.846%) | 52 / 52 / 0 (0.000%) | 52 / 52 / 0 (0.000%) | 52 / 52 / 0 (0.000%) |
| java-frontend/src/main/java/org/sonar/java/model/JMethodSymbol.java | 280 / 194 / 86 (30.714%) | 280 / 280 / 0 (0.000%) | 280 / 280 / 0 (0.000%) | 280 / 280 / 0 (0.000%) |
| java-frontend/src/main/java/org/sonar/java/model/JPackageSymbol.java | 14 / 11 / 3 (21.429%) | 14 / 14 / 0 (0.000%) | 14 / 14 / 0 (0.000%) | 14 / 14 / 0 (0.000%) |
| java-frontend/src/main/java/org/sonar/java/model/JParser.java | 7102 / 5826 / 1276 (17.967%) | 7102 / 7102 / 0 (0.000%) | 7102 / 7102 / 0 (0.000%) | 7102 / 7102 / 0 (0.000%) |
| java-frontend/src/main/java/org/sonar/java/model/JParserConfig.java | 546 / 492 / 54 (9.890%) | 546 / 546 / 0 (0.000%) | 546 / 546 / 0 (0.000%) | 546 / 546 / 0 (0.000%) |
| java-frontend/src/main/java/org/sonar/java/model/JProblem.java | 198 / 172 / 26 (13.131%) | 198 / 198 / 0 (0.000%) | 198 / 198 / 0 (0.000%) | 198 / 198 / 0 (0.000%) |
| java-frontend/src/main/java/org/sonar/java/model/JSema.java | 286 / 180 / 106 (37.063%) | 286 / 286 / 0 (0.000%) | 286 / 286 / 0 (0.000%) | 286 / 286 / 0 (0.000%) |
| java-frontend/src/main/java/org/sonar/java/model/JSymbol.java | 659 / 540 / 119 (18.058%) | 659 / 659 / 0 (0.000%) | 659 / 659 / 0 (0.000%) | 659 / 659 / 0 (0.000%) |
| java-frontend/src/main/java/org/sonar/java/model/JSymbolMetadata.java | 774 / 537 / 237 (30.620%) | 774 / 774 / 0 (0.000%) | 774 / 774 / 0 (0.000%) | 774 / 774 / 0 (0.000%) |
| java-frontend/src/main/java/org/sonar/java/model/JSymbolMetadataNullabilityHelper.java | 769 / 498 / 271 (35.241%) | 769 / 769 / 0 (0.000%) | 769 / 769 / 0 (0.000%) | 769 / 769 / 0 (0.000%) |
| java-frontend/src/main/java/org/sonar/java/model/JType.java | 454 / 404 / 50 (11.013%) | 454 / 454 / 0 (0.000%) | 454 / 454 / 0 (0.000%) | 454 / 454 / 0 (0.000%) |
| java-frontend/src/main/java/org/sonar/java/model/JTypeSymbol.java | 261 / 176 / 85 (32.567%) | 261 / 261 / 0 (0.000%) | 261 / 261 / 0 (0.000%) | 261 / 261 / 0 (0.000%) |
| java-frontend/src/main/java/org/sonar/java/model/JUtils.java | 400 / 240 / 160 (40.000%) | 400 / 400 / 0 (0.000%) | 400 / 400 / 0 (0.000%) | 400 / 400 / 0 (0.000%) |
| java-frontend/src/main/java/org/sonar/java/model/JVariableSymbol.java | 179 / 143 / 36 (20.112%) | 179 / 179 / 0 (0.000%) | 179 / 179 / 0 (0.000%) | 179 / 179 / 0 (0.000%) |
| java-frontend/src/main/java/org/sonar/java/model/JWarning.java | 308 / 213 / 95 (30.844%) | 308 / 308 / 0 (0.000%) | 308 / 308 / 0 (0.000%) | 308 / 308 / 0 (0.000%) |
| java-frontend/src/main/java/org/sonar/java/model/JavaTree.java | 1054 / 733 / 321 (30.455%) | 1054 / 1054 / 0 (0.000%) | 1054 / 1054 / 0 (0.000%) | 1054 / 1054 / 0 (0.000%) |
| java-frontend/src/main/java/org/sonar/java/model/JavaVersionImpl.java | 283 / 266 / 17 (6.007%) | 283 / 283 / 0 (0.000%) | 283 / 283 / 0 (0.000%) | 283 / 283 / 0 (0.000%) |
| java-frontend/src/main/java/org/sonar/java/model/KeywordSuper.java | 33 / 18 / 15 (45.455%) | 33 / 33 / 0 (0.000%) | 33 / 33 / 0 (0.000%) | 33 / 33 / 0 (0.000%) |
| java-frontend/src/main/java/org/sonar/java/model/KeywordThis.java | 28 / 15 / 13 (46.429%) | 28 / 28 / 0 (0.000%) | 28 / 28 / 0 (0.000%) | 28 / 28 / 0 (0.000%) |
| java-frontend/src/main/java/org/sonar/java/model/LineColumnConverter.java | 75 / 71 / 4 (5.333%) | 75 / 75 / 0 (0.000%) | 75 / 75 / 0 (0.000%) | 75 / 75 / 0 (0.000%) |
| java-frontend/src/main/java/org/sonar/java/model/LineUtils.java | 87 / 67 / 20 (22.989%) | 87 / 87 / 0 (0.000%) | 87 / 87 / 0 (0.000%) | 87 / 87 / 0 (0.000%) |
| java-frontend/src/main/java/org/sonar/java/model/LiteralUtils.java | 399 / 276 / 123 (30.827%) | 399 / 399 / 0 (0.000%) | 399 / 399 / 0 (0.000%) | 399 / 399 / 0 (0.000%) |
| java-frontend/src/main/java/org/sonar/java/model/ModifiersUtils.java | 111 / 60 / 51 (45.946%) | 111 / 111 / 0 (0.000%) | 111 / 111 / 0 (0.000%) | 111 / 111 / 0 (0.000%) |
| java-frontend/src/main/java/org/sonar/java/model/SmapFile.java | 408 / 408 / 0 (0.000%) | 408 / 408 / 0 (0.000%) | 408 / 408 / 0 (0.000%) | 408 / 408 / 0 (0.000%) |
| java-frontend/src/main/java/org/sonar/java/model/Symbols.java | 323 / 234 / 89 (27.554%) | 323 / 323 / 0 (0.000%) | 323 / 323 / 0 (0.000%) | 323 / 323 / 0 (0.000%) |
| java-frontend/src/main/java/org/sonar/java/model/SyntacticEquivalence.java | 300 / 165 / 135 (45.000%) | 300 / 300 / 0 (0.000%) | 300 / 300 / 0 (0.000%) | 300 / 300 / 0 (0.000%) |
| java-frontend/src/main/java/org/sonar/java/model/TypeParameterTreeImpl.java | 97 / 64 / 33 (34.021%) | 97 / 97 / 0 (0.000%) | 97 / 97 / 0 (0.000%) | 97 / 97 / 0 (0.000%) |
| java-frontend/src/main/java/org/sonar/java/model/TypeUtils.java | 37 / 30 / 7 (18.919%) | 37 / 37 / 0 (0.000%) | 37 / 37 / 0 (0.000%) | 37 / 37 / 0 (0.000%) |
| java-frontend/src/main/java/org/sonar/java/model/VisitorsBridge.java | 849 / 571 / 278 (32.744%) | 849 / 849 / 0 (0.000%) | 849 / 849 / 0 (0.000%) | 849 / 849 / 0 (0.000%) |
| java-frontend/src/main/java/org/sonar/java/model/declaration/AnnotationTreeImpl.java | 63 / 45 / 18 (28.571%) | 63 / 63 / 0 (0.000%) | 63 / 63 / 0 (0.000%) | 63 / 63 / 0 (0.000%) |
| java-frontend/src/main/java/org/sonar/java/model/declaration/ClassTreeImpl.java | 381 / 274 / 107 (28.084%) | 381 / 381 / 0 (0.000%) | 381 / 381 / 0 (0.000%) | 381 / 381 / 0 (0.000%) |
| java-frontend/src/main/java/org/sonar/java/model/declaration/EnumConstantTreeImpl.java | 76 / 48 / 28 (36.842%) | 76 / 76 / 0 (0.000%) | 76 / 76 / 0 (0.000%) | 76 / 76 / 0 (0.000%) |
| java-frontend/src/main/java/org/sonar/java/model/declaration/ExportsDirectiveTreeImpl.java | 54 / 32 / 22 (40.741%) | 54 / 54 / 0 (0.000%) | 54 / 54 / 0 (0.000%) | 54 / 54 / 0 (0.000%) |
| java-frontend/src/main/java/org/sonar/java/model/declaration/MethodTreeImpl.java | 476 / 320 / 156 (32.773%) | 476 / 476 / 0 (0.000%) | 476 / 476 / 0 (0.000%) | 476 / 476 / 0 (0.000%) |
| java-frontend/src/main/java/org/sonar/java/model/declaration/ModifierKeywordTreeImpl.java | 28 / 20 / 8 (28.571%) | 28 / 28 / 0 (0.000%) | 28 / 28 / 0 (0.000%) | 28 / 28 / 0 (0.000%) |
| java-frontend/src/main/java/org/sonar/java/model/declaration/ModifiersTreeImpl.java | 81 / 50 / 31 (38.272%) | 81 / 81 / 0 (0.000%) | 81 / 81 / 0 (0.000%) | 81 / 81 / 0 (0.000%) |
| java-frontend/src/main/java/org/sonar/java/model/declaration/ModuleDeclarationTreeImpl.java | 139 / 92 / 47 (33.813%) | 139 / 139 / 0 (0.000%) | 139 / 139 / 0 (0.000%) | 139 / 139 / 0 (0.000%) |
| java-frontend/src/main/java/org/sonar/java/model/declaration/ModuleDirectiveTreeImpl.java | 31 / 23 / 8 (25.806%) | 31 / 31 / 0 (0.000%) | 31 / 31 / 0 (0.000%) | 31 / 31 / 0 (0.000%) |
| java-frontend/src/main/java/org/sonar/java/model/declaration/OpensDirectiveTreeImpl.java | 54 / 32 / 22 (40.741%) | 54 / 54 / 0 (0.000%) | 54 / 54 / 0 (0.000%) | 54 / 54 / 0 (0.000%) |
| java-frontend/src/main/java/org/sonar/java/model/declaration/ProvidesDirectiveTreeImpl.java | 78 / 51 / 27 (34.615%) | 78 / 78 / 0 (0.000%) | 78 / 78 / 0 (0.000%) | 78 / 78 / 0 (0.000%) |
| java-frontend/src/main/java/org/sonar/java/model/declaration/RequiresDirectiveTreeImpl.java | 63 / 42 / 21 (33.333%) | 63 / 63 / 0 (0.000%) | 63 / 63 / 0 (0.000%) | 63 / 63 / 0 (0.000%) |
| java-frontend/src/main/java/org/sonar/java/model/declaration/SimpleModuleDirectiveTreeImpl.java | 67 / 44 / 23 (34.328%) | 67 / 67 / 0 (0.000%) | 67 / 67 / 0 (0.000%) | 67 / 67 / 0 (0.000%) |
| java-frontend/src/main/java/org/sonar/java/model/declaration/UsesDirectiveTreeImpl.java | 51 / 33 / 18 (35.294%) | 51 / 51 / 0 (0.000%) | 51 / 51 / 0 (0.000%) | 51 / 51 / 0 (0.000%) |
| java-frontend/src/main/java/org/sonar/java/model/declaration/VariableTreeImpl.java | 205 / 155 / 50 (24.390%) | 205 / 205 / 0 (0.000%) | 205 / 205 / 0 (0.000%) | 205 / 205 / 0 (0.000%) |
| java-frontend/src/main/java/org/sonar/java/model/declaration/package-info.java | 9 / 8 / 1 (11.111%) | 9 / 9 / 0 (0.000%) | 9 / 9 / 0 (0.000%) | 9 / 9 / 0 (0.000%) |
| java-frontend/src/main/java/org/sonar/java/model/expression/ArrayAccessExpressionTreeImpl.java | 51 / 36 / 15 (29.412%) | 51 / 51 / 0 (0.000%) | 51 / 51 / 0 (0.000%) | 51 / 51 / 0 (0.000%) |
| java-frontend/src/main/java/org/sonar/java/model/expression/AssessableExpressionTree.java | 32 / 28 / 4 (12.500%) | 32 / 32 / 0 (0.000%) | 32 / 32 / 0 (0.000%) | 32 / 32 / 0 (0.000%) |
| java-frontend/src/main/java/org/sonar/java/model/expression/AssignmentExpressionTreeImpl.java | 71 / 53 / 18 (25.352%) | 71 / 71 / 0 (0.000%) | 71 / 71 / 0 (0.000%) | 71 / 71 / 0 (0.000%) |
| java-frontend/src/main/java/org/sonar/java/model/expression/BinaryExpressionTreeImpl.java | 75 / 57 / 18 (24.000%) | 75 / 75 / 0 (0.000%) | 75 / 75 / 0 (0.000%) | 75 / 75 / 0 (0.000%) |
| java-frontend/src/main/java/org/sonar/java/model/expression/ConditionalExpressionTreeImpl.java | 87 / 63 / 24 (27.586%) | 87 / 87 / 0 (0.000%) | 87 / 87 / 0 (0.000%) | 87 / 87 / 0 (0.000%) |
| java-frontend/src/main/java/org/sonar/java/model/expression/IdentifierTreeImpl.java | 133 / 95 / 38 (28.571%) | 133 / 133 / 0 (0.000%) | 133 / 133 / 0 (0.000%) | 133 / 133 / 0 (0.000%) |
| java-frontend/src/main/java/org/sonar/java/model/expression/InstanceOfTreeImpl.java | 139 / 89 / 50 (35.971%) | 139 / 139 / 0 (0.000%) | 139 / 139 / 0 (0.000%) | 139 / 139 / 0 (0.000%) |
| java-frontend/src/main/java/org/sonar/java/model/expression/InternalPostfixUnaryExpression.java | 26 / 17 / 9 (34.615%) | 26 / 26 / 0 (0.000%) | 26 / 26 / 0 (0.000%) | 26 / 26 / 0 (0.000%) |
| java-frontend/src/main/java/org/sonar/java/model/expression/InternalPrefixUnaryExpression.java | 26 / 17 / 9 (34.615%) | 26 / 26 / 0 (0.000%) | 26 / 26 / 0 (0.000%) | 26 / 26 / 0 (0.000%) |
| java-frontend/src/main/java/org/sonar/java/model/expression/InternalUnaryExpression.java | 53 / 40 / 13 (24.528%) | 53 / 53 / 0 (0.000%) | 53 / 53 / 0 (0.000%) | 53 / 53 / 0 (0.000%) |
| java-frontend/src/main/java/org/sonar/java/model/expression/LambdaExpressionTreeImpl.java | 148 / 101 / 47 (31.757%) | 148 / 148 / 0 (0.000%) | 148 / 148 / 0 (0.000%) | 148 / 148 / 0 (0.000%) |
| java-frontend/src/main/java/org/sonar/java/model/expression/LiteralTreeImpl.java | 53 / 39 / 14 (26.415%) | 53 / 53 / 0 (0.000%) | 53 / 53 / 0 (0.000%) | 53 / 53 / 0 (0.000%) |
| java-frontend/src/main/java/org/sonar/java/model/expression/MemberSelectExpressionTreeImpl.java | 92 / 66 / 26 (28.261%) | 92 / 92 / 0 (0.000%) | 92 / 92 / 0 (0.000%) | 92 / 92 / 0 (0.000%) |
| java-frontend/src/main/java/org/sonar/java/model/expression/MethodInvocationTreeImpl.java | 119 / 80 / 39 (32.773%) | 119 / 119 / 0 (0.000%) | 119 / 119 / 0 (0.000%) | 119 / 119 / 0 (0.000%) |
| java-frontend/src/main/java/org/sonar/java/model/expression/MethodReferenceTreeImpl.java | 86 / 64 / 22 (25.581%) | 86 / 86 / 0 (0.000%) | 86 / 86 / 0 (0.000%) | 86 / 86 / 0 (0.000%) |
| java-frontend/src/main/java/org/sonar/java/model/expression/NewArrayTreeImpl.java | 177 / 127 / 50 (28.249%) | 177 / 177 / 0 (0.000%) | 177 / 177 / 0 (0.000%) | 177 / 177 / 0 (0.000%) |
| java-frontend/src/main/java/org/sonar/java/model/expression/NewClassTreeImpl.java | 225 / 153 / 72 (32.000%) | 225 / 225 / 0 (0.000%) | 225 / 225 / 0 (0.000%) | 225 / 225 / 0 (0.000%) |
| java-frontend/src/main/java/org/sonar/java/model/expression/ParenthesizedTreeImpl.java | 65 / 47 / 18 (27.692%) | 65 / 65 / 0 (0.000%) | 65 / 65 / 0 (0.000%) | 65 / 65 / 0 (0.000%) |
| java-frontend/src/main/java/org/sonar/java/model/expression/TypeArgumentListTreeImpl.java | 61 / 43 / 18 (29.508%) | 61 / 61 / 0 (0.000%) | 61 / 61 / 0 (0.000%) | 61 / 61 / 0 (0.000%) |
| java-frontend/src/main/java/org/sonar/java/model/expression/TypeCastExpressionTreeImpl.java | 131 / 94 / 37 (28.244%) | 131 / 131 / 0 (0.000%) | 131 / 131 / 0 (0.000%) | 131 / 131 / 0 (0.000%) |
| java-frontend/src/main/java/org/sonar/java/model/expression/VarTypeTreeImpl.java | 65 / 43 / 22 (33.846%) | 65 / 65 / 0 (0.000%) | 65 / 65 / 0 (0.000%) | 65 / 65 / 0 (0.000%) |
| java-frontend/src/main/java/org/sonar/java/model/expression/package-info.java | 9 / 8 / 1 (11.111%) | 9 / 9 / 0 (0.000%) | 9 / 9 / 0 (0.000%) | 9 / 9 / 0 (0.000%) |
| java-frontend/src/main/java/org/sonar/java/model/location/InternalPosition.java | 114 / 101 / 13 (11.404%) | 114 / 114 / 0 (0.000%) | 114 / 114 / 0 (0.000%) | 114 / 114 / 0 (0.000%) |
| java-frontend/src/main/java/org/sonar/java/model/location/InternalRange.java | 103 / 83 / 20 (19.417%) | 103 / 103 / 0 (0.000%) | 103 / 103 / 0 (0.000%) | 103 / 103 / 0 (0.000%) |
| java-frontend/src/main/java/org/sonar/java/model/location/package-info.java | 9 / 8 / 1 (11.111%) | 9 / 9 / 0 (0.000%) | 9 / 9 / 0 (0.000%) | 9 / 9 / 0 (0.000%) |
| java-frontend/src/main/java/org/sonar/java/model/package-info.java | 8 / 7 / 1 (12.500%) | 8 / 8 / 0 (0.000%) | 8 / 8 / 0 (0.000%) | 8 / 8 / 0 (0.000%) |
| java-frontend/src/main/java/org/sonar/java/model/pattern/AbstractPatternTree.java | 58 / 44 / 14 (24.138%) | 58 / 58 / 0 (0.000%) | 58 / 58 / 0 (0.000%) | 58 / 58 / 0 (0.000%) |
| java-frontend/src/main/java/org/sonar/java/model/pattern/DefaultPatternTreeImpl.java | 42 / 29 / 13 (30.952%) | 42 / 42 / 0 (0.000%) | 42 / 42 / 0 (0.000%) | 42 / 42 / 0 (0.000%) |
| java-frontend/src/main/java/org/sonar/java/model/pattern/GuardedPatternTreeImpl.java | 66 / 47 / 19 (28.788%) | 66 / 66 / 0 (0.000%) | 66 / 66 / 0 (0.000%) | 66 / 66 / 0 (0.000%) |
| java-frontend/src/main/java/org/sonar/java/model/pattern/NullPatternTreeImpl.java | 60 / 43 / 17 (28.333%) | 60 / 60 / 0 (0.000%) | 60 / 60 / 0 (0.000%) | 60 / 60 / 0 (0.000%) |
| java-frontend/src/main/java/org/sonar/java/model/pattern/RecordPatternTreeImpl.java | 90 / 61 / 29 (32.222%) | 90 / 90 / 0 (0.000%) | 90 / 90 / 0 (0.000%) | 90 / 90 / 0 (0.000%) |
| java-frontend/src/main/java/org/sonar/java/model/pattern/TypePatternTreeImpl.java | 42 / 29 / 13 (30.952%) | 42 / 42 / 0 (0.000%) | 42 / 42 / 0 (0.000%) | 42 / 42 / 0 (0.000%) |
| java-frontend/src/main/java/org/sonar/java/model/pattern/package-info.java | 9 / 8 / 1 (11.111%) | 9 / 9 / 0 (0.000%) | 9 / 9 / 0 (0.000%) | 9 / 9 / 0 (0.000%) |
| java-frontend/src/main/java/org/sonar/java/model/springcontext/BeanDefinitionGatherer.java | 416 / 186 / 230 (55.288%) | 416 / 416 / 0 (0.000%) | 416 / 416 / 0 (0.000%) | 416 / 416 / 0 (0.000%) |
| java-frontend/src/main/java/org/sonar/java/model/springcontext/BeanDefinitionHolder.java | 287 / 253 / 34 (11.847%) | 287 / 287 / 0 (0.000%) | 287 / 287 / 0 (0.000%) | 287 / 287 / 0 (0.000%) |
| java-frontend/src/main/java/org/sonar/java/model/springcontext/BeanDefinitionRegistry.java | 77 / 49 / 28 (36.364%) | 77 / 77 / 0 (0.000%) | 77 / 77 / 0 (0.000%) | 77 / 77 / 0 (0.000%) |
| java-frontend/src/main/java/org/sonar/java/model/springcontext/BeanLocation.java | 24 / 18 / 6 (25.000%) | 24 / 24 / 0 (0.000%) | 24 / 24 / 0 (0.000%) | 24 / 24 / 0 (0.000%) |
| java-frontend/src/main/java/org/sonar/java/model/springcontext/ComponentScanPackageGatherer.java | 328 / 236 / 92 (28.049%) | 328 / 328 / 0 (0.000%) | 328 / 328 / 0 (0.000%) | 328 / 328 / 0 (0.000%) |
| java-frontend/src/main/java/org/sonar/java/model/springcontext/EntityClassToPropertiesIndex.java | 87 / 78 / 9 (10.345%) | 87 / 87 / 0 (0.000%) | 87 / 87 / 0 (0.000%) | 87 / 87 / 0 (0.000%) |
| java-frontend/src/main/java/org/sonar/java/model/springcontext/InjectionPoint.java | 42 / 31 / 11 (26.190%) | 42 / 42 / 0 (0.000%) | 42 / 42 / 0 (0.000%) | 42 / 42 / 0 (0.000%) |
| java-frontend/src/main/java/org/sonar/java/model/springcontext/ProfileExpression.java | 417 / 405 / 12 (2.878%) | 417 / 417 / 0 (0.000%) | 417 / 417 / 0 (0.000%) | 417 / 417 / 0 (0.000%) |
| java-frontend/src/main/java/org/sonar/java/model/springcontext/ProfileExpressionParser.java | 147 / 124 / 23 (15.646%) | 147 / 147 / 0 (0.000%) | 147 / 147 / 0 (0.000%) | 147 / 147 / 0 (0.000%) |
| java-frontend/src/main/java/org/sonar/java/model/springcontext/ProjectPackageScan.java | 92 / 85 / 7 (7.609%) | 92 / 92 / 0 (0.000%) | 92 / 92 / 0 (0.000%) | 92 / 92 / 0 (0.000%) |
| java-frontend/src/main/java/org/sonar/java/model/springcontext/SpringContextCacheHelper.java | 94 / 49 / 45 (47.872%) | 94 / 94 / 0 (0.000%) | 94 / 94 / 0 (0.000%) | 94 / 94 / 0 (0.000%) |
| java-frontend/src/main/java/org/sonar/java/model/springcontext/SpringContextModel.java | 61 / 38 / 23 (37.705%) | 61 / 61 / 0 (0.000%) | 61 / 61 / 0 (0.000%) | 61 / 61 / 0 (0.000%) |
| java-frontend/src/main/java/org/sonar/java/model/springcontext/SpringContextModelGatherer.java | 130 / 95 / 35 (26.923%) | 130 / 130 / 0 (0.000%) | 130 / 130 / 0 (0.000%) | 130 / 130 / 0 (0.000%) |
| java-frontend/src/main/java/org/sonar/java/model/springcontext/SpringContextModelGatherers.java | 18 / 13 / 5 (27.778%) | 18 / 18 / 0 (0.000%) | 18 / 18 / 0 (0.000%) | 18 / 18 / 0 (0.000%) |
| java-frontend/src/main/java/org/sonar/java/model/springcontext/SpringContextModelMetrics.java | 36 / 26 / 10 (27.778%) | 36 / 36 / 0 (0.000%) | 36 / 36 / 0 (0.000%) | 36 / 36 / 0 (0.000%) |
| java-frontend/src/main/java/org/sonar/java/model/springcontext/TypeToBeansIndex.java | 153 / 140 / 13 (8.497%) | 153 / 153 / 0 (0.000%) | 153 / 153 / 0 (0.000%) | 153 / 153 / 0 (0.000%) |
| java-frontend/src/main/java/org/sonar/java/model/springcontext/TypeToDependenciesIndex.java | 88 / 59 / 29 (32.955%) | 88 / 88 / 0 (0.000%) | 88 / 88 / 0 (0.000%) | 88 / 88 / 0 (0.000%) |
| java-frontend/src/main/java/org/sonar/java/model/springcontext/package-info.java | 7 / 6 / 1 (14.286%) | 7 / 7 / 0 (0.000%) | 7 / 7 / 0 (0.000%) | 7 / 7 / 0 (0.000%) |
| java-frontend/src/main/java/org/sonar/java/model/statement/AssertStatementTreeImpl.java | 108 / 83 / 25 (23.148%) | 108 / 108 / 0 (0.000%) | 108 / 108 / 0 (0.000%) | 108 / 108 / 0 (0.000%) |
| java-frontend/src/main/java/org/sonar/java/model/statement/BlockTreeImpl.java | 92 / 65 / 27 (29.348%) | 92 / 92 / 0 (0.000%) | 92 / 92 / 0 (0.000%) | 92 / 92 / 0 (0.000%) |
| java-frontend/src/main/java/org/sonar/java/model/statement/BreakStatementTreeImpl.java | 80 / 59 / 21 (26.250%) | 80 / 80 / 0 (0.000%) | 80 / 80 / 0 (0.000%) | 80 / 80 / 0 (0.000%) |
| java-frontend/src/main/java/org/sonar/java/model/statement/CaseGroupTreeImpl.java | 62 / 42 / 20 (32.258%) | 62 / 62 / 0 (0.000%) | 62 / 62 / 0 (0.000%) | 62 / 62 / 0 (0.000%) |
| java-frontend/src/main/java/org/sonar/java/model/statement/CaseLabelTreeImpl.java | 82 / 56 / 26 (31.707%) | 82 / 82 / 0 (0.000%) | 82 / 82 / 0 (0.000%) | 82 / 82 / 0 (0.000%) |
| java-frontend/src/main/java/org/sonar/java/model/statement/CatchTreeImpl.java | 91 / 67 / 24 (26.374%) | 91 / 91 / 0 (0.000%) | 91 / 91 / 0 (0.000%) | 91 / 91 / 0 (0.000%) |
| java-frontend/src/main/java/org/sonar/java/model/statement/ContinueStatementTreeImpl.java | 76 / 57 / 19 (25.000%) | 76 / 76 / 0 (0.000%) | 76 / 76 / 0 (0.000%) | 76 / 76 / 0 (0.000%) |
| java-frontend/src/main/java/org/sonar/java/model/statement/DoWhileStatementTreeImpl.java | 115 / 85 / 30 (26.087%) | 115 / 115 / 0 (0.000%) | 115 / 115 / 0 (0.000%) | 115 / 115 / 0 (0.000%) |
| java-frontend/src/main/java/org/sonar/java/model/statement/EmptyStatementTreeImpl.java | 40 / 27 / 13 (32.500%) | 40 / 40 / 0 (0.000%) | 40 / 40 / 0 (0.000%) | 40 / 40 / 0 (0.000%) |
| java-frontend/src/main/java/org/sonar/java/model/statement/ExpressionStatementTreeImpl.java | 62 / 46 / 16 (25.806%) | 62 / 62 / 0 (0.000%) | 62 / 62 / 0 (0.000%) | 62 / 62 / 0 (0.000%) |
| java-frontend/src/main/java/org/sonar/java/model/statement/ForEachStatementImpl.java | 117 / 87 / 30 (25.641%) | 117 / 117 / 0 (0.000%) | 117 / 117 / 0 (0.000%) | 117 / 117 / 0 (0.000%) |
| java-frontend/src/main/java/org/sonar/java/model/statement/ForStatementTreeImpl.java | 174 / 121 / 53 (30.460%) | 174 / 174 / 0 (0.000%) | 174 / 174 / 0 (0.000%) | 174 / 174 / 0 (0.000%) |
| java-frontend/src/main/java/org/sonar/java/model/statement/IfStatementTreeImpl.java | 125 / 94 / 31 (24.800%) | 125 / 125 / 0 (0.000%) | 125 / 125 / 0 (0.000%) | 125 / 125 / 0 (0.000%) |
| java-frontend/src/main/java/org/sonar/java/model/statement/LabeledStatementTreeImpl.java | 74 / 53 / 21 (28.378%) | 74 / 74 / 0 (0.000%) | 74 / 74 / 0 (0.000%) | 74 / 74 / 0 (0.000%) |
| java-frontend/src/main/java/org/sonar/java/model/statement/ReturnStatementTreeImpl.java | 78 / 55 / 23 (29.487%) | 78 / 78 / 0 (0.000%) | 78 / 78 / 0 (0.000%) | 78 / 78 / 0 (0.000%) |
| java-frontend/src/main/java/org/sonar/java/model/statement/StaticInitializerTreeImpl.java | 51 / 30 / 21 (41.176%) | 51 / 51 / 0 (0.000%) | 51 / 51 / 0 (0.000%) | 51 / 51 / 0 (0.000%) |
| java-frontend/src/main/java/org/sonar/java/model/statement/SwitchExpressionTreeImpl.java | 44 / 28 / 16 (36.364%) | 44 / 44 / 0 (0.000%) | 44 / 44 / 0 (0.000%) | 44 / 44 / 0 (0.000%) |
| java-frontend/src/main/java/org/sonar/java/model/statement/SwitchStatementTreeImpl.java | 45 / 28 / 17 (37.778%) | 45 / 45 / 0 (0.000%) | 45 / 45 / 0 (0.000%) | 45 / 45 / 0 (0.000%) |
| java-frontend/src/main/java/org/sonar/java/model/statement/SwitchTreeImpl.java | 112 / 84 / 28 (25.000%) | 112 / 112 / 0 (0.000%) | 112 / 112 / 0 (0.000%) | 112 / 112 / 0 (0.000%) |
| java-frontend/src/main/java/org/sonar/java/model/statement/SynchronizedStatementTreeImpl.java | 91 / 67 / 24 (26.374%) | 91 / 91 / 0 (0.000%) | 91 / 91 / 0 (0.000%) | 91 / 91 / 0 (0.000%) |
| java-frontend/src/main/java/org/sonar/java/model/statement/ThrowStatementTreeImpl.java | 65 / 47 / 18 (27.692%) | 65 / 65 / 0 (0.000%) | 65 / 65 / 0 (0.000%) | 65 / 65 / 0 (0.000%) |
| java-frontend/src/main/java/org/sonar/java/model/statement/TryStatementTreeImpl.java | 162 / 114 / 48 (29.630%) | 162 / 162 / 0 (0.000%) | 162 / 162 / 0 (0.000%) | 162 / 162 / 0 (0.000%) |
| java-frontend/src/main/java/org/sonar/java/model/statement/WhileStatementTreeImpl.java | 91 / 67 / 24 (26.374%) | 91 / 91 / 0 (0.000%) | 91 / 91 / 0 (0.000%) | 91 / 91 / 0 (0.000%) |
| java-frontend/src/main/java/org/sonar/java/model/statement/YieldStatementTreeImpl.java | 71 / 53 / 18 (25.352%) | 71 / 71 / 0 (0.000%) | 71 / 71 / 0 (0.000%) | 71 / 71 / 0 (0.000%) |
| java-frontend/src/main/java/org/sonar/java/model/statement/package-info.java | 8 / 8 / 0 (0.000%) | 8 / 8 / 0 (0.000%) | 8 / 8 / 0 (0.000%) | 8 / 8 / 0 (0.000%) |
| java-frontend/src/main/java/org/sonar/java/package-info.java | 4 / 4 / 0 (0.000%) | 4 / 4 / 0 (0.000%) | 4 / 4 / 0 (0.000%) | 4 / 4 / 0 (0.000%) |
| java-frontend/src/main/java/org/sonar/java/regex/JavaAnalyzerRegexSource.java | 395 / 284 / 111 (28.101%) | 395 / 395 / 0 (0.000%) | 395 / 395 / 0 (0.000%) | 395 / 395 / 0 (0.000%) |
| java-frontend/src/main/java/org/sonar/java/regex/RegexCache.java | 38 / 17 / 21 (55.263%) | 38 / 38 / 0 (0.000%) | 38 / 38 / 0 (0.000%) | 38 / 38 / 0 (0.000%) |
| java-frontend/src/main/java/org/sonar/java/regex/RegexCheck.java | 159 / 126 / 33 (20.755%) | 159 / 159 / 0 (0.000%) | 159 / 159 / 0 (0.000%) | 159 / 159 / 0 (0.000%) |
| java-frontend/src/main/java/org/sonar/java/regex/RegexScannerContext.java | 39 / 29 / 10 (25.641%) | 39 / 39 / 0 (0.000%) | 39 / 39 / 0 (0.000%) | 39 / 39 / 0 (0.000%) |
| java-frontend/src/main/java/org/sonar/java/regex/package-info.java | 5 / 5 / 0 (0.000%) | 5 / 5 / 0 (0.000%) | 5 / 5 / 0 (0.000%) | 5 / 5 / 0 (0.000%) |
| java-frontend/src/main/java/org/sonar/java/reporting/AnalyzerMessage.java | 315 / 278 / 37 (11.746%) | 315 / 315 / 0 (0.000%) | 315 / 315 / 0 (0.000%) | 315 / 315 / 0 (0.000%) |
| java-frontend/src/main/java/org/sonar/java/reporting/FluentReporting.java | 72 / 53 / 19 (26.389%) | 72 / 72 / 0 (0.000%) | 72 / 72 / 0 (0.000%) | 72 / 72 / 0 (0.000%) |
| java-frontend/src/main/java/org/sonar/java/reporting/InternalJavaIssueBuilder.java | 587 / 438 / 149 (25.383%) | 587 / 587 / 0 (0.000%) | 587 / 587 / 0 (0.000%) | 587 / 587 / 0 (0.000%) |
| java-frontend/src/main/java/org/sonar/java/reporting/JavaIssue.java | 173 / 144 / 29 (16.763%) | 173 / 173 / 0 (0.000%) | 173 / 173 / 0 (0.000%) | 173 / 173 / 0 (0.000%) |
| java-frontend/src/main/java/org/sonar/java/reporting/JavaQuickFix.java | 143 / 111 / 32 (22.378%) | 143 / 143 / 0 (0.000%) | 143 / 143 / 0 (0.000%) | 143 / 143 / 0 (0.000%) |
| java-frontend/src/main/java/org/sonar/java/reporting/JavaTextEdit.java | 178 / 138 / 40 (22.472%) | 178 / 178 / 0 (0.000%) | 178 / 178 / 0 (0.000%) | 178 / 178 / 0 (0.000%) |
| java-frontend/src/main/java/org/sonar/java/reporting/package-info.java | 5 / 5 / 0 (0.000%) | 5 / 5 / 0 (0.000%) | 5 / 5 / 0 (0.000%) | 5 / 5 / 0 (0.000%) |
| java-frontend/src/main/java/org/sonar/java/serialization/BeanDefinitionHolderTypeAdapter.java | 347 / 214 / 133 (38.329%) | 347 / 347 / 0 (0.000%) | 347 / 347 / 0 (0.000%) | 347 / 347 / 0 (0.000%) |
| java-frontend/src/main/java/org/sonar/java/serialization/InjectionPointTypeAdapter.java | 97 / 62 / 35 (36.082%) | 97 / 97 / 0 (0.000%) | 97 / 97 / 0 (0.000%) | 97 / 97 / 0 (0.000%) |
| java-frontend/src/main/java/org/sonar/java/serialization/JsonUtils.java | 294 / 294 / 0 (0.000%) | 294 / 294 / 0 (0.000%) | 294 / 294 / 0 (0.000%) | 294 / 294 / 0 (0.000%) |
| java-frontend/src/main/java/org/sonar/java/serialization/TextSpanTypeAdapter.java | 132 / 93 / 39 (29.545%) | 132 / 132 / 0 (0.000%) | 132 / 132 / 0 (0.000%) | 132 / 132 / 0 (0.000%) |
| java-frontend/src/main/java/org/sonar/java/serialization/package-info.java | 5 / 5 / 0 (0.000%) | 5 / 5 / 0 (0.000%) | 5 / 5 / 0 (0.000%) | 5 / 5 / 0 (0.000%) |
| java-frontend/src/main/java/org/sonar/java/telemetry/AlphaNumericComparator.java | 87 / 87 / 0 (0.000%) | 87 / 87 / 0 (0.000%) | 87 / 87 / 0 (0.000%) | 87 / 87 / 0 (0.000%) |
| java-frontend/src/main/java/org/sonar/java/telemetry/DefaultTelemetry.java | 157 / 95 / 62 (39.490%) | 157 / 157 / 0 (0.000%) | 157 / 157 / 0 (0.000%) | 157 / 157 / 0 (0.000%) |
| java-frontend/src/main/java/org/sonar/java/telemetry/NoOpTelemetry.java | 33 / 28 / 5 (15.152%) | 33 / 33 / 0 (0.000%) | 33 / 33 / 0 (0.000%) | 33 / 33 / 0 (0.000%) |
| java-frontend/src/main/java/org/sonar/java/telemetry/SizeEstimable.java | 8 / 7 / 1 (12.500%) | 8 / 8 / 0 (0.000%) | 8 / 8 / 0 (0.000%) | 8 / 8 / 0 (0.000%) |
| java-frontend/src/main/java/org/sonar/java/telemetry/SizeEstimator.java | 151 / 145 / 6 (3.974%) | 151 / 151 / 0 (0.000%) | 151 / 151 / 0 (0.000%) | 151 / 151 / 0 (0.000%) |
| java-frontend/src/main/java/org/sonar/java/telemetry/Telemetry.java | 28 / 24 / 4 (14.286%) | 28 / 28 / 0 (0.000%) | 28 / 28 / 0 (0.000%) | 28 / 28 / 0 (0.000%) |
| java-frontend/src/main/java/org/sonar/java/telemetry/TelemetryKey.java | 157 / 157 / 0 (0.000%) | 157 / 157 / 0 (0.000%) | 157 / 157 / 0 (0.000%) | 157 / 157 / 0 (0.000%) |
| java-frontend/src/main/java/org/sonar/java/telemetry/package-info.java | 5 / 5 / 0 (0.000%) | 5 / 5 / 0 (0.000%) | 5 / 5 / 0 (0.000%) | 5 / 5 / 0 (0.000%) |
| java-frontend/src/main/java/org/sonar/java/testing/JavaFileScannerContextForTests.java | 268 / 169 / 99 (36.940%) | 268 / 268 / 0 (0.000%) | 268 / 268 / 0 (0.000%) | 268 / 268 / 0 (0.000%) |
| java-frontend/src/main/java/org/sonar/java/testing/JavaIssueBuilderForTests.java | 178 / 69 / 109 (61.236%) | 178 / 178 / 0 (0.000%) | 178 / 178 / 0 (0.000%) | 178 / 178 / 0 (0.000%) |
| java-frontend/src/main/java/org/sonar/java/testing/VisitorsBridgeForTests.java | 190 / 149 / 41 (21.579%) | 190 / 190 / 0 (0.000%) | 190 / 190 / 0 (0.000%) | 190 / 190 / 0 (0.000%) |
| java-frontend/src/main/java/org/sonar/java/testing/package-info.java | 5 / 5 / 0 (0.000%) | 5 / 5 / 0 (0.000%) | 5 / 5 / 0 (0.000%) | 5 / 5 / 0 (0.000%) |
| java-frontend/src/main/java/org/sonar/java/utils/JavaFileTypeClassifier.java | 126 / 84 / 42 (33.333%) | 126 / 126 / 0 (0.000%) | 126 / 126 / 0 (0.000%) | 126 / 126 / 0 (0.000%) |
| java-frontend/src/main/java/org/sonar/java/utils/ModuleMetadataUtils.java | 88 / 88 / 0 (0.000%) | 88 / 88 / 0 (0.000%) | 88 / 88 / 0 (0.000%) | 88 / 88 / 0 (0.000%) |
| java-frontend/src/main/java/org/sonar/java/utils/PackageUtils.java | 73 / 53 / 20 (27.397%) | 73 / 73 / 0 (0.000%) | 73 / 73 / 0 (0.000%) | 73 / 73 / 0 (0.000%) |
| java-frontend/src/main/java/org/sonar/java/utils/SpringUtils.java | 616 / 320 / 296 (48.052%) | 616 / 616 / 0 (0.000%) | 616 / 616 / 0 (0.000%) | 616 / 616 / 0 (0.000%) |
| java-frontend/src/main/java/org/sonar/java/utils/StringUtils.java | 177 / 177 / 0 (0.000%) | 177 / 177 / 0 (0.000%) | 177 / 177 / 0 (0.000%) | 177 / 177 / 0 (0.000%) |
| java-frontend/src/main/java/org/sonar/java/utils/UnitTestUtils.java | 547 / 254 / 293 (53.565%) | 547 / 547 / 0 (0.000%) | 547 / 547 / 0 (0.000%) | 547 / 547 / 0 (0.000%) |
| java-frontend/src/main/java/org/sonar/java/utils/package-info.java | 5 / 5 / 0 (0.000%) | 5 / 5 / 0 (0.000%) | 5 / 5 / 0 (0.000%) | 5 / 5 / 0 (0.000%) |
| java-frontend/src/main/java/org/sonar/plugins/java/api/CheckRegistrar.java | 153 / 123 / 30 (19.608%) | 153 / 153 / 0 (0.000%) | 153 / 153 / 0 (0.000%) | 153 / 153 / 0 (0.000%) |
| java-frontend/src/main/java/org/sonar/plugins/java/api/DependencyVersionAware.java | 12 / 9 / 3 (25.000%) | 12 / 12 / 0 (0.000%) | 12 / 12 / 0 (0.000%) | 12 / 12 / 0 (0.000%) |
| java-frontend/src/main/java/org/sonar/plugins/java/api/InputFileScannerContext.java | 29 / 25 / 4 (13.793%) | 29 / 29 / 0 (0.000%) | 29 / 29 / 0 (0.000%) | 29 / 29 / 0 (0.000%) |
| java-frontend/src/main/java/org/sonar/plugins/java/api/IssuableSubscriptionVisitor.java | 114 / 84 / 30 (26.316%) | 114 / 114 / 0 (0.000%) | 114 / 114 / 0 (0.000%) | 114 / 114 / 0 (0.000%) |
| java-frontend/src/main/java/org/sonar/plugins/java/api/JavaCheck.java | 6 / 6 / 0 (0.000%) | 6 / 6 / 0 (0.000%) | 6 / 6 / 0 (0.000%) | 6 / 6 / 0 (0.000%) |
| java-frontend/src/main/java/org/sonar/plugins/java/api/JavaFileScanner.java | 15 / 10 / 5 (33.333%) | 15 / 15 / 0 (0.000%) | 15 / 15 / 0 (0.000%) | 15 / 15 / 0 (0.000%) |
| java-frontend/src/main/java/org/sonar/plugins/java/api/JavaFileScannerContext.java | 135 / 113 / 22 (16.296%) | 135 / 135 / 0 (0.000%) | 135 / 135 / 0 (0.000%) | 135 / 135 / 0 (0.000%) |
| java-frontend/src/main/java/org/sonar/plugins/java/api/JavaResourceLocator.java | 30 / 28 / 2 (6.667%) | 30 / 30 / 0 (0.000%) | 30 / 30 / 0 (0.000%) | 30 / 30 / 0 (0.000%) |
| java-frontend/src/main/java/org/sonar/plugins/java/api/JavaVersion.java | 42 / 41 / 1 (2.381%) | 42 / 42 / 0 (0.000%) | 42 / 42 / 0 (0.000%) | 42 / 42 / 0 (0.000%) |
| java-frontend/src/main/java/org/sonar/plugins/java/api/JavaVersionAwareVisitor.java | 10 / 8 / 2 (20.000%) | 10 / 10 / 0 (0.000%) | 10 / 10 / 0 (0.000%) | 10 / 10 / 0 (0.000%) |
| java-frontend/src/main/java/org/sonar/plugins/java/api/JspCodeVisitor.java | 8 / 6 / 2 (25.000%) | 8 / 8 / 0 (0.000%) | 8 / 8 / 0 (0.000%) | 8 / 8 / 0 (0.000%) |
| java-frontend/src/main/java/org/sonar/plugins/java/api/ModuleScannerContext.java | 34 / 31 / 3 (8.824%) | 34 / 34 / 0 (0.000%) | 34 / 34 / 0 (0.000%) | 34 / 34 / 0 (0.000%) |
| java-frontend/src/main/java/org/sonar/plugins/java/api/ProfileRegistrar.java | 25 / 24 / 1 (4.000%) | 25 / 25 / 0 (0.000%) | 25 / 25 / 0 (0.000%) | 25 / 25 / 0 (0.000%) |
| java-frontend/src/main/java/org/sonar/plugins/java/api/SourceMap.java | 17 / 15 / 2 (11.765%) | 17 / 17 / 0 (0.000%) | 17 / 17 / 0 (0.000%) | 17 / 17 / 0 (0.000%) |
| java-frontend/src/main/java/org/sonar/plugins/java/api/Version.java | 38 / 38 / 0 (0.000%) | 38 / 38 / 0 (0.000%) | 38 / 38 / 0 (0.000%) | 38 / 38 / 0 (0.000%) |
| java-frontend/src/main/java/org/sonar/plugins/java/api/caching/CacheContext.java | 12 / 10 / 2 (16.667%) | 12 / 12 / 0 (0.000%) | 12 / 12 / 0 (0.000%) | 12 / 12 / 0 (0.000%) |
| java-frontend/src/main/java/org/sonar/plugins/java/api/caching/JavaReadCache.java | 18 / 18 / 0 (0.000%) | 18 / 18 / 0 (0.000%) | 18 / 18 / 0 (0.000%) | 18 / 18 / 0 (0.000%) |
| java-frontend/src/main/java/org/sonar/plugins/java/api/caching/JavaWriteCache.java | 19 / 19 / 0 (0.000%) | 19 / 19 / 0 (0.000%) | 19 / 19 / 0 (0.000%) | 19 / 19 / 0 (0.000%) |
| java-frontend/src/main/java/org/sonar/plugins/java/api/caching/SonarLintCache.java | 67 / 67 / 0 (0.000%) | 67 / 67 / 0 (0.000%) | 67 / 67 / 0 (0.000%) | 67 / 67 / 0 (0.000%) |
| java-frontend/src/main/java/org/sonar/plugins/java/api/caching/package-info.java | 7 / 7 / 0 (0.000%) | 7 / 7 / 0 (0.000%) | 7 / 7 / 0 (0.000%) | 7 / 7 / 0 (0.000%) |
| java-frontend/src/main/java/org/sonar/plugins/java/api/cfg/ControlFlowGraph.java | 51 / 44 / 7 (13.725%) | 51 / 51 / 0 (0.000%) | 51 / 51 / 0 (0.000%) | 51 / 51 / 0 (0.000%) |
| java-frontend/src/main/java/org/sonar/plugins/java/api/cfg/package-info.java | 10 / 9 / 1 (10.000%) | 10 / 10 / 0 (0.000%) | 10 / 10 / 0 (0.000%) | 10 / 10 / 0 (0.000%) |
| java-frontend/src/main/java/org/sonar/plugins/java/api/internal/EndOfAnalysis.java | 11 / 9 / 2 (18.182%) | 11 / 11 / 0 (0.000%) | 11 / 11 / 0 (0.000%) | 11 / 11 / 0 (0.000%) |
| java-frontend/src/main/java/org/sonar/plugins/java/api/internal/ModuleMetadata.java | 14 / 12 / 2 (14.286%) | 14 / 14 / 0 (0.000%) | 14 / 14 / 0 (0.000%) | 14 / 14 / 0 (0.000%) |
| java-frontend/src/main/java/org/sonar/plugins/java/api/internal/package-info.java | 7 / 7 / 0 (0.000%) | 7 / 7 / 0 (0.000%) | 7 / 7 / 0 (0.000%) | 7 / 7 / 0 (0.000%) |
| java-frontend/src/main/java/org/sonar/plugins/java/api/location/Position.java | 84 / 64 / 20 (23.810%) | 84 / 84 / 0 (0.000%) | 84 / 84 / 0 (0.000%) | 84 / 84 / 0 (0.000%) |
| java-frontend/src/main/java/org/sonar/plugins/java/api/location/Range.java | 58 / 40 / 18 (31.034%) | 58 / 58 / 0 (0.000%) | 58 / 58 / 0 (0.000%) | 58 / 58 / 0 (0.000%) |
| java-frontend/src/main/java/org/sonar/plugins/java/api/location/package-info.java | 10 / 9 / 1 (10.000%) | 10 / 10 / 0 (0.000%) | 10 / 10 / 0 (0.000%) | 10 / 10 / 0 (0.000%) |
| java-frontend/src/main/java/org/sonar/plugins/java/api/package-info.java | 6 / 6 / 0 (0.000%) | 6 / 6 / 0 (0.000%) | 6 / 6 / 0 (0.000%) | 6 / 6 / 0 (0.000%) |
| java-frontend/src/main/java/org/sonar/plugins/java/api/semantic/MethodMatchers.java | 109 / 94 / 15 (13.761%) | 109 / 109 / 0 (0.000%) | 109 / 109 / 0 (0.000%) | 109 / 109 / 0 (0.000%) |
| java-frontend/src/main/java/org/sonar/plugins/java/api/semantic/Sema.java | 11 / 10 / 1 (9.091%) | 11 / 11 / 0 (0.000%) | 11 / 11 / 0 (0.000%) | 11 / 11 / 0 (0.000%) |
| java-frontend/src/main/java/org/sonar/plugins/java/api/semantic/Symbol.java | 133 / 105 / 28 (21.053%) | 133 / 133 / 0 (0.000%) | 133 / 133 / 0 (0.000%) | 133 / 133 / 0 (0.000%) |
| java-frontend/src/main/java/org/sonar/plugins/java/api/semantic/SymbolMetadata.java | 106 / 103 / 3 (2.830%) | 106 / 106 / 0 (0.000%) | 106 / 106 / 0 (0.000%) | 106 / 106 / 0 (0.000%) |
| java-frontend/src/main/java/org/sonar/plugins/java/api/semantic/Type.java | 78 / 74 / 4 (5.128%) | 78 / 78 / 0 (0.000%) | 78 / 78 / 0 (0.000%) | 78 / 78 / 0 (0.000%) |
| java-frontend/src/main/java/org/sonar/plugins/java/api/semantic/package-info.java | 10 / 9 / 1 (10.000%) | 10 / 10 / 0 (0.000%) | 10 / 10 / 0 (0.000%) | 10 / 10 / 0 (0.000%) |
| java-frontend/src/main/java/org/sonar/plugins/java/api/tree/AnnotationTree.java | 16 / 10 / 6 (37.500%) | 16 / 16 / 0 (0.000%) | 16 / 16 / 0 (0.000%) | 16 / 16 / 0 (0.000%) |
| java-frontend/src/main/java/org/sonar/plugins/java/api/tree/Arguments.java | 15 / 11 / 4 (26.667%) | 15 / 15 / 0 (0.000%) | 15 / 15 / 0 (0.000%) | 15 / 15 / 0 (0.000%) |
| java-frontend/src/main/java/org/sonar/plugins/java/api/tree/ArrayAccessExpressionTree.java | 13 / 9 / 4 (30.769%) | 13 / 13 / 0 (0.000%) | 13 / 13 / 0 (0.000%) | 13 / 13 / 0 (0.000%) |
| java-frontend/src/main/java/org/sonar/plugins/java/api/tree/ArrayDimensionTree.java | 19 / 12 / 7 (36.842%) | 19 / 19 / 0 (0.000%) | 19 / 19 / 0 (0.000%) | 19 / 19 / 0 (0.000%) |
| java-frontend/src/main/java/org/sonar/plugins/java/api/tree/ArrayTypeTree.java | 21 / 14 / 7 (33.333%) | 21 / 21 / 0 (0.000%) | 21 / 21 / 0 (0.000%) | 21 / 21 / 0 (0.000%) |
| java-frontend/src/main/java/org/sonar/plugins/java/api/tree/AssertStatementTree.java | 21 / 14 / 7 (33.333%) | 21 / 21 / 0 (0.000%) | 21 / 21 / 0 (0.000%) | 21 / 21 / 0 (0.000%) |
| java-frontend/src/main/java/org/sonar/plugins/java/api/tree/AssignmentExpressionTree.java | 15 / 10 / 5 (33.333%) | 15 / 15 / 0 (0.000%) | 15 / 15 / 0 (0.000%) | 15 / 15 / 0 (0.000%) |
| java-frontend/src/main/java/org/sonar/plugins/java/api/tree/BaseTreeVisitor.java | 747 / 520 / 227 (30.388%) | 747 / 747 / 0 (0.000%) | 747 / 747 / 0 (0.000%) | 747 / 747 / 0 (0.000%) |
| java-frontend/src/main/java/org/sonar/plugins/java/api/tree/BinaryExpressionTree.java | 15 / 10 / 5 (33.333%) | 15 / 15 / 0 (0.000%) | 15 / 15 / 0 (0.000%) | 15 / 15 / 0 (0.000%) |
| java-frontend/src/main/java/org/sonar/plugins/java/api/tree/BlockTree.java | 16 / 10 / 6 (37.500%) | 16 / 16 / 0 (0.000%) | 16 / 16 / 0 (0.000%) | 16 / 16 / 0 (0.000%) |
| java-frontend/src/main/java/org/sonar/plugins/java/api/tree/BreakStatementTree.java | 16 / 11 / 5 (31.250%) | 16 / 16 / 0 (0.000%) | 16 / 16 / 0 (0.000%) | 16 / 16 / 0 (0.000%) |
| java-frontend/src/main/java/org/sonar/plugins/java/api/tree/CaseGroupTree.java | 15 / 9 / 6 (40.000%) | 15 / 15 / 0 (0.000%) | 15 / 15 / 0 (0.000%) | 15 / 15 / 0 (0.000%) |
| java-frontend/src/main/java/org/sonar/plugins/java/api/tree/CaseLabelTree.java | 17 / 11 / 6 (35.294%) | 17 / 17 / 0 (0.000%) | 17 / 17 / 0 (0.000%) | 17 / 17 / 0 (0.000%) |
| java-frontend/src/main/java/org/sonar/plugins/java/api/tree/CatchTree.java | 19 / 12 / 7 (36.842%) | 19 / 19 / 0 (0.000%) | 19 / 19 / 0 (0.000%) | 19 / 19 / 0 (0.000%) |
| java-frontend/src/main/java/org/sonar/plugins/java/api/tree/ClassTree.java | 50 / 28 / 22 (44.000%) | 50 / 50 / 0 (0.000%) | 50 / 50 / 0 (0.000%) | 50 / 50 / 0 (0.000%) |
| java-frontend/src/main/java/org/sonar/plugins/java/api/tree/CompilationUnitTree.java | 23 / 14 / 9 (39.130%) | 23 / 23 / 0 (0.000%) | 23 / 23 / 0 (0.000%) | 23 / 23 / 0 (0.000%) |
| java-frontend/src/main/java/org/sonar/plugins/java/api/tree/ConditionalExpressionTree.java | 19 / 12 / 7 (36.842%) | 19 / 19 / 0 (0.000%) | 19 / 19 / 0 (0.000%) | 19 / 19 / 0 (0.000%) |
| java-frontend/src/main/java/org/sonar/plugins/java/api/tree/ContinueStatementTree.java | 16 / 11 / 5 (31.250%) | 16 / 16 / 0 (0.000%) | 16 / 16 / 0 (0.000%) | 16 / 16 / 0 (0.000%) |
| java-frontend/src/main/java/org/sonar/plugins/java/api/tree/DefaultPatternTree.java | 11 / 8 / 3 (27.273%) | 11 / 11 / 0 (0.000%) | 11 / 11 / 0 (0.000%) | 11 / 11 / 0 (0.000%) |
| java-frontend/src/main/java/org/sonar/plugins/java/api/tree/DoWhileStatementTree.java | 23 / 14 / 9 (39.130%) | 23 / 23 / 0 (0.000%) | 23 / 23 / 0 (0.000%) | 23 / 23 / 0 (0.000%) |
| java-frontend/src/main/java/org/sonar/plugins/java/api/tree/EmptyStatementTree.java | 12 / 8 / 4 (33.333%) | 12 / 12 / 0 (0.000%) | 12 / 12 / 0 (0.000%) | 12 / 12 / 0 (0.000%) |
| java-frontend/src/main/java/org/sonar/plugins/java/api/tree/EnumConstantTree.java | 18 / 12 / 6 (33.333%) | 18 / 18 / 0 (0.000%) | 18 / 18 / 0 (0.000%) | 18 / 18 / 0 (0.000%) |
| java-frontend/src/main/java/org/sonar/plugins/java/api/tree/ExportsDirectiveTree.java | 17 / 11 / 6 (35.294%) | 17 / 17 / 0 (0.000%) | 17 / 17 / 0 (0.000%) | 17 / 17 / 0 (0.000%) |
| java-frontend/src/main/java/org/sonar/plugins/java/api/tree/ExpressionStatementTree.java | 13 / 9 / 4 (30.769%) | 13 / 13 / 0 (0.000%) | 13 / 13 / 0 (0.000%) | 13 / 13 / 0 (0.000%) |
| java-frontend/src/main/java/org/sonar/plugins/java/api/tree/ExpressionTree.java | 21 / 18 / 3 (14.286%) | 21 / 21 / 0 (0.000%) | 21 / 21 / 0 (0.000%) | 21 / 21 / 0 (0.000%) |
| java-frontend/src/main/java/org/sonar/plugins/java/api/tree/ForEachStatement.java | 23 / 14 / 9 (39.130%) | 23 / 23 / 0 (0.000%) | 23 / 23 / 0 (0.000%) | 23 / 23 / 0 (0.000%) |
| java-frontend/src/main/java/org/sonar/plugins/java/api/tree/ForStatementTree.java | 30 / 17 / 13 (43.333%) | 30 / 30 / 0 (0.000%) | 30 / 30 / 0 (0.000%) | 30 / 30 / 0 (0.000%) |
| java-frontend/src/main/java/org/sonar/plugins/java/api/tree/GuardedPatternTree.java | 15 / 10 / 5 (33.333%) | 15 / 15 / 0 (0.000%) | 15 / 15 / 0 (0.000%) | 15 / 15 / 0 (0.000%) |
| java-frontend/src/main/java/org/sonar/plugins/java/api/tree/IdentifierTree.java | 17 / 12 / 5 (29.412%) | 17 / 17 / 0 (0.000%) | 17 / 17 / 0 (0.000%) | 17 / 17 / 0 (0.000%) |
| java-frontend/src/main/java/org/sonar/plugins/java/api/tree/IfStatementTree.java | 25 / 16 / 9 (36.000%) | 25 / 25 / 0 (0.000%) | 25 / 25 / 0 (0.000%) | 25 / 25 / 0 (0.000%) |
| java-frontend/src/main/java/org/sonar/plugins/java/api/tree/ImportClauseTree.java | 9 / 7 / 2 (22.222%) | 9 / 9 / 0 (0.000%) | 9 / 9 / 0 (0.000%) | 9 / 9 / 0 (0.000%) |
| java-frontend/src/main/java/org/sonar/plugins/java/api/tree/ImportTree.java | 26 / 18 / 8 (30.769%) | 26 / 26 / 0 (0.000%) | 26 / 26 / 0 (0.000%) | 26 / 26 / 0 (0.000%) |
| java-frontend/src/main/java/org/sonar/plugins/java/api/tree/InferedTypeTree.java | 39 / 27 / 12 (30.769%) | 39 / 39 / 0 (0.000%) | 39 / 39 / 0 (0.000%) | 39 / 39 / 0 (0.000%) |
| java-frontend/src/main/java/org/sonar/plugins/java/api/tree/InstanceOfTree.java | 15 / 10 / 5 (33.333%) | 15 / 15 / 0 (0.000%) | 15 / 15 / 0 (0.000%) | 15 / 15 / 0 (0.000%) |
| java-frontend/src/main/java/org/sonar/plugins/java/api/tree/LabeledStatementTree.java | 18 / 11 / 7 (38.889%) | 18 / 18 / 0 (0.000%) | 18 / 18 / 0 (0.000%) | 18 / 18 / 0 (0.000%) |
| java-frontend/src/main/java/org/sonar/plugins/java/api/tree/LambdaExpressionTree.java | 27 / 16 / 11 (40.741%) | 27 / 27 / 0 (0.000%) | 27 / 27 / 0 (0.000%) | 27 / 27 / 0 (0.000%) |
| java-frontend/src/main/java/org/sonar/plugins/java/api/tree/ListTree.java | 15 / 9 / 6 (40.000%) | 15 / 15 / 0 (0.000%) | 15 / 15 / 0 (0.000%) | 15 / 15 / 0 (0.000%) |
| java-frontend/src/main/java/org/sonar/plugins/java/api/tree/LiteralTree.java | 13 / 10 / 3 (23.077%) | 13 / 13 / 0 (0.000%) | 13 / 13 / 0 (0.000%) | 13 / 13 / 0 (0.000%) |
| java-frontend/src/main/java/org/sonar/plugins/java/api/tree/MemberSelectExpressionTree.java | 16 / 10 / 6 (37.500%) | 16 / 16 / 0 (0.000%) | 16 / 16 / 0 (0.000%) | 16 / 16 / 0 (0.000%) |
| java-frontend/src/main/java/org/sonar/plugins/java/api/tree/MethodInvocationTree.java | 24 / 16 / 8 (33.333%) | 24 / 24 / 0 (0.000%) | 24 / 24 / 0 (0.000%) | 24 / 24 / 0 (0.000%) |
| java-frontend/src/main/java/org/sonar/plugins/java/api/tree/MethodReferenceTree.java | 18 / 12 / 6 (33.333%) | 18 / 18 / 0 (0.000%) | 18 / 18 / 0 (0.000%) | 18 / 18 / 0 (0.000%) |
| java-frontend/src/main/java/org/sonar/plugins/java/api/tree/MethodTree.java | 53 / 33 / 20 (37.736%) | 53 / 53 / 0 (0.000%) | 53 / 53 / 0 (0.000%) | 53 / 53 / 0 (0.000%) |
| java-frontend/src/main/java/org/sonar/plugins/java/api/tree/MethodsAreNonnullByDefault.java | 18 / 18 / 0 (0.000%) | 18 / 18 / 0 (0.000%) | 18 / 18 / 0 (0.000%) | 18 / 18 / 0 (0.000%) |
| java-frontend/src/main/java/org/sonar/plugins/java/api/tree/Modifier.java | 38 / 37 / 1 (2.632%) | 38 / 38 / 0 (0.000%) | 38 / 38 / 0 (0.000%) | 38 / 38 / 0 (0.000%) |
| java-frontend/src/main/java/org/sonar/plugins/java/api/tree/ModifierKeywordTree.java | 12 / 9 / 3 (25.000%) | 12 / 12 / 0 (0.000%) | 12 / 12 / 0 (0.000%) | 12 / 12 / 0 (0.000%) |
| java-frontend/src/main/java/org/sonar/plugins/java/api/tree/ModifierTree.java | 8 / 7 / 1 (12.500%) | 8 / 8 / 0 (0.000%) | 8 / 8 / 0 (0.000%) | 8 / 8 / 0 (0.000%) |
| java-frontend/src/main/java/org/sonar/plugins/java/api/tree/ModifiersTree.java | 16 / 9 / 7 (43.750%) | 16 / 16 / 0 (0.000%) | 16 / 16 / 0 (0.000%) | 16 / 16 / 0 (0.000%) |
| java-frontend/src/main/java/org/sonar/plugins/java/api/tree/ModuleDeclarationTree.java | 26 / 15 / 11 (42.308%) | 26 / 26 / 0 (0.000%) | 26 / 26 / 0 (0.000%) | 26 / 26 / 0 (0.000%) |
| java-frontend/src/main/java/org/sonar/plugins/java/api/tree/ModuleDirectiveTree.java | 13 / 9 / 4 (30.769%) | 13 / 13 / 0 (0.000%) | 13 / 13 / 0 (0.000%) | 13 / 13 / 0 (0.000%) |
| java-frontend/src/main/java/org/sonar/plugins/java/api/tree/ModuleNameTree.java | 10 / 7 / 3 (30.000%) | 10 / 10 / 0 (0.000%) | 10 / 10 / 0 (0.000%) | 10 / 10 / 0 (0.000%) |
| java-frontend/src/main/java/org/sonar/plugins/java/api/tree/NewArrayTree.java | 27 / 17 / 10 (37.037%) | 27 / 27 / 0 (0.000%) | 27 / 27 / 0 (0.000%) | 27 / 27 / 0 (0.000%) |
| java-frontend/src/main/java/org/sonar/plugins/java/api/tree/NewClassTree.java | 36 / 24 / 12 (33.333%) | 36 / 36 / 0 (0.000%) | 36 / 36 / 0 (0.000%) | 36 / 36 / 0 (0.000%) |
| java-frontend/src/main/java/org/sonar/plugins/java/api/tree/NullPatternTree.java | 11 / 8 / 3 (27.273%) | 11 / 11 / 0 (0.000%) | 11 / 11 / 0 (0.000%) | 11 / 11 / 0 (0.000%) |
| java-frontend/src/main/java/org/sonar/plugins/java/api/tree/OpensDirectiveTree.java | 17 / 11 / 6 (35.294%) | 17 / 17 / 0 (0.000%) | 17 / 17 / 0 (0.000%) | 17 / 17 / 0 (0.000%) |
| java-frontend/src/main/java/org/sonar/plugins/java/api/tree/PackageDeclarationTree.java | 18 / 11 / 7 (38.889%) | 18 / 18 / 0 (0.000%) | 18 / 18 / 0 (0.000%) | 18 / 18 / 0 (0.000%) |
| java-frontend/src/main/java/org/sonar/plugins/java/api/tree/ParameterizedTypeTree.java | 13 / 9 / 4 (30.769%) | 13 / 13 / 0 (0.000%) | 13 / 13 / 0 (0.000%) | 13 / 13 / 0 (0.000%) |
| java-frontend/src/main/java/org/sonar/plugins/java/api/tree/ParenthesizedTree.java | 15 / 10 / 5 (33.333%) | 15 / 15 / 0 (0.000%) | 15 / 15 / 0 (0.000%) | 15 / 15 / 0 (0.000%) |
| java-frontend/src/main/java/org/sonar/plugins/java/api/tree/PatternInstanceOfTree.java | 21 / 15 / 6 (28.571%) | 21 / 21 / 0 (0.000%) | 21 / 21 / 0 (0.000%) | 21 / 21 / 0 (0.000%) |
| java-frontend/src/main/java/org/sonar/plugins/java/api/tree/PatternTree.java | 9 / 7 / 2 (22.222%) | 9 / 9 / 0 (0.000%) | 9 / 9 / 0 (0.000%) | 9 / 9 / 0 (0.000%) |
| java-frontend/src/main/java/org/sonar/plugins/java/api/tree/PrimitiveTypeTree.java | 12 / 8 / 4 (33.333%) | 12 / 12 / 0 (0.000%) | 12 / 12 / 0 (0.000%) | 12 / 12 / 0 (0.000%) |
| java-frontend/src/main/java/org/sonar/plugins/java/api/tree/ProvidesDirectiveTree.java | 16 / 10 / 6 (37.500%) | 16 / 16 / 0 (0.000%) | 16 / 16 / 0 (0.000%) | 16 / 16 / 0 (0.000%) |
| java-frontend/src/main/java/org/sonar/plugins/java/api/tree/RecordPatternTree.java | 18 / 11 / 7 (38.889%) | 18 / 18 / 0 (0.000%) | 18 / 18 / 0 (0.000%) | 18 / 18 / 0 (0.000%) |
| java-frontend/src/main/java/org/sonar/plugins/java/api/tree/RequiresDirectiveTree.java | 13 / 9 / 4 (30.769%) | 13 / 13 / 0 (0.000%) | 13 / 13 / 0 (0.000%) | 13 / 13 / 0 (0.000%) |
| java-frontend/src/main/java/org/sonar/plugins/java/api/tree/ReturnStatementTree.java | 16 / 11 / 5 (31.250%) | 16 / 16 / 0 (0.000%) | 16 / 16 / 0 (0.000%) | 16 / 16 / 0 (0.000%) |
| java-frontend/src/main/java/org/sonar/plugins/java/api/tree/StatementTree.java | 9 / 7 / 2 (22.222%) | 9 / 9 / 0 (0.000%) | 9 / 9 / 0 (0.000%) | 9 / 9 / 0 (0.000%) |
| java-frontend/src/main/java/org/sonar/plugins/java/api/tree/StaticInitializerTree.java | 11 / 8 / 3 (27.273%) | 11 / 11 / 0 (0.000%) | 11 / 11 / 0 (0.000%) | 11 / 11 / 0 (0.000%) |
| java-frontend/src/main/java/org/sonar/plugins/java/api/tree/SwitchExpressionTree.java | 10 / 7 / 3 (30.000%) | 10 / 10 / 0 (0.000%) | 10 / 10 / 0 (0.000%) | 10 / 10 / 0 (0.000%) |
| java-frontend/src/main/java/org/sonar/plugins/java/api/tree/SwitchStatementTree.java | 10 / 7 / 3 (30.000%) | 10 / 10 / 0 (0.000%) | 10 / 10 / 0 (0.000%) | 10 / 10 / 0 (0.000%) |
| java-frontend/src/main/java/org/sonar/plugins/java/api/tree/SwitchTree.java | 23 / 14 / 9 (39.130%) | 23 / 23 / 0 (0.000%) | 23 / 23 / 0 (0.000%) | 23 / 23 / 0 (0.000%) |
| java-frontend/src/main/java/org/sonar/plugins/java/api/tree/SynchronizedStatementTree.java | 19 / 12 / 7 (36.842%) | 19 / 19 / 0 (0.000%) | 19 / 19 / 0 (0.000%) | 19 / 19 / 0 (0.000%) |
| java-frontend/src/main/java/org/sonar/plugins/java/api/tree/SyntaxToken.java | 24 / 19 / 5 (20.833%) | 24 / 24 / 0 (0.000%) | 24 / 24 / 0 (0.000%) | 24 / 24 / 0 (0.000%) |
| java-frontend/src/main/java/org/sonar/plugins/java/api/tree/SyntaxTrivia.java | 40 / 37 / 3 (7.500%) | 40 / 40 / 0 (0.000%) | 40 / 40 / 0 (0.000%) | 40 / 40 / 0 (0.000%) |
| java-frontend/src/main/java/org/sonar/plugins/java/api/tree/ThrowStatementTree.java | 15 / 10 / 5 (33.333%) | 15 / 15 / 0 (0.000%) | 15 / 15 / 0 (0.000%) | 15 / 15 / 0 (0.000%) |
| java-frontend/src/main/java/org/sonar/plugins/java/api/tree/Tree.java | 456 / 324 / 132 (28.947%) | 456 / 456 / 0 (0.000%) | 456 / 456 / 0 (0.000%) | 456 / 456 / 0 (0.000%) |
| java-frontend/src/main/java/org/sonar/plugins/java/api/tree/TreeVisitor.java | 218 / 147 / 71 (32.569%) | 218 / 218 / 0 (0.000%) | 218 / 218 / 0 (0.000%) | 218 / 218 / 0 (0.000%) |
| java-frontend/src/main/java/org/sonar/plugins/java/api/tree/TryStatementTree.java | 31 / 19 / 12 (38.710%) | 31 / 31 / 0 (0.000%) | 31 / 31 / 0 (0.000%) | 31 / 31 / 0 (0.000%) |
| java-frontend/src/main/java/org/sonar/plugins/java/api/tree/TypeArguments.java | 13 / 9 / 4 (30.769%) | 13 / 13 / 0 (0.000%) | 13 / 13 / 0 (0.000%) | 13 / 13 / 0 (0.000%) |
| java-frontend/src/main/java/org/sonar/plugins/java/api/tree/TypeCastTree.java | 23 / 14 / 9 (39.130%) | 23 / 23 / 0 (0.000%) | 23 / 23 / 0 (0.000%) | 23 / 23 / 0 (0.000%) |
| java-frontend/src/main/java/org/sonar/plugins/java/api/tree/TypeParameterTree.java | 19 / 12 / 7 (36.842%) | 19 / 19 / 0 (0.000%) | 19 / 19 / 0 (0.000%) | 19 / 19 / 0 (0.000%) |
| java-frontend/src/main/java/org/sonar/plugins/java/api/tree/TypeParameters.java | 15 / 11 / 4 (26.667%) | 15 / 15 / 0 (0.000%) | 15 / 15 / 0 (0.000%) | 15 / 15 / 0 (0.000%) |
| java-frontend/src/main/java/org/sonar/plugins/java/api/tree/TypePatternTree.java | 11 / 8 / 3 (27.273%) | 11 / 11 / 0 (0.000%) | 11 / 11 / 0 (0.000%) | 11 / 11 / 0 (0.000%) |
| java-frontend/src/main/java/org/sonar/plugins/java/api/tree/TypeTree.java | 13 / 9 / 4 (30.769%) | 13 / 13 / 0 (0.000%) | 13 / 13 / 0 (0.000%) | 13 / 13 / 0 (0.000%) |
| java-frontend/src/main/java/org/sonar/plugins/java/api/tree/UnaryExpressionTree.java | 13 / 9 / 4 (30.769%) | 13 / 13 / 0 (0.000%) | 13 / 13 / 0 (0.000%) | 13 / 13 / 0 (0.000%) |
| java-frontend/src/main/java/org/sonar/plugins/java/api/tree/UnionTypeTree.java | 12 / 8 / 4 (33.333%) | 12 / 12 / 0 (0.000%) | 12 / 12 / 0 (0.000%) | 12 / 12 / 0 (0.000%) |
| java-frontend/src/main/java/org/sonar/plugins/java/api/tree/UsesDirectiveTree.java | 11 / 8 / 3 (27.273%) | 11 / 11 / 0 (0.000%) | 11 / 11 / 0 (0.000%) | 11 / 11 / 0 (0.000%) |
| java-frontend/src/main/java/org/sonar/plugins/java/api/tree/VarTypeTree.java | 11 / 8 / 3 (27.273%) | 11 / 11 / 0 (0.000%) | 11 / 11 / 0 (0.000%) | 11 / 11 / 0 (0.000%) |
| java-frontend/src/main/java/org/sonar/plugins/java/api/tree/VariableTree.java | 26 / 17 / 9 (34.615%) | 26 / 26 / 0 (0.000%) | 26 / 26 / 0 (0.000%) | 26 / 26 / 0 (0.000%) |
| java-frontend/src/main/java/org/sonar/plugins/java/api/tree/WhileStatementTree.java | 19 / 12 / 7 (36.842%) | 19 / 19 / 0 (0.000%) | 19 / 19 / 0 (0.000%) | 19 / 19 / 0 (0.000%) |
| java-frontend/src/main/java/org/sonar/plugins/java/api/tree/WildcardTree.java | 21 / 14 / 7 (33.333%) | 21 / 21 / 0 (0.000%) | 21 / 21 / 0 (0.000%) | 21 / 21 / 0 (0.000%) |
| java-frontend/src/main/java/org/sonar/plugins/java/api/tree/YieldStatementTree.java | 16 / 11 / 5 (31.250%) | 16 / 16 / 0 (0.000%) | 16 / 16 / 0 (0.000%) | 16 / 16 / 0 (0.000%) |
| java-frontend/src/main/java/org/sonar/plugins/java/api/tree/package-info.java | 10 / 9 / 1 (10.000%) | 10 / 10 / 0 (0.000%) | 10 / 10 / 0 (0.000%) | 10 / 10 / 0 (0.000%) |
| java-jsp/src/main/java/org/sonar/java/jsp/Jasper.java | 492 / 467 / 25 (5.081%) | 492 / 492 / 0 (0.000%) | 492 / 467 / 25 (5.081%) | 492 / 492 / 0 (0.000%) |
| java-jsp/src/main/java/org/sonar/java/jsp/JasperOptions.java | 162 / 161 / 1 (0.617%) | 162 / 162 / 0 (0.000%) | 162 / 161 / 1 (0.617%) | 162 / 162 / 0 (0.000%) |
| java-jsp/src/main/java/org/sonar/java/jsp/package-info.java | 5 / 5 / 0 (0.000%) | 5 / 5 / 0 (0.000%) | 5 / 5 / 0 (0.000%) | 5 / 5 / 0 (0.000%) |
| java-surefire/src/main/java/org/sonar/plugins/surefire/StaxParser.java | 73 / 69 / 4 (5.479%) | 73 / 73 / 0 (0.000%) | 73 / 73 / 0 (0.000%) | 73 / 73 / 0 (0.000%) |
| java-surefire/src/main/java/org/sonar/plugins/surefire/SurefireExtensions.java | 31 / 18 / 13 (41.935%) | 31 / 31 / 0 (0.000%) | 31 / 26 / 5 (16.129%) | 31 / 31 / 0 (0.000%) |
| java-surefire/src/main/java/org/sonar/plugins/surefire/SurefireJavaParser.java | 330 / 256 / 74 (22.424%) | 330 / 330 / 0 (0.000%) | 330 / 322 / 8 (2.424%) | 330 / 330 / 0 (0.000%) |
| java-surefire/src/main/java/org/sonar/plugins/surefire/SurefireSensor.java | 84 / 77 / 7 (8.333%) | 84 / 84 / 0 (0.000%) | 84 / 81 / 3 (3.571%) | 84 / 84 / 0 (0.000%) |
| java-surefire/src/main/java/org/sonar/plugins/surefire/api/SurefireUtils.java | 89 / 89 / 0 (0.000%) | 89 / 89 / 0 (0.000%) | 89 / 89 / 0 (0.000%) | 89 / 89 / 0 (0.000%) |
| java-surefire/src/main/java/org/sonar/plugins/surefire/api/package-info.java | 6 / 6 / 0 (0.000%) | 6 / 6 / 0 (0.000%) | 6 / 6 / 0 (0.000%) | 6 / 6 / 0 (0.000%) |
| java-surefire/src/main/java/org/sonar/plugins/surefire/data/SurefireStaxHandler.java | 253 / 229 / 24 (9.486%) | 253 / 253 / 0 (0.000%) | 253 / 253 / 0 (0.000%) | 253 / 253 / 0 (0.000%) |
| java-surefire/src/main/java/org/sonar/plugins/surefire/data/UnitTestClassReport.java | 100 / 71 / 29 (29.000%) | 100 / 100 / 0 (0.000%) | 100 / 100 / 0 (0.000%) | 100 / 100 / 0 (0.000%) |
| java-surefire/src/main/java/org/sonar/plugins/surefire/data/UnitTestIndex.java | 73 / 53 / 20 (27.397%) | 73 / 73 / 0 (0.000%) | 73 / 73 / 0 (0.000%) | 73 / 73 / 0 (0.000%) |
| java-surefire/src/main/java/org/sonar/plugins/surefire/data/UnitTestResult.java | 105 / 105 / 0 (0.000%) | 105 / 105 / 0 (0.000%) | 105 / 105 / 0 (0.000%) | 105 / 105 / 0 (0.000%) |
| java-surefire/src/main/java/org/sonar/plugins/surefire/data/package-info.java | 6 / 6 / 0 (0.000%) | 6 / 6 / 0 (0.000%) | 6 / 6 / 0 (0.000%) | 6 / 6 / 0 (0.000%) |
| java-surefire/src/main/java/org/sonar/plugins/surefire/package-info.java | 5 / 5 / 0 (0.000%) | 5 / 5 / 0 (0.000%) | 5 / 5 / 0 (0.000%) | 5 / 5 / 0 (0.000%) |
| sonar-java-plugin/src/main/java/org/sonar/plugins/java/BuiltInJavaQualityProfile.java | 235 / 208 / 27 (11.489%) | 235 / 232 / 3 (1.277%) | 235 / 210 / 25 (10.638%) | 235 / 232 / 3 (1.277%) |
| sonar-java-plugin/src/main/java/org/sonar/plugins/java/DroppedPropertiesSensor.java | 105 / 105 / 0 (0.000%) | 105 / 105 / 0 (0.000%) | 105 / 105 / 0 (0.000%) | 105 / 105 / 0 (0.000%) |
| sonar-java-plugin/src/main/java/org/sonar/plugins/java/ExternalReportExtensions.java | 129 / 79 / 50 (38.760%) | 129 / 129 / 0 (0.000%) | 129 / 79 / 50 (38.760%) | 129 / 129 / 0 (0.000%) |
| sonar-java-plugin/src/main/java/org/sonar/plugins/java/Java.java | 49 / 49 / 0 (0.000%) | 49 / 49 / 0 (0.000%) | 49 / 49 / 0 (0.000%) | 49 / 49 / 0 (0.000%) |
| sonar-java-plugin/src/main/java/org/sonar/plugins/java/JavaAgenticAIProfile.java | 30 / 26 / 4 (13.333%) | 30 / 30 / 0 (0.000%) | 30 / 26 / 4 (13.333%) | 30 / 30 / 0 (0.000%) |
| sonar-java-plugin/src/main/java/org/sonar/plugins/java/JavaPlugin.java | 159 / 94 / 65 (40.881%) | 159 / 159 / 0 (0.000%) | 159 / 102 / 57 (35.849%) | 159 / 159 / 0 (0.000%) |
| sonar-java-plugin/src/main/java/org/sonar/plugins/java/JavaRulesDefinition.java | 135 / 105 / 30 (22.222%) | 135 / 115 / 20 (14.815%) | 135 / 107 / 28 (20.741%) | 135 / 115 / 20 (14.815%) |
| sonar-java-plugin/src/main/java/org/sonar/plugins/java/JavaSensor.java | 367 / 266 / 101 (27.520%) | 367 / 347 / 20 (5.450%) | 367 / 270 / 97 (26.431%) | 367 / 347 / 20 (5.450%) |
| sonar-java-plugin/src/main/java/org/sonar/plugins/java/JavaSonarWayProfile.java | 35 / 30 / 5 (14.286%) | 35 / 35 / 0 (0.000%) | 35 / 30 / 5 (14.286%) | 35 / 35 / 0 (0.000%) |
| sonar-java-plugin/src/main/java/org/sonar/plugins/java/ProjectEndOfAnalysisSensor.java | 128 / 64 / 64 (50.000%) | 128 / 128 / 0 (0.000%) | 128 / 66 / 62 (48.438%) | 128 / 128 / 0 (0.000%) |
| sonar-java-plugin/src/main/java/org/sonar/plugins/java/SpringContextModelSensor.java | 127 / 89 / 38 (29.921%) | 127 / 125 / 2 (1.575%) | 127 / 91 / 36 (28.346%) | 127 / 125 / 2 (1.575%) |
| sonar-java-plugin/src/main/java/org/sonar/plugins/java/package-info.java | 5 / 5 / 0 (0.000%) | 5 / 5 / 0 (0.000%) | 5 / 5 / 0 (0.000%) | 5 / 5 / 0 (0.000%) |

## Rules with findings

| Rule | Baseline | Source paths | Bytecode | Combined |
|---|---:|---:|---:|---:|
| java:S1066 | 1 | 1 | 1 | 1 |
| java:S107 | 2 | 8 | 8 | 8 |
| java:S1075 | 3 | 3 | 3 | 3 |
| java:S110 | 0 | 5 | 0 | 5 |
| java:S112 | 1 | 1 | 1 | 1 |
| java:S1121 | 2 | 2 | 2 | 2 |
| java:S1133 | 10 | 10 | 10 | 10 |
| java:S1134 | 5 | 5 | 5 | 5 |
| java:S1135 | 12 | 12 | 12 | 12 |
| java:S1141 | 1 | 1 | 1 | 1 |
| java:S1144 | 1 | 0 | 0 | 0 |
| java:S1185 | 0 | 1 | 1 | 1 |
| java:S1192 | 6 | 6 | 6 | 6 |
| java:S125 | 10 | 10 | 10 | 10 |
| java:S131 | 2 | 5 | 2 | 5 |
| java:S135 | 2 | 2 | 2 | 2 |
| java:S1450 | 1 | 0 | 1 | 0 |
| java:S1452 | 6 | 6 | 6 | 6 |
| java:S1479 | 1 | 1 | 1 | 1 |
| java:S1845 | 0 | 1 | 0 | 1 |
| java:S2097 | 1 | 1 | 1 | 1 |
| java:S2160 | 3 | 3 | 3 | 3 |
| java:S2234 | 0 | 1 | 0 | 1 |
| java:S2629 | 5 | 5 | 5 | 5 |
| java:S3398 | 1 | 0 | 0 | 0 |
| java:S3457 | 1 | 2 | 1 | 2 |
| java:S3776 | 2 | 2 | 2 | 2 |
| java:S4790 | 2 | 2 | 2 | 2 |
| java:S5411 | 2 | 2 | 2 | 2 |
| java:S5803 | 0 | 1 | 0 | 1 |
| java:S6204 | 0 | 1 | 0 | 1 |
| java:S6485 | 0 | 1 | 0 | 1 |
| java:S6539 | 0 | 2 | 2 | 2 |
| java:S6878 | 1 | 1 | 1 | 1 |
| java:S6880 | 5 | 5 | 5 | 5 |
| java:S7158 | 1 | 1 | 1 | 1 |
| java:S8491 | 2 | 2 | 2 | 2 |
| java:S9358 | 2 | 0 | 0 | 0 |
| java:S9395 | 2 | 2 | 2 | 2 |
| java:S9398 | 3 | 3 | 3 | 3 |

559 configured rules had no findings in any mode and are omitted.

## Detailed differences

<details>
<summary>Source paths versus Baseline</summary>

### Largest semantic improvements

Top five by absolute change in unknown percentage.

| File | Second mode unknown % | First mode unknown % | Change (pp) |
|---|---:|---:|---:|
| java-checks/src/main/java/org/sonar/java/checks/IgnoredOperationStatusCheck.java | 76.821% | 0.000% | -76.821 |
| java-checks/src/main/java/org/sonar/java/checks/spring/AsyncMethodsOnConfigurationClassCheck.java | 73.864% | 0.000% | -73.864 |
| java-checks/src/main/java/org/sonar/java/checks/PublicConstructorInAbstractClassCheck.java | 73.737% | 0.000% | -73.737 |
| java-checks/src/main/java/org/sonar/java/checks/AvoidHighFrameratesOnMobileCheck.java | 70.667% | 0.000% | -70.667 |
| java-checks/src/main/java/org/sonar/java/checks/security/IntegerToHexStringCheck.java | 70.476% | 0.000% | -70.476 |

### Largest semantic regressions

Top five by absolute change in unknown percentage.

None.

| Unknown occurrences | Count |
|---|---:|
| Still unknown in both modes | 48 |
| No longer reported unknown | 83254 |
| Newly reported unknown | 0 |

### No longer reported unknown

| File | Range | Identifier | AST context |
|---|---|---|---|
| external-reports/src/main/java/org/sonar/java/externalreport/CheckstyleSensor.java | (70:35)-(70:48) | importIfExist | METHOD_INVOCATION |
| external-reports/src/main/java/org/sonar/java/externalreport/CheckstyleSensor.java | (76:33)-(76:37) | read | MEMBER_SELECT |
| external-reports/src/main/java/org/sonar/java/externalreport/CheckstyleSensor.java | (76:7)-(76:32) | CheckstyleXmlReportReader | MEMBER_SELECT |
| external-reports/src/main/java/org/sonar/java/externalreport/CheckstyleSensor.java | (83:24)-(83:33) | saveIssue | MEMBER_SELECT |
| external-reports/src/main/java/org/sonar/java/externalreport/CheckstyleSensor.java | (83:5)-(83:23) | ExternalIssueUtils | MEMBER_SELECT |
| external-reports/src/main/java/org/sonar/java/externalreport/PmdSensor.java | (68:35)-(68:48) | importIfExist | METHOD_INVOCATION |
| external-reports/src/main/java/org/sonar/java/externalreport/PmdSensor.java | (74:26)-(74:30) | read | MEMBER_SELECT |
| external-reports/src/main/java/org/sonar/java/externalreport/PmdSensor.java | (74:7)-(74:25) | PmdXmlReportReader | MEMBER_SELECT |
| external-reports/src/main/java/org/sonar/java/externalreport/PmdXmlReportReader.java | (110:19)-(110:28) | PmdSensor | MEMBER_SELECT |
| external-reports/src/main/java/org/sonar/java/externalreport/PmdXmlReportReader.java | (110:29)-(110:39) | LINTER_KEY | MEMBER_SELECT |
| external-reports/src/main/java/org/sonar/java/externalreport/PmdXmlReportReader.java | (111:10)-(111:16) | ruleId | MEMBER_SELECT |
| external-reports/src/main/java/org/sonar/java/externalreport/PmdXmlReportReader.java | (112:10)-(112:14) | type | MEMBER_SELECT |
| external-reports/src/main/java/org/sonar/java/externalreport/PmdXmlReportReader.java | (113:10)-(113:18) | severity | MEMBER_SELECT |
| external-reports/src/main/java/org/sonar/java/externalreport/PmdXmlReportReader.java | (114:10)-(114:34) | remediationEffortMinutes | MEMBER_SELECT |
| external-reports/src/main/java/org/sonar/java/externalreport/SpotBugsSensor.java | (102:35)-(102:48) | importIfExist | METHOD_INVOCATION |
| external-reports/src/main/java/org/sonar/java/externalreport/SpotBugsSensor.java | (112:31)-(112:35) | read | MEMBER_SELECT |
| external-reports/src/main/java/org/sonar/java/externalreport/SpotBugsSensor.java | (112:7)-(112:30) | SpotBugsXmlReportReader | MEMBER_SELECT |
| external-reports/src/main/java/org/sonar/java/externalreport/SpotBugsXmlReportReader.java | (164:23)-(164:37) | SpotBugsSensor | MEMBER_SELECT |
| external-reports/src/main/java/org/sonar/java/externalreport/SpotBugsXmlReportReader.java | (164:38)-(164:50) | SPOTBUGS_KEY | MEMBER_SELECT |
| external-reports/src/main/java/org/sonar/java/externalreport/SpotBugsXmlReportReader.java | (172:24)-(172:33) | saveIssue | MEMBER_SELECT |

83234 additional occurrences omitted.


### Newly reported unknown

None.


Semantic graph location differences unavailable: traversal is DISABLED, PARTIAL, or UNKNOWN in at least one compared mode. Primary identifier and finding comparisons remain valid.

### Findings only in Baseline

| Rule | File | Line | Message |
|---|---|---:|---|
| java:S1144 | java-frontend/src/main/java/org/sonar/java/model/JParserConfig.java | 227 | Remove this unused private "FileByFile" constructor. |
| java:S1450 | java-checks/src/main/java/org/sonar/java/checks/TypeParametersShadowingCheck.java | 37 | Remove the "context" field and declare it as a local variable in the relevant methods. |
| java:S3398 | java-frontend/src/main/java/org/sonar/java/SemanticReportScanner.java | 338 | Move this method into "ModuleReferences". |
| java:S9358 | java-frontend/src/main/java/org/sonar/java/model/InternalSyntaxToken.java | 45 | Move the conditional expression inside this operation. |
| java:S9358 | java-frontend/src/main/java/org/sonar/java/model/InternalSyntaxTrivia.java | 41 | Move the conditional expression inside this operation. |

### Findings only in Source paths

| Rule | File | Line | Message |
|---|---|---:|---|
| java:S107 | java-frontend/src/main/java/org/sonar/java/SonarComponents.java | 169 | Constructor has 8 parameters, which is greater than 7 authorized. |
| java:S107 | java-frontend/src/main/java/org/sonar/java/SonarComponents.java | 188 | Constructor has 8 parameters, which is greater than 7 authorized. |
| java:S107 | java-frontend/src/main/java/org/sonar/java/SonarComponents.java | 213 | Constructor has 9 parameters, which is greater than 7 authorized. |
| java:S107 | java-frontend/src/main/java/org/sonar/java/model/DefaultJavaFileScannerContext.java | 62 | Constructor has 8 parameters, which is greater than 7 authorized. |
| java:S107 | java-frontend/src/main/java/org/sonar/java/model/statement/ForStatementTreeImpl.java | 47 | Constructor has 9 parameters, which is greater than 7 authorized. |
| java:S107 | java-frontend/src/main/java/org/sonar/java/model/statement/TryStatementTreeImpl.java | 51 | Constructor has 8 parameters, which is greater than 7 authorized. |
| java:S110 | java-checks/src/main/java/org/sonar/java/checks/regex/AbstractRedosCheck.java | 51 | This class has 6 parents which is greater than 5 authorized. |
| java:S110 | java-checks/src/main/java/org/sonar/java/checks/regex/RedosCheck.java | 23 | This class has 7 parents which is greater than 5 authorized. |
| java:S110 | java-checks/src/main/java/org/sonar/java/checks/regex/RegexLookaheadCheck.java | 26 | This class has 6 parents which is greater than 5 authorized. |
| java:S110 | java-checks/src/main/java/org/sonar/java/checks/regex/ReluctantQuantifierWithEmptyContinuationCheck.java | 26 | This class has 6 parents which is greater than 5 authorized. |
| java:S110 | java-checks/src/main/java/org/sonar/java/checks/regex/SuperLinearRegexCheck.java | 23 | This class has 7 parents which is greater than 5 authorized. |
| java:S1185 | java-frontend/src/main/java/org/sonar/plugins/java/api/IssuableSubscriptionVisitor.java | 36 | Remove this method to simply inherit it. |
| java:S131 | java-checks/src/main/java/org/sonar/java/checks/BlockingOperationsInVirtualThreadsCheck.java | 96 | Complete cases by adding the missing enum constants or add a default case to this switch. |
| java:S131 | java-checks/src/main/java/org/sonar/java/checks/ThreadAsRunnableArgumentCheck.java | 55 | Complete cases by adding the missing enum constants or add a default case to this switch. |
| java:S131 | java-checks/src/main/java/org/sonar/java/checks/VolatileVariablesOperationsCheck.java | 125 | Complete cases by adding the missing enum constants or add a default case to this switch. |
| java:S1845 | java-checks-testkit/src/main/java/org/sonar/java/checks/verifier/TestCheckRegistrarContext.java | 39 | Rename field "testCheckClasses" to prevent any misunderstanding/clash with method "testCheckClasses" defined in superclass "org.sonar.plugins.java.api.CheckRegistrar$RegistrarContext". |
| java:S2234 | java-checks/src/main/java/org/sonar/java/checks/MembersDifferOnlyByCapitalizationCheck.java | 184 | Parameters to variableAndMethod have the same names but not the same order as the method arguments. |
| java:S3457 | java-checks/src/main/java/org/sonar/java/checks/OneDeclarationPerLineCheck.java | 123 | %n should be used in place of \n to produce the platform-specific line separator. |
| java:S5803 | java-checks-testkit/src/main/java/org/sonar/java/checks/verifier/internal/JavaCheckVerifier.java | 121 | Remove this usage of "scanForTesting", it is annotated with @VisibleForTesting and should not be accessed from production code. |
| java:S6204 | java-checks/src/main/java/org/sonar/java/checks/spring/SpringCacheableWithCachePutCheck.java | 89 | Replace this usage of 'Stream.collect(Collectors.toList())' with 'Stream.toList()' and ensure that the list is unmodified. |
| java:S6485 | java-checks/src/main/java/org/sonar/java/checks/unused/utils/AnnotationFieldReferenceFinder.java | 75 | Replace this call to the constructor with the better suited static method HashMap.newHashMap(int numMappings) |
| java:S6539 | java-frontend/src/main/java/org/sonar/java/model/JParser.java | 256 | Split this “Monster Class” into smaller and more specialized ones to reduce its dependencies on other classes from 59 to the maximum authorized 20 or less. |
| java:S6539 | java-frontend/src/main/java/org/sonar/plugins/java/api/tree/BaseTreeVisitor.java | 27 | Split this “Monster Class” into smaller and more specialized ones to reduce its dependencies on other classes from 73 to the maximum authorized 20 or less. |

</details>

<details>
<summary>Bytecode versus Baseline</summary>

### Largest semantic improvements

Top five by absolute change in unknown percentage.

| File | Second mode unknown % | First mode unknown % | Change (pp) |
|---|---:|---:|---:|
| java-frontend/src/main/java/org/sonar/java/testing/JavaIssueBuilderForTests.java | 61.236% | 0.000% | -61.236 |
| java-frontend/src/main/java/org/sonar/java/matcher/MethodMatchersList.java | 56.944% | 0.000% | -56.944 |
| java-frontend/src/main/java/org/sonar/java/model/springcontext/BeanDefinitionGatherer.java | 55.288% | 0.000% | -55.288 |
| java-frontend/src/main/java/org/sonar/java/regex/RegexCache.java | 55.263% | 0.000% | -55.263 |
| java-frontend/src/main/java/org/sonar/java/utils/UnitTestUtils.java | 53.565% | 0.000% | -53.565 |

### Largest semantic regressions

Top five by absolute change in unknown percentage.

None.

| Unknown occurrences | Count |
|---|---:|
| Still unknown in both modes | 69069 |
| No longer reported unknown | 14233 |
| Newly reported unknown | 0 |

### No longer reported unknown

| File | Range | Identifier | AST context |
|---|---|---|---|
| external-reports/src/main/java/org/sonar/java/externalreport/CheckstyleSensor.java | (70:35)-(70:48) | importIfExist | METHOD_INVOCATION |
| external-reports/src/main/java/org/sonar/java/externalreport/CheckstyleSensor.java | (76:33)-(76:37) | read | MEMBER_SELECT |
| external-reports/src/main/java/org/sonar/java/externalreport/CheckstyleSensor.java | (76:7)-(76:32) | CheckstyleXmlReportReader | MEMBER_SELECT |
| external-reports/src/main/java/org/sonar/java/externalreport/CheckstyleSensor.java | (83:24)-(83:33) | saveIssue | MEMBER_SELECT |
| external-reports/src/main/java/org/sonar/java/externalreport/CheckstyleSensor.java | (83:5)-(83:23) | ExternalIssueUtils | MEMBER_SELECT |
| external-reports/src/main/java/org/sonar/java/externalreport/PmdSensor.java | (68:35)-(68:48) | importIfExist | METHOD_INVOCATION |
| external-reports/src/main/java/org/sonar/java/externalreport/PmdSensor.java | (74:26)-(74:30) | read | MEMBER_SELECT |
| external-reports/src/main/java/org/sonar/java/externalreport/PmdSensor.java | (74:7)-(74:25) | PmdXmlReportReader | MEMBER_SELECT |
| external-reports/src/main/java/org/sonar/java/externalreport/PmdXmlReportReader.java | (110:19)-(110:28) | PmdSensor | MEMBER_SELECT |
| external-reports/src/main/java/org/sonar/java/externalreport/PmdXmlReportReader.java | (110:29)-(110:39) | LINTER_KEY | MEMBER_SELECT |
| external-reports/src/main/java/org/sonar/java/externalreport/PmdXmlReportReader.java | (111:10)-(111:16) | ruleId | MEMBER_SELECT |
| external-reports/src/main/java/org/sonar/java/externalreport/PmdXmlReportReader.java | (112:10)-(112:14) | type | MEMBER_SELECT |
| external-reports/src/main/java/org/sonar/java/externalreport/PmdXmlReportReader.java | (113:10)-(113:18) | severity | MEMBER_SELECT |
| external-reports/src/main/java/org/sonar/java/externalreport/PmdXmlReportReader.java | (114:10)-(114:34) | remediationEffortMinutes | MEMBER_SELECT |
| external-reports/src/main/java/org/sonar/java/externalreport/SpotBugsSensor.java | (102:35)-(102:48) | importIfExist | METHOD_INVOCATION |
| external-reports/src/main/java/org/sonar/java/externalreport/SpotBugsSensor.java | (112:31)-(112:35) | read | MEMBER_SELECT |
| external-reports/src/main/java/org/sonar/java/externalreport/SpotBugsSensor.java | (112:7)-(112:30) | SpotBugsXmlReportReader | MEMBER_SELECT |
| external-reports/src/main/java/org/sonar/java/externalreport/SpotBugsXmlReportReader.java | (164:23)-(164:37) | SpotBugsSensor | MEMBER_SELECT |
| external-reports/src/main/java/org/sonar/java/externalreport/SpotBugsXmlReportReader.java | (164:38)-(164:50) | SPOTBUGS_KEY | MEMBER_SELECT |
| external-reports/src/main/java/org/sonar/java/externalreport/SpotBugsXmlReportReader.java | (172:24)-(172:33) | saveIssue | MEMBER_SELECT |

14213 additional occurrences omitted.


### Newly reported unknown

None.


Semantic graph location differences unavailable: traversal is DISABLED, PARTIAL, or UNKNOWN in at least one compared mode. Primary identifier and finding comparisons remain valid.

### Findings only in Baseline

| Rule | File | Line | Message |
|---|---|---:|---|
| java:S1144 | java-frontend/src/main/java/org/sonar/java/model/JParserConfig.java | 227 | Remove this unused private "FileByFile" constructor. |
| java:S3398 | java-frontend/src/main/java/org/sonar/java/SemanticReportScanner.java | 338 | Move this method into "ModuleReferences". |
| java:S9358 | java-frontend/src/main/java/org/sonar/java/model/InternalSyntaxToken.java | 45 | Move the conditional expression inside this operation. |
| java:S9358 | java-frontend/src/main/java/org/sonar/java/model/InternalSyntaxTrivia.java | 41 | Move the conditional expression inside this operation. |

### Findings only in Bytecode

| Rule | File | Line | Message |
|---|---|---:|---|
| java:S107 | java-frontend/src/main/java/org/sonar/java/SonarComponents.java | 169 | Constructor has 8 parameters, which is greater than 7 authorized. |
| java:S107 | java-frontend/src/main/java/org/sonar/java/SonarComponents.java | 188 | Constructor has 8 parameters, which is greater than 7 authorized. |
| java:S107 | java-frontend/src/main/java/org/sonar/java/SonarComponents.java | 213 | Constructor has 9 parameters, which is greater than 7 authorized. |
| java:S107 | java-frontend/src/main/java/org/sonar/java/model/DefaultJavaFileScannerContext.java | 62 | Constructor has 8 parameters, which is greater than 7 authorized. |
| java:S107 | java-frontend/src/main/java/org/sonar/java/model/statement/ForStatementTreeImpl.java | 47 | Constructor has 9 parameters, which is greater than 7 authorized. |
| java:S107 | java-frontend/src/main/java/org/sonar/java/model/statement/TryStatementTreeImpl.java | 51 | Constructor has 8 parameters, which is greater than 7 authorized. |
| java:S1185 | java-frontend/src/main/java/org/sonar/plugins/java/api/IssuableSubscriptionVisitor.java | 36 | Remove this method to simply inherit it. |
| java:S6539 | java-frontend/src/main/java/org/sonar/java/model/JParser.java | 256 | Split this “Monster Class” into smaller and more specialized ones to reduce its dependencies on other classes from 59 to the maximum authorized 20 or less. |
| java:S6539 | java-frontend/src/main/java/org/sonar/plugins/java/api/tree/BaseTreeVisitor.java | 27 | Split this “Monster Class” into smaller and more specialized ones to reduce its dependencies on other classes from 73 to the maximum authorized 20 or less. |

</details>

<details>
<summary>Combined versus Baseline</summary>

### Largest semantic improvements

Top five by absolute change in unknown percentage.

| File | Second mode unknown % | First mode unknown % | Change (pp) |
|---|---:|---:|---:|
| java-checks/src/main/java/org/sonar/java/checks/IgnoredOperationStatusCheck.java | 76.821% | 0.000% | -76.821 |
| java-checks/src/main/java/org/sonar/java/checks/spring/AsyncMethodsOnConfigurationClassCheck.java | 73.864% | 0.000% | -73.864 |
| java-checks/src/main/java/org/sonar/java/checks/PublicConstructorInAbstractClassCheck.java | 73.737% | 0.000% | -73.737 |
| java-checks/src/main/java/org/sonar/java/checks/AvoidHighFrameratesOnMobileCheck.java | 70.667% | 0.000% | -70.667 |
| java-checks/src/main/java/org/sonar/java/checks/security/IntegerToHexStringCheck.java | 70.476% | 0.000% | -70.476 |

### Largest semantic regressions

Top five by absolute change in unknown percentage.

None.

| Unknown occurrences | Count |
|---|---:|
| Still unknown in both modes | 48 |
| No longer reported unknown | 83254 |
| Newly reported unknown | 0 |

### No longer reported unknown

| File | Range | Identifier | AST context |
|---|---|---|---|
| external-reports/src/main/java/org/sonar/java/externalreport/CheckstyleSensor.java | (70:35)-(70:48) | importIfExist | METHOD_INVOCATION |
| external-reports/src/main/java/org/sonar/java/externalreport/CheckstyleSensor.java | (76:33)-(76:37) | read | MEMBER_SELECT |
| external-reports/src/main/java/org/sonar/java/externalreport/CheckstyleSensor.java | (76:7)-(76:32) | CheckstyleXmlReportReader | MEMBER_SELECT |
| external-reports/src/main/java/org/sonar/java/externalreport/CheckstyleSensor.java | (83:24)-(83:33) | saveIssue | MEMBER_SELECT |
| external-reports/src/main/java/org/sonar/java/externalreport/CheckstyleSensor.java | (83:5)-(83:23) | ExternalIssueUtils | MEMBER_SELECT |
| external-reports/src/main/java/org/sonar/java/externalreport/PmdSensor.java | (68:35)-(68:48) | importIfExist | METHOD_INVOCATION |
| external-reports/src/main/java/org/sonar/java/externalreport/PmdSensor.java | (74:26)-(74:30) | read | MEMBER_SELECT |
| external-reports/src/main/java/org/sonar/java/externalreport/PmdSensor.java | (74:7)-(74:25) | PmdXmlReportReader | MEMBER_SELECT |
| external-reports/src/main/java/org/sonar/java/externalreport/PmdXmlReportReader.java | (110:19)-(110:28) | PmdSensor | MEMBER_SELECT |
| external-reports/src/main/java/org/sonar/java/externalreport/PmdXmlReportReader.java | (110:29)-(110:39) | LINTER_KEY | MEMBER_SELECT |
| external-reports/src/main/java/org/sonar/java/externalreport/PmdXmlReportReader.java | (111:10)-(111:16) | ruleId | MEMBER_SELECT |
| external-reports/src/main/java/org/sonar/java/externalreport/PmdXmlReportReader.java | (112:10)-(112:14) | type | MEMBER_SELECT |
| external-reports/src/main/java/org/sonar/java/externalreport/PmdXmlReportReader.java | (113:10)-(113:18) | severity | MEMBER_SELECT |
| external-reports/src/main/java/org/sonar/java/externalreport/PmdXmlReportReader.java | (114:10)-(114:34) | remediationEffortMinutes | MEMBER_SELECT |
| external-reports/src/main/java/org/sonar/java/externalreport/SpotBugsSensor.java | (102:35)-(102:48) | importIfExist | METHOD_INVOCATION |
| external-reports/src/main/java/org/sonar/java/externalreport/SpotBugsSensor.java | (112:31)-(112:35) | read | MEMBER_SELECT |
| external-reports/src/main/java/org/sonar/java/externalreport/SpotBugsSensor.java | (112:7)-(112:30) | SpotBugsXmlReportReader | MEMBER_SELECT |
| external-reports/src/main/java/org/sonar/java/externalreport/SpotBugsXmlReportReader.java | (164:23)-(164:37) | SpotBugsSensor | MEMBER_SELECT |
| external-reports/src/main/java/org/sonar/java/externalreport/SpotBugsXmlReportReader.java | (164:38)-(164:50) | SPOTBUGS_KEY | MEMBER_SELECT |
| external-reports/src/main/java/org/sonar/java/externalreport/SpotBugsXmlReportReader.java | (172:24)-(172:33) | saveIssue | MEMBER_SELECT |

83234 additional occurrences omitted.


### Newly reported unknown

None.


Semantic graph location differences unavailable: traversal is DISABLED, PARTIAL, or UNKNOWN in at least one compared mode. Primary identifier and finding comparisons remain valid.

### Findings only in Baseline

| Rule | File | Line | Message |
|---|---|---:|---|
| java:S1144 | java-frontend/src/main/java/org/sonar/java/model/JParserConfig.java | 227 | Remove this unused private "FileByFile" constructor. |
| java:S1450 | java-checks/src/main/java/org/sonar/java/checks/TypeParametersShadowingCheck.java | 37 | Remove the "context" field and declare it as a local variable in the relevant methods. |
| java:S3398 | java-frontend/src/main/java/org/sonar/java/SemanticReportScanner.java | 338 | Move this method into "ModuleReferences". |
| java:S9358 | java-frontend/src/main/java/org/sonar/java/model/InternalSyntaxToken.java | 45 | Move the conditional expression inside this operation. |
| java:S9358 | java-frontend/src/main/java/org/sonar/java/model/InternalSyntaxTrivia.java | 41 | Move the conditional expression inside this operation. |

### Findings only in Combined

| Rule | File | Line | Message |
|---|---|---:|---|
| java:S107 | java-frontend/src/main/java/org/sonar/java/SonarComponents.java | 169 | Constructor has 8 parameters, which is greater than 7 authorized. |
| java:S107 | java-frontend/src/main/java/org/sonar/java/SonarComponents.java | 188 | Constructor has 8 parameters, which is greater than 7 authorized. |
| java:S107 | java-frontend/src/main/java/org/sonar/java/SonarComponents.java | 213 | Constructor has 9 parameters, which is greater than 7 authorized. |
| java:S107 | java-frontend/src/main/java/org/sonar/java/model/DefaultJavaFileScannerContext.java | 62 | Constructor has 8 parameters, which is greater than 7 authorized. |
| java:S107 | java-frontend/src/main/java/org/sonar/java/model/statement/ForStatementTreeImpl.java | 47 | Constructor has 9 parameters, which is greater than 7 authorized. |
| java:S107 | java-frontend/src/main/java/org/sonar/java/model/statement/TryStatementTreeImpl.java | 51 | Constructor has 8 parameters, which is greater than 7 authorized. |
| java:S110 | java-checks/src/main/java/org/sonar/java/checks/regex/AbstractRedosCheck.java | 51 | This class has 6 parents which is greater than 5 authorized. |
| java:S110 | java-checks/src/main/java/org/sonar/java/checks/regex/RedosCheck.java | 23 | This class has 7 parents which is greater than 5 authorized. |
| java:S110 | java-checks/src/main/java/org/sonar/java/checks/regex/RegexLookaheadCheck.java | 26 | This class has 6 parents which is greater than 5 authorized. |
| java:S110 | java-checks/src/main/java/org/sonar/java/checks/regex/ReluctantQuantifierWithEmptyContinuationCheck.java | 26 | This class has 6 parents which is greater than 5 authorized. |
| java:S110 | java-checks/src/main/java/org/sonar/java/checks/regex/SuperLinearRegexCheck.java | 23 | This class has 7 parents which is greater than 5 authorized. |
| java:S1185 | java-frontend/src/main/java/org/sonar/plugins/java/api/IssuableSubscriptionVisitor.java | 36 | Remove this method to simply inherit it. |
| java:S131 | java-checks/src/main/java/org/sonar/java/checks/BlockingOperationsInVirtualThreadsCheck.java | 96 | Complete cases by adding the missing enum constants or add a default case to this switch. |
| java:S131 | java-checks/src/main/java/org/sonar/java/checks/ThreadAsRunnableArgumentCheck.java | 55 | Complete cases by adding the missing enum constants or add a default case to this switch. |
| java:S131 | java-checks/src/main/java/org/sonar/java/checks/VolatileVariablesOperationsCheck.java | 125 | Complete cases by adding the missing enum constants or add a default case to this switch. |
| java:S1845 | java-checks-testkit/src/main/java/org/sonar/java/checks/verifier/TestCheckRegistrarContext.java | 39 | Rename field "testCheckClasses" to prevent any misunderstanding/clash with method "testCheckClasses" defined in superclass "org.sonar.plugins.java.api.CheckRegistrar$RegistrarContext". |
| java:S2234 | java-checks/src/main/java/org/sonar/java/checks/MembersDifferOnlyByCapitalizationCheck.java | 184 | Parameters to variableAndMethod have the same names but not the same order as the method arguments. |
| java:S3457 | java-checks/src/main/java/org/sonar/java/checks/OneDeclarationPerLineCheck.java | 123 | %n should be used in place of \n to produce the platform-specific line separator. |
| java:S5803 | java-checks-testkit/src/main/java/org/sonar/java/checks/verifier/internal/JavaCheckVerifier.java | 121 | Remove this usage of "scanForTesting", it is annotated with @VisibleForTesting and should not be accessed from production code. |
| java:S6204 | java-checks/src/main/java/org/sonar/java/checks/spring/SpringCacheableWithCachePutCheck.java | 89 | Replace this usage of 'Stream.collect(Collectors.toList())' with 'Stream.toList()' and ensure that the list is unmodified. |
| java:S6485 | java-checks/src/main/java/org/sonar/java/checks/unused/utils/AnnotationFieldReferenceFinder.java | 75 | Replace this call to the constructor with the better suited static method HashMap.newHashMap(int numMappings) |
| java:S6539 | java-frontend/src/main/java/org/sonar/java/model/JParser.java | 256 | Split this “Monster Class” into smaller and more specialized ones to reduce its dependencies on other classes from 59 to the maximum authorized 20 or less. |
| java:S6539 | java-frontend/src/main/java/org/sonar/plugins/java/api/tree/BaseTreeVisitor.java | 27 | Split this “Monster Class” into smaller and more specialized ones to reduce its dependencies on other classes from 73 to the maximum authorized 20 or less. |

</details>

<details>
<summary>Combined versus Source paths</summary>

### Largest semantic improvements

Top five by absolute change in unknown percentage.

None.

### Largest semantic regressions

Top five by absolute change in unknown percentage.

None.

| Unknown occurrences | Count |
|---|---:|
| Still unknown in both modes | 48 |
| No longer reported unknown | 0 |
| Newly reported unknown | 0 |

### No longer reported unknown

None.


### Newly reported unknown

None.


Semantic graph location differences unavailable: traversal is DISABLED, PARTIAL, or UNKNOWN in at least one compared mode. Primary identifier and finding comparisons remain valid.

### Findings only in Source paths

None.

### Findings only in Combined

None.

</details>

<details>
<summary>Combined versus Bytecode</summary>

### Largest semantic improvements

Top five by absolute change in unknown percentage.

| File | Second mode unknown % | First mode unknown % | Change (pp) |
|---|---:|---:|---:|
| java-checks/src/main/java/org/sonar/java/checks/IgnoredOperationStatusCheck.java | 76.821% | 0.000% | -76.821 |
| java-checks/src/main/java/org/sonar/java/checks/spring/AsyncMethodsOnConfigurationClassCheck.java | 73.864% | 0.000% | -73.864 |
| java-checks/src/main/java/org/sonar/java/checks/PublicConstructorInAbstractClassCheck.java | 73.737% | 0.000% | -73.737 |
| java-checks/src/main/java/org/sonar/java/checks/AvoidHighFrameratesOnMobileCheck.java | 70.667% | 0.000% | -70.667 |
| java-checks/src/main/java/org/sonar/java/checks/security/IntegerToHexStringCheck.java | 70.476% | 0.000% | -70.476 |

### Largest semantic regressions

Top five by absolute change in unknown percentage.

None.

| Unknown occurrences | Count |
|---|---:|
| Still unknown in both modes | 48 |
| No longer reported unknown | 69021 |
| Newly reported unknown | 0 |

### No longer reported unknown

| File | Range | Identifier | AST context |
|---|---|---|---|
| java-checks-aws/src/main/java/org/sonar/java/checks/aws/AbstractAwsMethodVisitor.java | (29:56)-(29:83) | IssuableSubscriptionVisitor | CLASS |
| java-checks-aws/src/main/java/org/sonar/java/checks/aws/AbstractAwsMethodVisitor.java | (30:24)-(30:28) | List | PARAMETERIZED_TYPE |
| java-checks-aws/src/main/java/org/sonar/java/checks/aws/AbstractAwsMethodVisitor.java | (30:29)-(30:33) | Tree | MEMBER_SELECT |
| java-checks-aws/src/main/java/org/sonar/java/checks/aws/AbstractAwsMethodVisitor.java | (30:34)-(30:38) | Kind | MEMBER_SELECT |
| java-checks-aws/src/main/java/org/sonar/java/checks/aws/AbstractAwsMethodVisitor.java | (30:65)-(30:69) | Tree | MEMBER_SELECT |
| java-checks-aws/src/main/java/org/sonar/java/checks/aws/AbstractAwsMethodVisitor.java | (30:70)-(30:74) | Kind | MEMBER_SELECT |
| java-checks-aws/src/main/java/org/sonar/java/checks/aws/AbstractAwsMethodVisitor.java | (30:75)-(30:81) | METHOD | MEMBER_SELECT |
| java-checks-aws/src/main/java/org/sonar/java/checks/aws/AbstractAwsMethodVisitor.java | (32:26)-(32:40) | MethodMatchers | VARIABLE |
| java-checks-aws/src/main/java/org/sonar/java/checks/aws/AbstractAwsMethodVisitor.java | (32:66)-(32:80) | MethodMatchers | MEMBER_SELECT |
| java-checks-aws/src/main/java/org/sonar/java/checks/aws/AbstractAwsMethodVisitor.java | (32:81)-(32:83) | or | MEMBER_SELECT |
| java-checks-aws/src/main/java/org/sonar/java/checks/aws/AbstractAwsMethodVisitor.java | (33:20)-(33:26) | create | MEMBER_SELECT |
| java-checks-aws/src/main/java/org/sonar/java/checks/aws/AbstractAwsMethodVisitor.java | (33:5)-(33:19) | MethodMatchers | MEMBER_SELECT |
| java-checks-aws/src/main/java/org/sonar/java/checks/aws/AbstractAwsMethodVisitor.java | (34:8)-(34:18) | ofSubTypes | MEMBER_SELECT |
| java-checks-aws/src/main/java/org/sonar/java/checks/aws/AbstractAwsMethodVisitor.java | (35:8)-(35:13) | names | MEMBER_SELECT |
| java-checks-aws/src/main/java/org/sonar/java/checks/aws/AbstractAwsMethodVisitor.java | (36:8)-(36:28) | addParametersMatcher | MEMBER_SELECT |
| java-checks-aws/src/main/java/org/sonar/java/checks/aws/AbstractAwsMethodVisitor.java | (37:8)-(37:13) | build | MEMBER_SELECT |
| java-checks-aws/src/main/java/org/sonar/java/checks/aws/AbstractAwsMethodVisitor.java | (38:20)-(38:26) | create | MEMBER_SELECT |
| java-checks-aws/src/main/java/org/sonar/java/checks/aws/AbstractAwsMethodVisitor.java | (38:5)-(38:19) | MethodMatchers | MEMBER_SELECT |
| java-checks-aws/src/main/java/org/sonar/java/checks/aws/AbstractAwsMethodVisitor.java | (39:8)-(39:18) | ofSubTypes | MEMBER_SELECT |
| java-checks-aws/src/main/java/org/sonar/java/checks/aws/AbstractAwsMethodVisitor.java | (40:8)-(40:13) | names | MEMBER_SELECT |

69001 additional occurrences omitted.


### Newly reported unknown

None.


Semantic graph location differences unavailable: traversal is DISABLED, PARTIAL, or UNKNOWN in at least one compared mode. Primary identifier and finding comparisons remain valid.

### Findings only in Bytecode

| Rule | File | Line | Message |
|---|---|---:|---|
| java:S1450 | java-checks/src/main/java/org/sonar/java/checks/TypeParametersShadowingCheck.java | 37 | Remove the "context" field and declare it as a local variable in the relevant methods. |

### Findings only in Combined

| Rule | File | Line | Message |
|---|---|---:|---|
| java:S110 | java-checks/src/main/java/org/sonar/java/checks/regex/AbstractRedosCheck.java | 51 | This class has 6 parents which is greater than 5 authorized. |
| java:S110 | java-checks/src/main/java/org/sonar/java/checks/regex/RedosCheck.java | 23 | This class has 7 parents which is greater than 5 authorized. |
| java:S110 | java-checks/src/main/java/org/sonar/java/checks/regex/RegexLookaheadCheck.java | 26 | This class has 6 parents which is greater than 5 authorized. |
| java:S110 | java-checks/src/main/java/org/sonar/java/checks/regex/ReluctantQuantifierWithEmptyContinuationCheck.java | 26 | This class has 6 parents which is greater than 5 authorized. |
| java:S110 | java-checks/src/main/java/org/sonar/java/checks/regex/SuperLinearRegexCheck.java | 23 | This class has 7 parents which is greater than 5 authorized. |
| java:S131 | java-checks/src/main/java/org/sonar/java/checks/BlockingOperationsInVirtualThreadsCheck.java | 96 | Complete cases by adding the missing enum constants or add a default case to this switch. |
| java:S131 | java-checks/src/main/java/org/sonar/java/checks/ThreadAsRunnableArgumentCheck.java | 55 | Complete cases by adding the missing enum constants or add a default case to this switch. |
| java:S131 | java-checks/src/main/java/org/sonar/java/checks/VolatileVariablesOperationsCheck.java | 125 | Complete cases by adding the missing enum constants or add a default case to this switch. |
| java:S1845 | java-checks-testkit/src/main/java/org/sonar/java/checks/verifier/TestCheckRegistrarContext.java | 39 | Rename field "testCheckClasses" to prevent any misunderstanding/clash with method "testCheckClasses" defined in superclass "org.sonar.plugins.java.api.CheckRegistrar$RegistrarContext". |
| java:S2234 | java-checks/src/main/java/org/sonar/java/checks/MembersDifferOnlyByCapitalizationCheck.java | 184 | Parameters to variableAndMethod have the same names but not the same order as the method arguments. |
| java:S3457 | java-checks/src/main/java/org/sonar/java/checks/OneDeclarationPerLineCheck.java | 123 | %n should be used in place of \n to produce the platform-specific line separator. |
| java:S5803 | java-checks-testkit/src/main/java/org/sonar/java/checks/verifier/internal/JavaCheckVerifier.java | 121 | Remove this usage of "scanForTesting", it is annotated with @VisibleForTesting and should not be accessed from production code. |
| java:S6204 | java-checks/src/main/java/org/sonar/java/checks/spring/SpringCacheableWithCachePutCheck.java | 89 | Replace this usage of 'Stream.collect(Collectors.toList())' with 'Stream.toList()' and ensure that the list is unmodified. |
| java:S6485 | java-checks/src/main/java/org/sonar/java/checks/unused/utils/AnnotationFieldReferenceFinder.java | 75 | Replace this call to the constructor with the better suited static method HashMap.newHashMap(int numMappings) |

</details>


## Measured timings

Warm-ups are excluded. JavaSensor time includes compilation.

| Sample | Mode | Maven/server wall (ms) | JavaSensor (ms) | Compilation (ms) | Compilation outcome | Generated classes |
|---|---|---:|---:|---:|---|---:|
| 1 | Baseline | 158672 | 80280.0 | 0.0 | DISABLED | 0 |
| 1 | Source paths | 289678 | 213817.0 | 0.0 | DISABLED | 0 |
| 1 | Bytecode | 165487 | 86662.0 | 1734.0 | FAILED | 574 |
| 1 | Combined | 301603 | 222564.0 | 1558.0 | FAILED | 574 |

## Run metadata

| Setting | Value |
|---|---|
| Baseline properties | {sonar.java.compileToByteCode=false, sonar.java.experimental.batchModeSizeInKB=500, sonar.java.fileByFile=true, sonar.java.sourcepath=, sonar.java.test.sourcepath=} |
| Bytecode properties | {sonar.java.compileToByteCode=true, sonar.java.experimental.batchModeSizeInKB=500, sonar.java.fileByFile=true, sonar.java.sourcepath=, sonar.java.test.sourcepath=} |
| Combined properties | {sonar.java.compileToByteCode=true, sonar.java.experimental.batchModeSizeInKB=500, sonar.java.fileByFile=true, sonar.java.sourcepath=/var/folders/q0/kmr6_fp118n0633zlbj5ydtw0000gq/T/junit-1607271975678351325/sonar-java-file-by-file-dependencies/combined/java-frontend/src/main/java,/var/folders/q0/kmr6_fp118n0633zlbj5ydtw0000gq/T/junit-1607271975678351325/sonar-java-file-by-file-dependencies/combined/java-checks-testkit/src/main/java,/var/folders/q0/kmr6_fp118n0633zlbj5ydtw0000gq/T/junit-1607271975678351325/sonar-java-file-by-file-dependencies/combined/java-checks/src/main/java,/var/folders/q0/kmr6_fp118n0633zlbj5ydtw0000gq/T/junit-1607271975678351325/sonar-java-file-by-file-dependencies/combined/java-checks-aws/src/main/java,/var/folders/q0/kmr6_fp118n0633zlbj5ydtw0000gq/T/junit-1607271975678351325/sonar-java-file-by-file-dependencies/combined/check-list/src/main/java,/var/folders/q0/kmr6_fp118n0633zlbj5ydtw0000gq/T/junit-1607271975678351325/sonar-java-file-by-file-dependencies/combined/external-reports/src/main/java,/var/folders/q0/kmr6_fp118n0633zlbj5ydtw0000gq/T/junit-1607271975678351325/sonar-java-file-by-file-dependencies/combined/sonar-java-plugin/src/main/java,/var/folders/q0/kmr6_fp118n0633zlbj5ydtw0000gq/T/junit-1607271975678351325/sonar-java-file-by-file-dependencies/combined/java-surefire/src/main/java,/var/folders/q0/kmr6_fp118n0633zlbj5ydtw0000gq/T/junit-1607271975678351325/sonar-java-file-by-file-dependencies/combined/java-jsp/src/main/java,/var/folders/q0/kmr6_fp118n0633zlbj5ydtw0000gq/T/junit-1607271975678351325/sonar-java-file-by-file-dependencies/combined/java-checks-common/src/main/java,/var/folders/q0/kmr6_fp118n0633zlbj5ydtw0000gq/T/junit-1607271975678351325/sonar-java-file-by-file-dependencies/combined/java-checks-test-sources/test-classpath-reader/src/main/java, sonar.java.test.sourcepath=} |
| External library count | 27 |
| Graph diagnostic stability | stable |
| Module check-list external classpath | [/var/folders/q0/kmr6_fp118n0633zlbj5ydtw0000gq/T/junit-1607271975678351325/libraries/01/sonar-plugin-api-14.1.0.5543.jar, /var/folders/q0/kmr6_fp118n0633zlbj5ydtw0000gq/T/junit-1607271975678351325/libraries/02/slf4j-api-2.0.20.jar, /var/folders/q0/kmr6_fp118n0633zlbj5ydtw0000gq/T/junit-1607271975678351325/libraries/00/jdt-package-1.9.0.1840.jar, /var/folders/q0/kmr6_fp118n0633zlbj5ydtw0000gq/T/junit-1607271975678351325/libraries/04/sslr-core-1.26.0.4194.jar, /var/folders/q0/kmr6_fp118n0633zlbj5ydtw0000gq/T/junit-1607271975678351325/libraries/10/sonar-regex-parsing-2.33.0.5369.jar, /var/folders/q0/kmr6_fp118n0633zlbj5ydtw0000gq/T/junit-1607271975678351325/libraries/11/sonar-performance-measure-2.33.0.5369.jar, /var/folders/q0/kmr6_fp118n0633zlbj5ydtw0000gq/T/junit-1607271975678351325/libraries/14/sonar-analyzer-recognizers-2.33.0.5369.jar, /var/folders/q0/kmr6_fp118n0633zlbj5ydtw0000gq/T/junit-1607271975678351325/libraries/05/jsr305-3.0.2.jar, /var/folders/q0/kmr6_fp118n0633zlbj5ydtw0000gq/T/junit-1607271975678351325/libraries/09/sonar-analyzer-commons-2.33.0.5369.jar, /var/folders/q0/kmr6_fp118n0633zlbj5ydtw0000gq/T/junit-1607271975678351325/libraries/08/commons-lang3-3.21.0.jar, /var/folders/q0/kmr6_fp118n0633zlbj5ydtw0000gq/T/junit-1607271975678351325/libraries/15/spring-expression-7.0.9.jar, /var/folders/q0/kmr6_fp118n0633zlbj5ydtw0000gq/T/junit-1607271975678351325/libraries/16/spring-core-7.0.9.jar, /var/folders/q0/kmr6_fp118n0633zlbj5ydtw0000gq/T/junit-1607271975678351325/libraries/17/commons-logging-1.4.0.jar, /var/folders/q0/kmr6_fp118n0633zlbj5ydtw0000gq/T/junit-1607271975678351325/libraries/07/gson-2.14.0.jar, /var/folders/q0/kmr6_fp118n0633zlbj5ydtw0000gq/T/junit-1607271975678351325/libraries/12/error_prone_annotations-2.48.0.jar] |
| Module external-reports external classpath | [/var/folders/q0/kmr6_fp118n0633zlbj5ydtw0000gq/T/junit-1607271975678351325/libraries/01/sonar-plugin-api-14.1.0.5543.jar, /var/folders/q0/kmr6_fp118n0633zlbj5ydtw0000gq/T/junit-1607271975678351325/libraries/02/slf4j-api-2.0.20.jar, /var/folders/q0/kmr6_fp118n0633zlbj5ydtw0000gq/T/junit-1607271975678351325/libraries/09/sonar-analyzer-commons-2.33.0.5369.jar, /var/folders/q0/kmr6_fp118n0633zlbj5ydtw0000gq/T/junit-1607271975678351325/libraries/18/sonar-xml-parsing-2.33.0.5369.jar, /var/folders/q0/kmr6_fp118n0633zlbj5ydtw0000gq/T/junit-1607271975678351325/libraries/05/jsr305-3.0.2.jar, /var/folders/q0/kmr6_fp118n0633zlbj5ydtw0000gq/T/junit-1607271975678351325/libraries/19/woodstox-core-7.2.2.jar, /var/folders/q0/kmr6_fp118n0633zlbj5ydtw0000gq/T/junit-1607271975678351325/libraries/20/stax2-api-4.3.0.jar] |
| Module java-checks external classpath | [/var/folders/q0/kmr6_fp118n0633zlbj5ydtw0000gq/T/junit-1607271975678351325/libraries/01/sonar-plugin-api-14.1.0.5543.jar, /var/folders/q0/kmr6_fp118n0633zlbj5ydtw0000gq/T/junit-1607271975678351325/libraries/02/slf4j-api-2.0.20.jar, /var/folders/q0/kmr6_fp118n0633zlbj5ydtw0000gq/T/junit-1607271975678351325/libraries/00/jdt-package-1.9.0.1840.jar, /var/folders/q0/kmr6_fp118n0633zlbj5ydtw0000gq/T/junit-1607271975678351325/libraries/04/sslr-core-1.26.0.4194.jar, /var/folders/q0/kmr6_fp118n0633zlbj5ydtw0000gq/T/junit-1607271975678351325/libraries/07/gson-2.14.0.jar, /var/folders/q0/kmr6_fp118n0633zlbj5ydtw0000gq/T/junit-1607271975678351325/libraries/10/sonar-regex-parsing-2.33.0.5369.jar, /var/folders/q0/kmr6_fp118n0633zlbj5ydtw0000gq/T/junit-1607271975678351325/libraries/11/sonar-performance-measure-2.33.0.5369.jar, /var/folders/q0/kmr6_fp118n0633zlbj5ydtw0000gq/T/junit-1607271975678351325/libraries/05/jsr305-3.0.2.jar, /var/folders/q0/kmr6_fp118n0633zlbj5ydtw0000gq/T/junit-1607271975678351325/libraries/14/sonar-analyzer-recognizers-2.33.0.5369.jar, /var/folders/q0/kmr6_fp118n0633zlbj5ydtw0000gq/T/junit-1607271975678351325/libraries/09/sonar-analyzer-commons-2.33.0.5369.jar, /var/folders/q0/kmr6_fp118n0633zlbj5ydtw0000gq/T/junit-1607271975678351325/libraries/08/commons-lang3-3.21.0.jar, /var/folders/q0/kmr6_fp118n0633zlbj5ydtw0000gq/T/junit-1607271975678351325/libraries/15/spring-expression-7.0.9.jar, /var/folders/q0/kmr6_fp118n0633zlbj5ydtw0000gq/T/junit-1607271975678351325/libraries/16/spring-core-7.0.9.jar, /var/folders/q0/kmr6_fp118n0633zlbj5ydtw0000gq/T/junit-1607271975678351325/libraries/17/commons-logging-1.4.0.jar, /var/folders/q0/kmr6_fp118n0633zlbj5ydtw0000gq/T/junit-1607271975678351325/libraries/06/error_prone_annotations-2.50.0.jar] |
| Module java-checks-aws external classpath | [/var/folders/q0/kmr6_fp118n0633zlbj5ydtw0000gq/T/junit-1607271975678351325/libraries/01/sonar-plugin-api-14.1.0.5543.jar, /var/folders/q0/kmr6_fp118n0633zlbj5ydtw0000gq/T/junit-1607271975678351325/libraries/02/slf4j-api-2.0.20.jar, /var/folders/q0/kmr6_fp118n0633zlbj5ydtw0000gq/T/junit-1607271975678351325/libraries/00/jdt-package-1.9.0.1840.jar, /var/folders/q0/kmr6_fp118n0633zlbj5ydtw0000gq/T/junit-1607271975678351325/libraries/04/sslr-core-1.26.0.4194.jar, /var/folders/q0/kmr6_fp118n0633zlbj5ydtw0000gq/T/junit-1607271975678351325/libraries/07/gson-2.14.0.jar, /var/folders/q0/kmr6_fp118n0633zlbj5ydtw0000gq/T/junit-1607271975678351325/libraries/10/sonar-regex-parsing-2.33.0.5369.jar, /var/folders/q0/kmr6_fp118n0633zlbj5ydtw0000gq/T/junit-1607271975678351325/libraries/11/sonar-performance-measure-2.33.0.5369.jar, /var/folders/q0/kmr6_fp118n0633zlbj5ydtw0000gq/T/junit-1607271975678351325/libraries/05/jsr305-3.0.2.jar, /var/folders/q0/kmr6_fp118n0633zlbj5ydtw0000gq/T/junit-1607271975678351325/libraries/09/sonar-analyzer-commons-2.33.0.5369.jar, /var/folders/q0/kmr6_fp118n0633zlbj5ydtw0000gq/T/junit-1607271975678351325/libraries/14/sonar-analyzer-recognizers-2.33.0.5369.jar, /var/folders/q0/kmr6_fp118n0633zlbj5ydtw0000gq/T/junit-1607271975678351325/libraries/08/commons-lang3-3.21.0.jar, /var/folders/q0/kmr6_fp118n0633zlbj5ydtw0000gq/T/junit-1607271975678351325/libraries/06/error_prone_annotations-2.50.0.jar] |
| Module java-checks-common external classpath | [/var/folders/q0/kmr6_fp118n0633zlbj5ydtw0000gq/T/junit-1607271975678351325/libraries/01/sonar-plugin-api-14.1.0.5543.jar, /var/folders/q0/kmr6_fp118n0633zlbj5ydtw0000gq/T/junit-1607271975678351325/libraries/02/slf4j-api-2.0.20.jar, /var/folders/q0/kmr6_fp118n0633zlbj5ydtw0000gq/T/junit-1607271975678351325/libraries/00/jdt-package-1.9.0.1840.jar, /var/folders/q0/kmr6_fp118n0633zlbj5ydtw0000gq/T/junit-1607271975678351325/libraries/04/sslr-core-1.26.0.4194.jar, /var/folders/q0/kmr6_fp118n0633zlbj5ydtw0000gq/T/junit-1607271975678351325/libraries/07/gson-2.14.0.jar, /var/folders/q0/kmr6_fp118n0633zlbj5ydtw0000gq/T/junit-1607271975678351325/libraries/09/sonar-analyzer-commons-2.33.0.5369.jar, /var/folders/q0/kmr6_fp118n0633zlbj5ydtw0000gq/T/junit-1607271975678351325/libraries/10/sonar-regex-parsing-2.33.0.5369.jar, /var/folders/q0/kmr6_fp118n0633zlbj5ydtw0000gq/T/junit-1607271975678351325/libraries/11/sonar-performance-measure-2.33.0.5369.jar, /var/folders/q0/kmr6_fp118n0633zlbj5ydtw0000gq/T/junit-1607271975678351325/libraries/14/sonar-analyzer-recognizers-2.33.0.5369.jar, /var/folders/q0/kmr6_fp118n0633zlbj5ydtw0000gq/T/junit-1607271975678351325/libraries/05/jsr305-3.0.2.jar, /var/folders/q0/kmr6_fp118n0633zlbj5ydtw0000gq/T/junit-1607271975678351325/libraries/08/commons-lang3-3.21.0.jar, /var/folders/q0/kmr6_fp118n0633zlbj5ydtw0000gq/T/junit-1607271975678351325/libraries/06/error_prone_annotations-2.50.0.jar] |
| Module java-checks-test-sources/test-classpath-reader external classpath | [/var/folders/q0/kmr6_fp118n0633zlbj5ydtw0000gq/T/junit-1607271975678351325/libraries/05/jsr305-3.0.2.jar] |
| Module java-checks-testkit external classpath | [/var/folders/q0/kmr6_fp118n0633zlbj5ydtw0000gq/T/junit-1607271975678351325/libraries/05/jsr305-3.0.2.jar, /var/folders/q0/kmr6_fp118n0633zlbj5ydtw0000gq/T/junit-1607271975678351325/libraries/00/jdt-package-1.9.0.1840.jar, /var/folders/q0/kmr6_fp118n0633zlbj5ydtw0000gq/T/junit-1607271975678351325/libraries/04/sslr-core-1.26.0.4194.jar, /var/folders/q0/kmr6_fp118n0633zlbj5ydtw0000gq/T/junit-1607271975678351325/libraries/08/commons-lang3-3.21.0.jar, /var/folders/q0/kmr6_fp118n0633zlbj5ydtw0000gq/T/junit-1607271975678351325/libraries/09/sonar-analyzer-commons-2.33.0.5369.jar, /var/folders/q0/kmr6_fp118n0633zlbj5ydtw0000gq/T/junit-1607271975678351325/libraries/10/sonar-regex-parsing-2.33.0.5369.jar, /var/folders/q0/kmr6_fp118n0633zlbj5ydtw0000gq/T/junit-1607271975678351325/libraries/11/sonar-performance-measure-2.33.0.5369.jar, /var/folders/q0/kmr6_fp118n0633zlbj5ydtw0000gq/T/junit-1607271975678351325/libraries/01/sonar-plugin-api-14.1.0.5543.jar, /var/folders/q0/kmr6_fp118n0633zlbj5ydtw0000gq/T/junit-1607271975678351325/libraries/02/slf4j-api-2.0.20.jar, /var/folders/q0/kmr6_fp118n0633zlbj5ydtw0000gq/T/junit-1607271975678351325/libraries/07/gson-2.14.0.jar, /var/folders/q0/kmr6_fp118n0633zlbj5ydtw0000gq/T/junit-1607271975678351325/libraries/12/error_prone_annotations-2.48.0.jar, /var/folders/q0/kmr6_fp118n0633zlbj5ydtw0000gq/T/junit-1607271975678351325/libraries/13/sonar-analyzer-test-commons-2.33.0.5369.jar] |
| Module java-frontend external classpath | [/var/folders/q0/kmr6_fp118n0633zlbj5ydtw0000gq/T/junit-1607271975678351325/libraries/00/jdt-package-1.9.0.1840.jar, /var/folders/q0/kmr6_fp118n0633zlbj5ydtw0000gq/T/junit-1607271975678351325/libraries/01/sonar-plugin-api-14.1.0.5543.jar, /var/folders/q0/kmr6_fp118n0633zlbj5ydtw0000gq/T/junit-1607271975678351325/libraries/02/slf4j-api-2.0.20.jar, /var/folders/q0/kmr6_fp118n0633zlbj5ydtw0000gq/T/junit-1607271975678351325/libraries/03/sonarlint-plugin-api-12.0.2.87499.jar, /var/folders/q0/kmr6_fp118n0633zlbj5ydtw0000gq/T/junit-1607271975678351325/libraries/04/sslr-core-1.26.0.4194.jar, /var/folders/q0/kmr6_fp118n0633zlbj5ydtw0000gq/T/junit-1607271975678351325/libraries/05/jsr305-3.0.2.jar, /var/folders/q0/kmr6_fp118n0633zlbj5ydtw0000gq/T/junit-1607271975678351325/libraries/06/error_prone_annotations-2.50.0.jar, /var/folders/q0/kmr6_fp118n0633zlbj5ydtw0000gq/T/junit-1607271975678351325/libraries/07/gson-2.14.0.jar, /var/folders/q0/kmr6_fp118n0633zlbj5ydtw0000gq/T/junit-1607271975678351325/libraries/08/commons-lang3-3.21.0.jar, /var/folders/q0/kmr6_fp118n0633zlbj5ydtw0000gq/T/junit-1607271975678351325/libraries/09/sonar-analyzer-commons-2.33.0.5369.jar, /var/folders/q0/kmr6_fp118n0633zlbj5ydtw0000gq/T/junit-1607271975678351325/libraries/10/sonar-regex-parsing-2.33.0.5369.jar, /var/folders/q0/kmr6_fp118n0633zlbj5ydtw0000gq/T/junit-1607271975678351325/libraries/11/sonar-performance-measure-2.33.0.5369.jar] |
| Module java-jsp external classpath | [/var/folders/q0/kmr6_fp118n0633zlbj5ydtw0000gq/T/junit-1607271975678351325/libraries/01/sonar-plugin-api-14.1.0.5543.jar, /var/folders/q0/kmr6_fp118n0633zlbj5ydtw0000gq/T/junit-1607271975678351325/libraries/02/slf4j-api-2.0.20.jar, /var/folders/q0/kmr6_fp118n0633zlbj5ydtw0000gq/T/junit-1607271975678351325/libraries/00/jdt-package-1.9.0.1840.jar, /var/folders/q0/kmr6_fp118n0633zlbj5ydtw0000gq/T/junit-1607271975678351325/libraries/04/sslr-core-1.26.0.4194.jar, /var/folders/q0/kmr6_fp118n0633zlbj5ydtw0000gq/T/junit-1607271975678351325/libraries/05/jsr305-3.0.2.jar, /var/folders/q0/kmr6_fp118n0633zlbj5ydtw0000gq/T/junit-1607271975678351325/libraries/07/gson-2.14.0.jar, /var/folders/q0/kmr6_fp118n0633zlbj5ydtw0000gq/T/junit-1607271975678351325/libraries/12/error_prone_annotations-2.48.0.jar, /var/folders/q0/kmr6_fp118n0633zlbj5ydtw0000gq/T/junit-1607271975678351325/libraries/08/commons-lang3-3.21.0.jar, /var/folders/q0/kmr6_fp118n0633zlbj5ydtw0000gq/T/junit-1607271975678351325/libraries/09/sonar-analyzer-commons-2.33.0.5369.jar, /var/folders/q0/kmr6_fp118n0633zlbj5ydtw0000gq/T/junit-1607271975678351325/libraries/10/sonar-regex-parsing-2.33.0.5369.jar, /var/folders/q0/kmr6_fp118n0633zlbj5ydtw0000gq/T/junit-1607271975678351325/libraries/11/sonar-performance-measure-2.33.0.5369.jar, /var/folders/q0/kmr6_fp118n0633zlbj5ydtw0000gq/T/junit-1607271975678351325/libraries/22/tomcat-embed-jasper-9.0.122.jar, /var/folders/q0/kmr6_fp118n0633zlbj5ydtw0000gq/T/junit-1607271975678351325/libraries/23/tomcat-embed-core-9.0.122.jar, /var/folders/q0/kmr6_fp118n0633zlbj5ydtw0000gq/T/junit-1607271975678351325/libraries/24/tomcat-annotations-api-9.0.122.jar, /var/folders/q0/kmr6_fp118n0633zlbj5ydtw0000gq/T/junit-1607271975678351325/libraries/25/tomcat-embed-el-9.0.122.jar] |
| Module java-surefire external classpath | [/var/folders/q0/kmr6_fp118n0633zlbj5ydtw0000gq/T/junit-1607271975678351325/libraries/01/sonar-plugin-api-14.1.0.5543.jar, /var/folders/q0/kmr6_fp118n0633zlbj5ydtw0000gq/T/junit-1607271975678351325/libraries/02/slf4j-api-2.0.20.jar, /var/folders/q0/kmr6_fp118n0633zlbj5ydtw0000gq/T/junit-1607271975678351325/libraries/00/jdt-package-1.9.0.1840.jar, /var/folders/q0/kmr6_fp118n0633zlbj5ydtw0000gq/T/junit-1607271975678351325/libraries/04/sslr-core-1.26.0.4194.jar, /var/folders/q0/kmr6_fp118n0633zlbj5ydtw0000gq/T/junit-1607271975678351325/libraries/07/gson-2.14.0.jar, /var/folders/q0/kmr6_fp118n0633zlbj5ydtw0000gq/T/junit-1607271975678351325/libraries/12/error_prone_annotations-2.48.0.jar, /var/folders/q0/kmr6_fp118n0633zlbj5ydtw0000gq/T/junit-1607271975678351325/libraries/08/commons-lang3-3.21.0.jar, /var/folders/q0/kmr6_fp118n0633zlbj5ydtw0000gq/T/junit-1607271975678351325/libraries/09/sonar-analyzer-commons-2.33.0.5369.jar, /var/folders/q0/kmr6_fp118n0633zlbj5ydtw0000gq/T/junit-1607271975678351325/libraries/10/sonar-regex-parsing-2.33.0.5369.jar, /var/folders/q0/kmr6_fp118n0633zlbj5ydtw0000gq/T/junit-1607271975678351325/libraries/11/sonar-performance-measure-2.33.0.5369.jar, /var/folders/q0/kmr6_fp118n0633zlbj5ydtw0000gq/T/junit-1607271975678351325/libraries/21/staxmate-2.4.2.jar, /var/folders/q0/kmr6_fp118n0633zlbj5ydtw0000gq/T/junit-1607271975678351325/libraries/20/stax2-api-4.3.0.jar, /var/folders/q0/kmr6_fp118n0633zlbj5ydtw0000gq/T/junit-1607271975678351325/libraries/18/sonar-xml-parsing-2.33.0.5369.jar, /var/folders/q0/kmr6_fp118n0633zlbj5ydtw0000gq/T/junit-1607271975678351325/libraries/05/jsr305-3.0.2.jar, /var/folders/q0/kmr6_fp118n0633zlbj5ydtw0000gq/T/junit-1607271975678351325/libraries/19/woodstox-core-7.2.2.jar] |
| Module sonar-java-plugin external classpath | [/var/folders/q0/kmr6_fp118n0633zlbj5ydtw0000gq/T/junit-1607271975678351325/libraries/21/staxmate-2.4.2.jar, /var/folders/q0/kmr6_fp118n0633zlbj5ydtw0000gq/T/junit-1607271975678351325/libraries/20/stax2-api-4.3.0.jar, /var/folders/q0/kmr6_fp118n0633zlbj5ydtw0000gq/T/junit-1607271975678351325/libraries/00/jdt-package-1.9.0.1840.jar, /var/folders/q0/kmr6_fp118n0633zlbj5ydtw0000gq/T/junit-1607271975678351325/libraries/04/sslr-core-1.26.0.4194.jar, /var/folders/q0/kmr6_fp118n0633zlbj5ydtw0000gq/T/junit-1607271975678351325/libraries/07/gson-2.14.0.jar, /var/folders/q0/kmr6_fp118n0633zlbj5ydtw0000gq/T/junit-1607271975678351325/libraries/08/commons-lang3-3.21.0.jar, /var/folders/q0/kmr6_fp118n0633zlbj5ydtw0000gq/T/junit-1607271975678351325/libraries/10/sonar-regex-parsing-2.33.0.5369.jar, /var/folders/q0/kmr6_fp118n0633zlbj5ydtw0000gq/T/junit-1607271975678351325/libraries/14/sonar-analyzer-recognizers-2.33.0.5369.jar, /var/folders/q0/kmr6_fp118n0633zlbj5ydtw0000gq/T/junit-1607271975678351325/libraries/15/spring-expression-7.0.9.jar, /var/folders/q0/kmr6_fp118n0633zlbj5ydtw0000gq/T/junit-1607271975678351325/libraries/16/spring-core-7.0.9.jar, /var/folders/q0/kmr6_fp118n0633zlbj5ydtw0000gq/T/junit-1607271975678351325/libraries/22/tomcat-embed-jasper-9.0.122.jar, /var/folders/q0/kmr6_fp118n0633zlbj5ydtw0000gq/T/junit-1607271975678351325/libraries/23/tomcat-embed-core-9.0.122.jar, /var/folders/q0/kmr6_fp118n0633zlbj5ydtw0000gq/T/junit-1607271975678351325/libraries/24/tomcat-annotations-api-9.0.122.jar, /var/folders/q0/kmr6_fp118n0633zlbj5ydtw0000gq/T/junit-1607271975678351325/libraries/25/tomcat-embed-el-9.0.122.jar, /var/folders/q0/kmr6_fp118n0633zlbj5ydtw0000gq/T/junit-1607271975678351325/libraries/09/sonar-analyzer-commons-2.33.0.5369.jar, /var/folders/q0/kmr6_fp118n0633zlbj5ydtw0000gq/T/junit-1607271975678351325/libraries/18/sonar-xml-parsing-2.33.0.5369.jar, /var/folders/q0/kmr6_fp118n0633zlbj5ydtw0000gq/T/junit-1607271975678351325/libraries/05/jsr305-3.0.2.jar, /var/folders/q0/kmr6_fp118n0633zlbj5ydtw0000gq/T/junit-1607271975678351325/libraries/26/xercesImpl-2.12.2.jar, /var/folders/q0/kmr6_fp118n0633zlbj5ydtw0000gq/T/junit-1607271975678351325/libraries/19/woodstox-core-7.2.2.jar, /var/folders/q0/kmr6_fp118n0633zlbj5ydtw0000gq/T/junit-1607271975678351325/libraries/11/sonar-performance-measure-2.33.0.5369.jar, /var/folders/q0/kmr6_fp118n0633zlbj5ydtw0000gq/T/junit-1607271975678351325/libraries/01/sonar-plugin-api-14.1.0.5543.jar, /var/folders/q0/kmr6_fp118n0633zlbj5ydtw0000gq/T/junit-1607271975678351325/libraries/02/slf4j-api-2.0.20.jar, /var/folders/q0/kmr6_fp118n0633zlbj5ydtw0000gq/T/junit-1607271975678351325/libraries/17/commons-logging-1.4.0.jar, /var/folders/q0/kmr6_fp118n0633zlbj5ydtw0000gq/T/junit-1607271975678351325/libraries/06/error_prone_annotations-2.50.0.jar] |
| Repeated result stability | stable |
| Scenario | Each production file is parsed separately; every mode receives the same compile-scope external JARs; native Maven module scope |
| Shared scenario properties | {sonar.java.experimental.batchModeSizeInKB=500, sonar.java.fileByFile=true} |
| Source paths properties | {sonar.java.compileToByteCode=false, sonar.java.experimental.batchModeSizeInKB=500, sonar.java.fileByFile=true, sonar.java.sourcepath=/var/folders/q0/kmr6_fp118n0633zlbj5ydtw0000gq/T/junit-1607271975678351325/sonar-java-file-by-file-dependencies/sourcepaths/java-frontend/src/main/java,/var/folders/q0/kmr6_fp118n0633zlbj5ydtw0000gq/T/junit-1607271975678351325/sonar-java-file-by-file-dependencies/sourcepaths/java-checks-testkit/src/main/java,/var/folders/q0/kmr6_fp118n0633zlbj5ydtw0000gq/T/junit-1607271975678351325/sonar-java-file-by-file-dependencies/sourcepaths/java-checks/src/main/java,/var/folders/q0/kmr6_fp118n0633zlbj5ydtw0000gq/T/junit-1607271975678351325/sonar-java-file-by-file-dependencies/sourcepaths/java-checks-aws/src/main/java,/var/folders/q0/kmr6_fp118n0633zlbj5ydtw0000gq/T/junit-1607271975678351325/sonar-java-file-by-file-dependencies/sourcepaths/check-list/src/main/java,/var/folders/q0/kmr6_fp118n0633zlbj5ydtw0000gq/T/junit-1607271975678351325/sonar-java-file-by-file-dependencies/sourcepaths/external-reports/src/main/java,/var/folders/q0/kmr6_fp118n0633zlbj5ydtw0000gq/T/junit-1607271975678351325/sonar-java-file-by-file-dependencies/sourcepaths/sonar-java-plugin/src/main/java,/var/folders/q0/kmr6_fp118n0633zlbj5ydtw0000gq/T/junit-1607271975678351325/sonar-java-file-by-file-dependencies/sourcepaths/java-surefire/src/main/java,/var/folders/q0/kmr6_fp118n0633zlbj5ydtw0000gq/T/junit-1607271975678351325/sonar-java-file-by-file-dependencies/sourcepaths/java-jsp/src/main/java,/var/folders/q0/kmr6_fp118n0633zlbj5ydtw0000gq/T/junit-1607271975678351325/sonar-java-file-by-file-dependencies/sourcepaths/java-checks-common/src/main/java,/var/folders/q0/kmr6_fp118n0633zlbj5ydtw0000gq/T/junit-1607271975678351325/sonar-java-file-by-file-dependencies/sourcepaths/java-checks-test-sources/test-classpath-reader/src/main/java, sonar.java.test.sourcepath=} |

</details>


## Run metadata

| Setting | Value |
|---|---|
| Analyzed Java files | 1320 |
| Analyzed project | sonar-java |
| Analyzer build manifest revision | 9cad3cae7337713c49596b56061ba576b7688aee |
| Analyzer checkout dirty | false |
| Analyzer checkout revision | bf3a4fe9c7d469ebc22e0915fb903f9bfa310c37 |
| Analyzer plugin SHA-256 | 698e6d5e02cb44c4c1bcd4d1c011f7149da6bb7fe555b8e5502880f1718e19f5 |
| Analyzer plugin path | /Users/matthew.elliott/Work/Code/sonar-java/sonar-java-plugin/target/sonar-java-plugin-8.45.0-SNAPSHOT.jar |
| Analyzer plugin version | 8.45.0-SNAPSHOT |
| Binding probe plugin SHA-256 | bda871482ab0648e19e2bcaca1e9cb0b7c8004116e68c3a80548d12761ca16c7 |
| Classpath policy | External compile-scope JARs only, preserving each module's versions and order; cached project module artifacts excluded |
| Clean compilation/Baseline bindings | Semantic binding probes: 4 checked, 0 failed. |
| Clean compilation/Bytecode bindings | Semantic binding probes: 4 checked, 0 failed. |
| Clean compilation/Combined bindings | Semantic binding probes: 4 checked, 0 failed. |
| Clean compilation/Source paths bindings | Semantic binding probes: 4 checked, 0 failed. |
| Compilation failure recovery/Baseline bindings | Semantic binding probes: 5 checked, 0 failed. |
| Compilation failure recovery/Bytecode bindings | Semantic binding probes: 5 checked, 0 failed. |
| Compilation failure recovery/Combined bindings | Semantic binding probes: 5 checked, 0 failed. |
| Compilation failure recovery/Source paths bindings | Semantic binding probes: 5 checked, 0 failed. |
| Compilation measurements | Evaluation-only structured log hook records outcome, source/class counts, and elapsed time before cleanup |
| Compile classpath SHA-256 (ordered) | 92e6459b94521c1f4e794f9308f856d28253a1f8b126c23046eee1b147a5bea2 |
| Compile dependency 0 | jdt-package-1.9.0.1840.jar / SHA-256 c8a34cb623f5476076ce05fafdf17e678269f3d24172f7cf3a9f72af5f709831 |
| Compile dependency 1 | sonar-plugin-api-14.1.0.5543.jar / SHA-256 ac70438a14ba2de9459b8c4d8662396c4ff9c5917166dccf5faf30500f322f31 |
| Compile dependency 10 | sonar-regex-parsing-2.33.0.5369.jar / SHA-256 f4c934dd6f5a4d11368bfb6608cda4e49c86f2196debad9169d2da3b6163e053 |
| Compile dependency 11 | sonar-performance-measure-2.33.0.5369.jar / SHA-256 053c82649a5928e43f3b204f490e09e592926a9d6402146c6ac8c2ee96bf6906 |
| Compile dependency 12 | error_prone_annotations-2.48.0.jar / SHA-256 b49c5c958316ed67a09c699dda9aa749caf434d51d863dea599ef36a49b9c855 |
| Compile dependency 13 | sonar-analyzer-test-commons-2.33.0.5369.jar / SHA-256 fd8bec6076ed1a644daafe8c4f9bcd56a2079f666036b22470a6c23a9b516808 |
| Compile dependency 14 | sonar-analyzer-recognizers-2.33.0.5369.jar / SHA-256 1e62e3a690fc08b40764707c35baf7cab70eb048708812754f82259cc9cf22e5 |
| Compile dependency 15 | spring-expression-7.0.9.jar / SHA-256 046434c40f43819729b9b1db0e6c659dfa68184c1c2c2efa5f7b3a5b27c4e2e2 |
| Compile dependency 16 | spring-core-7.0.9.jar / SHA-256 5195f4722699b39878d99a832549fe65df2890b159d063b88fff31b1ca65ae36 |
| Compile dependency 17 | commons-logging-1.4.0.jar / SHA-256 d175dbd751dd782a63bde28c7a039520e971f25e84b79c19b8435edc3603e0dc |
| Compile dependency 18 | sonar-xml-parsing-2.33.0.5369.jar / SHA-256 0ab7dbea474a67e6ac99c36828181c95721acccfddddde705fdceddb82b9deae |
| Compile dependency 19 | woodstox-core-7.2.2.jar / SHA-256 3bd35f778fb15cd517fae51961ba7a9388eae8e4a836fac1166c7f70740e55cd |
| Compile dependency 2 | slf4j-api-2.0.20.jar / SHA-256 7e1446499b359675d8aabe6af2de86b85e72ae8a2a932c0b40d0e5ba57097439 |
| Compile dependency 20 | stax2-api-4.3.0.jar / SHA-256 7c805f36129ea9fa42b696093b7ae1eb20bb6ccec65c8280d6f33db5609ca5e1 |
| Compile dependency 21 | staxmate-2.4.2.jar / SHA-256 e394700bcade6b7958c1a9929b11972be7bda2a5dc69080a6836e2c1e8daf54f |
| Compile dependency 22 | tomcat-embed-jasper-9.0.122.jar / SHA-256 c01a2ac576f40e8a6e4bf4a6dc1094d51524df5b490a6df68c71f90cb0d21110 |
| Compile dependency 23 | tomcat-embed-core-9.0.122.jar / SHA-256 d89e48fc2835102846988d3cdb9d833f61e1934e5e46440697b6f0cc24923455 |
| Compile dependency 24 | tomcat-annotations-api-9.0.122.jar / SHA-256 c7d79297d803ce3f1257c2349194567cca17108870bfda52b9b2c10d3cc8d18b |
| Compile dependency 25 | tomcat-embed-el-9.0.122.jar / SHA-256 cd9fd2b078ec8129134669a9a6bd71c87d87b5e3d4f39dbc328e85342b90bfc6 |
| Compile dependency 26 | xercesImpl-2.12.2.jar / SHA-256 6fc991829af1708d15aea50c66f0beadcd2cfeb6968e0b2f55c1b0909883fe16 |
| Compile dependency 3 | sonarlint-plugin-api-12.0.2.87499.jar / SHA-256 36271f29eeec76fcba3e8edd5c93780d597d315e0b3b8fa2c70b22de45566471 |
| Compile dependency 4 | sslr-core-1.26.0.4194.jar / SHA-256 ee8de7cf40b2852c55f9d4ae2d2223410f9f3816f5720b6f2a6963a21e189158 |
| Compile dependency 5 | jsr305-3.0.2.jar / SHA-256 766ad2a0783f2687962c8ad74ceecc38a28b9f72a2d085ee438b7813e928d0c7 |
| Compile dependency 6 | error_prone_annotations-2.50.0.jar / SHA-256 4667724877f1d37a689202da191e23efa7657c62eef93ccdac406eccfe5cdd0a |
| Compile dependency 7 | gson-2.14.0.jar / SHA-256 2cbd119bf1961c28788310963dc80ba65f58cdeec1dd139c8bdb1240faa2c36f |
| Compile dependency 8 | commons-lang3-3.21.0.jar / SHA-256 a2af247ec4db6eb8947e18e23e0433e4871a7c5d0a40e1f92e46ede54ae1defe |
| Compile dependency 9 | sonar-analyzer-commons-2.33.0.5369.jar / SHA-256 7e1974e0a661b67b105618eadee240b4987461297ffffe0262585f3709c624cf |
| Dependency outside analysis/Baseline bindings | Semantic binding probes: 4 checked, 0 failed. |
| Dependency outside analysis/Bytecode bindings | Semantic binding probes: 4 checked, 0 failed. |
| Dependency outside analysis/Combined bindings | Semantic binding probes: 4 checked, 0 failed. |
| Dependency outside analysis/Source paths bindings | Semantic binding probes: 4 checked, 0 failed. |
| Dependency resolution | Pinned dependency:build-classpath on copied POMs; compile scope only; no Maven compilation |
| Dependency resolution time (ms), excluded from scans | 2092 |
| Encoding compatibility/Baseline bindings | Semantic binding probes: 4 checked, 0 failed. |
| Encoding compatibility/Bytecode bindings | Semantic binding probes: 4 checked, 0 failed. |
| Encoding compatibility/Combined bindings | Semantic binding probes: 4 checked, 0 failed. |
| Encoding compatibility/Source paths bindings | Semantic binding probes: 4 checked, 0 failed. |
| Excluded observer run | Unbounded refreshed graph traversal stalled on the two-file smoke scan and was stopped; excluded from all recorded comparison timings |
| Extra properties shared by all modes | {} |
| Features | PR #6308 source paths and PR #6309 internal compilation; same plugin used for all four modes |
| Graph observation budget | 0 |
| Graph timing mode | Recursive diagnostics disabled; full AST identifiers, findings, and per-file observations remain enabled |
| JDK | 21.0.10 / Microsoft |
| Java language level | 21 |
| Maven scanner version | 5.9.0.7291 |
| Measured repetitions per mode | 1 |
| Module scope | [java-frontend, java-checks-testkit, java-checks, java-checks-aws, check-list, external-reports, sonar-java-plugin, java-surefire, java-jsp, java-checks-common, java-checks-test-sources/test-classpath-reader] |
| PR #6308 revision | 7068bcd4c102a0d175db7221785660f685f238ea |
| PR #6309 revision | 65bf117ea48490855fc552b2e9c182268ec75b2d |
| Parser scenario selection | sonar.java.fileByFile=true |
| Per-file measurements | Additive module.files observations preserve latest canonical identifier and graph fields |
| Profile | Sonar way |
| Recorded at (UTC) | 2026-10-07T14:20:40.561688Z |
| Scan topology | Native synthetic Maven reactor; real production module boundaries; no project dependencies or supplied project bytecode |
| Semantic reporter revision | 9cad3cae7337713c49596b56061ba576b7688aee |
| Server version | 26.10.0.132816 |
| Server workspace | /Users/matthew.elliott/Work/Code/sonar-java/its/plugin/tests/target/comparison-orchestrator-10359840132084580663 |
| Target checkout dirty | true |
| Target checkout revision | 6fe7dca3eaf2ee819db7c9b674fe71b938c2bed0 |
| Target source snapshot SHA-256 | bbe86a6640f28d6927100bd3e018e81d51a409b490f70d787d88768be7081847 |
| Test harness dirty | true |
| Test harness revision | 6fe7dca3eaf2ee819db7c9b674fe71b938c2bed0 |
| Timing protocol | one measured scan per mode; no warm-up |
| compilation-measurements.patch SHA-256 | 790b2230deb0ea1e3e5a75b788f14f6267ad819981d3a20978bc54a56a05a824 |
| semantic-file-measurements.patch SHA-256 | 558f1f785e014efa673f0b4721b5150a0aa1faaae6bd245fe465d83b9e951d5c |
| semantic-graph-bounds.patch SHA-256 | 6282981166ca733aa70e840dacf7c210bb2bf0e19ca2f923ffae5be914763109 |

## Reading the comparison

- Identifier percentages use aggregate counts. Same-dataset comparisons require successful scans, identical indexed files, matching canonical modules and global/module identifier totals. Per-file coverage and totals are checked when observations exist; unavailable file observations are labelled explicitly. Unstable primary repetitions have no comparison metrics.
- Compilation SUCCESS or FAILED is measured separately from scan success. Missing measurements are UNAVAILABLE or N/A. Failed compilation can leave partial classes that are available during analysis; generated classes must be removed afterward.
- JavaSensor time includes internal compilation; Maven/server time also includes scanner startup and server processing. All timing medians exclude warm-ups. A few repetitions describe this run and do not establish a general performance ranking.
- Product findings include only `java:` rules. Probe findings are instrumentation. Findings on real projects measure agreement, while the explicitly reviewed fixture checks binding correctness and expected findings.
- Unknown identifier occurrences match by file, name, and token range. Fewer unknowns alone do not prove correct bindings. AST context is diagnostic rather than a semantic role.
- Semantic graph counts describe unique references reached by the target branch's bounded traversal. Resolving a name can expose additional graph nodes, so these counts and location differences are diagnostics rather than coverage percentages or correctness scores.
- Source paths do not expand analyzed scope. Internal compilation disables annotation processing. External libraries, parser batching, source files, JDK, profile, and extra properties are held fixed across each dataset's four modes.
