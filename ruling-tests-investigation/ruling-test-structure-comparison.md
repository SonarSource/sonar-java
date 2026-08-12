# Ruling Test Structure Comparison Across Analyzers

## Overview

This report checks whether the sonar-java ruling test pattern generalizes to all other
Orchestrator+LITS-based analyzers. The sonar-java pattern is:

1. A ruling test class analyzes a fixed set of real projects
2. Test sources are checked out under `its/sources/` (git submodule)
3. Expected issues are stored in `its/ruling/src/test/resources/<projectName>/`
4. One golden file per rule, named `java-<ruleKey>.json`
5. JSON keys are `"<sonar.projectKey>:<relative path>"` mapping to `[lineNumbers]`
6. Keys without a file path suffix represent project-level issues (line `0`)

## Detailed Findings by Repository

Repos marked with **(local)** were verified by reading actual golden files. Others are based
on the existing investigation files in this directory (which may lack golden file format detail
for repos without a local checkout).

### sonar-java (local) — Reference Pattern

- **Test class:** `its/ruling/src/test/java/org/sonar/java/it/JavaRulingTest.java`
- **Sources:** Git submodule `its/sources` → `SonarSource/ruling_java`
- **Golden files:** `its/ruling/src/test/resources/<projectName>/java-<ruleKey>.json`
- **Directory structure:** One subdirectory per analyzed project (`guava/`, `mall/`, etc.)
- **Key format:** `"com.google.guava:guava:src/com/google/common/hash/Hashing.java": [101, 113]`
  - Keys use the `sonar.projectKey` set in the test code. Multi-segment Maven coordinates
    like `com.google.guava:guava` become part of the key.
- **Project-level issues:** Yes. S1228 ("Every source file should have a package declaration")
  produces `"jboss-ejb3-tutorial": [0, 0, ...]` — key is just the project key, no file path.
  Line `0` means the issue is attached to the file/project as a whole.
- **Quotes:** Double quotes in JSON

### SonarJS (local)

- **Test class:** `its/ruling/src/test/java/org/sonar/javascript/it/RulingTest.java`
- **Sources:** Git submodule `its/sources/projects` → `SonarSource/jsts-test-sources`
- **Golden files:** `its/ruling/src/test/expected/<projectName>/<language>-<ruleKey>.json`
  - Note: `src/test/expected/` not `src/test/resources/expected/`
- **Directory structure:** One subdirectory per project (`jquery/`, `Ghost/`, `TypeScript/`, etc.)
  — 57 project directories
- **Key format:** `"jquery:src/deferred.js": [52]`
  - Same `projectKey:relativePath` pattern. Project key matches the directory name.
- **Language prefixes:** `javascript-` and `typescript-` (two languages in one analyzer)
- **Quotes:** Double quotes

### sonar-kotlin (local)

- **Test class:** `its/ruling/src/test/java/org/sonarsource/slang/SlangRulingTest.java`
- **Sources:** Git submodule `its/sources` → `SonarSource/slang-test-sources`
- **Golden files:** `its/ruling/src/test/resources/expected/kotlin/<corpusName>/kotlin-<ruleKey>.json`
  - Nested: `expected/kotlin/` then per-corpus directories (`kotlin/`, `corda/`, `okio/`, etc.)
- **Directory structure:** One subdirectory per corpus inside `expected/kotlin/`
- **Key format:** `"kotlin-kotlin-project:sources/kotlin/kotlin/compiler/backend/src/.../File.kt": [192]`
  - Same `projectKey:relativePath` pattern. Paths include `sources/kotlin/` prefix.
- **Quotes:** Double quotes

### sonar-ruby (local)

- **Test class:** `its/ruling/src/test/java/org/sonarsource/slang/SlangRulingTest.java`
  (same class name as sonar-kotlin — shared Slang framework)
- **Sources:** Git submodule `its/sources` → `SonarSource/slang-test-sources` (same as kotlin)
- **Golden files:** `its/ruling/src/test/resources/expected/ruby/ruby-<ruleKey>.json`
  - Flat: all golden files in a single `expected/ruby/` directory (no per-project split)
- **Key format:** `'ruby-project:sources/ruby/vagrant/lib/vagrant/bundler.rb': [80]`
  - Same pattern but **single quotes** in JSON (non-standard but accepted by LITS)
- **Quotes:** Single quotes

### sonar-apex (local)

- **Test class:** `its/ruling/src/test/java/com/sonarsource/apex/it/ApexRulingTest.java`
- **Sources:** Git submodule `its/sources` → `SonarSource/apex-test-sources`
- **Golden files:** `its/ruling/src/test/resources/expected/apex/apex-<ruleKey>.json`
  - Flat: all golden files in a single `expected/apex/` directory
- **Key format:** `'apex-project:sources/projects/Batch-Entry-for-Salesforce.com/src/.../File.cls': [76]`
- **Quotes:** Single quotes

### sonar-xml (local)

- **Test class:** `its/ruling/src/test/java/org/sonarsource/xml/it/XmlRulingTest.java`
- **Sources:** **Committed directly** in repo (no submodule)
- **Golden files:** `its/ruling/src/test/resources/expected/xml-<ruleKey>.json`
  - Flat: all golden files in a single `expected/` directory (no language subdirectory)
- **Key format:** `'project:jboss-ejb3-tutorial/common/pom.xml': [7, 7, 7]`
  - Same pattern. Project key is simply `project`.
- **Quotes:** Single quotes

### sonar-python-enterprise (local)

- **Test class:** `private/its-enterprise/ruling/src/test/java/org/sonar/python/it/PythonRulingTest.java`
- **Sources:** Git submodule `private/its-enterprise/sources_ruling` → `SonarSource/python-test-sources`
  + committed internal sources in `sources_internal_ruling/`
- **Golden files:** `private/its-enterprise/ruling/src/test/resources/expected_ruling/`
  - Flat: all golden files in a single directory (no per-project split)
- **Filename prefixes:** `python-`, `pythonenterprise-`, `ipython-` (three rule repositories)
- **Key format:** `"airflow:airflow/cli/cli_parser.py": [0]`
  - Same pattern. Project key matches the test method name.
- **Project-level issues:** Not found in sampled files
- **Quotes:** Double quotes

### Remaining analyzers (from investigation files, no local checkout)

These findings come from the existing `.md` investigation files. The golden file key format
was not always verified by reading actual files.

| Repository | Test Class | Sources | Golden File Location | Filename Pattern | Source Management |
|---|---|---|---|---|---|
| sonar-php | `PhpRulingTest.java` | `its/sources/` | `its/ruling/.../expected/` | `php-<ruleKey>.json` | Git submodule |
| sonar-cobol | `its/ruling/` module | `its/sources/` | `its/ruling/.../expected/` | `cobol-<ruleKey>.json` | Git submodule |
| sonar-plsql | `PlSqlRulingTest.java` | `its/sources/` | `its/ruling/.../expected/` | `plsql-<ruleKey>.json` | **Committed** |
| sonar-tsql | `TsqlRulingTest.java` | `its/sources/` | `its/ruling/.../expected/` | `tsql-<ruleKey>.json` | **Committed** |
| sonar-vb | `VbRulingTest.java` | `its/sources/` | `its/ruling/.../expected/` | `vb-<ruleKey>.json` | **Committed** |
| sonar-jcl | `RulingTest.java` | `its/sources/` | `its/ruling/.../expected/<projectKey>/` | `jcl-<ruleKey>.json` | Git submodule |
| sonar-flex | `FlexRulingTest.java` | `its/sources/` | `its/ruling/.../expected/` | `flex-<ruleKey>.json` | Git submodule |
| sonar-html | `WebRulingTest.java` | `its/sources/` | `its/ruling/.../expected/` | `Web-<ruleKey>.json` | Git submodule |
| sonar-css | `its/ruling/` module | — | — | — | Git submodule |

## Pattern Comparison

### What is universal

These elements hold across **all** Orchestrator+LITS-based analyzers:

1. **One golden file per rule**, named `<language>-<ruleKey>.json`
2. **JSON maps component keys to arrays of line numbers**
3. **Component keys follow `projectKey:relativePath` format**
4. **Test sources under `its/sources/`** (whether submodule or committed)
5. **LITS plugin handles the diff** — tests assert the diff file is empty

### What varies

| Aspect | sonar-java | Most others | Notes |
|---|---|---|---|
| **Golden file directory structure** | Per-project subdirs (`guava/`, `mall/`) | Flat or per-language (`expected/ruby/`) | sonar-java and SonarJS use per-project dirs; most others use flat dirs |
| **Golden file base path** | `src/test/resources/` (direct) | `src/test/resources/expected/` | sonar-java is the exception — no `expected/` subdirectory |
| **SonarJS golden file path** | — | `src/test/expected/` | SonarJS doesn't use `resources/` at all |
| **JSON quotes** | Double quotes | Mixed (single or double) | sonar-xml, sonar-ruby, sonar-apex use single quotes. LITS accepts both. |
| **Source management** | Submodule | Mixed | sonar-xml, sonar-plsql, sonar-tsql, sonar-vb commit sources directly |
| **Project-level issues** | Yes (S1228: `"projectKey": [0]`) | Not found in others | sonar-java is the only one where I found keys without a file path suffix |
| **Language prefix in filename** | `java-` | Language-specific | SonarJS uses both `javascript-` and `typescript-` prefixes |
| **sonar-python-enterprise prefixes** | — | `python-`, `pythonenterprise-`, `ipython-` | Three rule repositories produce golden files with different prefixes |
| **sonar-html filename** | — | `Web-<ruleKey>.json` | Uses `Web` (capital W) instead of `html` as the language prefix |
| **sonar-jcl directory structure** | — | `expected/<projectKey>/jcl-<ruleKey>.json` | Per-project subdirectories inside `expected/`, like sonar-java |

### Project key in golden file keys

The project key embedded in golden file JSON keys comes from the `sonar.projectKey` property
set in the test code. This varies by analyzer:

| Analyzer | Example key | Project key style |
|---|---|---|
| sonar-java | `"com.google.guava:guava:src/..."` | Maven groupId:artifactId |
| sonar-java | `"jboss-ejb3-tutorial:blob/src/..."` | Simple name |
| SonarJS | `"jquery:Gruntfile.js"` | Simple name |
| sonar-kotlin | `"kotlin-kotlin-project:sources/kotlin/..."` | Prefixed name |
| sonar-ruby | `"ruby-project:sources/ruby/..."` | Prefixed name |
| sonar-apex | `"apex-project:sources/projects/..."` | Prefixed name |
| sonar-xml | `"project:jboss-ejb3-tutorial/..."` | Generic `project` |
| sonar-python-enterprise | `"airflow:airflow/cli/..."` | Simple name |

The key prefix is whatever `sonar.projectKey` was set to in the test — there's no standardized
convention across analyzers.

## Conclusion

The sonar-java ruling test pattern **largely generalizes** to all other analyzers, with the
following caveats:

- The **golden file directory organization** varies: sonar-java and SonarJS use per-project
  subdirectories, while most others use a flat directory (sometimes with a language
  subdirectory like `expected/ruby/`).
- The **JSON key format** (`projectKey:relativePath → [lines]`) is universal, but the
  project key string and quoting style vary.
- **Project-level issues** (keys without a file path, line 0) were only found in sonar-java
  (rule S1228). Other analyzers may not have rules that produce project-level issues.
- **Source management** is predominantly git submodules, but sonar-xml, sonar-plsql,
  sonar-tsql, and sonar-vb commit sources directly.
- The **filename convention** `<language>-<ruleKey>.json` is universal, though the language
  prefix varies (`java-`, `kotlin-`, `Web-`, `pythonenterprise-`, etc.).
