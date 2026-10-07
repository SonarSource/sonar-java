# Java source-path and internal-compilation comparison

One analyzer artifact, four modes: **Baseline**, **Source paths (#6308)**, **Bytecode (#6309)**, and **Combined**. Maven runs only `sonar:sonar`; no project bytecode is supplied.

## Results at a glance

Cells show **unknown identifiers (unknown %; change from the same dataset's baseline)**. A negative change means fewer unknown identifiers. Invalid or unstable comparisons have no change metric.

| Dataset / parser batching / external JARs | Baseline | Source paths | Bytecode | Combined |
|---|---:|---:|---:|---:|
| sonar-xml / normal / no-libraries | 1437 (25.624%; Δ +0) | 1437 (25.624%; Δ +0) | 1437 (25.624%; Δ +0) | 1437 (25.624%; Δ +0) |
| sonar-xml / file-by-file / no-libraries | 1602 (28.566%; Δ +0) | 1437 (25.624%; Δ -165) | 1593 (28.406%; Δ -9) | 1437 (25.624%; Δ -165) |
| sonar-xml / normal / dependencies | 0 (0.000%; Δ +0) | 0 (0.000%; Δ +0) | 0 (0.000%; Δ +0) | 0 (0.000%; Δ +0) |
| sonar-xml / file-by-file / dependencies | 385 (6.865%; Δ +0) | 0 (0.000%; Δ -385) | 0 (0.000%; Δ -385) | 0 (0.000%; Δ -385) |

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
| sonar-xml / normal / no-libraries | Baseline | SUCCESS | VALID | +0 | 1 | 5878.0 / 6008.0 / 6089.0 | +0.0 | DISABLED | 0.0 | 0 | true |
| sonar-xml / normal / no-libraries | Source paths | SUCCESS | VALID | +0 | 1 | 5954.0 / 6035.0 / 6143.0 | +27.0 | DISABLED | 0.0 | 0 | true |
| sonar-xml / normal / no-libraries | Bytecode | SUCCESS | VALID | +0 | 1 | 6191.0 / 6322.0 / 6570.0 | +314.0 | FAILED | 357.0 | 3 | true |
| sonar-xml / normal / no-libraries | Combined | SUCCESS | VALID | +0 | 1 | 5995.0 / 6173.0 / 6271.0 | +165.0 | FAILED | 343.0 | 3 | true |
| sonar-xml / file-by-file / no-libraries | Baseline | SUCCESS | VALID | +0 | 1 | 5997.0 / 6115.0 / 6141.0 | +0.0 | DISABLED | 0.0 | 0 | true |
| sonar-xml / file-by-file / no-libraries | Source paths | SUCCESS | VALID | -165 | 1 | 6482.0 / 6563.0 / 6616.0 | +448.0 | DISABLED | 0.0 | 0 | true |
| sonar-xml / file-by-file / no-libraries | Bytecode | SUCCESS | VALID | -9 | 1 | 6435.0 / 6473.0 / 6500.0 | +358.0 | FAILED | 346.0 | 3 | true |
| sonar-xml / file-by-file / no-libraries | Combined | SUCCESS | VALID | -165 | 1 | 6818.0 / 6827.0 / 6910.0 | +712.0 | FAILED | 335.0 | 3 | true |
| sonar-xml / normal / dependencies | Baseline | SUCCESS | VALID | +0 | 5 | 5975.0 / 6019.0 / 6446.0 | +0.0 | DISABLED | 0.0 | 0 | true |
| sonar-xml / normal / dependencies | Source paths | SUCCESS | VALID | +0 | 5 | 6024.0 / 6150.0 / 6151.0 | +131.0 | DISABLED | 0.0 | 0 | true |
| sonar-xml / normal / dependencies | Bytecode | SUCCESS | VALID | +0 | 5 | 6165.0 / 6291.0 / 6688.0 | +272.0 | SUCCESS | 411.0 | 75 | true |
| sonar-xml / normal / dependencies | Combined | SUCCESS | VALID | +0 | 5 | 6189.0 / 6234.0 / 6254.0 | +215.0 | SUCCESS | 420.0 | 75 | true |
| sonar-xml / file-by-file / dependencies | Baseline | SUCCESS | VALID | +0 | 3 | 6316.0 / 6403.0 / 6423.0 | +0.0 | DISABLED | 0.0 | 0 | true |
| sonar-xml / file-by-file / dependencies | Source paths | SUCCESS | VALID | -385 | 5 | 6915.0 / 6915.0 / 7339.0 | +512.0 | DISABLED | 0.0 | 0 | true |
| sonar-xml / file-by-file / dependencies | Bytecode | SUCCESS | VALID | -385 | 5 | 6821.0 / 6846.0 / 6970.0 | +443.0 | SUCCESS | 412.0 | 75 | true |
| sonar-xml / file-by-file / dependencies | Combined | SUCCESS | VALID | -385 | 5 | 7164.0 / 7188.0 / 7370.0 | +785.0 | SUCCESS | 404.0 | 75 | true |

### JavaSensor phases

The remaining analysis phase is each sample's JavaSensor time minus compilation time. It includes parsing, rule execution, and enabled semantic observations. Invalid or missing timers give N/A; phase medians need not sum to the overall median.

| Dataset | Mode | Compilation min / median / max (ms) | Analysis excluding compilation min / median / max (ms) |
|---|---|---:|---:|
| sonar-xml / normal / no-libraries | Baseline | 0.0 / 0.0 / 0.0 | 5878.0 / 6008.0 / 6089.0 |
| sonar-xml / normal / no-libraries | Source paths | 0.0 / 0.0 / 0.0 | 5954.0 / 6035.0 / 6143.0 |
| sonar-xml / normal / no-libraries | Bytecode | 342.0 / 357.0 / 361.0 | 5849.0 / 5965.0 / 6209.0 |
| sonar-xml / normal / no-libraries | Combined | 323.0 / 343.0 / 354.0 | 5672.0 / 5830.0 / 5917.0 |
| sonar-xml / file-by-file / no-libraries | Baseline | 0.0 / 0.0 / 0.0 | 5997.0 / 6115.0 / 6141.0 |
| sonar-xml / file-by-file / no-libraries | Source paths | 0.0 / 0.0 / 0.0 | 6482.0 / 6563.0 / 6616.0 |
| sonar-xml / file-by-file / no-libraries | Bytecode | 329.0 / 346.0 / 349.0 | 6106.0 / 6127.0 / 6151.0 |
| sonar-xml / file-by-file / no-libraries | Combined | 335.0 / 335.0 / 343.0 | 6483.0 / 6492.0 / 6567.0 |
| sonar-xml / normal / dependencies | Baseline | 0.0 / 0.0 / 0.0 | 5975.0 / 6019.0 / 6446.0 |
| sonar-xml / normal / dependencies | Source paths | 0.0 / 0.0 / 0.0 | 6024.0 / 6150.0 / 6151.0 |
| sonar-xml / normal / dependencies | Bytecode | 411.0 / 411.0 / 425.0 | 5754.0 / 5880.0 / 6263.0 |
| sonar-xml / normal / dependencies | Combined | 407.0 / 420.0 / 422.0 | 5782.0 / 5814.0 / 5832.0 |
| sonar-xml / file-by-file / dependencies | Baseline | 0.0 / 0.0 / 0.0 | 6316.0 / 6403.0 / 6423.0 |
| sonar-xml / file-by-file / dependencies | Source paths | 0.0 / 0.0 / 0.0 | 6915.0 / 6915.0 / 7339.0 |
| sonar-xml / file-by-file / dependencies | Bytecode | 412.0 / 412.0 / 428.0 | 6409.0 / 6434.0 / 6542.0 |
| sonar-xml / file-by-file / dependencies | Combined | 398.0 / 404.0 / 406.0 | 6760.0 / 6790.0 / 6964.0 |

## Observed tradeoffs

- **sonar-xml / normal / no-libraries / Source paths**: no reduction in unknown identifiers; JavaSensor median +27.0 ms versus baseline.
- **sonar-xml / normal / no-libraries / Bytecode**: no reduction in unknown identifiers; JavaSensor median +314.0 ms versus baseline; compilation FAILED.
- **sonar-xml / normal / no-libraries / Combined**: no reduction in unknown identifiers; JavaSensor median +165.0 ms versus baseline; compilation FAILED.
- **sonar-xml / file-by-file / no-libraries / Source paths**: 165 fewer unknown identifiers; JavaSensor median +448.0 ms versus baseline.
- **sonar-xml / file-by-file / no-libraries / Bytecode**: 9 fewer unknown identifiers; JavaSensor median +358.0 ms versus baseline; compilation FAILED.
- **sonar-xml / file-by-file / no-libraries / Combined**: 165 fewer unknown identifiers; JavaSensor median +712.0 ms versus baseline; compilation FAILED.
- **sonar-xml / normal / dependencies / Source paths**: no reduction in unknown identifiers; JavaSensor median +131.0 ms versus baseline.
- **sonar-xml / normal / dependencies / Bytecode**: no reduction in unknown identifiers; JavaSensor median +272.0 ms versus baseline; compilation SUCCESS.
- **sonar-xml / normal / dependencies / Combined**: no reduction in unknown identifiers; JavaSensor median +215.0 ms versus baseline; compilation SUCCESS.
- **sonar-xml / file-by-file / dependencies / Source paths**: 385 fewer unknown identifiers; JavaSensor median +512.0 ms versus baseline.
- **sonar-xml / file-by-file / dependencies / Bytecode**: 385 fewer unknown identifiers; JavaSensor median +443.0 ms versus baseline; compilation SUCCESS.
- **sonar-xml / file-by-file / dependencies / Combined**: 385 fewer unknown identifiers; JavaSensor median +785.0 ms versus baseline; compilation SUCCESS.

These observations apply to the recorded source snapshot and environment. No universal winner or statistical significance is inferred.

## Semantic graph diagnostics

Unique symbol/type references reached during bounded traversal, which can expand as names resolve. Cells show the observed count or min–max range across measured repetitions. These are counts, not resolution-coverage percentages.

Recursive graph inspection is disabled by default for feature timing. Optional budgeted diagnostics can still be expensive. The initial unrestricted observer stalled for over seven minutes on a two-file smoke scan; that aborted scan is excluded from results. Identifier counts, per-file coverage, and product findings still cover the full analyzed AST.

**DISABLED** means a zero expansion limit; graph counts are N/A rather than zero. **COMPLETE** means all measured traversals finished. **PARTIAL** means at least one hit the budget. **UNKNOWN** means completeness was not recorded. Location-change interpretations require complete, stable graph observations. Dataset matrices show representative graph counts.

| Dataset | Mode | Traversal | Expansion limit / module | Expansions executed | Resolved symbols | Unknown symbols | Resolved types | Unknown types |
|---|---|---|---:|---:|---:|---:|---:|---:|
| sonar-xml / normal / no-libraries | Baseline | DISABLED | 0 | 0 | N/A | N/A | N/A | N/A |
| sonar-xml / normal / no-libraries | Source paths | DISABLED | 0 | 0 | N/A | N/A | N/A | N/A |
| sonar-xml / normal / no-libraries | Bytecode | DISABLED | 0 | 0 | N/A | N/A | N/A | N/A |
| sonar-xml / normal / no-libraries | Combined | DISABLED | 0 | 0 | N/A | N/A | N/A | N/A |
| sonar-xml / file-by-file / no-libraries | Baseline | DISABLED | 0 | 0 | N/A | N/A | N/A | N/A |
| sonar-xml / file-by-file / no-libraries | Source paths | DISABLED | 0 | 0 | N/A | N/A | N/A | N/A |
| sonar-xml / file-by-file / no-libraries | Bytecode | DISABLED | 0 | 0 | N/A | N/A | N/A | N/A |
| sonar-xml / file-by-file / no-libraries | Combined | DISABLED | 0 | 0 | N/A | N/A | N/A | N/A |
| sonar-xml / normal / dependencies | Baseline | DISABLED | 0 | 0 | N/A | N/A | N/A | N/A |
| sonar-xml / normal / dependencies | Source paths | DISABLED | 0 | 0 | N/A | N/A | N/A | N/A |
| sonar-xml / normal / dependencies | Bytecode | DISABLED | 0 | 0 | N/A | N/A | N/A | N/A |
| sonar-xml / normal / dependencies | Combined | DISABLED | 0 | 0 | N/A | N/A | N/A | N/A |
| sonar-xml / file-by-file / dependencies | Baseline | DISABLED | 0 | 0 | N/A | N/A | N/A | N/A |
| sonar-xml / file-by-file / dependencies | Source paths | DISABLED | 0 | 0 | N/A | N/A | N/A | N/A |
| sonar-xml / file-by-file / dependencies | Bytecode | DISABLED | 0 | 0 | N/A | N/A | N/A | N/A |
| sonar-xml / file-by-file / dependencies | Combined | DISABLED | 0 | 0 | N/A | N/A | N/A | N/A |

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
<summary>sonar-xml / normal / no-libraries</summary>

Production files use the normal parser batch size; no external dependency JARs are supplied

## Summary

| Metric | Baseline | Source paths | Bytecode | Combined |
|---|---:|---:|---:|---:|
| Source paths enabled | NO | YES | NO | YES |
| Internal compilation enabled | NO | NO | YES | YES |
| Scan status | SUCCESS | SUCCESS | SUCCESS | SUCCESS |
| Java files analyzed | 69 | 69 | 69 | 69 |
| Identifiers (total) | 5608 | 5608 | 5608 | 5608 |
| Known identifiers | 4171 | 4171 | 4171 | 4171 |
| Unknown identifiers | 1437 | 1437 | 1437 | 1437 |
| Unknown identifiers (%) | 25.624% | 25.624% | 25.624% | 25.624% |
| Semantic graph resolved symbols | N/A | N/A | N/A | N/A |
| Semantic graph unknown symbols | N/A | N/A | N/A | N/A |
| Semantic graph resolved types | N/A | N/A | N/A | N/A |
| Semantic graph unknown types | N/A | N/A | N/A | N/A |
| Semantic graph traversal | DISABLED | DISABLED | DISABLED | DISABLED |
| Graph expansion limit per module | 0 | 0 | 0 | 0 |
| Graph expansions executed (representative sample) | 0 | 0 | 0 | 0 |
| Files with no unknown identifiers | 3 / 69 | 3 / 69 | 3 / 69 | 3 / 69 |
| Findings | 1 | 1 | 1 | 1 |
| Median Maven/server time (ms) | 16074.0 | 16085.0 | 17588.0 | 17872.0 |
| Median JavaSensor time, including compilation (ms) | 6008.0 | 6035.0 | 6322.0 | 6173.0 |
| Compilation outcome | DISABLED | DISABLED | FAILED | FAILED |
| Median internal compilation time (ms) | 0.0 | 0.0 | 357.0 | 343.0 |
| Compilation source files | 0 | 0 | 69 | 69 |
| Generated class files before cleanup | 0 | 0 | 3 | 3 |
| Temporary bytecode cleaned up | YES | YES | YES | YES |
| Source characters analyzed | 182211 | 182211 | 182211 | 182211 |
| Undefined-type errors | 390 | 390 | 390 | 390 |
| Source characters with parse errors | N/A | N/A | N/A | N/A |
| Source characters with analysis exceptions | N/A | N/A | N/A | N/A |

### Feature comparisons

Changes are the first named mode minus the second.

| Comparison | Status | Unknown count change | Unknown % change (pp) | Files improved / unchanged / regressed | Shared findings | Second mode only | First mode only | Retention |
|---|---|---:|---:|---:|---:|---:|---:|---:|
| Source paths versus Baseline | VALID | +0 | +0.000 | 0 / 69 / 0 | 1 | 0 | 0 | 100.000% |
| Bytecode versus Baseline | VALID | +0 | +0.000 | 0 / 69 / 0 | 1 | 0 | 0 | 100.000% |
| Combined versus Baseline | VALID | +0 | +0.000 | 0 / 69 / 0 | 1 | 0 | 0 | 100.000% |
| Combined versus Source paths | VALID | +0 | +0.000 | 0 / 69 / 0 | 1 | 0 | 0 | 100.000% |
| Combined versus Bytecode | VALID | +0 | +0.000 | 0 / 69 / 0 | 1 | 0 | 0 | 100.000% |

## Unknown occurrences by AST context

Context is the identifier's AST parent kind. Modes without occurrence details are N/A.

| AST context | Baseline | Source paths | Bytecode | Combined |
|---|---:|---:|---:|---:|
| ANNOTATION | 83 | 83 | 83 | 83 |
| ARGUMENTS | 75 | 75 | 75 | 75 |
| ASSIGNMENT | 110 | 110 | 110 | 110 |
| CLASS | 27 | 27 | 27 | 27 |
| CONDITIONAL_AND | 1 | 1 | 1 | 1 |
| EQUAL_TO | 1 | 1 | 1 | 1 |
| INSTANCE_OF | 1 | 1 | 1 | 1 |
| LIST | 4 | 4 | 4 | 4 |
| MEMBER_SELECT | 705 | 705 | 705 | 705 |
| METHOD_INVOCATION | 156 | 156 | 156 | 156 |
| METHOD_REFERENCE | 8 | 8 | 8 | 8 |
| NEW_CLASS | 14 | 14 | 14 | 14 |
| PARAMETERIZED_TYPE | 14 | 14 | 14 | 14 |
| PLUS | 2 | 2 | 2 | 2 |
| TYPE_ARGUMENTS | 12 | 12 | 12 | 12 |
| TYPE_CAST | 12 | 12 | 12 | 12 |
| VARIABLE | 212 | 212 | 212 | 212 |

## Top files contributing unknown identifiers

Top five by the largest unknown count in any mode. Cells show **unknown count (share of project unknowns)**.

| File | Baseline | Source paths | Bytecode | Combined |
|---|---:|---:|---:|---:|
| sonar-xml-plugin/src/main/java/org/sonar/plugins/xml/XmlSensor.java | 139 (9.673%) | 139 (9.673%) | 139 (9.673%) | 139 (9.673%) |
| sonar-xml-plugin/src/main/java/org/sonar/plugins/xml/XmlHighlighting.java | 93 (6.472%) | 93 (6.472%) | 93 (6.472%) | 93 (6.472%) |
| sonar-xml-plugin/src/main/java/org/sonar/plugins/xml/checks/maven/DisallowedDependenciesCheck.java | 70 (4.871%) | 70 (4.871%) | 70 (4.871%) | 70 (4.871%) |
| sonar-xml-plugin/src/main/java/org/sonar/plugins/xml/LineCounter.java | 62 (4.315%) | 62 (4.315%) | 62 (4.315%) | 62 (4.315%) |
| sonar-xml-plugin/src/main/java/org/sonar/plugins/xml/checks/security/HardcodedCredentialsCheck.java | 57 (3.967%) | 57 (3.967%) | 57 (3.967%) | 57 (3.967%) |

## Semantics per file

Each cell shows **total / known / unknown (unknown %)**. Missing semantic coverage is N/A.

| File | Baseline | Source paths | Bytecode | Combined |
|---|---:|---:|---:|---:|
| sonar-xml-plugin/src/main/java/org/sonar/plugins/xml/LineCounter.java | 213 / 151 / 62 (29.108%) | 213 / 151 / 62 (29.108%) | 213 / 151 / 62 (29.108%) | 213 / 151 / 62 (29.108%) |
| sonar-xml-plugin/src/main/java/org/sonar/plugins/xml/Utils.java | 33 / 23 / 10 (30.303%) | 33 / 23 / 10 (30.303%) | 33 / 23 / 10 (30.303%) | 33 / 23 / 10 (30.303%) |
| sonar-xml-plugin/src/main/java/org/sonar/plugins/xml/Xml.java | 82 / 72 / 10 (12.195%) | 82 / 72 / 10 (12.195%) | 82 / 72 / 10 (12.195%) | 82 / 72 / 10 (12.195%) |
| sonar-xml-plugin/src/main/java/org/sonar/plugins/xml/XmlHighlighting.java | 252 / 159 / 93 (36.905%) | 252 / 159 / 93 (36.905%) | 252 / 159 / 93 (36.905%) | 252 / 159 / 93 (36.905%) |
| sonar-xml-plugin/src/main/java/org/sonar/plugins/xml/XmlPlugin.java | 31 / 17 / 14 (45.161%) | 31 / 17 / 14 (45.161%) | 31 / 17 / 14 (45.161%) | 31 / 17 / 14 (45.161%) |
| sonar-xml-plugin/src/main/java/org/sonar/plugins/xml/XmlRulesDefinition.java | 50 / 35 / 15 (30.000%) | 50 / 35 / 15 (30.000%) | 50 / 35 / 15 (30.000%) | 50 / 35 / 15 (30.000%) |
| sonar-xml-plugin/src/main/java/org/sonar/plugins/xml/XmlSensor.java | 364 / 225 / 139 (38.187%) | 364 / 225 / 139 (38.187%) | 364 / 225 / 139 (38.187%) | 364 / 225 / 139 (38.187%) |
| sonar-xml-plugin/src/main/java/org/sonar/plugins/xml/XmlSonarWayProfile.java | 53 / 35 / 18 (33.962%) | 53 / 35 / 18 (33.962%) | 53 / 35 / 18 (33.962%) | 53 / 35 / 18 (33.962%) |
| sonar-xml-plugin/src/main/java/org/sonar/plugins/xml/api/XmlProfileRegistrar.java | 15 / 12 / 3 (20.000%) | 15 / 12 / 3 (20.000%) | 15 / 12 / 3 (20.000%) | 15 / 12 / 3 (20.000%) |
| sonar-xml-plugin/src/main/java/org/sonar/plugins/xml/api/package-info.java | 8 / 7 / 1 (12.500%) | 8 / 7 / 1 (12.500%) | 8 / 7 / 1 (12.500%) | 8 / 7 / 1 (12.500%) |
| sonar-xml-plugin/src/main/java/org/sonar/plugins/xml/checks/CharBeforePrologCheck.java | 33 / 14 / 19 (57.576%) | 33 / 14 / 19 (57.576%) | 33 / 14 / 19 (57.576%) | 33 / 14 / 19 (57.576%) |
| sonar-xml-plugin/src/main/java/org/sonar/plugins/xml/checks/CheckList.java | 51 / 51 / 0 (0.000%) | 51 / 51 / 0 (0.000%) | 51 / 51 / 0 (0.000%) | 51 / 51 / 0 (0.000%) |
| sonar-xml-plugin/src/main/java/org/sonar/plugins/xml/checks/CommentContainsPatternChecker.java | 90 / 66 / 24 (26.667%) | 90 / 66 / 24 (26.667%) | 90 / 66 / 24 (26.667%) | 90 / 66 / 24 (26.667%) |
| sonar-xml-plugin/src/main/java/org/sonar/plugins/xml/checks/CommentedOutCodeCheck.java | 209 / 177 / 32 (15.311%) | 209 / 177 / 32 (15.311%) | 209 / 177 / 32 (15.311%) | 209 / 177 / 32 (15.311%) |
| sonar-xml-plugin/src/main/java/org/sonar/plugins/xml/checks/FixmeCommentCheck.java | 21 / 19 / 2 (9.524%) | 21 / 19 / 2 (9.524%) | 21 / 19 / 2 (9.524%) | 21 / 19 / 2 (9.524%) |
| sonar-xml-plugin/src/main/java/org/sonar/plugins/xml/checks/IndentationCheck.java | 282 / 251 / 31 (10.993%) | 282 / 251 / 31 (10.993%) | 282 / 251 / 31 (10.993%) | 282 / 251 / 31 (10.993%) |
| sonar-xml-plugin/src/main/java/org/sonar/plugins/xml/checks/LineLengthCheck.java | 75 / 62 / 13 (17.333%) | 75 / 62 / 13 (17.333%) | 75 / 62 / 13 (17.333%) | 75 / 62 / 13 (17.333%) |
| sonar-xml-plugin/src/main/java/org/sonar/plugins/xml/checks/NewlineCheck.java | 217 / 176 / 41 (18.894%) | 217 / 176 / 41 (18.894%) | 217 / 176 / 41 (18.894%) | 217 / 176 / 41 (18.894%) |
| sonar-xml-plugin/src/main/java/org/sonar/plugins/xml/checks/ParsingErrorCheck.java | 17 / 13 / 4 (23.529%) | 17 / 13 / 4 (23.529%) | 17 / 13 / 4 (23.529%) | 17 / 13 / 4 (23.529%) |
| sonar-xml-plugin/src/main/java/org/sonar/plugins/xml/checks/TabCharacterCheck.java | 115 / 90 / 25 (21.739%) | 115 / 90 / 25 (21.739%) | 115 / 90 / 25 (21.739%) | 115 / 90 / 25 (21.739%) |
| sonar-xml-plugin/src/main/java/org/sonar/plugins/xml/checks/TodoCommentCheck.java | 21 / 19 / 2 (9.524%) | 21 / 19 / 2 (9.524%) | 21 / 19 / 2 (9.524%) | 21 / 19 / 2 (9.524%) |
| sonar-xml-plugin/src/main/java/org/sonar/plugins/xml/checks/XPathCheck.java | 285 / 246 / 39 (13.684%) | 285 / 246 / 39 (13.684%) | 285 / 246 / 39 (13.684%) | 285 / 246 / 39 (13.684%) |
| sonar-xml-plugin/src/main/java/org/sonar/plugins/xml/checks/ejb/DefaultInterceptorsLocationCheck.java | 32 / 16 / 16 (50.000%) | 32 / 16 / 16 (50.000%) | 32 / 16 / 16 (50.000%) | 32 / 16 / 16 (50.000%) |
| sonar-xml-plugin/src/main/java/org/sonar/plugins/xml/checks/ejb/InterceptorExclusionsCheck.java | 40 / 23 / 17 (42.500%) | 40 / 23 / 17 (42.500%) | 40 / 23 / 17 (42.500%) | 40 / 23 / 17 (42.500%) |
| sonar-xml-plugin/src/main/java/org/sonar/plugins/xml/checks/ejb/package-info.java | 9 / 8 / 1 (11.111%) | 9 / 8 / 1 (11.111%) | 9 / 8 / 1 (11.111%) | 9 / 8 / 1 (11.111%) |
| sonar-xml-plugin/src/main/java/org/sonar/plugins/xml/checks/hibernate/DatabaseSchemaUpdateCheck.java | 48 / 36 / 12 (25.000%) | 48 / 36 / 12 (25.000%) | 48 / 36 / 12 (25.000%) | 48 / 36 / 12 (25.000%) |
| sonar-xml-plugin/src/main/java/org/sonar/plugins/xml/checks/hibernate/package-info.java | 9 / 8 / 1 (11.111%) | 9 / 8 / 1 (11.111%) | 9 / 8 / 1 (11.111%) | 9 / 8 / 1 (11.111%) |
| sonar-xml-plugin/src/main/java/org/sonar/plugins/xml/checks/maven/ArtifactIdNamingConventionCheck.java | 79 / 62 / 17 (21.519%) | 79 / 62 / 17 (21.519%) | 79 / 62 / 17 (21.519%) | 79 / 62 / 17 (21.519%) |
| sonar-xml-plugin/src/main/java/org/sonar/plugins/xml/checks/maven/DependencyWithSystemScopeCheck.java | 87 / 65 / 22 (25.287%) | 87 / 65 / 22 (25.287%) | 87 / 65 / 22 (25.287%) | 87 / 65 / 22 (25.287%) |
| sonar-xml-plugin/src/main/java/org/sonar/plugins/xml/checks/maven/DeprecatedPomPropertiesCheck.java | 111 / 68 / 43 (38.739%) | 111 / 68 / 43 (38.739%) | 111 / 68 / 43 (38.739%) | 111 / 68 / 43 (38.739%) |
| sonar-xml-plugin/src/main/java/org/sonar/plugins/xml/checks/maven/DisallowedDependenciesCheck.java | 178 / 108 / 70 (39.326%) | 178 / 108 / 70 (39.326%) | 178 / 108 / 70 (39.326%) | 178 / 108 / 70 (39.326%) |
| sonar-xml-plugin/src/main/java/org/sonar/plugins/xml/checks/maven/GroupIdNamingConventionCheck.java | 79 / 62 / 17 (21.519%) | 79 / 62 / 17 (21.519%) | 79 / 62 / 17 (21.519%) | 79 / 62 / 17 (21.519%) |
| sonar-xml-plugin/src/main/java/org/sonar/plugins/xml/checks/maven/PomElementOrderCheck.java | 146 / 122 / 24 (16.438%) | 146 / 122 / 24 (16.438%) | 146 / 122 / 24 (16.438%) | 146 / 122 / 24 (16.438%) |
| sonar-xml-plugin/src/main/java/org/sonar/plugins/xml/checks/maven/helpers/MavenDependencyMatcher.java | 100 / 100 / 0 (0.000%) | 100 / 100 / 0 (0.000%) | 100 / 100 / 0 (0.000%) | 100 / 100 / 0 (0.000%) |
| sonar-xml-plugin/src/main/java/org/sonar/plugins/xml/checks/maven/helpers/PatternMatcher.java | 43 / 43 / 0 (0.000%) | 43 / 43 / 0 (0.000%) | 43 / 43 / 0 (0.000%) | 43 / 43 / 0 (0.000%) |
| sonar-xml-plugin/src/main/java/org/sonar/plugins/xml/checks/maven/helpers/RangedVersionMatcher.java | 168 / 164 / 4 (2.381%) | 168 / 164 / 4 (2.381%) | 168 / 164 / 4 (2.381%) | 168 / 164 / 4 (2.381%) |
| sonar-xml-plugin/src/main/java/org/sonar/plugins/xml/checks/maven/helpers/StringMatcher.java | 39 / 38 / 1 (2.564%) | 39 / 38 / 1 (2.564%) | 39 / 38 / 1 (2.564%) | 39 / 38 / 1 (2.564%) |
| sonar-xml-plugin/src/main/java/org/sonar/plugins/xml/checks/maven/helpers/package-info.java | 10 / 9 / 1 (10.000%) | 10 / 9 / 1 (10.000%) | 10 / 9 / 1 (10.000%) | 10 / 9 / 1 (10.000%) |
| sonar-xml-plugin/src/main/java/org/sonar/plugins/xml/checks/maven/package-info.java | 9 / 8 / 1 (11.111%) | 9 / 8 / 1 (11.111%) | 9 / 8 / 1 (11.111%) | 9 / 8 / 1 (11.111%) |
| sonar-xml-plugin/src/main/java/org/sonar/plugins/xml/checks/package-info.java | 8 / 7 / 1 (12.500%) | 8 / 7 / 1 (12.500%) | 8 / 7 / 1 (12.500%) | 8 / 7 / 1 (12.500%) |
| sonar-xml-plugin/src/main/java/org/sonar/plugins/xml/checks/security/HardcodedCredentialsCheck.java | 446 / 389 / 57 (12.780%) | 446 / 389 / 57 (12.780%) | 446 / 389 / 57 (12.780%) | 446 / 389 / 57 (12.780%) |
| sonar-xml-plugin/src/main/java/org/sonar/plugins/xml/checks/security/android/AbstractAndroidManifestCheck.java | 20 / 17 / 3 (15.000%) | 20 / 17 / 3 (15.000%) | 20 / 17 / 3 (15.000%) | 20 / 17 / 3 (15.000%) |
| sonar-xml-plugin/src/main/java/org/sonar/plugins/xml/checks/security/android/AndroidApplicationBackupCheck.java | 80 / 58 / 22 (27.500%) | 80 / 58 / 22 (27.500%) | 80 / 58 / 22 (27.500%) | 80 / 58 / 22 (27.500%) |
| sonar-xml-plugin/src/main/java/org/sonar/plugins/xml/checks/security/android/AndroidClearTextCheck.java | 74 / 43 / 31 (41.892%) | 74 / 43 / 31 (41.892%) | 74 / 43 / 31 (41.892%) | 74 / 43 / 31 (41.892%) |
| sonar-xml-plugin/src/main/java/org/sonar/plugins/xml/checks/security/android/AndroidComponentWithIntentFilterExportedCheck.java | 38 / 19 / 19 (50.000%) | 38 / 19 / 19 (50.000%) | 38 / 19 / 19 (50.000%) | 38 / 19 / 19 (50.000%) |
| sonar-xml-plugin/src/main/java/org/sonar/plugins/xml/checks/security/android/AndroidCustomPermissionCheck.java | 52 / 27 / 25 (48.077%) | 52 / 27 / 25 (48.077%) | 52 / 27 / 25 (48.077%) | 52 / 27 / 25 (48.077%) |
| sonar-xml-plugin/src/main/java/org/sonar/plugins/xml/checks/security/android/AndroidExportedContentPermissionsCheck.java | 38 / 19 / 19 (50.000%) | 38 / 19 / 19 (50.000%) | 38 / 19 / 19 (50.000%) | 38 / 19 / 19 (50.000%) |
| sonar-xml-plugin/src/main/java/org/sonar/plugins/xml/checks/security/android/AndroidPermissionsCheck.java | 106 / 97 / 9 (8.491%) | 106 / 97 / 9 (8.491%) | 106 / 97 / 9 (8.491%) | 106 / 97 / 9 (8.491%) |
| sonar-xml-plugin/src/main/java/org/sonar/plugins/xml/checks/security/android/AndroidProviderPermissionCheck.java | 66 / 19 / 47 (71.212%) | 66 / 19 / 47 (71.212%) | 66 / 19 / 47 (71.212%) | 66 / 19 / 47 (71.212%) |
| sonar-xml-plugin/src/main/java/org/sonar/plugins/xml/checks/security/android/AndroidReceivingIntentsCheck.java | 38 / 19 / 19 (50.000%) | 38 / 19 / 19 (50.000%) | 38 / 19 / 19 (50.000%) | 38 / 19 / 19 (50.000%) |
| sonar-xml-plugin/src/main/java/org/sonar/plugins/xml/checks/security/android/DebugFeatureCheck.java | 59 / 27 / 32 (54.237%) | 59 / 27 / 32 (54.237%) | 59 / 27 / 32 (54.237%) | 59 / 27 / 32 (54.237%) |
| sonar-xml-plugin/src/main/java/org/sonar/plugins/xml/checks/security/android/Utils.java | 23 / 20 / 3 (13.043%) | 23 / 20 / 3 (13.043%) | 23 / 20 / 3 (13.043%) | 23 / 20 / 3 (13.043%) |
| sonar-xml-plugin/src/main/java/org/sonar/plugins/xml/checks/security/android/package-info.java | 10 / 9 / 1 (10.000%) | 10 / 9 / 1 (10.000%) | 10 / 9 / 1 (10.000%) | 10 / 9 / 1 (10.000%) |
| sonar-xml-plugin/src/main/java/org/sonar/plugins/xml/checks/security/package-info.java | 9 / 8 / 1 (11.111%) | 9 / 8 / 1 (11.111%) | 9 / 8 / 1 (11.111%) | 9 / 8 / 1 (11.111%) |
| sonar-xml-plugin/src/main/java/org/sonar/plugins/xml/checks/security/web/BaseWebCheck.java | 73 / 65 / 8 (10.959%) | 73 / 65 / 8 (10.959%) | 73 / 65 / 8 (10.959%) | 73 / 65 / 8 (10.959%) |
| sonar-xml-plugin/src/main/java/org/sonar/plugins/xml/checks/security/web/BasicAuthenticationCheck.java | 42 / 23 / 19 (45.238%) | 42 / 23 / 19 (45.238%) | 42 / 23 / 19 (45.238%) | 42 / 23 / 19 (45.238%) |
| sonar-xml-plugin/src/main/java/org/sonar/plugins/xml/checks/security/web/CrossOriginResourceSharingCheck.java | 41 / 20 / 21 (51.220%) | 41 / 20 / 21 (51.220%) | 41 / 20 / 21 (51.220%) | 41 / 20 / 21 (51.220%) |
| sonar-xml-plugin/src/main/java/org/sonar/plugins/xml/checks/security/web/HttpOnlyOnCookiesCheck.java | 120 / 78 / 42 (35.000%) | 120 / 78 / 42 (35.000%) | 120 / 78 / 42 (35.000%) | 120 / 78 / 42 (35.000%) |
| sonar-xml-plugin/src/main/java/org/sonar/plugins/xml/checks/security/web/MimeNosniffCheck.java | 52 / 29 / 23 (44.231%) | 52 / 29 / 23 (44.231%) | 52 / 29 / 23 (44.231%) | 52 / 29 / 23 (44.231%) |
| sonar-xml-plugin/src/main/java/org/sonar/plugins/xml/checks/security/web/PasswordsInWebConfigCheck.java | 48 / 34 / 14 (29.167%) | 48 / 34 / 14 (29.167%) | 48 / 34 / 14 (29.167%) | 48 / 34 / 14 (29.167%) |
| sonar-xml-plugin/src/main/java/org/sonar/plugins/xml/checks/security/web/ValidationFiltersCheck.java | 78 / 42 / 36 (46.154%) | 78 / 42 / 36 (46.154%) | 78 / 42 / 36 (46.154%) | 78 / 42 / 36 (46.154%) |
| sonar-xml-plugin/src/main/java/org/sonar/plugins/xml/checks/security/web/package-info.java | 10 / 9 / 1 (10.000%) | 10 / 9 / 1 (10.000%) | 10 / 9 / 1 (10.000%) | 10 / 9 / 1 (10.000%) |
| sonar-xml-plugin/src/main/java/org/sonar/plugins/xml/checks/spring/DefaultMessageListenerContainerCheck.java | 99 / 68 / 31 (31.313%) | 99 / 68 / 31 (31.313%) | 99 / 68 / 31 (31.313%) | 99 / 68 / 31 (31.313%) |
| sonar-xml-plugin/src/main/java/org/sonar/plugins/xml/checks/spring/SingleConnectionFactoryCheck.java | 81 / 50 / 31 (38.272%) | 81 / 50 / 31 (38.272%) | 81 / 50 / 31 (38.272%) | 81 / 50 / 31 (38.272%) |
| sonar-xml-plugin/src/main/java/org/sonar/plugins/xml/checks/spring/package-info.java | 7 / 6 / 1 (14.286%) | 7 / 6 / 1 (14.286%) | 7 / 6 / 1 (14.286%) | 7 / 6 / 1 (14.286%) |
| sonar-xml-plugin/src/main/java/org/sonar/plugins/xml/checks/struts/ActionNumberCheck.java | 73 / 42 / 31 (42.466%) | 73 / 42 / 31 (42.466%) | 73 / 42 / 31 (42.466%) | 73 / 42 / 31 (42.466%) |
| sonar-xml-plugin/src/main/java/org/sonar/plugins/xml/checks/struts/FormNameDuplicationCheck.java | 97 / 53 / 44 (45.361%) | 97 / 53 / 44 (45.361%) | 97 / 53 / 44 (45.361%) | 97 / 53 / 44 (45.361%) |
| sonar-xml-plugin/src/main/java/org/sonar/plugins/xml/checks/struts/package-info.java | 9 / 8 / 1 (11.111%) | 9 / 8 / 1 (11.111%) | 9 / 8 / 1 (11.111%) | 9 / 8 / 1 (11.111%) |
| sonar-xml-plugin/src/main/java/org/sonar/plugins/xml/package-info.java | 7 / 6 / 1 (14.286%) | 7 / 6 / 1 (14.286%) | 7 / 6 / 1 (14.286%) | 7 / 6 / 1 (14.286%) |

## Rules with findings

| Rule | Baseline | Source paths | Bytecode | Combined |
|---|---:|---:|---:|---:|
| java:S6204 | 1 | 1 | 1 | 1 |

598 configured rules had no findings in any mode and are omitted.

## Detailed differences

<details>
<summary>Source paths versus Baseline</summary>

### Largest semantic improvements

Top five by absolute change in unknown percentage.

None.

### Largest semantic regressions

Top five by absolute change in unknown percentage.

None.

| Unknown occurrences | Count |
|---|---:|
| Still unknown in both modes | 1437 |
| No longer reported unknown | 0 |
| Newly reported unknown | 0 |

### No longer reported unknown

None.


### Newly reported unknown

None.


Semantic graph location differences unavailable: traversal is DISABLED, PARTIAL, or UNKNOWN in at least one compared mode. Primary identifier and finding comparisons remain valid.

### Findings only in Baseline

None.

### Findings only in Source paths

None.

</details>

<details>
<summary>Bytecode versus Baseline</summary>

### Largest semantic improvements

Top five by absolute change in unknown percentage.

None.

### Largest semantic regressions

Top five by absolute change in unknown percentage.

None.

| Unknown occurrences | Count |
|---|---:|
| Still unknown in both modes | 1437 |
| No longer reported unknown | 0 |
| Newly reported unknown | 0 |

### No longer reported unknown

None.


### Newly reported unknown

None.


Semantic graph location differences unavailable: traversal is DISABLED, PARTIAL, or UNKNOWN in at least one compared mode. Primary identifier and finding comparisons remain valid.

### Findings only in Baseline

None.

### Findings only in Bytecode

None.

</details>

<details>
<summary>Combined versus Baseline</summary>

### Largest semantic improvements

Top five by absolute change in unknown percentage.

None.

### Largest semantic regressions

Top five by absolute change in unknown percentage.

None.

| Unknown occurrences | Count |
|---|---:|
| Still unknown in both modes | 1437 |
| No longer reported unknown | 0 |
| Newly reported unknown | 0 |

### No longer reported unknown

None.


### Newly reported unknown

None.


Semantic graph location differences unavailable: traversal is DISABLED, PARTIAL, or UNKNOWN in at least one compared mode. Primary identifier and finding comparisons remain valid.

### Findings only in Baseline

None.

### Findings only in Combined

None.

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
| Still unknown in both modes | 1437 |
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

None.

### Largest semantic regressions

Top five by absolute change in unknown percentage.

None.

| Unknown occurrences | Count |
|---|---:|
| Still unknown in both modes | 1437 |
| No longer reported unknown | 0 |
| Newly reported unknown | 0 |

### No longer reported unknown

None.


### Newly reported unknown

None.


Semantic graph location differences unavailable: traversal is DISABLED, PARTIAL, or UNKNOWN in at least one compared mode. Primary identifier and finding comparisons remain valid.

### Findings only in Bytecode

None.

### Findings only in Combined

None.

</details>


## Measured timings

Warm-ups are excluded. JavaSensor time includes compilation.

| Sample | Mode | Maven/server wall (ms) | JavaSensor (ms) | Compilation (ms) | Compilation outcome | Generated classes |
|---|---|---:|---:|---:|---|---:|
| 1 | Baseline | 15800 | 6008.0 | 0.0 | DISABLED | 0 |
| 1 | Source paths | 16085 | 6035.0 | 0.0 | DISABLED | 0 |
| 1 | Bytecode | 18071 | 6570.0 | 361.0 | FAILED | 3 |
| 1 | Combined | 17872 | 6173.0 | 343.0 | FAILED | 3 |
| 2 | Baseline | 17943 | 6089.0 | 0.0 | DISABLED | 0 |
| 2 | Source paths | 16098 | 6143.0 | 0.0 | DISABLED | 0 |
| 2 | Bytecode | 17588 | 6322.0 | 357.0 | FAILED | 3 |
| 2 | Combined | 18106 | 6271.0 | 354.0 | FAILED | 3 |
| 3 | Baseline | 16074 | 5878.0 | 0.0 | DISABLED | 0 |
| 3 | Source paths | 15608 | 5954.0 | 0.0 | DISABLED | 0 |
| 3 | Bytecode | 15781 | 6191.0 | 342.0 | FAILED | 3 |
| 3 | Combined | 15921 | 5995.0 | 323.0 | FAILED | 3 |

## Run metadata

| Setting | Value |
|---|---|
| Baseline properties | {sonar.java.compileToByteCode=false, sonar.java.experimental.batchModeSizeInKB=500, sonar.java.fileByFile=false, sonar.java.sourcepath=, sonar.java.test.sourcepath=} |
| Baseline warm-up status | SUCCESS |
| Bytecode properties | {sonar.java.compileToByteCode=true, sonar.java.experimental.batchModeSizeInKB=500, sonar.java.fileByFile=false, sonar.java.sourcepath=, sonar.java.test.sourcepath=} |
| Bytecode warm-up status | SUCCESS |
| Combined properties | {sonar.java.compileToByteCode=true, sonar.java.experimental.batchModeSizeInKB=500, sonar.java.fileByFile=false, sonar.java.sourcepath=sonar-xml-plugin/src/main/java, sonar.java.test.sourcepath=} |
| Combined warm-up status | SUCCESS |
| External library count | 0 |
| Graph diagnostic stability | stable |
| Repeated result stability | stable |
| Scenario | Production files use the normal parser batch size; no external dependency JARs are supplied |
| Shared scenario properties | {sonar.java.experimental.batchModeSizeInKB=500, sonar.java.fileByFile=false} |
| Source paths properties | {sonar.java.compileToByteCode=false, sonar.java.experimental.batchModeSizeInKB=500, sonar.java.fileByFile=false, sonar.java.sourcepath=sonar-xml-plugin/src/main/java, sonar.java.test.sourcepath=} |
| Source paths warm-up status | SUCCESS |

</details>

<details>
<summary>sonar-xml / file-by-file / no-libraries</summary>

Each production file is parsed separately; no external dependency JARs are supplied

## Summary

| Metric | Baseline | Source paths | Bytecode | Combined |
|---|---:|---:|---:|---:|
| Source paths enabled | NO | YES | NO | YES |
| Internal compilation enabled | NO | NO | YES | YES |
| Scan status | SUCCESS | SUCCESS | SUCCESS | SUCCESS |
| Java files analyzed | 69 | 69 | 69 | 69 |
| Identifiers (total) | 5608 | 5608 | 5608 | 5608 |
| Known identifiers | 4006 | 4171 | 4015 | 4171 |
| Unknown identifiers | 1602 | 1437 | 1593 | 1437 |
| Unknown identifiers (%) | 28.566% | 25.624% | 28.406% | 25.624% |
| Semantic graph resolved symbols | N/A | N/A | N/A | N/A |
| Semantic graph unknown symbols | N/A | N/A | N/A | N/A |
| Semantic graph resolved types | N/A | N/A | N/A | N/A |
| Semantic graph unknown types | N/A | N/A | N/A | N/A |
| Semantic graph traversal | DISABLED | DISABLED | DISABLED | DISABLED |
| Graph expansion limit per module | 0 | 0 | 0 | 0 |
| Graph expansions executed (representative sample) | 0 | 0 | 0 | 0 |
| Files with no unknown identifiers | 0 / 69 | 3 / 69 | 0 / 69 | 3 / 69 |
| Findings | 1 | 1 | 1 | 1 |
| Median Maven/server time (ms) | 15836.0 | 16219.0 | 17856.0 | 17729.0 |
| Median JavaSensor time, including compilation (ms) | 6115.0 | 6563.0 | 6473.0 | 6827.0 |
| Compilation outcome | DISABLED | DISABLED | FAILED | FAILED |
| Median internal compilation time (ms) | 0.0 | 0.0 | 346.0 | 335.0 |
| Compilation source files | 0 | 0 | 69 | 69 |
| Generated class files before cleanup | 0 | 0 | 3 | 3 |
| Temporary bytecode cleaned up | YES | YES | YES | YES |
| Source characters analyzed | 182211 | 182211 | 182211 | 182211 |
| Undefined-type errors | 504 | 390 | 503 | 390 |
| Source characters with parse errors | N/A | N/A | N/A | N/A |
| Source characters with analysis exceptions | N/A | N/A | N/A | N/A |

### Feature comparisons

Changes are the first named mode minus the second.

| Comparison | Status | Unknown count change | Unknown % change (pp) | Files improved / unchanged / regressed | Shared findings | Second mode only | First mode only | Retention |
|---|---|---:|---:|---:|---:|---:|---:|---:|
| Source paths versus Baseline | VALID | -165 | -2.942 | 36 / 33 / 0 | 1 | 0 | 0 | 100.000% |
| Bytecode versus Baseline | VALID | -9 | -0.160 | 4 / 65 / 0 | 1 | 0 | 0 | 100.000% |
| Combined versus Baseline | VALID | -165 | -2.942 | 36 / 33 / 0 | 1 | 0 | 0 | 100.000% |
| Combined versus Source paths | VALID | +0 | +0.000 | 0 / 69 / 0 | 1 | 0 | 0 | 100.000% |
| Combined versus Bytecode | VALID | -156 | -2.782 | 35 / 34 / 0 | 1 | 0 | 0 | 100.000% |

## Unknown occurrences by AST context

Context is the identifier's AST parent kind. Modes without occurrence details are N/A.

| AST context | Baseline | Source paths | Bytecode | Combined |
|---|---:|---:|---:|---:|
| ANNOTATION | 83 | 83 | 83 | 83 |
| ARGUMENTS | 87 | 75 | 87 | 75 |
| ARRAY_TYPE | 1 | 0 | 1 | 0 |
| ASSIGNMENT | 110 | 110 | 110 | 110 |
| CLASS | 43 | 27 | 43 | 27 |
| CONDITIONAL_AND | 1 | 1 | 1 | 1 |
| EQUAL_TO | 1 | 1 | 1 | 1 |
| INSTANCE_OF | 1 | 1 | 1 | 1 |
| LIST | 6 | 4 | 6 | 4 |
| MEMBER_SELECT | 814 | 705 | 810 | 705 |
| METHOD | 3 | 0 | 2 | 0 |
| METHOD_INVOCATION | 165 | 156 | 165 | 156 |
| METHOD_REFERENCE | 8 | 8 | 8 | 8 |
| NEW_ARRAY | 1 | 0 | 1 | 0 |
| NEW_CLASS | 17 | 14 | 15 | 14 |
| PARAMETERIZED_TYPE | 14 | 14 | 14 | 14 |
| PLUS | 4 | 2 | 4 | 2 |
| TYPE_ARGUMENTS | 12 | 12 | 12 | 12 |
| TYPE_CAST | 12 | 12 | 12 | 12 |
| VARIABLE | 219 | 212 | 217 | 212 |

## Top files contributing unknown identifiers

Top five by the largest unknown count in any mode. Cells show **unknown count (share of project unknowns)**.

| File | Baseline | Source paths | Bytecode | Combined |
|---|---:|---:|---:|---:|
| sonar-xml-plugin/src/main/java/org/sonar/plugins/xml/XmlSensor.java | 157 (9.800%) | 139 (9.673%) | 155 (9.730%) | 139 (9.673%) |
| sonar-xml-plugin/src/main/java/org/sonar/plugins/xml/XmlHighlighting.java | 94 (5.868%) | 93 (6.472%) | 94 (5.901%) | 93 (6.472%) |
| sonar-xml-plugin/src/main/java/org/sonar/plugins/xml/checks/maven/DisallowedDependenciesCheck.java | 74 (4.619%) | 70 (4.871%) | 70 (4.394%) | 70 (4.871%) |
| sonar-xml-plugin/src/main/java/org/sonar/plugins/xml/LineCounter.java | 63 (3.933%) | 62 (4.315%) | 63 (3.955%) | 62 (4.315%) |
| sonar-xml-plugin/src/main/java/org/sonar/plugins/xml/checks/security/HardcodedCredentialsCheck.java | 59 (3.683%) | 57 (3.967%) | 59 (3.704%) | 57 (3.967%) |

## Semantics per file

Each cell shows **total / known / unknown (unknown %)**. Missing semantic coverage is N/A.

| File | Baseline | Source paths | Bytecode | Combined |
|---|---:|---:|---:|---:|
| sonar-xml-plugin/src/main/java/org/sonar/plugins/xml/LineCounter.java | 213 / 150 / 63 (29.577%) | 213 / 151 / 62 (29.108%) | 213 / 150 / 63 (29.577%) | 213 / 151 / 62 (29.108%) |
| sonar-xml-plugin/src/main/java/org/sonar/plugins/xml/Utils.java | 33 / 23 / 10 (30.303%) | 33 / 23 / 10 (30.303%) | 33 / 23 / 10 (30.303%) | 33 / 23 / 10 (30.303%) |
| sonar-xml-plugin/src/main/java/org/sonar/plugins/xml/Xml.java | 82 / 70 / 12 (14.634%) | 82 / 72 / 10 (12.195%) | 82 / 70 / 12 (14.634%) | 82 / 72 / 10 (12.195%) |
| sonar-xml-plugin/src/main/java/org/sonar/plugins/xml/XmlHighlighting.java | 252 / 158 / 94 (37.302%) | 252 / 159 / 93 (36.905%) | 252 / 158 / 94 (37.302%) | 252 / 159 / 93 (36.905%) |
| sonar-xml-plugin/src/main/java/org/sonar/plugins/xml/XmlPlugin.java | 31 / 13 / 18 (58.065%) | 31 / 17 / 14 (45.161%) | 31 / 13 / 18 (58.065%) | 31 / 17 / 14 (45.161%) |
| sonar-xml-plugin/src/main/java/org/sonar/plugins/xml/XmlRulesDefinition.java | 50 / 23 / 27 (54.000%) | 50 / 35 / 15 (30.000%) | 50 / 25 / 25 (50.000%) | 50 / 35 / 15 (30.000%) |
| sonar-xml-plugin/src/main/java/org/sonar/plugins/xml/XmlSensor.java | 364 / 207 / 157 (43.132%) | 364 / 225 / 139 (38.187%) | 364 / 209 / 155 (42.582%) | 364 / 225 / 139 (38.187%) |
| sonar-xml-plugin/src/main/java/org/sonar/plugins/xml/XmlSonarWayProfile.java | 53 / 23 / 30 (56.604%) | 53 / 35 / 18 (33.962%) | 53 / 23 / 30 (56.604%) | 53 / 35 / 18 (33.962%) |
| sonar-xml-plugin/src/main/java/org/sonar/plugins/xml/api/XmlProfileRegistrar.java | 15 / 12 / 3 (20.000%) | 15 / 12 / 3 (20.000%) | 15 / 12 / 3 (20.000%) | 15 / 12 / 3 (20.000%) |
| sonar-xml-plugin/src/main/java/org/sonar/plugins/xml/api/package-info.java | 8 / 7 / 1 (12.500%) | 8 / 7 / 1 (12.500%) | 8 / 7 / 1 (12.500%) | 8 / 7 / 1 (12.500%) |
| sonar-xml-plugin/src/main/java/org/sonar/plugins/xml/checks/CharBeforePrologCheck.java | 33 / 14 / 19 (57.576%) | 33 / 14 / 19 (57.576%) | 33 / 14 / 19 (57.576%) | 33 / 14 / 19 (57.576%) |
| sonar-xml-plugin/src/main/java/org/sonar/plugins/xml/checks/CheckList.java | 51 / 12 / 39 (76.471%) | 51 / 51 / 0 (0.000%) | 51 / 12 / 39 (76.471%) | 51 / 51 / 0 (0.000%) |
| sonar-xml-plugin/src/main/java/org/sonar/plugins/xml/checks/CommentContainsPatternChecker.java | 90 / 66 / 24 (26.667%) | 90 / 66 / 24 (26.667%) | 90 / 66 / 24 (26.667%) | 90 / 66 / 24 (26.667%) |
| sonar-xml-plugin/src/main/java/org/sonar/plugins/xml/checks/CommentedOutCodeCheck.java | 209 / 177 / 32 (15.311%) | 209 / 177 / 32 (15.311%) | 209 / 177 / 32 (15.311%) | 209 / 177 / 32 (15.311%) |
| sonar-xml-plugin/src/main/java/org/sonar/plugins/xml/checks/FixmeCommentCheck.java | 21 / 17 / 4 (19.048%) | 21 / 19 / 2 (9.524%) | 21 / 17 / 4 (19.048%) | 21 / 19 / 2 (9.524%) |
| sonar-xml-plugin/src/main/java/org/sonar/plugins/xml/checks/IndentationCheck.java | 282 / 248 / 34 (12.057%) | 282 / 251 / 31 (10.993%) | 282 / 248 / 34 (12.057%) | 282 / 251 / 31 (10.993%) |
| sonar-xml-plugin/src/main/java/org/sonar/plugins/xml/checks/LineLengthCheck.java | 75 / 60 / 15 (20.000%) | 75 / 62 / 13 (17.333%) | 75 / 60 / 15 (20.000%) | 75 / 62 / 13 (17.333%) |
| sonar-xml-plugin/src/main/java/org/sonar/plugins/xml/checks/NewlineCheck.java | 217 / 174 / 43 (19.816%) | 217 / 176 / 41 (18.894%) | 217 / 174 / 43 (19.816%) | 217 / 176 / 41 (18.894%) |
| sonar-xml-plugin/src/main/java/org/sonar/plugins/xml/checks/ParsingErrorCheck.java | 17 / 13 / 4 (23.529%) | 17 / 13 / 4 (23.529%) | 17 / 13 / 4 (23.529%) | 17 / 13 / 4 (23.529%) |
| sonar-xml-plugin/src/main/java/org/sonar/plugins/xml/checks/TabCharacterCheck.java | 115 / 86 / 29 (25.217%) | 115 / 90 / 25 (21.739%) | 115 / 86 / 29 (25.217%) | 115 / 90 / 25 (21.739%) |
| sonar-xml-plugin/src/main/java/org/sonar/plugins/xml/checks/TodoCommentCheck.java | 21 / 17 / 4 (19.048%) | 21 / 19 / 2 (9.524%) | 21 / 17 / 4 (19.048%) | 21 / 19 / 2 (9.524%) |
| sonar-xml-plugin/src/main/java/org/sonar/plugins/xml/checks/XPathCheck.java | 285 / 246 / 39 (13.684%) | 285 / 246 / 39 (13.684%) | 285 / 246 / 39 (13.684%) | 285 / 246 / 39 (13.684%) |
| sonar-xml-plugin/src/main/java/org/sonar/plugins/xml/checks/ejb/DefaultInterceptorsLocationCheck.java | 32 / 16 / 16 (50.000%) | 32 / 16 / 16 (50.000%) | 32 / 16 / 16 (50.000%) | 32 / 16 / 16 (50.000%) |
| sonar-xml-plugin/src/main/java/org/sonar/plugins/xml/checks/ejb/InterceptorExclusionsCheck.java | 40 / 23 / 17 (42.500%) | 40 / 23 / 17 (42.500%) | 40 / 23 / 17 (42.500%) | 40 / 23 / 17 (42.500%) |
| sonar-xml-plugin/src/main/java/org/sonar/plugins/xml/checks/ejb/package-info.java | 9 / 8 / 1 (11.111%) | 9 / 8 / 1 (11.111%) | 9 / 8 / 1 (11.111%) | 9 / 8 / 1 (11.111%) |
| sonar-xml-plugin/src/main/java/org/sonar/plugins/xml/checks/hibernate/DatabaseSchemaUpdateCheck.java | 48 / 36 / 12 (25.000%) | 48 / 36 / 12 (25.000%) | 48 / 36 / 12 (25.000%) | 48 / 36 / 12 (25.000%) |
| sonar-xml-plugin/src/main/java/org/sonar/plugins/xml/checks/hibernate/package-info.java | 9 / 8 / 1 (11.111%) | 9 / 8 / 1 (11.111%) | 9 / 8 / 1 (11.111%) | 9 / 8 / 1 (11.111%) |
| sonar-xml-plugin/src/main/java/org/sonar/plugins/xml/checks/maven/ArtifactIdNamingConventionCheck.java | 79 / 62 / 17 (21.519%) | 79 / 62 / 17 (21.519%) | 79 / 62 / 17 (21.519%) | 79 / 62 / 17 (21.519%) |
| sonar-xml-plugin/src/main/java/org/sonar/plugins/xml/checks/maven/DependencyWithSystemScopeCheck.java | 87 / 65 / 22 (25.287%) | 87 / 65 / 22 (25.287%) | 87 / 65 / 22 (25.287%) | 87 / 65 / 22 (25.287%) |
| sonar-xml-plugin/src/main/java/org/sonar/plugins/xml/checks/maven/DeprecatedPomPropertiesCheck.java | 111 / 68 / 43 (38.739%) | 111 / 68 / 43 (38.739%) | 111 / 68 / 43 (38.739%) | 111 / 68 / 43 (38.739%) |
| sonar-xml-plugin/src/main/java/org/sonar/plugins/xml/checks/maven/DisallowedDependenciesCheck.java | 178 / 104 / 74 (41.573%) | 178 / 108 / 70 (39.326%) | 178 / 108 / 70 (39.326%) | 178 / 108 / 70 (39.326%) |
| sonar-xml-plugin/src/main/java/org/sonar/plugins/xml/checks/maven/GroupIdNamingConventionCheck.java | 79 / 62 / 17 (21.519%) | 79 / 62 / 17 (21.519%) | 79 / 62 / 17 (21.519%) | 79 / 62 / 17 (21.519%) |
| sonar-xml-plugin/src/main/java/org/sonar/plugins/xml/checks/maven/PomElementOrderCheck.java | 146 / 122 / 24 (16.438%) | 146 / 122 / 24 (16.438%) | 146 / 122 / 24 (16.438%) | 146 / 122 / 24 (16.438%) |
| sonar-xml-plugin/src/main/java/org/sonar/plugins/xml/checks/maven/helpers/MavenDependencyMatcher.java | 100 / 83 / 17 (17.000%) | 100 / 100 / 0 (0.000%) | 100 / 84 / 16 (16.000%) | 100 / 100 / 0 (0.000%) |
| sonar-xml-plugin/src/main/java/org/sonar/plugins/xml/checks/maven/helpers/PatternMatcher.java | 43 / 42 / 1 (2.326%) | 43 / 43 / 0 (0.000%) | 43 / 42 / 1 (2.326%) | 43 / 43 / 0 (0.000%) |
| sonar-xml-plugin/src/main/java/org/sonar/plugins/xml/checks/maven/helpers/RangedVersionMatcher.java | 168 / 163 / 5 (2.976%) | 168 / 164 / 4 (2.381%) | 168 / 163 / 5 (2.976%) | 168 / 164 / 4 (2.381%) |
| sonar-xml-plugin/src/main/java/org/sonar/plugins/xml/checks/maven/helpers/StringMatcher.java | 39 / 38 / 1 (2.564%) | 39 / 38 / 1 (2.564%) | 39 / 38 / 1 (2.564%) | 39 / 38 / 1 (2.564%) |
| sonar-xml-plugin/src/main/java/org/sonar/plugins/xml/checks/maven/helpers/package-info.java | 10 / 9 / 1 (10.000%) | 10 / 9 / 1 (10.000%) | 10 / 9 / 1 (10.000%) | 10 / 9 / 1 (10.000%) |
| sonar-xml-plugin/src/main/java/org/sonar/plugins/xml/checks/maven/package-info.java | 9 / 8 / 1 (11.111%) | 9 / 8 / 1 (11.111%) | 9 / 8 / 1 (11.111%) | 9 / 8 / 1 (11.111%) |
| sonar-xml-plugin/src/main/java/org/sonar/plugins/xml/checks/package-info.java | 8 / 7 / 1 (12.500%) | 8 / 7 / 1 (12.500%) | 8 / 7 / 1 (12.500%) | 8 / 7 / 1 (12.500%) |
| sonar-xml-plugin/src/main/java/org/sonar/plugins/xml/checks/security/HardcodedCredentialsCheck.java | 446 / 387 / 59 (13.229%) | 446 / 389 / 57 (12.780%) | 446 / 387 / 59 (13.229%) | 446 / 389 / 57 (12.780%) |
| sonar-xml-plugin/src/main/java/org/sonar/plugins/xml/checks/security/android/AbstractAndroidManifestCheck.java | 20 / 16 / 4 (20.000%) | 20 / 17 / 3 (15.000%) | 20 / 16 / 4 (20.000%) | 20 / 17 / 3 (15.000%) |
| sonar-xml-plugin/src/main/java/org/sonar/plugins/xml/checks/security/android/AndroidApplicationBackupCheck.java | 80 / 55 / 25 (31.250%) | 80 / 58 / 22 (27.500%) | 80 / 55 / 25 (31.250%) | 80 / 58 / 22 (27.500%) |
| sonar-xml-plugin/src/main/java/org/sonar/plugins/xml/checks/security/android/AndroidClearTextCheck.java | 74 / 40 / 34 (45.946%) | 74 / 43 / 31 (41.892%) | 74 / 40 / 34 (45.946%) | 74 / 43 / 31 (41.892%) |
| sonar-xml-plugin/src/main/java/org/sonar/plugins/xml/checks/security/android/AndroidComponentWithIntentFilterExportedCheck.java | 38 / 17 / 21 (55.263%) | 38 / 19 / 19 (50.000%) | 38 / 17 / 21 (55.263%) | 38 / 19 / 19 (50.000%) |
| sonar-xml-plugin/src/main/java/org/sonar/plugins/xml/checks/security/android/AndroidCustomPermissionCheck.java | 52 / 25 / 27 (51.923%) | 52 / 27 / 25 (48.077%) | 52 / 25 / 27 (51.923%) | 52 / 27 / 25 (48.077%) |
| sonar-xml-plugin/src/main/java/org/sonar/plugins/xml/checks/security/android/AndroidExportedContentPermissionsCheck.java | 38 / 17 / 21 (55.263%) | 38 / 19 / 19 (50.000%) | 38 / 17 / 21 (55.263%) | 38 / 19 / 19 (50.000%) |
| sonar-xml-plugin/src/main/java/org/sonar/plugins/xml/checks/security/android/AndroidPermissionsCheck.java | 106 / 94 / 12 (11.321%) | 106 / 97 / 9 (8.491%) | 106 / 94 / 12 (11.321%) | 106 / 97 / 9 (8.491%) |
| sonar-xml-plugin/src/main/java/org/sonar/plugins/xml/checks/security/android/AndroidProviderPermissionCheck.java | 66 / 17 / 49 (74.242%) | 66 / 19 / 47 (71.212%) | 66 / 17 / 49 (74.242%) | 66 / 19 / 47 (71.212%) |
| sonar-xml-plugin/src/main/java/org/sonar/plugins/xml/checks/security/android/AndroidReceivingIntentsCheck.java | 38 / 17 / 21 (55.263%) | 38 / 19 / 19 (50.000%) | 38 / 17 / 21 (55.263%) | 38 / 19 / 19 (50.000%) |
| sonar-xml-plugin/src/main/java/org/sonar/plugins/xml/checks/security/android/DebugFeatureCheck.java | 59 / 23 / 36 (61.017%) | 59 / 27 / 32 (54.237%) | 59 / 23 / 36 (61.017%) | 59 / 27 / 32 (54.237%) |
| sonar-xml-plugin/src/main/java/org/sonar/plugins/xml/checks/security/android/Utils.java | 23 / 20 / 3 (13.043%) | 23 / 20 / 3 (13.043%) | 23 / 20 / 3 (13.043%) | 23 / 20 / 3 (13.043%) |
| sonar-xml-plugin/src/main/java/org/sonar/plugins/xml/checks/security/android/package-info.java | 10 / 9 / 1 (10.000%) | 10 / 9 / 1 (10.000%) | 10 / 9 / 1 (10.000%) | 10 / 9 / 1 (10.000%) |
| sonar-xml-plugin/src/main/java/org/sonar/plugins/xml/checks/security/package-info.java | 9 / 8 / 1 (11.111%) | 9 / 8 / 1 (11.111%) | 9 / 8 / 1 (11.111%) | 9 / 8 / 1 (11.111%) |
| sonar-xml-plugin/src/main/java/org/sonar/plugins/xml/checks/security/web/BaseWebCheck.java | 73 / 63 / 10 (13.699%) | 73 / 65 / 8 (10.959%) | 73 / 63 / 10 (13.699%) | 73 / 65 / 8 (10.959%) |
| sonar-xml-plugin/src/main/java/org/sonar/plugins/xml/checks/security/web/BasicAuthenticationCheck.java | 42 / 22 / 20 (47.619%) | 42 / 23 / 19 (45.238%) | 42 / 22 / 20 (47.619%) | 42 / 23 / 19 (45.238%) |
| sonar-xml-plugin/src/main/java/org/sonar/plugins/xml/checks/security/web/CrossOriginResourceSharingCheck.java | 41 / 19 / 22 (53.659%) | 41 / 20 / 21 (51.220%) | 41 / 19 / 22 (53.659%) | 41 / 20 / 21 (51.220%) |
| sonar-xml-plugin/src/main/java/org/sonar/plugins/xml/checks/security/web/HttpOnlyOnCookiesCheck.java | 120 / 76 / 44 (36.667%) | 120 / 78 / 42 (35.000%) | 120 / 76 / 44 (36.667%) | 120 / 78 / 42 (35.000%) |
| sonar-xml-plugin/src/main/java/org/sonar/plugins/xml/checks/security/web/MimeNosniffCheck.java | 52 / 27 / 25 (48.077%) | 52 / 29 / 23 (44.231%) | 52 / 27 / 25 (48.077%) | 52 / 29 / 23 (44.231%) |
| sonar-xml-plugin/src/main/java/org/sonar/plugins/xml/checks/security/web/PasswordsInWebConfigCheck.java | 48 / 33 / 15 (31.250%) | 48 / 34 / 14 (29.167%) | 48 / 33 / 15 (31.250%) | 48 / 34 / 14 (29.167%) |
| sonar-xml-plugin/src/main/java/org/sonar/plugins/xml/checks/security/web/ValidationFiltersCheck.java | 78 / 39 / 39 (50.000%) | 78 / 42 / 36 (46.154%) | 78 / 39 / 39 (50.000%) | 78 / 42 / 36 (46.154%) |
| sonar-xml-plugin/src/main/java/org/sonar/plugins/xml/checks/security/web/package-info.java | 10 / 9 / 1 (10.000%) | 10 / 9 / 1 (10.000%) | 10 / 9 / 1 (10.000%) | 10 / 9 / 1 (10.000%) |
| sonar-xml-plugin/src/main/java/org/sonar/plugins/xml/checks/spring/DefaultMessageListenerContainerCheck.java | 99 / 68 / 31 (31.313%) | 99 / 68 / 31 (31.313%) | 99 / 68 / 31 (31.313%) | 99 / 68 / 31 (31.313%) |
| sonar-xml-plugin/src/main/java/org/sonar/plugins/xml/checks/spring/SingleConnectionFactoryCheck.java | 81 / 50 / 31 (38.272%) | 81 / 50 / 31 (38.272%) | 81 / 50 / 31 (38.272%) | 81 / 50 / 31 (38.272%) |
| sonar-xml-plugin/src/main/java/org/sonar/plugins/xml/checks/spring/package-info.java | 7 / 6 / 1 (14.286%) | 7 / 6 / 1 (14.286%) | 7 / 6 / 1 (14.286%) | 7 / 6 / 1 (14.286%) |
| sonar-xml-plugin/src/main/java/org/sonar/plugins/xml/checks/struts/ActionNumberCheck.java | 73 / 42 / 31 (42.466%) | 73 / 42 / 31 (42.466%) | 73 / 42 / 31 (42.466%) | 73 / 42 / 31 (42.466%) |
| sonar-xml-plugin/src/main/java/org/sonar/plugins/xml/checks/struts/FormNameDuplicationCheck.java | 97 / 53 / 44 (45.361%) | 97 / 53 / 44 (45.361%) | 97 / 53 / 44 (45.361%) | 97 / 53 / 44 (45.361%) |
| sonar-xml-plugin/src/main/java/org/sonar/plugins/xml/checks/struts/package-info.java | 9 / 8 / 1 (11.111%) | 9 / 8 / 1 (11.111%) | 9 / 8 / 1 (11.111%) | 9 / 8 / 1 (11.111%) |
| sonar-xml-plugin/src/main/java/org/sonar/plugins/xml/package-info.java | 7 / 6 / 1 (14.286%) | 7 / 6 / 1 (14.286%) | 7 / 6 / 1 (14.286%) | 7 / 6 / 1 (14.286%) |

## Rules with findings

| Rule | Baseline | Source paths | Bytecode | Combined |
|---|---:|---:|---:|---:|
| java:S6204 | 1 | 1 | 1 | 1 |

598 configured rules had no findings in any mode and are omitted.

## Detailed differences

<details>
<summary>Source paths versus Baseline</summary>

### Largest semantic improvements

Top five by absolute change in unknown percentage.

| File | Second mode unknown % | First mode unknown % | Change (pp) |
|---|---:|---:|---:|
| sonar-xml-plugin/src/main/java/org/sonar/plugins/xml/checks/CheckList.java | 76.471% | 0.000% | -76.471 |
| sonar-xml-plugin/src/main/java/org/sonar/plugins/xml/XmlRulesDefinition.java | 54.000% | 30.000% | -24.000 |
| sonar-xml-plugin/src/main/java/org/sonar/plugins/xml/XmlSonarWayProfile.java | 56.604% | 33.962% | -22.642 |
| sonar-xml-plugin/src/main/java/org/sonar/plugins/xml/checks/maven/helpers/MavenDependencyMatcher.java | 17.000% | 0.000% | -17.000 |
| sonar-xml-plugin/src/main/java/org/sonar/plugins/xml/XmlPlugin.java | 58.065% | 45.161% | -12.903 |

### Largest semantic regressions

Top five by absolute change in unknown percentage.

None.

| Unknown occurrences | Count |
|---|---:|
| Still unknown in both modes | 1437 |
| No longer reported unknown | 165 |
| Newly reported unknown | 0 |

### No longer reported unknown

| File | Range | Identifier | AST context |
|---|---|---|---|
| sonar-xml-plugin/src/main/java/org/sonar/plugins/xml/LineCounter.java | (104:24)-(104:34) | splitLines | METHOD_INVOCATION |
| sonar-xml-plugin/src/main/java/org/sonar/plugins/xml/Xml.java | (64:73)-(64:82) | XmlPlugin | MEMBER_SELECT |
| sonar-xml-plugin/src/main/java/org/sonar/plugins/xml/Xml.java | (64:83)-(64:100) | FILE_SUFFIXES_KEY | MEMBER_SELECT |
| sonar-xml-plugin/src/main/java/org/sonar/plugins/xml/XmlHighlighting.java | (91:9)-(91:22) | isSelfClosing | METHOD_INVOCATION |
| sonar-xml-plugin/src/main/java/org/sonar/plugins/xml/XmlPlugin.java | (38:7)-(38:10) | Xml | MEMBER_SELECT |
| sonar-xml-plugin/src/main/java/org/sonar/plugins/xml/XmlPlugin.java | (39:7)-(39:25) | XmlRulesDefinition | MEMBER_SELECT |
| sonar-xml-plugin/src/main/java/org/sonar/plugins/xml/XmlPlugin.java | (40:7)-(40:25) | XmlSonarWayProfile | MEMBER_SELECT |
| sonar-xml-plugin/src/main/java/org/sonar/plugins/xml/XmlPlugin.java | (41:7)-(41:16) | XmlSensor | MEMBER_SELECT |
| sonar-xml-plugin/src/main/java/org/sonar/plugins/xml/XmlRulesDefinition.java | (34:57)-(34:60) | Xml | MEMBER_SELECT |
| sonar-xml-plugin/src/main/java/org/sonar/plugins/xml/XmlRulesDefinition.java | (34:61)-(34:75) | REPOSITORY_KEY | MEMBER_SELECT |
| sonar-xml-plugin/src/main/java/org/sonar/plugins/xml/XmlRulesDefinition.java | (34:77)-(34:80) | Xml | MEMBER_SELECT |
| sonar-xml-plugin/src/main/java/org/sonar/plugins/xml/XmlRulesDefinition.java | (34:81)-(34:84) | KEY | MEMBER_SELECT |
| sonar-xml-plugin/src/main/java/org/sonar/plugins/xml/XmlRulesDefinition.java | (34:94)-(34:97) | Xml | MEMBER_SELECT |
| sonar-xml-plugin/src/main/java/org/sonar/plugins/xml/XmlRulesDefinition.java | (34:98)-(34:113) | REPOSITORY_NAME | MEMBER_SELECT |
| sonar-xml-plugin/src/main/java/org/sonar/plugins/xml/XmlRulesDefinition.java | (36:68)-(36:71) | Xml | MEMBER_SELECT |
| sonar-xml-plugin/src/main/java/org/sonar/plugins/xml/XmlRulesDefinition.java | (36:72)-(36:89) | XML_RESOURCE_PATH | MEMBER_SELECT |
| sonar-xml-plugin/src/main/java/org/sonar/plugins/xml/XmlRulesDefinition.java | (36:91)-(36:94) | Xml | MEMBER_SELECT |
| sonar-xml-plugin/src/main/java/org/sonar/plugins/xml/XmlRulesDefinition.java | (36:95)-(36:109) | SONAR_WAY_PATH | MEMBER_SELECT |
| sonar-xml-plugin/src/main/java/org/sonar/plugins/xml/XmlRulesDefinition.java | (39:61)-(39:70) | CheckList | MEMBER_SELECT |
| sonar-xml-plugin/src/main/java/org/sonar/plugins/xml/XmlRulesDefinition.java | (39:71)-(39:86) | getCheckClasses | MEMBER_SELECT |

145 additional occurrences omitted.


### Newly reported unknown

None.


Semantic graph location differences unavailable: traversal is DISABLED, PARTIAL, or UNKNOWN in at least one compared mode. Primary identifier and finding comparisons remain valid.

### Findings only in Baseline

None.

### Findings only in Source paths

None.

</details>

<details>
<summary>Bytecode versus Baseline</summary>

### Largest semantic improvements

Top five by absolute change in unknown percentage.

| File | Second mode unknown % | First mode unknown % | Change (pp) |
|---|---:|---:|---:|
| sonar-xml-plugin/src/main/java/org/sonar/plugins/xml/XmlRulesDefinition.java | 54.000% | 50.000% | -4.000 |
| sonar-xml-plugin/src/main/java/org/sonar/plugins/xml/checks/maven/DisallowedDependenciesCheck.java | 41.573% | 39.326% | -2.247 |
| sonar-xml-plugin/src/main/java/org/sonar/plugins/xml/checks/maven/helpers/MavenDependencyMatcher.java | 17.000% | 16.000% | -1.000 |
| sonar-xml-plugin/src/main/java/org/sonar/plugins/xml/XmlSensor.java | 43.132% | 42.582% | -0.549 |

### Largest semantic regressions

Top five by absolute change in unknown percentage.

None.

| Unknown occurrences | Count |
|---|---:|
| Still unknown in both modes | 1593 |
| No longer reported unknown | 9 |
| Newly reported unknown | 0 |

### No longer reported unknown

| File | Range | Identifier | AST context |
|---|---|---|---|
| sonar-xml-plugin/src/main/java/org/sonar/plugins/xml/XmlRulesDefinition.java | (39:61)-(39:70) | CheckList | MEMBER_SELECT |
| sonar-xml-plugin/src/main/java/org/sonar/plugins/xml/XmlRulesDefinition.java | (39:71)-(39:86) | getCheckClasses | MEMBER_SELECT |
| sonar-xml-plugin/src/main/java/org/sonar/plugins/xml/XmlSensor.java | (69:78)-(69:87) | CheckList | MEMBER_SELECT |
| sonar-xml-plugin/src/main/java/org/sonar/plugins/xml/XmlSensor.java | (69:88)-(69:103) | getCheckClasses | MEMBER_SELECT |
| sonar-xml-plugin/src/main/java/org/sonar/plugins/xml/checks/maven/DisallowedDependenciesCheck.java | (109:11)-(109:33) | MavenDependencyMatcher | METHOD |
| sonar-xml-plugin/src/main/java/org/sonar/plugins/xml/checks/maven/DisallowedDependenciesCheck.java | (113:33)-(113:55) | MavenDependencyMatcher | NEW_CLASS |
| sonar-xml-plugin/src/main/java/org/sonar/plugins/xml/checks/maven/DisallowedDependenciesCheck.java | (60:11)-(60:33) | MavenDependencyMatcher | VARIABLE |
| sonar-xml-plugin/src/main/java/org/sonar/plugins/xml/checks/maven/DisallowedDependenciesCheck.java | (67:5)-(67:27) | MavenDependencyMatcher | VARIABLE |
| sonar-xml-plugin/src/main/java/org/sonar/plugins/xml/checks/maven/helpers/MavenDependencyMatcher.java | (77:114)-(77:128) | PatternMatcher | NEW_CLASS |


### Newly reported unknown

None.


Semantic graph location differences unavailable: traversal is DISABLED, PARTIAL, or UNKNOWN in at least one compared mode. Primary identifier and finding comparisons remain valid.

### Findings only in Baseline

None.

### Findings only in Bytecode

None.

</details>

<details>
<summary>Combined versus Baseline</summary>

### Largest semantic improvements

Top five by absolute change in unknown percentage.

| File | Second mode unknown % | First mode unknown % | Change (pp) |
|---|---:|---:|---:|
| sonar-xml-plugin/src/main/java/org/sonar/plugins/xml/checks/CheckList.java | 76.471% | 0.000% | -76.471 |
| sonar-xml-plugin/src/main/java/org/sonar/plugins/xml/XmlRulesDefinition.java | 54.000% | 30.000% | -24.000 |
| sonar-xml-plugin/src/main/java/org/sonar/plugins/xml/XmlSonarWayProfile.java | 56.604% | 33.962% | -22.642 |
| sonar-xml-plugin/src/main/java/org/sonar/plugins/xml/checks/maven/helpers/MavenDependencyMatcher.java | 17.000% | 0.000% | -17.000 |
| sonar-xml-plugin/src/main/java/org/sonar/plugins/xml/XmlPlugin.java | 58.065% | 45.161% | -12.903 |

### Largest semantic regressions

Top five by absolute change in unknown percentage.

None.

| Unknown occurrences | Count |
|---|---:|
| Still unknown in both modes | 1437 |
| No longer reported unknown | 165 |
| Newly reported unknown | 0 |

### No longer reported unknown

| File | Range | Identifier | AST context |
|---|---|---|---|
| sonar-xml-plugin/src/main/java/org/sonar/plugins/xml/LineCounter.java | (104:24)-(104:34) | splitLines | METHOD_INVOCATION |
| sonar-xml-plugin/src/main/java/org/sonar/plugins/xml/Xml.java | (64:73)-(64:82) | XmlPlugin | MEMBER_SELECT |
| sonar-xml-plugin/src/main/java/org/sonar/plugins/xml/Xml.java | (64:83)-(64:100) | FILE_SUFFIXES_KEY | MEMBER_SELECT |
| sonar-xml-plugin/src/main/java/org/sonar/plugins/xml/XmlHighlighting.java | (91:9)-(91:22) | isSelfClosing | METHOD_INVOCATION |
| sonar-xml-plugin/src/main/java/org/sonar/plugins/xml/XmlPlugin.java | (38:7)-(38:10) | Xml | MEMBER_SELECT |
| sonar-xml-plugin/src/main/java/org/sonar/plugins/xml/XmlPlugin.java | (39:7)-(39:25) | XmlRulesDefinition | MEMBER_SELECT |
| sonar-xml-plugin/src/main/java/org/sonar/plugins/xml/XmlPlugin.java | (40:7)-(40:25) | XmlSonarWayProfile | MEMBER_SELECT |
| sonar-xml-plugin/src/main/java/org/sonar/plugins/xml/XmlPlugin.java | (41:7)-(41:16) | XmlSensor | MEMBER_SELECT |
| sonar-xml-plugin/src/main/java/org/sonar/plugins/xml/XmlRulesDefinition.java | (34:57)-(34:60) | Xml | MEMBER_SELECT |
| sonar-xml-plugin/src/main/java/org/sonar/plugins/xml/XmlRulesDefinition.java | (34:61)-(34:75) | REPOSITORY_KEY | MEMBER_SELECT |
| sonar-xml-plugin/src/main/java/org/sonar/plugins/xml/XmlRulesDefinition.java | (34:77)-(34:80) | Xml | MEMBER_SELECT |
| sonar-xml-plugin/src/main/java/org/sonar/plugins/xml/XmlRulesDefinition.java | (34:81)-(34:84) | KEY | MEMBER_SELECT |
| sonar-xml-plugin/src/main/java/org/sonar/plugins/xml/XmlRulesDefinition.java | (34:94)-(34:97) | Xml | MEMBER_SELECT |
| sonar-xml-plugin/src/main/java/org/sonar/plugins/xml/XmlRulesDefinition.java | (34:98)-(34:113) | REPOSITORY_NAME | MEMBER_SELECT |
| sonar-xml-plugin/src/main/java/org/sonar/plugins/xml/XmlRulesDefinition.java | (36:68)-(36:71) | Xml | MEMBER_SELECT |
| sonar-xml-plugin/src/main/java/org/sonar/plugins/xml/XmlRulesDefinition.java | (36:72)-(36:89) | XML_RESOURCE_PATH | MEMBER_SELECT |
| sonar-xml-plugin/src/main/java/org/sonar/plugins/xml/XmlRulesDefinition.java | (36:91)-(36:94) | Xml | MEMBER_SELECT |
| sonar-xml-plugin/src/main/java/org/sonar/plugins/xml/XmlRulesDefinition.java | (36:95)-(36:109) | SONAR_WAY_PATH | MEMBER_SELECT |
| sonar-xml-plugin/src/main/java/org/sonar/plugins/xml/XmlRulesDefinition.java | (39:61)-(39:70) | CheckList | MEMBER_SELECT |
| sonar-xml-plugin/src/main/java/org/sonar/plugins/xml/XmlRulesDefinition.java | (39:71)-(39:86) | getCheckClasses | MEMBER_SELECT |

145 additional occurrences omitted.


### Newly reported unknown

None.


Semantic graph location differences unavailable: traversal is DISABLED, PARTIAL, or UNKNOWN in at least one compared mode. Primary identifier and finding comparisons remain valid.

### Findings only in Baseline

None.

### Findings only in Combined

None.

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
| Still unknown in both modes | 1437 |
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
| sonar-xml-plugin/src/main/java/org/sonar/plugins/xml/checks/CheckList.java | 76.471% | 0.000% | -76.471 |
| sonar-xml-plugin/src/main/java/org/sonar/plugins/xml/XmlSonarWayProfile.java | 56.604% | 33.962% | -22.642 |
| sonar-xml-plugin/src/main/java/org/sonar/plugins/xml/XmlRulesDefinition.java | 50.000% | 30.000% | -20.000 |
| sonar-xml-plugin/src/main/java/org/sonar/plugins/xml/checks/maven/helpers/MavenDependencyMatcher.java | 16.000% | 0.000% | -16.000 |
| sonar-xml-plugin/src/main/java/org/sonar/plugins/xml/XmlPlugin.java | 58.065% | 45.161% | -12.903 |

### Largest semantic regressions

Top five by absolute change in unknown percentage.

None.

| Unknown occurrences | Count |
|---|---:|
| Still unknown in both modes | 1437 |
| No longer reported unknown | 156 |
| Newly reported unknown | 0 |

### No longer reported unknown

| File | Range | Identifier | AST context |
|---|---|---|---|
| sonar-xml-plugin/src/main/java/org/sonar/plugins/xml/LineCounter.java | (104:24)-(104:34) | splitLines | METHOD_INVOCATION |
| sonar-xml-plugin/src/main/java/org/sonar/plugins/xml/Xml.java | (64:73)-(64:82) | XmlPlugin | MEMBER_SELECT |
| sonar-xml-plugin/src/main/java/org/sonar/plugins/xml/Xml.java | (64:83)-(64:100) | FILE_SUFFIXES_KEY | MEMBER_SELECT |
| sonar-xml-plugin/src/main/java/org/sonar/plugins/xml/XmlHighlighting.java | (91:9)-(91:22) | isSelfClosing | METHOD_INVOCATION |
| sonar-xml-plugin/src/main/java/org/sonar/plugins/xml/XmlPlugin.java | (38:7)-(38:10) | Xml | MEMBER_SELECT |
| sonar-xml-plugin/src/main/java/org/sonar/plugins/xml/XmlPlugin.java | (39:7)-(39:25) | XmlRulesDefinition | MEMBER_SELECT |
| sonar-xml-plugin/src/main/java/org/sonar/plugins/xml/XmlPlugin.java | (40:7)-(40:25) | XmlSonarWayProfile | MEMBER_SELECT |
| sonar-xml-plugin/src/main/java/org/sonar/plugins/xml/XmlPlugin.java | (41:7)-(41:16) | XmlSensor | MEMBER_SELECT |
| sonar-xml-plugin/src/main/java/org/sonar/plugins/xml/XmlRulesDefinition.java | (34:57)-(34:60) | Xml | MEMBER_SELECT |
| sonar-xml-plugin/src/main/java/org/sonar/plugins/xml/XmlRulesDefinition.java | (34:61)-(34:75) | REPOSITORY_KEY | MEMBER_SELECT |
| sonar-xml-plugin/src/main/java/org/sonar/plugins/xml/XmlRulesDefinition.java | (34:77)-(34:80) | Xml | MEMBER_SELECT |
| sonar-xml-plugin/src/main/java/org/sonar/plugins/xml/XmlRulesDefinition.java | (34:81)-(34:84) | KEY | MEMBER_SELECT |
| sonar-xml-plugin/src/main/java/org/sonar/plugins/xml/XmlRulesDefinition.java | (34:94)-(34:97) | Xml | MEMBER_SELECT |
| sonar-xml-plugin/src/main/java/org/sonar/plugins/xml/XmlRulesDefinition.java | (34:98)-(34:113) | REPOSITORY_NAME | MEMBER_SELECT |
| sonar-xml-plugin/src/main/java/org/sonar/plugins/xml/XmlRulesDefinition.java | (36:68)-(36:71) | Xml | MEMBER_SELECT |
| sonar-xml-plugin/src/main/java/org/sonar/plugins/xml/XmlRulesDefinition.java | (36:72)-(36:89) | XML_RESOURCE_PATH | MEMBER_SELECT |
| sonar-xml-plugin/src/main/java/org/sonar/plugins/xml/XmlRulesDefinition.java | (36:91)-(36:94) | Xml | MEMBER_SELECT |
| sonar-xml-plugin/src/main/java/org/sonar/plugins/xml/XmlRulesDefinition.java | (36:95)-(36:109) | SONAR_WAY_PATH | MEMBER_SELECT |
| sonar-xml-plugin/src/main/java/org/sonar/plugins/xml/XmlSensor.java | (119:21)-(119:28) | analyse | MEMBER_SELECT |
| sonar-xml-plugin/src/main/java/org/sonar/plugins/xml/XmlSensor.java | (119:9)-(119:20) | LineCounter | MEMBER_SELECT |

136 additional occurrences omitted.


### Newly reported unknown

None.


Semantic graph location differences unavailable: traversal is DISABLED, PARTIAL, or UNKNOWN in at least one compared mode. Primary identifier and finding comparisons remain valid.

### Findings only in Bytecode

None.

### Findings only in Combined

None.

</details>


## Measured timings

Warm-ups are excluded. JavaSensor time includes compilation.

| Sample | Mode | Maven/server wall (ms) | JavaSensor (ms) | Compilation (ms) | Compilation outcome | Generated classes |
|---|---|---:|---:|---:|---|---:|
| 1 | Baseline | 15834 | 5997.0 | 0.0 | DISABLED | 0 |
| 1 | Source paths | 16102 | 6482.0 | 0.0 | DISABLED | 0 |
| 1 | Bytecode | 17856 | 6473.0 | 346.0 | FAILED | 3 |
| 1 | Combined | 17556 | 6910.0 | 343.0 | FAILED | 3 |
| 2 | Baseline | 15993 | 6141.0 | 0.0 | DISABLED | 0 |
| 2 | Source paths | 17778 | 6616.0 | 0.0 | DISABLED | 0 |
| 2 | Bytecode | 17929 | 6500.0 | 349.0 | FAILED | 3 |
| 2 | Combined | 17729 | 6827.0 | 335.0 | FAILED | 3 |
| 3 | Baseline | 15836 | 6115.0 | 0.0 | DISABLED | 0 |
| 3 | Source paths | 16219 | 6563.0 | 0.0 | DISABLED | 0 |
| 3 | Bytecode | 15940 | 6435.0 | 329.0 | FAILED | 3 |
| 3 | Combined | 17831 | 6818.0 | 335.0 | FAILED | 3 |

## Run metadata

| Setting | Value |
|---|---|
| Baseline properties | {sonar.java.compileToByteCode=false, sonar.java.experimental.batchModeSizeInKB=500, sonar.java.fileByFile=true, sonar.java.sourcepath=, sonar.java.test.sourcepath=} |
| Baseline warm-up status | SUCCESS |
| Bytecode properties | {sonar.java.compileToByteCode=true, sonar.java.experimental.batchModeSizeInKB=500, sonar.java.fileByFile=true, sonar.java.sourcepath=, sonar.java.test.sourcepath=} |
| Bytecode warm-up status | SUCCESS |
| Combined properties | {sonar.java.compileToByteCode=true, sonar.java.experimental.batchModeSizeInKB=500, sonar.java.fileByFile=true, sonar.java.sourcepath=sonar-xml-plugin/src/main/java, sonar.java.test.sourcepath=} |
| Combined warm-up status | SUCCESS |
| External library count | 0 |
| Graph diagnostic stability | stable |
| Repeated result stability | stable |
| Scenario | Each production file is parsed separately; no external dependency JARs are supplied |
| Shared scenario properties | {sonar.java.experimental.batchModeSizeInKB=500, sonar.java.fileByFile=true} |
| Source paths properties | {sonar.java.compileToByteCode=false, sonar.java.experimental.batchModeSizeInKB=500, sonar.java.fileByFile=true, sonar.java.sourcepath=sonar-xml-plugin/src/main/java, sonar.java.test.sourcepath=} |
| Source paths warm-up status | SUCCESS |

</details>

<details>
<summary>sonar-xml / normal / dependencies</summary>

Production files use the normal parser batch size; every mode receives the same compile-scope external JARs

## Summary

| Metric | Baseline | Source paths | Bytecode | Combined |
|---|---:|---:|---:|---:|
| Source paths enabled | NO | YES | NO | YES |
| Internal compilation enabled | NO | NO | YES | YES |
| Scan status | SUCCESS | SUCCESS | SUCCESS | SUCCESS |
| Java files analyzed | 69 | 69 | 69 | 69 |
| Identifiers (total) | 5608 | 5608 | 5608 | 5608 |
| Known identifiers | 5608 | 5608 | 5608 | 5608 |
| Unknown identifiers | 0 | 0 | 0 | 0 |
| Unknown identifiers (%) | 0.000% | 0.000% | 0.000% | 0.000% |
| Semantic graph resolved symbols | N/A | N/A | N/A | N/A |
| Semantic graph unknown symbols | N/A | N/A | N/A | N/A |
| Semantic graph resolved types | N/A | N/A | N/A | N/A |
| Semantic graph unknown types | N/A | N/A | N/A | N/A |
| Semantic graph traversal | DISABLED | DISABLED | DISABLED | DISABLED |
| Graph expansion limit per module | 0 | 0 | 0 | 0 |
| Graph expansions executed (representative sample) | 0 | 0 | 0 | 0 |
| Files with no unknown identifiers | 69 / 69 | 69 / 69 | 69 / 69 | 69 / 69 |
| Findings | 5 | 5 | 5 | 5 |
| Median Maven/server time (ms) | 15814.0 | 16160.0 | 16466.0 | 16196.0 |
| Median JavaSensor time, including compilation (ms) | 6019.0 | 6150.0 | 6291.0 | 6234.0 |
| Compilation outcome | DISABLED | DISABLED | SUCCESS | SUCCESS |
| Median internal compilation time (ms) | 0.0 | 0.0 | 411.0 | 420.0 |
| Compilation source files | 0 | 0 | 69 | 69 |
| Generated class files before cleanup | 0 | 0 | 75 | 75 |
| Temporary bytecode cleaned up | YES | YES | YES | YES |
| Source characters analyzed | 182211 | 182211 | 182211 | 182211 |
| Undefined-type errors | 0 | 0 | 0 | 0 |
| Source characters with parse errors | N/A | N/A | N/A | N/A |
| Source characters with analysis exceptions | N/A | N/A | N/A | N/A |

### Feature comparisons

Changes are the first named mode minus the second.

| Comparison | Status | Unknown count change | Unknown % change (pp) | Files improved / unchanged / regressed | Shared findings | Second mode only | First mode only | Retention |
|---|---|---:|---:|---:|---:|---:|---:|---:|
| Source paths versus Baseline | VALID | +0 | +0.000 | 0 / 69 / 0 | 5 | 0 | 0 | 100.000% |
| Bytecode versus Baseline | VALID | +0 | +0.000 | 0 / 69 / 0 | 5 | 0 | 0 | 100.000% |
| Combined versus Baseline | VALID | +0 | +0.000 | 0 / 69 / 0 | 5 | 0 | 0 | 100.000% |
| Combined versus Source paths | VALID | +0 | +0.000 | 0 / 69 / 0 | 5 | 0 | 0 | 100.000% |
| Combined versus Bytecode | VALID | +0 | +0.000 | 0 / 69 / 0 | 5 | 0 | 0 | 100.000% |

## Unknown occurrences by AST context

Context is the identifier's AST parent kind. Modes without occurrence details are N/A.

No unknown contexts available.

## Top files contributing unknown identifiers

Top five by the largest unknown count in any mode. Cells show **unknown count (share of project unknowns)**.

None.

## Semantics per file

Each cell shows **total / known / unknown (unknown %)**. Missing semantic coverage is N/A.

| File | Baseline | Source paths | Bytecode | Combined |
|---|---:|---:|---:|---:|
| sonar-xml-plugin/src/main/java/org/sonar/plugins/xml/LineCounter.java | 213 / 213 / 0 (0.000%) | 213 / 213 / 0 (0.000%) | 213 / 213 / 0 (0.000%) | 213 / 213 / 0 (0.000%) |
| sonar-xml-plugin/src/main/java/org/sonar/plugins/xml/Utils.java | 33 / 33 / 0 (0.000%) | 33 / 33 / 0 (0.000%) | 33 / 33 / 0 (0.000%) | 33 / 33 / 0 (0.000%) |
| sonar-xml-plugin/src/main/java/org/sonar/plugins/xml/Xml.java | 82 / 82 / 0 (0.000%) | 82 / 82 / 0 (0.000%) | 82 / 82 / 0 (0.000%) | 82 / 82 / 0 (0.000%) |
| sonar-xml-plugin/src/main/java/org/sonar/plugins/xml/XmlHighlighting.java | 252 / 252 / 0 (0.000%) | 252 / 252 / 0 (0.000%) | 252 / 252 / 0 (0.000%) | 252 / 252 / 0 (0.000%) |
| sonar-xml-plugin/src/main/java/org/sonar/plugins/xml/XmlPlugin.java | 31 / 31 / 0 (0.000%) | 31 / 31 / 0 (0.000%) | 31 / 31 / 0 (0.000%) | 31 / 31 / 0 (0.000%) |
| sonar-xml-plugin/src/main/java/org/sonar/plugins/xml/XmlRulesDefinition.java | 50 / 50 / 0 (0.000%) | 50 / 50 / 0 (0.000%) | 50 / 50 / 0 (0.000%) | 50 / 50 / 0 (0.000%) |
| sonar-xml-plugin/src/main/java/org/sonar/plugins/xml/XmlSensor.java | 364 / 364 / 0 (0.000%) | 364 / 364 / 0 (0.000%) | 364 / 364 / 0 (0.000%) | 364 / 364 / 0 (0.000%) |
| sonar-xml-plugin/src/main/java/org/sonar/plugins/xml/XmlSonarWayProfile.java | 53 / 53 / 0 (0.000%) | 53 / 53 / 0 (0.000%) | 53 / 53 / 0 (0.000%) | 53 / 53 / 0 (0.000%) |
| sonar-xml-plugin/src/main/java/org/sonar/plugins/xml/api/XmlProfileRegistrar.java | 15 / 15 / 0 (0.000%) | 15 / 15 / 0 (0.000%) | 15 / 15 / 0 (0.000%) | 15 / 15 / 0 (0.000%) |
| sonar-xml-plugin/src/main/java/org/sonar/plugins/xml/api/package-info.java | 8 / 8 / 0 (0.000%) | 8 / 8 / 0 (0.000%) | 8 / 8 / 0 (0.000%) | 8 / 8 / 0 (0.000%) |
| sonar-xml-plugin/src/main/java/org/sonar/plugins/xml/checks/CharBeforePrologCheck.java | 33 / 33 / 0 (0.000%) | 33 / 33 / 0 (0.000%) | 33 / 33 / 0 (0.000%) | 33 / 33 / 0 (0.000%) |
| sonar-xml-plugin/src/main/java/org/sonar/plugins/xml/checks/CheckList.java | 51 / 51 / 0 (0.000%) | 51 / 51 / 0 (0.000%) | 51 / 51 / 0 (0.000%) | 51 / 51 / 0 (0.000%) |
| sonar-xml-plugin/src/main/java/org/sonar/plugins/xml/checks/CommentContainsPatternChecker.java | 90 / 90 / 0 (0.000%) | 90 / 90 / 0 (0.000%) | 90 / 90 / 0 (0.000%) | 90 / 90 / 0 (0.000%) |
| sonar-xml-plugin/src/main/java/org/sonar/plugins/xml/checks/CommentedOutCodeCheck.java | 209 / 209 / 0 (0.000%) | 209 / 209 / 0 (0.000%) | 209 / 209 / 0 (0.000%) | 209 / 209 / 0 (0.000%) |
| sonar-xml-plugin/src/main/java/org/sonar/plugins/xml/checks/FixmeCommentCheck.java | 21 / 21 / 0 (0.000%) | 21 / 21 / 0 (0.000%) | 21 / 21 / 0 (0.000%) | 21 / 21 / 0 (0.000%) |
| sonar-xml-plugin/src/main/java/org/sonar/plugins/xml/checks/IndentationCheck.java | 282 / 282 / 0 (0.000%) | 282 / 282 / 0 (0.000%) | 282 / 282 / 0 (0.000%) | 282 / 282 / 0 (0.000%) |
| sonar-xml-plugin/src/main/java/org/sonar/plugins/xml/checks/LineLengthCheck.java | 75 / 75 / 0 (0.000%) | 75 / 75 / 0 (0.000%) | 75 / 75 / 0 (0.000%) | 75 / 75 / 0 (0.000%) |
| sonar-xml-plugin/src/main/java/org/sonar/plugins/xml/checks/NewlineCheck.java | 217 / 217 / 0 (0.000%) | 217 / 217 / 0 (0.000%) | 217 / 217 / 0 (0.000%) | 217 / 217 / 0 (0.000%) |
| sonar-xml-plugin/src/main/java/org/sonar/plugins/xml/checks/ParsingErrorCheck.java | 17 / 17 / 0 (0.000%) | 17 / 17 / 0 (0.000%) | 17 / 17 / 0 (0.000%) | 17 / 17 / 0 (0.000%) |
| sonar-xml-plugin/src/main/java/org/sonar/plugins/xml/checks/TabCharacterCheck.java | 115 / 115 / 0 (0.000%) | 115 / 115 / 0 (0.000%) | 115 / 115 / 0 (0.000%) | 115 / 115 / 0 (0.000%) |
| sonar-xml-plugin/src/main/java/org/sonar/plugins/xml/checks/TodoCommentCheck.java | 21 / 21 / 0 (0.000%) | 21 / 21 / 0 (0.000%) | 21 / 21 / 0 (0.000%) | 21 / 21 / 0 (0.000%) |
| sonar-xml-plugin/src/main/java/org/sonar/plugins/xml/checks/XPathCheck.java | 285 / 285 / 0 (0.000%) | 285 / 285 / 0 (0.000%) | 285 / 285 / 0 (0.000%) | 285 / 285 / 0 (0.000%) |
| sonar-xml-plugin/src/main/java/org/sonar/plugins/xml/checks/ejb/DefaultInterceptorsLocationCheck.java | 32 / 32 / 0 (0.000%) | 32 / 32 / 0 (0.000%) | 32 / 32 / 0 (0.000%) | 32 / 32 / 0 (0.000%) |
| sonar-xml-plugin/src/main/java/org/sonar/plugins/xml/checks/ejb/InterceptorExclusionsCheck.java | 40 / 40 / 0 (0.000%) | 40 / 40 / 0 (0.000%) | 40 / 40 / 0 (0.000%) | 40 / 40 / 0 (0.000%) |
| sonar-xml-plugin/src/main/java/org/sonar/plugins/xml/checks/ejb/package-info.java | 9 / 9 / 0 (0.000%) | 9 / 9 / 0 (0.000%) | 9 / 9 / 0 (0.000%) | 9 / 9 / 0 (0.000%) |
| sonar-xml-plugin/src/main/java/org/sonar/plugins/xml/checks/hibernate/DatabaseSchemaUpdateCheck.java | 48 / 48 / 0 (0.000%) | 48 / 48 / 0 (0.000%) | 48 / 48 / 0 (0.000%) | 48 / 48 / 0 (0.000%) |
| sonar-xml-plugin/src/main/java/org/sonar/plugins/xml/checks/hibernate/package-info.java | 9 / 9 / 0 (0.000%) | 9 / 9 / 0 (0.000%) | 9 / 9 / 0 (0.000%) | 9 / 9 / 0 (0.000%) |
| sonar-xml-plugin/src/main/java/org/sonar/plugins/xml/checks/maven/ArtifactIdNamingConventionCheck.java | 79 / 79 / 0 (0.000%) | 79 / 79 / 0 (0.000%) | 79 / 79 / 0 (0.000%) | 79 / 79 / 0 (0.000%) |
| sonar-xml-plugin/src/main/java/org/sonar/plugins/xml/checks/maven/DependencyWithSystemScopeCheck.java | 87 / 87 / 0 (0.000%) | 87 / 87 / 0 (0.000%) | 87 / 87 / 0 (0.000%) | 87 / 87 / 0 (0.000%) |
| sonar-xml-plugin/src/main/java/org/sonar/plugins/xml/checks/maven/DeprecatedPomPropertiesCheck.java | 111 / 111 / 0 (0.000%) | 111 / 111 / 0 (0.000%) | 111 / 111 / 0 (0.000%) | 111 / 111 / 0 (0.000%) |
| sonar-xml-plugin/src/main/java/org/sonar/plugins/xml/checks/maven/DisallowedDependenciesCheck.java | 178 / 178 / 0 (0.000%) | 178 / 178 / 0 (0.000%) | 178 / 178 / 0 (0.000%) | 178 / 178 / 0 (0.000%) |
| sonar-xml-plugin/src/main/java/org/sonar/plugins/xml/checks/maven/GroupIdNamingConventionCheck.java | 79 / 79 / 0 (0.000%) | 79 / 79 / 0 (0.000%) | 79 / 79 / 0 (0.000%) | 79 / 79 / 0 (0.000%) |
| sonar-xml-plugin/src/main/java/org/sonar/plugins/xml/checks/maven/PomElementOrderCheck.java | 146 / 146 / 0 (0.000%) | 146 / 146 / 0 (0.000%) | 146 / 146 / 0 (0.000%) | 146 / 146 / 0 (0.000%) |
| sonar-xml-plugin/src/main/java/org/sonar/plugins/xml/checks/maven/helpers/MavenDependencyMatcher.java | 100 / 100 / 0 (0.000%) | 100 / 100 / 0 (0.000%) | 100 / 100 / 0 (0.000%) | 100 / 100 / 0 (0.000%) |
| sonar-xml-plugin/src/main/java/org/sonar/plugins/xml/checks/maven/helpers/PatternMatcher.java | 43 / 43 / 0 (0.000%) | 43 / 43 / 0 (0.000%) | 43 / 43 / 0 (0.000%) | 43 / 43 / 0 (0.000%) |
| sonar-xml-plugin/src/main/java/org/sonar/plugins/xml/checks/maven/helpers/RangedVersionMatcher.java | 168 / 168 / 0 (0.000%) | 168 / 168 / 0 (0.000%) | 168 / 168 / 0 (0.000%) | 168 / 168 / 0 (0.000%) |
| sonar-xml-plugin/src/main/java/org/sonar/plugins/xml/checks/maven/helpers/StringMatcher.java | 39 / 39 / 0 (0.000%) | 39 / 39 / 0 (0.000%) | 39 / 39 / 0 (0.000%) | 39 / 39 / 0 (0.000%) |
| sonar-xml-plugin/src/main/java/org/sonar/plugins/xml/checks/maven/helpers/package-info.java | 10 / 10 / 0 (0.000%) | 10 / 10 / 0 (0.000%) | 10 / 10 / 0 (0.000%) | 10 / 10 / 0 (0.000%) |
| sonar-xml-plugin/src/main/java/org/sonar/plugins/xml/checks/maven/package-info.java | 9 / 9 / 0 (0.000%) | 9 / 9 / 0 (0.000%) | 9 / 9 / 0 (0.000%) | 9 / 9 / 0 (0.000%) |
| sonar-xml-plugin/src/main/java/org/sonar/plugins/xml/checks/package-info.java | 8 / 8 / 0 (0.000%) | 8 / 8 / 0 (0.000%) | 8 / 8 / 0 (0.000%) | 8 / 8 / 0 (0.000%) |
| sonar-xml-plugin/src/main/java/org/sonar/plugins/xml/checks/security/HardcodedCredentialsCheck.java | 446 / 446 / 0 (0.000%) | 446 / 446 / 0 (0.000%) | 446 / 446 / 0 (0.000%) | 446 / 446 / 0 (0.000%) |
| sonar-xml-plugin/src/main/java/org/sonar/plugins/xml/checks/security/android/AbstractAndroidManifestCheck.java | 20 / 20 / 0 (0.000%) | 20 / 20 / 0 (0.000%) | 20 / 20 / 0 (0.000%) | 20 / 20 / 0 (0.000%) |
| sonar-xml-plugin/src/main/java/org/sonar/plugins/xml/checks/security/android/AndroidApplicationBackupCheck.java | 80 / 80 / 0 (0.000%) | 80 / 80 / 0 (0.000%) | 80 / 80 / 0 (0.000%) | 80 / 80 / 0 (0.000%) |
| sonar-xml-plugin/src/main/java/org/sonar/plugins/xml/checks/security/android/AndroidClearTextCheck.java | 74 / 74 / 0 (0.000%) | 74 / 74 / 0 (0.000%) | 74 / 74 / 0 (0.000%) | 74 / 74 / 0 (0.000%) |
| sonar-xml-plugin/src/main/java/org/sonar/plugins/xml/checks/security/android/AndroidComponentWithIntentFilterExportedCheck.java | 38 / 38 / 0 (0.000%) | 38 / 38 / 0 (0.000%) | 38 / 38 / 0 (0.000%) | 38 / 38 / 0 (0.000%) |
| sonar-xml-plugin/src/main/java/org/sonar/plugins/xml/checks/security/android/AndroidCustomPermissionCheck.java | 52 / 52 / 0 (0.000%) | 52 / 52 / 0 (0.000%) | 52 / 52 / 0 (0.000%) | 52 / 52 / 0 (0.000%) |
| sonar-xml-plugin/src/main/java/org/sonar/plugins/xml/checks/security/android/AndroidExportedContentPermissionsCheck.java | 38 / 38 / 0 (0.000%) | 38 / 38 / 0 (0.000%) | 38 / 38 / 0 (0.000%) | 38 / 38 / 0 (0.000%) |
| sonar-xml-plugin/src/main/java/org/sonar/plugins/xml/checks/security/android/AndroidPermissionsCheck.java | 106 / 106 / 0 (0.000%) | 106 / 106 / 0 (0.000%) | 106 / 106 / 0 (0.000%) | 106 / 106 / 0 (0.000%) |
| sonar-xml-plugin/src/main/java/org/sonar/plugins/xml/checks/security/android/AndroidProviderPermissionCheck.java | 66 / 66 / 0 (0.000%) | 66 / 66 / 0 (0.000%) | 66 / 66 / 0 (0.000%) | 66 / 66 / 0 (0.000%) |
| sonar-xml-plugin/src/main/java/org/sonar/plugins/xml/checks/security/android/AndroidReceivingIntentsCheck.java | 38 / 38 / 0 (0.000%) | 38 / 38 / 0 (0.000%) | 38 / 38 / 0 (0.000%) | 38 / 38 / 0 (0.000%) |
| sonar-xml-plugin/src/main/java/org/sonar/plugins/xml/checks/security/android/DebugFeatureCheck.java | 59 / 59 / 0 (0.000%) | 59 / 59 / 0 (0.000%) | 59 / 59 / 0 (0.000%) | 59 / 59 / 0 (0.000%) |
| sonar-xml-plugin/src/main/java/org/sonar/plugins/xml/checks/security/android/Utils.java | 23 / 23 / 0 (0.000%) | 23 / 23 / 0 (0.000%) | 23 / 23 / 0 (0.000%) | 23 / 23 / 0 (0.000%) |
| sonar-xml-plugin/src/main/java/org/sonar/plugins/xml/checks/security/android/package-info.java | 10 / 10 / 0 (0.000%) | 10 / 10 / 0 (0.000%) | 10 / 10 / 0 (0.000%) | 10 / 10 / 0 (0.000%) |
| sonar-xml-plugin/src/main/java/org/sonar/plugins/xml/checks/security/package-info.java | 9 / 9 / 0 (0.000%) | 9 / 9 / 0 (0.000%) | 9 / 9 / 0 (0.000%) | 9 / 9 / 0 (0.000%) |
| sonar-xml-plugin/src/main/java/org/sonar/plugins/xml/checks/security/web/BaseWebCheck.java | 73 / 73 / 0 (0.000%) | 73 / 73 / 0 (0.000%) | 73 / 73 / 0 (0.000%) | 73 / 73 / 0 (0.000%) |
| sonar-xml-plugin/src/main/java/org/sonar/plugins/xml/checks/security/web/BasicAuthenticationCheck.java | 42 / 42 / 0 (0.000%) | 42 / 42 / 0 (0.000%) | 42 / 42 / 0 (0.000%) | 42 / 42 / 0 (0.000%) |
| sonar-xml-plugin/src/main/java/org/sonar/plugins/xml/checks/security/web/CrossOriginResourceSharingCheck.java | 41 / 41 / 0 (0.000%) | 41 / 41 / 0 (0.000%) | 41 / 41 / 0 (0.000%) | 41 / 41 / 0 (0.000%) |
| sonar-xml-plugin/src/main/java/org/sonar/plugins/xml/checks/security/web/HttpOnlyOnCookiesCheck.java | 120 / 120 / 0 (0.000%) | 120 / 120 / 0 (0.000%) | 120 / 120 / 0 (0.000%) | 120 / 120 / 0 (0.000%) |
| sonar-xml-plugin/src/main/java/org/sonar/plugins/xml/checks/security/web/MimeNosniffCheck.java | 52 / 52 / 0 (0.000%) | 52 / 52 / 0 (0.000%) | 52 / 52 / 0 (0.000%) | 52 / 52 / 0 (0.000%) |
| sonar-xml-plugin/src/main/java/org/sonar/plugins/xml/checks/security/web/PasswordsInWebConfigCheck.java | 48 / 48 / 0 (0.000%) | 48 / 48 / 0 (0.000%) | 48 / 48 / 0 (0.000%) | 48 / 48 / 0 (0.000%) |
| sonar-xml-plugin/src/main/java/org/sonar/plugins/xml/checks/security/web/ValidationFiltersCheck.java | 78 / 78 / 0 (0.000%) | 78 / 78 / 0 (0.000%) | 78 / 78 / 0 (0.000%) | 78 / 78 / 0 (0.000%) |
| sonar-xml-plugin/src/main/java/org/sonar/plugins/xml/checks/security/web/package-info.java | 10 / 10 / 0 (0.000%) | 10 / 10 / 0 (0.000%) | 10 / 10 / 0 (0.000%) | 10 / 10 / 0 (0.000%) |
| sonar-xml-plugin/src/main/java/org/sonar/plugins/xml/checks/spring/DefaultMessageListenerContainerCheck.java | 99 / 99 / 0 (0.000%) | 99 / 99 / 0 (0.000%) | 99 / 99 / 0 (0.000%) | 99 / 99 / 0 (0.000%) |
| sonar-xml-plugin/src/main/java/org/sonar/plugins/xml/checks/spring/SingleConnectionFactoryCheck.java | 81 / 81 / 0 (0.000%) | 81 / 81 / 0 (0.000%) | 81 / 81 / 0 (0.000%) | 81 / 81 / 0 (0.000%) |
| sonar-xml-plugin/src/main/java/org/sonar/plugins/xml/checks/spring/package-info.java | 7 / 7 / 0 (0.000%) | 7 / 7 / 0 (0.000%) | 7 / 7 / 0 (0.000%) | 7 / 7 / 0 (0.000%) |
| sonar-xml-plugin/src/main/java/org/sonar/plugins/xml/checks/struts/ActionNumberCheck.java | 73 / 73 / 0 (0.000%) | 73 / 73 / 0 (0.000%) | 73 / 73 / 0 (0.000%) | 73 / 73 / 0 (0.000%) |
| sonar-xml-plugin/src/main/java/org/sonar/plugins/xml/checks/struts/FormNameDuplicationCheck.java | 97 / 97 / 0 (0.000%) | 97 / 97 / 0 (0.000%) | 97 / 97 / 0 (0.000%) | 97 / 97 / 0 (0.000%) |
| sonar-xml-plugin/src/main/java/org/sonar/plugins/xml/checks/struts/package-info.java | 9 / 9 / 0 (0.000%) | 9 / 9 / 0 (0.000%) | 9 / 9 / 0 (0.000%) | 9 / 9 / 0 (0.000%) |
| sonar-xml-plugin/src/main/java/org/sonar/plugins/xml/package-info.java | 7 / 7 / 0 (0.000%) | 7 / 7 / 0 (0.000%) | 7 / 7 / 0 (0.000%) | 7 / 7 / 0 (0.000%) |

## Rules with findings

| Rule | Baseline | Source paths | Bytecode | Combined |
|---|---:|---:|---:|---:|
| java:S1128 | 1 | 1 | 1 | 1 |
| java:S2160 | 1 | 1 | 1 | 1 |
| java:S6204 | 3 | 3 | 3 | 3 |

596 configured rules had no findings in any mode and are omitted.

## Detailed differences

<details>
<summary>Source paths versus Baseline</summary>

### Largest semantic improvements

Top five by absolute change in unknown percentage.

None.

### Largest semantic regressions

Top five by absolute change in unknown percentage.

None.

| Unknown occurrences | Count |
|---|---:|
| Still unknown in both modes | 0 |
| No longer reported unknown | 0 |
| Newly reported unknown | 0 |

### No longer reported unknown

None.


### Newly reported unknown

None.


Semantic graph location differences unavailable: traversal is DISABLED, PARTIAL, or UNKNOWN in at least one compared mode. Primary identifier and finding comparisons remain valid.

### Findings only in Baseline

None.

### Findings only in Source paths

None.

</details>

<details>
<summary>Bytecode versus Baseline</summary>

### Largest semantic improvements

Top five by absolute change in unknown percentage.

None.

### Largest semantic regressions

Top five by absolute change in unknown percentage.

None.

| Unknown occurrences | Count |
|---|---:|
| Still unknown in both modes | 0 |
| No longer reported unknown | 0 |
| Newly reported unknown | 0 |

### No longer reported unknown

None.


### Newly reported unknown

None.


Semantic graph location differences unavailable: traversal is DISABLED, PARTIAL, or UNKNOWN in at least one compared mode. Primary identifier and finding comparisons remain valid.

### Findings only in Baseline

None.

### Findings only in Bytecode

None.

</details>

<details>
<summary>Combined versus Baseline</summary>

### Largest semantic improvements

Top five by absolute change in unknown percentage.

None.

### Largest semantic regressions

Top five by absolute change in unknown percentage.

None.

| Unknown occurrences | Count |
|---|---:|
| Still unknown in both modes | 0 |
| No longer reported unknown | 0 |
| Newly reported unknown | 0 |

### No longer reported unknown

None.


### Newly reported unknown

None.


Semantic graph location differences unavailable: traversal is DISABLED, PARTIAL, or UNKNOWN in at least one compared mode. Primary identifier and finding comparisons remain valid.

### Findings only in Baseline

None.

### Findings only in Combined

None.

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
| Still unknown in both modes | 0 |
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

None.

### Largest semantic regressions

Top five by absolute change in unknown percentage.

None.

| Unknown occurrences | Count |
|---|---:|
| Still unknown in both modes | 0 |
| No longer reported unknown | 0 |
| Newly reported unknown | 0 |

### No longer reported unknown

None.


### Newly reported unknown

None.


Semantic graph location differences unavailable: traversal is DISABLED, PARTIAL, or UNKNOWN in at least one compared mode. Primary identifier and finding comparisons remain valid.

### Findings only in Bytecode

None.

### Findings only in Combined

None.

</details>


## Measured timings

Warm-ups are excluded. JavaSensor time includes compilation.

| Sample | Mode | Maven/server wall (ms) | JavaSensor (ms) | Compilation (ms) | Compilation outcome | Generated classes |
|---|---|---:|---:|---:|---|---:|
| 1 | Baseline | 15814 | 6446.0 | 0.0 | DISABLED | 0 |
| 1 | Source paths | 16396 | 6150.0 | 0.0 | DISABLED | 0 |
| 1 | Bytecode | 17908 | 6688.0 | 425.0 | SUCCESS | 75 |
| 1 | Combined | 16000 | 6254.0 | 422.0 | SUCCESS | 75 |
| 2 | Baseline | 15795 | 6019.0 | 0.0 | DISABLED | 0 |
| 2 | Source paths | 15672 | 6151.0 | 0.0 | DISABLED | 0 |
| 2 | Bytecode | 15772 | 6291.0 | 411.0 | SUCCESS | 75 |
| 2 | Combined | 16196 | 6189.0 | 407.0 | SUCCESS | 75 |
| 3 | Baseline | 16205 | 5975.0 | 0.0 | DISABLED | 0 |
| 3 | Source paths | 16160 | 6024.0 | 0.0 | DISABLED | 0 |
| 3 | Bytecode | 16466 | 6165.0 | 411.0 | SUCCESS | 75 |
| 3 | Combined | 16226 | 6234.0 | 420.0 | SUCCESS | 75 |

## Run metadata

| Setting | Value |
|---|---|
| Baseline properties | {sonar.java.compileToByteCode=false, sonar.java.experimental.batchModeSizeInKB=500, sonar.java.fileByFile=false, sonar.java.sourcepath=, sonar.java.test.sourcepath=} |
| Baseline warm-up status | SUCCESS |
| Bytecode properties | {sonar.java.compileToByteCode=true, sonar.java.experimental.batchModeSizeInKB=500, sonar.java.fileByFile=false, sonar.java.sourcepath=, sonar.java.test.sourcepath=} |
| Bytecode warm-up status | SUCCESS |
| Combined properties | {sonar.java.compileToByteCode=true, sonar.java.experimental.batchModeSizeInKB=500, sonar.java.fileByFile=false, sonar.java.sourcepath=sonar-xml-plugin/src/main/java, sonar.java.test.sourcepath=} |
| Combined warm-up status | SUCCESS |
| External library count | 8 |
| Graph diagnostic stability | stable |
| Repeated result stability | stable |
| Scenario | Production files use the normal parser batch size; every mode receives the same compile-scope external JARs |
| Shared scenario properties | {sonar.java.experimental.batchModeSizeInKB=500, sonar.java.fileByFile=false} |
| Source paths properties | {sonar.java.compileToByteCode=false, sonar.java.experimental.batchModeSizeInKB=500, sonar.java.fileByFile=false, sonar.java.sourcepath=sonar-xml-plugin/src/main/java, sonar.java.test.sourcepath=} |
| Source paths warm-up status | SUCCESS |

</details>

<details>
<summary>sonar-xml / file-by-file / dependencies</summary>

Each production file is parsed separately; every mode receives the same compile-scope external JARs

## Summary

| Metric | Baseline | Source paths | Bytecode | Combined |
|---|---:|---:|---:|---:|
| Source paths enabled | NO | YES | NO | YES |
| Internal compilation enabled | NO | NO | YES | YES |
| Scan status | SUCCESS | SUCCESS | SUCCESS | SUCCESS |
| Java files analyzed | 69 | 69 | 69 | 69 |
| Identifiers (total) | 5608 | 5608 | 5608 | 5608 |
| Known identifiers | 5223 | 5608 | 5608 | 5608 |
| Unknown identifiers | 385 | 0 | 0 | 0 |
| Unknown identifiers (%) | 6.865% | 0.000% | 0.000% | 0.000% |
| Semantic graph resolved symbols | N/A | N/A | N/A | N/A |
| Semantic graph unknown symbols | N/A | N/A | N/A | N/A |
| Semantic graph resolved types | N/A | N/A | N/A | N/A |
| Semantic graph unknown types | N/A | N/A | N/A | N/A |
| Semantic graph traversal | DISABLED | DISABLED | DISABLED | DISABLED |
| Graph expansion limit per module | 0 | 0 | 0 | 0 |
| Graph expansions executed (representative sample) | 0 | 0 | 0 | 0 |
| Files with no unknown identifiers | 33 / 69 | 69 / 69 | 69 / 69 | 69 / 69 |
| Findings | 3 | 5 | 5 | 5 |
| Median Maven/server time (ms) | 16006.0 | 17829.0 | 17967.0 | 18075.0 |
| Median JavaSensor time, including compilation (ms) | 6403.0 | 6915.0 | 6846.0 | 7188.0 |
| Compilation outcome | DISABLED | DISABLED | SUCCESS | SUCCESS |
| Median internal compilation time (ms) | 0.0 | 0.0 | 412.0 | 404.0 |
| Compilation source files | 0 | 0 | 69 | 69 |
| Generated class files before cleanup | 0 | 0 | 75 | 75 |
| Temporary bytecode cleaned up | YES | YES | YES | YES |
| Source characters analyzed | 182211 | 182211 | 182211 | 182211 |
| Undefined-type errors | 145 | 0 | 0 | 0 |
| Source characters with parse errors | N/A | N/A | N/A | N/A |
| Source characters with analysis exceptions | N/A | N/A | N/A | N/A |

### Feature comparisons

Changes are the first named mode minus the second.

| Comparison | Status | Unknown count change | Unknown % change (pp) | Files improved / unchanged / regressed | Shared findings | Second mode only | First mode only | Retention |
|---|---|---:|---:|---:|---:|---:|---:|---:|
| Source paths versus Baseline | VALID | -385 | -6.865 | 36 / 33 / 0 | 3 | 0 | 2 | 100.000% |
| Bytecode versus Baseline | VALID | -385 | -6.865 | 36 / 33 / 0 | 3 | 0 | 2 | 100.000% |
| Combined versus Baseline | VALID | -385 | -6.865 | 36 / 33 / 0 | 3 | 0 | 2 | 100.000% |
| Combined versus Source paths | VALID | +0 | +0.000 | 0 / 69 / 0 | 5 | 0 | 0 | 100.000% |
| Combined versus Bytecode | VALID | +0 | +0.000 | 0 / 69 / 0 | 5 | 0 | 0 | 100.000% |

## Unknown occurrences by AST context

Context is the identifier's AST parent kind. Modes without occurrence details are N/A.

| AST context | Baseline | Source paths | Bytecode | Combined |
|---|---:|---:|---:|---:|
| ARGUMENTS | 30 | 0 | 0 | 0 |
| ARRAY_TYPE | 1 | 0 | 0 | 0 |
| CLASS | 16 | 0 | 0 | 0 |
| LIST | 2 | 0 | 0 | 0 |
| MEMBER_SELECT | 222 | 0 | 0 | 0 |
| METHOD | 3 | 0 | 0 | 0 |
| METHOD_INVOCATION | 57 | 0 | 0 | 0 |
| METHOD_REFERENCE | 5 | 0 | 0 | 0 |
| NEW_ARRAY | 1 | 0 | 0 | 0 |
| NEW_CLASS | 4 | 0 | 0 | 0 |
| PARAMETERIZED_TYPE | 2 | 0 | 0 | 0 |
| PLUS | 2 | 0 | 0 | 0 |
| TYPE_ARGUMENTS | 2 | 0 | 0 | 0 |
| TYPE_CAST | 10 | 0 | 0 | 0 |
| VARIABLE | 28 | 0 | 0 | 0 |

## Top files contributing unknown identifiers

Top five by the largest unknown count in any mode. Cells show **unknown count (share of project unknowns)**.

| File | Baseline | Source paths | Bytecode | Combined |
|---|---:|---:|---:|---:|
| sonar-xml-plugin/src/main/java/org/sonar/plugins/xml/checks/security/android/AndroidProviderPermissionCheck.java | 42 (10.909%) | 0 (N/A) | 0 (N/A) | 0 (N/A) |
| sonar-xml-plugin/src/main/java/org/sonar/plugins/xml/checks/CheckList.java | 39 (10.130%) | 0 (N/A) | 0 (N/A) | 0 (N/A) |
| sonar-xml-plugin/src/main/java/org/sonar/plugins/xml/checks/security/web/ValidationFiltersCheck.java | 31 (8.052%) | 0 (N/A) | 0 (N/A) | 0 (N/A) |
| sonar-xml-plugin/src/main/java/org/sonar/plugins/xml/checks/security/android/AndroidClearTextCheck.java | 22 (5.714%) | 0 (N/A) | 0 (N/A) | 0 (N/A) |
| sonar-xml-plugin/src/main/java/org/sonar/plugins/xml/checks/security/web/HttpOnlyOnCookiesCheck.java | 22 (5.714%) | 0 (N/A) | 0 (N/A) | 0 (N/A) |

## Semantics per file

Each cell shows **total / known / unknown (unknown %)**. Missing semantic coverage is N/A.

| File | Baseline | Source paths | Bytecode | Combined |
|---|---:|---:|---:|---:|
| sonar-xml-plugin/src/main/java/org/sonar/plugins/xml/LineCounter.java | 213 / 212 / 1 (0.469%) | 213 / 213 / 0 (0.000%) | 213 / 213 / 0 (0.000%) | 213 / 213 / 0 (0.000%) |
| sonar-xml-plugin/src/main/java/org/sonar/plugins/xml/Utils.java | 33 / 33 / 0 (0.000%) | 33 / 33 / 0 (0.000%) | 33 / 33 / 0 (0.000%) | 33 / 33 / 0 (0.000%) |
| sonar-xml-plugin/src/main/java/org/sonar/plugins/xml/Xml.java | 82 / 80 / 2 (2.439%) | 82 / 82 / 0 (0.000%) | 82 / 82 / 0 (0.000%) | 82 / 82 / 0 (0.000%) |
| sonar-xml-plugin/src/main/java/org/sonar/plugins/xml/XmlHighlighting.java | 252 / 251 / 1 (0.397%) | 252 / 252 / 0 (0.000%) | 252 / 252 / 0 (0.000%) | 252 / 252 / 0 (0.000%) |
| sonar-xml-plugin/src/main/java/org/sonar/plugins/xml/XmlPlugin.java | 31 / 27 / 4 (12.903%) | 31 / 31 / 0 (0.000%) | 31 / 31 / 0 (0.000%) | 31 / 31 / 0 (0.000%) |
| sonar-xml-plugin/src/main/java/org/sonar/plugins/xml/XmlRulesDefinition.java | 50 / 37 / 13 (26.000%) | 50 / 50 / 0 (0.000%) | 50 / 50 / 0 (0.000%) | 50 / 50 / 0 (0.000%) |
| sonar-xml-plugin/src/main/java/org/sonar/plugins/xml/XmlSensor.java | 364 / 343 / 21 (5.769%) | 364 / 364 / 0 (0.000%) | 364 / 364 / 0 (0.000%) | 364 / 364 / 0 (0.000%) |
| sonar-xml-plugin/src/main/java/org/sonar/plugins/xml/XmlSonarWayProfile.java | 53 / 40 / 13 (24.528%) | 53 / 53 / 0 (0.000%) | 53 / 53 / 0 (0.000%) | 53 / 53 / 0 (0.000%) |
| sonar-xml-plugin/src/main/java/org/sonar/plugins/xml/api/XmlProfileRegistrar.java | 15 / 15 / 0 (0.000%) | 15 / 15 / 0 (0.000%) | 15 / 15 / 0 (0.000%) | 15 / 15 / 0 (0.000%) |
| sonar-xml-plugin/src/main/java/org/sonar/plugins/xml/api/package-info.java | 8 / 8 / 0 (0.000%) | 8 / 8 / 0 (0.000%) | 8 / 8 / 0 (0.000%) | 8 / 8 / 0 (0.000%) |
| sonar-xml-plugin/src/main/java/org/sonar/plugins/xml/checks/CharBeforePrologCheck.java | 33 / 33 / 0 (0.000%) | 33 / 33 / 0 (0.000%) | 33 / 33 / 0 (0.000%) | 33 / 33 / 0 (0.000%) |
| sonar-xml-plugin/src/main/java/org/sonar/plugins/xml/checks/CheckList.java | 51 / 12 / 39 (76.471%) | 51 / 51 / 0 (0.000%) | 51 / 51 / 0 (0.000%) | 51 / 51 / 0 (0.000%) |
| sonar-xml-plugin/src/main/java/org/sonar/plugins/xml/checks/CommentContainsPatternChecker.java | 90 / 90 / 0 (0.000%) | 90 / 90 / 0 (0.000%) | 90 / 90 / 0 (0.000%) | 90 / 90 / 0 (0.000%) |
| sonar-xml-plugin/src/main/java/org/sonar/plugins/xml/checks/CommentedOutCodeCheck.java | 209 / 209 / 0 (0.000%) | 209 / 209 / 0 (0.000%) | 209 / 209 / 0 (0.000%) | 209 / 209 / 0 (0.000%) |
| sonar-xml-plugin/src/main/java/org/sonar/plugins/xml/checks/FixmeCommentCheck.java | 21 / 19 / 2 (9.524%) | 21 / 21 / 0 (0.000%) | 21 / 21 / 0 (0.000%) | 21 / 21 / 0 (0.000%) |
| sonar-xml-plugin/src/main/java/org/sonar/plugins/xml/checks/IndentationCheck.java | 282 / 279 / 3 (1.064%) | 282 / 282 / 0 (0.000%) | 282 / 282 / 0 (0.000%) | 282 / 282 / 0 (0.000%) |
| sonar-xml-plugin/src/main/java/org/sonar/plugins/xml/checks/LineLengthCheck.java | 75 / 73 / 2 (2.667%) | 75 / 75 / 0 (0.000%) | 75 / 75 / 0 (0.000%) | 75 / 75 / 0 (0.000%) |
| sonar-xml-plugin/src/main/java/org/sonar/plugins/xml/checks/NewlineCheck.java | 217 / 215 / 2 (0.922%) | 217 / 217 / 0 (0.000%) | 217 / 217 / 0 (0.000%) | 217 / 217 / 0 (0.000%) |
| sonar-xml-plugin/src/main/java/org/sonar/plugins/xml/checks/ParsingErrorCheck.java | 17 / 17 / 0 (0.000%) | 17 / 17 / 0 (0.000%) | 17 / 17 / 0 (0.000%) | 17 / 17 / 0 (0.000%) |
| sonar-xml-plugin/src/main/java/org/sonar/plugins/xml/checks/TabCharacterCheck.java | 115 / 111 / 4 (3.478%) | 115 / 115 / 0 (0.000%) | 115 / 115 / 0 (0.000%) | 115 / 115 / 0 (0.000%) |
| sonar-xml-plugin/src/main/java/org/sonar/plugins/xml/checks/TodoCommentCheck.java | 21 / 19 / 2 (9.524%) | 21 / 21 / 0 (0.000%) | 21 / 21 / 0 (0.000%) | 21 / 21 / 0 (0.000%) |
| sonar-xml-plugin/src/main/java/org/sonar/plugins/xml/checks/XPathCheck.java | 285 / 285 / 0 (0.000%) | 285 / 285 / 0 (0.000%) | 285 / 285 / 0 (0.000%) | 285 / 285 / 0 (0.000%) |
| sonar-xml-plugin/src/main/java/org/sonar/plugins/xml/checks/ejb/DefaultInterceptorsLocationCheck.java | 32 / 32 / 0 (0.000%) | 32 / 32 / 0 (0.000%) | 32 / 32 / 0 (0.000%) | 32 / 32 / 0 (0.000%) |
| sonar-xml-plugin/src/main/java/org/sonar/plugins/xml/checks/ejb/InterceptorExclusionsCheck.java | 40 / 40 / 0 (0.000%) | 40 / 40 / 0 (0.000%) | 40 / 40 / 0 (0.000%) | 40 / 40 / 0 (0.000%) |
| sonar-xml-plugin/src/main/java/org/sonar/plugins/xml/checks/ejb/package-info.java | 9 / 9 / 0 (0.000%) | 9 / 9 / 0 (0.000%) | 9 / 9 / 0 (0.000%) | 9 / 9 / 0 (0.000%) |
| sonar-xml-plugin/src/main/java/org/sonar/plugins/xml/checks/hibernate/DatabaseSchemaUpdateCheck.java | 48 / 48 / 0 (0.000%) | 48 / 48 / 0 (0.000%) | 48 / 48 / 0 (0.000%) | 48 / 48 / 0 (0.000%) |
| sonar-xml-plugin/src/main/java/org/sonar/plugins/xml/checks/hibernate/package-info.java | 9 / 9 / 0 (0.000%) | 9 / 9 / 0 (0.000%) | 9 / 9 / 0 (0.000%) | 9 / 9 / 0 (0.000%) |
| sonar-xml-plugin/src/main/java/org/sonar/plugins/xml/checks/maven/ArtifactIdNamingConventionCheck.java | 79 / 79 / 0 (0.000%) | 79 / 79 / 0 (0.000%) | 79 / 79 / 0 (0.000%) | 79 / 79 / 0 (0.000%) |
| sonar-xml-plugin/src/main/java/org/sonar/plugins/xml/checks/maven/DependencyWithSystemScopeCheck.java | 87 / 87 / 0 (0.000%) | 87 / 87 / 0 (0.000%) | 87 / 87 / 0 (0.000%) | 87 / 87 / 0 (0.000%) |
| sonar-xml-plugin/src/main/java/org/sonar/plugins/xml/checks/maven/DeprecatedPomPropertiesCheck.java | 111 / 111 / 0 (0.000%) | 111 / 111 / 0 (0.000%) | 111 / 111 / 0 (0.000%) | 111 / 111 / 0 (0.000%) |
| sonar-xml-plugin/src/main/java/org/sonar/plugins/xml/checks/maven/DisallowedDependenciesCheck.java | 178 / 173 / 5 (2.809%) | 178 / 178 / 0 (0.000%) | 178 / 178 / 0 (0.000%) | 178 / 178 / 0 (0.000%) |
| sonar-xml-plugin/src/main/java/org/sonar/plugins/xml/checks/maven/GroupIdNamingConventionCheck.java | 79 / 79 / 0 (0.000%) | 79 / 79 / 0 (0.000%) | 79 / 79 / 0 (0.000%) | 79 / 79 / 0 (0.000%) |
| sonar-xml-plugin/src/main/java/org/sonar/plugins/xml/checks/maven/PomElementOrderCheck.java | 146 / 146 / 0 (0.000%) | 146 / 146 / 0 (0.000%) | 146 / 146 / 0 (0.000%) | 146 / 146 / 0 (0.000%) |
| sonar-xml-plugin/src/main/java/org/sonar/plugins/xml/checks/maven/helpers/MavenDependencyMatcher.java | 100 / 83 / 17 (17.000%) | 100 / 100 / 0 (0.000%) | 100 / 100 / 0 (0.000%) | 100 / 100 / 0 (0.000%) |
| sonar-xml-plugin/src/main/java/org/sonar/plugins/xml/checks/maven/helpers/PatternMatcher.java | 43 / 42 / 1 (2.326%) | 43 / 43 / 0 (0.000%) | 43 / 43 / 0 (0.000%) | 43 / 43 / 0 (0.000%) |
| sonar-xml-plugin/src/main/java/org/sonar/plugins/xml/checks/maven/helpers/RangedVersionMatcher.java | 168 / 167 / 1 (0.595%) | 168 / 168 / 0 (0.000%) | 168 / 168 / 0 (0.000%) | 168 / 168 / 0 (0.000%) |
| sonar-xml-plugin/src/main/java/org/sonar/plugins/xml/checks/maven/helpers/StringMatcher.java | 39 / 39 / 0 (0.000%) | 39 / 39 / 0 (0.000%) | 39 / 39 / 0 (0.000%) | 39 / 39 / 0 (0.000%) |
| sonar-xml-plugin/src/main/java/org/sonar/plugins/xml/checks/maven/helpers/package-info.java | 10 / 10 / 0 (0.000%) | 10 / 10 / 0 (0.000%) | 10 / 10 / 0 (0.000%) | 10 / 10 / 0 (0.000%) |
| sonar-xml-plugin/src/main/java/org/sonar/plugins/xml/checks/maven/package-info.java | 9 / 9 / 0 (0.000%) | 9 / 9 / 0 (0.000%) | 9 / 9 / 0 (0.000%) | 9 / 9 / 0 (0.000%) |
| sonar-xml-plugin/src/main/java/org/sonar/plugins/xml/checks/package-info.java | 8 / 8 / 0 (0.000%) | 8 / 8 / 0 (0.000%) | 8 / 8 / 0 (0.000%) | 8 / 8 / 0 (0.000%) |
| sonar-xml-plugin/src/main/java/org/sonar/plugins/xml/checks/security/HardcodedCredentialsCheck.java | 446 / 444 / 2 (0.448%) | 446 / 446 / 0 (0.000%) | 446 / 446 / 0 (0.000%) | 446 / 446 / 0 (0.000%) |
| sonar-xml-plugin/src/main/java/org/sonar/plugins/xml/checks/security/android/AbstractAndroidManifestCheck.java | 20 / 19 / 1 (5.000%) | 20 / 20 / 0 (0.000%) | 20 / 20 / 0 (0.000%) | 20 / 20 / 0 (0.000%) |
| sonar-xml-plugin/src/main/java/org/sonar/plugins/xml/checks/security/android/AndroidApplicationBackupCheck.java | 80 / 68 / 12 (15.000%) | 80 / 80 / 0 (0.000%) | 80 / 80 / 0 (0.000%) | 80 / 80 / 0 (0.000%) |
| sonar-xml-plugin/src/main/java/org/sonar/plugins/xml/checks/security/android/AndroidClearTextCheck.java | 74 / 52 / 22 (29.730%) | 74 / 74 / 0 (0.000%) | 74 / 74 / 0 (0.000%) | 74 / 74 / 0 (0.000%) |
| sonar-xml-plugin/src/main/java/org/sonar/plugins/xml/checks/security/android/AndroidComponentWithIntentFilterExportedCheck.java | 38 / 24 / 14 (36.842%) | 38 / 38 / 0 (0.000%) | 38 / 38 / 0 (0.000%) | 38 / 38 / 0 (0.000%) |
| sonar-xml-plugin/src/main/java/org/sonar/plugins/xml/checks/security/android/AndroidCustomPermissionCheck.java | 52 / 32 / 20 (38.462%) | 52 / 52 / 0 (0.000%) | 52 / 52 / 0 (0.000%) | 52 / 52 / 0 (0.000%) |
| sonar-xml-plugin/src/main/java/org/sonar/plugins/xml/checks/security/android/AndroidExportedContentPermissionsCheck.java | 38 / 24 / 14 (36.842%) | 38 / 38 / 0 (0.000%) | 38 / 38 / 0 (0.000%) | 38 / 38 / 0 (0.000%) |
| sonar-xml-plugin/src/main/java/org/sonar/plugins/xml/checks/security/android/AndroidPermissionsCheck.java | 106 / 101 / 5 (4.717%) | 106 / 106 / 0 (0.000%) | 106 / 106 / 0 (0.000%) | 106 / 106 / 0 (0.000%) |
| sonar-xml-plugin/src/main/java/org/sonar/plugins/xml/checks/security/android/AndroidProviderPermissionCheck.java | 66 / 24 / 42 (63.636%) | 66 / 66 / 0 (0.000%) | 66 / 66 / 0 (0.000%) | 66 / 66 / 0 (0.000%) |
| sonar-xml-plugin/src/main/java/org/sonar/plugins/xml/checks/security/android/AndroidReceivingIntentsCheck.java | 38 / 24 / 14 (36.842%) | 38 / 38 / 0 (0.000%) | 38 / 38 / 0 (0.000%) | 38 / 38 / 0 (0.000%) |
| sonar-xml-plugin/src/main/java/org/sonar/plugins/xml/checks/security/android/DebugFeatureCheck.java | 59 / 54 / 5 (8.475%) | 59 / 59 / 0 (0.000%) | 59 / 59 / 0 (0.000%) | 59 / 59 / 0 (0.000%) |
| sonar-xml-plugin/src/main/java/org/sonar/plugins/xml/checks/security/android/Utils.java | 23 / 23 / 0 (0.000%) | 23 / 23 / 0 (0.000%) | 23 / 23 / 0 (0.000%) | 23 / 23 / 0 (0.000%) |
| sonar-xml-plugin/src/main/java/org/sonar/plugins/xml/checks/security/android/package-info.java | 10 / 10 / 0 (0.000%) | 10 / 10 / 0 (0.000%) | 10 / 10 / 0 (0.000%) | 10 / 10 / 0 (0.000%) |
| sonar-xml-plugin/src/main/java/org/sonar/plugins/xml/checks/security/package-info.java | 9 / 9 / 0 (0.000%) | 9 / 9 / 0 (0.000%) | 9 / 9 / 0 (0.000%) | 9 / 9 / 0 (0.000%) |
| sonar-xml-plugin/src/main/java/org/sonar/plugins/xml/checks/security/web/BaseWebCheck.java | 73 / 71 / 2 (2.740%) | 73 / 73 / 0 (0.000%) | 73 / 73 / 0 (0.000%) | 73 / 73 / 0 (0.000%) |
| sonar-xml-plugin/src/main/java/org/sonar/plugins/xml/checks/security/web/BasicAuthenticationCheck.java | 42 / 34 / 8 (19.048%) | 42 / 42 / 0 (0.000%) | 42 / 42 / 0 (0.000%) | 42 / 42 / 0 (0.000%) |
| sonar-xml-plugin/src/main/java/org/sonar/plugins/xml/checks/security/web/CrossOriginResourceSharingCheck.java | 41 / 27 / 14 (34.146%) | 41 / 41 / 0 (0.000%) | 41 / 41 / 0 (0.000%) | 41 / 41 / 0 (0.000%) |
| sonar-xml-plugin/src/main/java/org/sonar/plugins/xml/checks/security/web/HttpOnlyOnCookiesCheck.java | 120 / 98 / 22 (18.333%) | 120 / 120 / 0 (0.000%) | 120 / 120 / 0 (0.000%) | 120 / 120 / 0 (0.000%) |
| sonar-xml-plugin/src/main/java/org/sonar/plugins/xml/checks/security/web/MimeNosniffCheck.java | 52 / 36 / 16 (30.769%) | 52 / 52 / 0 (0.000%) | 52 / 52 / 0 (0.000%) | 52 / 52 / 0 (0.000%) |
| sonar-xml-plugin/src/main/java/org/sonar/plugins/xml/checks/security/web/PasswordsInWebConfigCheck.java | 48 / 40 / 8 (16.667%) | 48 / 48 / 0 (0.000%) | 48 / 48 / 0 (0.000%) | 48 / 48 / 0 (0.000%) |
| sonar-xml-plugin/src/main/java/org/sonar/plugins/xml/checks/security/web/ValidationFiltersCheck.java | 78 / 47 / 31 (39.744%) | 78 / 78 / 0 (0.000%) | 78 / 78 / 0 (0.000%) | 78 / 78 / 0 (0.000%) |
| sonar-xml-plugin/src/main/java/org/sonar/plugins/xml/checks/security/web/package-info.java | 10 / 10 / 0 (0.000%) | 10 / 10 / 0 (0.000%) | 10 / 10 / 0 (0.000%) | 10 / 10 / 0 (0.000%) |
| sonar-xml-plugin/src/main/java/org/sonar/plugins/xml/checks/spring/DefaultMessageListenerContainerCheck.java | 99 / 99 / 0 (0.000%) | 99 / 99 / 0 (0.000%) | 99 / 99 / 0 (0.000%) | 99 / 99 / 0 (0.000%) |
| sonar-xml-plugin/src/main/java/org/sonar/plugins/xml/checks/spring/SingleConnectionFactoryCheck.java | 81 / 81 / 0 (0.000%) | 81 / 81 / 0 (0.000%) | 81 / 81 / 0 (0.000%) | 81 / 81 / 0 (0.000%) |
| sonar-xml-plugin/src/main/java/org/sonar/plugins/xml/checks/spring/package-info.java | 7 / 7 / 0 (0.000%) | 7 / 7 / 0 (0.000%) | 7 / 7 / 0 (0.000%) | 7 / 7 / 0 (0.000%) |
| sonar-xml-plugin/src/main/java/org/sonar/plugins/xml/checks/struts/ActionNumberCheck.java | 73 / 73 / 0 (0.000%) | 73 / 73 / 0 (0.000%) | 73 / 73 / 0 (0.000%) | 73 / 73 / 0 (0.000%) |
| sonar-xml-plugin/src/main/java/org/sonar/plugins/xml/checks/struts/FormNameDuplicationCheck.java | 97 / 97 / 0 (0.000%) | 97 / 97 / 0 (0.000%) | 97 / 97 / 0 (0.000%) | 97 / 97 / 0 (0.000%) |
| sonar-xml-plugin/src/main/java/org/sonar/plugins/xml/checks/struts/package-info.java | 9 / 9 / 0 (0.000%) | 9 / 9 / 0 (0.000%) | 9 / 9 / 0 (0.000%) | 9 / 9 / 0 (0.000%) |
| sonar-xml-plugin/src/main/java/org/sonar/plugins/xml/package-info.java | 7 / 7 / 0 (0.000%) | 7 / 7 / 0 (0.000%) | 7 / 7 / 0 (0.000%) | 7 / 7 / 0 (0.000%) |

## Rules with findings

| Rule | Baseline | Source paths | Bytecode | Combined |
|---|---:|---:|---:|---:|
| java:S1128 | 0 | 1 | 1 | 1 |
| java:S2160 | 1 | 1 | 1 | 1 |
| java:S6204 | 2 | 3 | 3 | 3 |

596 configured rules had no findings in any mode and are omitted.

## Detailed differences

<details>
<summary>Source paths versus Baseline</summary>

### Largest semantic improvements

Top five by absolute change in unknown percentage.

| File | Second mode unknown % | First mode unknown % | Change (pp) |
|---|---:|---:|---:|
| sonar-xml-plugin/src/main/java/org/sonar/plugins/xml/checks/CheckList.java | 76.471% | 0.000% | -76.471 |
| sonar-xml-plugin/src/main/java/org/sonar/plugins/xml/checks/security/android/AndroidProviderPermissionCheck.java | 63.636% | 0.000% | -63.636 |
| sonar-xml-plugin/src/main/java/org/sonar/plugins/xml/checks/security/web/ValidationFiltersCheck.java | 39.744% | 0.000% | -39.744 |
| sonar-xml-plugin/src/main/java/org/sonar/plugins/xml/checks/security/android/AndroidCustomPermissionCheck.java | 38.462% | 0.000% | -38.462 |
| sonar-xml-plugin/src/main/java/org/sonar/plugins/xml/checks/security/android/AndroidComponentWithIntentFilterExportedCheck.java | 36.842% | 0.000% | -36.842 |

### Largest semantic regressions

Top five by absolute change in unknown percentage.

None.

| Unknown occurrences | Count |
|---|---:|
| Still unknown in both modes | 0 |
| No longer reported unknown | 385 |
| Newly reported unknown | 0 |

### No longer reported unknown

| File | Range | Identifier | AST context |
|---|---|---|---|
| sonar-xml-plugin/src/main/java/org/sonar/plugins/xml/LineCounter.java | (104:24)-(104:34) | splitLines | METHOD_INVOCATION |
| sonar-xml-plugin/src/main/java/org/sonar/plugins/xml/Xml.java | (64:73)-(64:82) | XmlPlugin | MEMBER_SELECT |
| sonar-xml-plugin/src/main/java/org/sonar/plugins/xml/Xml.java | (64:83)-(64:100) | FILE_SUFFIXES_KEY | MEMBER_SELECT |
| sonar-xml-plugin/src/main/java/org/sonar/plugins/xml/XmlHighlighting.java | (91:9)-(91:22) | isSelfClosing | METHOD_INVOCATION |
| sonar-xml-plugin/src/main/java/org/sonar/plugins/xml/XmlPlugin.java | (38:7)-(38:10) | Xml | MEMBER_SELECT |
| sonar-xml-plugin/src/main/java/org/sonar/plugins/xml/XmlPlugin.java | (39:7)-(39:25) | XmlRulesDefinition | MEMBER_SELECT |
| sonar-xml-plugin/src/main/java/org/sonar/plugins/xml/XmlPlugin.java | (40:7)-(40:25) | XmlSonarWayProfile | MEMBER_SELECT |
| sonar-xml-plugin/src/main/java/org/sonar/plugins/xml/XmlPlugin.java | (41:7)-(41:16) | XmlSensor | MEMBER_SELECT |
| sonar-xml-plugin/src/main/java/org/sonar/plugins/xml/XmlRulesDefinition.java | (34:57)-(34:60) | Xml | MEMBER_SELECT |
| sonar-xml-plugin/src/main/java/org/sonar/plugins/xml/XmlRulesDefinition.java | (34:61)-(34:75) | REPOSITORY_KEY | MEMBER_SELECT |
| sonar-xml-plugin/src/main/java/org/sonar/plugins/xml/XmlRulesDefinition.java | (34:77)-(34:80) | Xml | MEMBER_SELECT |
| sonar-xml-plugin/src/main/java/org/sonar/plugins/xml/XmlRulesDefinition.java | (34:81)-(34:84) | KEY | MEMBER_SELECT |
| sonar-xml-plugin/src/main/java/org/sonar/plugins/xml/XmlRulesDefinition.java | (34:86)-(34:93) | setName | MEMBER_SELECT |
| sonar-xml-plugin/src/main/java/org/sonar/plugins/xml/XmlRulesDefinition.java | (34:94)-(34:97) | Xml | MEMBER_SELECT |
| sonar-xml-plugin/src/main/java/org/sonar/plugins/xml/XmlRulesDefinition.java | (34:98)-(34:113) | REPOSITORY_NAME | MEMBER_SELECT |
| sonar-xml-plugin/src/main/java/org/sonar/plugins/xml/XmlRulesDefinition.java | (36:68)-(36:71) | Xml | MEMBER_SELECT |
| sonar-xml-plugin/src/main/java/org/sonar/plugins/xml/XmlRulesDefinition.java | (36:72)-(36:89) | XML_RESOURCE_PATH | MEMBER_SELECT |
| sonar-xml-plugin/src/main/java/org/sonar/plugins/xml/XmlRulesDefinition.java | (36:91)-(36:94) | Xml | MEMBER_SELECT |
| sonar-xml-plugin/src/main/java/org/sonar/plugins/xml/XmlRulesDefinition.java | (36:95)-(36:109) | SONAR_WAY_PATH | MEMBER_SELECT |
| sonar-xml-plugin/src/main/java/org/sonar/plugins/xml/XmlRulesDefinition.java | (39:61)-(39:70) | CheckList | MEMBER_SELECT |

365 additional occurrences omitted.


### Newly reported unknown

None.


Semantic graph location differences unavailable: traversal is DISABLED, PARTIAL, or UNKNOWN in at least one compared mode. Primary identifier and finding comparisons remain valid.

### Findings only in Baseline

None.

### Findings only in Source paths

| Rule | File | Line | Message |
|---|---|---:|---|
| java:S1128 | sonar-xml-plugin/src/main/java/org/sonar/plugins/xml/checks/security/android/AndroidComponentWithIntentFilterExportedCheck.java | 27 | Remove this unused import 'org.sonar.plugins.xml.checks.security.android.Utils.isAndroidManifestFile'. |
| java:S6204 | sonar-xml-plugin/src/main/java/org/sonar/plugins/xml/checks/security/android/AndroidProviderPermissionCheck.java | 61 | Replace this usage of 'Stream.collect(Collectors.toList())' with 'Stream.toList()' and ensure that the list is unmodified. |

</details>

<details>
<summary>Bytecode versus Baseline</summary>

### Largest semantic improvements

Top five by absolute change in unknown percentage.

| File | Second mode unknown % | First mode unknown % | Change (pp) |
|---|---:|---:|---:|
| sonar-xml-plugin/src/main/java/org/sonar/plugins/xml/checks/CheckList.java | 76.471% | 0.000% | -76.471 |
| sonar-xml-plugin/src/main/java/org/sonar/plugins/xml/checks/security/android/AndroidProviderPermissionCheck.java | 63.636% | 0.000% | -63.636 |
| sonar-xml-plugin/src/main/java/org/sonar/plugins/xml/checks/security/web/ValidationFiltersCheck.java | 39.744% | 0.000% | -39.744 |
| sonar-xml-plugin/src/main/java/org/sonar/plugins/xml/checks/security/android/AndroidCustomPermissionCheck.java | 38.462% | 0.000% | -38.462 |
| sonar-xml-plugin/src/main/java/org/sonar/plugins/xml/checks/security/android/AndroidComponentWithIntentFilterExportedCheck.java | 36.842% | 0.000% | -36.842 |

### Largest semantic regressions

Top five by absolute change in unknown percentage.

None.

| Unknown occurrences | Count |
|---|---:|
| Still unknown in both modes | 0 |
| No longer reported unknown | 385 |
| Newly reported unknown | 0 |

### No longer reported unknown

| File | Range | Identifier | AST context |
|---|---|---|---|
| sonar-xml-plugin/src/main/java/org/sonar/plugins/xml/LineCounter.java | (104:24)-(104:34) | splitLines | METHOD_INVOCATION |
| sonar-xml-plugin/src/main/java/org/sonar/plugins/xml/Xml.java | (64:73)-(64:82) | XmlPlugin | MEMBER_SELECT |
| sonar-xml-plugin/src/main/java/org/sonar/plugins/xml/Xml.java | (64:83)-(64:100) | FILE_SUFFIXES_KEY | MEMBER_SELECT |
| sonar-xml-plugin/src/main/java/org/sonar/plugins/xml/XmlHighlighting.java | (91:9)-(91:22) | isSelfClosing | METHOD_INVOCATION |
| sonar-xml-plugin/src/main/java/org/sonar/plugins/xml/XmlPlugin.java | (38:7)-(38:10) | Xml | MEMBER_SELECT |
| sonar-xml-plugin/src/main/java/org/sonar/plugins/xml/XmlPlugin.java | (39:7)-(39:25) | XmlRulesDefinition | MEMBER_SELECT |
| sonar-xml-plugin/src/main/java/org/sonar/plugins/xml/XmlPlugin.java | (40:7)-(40:25) | XmlSonarWayProfile | MEMBER_SELECT |
| sonar-xml-plugin/src/main/java/org/sonar/plugins/xml/XmlPlugin.java | (41:7)-(41:16) | XmlSensor | MEMBER_SELECT |
| sonar-xml-plugin/src/main/java/org/sonar/plugins/xml/XmlRulesDefinition.java | (34:57)-(34:60) | Xml | MEMBER_SELECT |
| sonar-xml-plugin/src/main/java/org/sonar/plugins/xml/XmlRulesDefinition.java | (34:61)-(34:75) | REPOSITORY_KEY | MEMBER_SELECT |
| sonar-xml-plugin/src/main/java/org/sonar/plugins/xml/XmlRulesDefinition.java | (34:77)-(34:80) | Xml | MEMBER_SELECT |
| sonar-xml-plugin/src/main/java/org/sonar/plugins/xml/XmlRulesDefinition.java | (34:81)-(34:84) | KEY | MEMBER_SELECT |
| sonar-xml-plugin/src/main/java/org/sonar/plugins/xml/XmlRulesDefinition.java | (34:86)-(34:93) | setName | MEMBER_SELECT |
| sonar-xml-plugin/src/main/java/org/sonar/plugins/xml/XmlRulesDefinition.java | (34:94)-(34:97) | Xml | MEMBER_SELECT |
| sonar-xml-plugin/src/main/java/org/sonar/plugins/xml/XmlRulesDefinition.java | (34:98)-(34:113) | REPOSITORY_NAME | MEMBER_SELECT |
| sonar-xml-plugin/src/main/java/org/sonar/plugins/xml/XmlRulesDefinition.java | (36:68)-(36:71) | Xml | MEMBER_SELECT |
| sonar-xml-plugin/src/main/java/org/sonar/plugins/xml/XmlRulesDefinition.java | (36:72)-(36:89) | XML_RESOURCE_PATH | MEMBER_SELECT |
| sonar-xml-plugin/src/main/java/org/sonar/plugins/xml/XmlRulesDefinition.java | (36:91)-(36:94) | Xml | MEMBER_SELECT |
| sonar-xml-plugin/src/main/java/org/sonar/plugins/xml/XmlRulesDefinition.java | (36:95)-(36:109) | SONAR_WAY_PATH | MEMBER_SELECT |
| sonar-xml-plugin/src/main/java/org/sonar/plugins/xml/XmlRulesDefinition.java | (39:61)-(39:70) | CheckList | MEMBER_SELECT |

365 additional occurrences omitted.


### Newly reported unknown

None.


Semantic graph location differences unavailable: traversal is DISABLED, PARTIAL, or UNKNOWN in at least one compared mode. Primary identifier and finding comparisons remain valid.

### Findings only in Baseline

None.

### Findings only in Bytecode

| Rule | File | Line | Message |
|---|---|---:|---|
| java:S1128 | sonar-xml-plugin/src/main/java/org/sonar/plugins/xml/checks/security/android/AndroidComponentWithIntentFilterExportedCheck.java | 27 | Remove this unused import 'org.sonar.plugins.xml.checks.security.android.Utils.isAndroidManifestFile'. |
| java:S6204 | sonar-xml-plugin/src/main/java/org/sonar/plugins/xml/checks/security/android/AndroidProviderPermissionCheck.java | 61 | Replace this usage of 'Stream.collect(Collectors.toList())' with 'Stream.toList()' and ensure that the list is unmodified. |

</details>

<details>
<summary>Combined versus Baseline</summary>

### Largest semantic improvements

Top five by absolute change in unknown percentage.

| File | Second mode unknown % | First mode unknown % | Change (pp) |
|---|---:|---:|---:|
| sonar-xml-plugin/src/main/java/org/sonar/plugins/xml/checks/CheckList.java | 76.471% | 0.000% | -76.471 |
| sonar-xml-plugin/src/main/java/org/sonar/plugins/xml/checks/security/android/AndroidProviderPermissionCheck.java | 63.636% | 0.000% | -63.636 |
| sonar-xml-plugin/src/main/java/org/sonar/plugins/xml/checks/security/web/ValidationFiltersCheck.java | 39.744% | 0.000% | -39.744 |
| sonar-xml-plugin/src/main/java/org/sonar/plugins/xml/checks/security/android/AndroidCustomPermissionCheck.java | 38.462% | 0.000% | -38.462 |
| sonar-xml-plugin/src/main/java/org/sonar/plugins/xml/checks/security/android/AndroidComponentWithIntentFilterExportedCheck.java | 36.842% | 0.000% | -36.842 |

### Largest semantic regressions

Top five by absolute change in unknown percentage.

None.

| Unknown occurrences | Count |
|---|---:|
| Still unknown in both modes | 0 |
| No longer reported unknown | 385 |
| Newly reported unknown | 0 |

### No longer reported unknown

| File | Range | Identifier | AST context |
|---|---|---|---|
| sonar-xml-plugin/src/main/java/org/sonar/plugins/xml/LineCounter.java | (104:24)-(104:34) | splitLines | METHOD_INVOCATION |
| sonar-xml-plugin/src/main/java/org/sonar/plugins/xml/Xml.java | (64:73)-(64:82) | XmlPlugin | MEMBER_SELECT |
| sonar-xml-plugin/src/main/java/org/sonar/plugins/xml/Xml.java | (64:83)-(64:100) | FILE_SUFFIXES_KEY | MEMBER_SELECT |
| sonar-xml-plugin/src/main/java/org/sonar/plugins/xml/XmlHighlighting.java | (91:9)-(91:22) | isSelfClosing | METHOD_INVOCATION |
| sonar-xml-plugin/src/main/java/org/sonar/plugins/xml/XmlPlugin.java | (38:7)-(38:10) | Xml | MEMBER_SELECT |
| sonar-xml-plugin/src/main/java/org/sonar/plugins/xml/XmlPlugin.java | (39:7)-(39:25) | XmlRulesDefinition | MEMBER_SELECT |
| sonar-xml-plugin/src/main/java/org/sonar/plugins/xml/XmlPlugin.java | (40:7)-(40:25) | XmlSonarWayProfile | MEMBER_SELECT |
| sonar-xml-plugin/src/main/java/org/sonar/plugins/xml/XmlPlugin.java | (41:7)-(41:16) | XmlSensor | MEMBER_SELECT |
| sonar-xml-plugin/src/main/java/org/sonar/plugins/xml/XmlRulesDefinition.java | (34:57)-(34:60) | Xml | MEMBER_SELECT |
| sonar-xml-plugin/src/main/java/org/sonar/plugins/xml/XmlRulesDefinition.java | (34:61)-(34:75) | REPOSITORY_KEY | MEMBER_SELECT |
| sonar-xml-plugin/src/main/java/org/sonar/plugins/xml/XmlRulesDefinition.java | (34:77)-(34:80) | Xml | MEMBER_SELECT |
| sonar-xml-plugin/src/main/java/org/sonar/plugins/xml/XmlRulesDefinition.java | (34:81)-(34:84) | KEY | MEMBER_SELECT |
| sonar-xml-plugin/src/main/java/org/sonar/plugins/xml/XmlRulesDefinition.java | (34:86)-(34:93) | setName | MEMBER_SELECT |
| sonar-xml-plugin/src/main/java/org/sonar/plugins/xml/XmlRulesDefinition.java | (34:94)-(34:97) | Xml | MEMBER_SELECT |
| sonar-xml-plugin/src/main/java/org/sonar/plugins/xml/XmlRulesDefinition.java | (34:98)-(34:113) | REPOSITORY_NAME | MEMBER_SELECT |
| sonar-xml-plugin/src/main/java/org/sonar/plugins/xml/XmlRulesDefinition.java | (36:68)-(36:71) | Xml | MEMBER_SELECT |
| sonar-xml-plugin/src/main/java/org/sonar/plugins/xml/XmlRulesDefinition.java | (36:72)-(36:89) | XML_RESOURCE_PATH | MEMBER_SELECT |
| sonar-xml-plugin/src/main/java/org/sonar/plugins/xml/XmlRulesDefinition.java | (36:91)-(36:94) | Xml | MEMBER_SELECT |
| sonar-xml-plugin/src/main/java/org/sonar/plugins/xml/XmlRulesDefinition.java | (36:95)-(36:109) | SONAR_WAY_PATH | MEMBER_SELECT |
| sonar-xml-plugin/src/main/java/org/sonar/plugins/xml/XmlRulesDefinition.java | (39:61)-(39:70) | CheckList | MEMBER_SELECT |

365 additional occurrences omitted.


### Newly reported unknown

None.


Semantic graph location differences unavailable: traversal is DISABLED, PARTIAL, or UNKNOWN in at least one compared mode. Primary identifier and finding comparisons remain valid.

### Findings only in Baseline

None.

### Findings only in Combined

| Rule | File | Line | Message |
|---|---|---:|---|
| java:S1128 | sonar-xml-plugin/src/main/java/org/sonar/plugins/xml/checks/security/android/AndroidComponentWithIntentFilterExportedCheck.java | 27 | Remove this unused import 'org.sonar.plugins.xml.checks.security.android.Utils.isAndroidManifestFile'. |
| java:S6204 | sonar-xml-plugin/src/main/java/org/sonar/plugins/xml/checks/security/android/AndroidProviderPermissionCheck.java | 61 | Replace this usage of 'Stream.collect(Collectors.toList())' with 'Stream.toList()' and ensure that the list is unmodified. |

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
| Still unknown in both modes | 0 |
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

None.

### Largest semantic regressions

Top five by absolute change in unknown percentage.

None.

| Unknown occurrences | Count |
|---|---:|
| Still unknown in both modes | 0 |
| No longer reported unknown | 0 |
| Newly reported unknown | 0 |

### No longer reported unknown

None.


### Newly reported unknown

None.


Semantic graph location differences unavailable: traversal is DISABLED, PARTIAL, or UNKNOWN in at least one compared mode. Primary identifier and finding comparisons remain valid.

### Findings only in Bytecode

None.

### Findings only in Combined

None.

</details>


## Measured timings

Warm-ups are excluded. JavaSensor time includes compilation.

| Sample | Mode | Maven/server wall (ms) | JavaSensor (ms) | Compilation (ms) | Compilation outcome | Generated classes |
|---|---|---:|---:|---:|---|---:|
| 1 | Baseline | 16006 | 6316.0 | 0.0 | DISABLED | 0 |
| 1 | Source paths | 16337 | 6915.0 | 0.0 | DISABLED | 0 |
| 1 | Bytecode | 17967 | 6821.0 | 412.0 | SUCCESS | 75 |
| 1 | Combined | 18046 | 7370.0 | 406.0 | SUCCESS | 75 |
| 2 | Baseline | 17809 | 6423.0 | 0.0 | DISABLED | 0 |
| 2 | Source paths | 17880 | 7339.0 | 0.0 | DISABLED | 0 |
| 2 | Bytecode | 18422 | 6970.0 | 428.0 | SUCCESS | 75 |
| 2 | Combined | 18164 | 7188.0 | 398.0 | SUCCESS | 75 |
| 3 | Baseline | 15952 | 6403.0 | 0.0 | DISABLED | 0 |
| 3 | Source paths | 17829 | 6915.0 | 0.0 | DISABLED | 0 |
| 3 | Bytecode | 16404 | 6846.0 | 412.0 | SUCCESS | 75 |
| 3 | Combined | 18075 | 7164.0 | 404.0 | SUCCESS | 75 |

## Run metadata

| Setting | Value |
|---|---|
| Baseline properties | {sonar.java.compileToByteCode=false, sonar.java.experimental.batchModeSizeInKB=500, sonar.java.fileByFile=true, sonar.java.sourcepath=, sonar.java.test.sourcepath=} |
| Baseline warm-up status | SUCCESS |
| Bytecode properties | {sonar.java.compileToByteCode=true, sonar.java.experimental.batchModeSizeInKB=500, sonar.java.fileByFile=true, sonar.java.sourcepath=, sonar.java.test.sourcepath=} |
| Bytecode warm-up status | SUCCESS |
| Combined properties | {sonar.java.compileToByteCode=true, sonar.java.experimental.batchModeSizeInKB=500, sonar.java.fileByFile=true, sonar.java.sourcepath=sonar-xml-plugin/src/main/java, sonar.java.test.sourcepath=} |
| Combined warm-up status | SUCCESS |
| External library count | 8 |
| Graph diagnostic stability | stable |
| Repeated result stability | stable |
| Scenario | Each production file is parsed separately; every mode receives the same compile-scope external JARs |
| Shared scenario properties | {sonar.java.experimental.batchModeSizeInKB=500, sonar.java.fileByFile=true} |
| Source paths properties | {sonar.java.compileToByteCode=false, sonar.java.experimental.batchModeSizeInKB=500, sonar.java.fileByFile=true, sonar.java.sourcepath=sonar-xml-plugin/src/main/java, sonar.java.test.sourcepath=} |
| Source paths warm-up status | SUCCESS |

</details>


## Run metadata

| Setting | Value |
|---|---|
| Analyzer build manifest revision | c7c2be4f14eca303fbc6c23bd0d24637ef89307b |
| Analyzer checkout dirty | false |
| Analyzer checkout revision | c7c2be4f14eca303fbc6c23bd0d24637ef89307b |
| Analyzer plugin SHA-256 | 811099f36973f1abe9b043d8adabc8c8c21b75516214afae2094f9f9edaaf73d |
| Analyzer plugin path | /private/tmp/sonar-java-sourcepath-evaluation/sonar-java-plugin/target/sonar-java-plugin-8.45.0-SNAPSHOT.jar |
| Analyzer plugin version | 8.45.0-SNAPSHOT |
| Binding probe plugin SHA-256 | bda871482ab0648e19e2bcaca1e9cb0b7c8004116e68c3a80548d12761ca16c7 |
| Clean compilation/Baseline bindings | Semantic binding probes: 4 checked, 0 failed. |
| Clean compilation/Bytecode bindings | Semantic binding probes: 4 checked, 0 failed. |
| Clean compilation/Combined bindings | Semantic binding probes: 4 checked, 0 failed. |
| Clean compilation/Source paths bindings | Semantic binding probes: 4 checked, 0 failed. |
| Compilation failure recovery/Baseline bindings | Semantic binding probes: 5 checked, 0 failed. |
| Compilation failure recovery/Bytecode bindings | Semantic binding probes: 5 checked, 0 failed. |
| Compilation failure recovery/Combined bindings | Semantic binding probes: 5 checked, 0 failed. |
| Compilation failure recovery/Source paths bindings | Semantic binding probes: 5 checked, 0 failed. |
| Compilation measurements | Evaluation-only structured log hook records outcome, source/class counts, and elapsed time before cleanup |
| Compile classpath SHA-256 (ordered) | ca8474d062885faaec5827335c183865cc4051003e274a321c3836ed16241e05 |
| Compile dependency 0 | 00-sonar-analyzer-commons-2.33.0.5369.jar / SHA-256 7e1974e0a661b67b105618eadee240b4987461297ffffe0262585f3709c624cf |
| Compile dependency 1 | 01-sonar-plugin-api-14.1.0.5543.jar / SHA-256 ac70438a14ba2de9459b8c4d8662396c4ff9c5917166dccf5faf30500f322f31 |
| Compile dependency 2 | 02-slf4j-api-2.0.18.jar / SHA-256 44508fd1576500688c790b190acdd16fec4f8c79a3e0b900afd70503cf055f55 |
| Compile dependency 3 | 03-jsr305-3.0.2.jar / SHA-256 766ad2a0783f2687962c8ad74ceecc38a28b9f72a2d085ee438b7813e928d0c7 |
| Compile dependency 4 | 04-sonar-xml-parsing-2.33.0.5369.jar / SHA-256 0ab7dbea474a67e6ac99c36828181c95721acccfddddde705fdceddb82b9deae |
| Compile dependency 5 | 05-xercesImpl-2.12.2.jar / SHA-256 6fc991829af1708d15aea50c66f0beadcd2cfeb6968e0b2f55c1b0909883fe16 |
| Compile dependency 6 | 06-woodstox-core-7.2.2.jar / SHA-256 3bd35f778fb15cd517fae51961ba7a9388eae8e4a836fac1166c7f70740e55cd |
| Compile dependency 7 | 07-stax2-api-4.3.0.jar / SHA-256 7c805f36129ea9fa42b696093b7ae1eb20bb6ccec65c8280d6f33db5609ca5e1 |
| Dependency outside analysis/Baseline bindings | Semantic binding probes: 4 checked, 0 failed. |
| Dependency outside analysis/Bytecode bindings | Semantic binding probes: 4 checked, 0 failed. |
| Dependency outside analysis/Combined bindings | Semantic binding probes: 4 checked, 0 failed. |
| Dependency outside analysis/Source paths bindings | Semantic binding probes: 4 checked, 0 failed. |
| Dependency resolution | Pinned dependency:build-classpath on copied POMs; compile scope only; no Maven compilation |
| Dependency resolution time (ms), excluded from scans | 1398 |
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
| Measured repetitions per mode | 3 |
| PR #6308 revision | 7068bcd4c102a0d175db7221785660f685f238ea |
| PR #6309 revision | 6486014b40f0bbbeb6e597976a1505287df66240 |
| Per-file measurements | Additive module.files observations preserve latest canonical identifier and graph fields |
| Profile | Sonar way |
| Recorded at (UTC) | 2026-10-07T13:06:21.130061Z |
| Semantic reporter revision | ea7e66906d101c33502fb6df9de305e09fc6f4c5 |
| Server version | 26.10.0.132816 |
| Server workspace | /Users/matthew.elliott/Work/Code/sonar-java/its/plugin/tests/target/comparison-orchestrator-829356208127292802 |
| Target checkout dirty | false |
| Target checkout revision | 627ac4747bef73345338aae435b7c987873cf86f |
| Target source snapshot SHA-256 | 9ac9e9d9cc60dfc68a4e174741e5db76abcee26dbc7ecd41a72af5bd38303586 |
| Test harness dirty | true |
| Test harness revision | 8cfaa60de2437a6d5ac15519a1d584beacd8b49f |
| Timing protocol | one warm-up per mode excluded; measured order rotates and reverses across repetitions |
| compilation-measurements.patch SHA-256 | 8ffdee83415d887ece4adb49ccc6279da5584f5941a50f8fdbd75a6e133ead9d |
| semantic-file-measurements.patch SHA-256 | d13fc66bdbf107bafad98b480af2bbfa749d2ddd21e04d0e65737d85706b50d2 |
| semantic-graph-bounds.patch SHA-256 | fbad6b930545a0c4094fd295859affa050692265ef8e2ef9190da4199b4f272b |

## Reading the comparison

- Identifier percentages use aggregate counts across analyzed files. Same-dataset comparisons require successful scans, identical indexed and semantic file coverage, and identical per-file identifier totals. Unstable repetitions have no comparison metrics.
- Compilation SUCCESS or FAILED is measured separately from scan success. Missing measurements are UNAVAILABLE or N/A. Failed compilation can leave partial classes that are available during analysis; generated classes must be removed afterward.
- JavaSensor time includes internal compilation; Maven/server time also includes scanner startup and server processing. All timing medians exclude warm-ups. A few repetitions describe this run and do not establish a general performance ranking.
- Product findings include only `java:` rules. Probe findings are instrumentation. Findings on real projects measure agreement, while the explicitly reviewed fixture checks binding correctness and expected findings.
- Unknown identifier occurrences match by file, name, and token range. Fewer unknowns alone do not prove correct bindings. AST context is diagnostic rather than a semantic role.
- Semantic graph counts describe unique references reached by the target branch's bounded traversal. Resolving a name can expose additional graph nodes, so these counts and location differences are diagnostics rather than coverage percentages or correctness scores.
- Source paths do not expand analyzed scope. Internal compilation disables annotation processing. External libraries, parser batching, source files, JDK, profile, and extra properties are held fixed across each dataset's four modes.
