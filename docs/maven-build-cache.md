# Apache Maven build cache

The repository has no Maven wrapper. It uses Maven 3.9 (`mise.toml`, currently 3.9.16) and JDK 26 for the ordinary CI build. Apache Maven Build Cache Extension 1.3.0 requires Maven 3.9 or later. Its [getting started](https://maven.apache.org/extensions/maven-build-cache-extension/), [concepts](https://maven.apache.org/extensions/maven-build-cache-extension/concepts.html), [recipes](https://maven.apache.org/extensions/maven-build-cache-extension/how-to.html), [usage](https://maven.apache.org/extensions/maven-build-cache-extension/usage.html), [remote cache](https://maven.apache.org/extensions/maven-build-cache-extension/remote-cache.html), [parameters](https://maven.apache.org/extensions/maven-build-cache-extension/parameters.html), and [XML reference](https://maven.apache.org/extensions/maven-build-cache-extension/build-cache-config.html) were checked for 1.3.0. Its configuration uses the published 1.2.0 XML schema.

## Scope

The extension stores and restores module build results. It does not select individual tests or cache each lifecycle phase independently. The 13 top-level modules include analyzer libraries, test-source corpus modules, the packaged plugin, Surefire support, documentation, and integration projects. Normal verification is `mvn clean verify --batch-mode`. Surefire's actual skip and failure-ignore properties are explicit in its POM configuration, so their resolved values contribute to the effective-POM fingerprint. This also separates `maven.test.skip`, which controls test compilation. A `-Dtest=...` selection disables Apache lookup and save for that build, avoiding a partial-test cache entry. The extension cannot reflect Surefire 3.5.2's skip fields in this build, and its published 1.2.0 XSD rejects the generated reference's expression-based reconciliation; these POM settings avoid both problems. Test reports, compiled classes, generated sources and resources, copied test jars, classpath files, and the license validator's temporary files are attached outputs. The complete `src/main` and `src/test` trees are fingerprinted, including legacy `src/test/files`, `src/test/jdk`, and generator inputs. The root license and plugin license resources are included explicitly. Frontend, checks, AWS checks, and testkit also fingerprint the sibling test-source corpus they read directly; a Java sample's comments can affect a test even when its compiled dependency artifact is unchanged. Surefire `argLine` remains in the effective POM and is never excluded from hashing. The compiler release is explicit in the inherited compiler plugin configuration so `-Dmaven.compiler.release` changes the cache key. The `apache-build-cache` profile fixes `Implementation-Build` in library JARs and the inherited packaging plugin's `Build-Time` to `_` only when Apache caching is explicitly enabled. Those fields otherwise change with the commit or clock. The packaged plugin always rebuilds because its `Plugin-BuildDate` is dynamic; release builds with caching disabled retain their original metadata. Develocity remains configured for existing builds; the Apache opt-in runs below disable its cache for isolation.

The packaged plugin is never restored or saved because its manifest includes the current build date. The `its` subtree is never restored or saved because its profiles run service-backed tests and download a JRE. The plugin's license download and validation executions run on every build; they are also listed as always-run goals. Install and deploy goals always run. CI additionally disables Apache caching in the deployment, ruling, plugin QA, sanity, and custom-rules jobs. Their existing goals and test profiles are unchanged. The Windows `qa-os-win` job keeps `mvn clean verify --batch-mode` and explicitly disables Develocity's local and remote caches during that Apache cache run. Its `config-maven` setting only disables the dependency cache step in that action.

## CI sharing and trust

The Windows job restores `~/.m2/build-cache` using the existing GitHub Actions cache service and its ephemeral workflow token; no Maven cache credential is committed. A successful `master` push from this repository saves it. Pull requests only restore; fork code has no Apache cache-save step. Keys separate extension version, OS, architecture, the exact Java/Maven runtime identity, and extension configuration, while the extension's module fingerprints cover source contents, dependencies, effective POM, profiles, and the explicitly configured compiler and Surefire parameters. The restore prefix lets a new commit retrieve prior entries within the same environment and cache configuration; each module still must pass fingerprint checks. Fresh runners fetch this archive before Maven starts. A cache miss runs the full Windows verification command with Apache enabled and Develocity cache disabled. Backend outage behavior has not been exercised in CI; do not assume an action transport error is equivalent to a cache miss.

The extension's native remote transport is **not configured**. If an approved Maven Resolver-compatible HTTP GET/HEAD/PUT service becomes available, supply `-Dmaven.build.cache.remote.url=https://<approved-cache-host>/<namespace>` and `-Dmaven.build.cache.remote.server.id=<settings-server-id>` from CI, with credentials in Maven `settings.xml` through the existing secret mechanism. The remote namespace must be partitioned by JDK and OS, as the CI archive key is. Enable reads with `-Dmaven.build.cache.remote.enabled=true`; allow writes only in a trusted job with `-Dmaven.build.cache.remote.save.enabled=true`, and use `false` in pull requests. Do not commit credentials or infer that an HTTP failure falls back safely without testing it. A second clean checkout with an empty local cache and remote writes disabled is required before claiming native remote reuse.

## Local verification and rollback

Apache caching is disabled by default. Opt in with `-Dmaven.build.cache.enabled=true` and use a separate cache for each JDK and OS when switching environments locally. The documented override is `-Dmaven.build.cache.location=/absolute/path`. `-Dmaven.build.cache.enabled=false` disables Apache caching. Also pass `-Ddevelocity.cache.local.enabled=false -Ddevelocity.cache.remote.enabled=false` for a genuinely uncached reference in this repository. Release/reference builds should use those switches; the existing Develocity extension remains installed.

Inspect `Found cached build` and `Local build was not found` lines and `buildinfo.xml` under the isolated cache to diagnose unexpected hits or misses. Check `target/surefire-reports` after a clean hit before relying on report collectors. The cache does not restore every `target` file; add an attached output only after identifying a consumer. Compare artifact contents with an uncached build while accounting for archive timestamps and other nondeterministic metadata. A cache archive failure, missing credentials, or a changed checkout line ending can prevent reuse. The native remote cache does not normalize CRLF and LF.

## Reproducible isolation procedure

Run from the repository root. Use a disposable worktree; only edit files there. Preserve `/tmp/maven-cache-validation-logs` or choose another output directory for the logs. The following base commands match the CI verification goal; the added properties isolate both cache engines. Do not run `install` or `deploy` during this procedure.

```bash
validation_root="$(mktemp -d)"
git worktree add --detach "$validation_root/worktree" HEAD
cp .mvn/extensions.xml .mvn/maven-build-cache-config.xml "$validation_root/worktree/.mvn/"
cp pom.xml "$validation_root/worktree/pom.xml"
cp sonar-java-plugin/pom.xml "$validation_root/worktree/sonar-java-plugin/pom.xml"
cp its/pom.xml "$validation_root/worktree/its/pom.xml"
cp java-frontend/pom.xml "$validation_root/worktree/java-frontend/pom.xml"
cp java-checks/pom.xml "$validation_root/worktree/java-checks/pom.xml"
cp java-checks-aws/pom.xml "$validation_root/worktree/java-checks-aws/pom.xml"
cp java-checks-testkit/pom.xml "$validation_root/worktree/java-checks-testkit/pom.xml"
cd "$validation_root/worktree"
common=(-Ddevelocity.cache.local.enabled=false -Ddevelocity.cache.remote.enabled=false "-Dmaven.build.cache.location=$validation_root/cache")
run_logged() {
  label="$1"
  shift
  "$@" > "$validation_root/$label.log" 2>&1
  printf '%s\n' "$?" > "$validation_root/$label.exit"
}
run_logged A mvn clean verify --batch-mode "${common[@]}" -Dmaven.build.cache.enabled=false
run_logged B mvn clean verify --batch-mode "${common[@]}" -Dmaven.build.cache.enabled=true
run_logged C mvn clean verify --batch-mode "${common[@]}" -Dmaven.build.cache.enabled=true
```

Check the `.exit` files, reactor and cache-restore lines in every log, plus Surefire execution and `target/surefire-reports`. Stop if A, B, or C fails. The following edits affect only the disposable worktree. Keep the same Maven command and flags for all cache-enabled verification runs.

```bash
source_file=java-surefire/src/main/java/org/sonar/plugins/surefire/data/UnitTestResult.java
test_file=java-surefire/src/test/java/org/sonar/plugins/surefire/data/UnitTestResultTest.java
sample_file=java-checks-test-sources/default/src/main/java/checks/AccessibilityChangeOnRecordsCheckSample.java
fixture_file=java-surefire/src/test/resources/org/sonar/plugins/surefire/data/SurefireStaxHandlerTest/zeroTests.xml
printf '\n' >> "$source_file"
run_logged D-source mvn clean verify --batch-mode "${common[@]}" -Dmaven.build.cache.enabled=true
git restore -- "$source_file"
python3 - "$sample_file" <<'PY'
from pathlib import Path
import sys
p = Path(sys.argv[1])
p.write_text(p.read_text().replace('// Noncompliant', '// Compliant', 1))
PY
run_logged D-consumer mvn clean verify --batch-mode "${common[@]}" -Dmaven.build.cache.enabled=true
git restore -- "$sample_file"
python3 - "$test_file" <<'PY'
from pathlib import Path
import sys
p = Path(sys.argv[1])
p.write_text(p.read_text().replace('assertThat(result.isError()).isTrue();', 'assertThat(result.isError()).isFalse();', 1))
PY
run_logged E-test mvn clean verify --batch-mode "${common[@]}" -Dmaven.build.cache.enabled=true
git restore -- "$test_file"
python3 - "$fixture_file" <<'PY'
from pathlib import Path
import sys
p = Path(sys.argv[1])
p.write_text(p.read_text().replace('</testsuite>', '<testcase name="cache-validation" classname="Foo"/></testsuite>', 1))
PY
run_logged E-fixture mvn clean verify --batch-mode "${common[@]}" -Dmaven.build.cache.enabled=true
git restore -- "$fixture_file"
```

For poisoning, reapply the failing test edit above with the same Python replacement command and keep the file unchanged between each pair of runs. Use **separate empty cache directories** for `skipTests` and `maven.test.skip` (the latter is used by the CI build job). Each required-test rerun must fail. No Failsafe execution or failure-ignore flag is configured in today's CI jobs.

```bash
skip_cache="-Dmaven.build.cache.location=$validation_root/skip-cache"
run_logged F-skip-populate mvn clean verify --batch-mode "${common[@]:0:2}" "$skip_cache" -Dmaven.build.cache.enabled=true -DskipTests=true
run_logged F-skip-required mvn clean verify --batch-mode "${common[@]:0:2}" "$skip_cache" -Dmaven.build.cache.enabled=true
compiler_skip_cache="-Dmaven.build.cache.location=$validation_root/compiler-skip-cache"
run_logged F-compiler-populate mvn clean verify --batch-mode "${common[@]:0:2}" "$compiler_skip_cache" -Dmaven.build.cache.enabled=true -Dmaven.test.skip=true
run_logged F-compiler-required mvn clean verify --batch-mode "${common[@]:0:2}" "$compiler_skip_cache" -Dmaven.build.cache.enabled=true
git restore -- "$test_file"
run_logged G-compiler mvn clean verify --batch-mode "${common[@]}" -Dmaven.build.cache.enabled=true -Dmaven.compiler.release=22
run_logged I-disabled mvn clean verify --batch-mode "${common[@]}" -Dmaven.build.cache.enabled=false
```

Inspect `G-compiler.log` for changed module keys and real compilation. Keep distinct cache namespaces across JDK and OS changes; a cross-JDK test requires a supported second runtime and its own namespace. Compare the plugin JAR's entry contents after B, C, and I, accounting for timestamps and other metadata. The native remote checkout test requires an approved endpoint and is not represented by copying a local directory. After checking that `validation_root` is the directory created above, leave the disposable worktree and remove it with `git worktree remove --force "$validation_root/worktree"`. Remove only that newly created cache directory if desired; never delete the normal Maven repository.

## Results

The commands below use `mvn clean verify --batch-mode`, the two Develocity-disable properties, and the isolated cache location shown in the procedure. Logs are under `/tmp/maven-cache-validation-logs`; a changed compiler fingerprint was corrected before the final cold, warm, compiler, and disabled runs. The full poisoning pair preceded that compiler-only correction and was repeated on the final POM in a focused reactor.

| Scenario | Command addition | Expected | Observed evidence | Status |
| --- | --- | --- | --- | --- |
| A. Reference | `-Dmaven.build.cache.enabled=false` | All checks execute | Exit 0; 381 goals executed | Pass |
| B. Cold | `-Dmaven.build.cache.enabled=true` with empty cache | Misses and population | Exit 0; 20 module misses; 381 goals executed | Pass |
| C. Warm after clean | Same as B | Safe module hits; needed checks remain | Exit 0; 20 module hits; 124 goals executed; plugin and integration modules rebuilt; 809 `java-checks` Surefire XML reports restored | Pass |
| D. Production source | Same as B, with temporary `java-surefire` source edit | Changed key; other modules reusable | Exit 0; `java-surefire` missed while unrelated modules hit | Pass |
| D. Shared consumer input | Same as B, with temporary sibling sample edit | Consumer reruns and detects change | Exit 1; sample, frontend, and checks keys changed; `AccessibilityChangeOnRecordsCheckTest` failed | Pass |
| E. Java test | Same as B, with temporary failing test | Required test runs and fails | Exit 1; `java-surefire` missed; `UnitTestResultTest` ran | Pass |
| E. XML fixture | Same as B, with temporary failing fixture | Required test runs and fails | Exit 1; `java-surefire` missed; `SurefireStaxHandlerTest` ran | Pass |
| F. Skip poisoning | Separate empty cache: `-Dmaven.test.skip=true`, then no skip flag with unchanged failing test | Skipped entry cannot satisfy required tests | Exits 0 then 1; `java-surefire` key changed from `774209abf0cb60df` to `532729fe162c5e68`; failing test ran | Pass |
| F. Other skip flag | Separate empty cache: `-DskipTests=true`, then no skip flag | Same protection | Focused reactor exited 0 then 1; failing test ran | Pass |
| G. Compiler parameter | `-Dmaven.compiler.release=22` | Incompatible module result rejected | Exit 0; `java-frontend` missed and compiled with release 22; child modules with fixed releases could hit | Pass |
| H. Fresh runner | CI archive restore or native remote with empty local cache | Reuse from shared backend | No GitHub Actions runner or approved native endpoint available locally | Not run |
| I. Disable and compare | `-Dmaven.build.cache.enabled=false` after restoring edits | Full checks and equivalent functional artifacts | Exit 0; 381 goals executed; 1,216 checks and 8,230 plugin class entries matched cached JARs byte for byte; only manifests differed | Pass |
| Cross-JDK | Same goal on another supported JDK and separate namespace | Environment separation | Full reactor requires the configured JDK 26 test corpus; another full-JDK run was not performed | Not run |

The Windows CI archive path and a fresh checkout restore remain unverified until a trusted CI run seeds an archive and a later job restores it. Native remote transport is unconfigured. Local hits prove only local reuse.
