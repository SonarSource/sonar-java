# Ruling Tests Report

## Scope

This report answers, completely and directly:

- which projects are used by the `sonar-java` ruling tests
- how each of those projects is checked out
- from which source each checkout comes
- in which directory each checkout lives
- where the expectation JSON files are stored
- what the keys in those JSON files are
- what the exact correspondence is between source projects, expectation directories, JSON filenames, JSON keys, and issue lines

Primary implementation reference:

- [its/ruling/src/test/java/org/sonar/java/it/JavaRulingTest.java](its/ruling/src/test/java/org/sonar/java/it/JavaRulingTest.java)

## Source of Truth Used for This Report

The report is based on the current repository state visible in this checkout:

- the ruling test code in `its/ruling/src/test/java/org/sonar/java/it/JavaRulingTest.java`
- the top-level submodule declaration in `.gitmodules`
- the nested submodule declaration in `its/sources/.gitmodules`
- the current directories under `its/sources`
- the current expectation directories under `its/ruling/src/test/resources`
- representative expectation JSON files to confirm the key/value structure

## Complete List of Projects Used by the Ruling Tests

The current `JavaRulingTest` class defines the following ruling test projects:

| projectName used by the test | JUnit test method | Sonar project key | Source path used by the test code | How it is checked out in the current repository | Source declaration visible in the current repository |
|---|---|---|---|---|---|
| `mall` | `spring_mall()` | `com.macro.mall:mall` | `its/sources/mall` | Ordinary directory inside the `its/sources` submodule | Comes from top-level submodule `its/sources` declared in `.gitmodules`, URL `https://github.com/SonarSource/ruling_java.git` |
| `guava` | `guava()` | `com.google.guava:guava` | `its/sources/guava` | Ordinary directory inside the `its/sources` submodule | Comes from top-level submodule `its/sources` declared in `.gitmodules`, URL `https://github.com/SonarSource/ruling_java.git` |
| `commons-beanutils` | `apache_commons_beanutils()` | `commons-beanutils:commons-beanutils` | `its/sources/commons-beanutils` | Ordinary directory inside the `its/sources` submodule | Comes from top-level submodule `its/sources` declared in `.gitmodules`, URL `https://github.com/SonarSource/ruling_java.git` |
| `eclipse-jetty` | `eclipse_jetty_incremental()` main-branch analysis | `org.eclipse.jetty:jetty-project` | `its/sources/eclipse-jetty` | Git submodule inside the `its/sources` submodule | Declared in `its/sources/.gitmodules`, URL `https://github.com/SonarSource/ruling_java.git`, branch `eclipse-jetty-main` |
| `eclipse-jetty-similar-to-main` | `eclipse_jetty_incremental()` large-PR analysis | `org.eclipse.jetty:jetty-project` | `its/sources/eclipse-jetty-similar-to-main` | Git submodule inside the `its/sources` submodule | Declared in `its/sources/.gitmodules`, URL `https://github.com/SonarSource/ruling_java.git`, branch `eclipse-jetty-same-issues-as-main` |
| `eclipse-jetty-similar-to-main-small` | `eclipse_jetty_incremental()` small-PR analysis | `org.eclipse.jetty:jetty-project` | `its/sources/eclipse-jetty-similar-to-main-small` | Git submodule inside the `its/sources` submodule | Declared in `its/sources/.gitmodules`, URL `https://github.com/SonarSource/ruling_java.git`, branch `eclipse-jetty-same-issues-as-main-small` |
| `java-time-example` | `java_time_example_incremental()` main-branch analysis | `example:java-time-example` | expected by code at `its/sources/java-time-example` | Not present in the current checkout | Not declared in the visible `.gitmodules` files in this checkout |
| `java-time-example-less-threshold` | `java_time_example_incremental()` PR analysis | `example:java-time-example` | expected by code at `its/sources/java-time-example-less-threshold` | Not present in the current checkout | Not declared in the visible `.gitmodules` files in this checkout |
| `sonar-server` | `sonarqube_server()` | `org.sonarsource.sonarqube:sonar-server` | `its/sources/sonarqube-6.5/server/sonar-server` | Ordinary directory inside the `its/sources` submodule | Comes from top-level submodule `its/sources` declared in `.gitmodules`, URL `https://github.com/SonarSource/ruling_java.git` |
| `jboss-ejb3-tutorial` | `jboss_ejb3_tutorial()` | `jboss-ejb3-tutorial` | `its/sources/jboss-ejb3-tutorial` | Ordinary directory inside the `its/sources` submodule | Comes from top-level submodule `its/sources` declared in `.gitmodules`, URL `https://github.com/SonarSource/ruling_java.git` |
| `regex-examples` | `regex_examples()` | `org.regex-examples:regex-examples` | `its/sources/regex-examples` | Ordinary directory inside the `its/sources` submodule | Comes from top-level submodule `its/sources` declared in `.gitmodules`, URL `https://github.com/SonarSource/ruling_java.git` |
| `vibebot` | `vibebot()` | `org.vibebot:vibebot` | `its/vibebot` | Ordinary directory checked into the main repository | Local project in this repository, not part of `its/sources` |

## Meaning of “Nested Submodule”

A nested submodule is a git submodule declared inside another submodule.

In this repository:

- `its/sources` is a submodule of the main `sonar-java` repository
- `its/sources` contains its own `.gitmodules`
- that inner `.gitmodules` declares three more submodules:
  - `its/sources/eclipse-jetty`
  - `its/sources/eclipse-jetty-similar-to-main`
  - `its/sources/eclipse-jetty-similar-to-main-small`

So those three Jetty directories are separate git submodules managed from inside the `its/sources` submodule. They are not just ordinary tracked directories inside `its/sources`.

## Complete List of Expectation Directories Present in This Repository

Expectation JSON files are stored under:

- `its/ruling/src/test/resources/`

The project expectation directories currently present there are:

- `its/ruling/src/test/resources/commons-beanutils/`
- `its/ruling/src/test/resources/eclipse-jetty/`
- `its/ruling/src/test/resources/eclipse-jetty-similar-to-main/`
- `its/ruling/src/test/resources/eclipse-jetty-similar-to-main-small/`
- `its/ruling/src/test/resources/guava/`
- `its/ruling/src/test/resources/java-time-example/`
- `its/ruling/src/test/resources/java-time-example-less-threshold/`
- `its/ruling/src/test/resources/jboss-ejb3-tutorial/`
- `its/ruling/src/test/resources/mall/`
- `its/ruling/src/test/resources/regex-examples/`
- `its/ruling/src/test/resources/sonar-server/`
- `its/ruling/src/test/resources/vibebot/`

In addition to those project directories, the same folder also contains:

- `its/ruling/src/test/resources/diff_S9130.json`

## Complete Naming Rule for Expectation JSON Files

Inside each project expectation directory, each JSON file corresponds to exactly one rule for that project.

The naming convention is:

- `java-<ruleKey>.json`

The `<ruleKey>` part can be:

- a built-in Java rule key such as `S100`, `S1451`, `S8694`, and so on
- a custom instantiated template-rule key created by the test setup, such as `stringToCharArray`, `longDate`, or `commentRegexTest`

That means the filename alone identifies the rule whose expected issues are stored in the file.

## Complete Structure of an Expectation JSON File

Each expectation JSON file is a single top-level JSON object.

That object contains:

- keys: analyzed components for that one project
- values: arrays of expected issue line numbers for the one rule identified by the filename

The file does not repeat the project name or the rule name as separate JSON fields, because:

- the project is identified by the parent directory name
- the rule is identified by the filename

## Complete Definition of the JSON Keys

Across the ruling expectation files in this repository, the keys follow exactly two shapes.

### 1. File-level key

Format:

- `<sonar.projectKey>:<relative path>`

Meaning:

- the prefix before the final `:<relative path>` is the `sonar.projectKey` used for the analyzed project
- the suffix after that prefix is the relative path of the file inside the analyzed source tree
- the value array attached to that key is the complete set of expected issue lines for that rule on that file

Concrete interpretation:

- one file-level JSON key identifies one analyzed source file in one project
- the associated array identifies every expected issue line for the rule represented by the JSON filename

### 2. Project-level key

Format:

- `<sonar.projectKey>`

Meaning:

- the issue is attached to the project as a whole rather than to a specific file path
- the associated array gives the expected stored line markers for that project-level issue

Concrete interpretation:

- one project-level JSON key identifies the whole analyzed project
- the associated array records the expected stored line markers for that rule at project scope

## Complete Definition of the JSON Values

Each value is an array of integers.

The complete meaning of that array is:

- each integer is one expected issue location for the rule identified by the JSON filename
- if the key is file-level, the integers are source line numbers in that file
- if the key is project-level, the array records the stored marker used by the ruling dump for that project-level issue
- if the array contains `0`, the dump is recording an issue without a meaningful source line number
- if the array contains `n` integers, then `n` issues are expected for that rule on that component

The JSON values therefore encode only issue locations. They do not encode severity, message, effort, flows, or rule key, because those are not part of this baseline format.

## Complete Correspondence Between Source Projects, Directories, Filenames, Keys, and Values

The ruling baseline identity is assembled from four layers:

1. Source checkout
2. Expectation directory
3. JSON filename
4. JSON key/value entry

The exact correspondence is:

- one analyzed source checkout is selected by one JUnit test invocation
- that analysis is associated with one `projectName`
- that `projectName` maps to one expectation directory at `its/ruling/src/test/resources/<projectName>/`
- inside that directory, each file `java-<ruleKey>.json` stores the complete baseline for one rule on that project
- inside that file, each JSON key identifies one analyzed component for that project
- the array attached to that key lists all expected issue lines for that component and that rule

So the full meaning of one baseline entry is:

- directory name = which analyzed project
- filename = which rule
- JSON key = which component of that project
- JSON value array = which issue lines are expected for that rule on that component

## Complete Correspondence Table

| Level | Repository location or value | What it identifies |
|---|---|---|
| Source checkout | `its/sources/...` or `its/vibebot` | The source code that is analyzed |
| Project identity in the test | `projectName` in `JavaRulingTest` | The logical project bucket used for expected and actual dumps |
| Expected dump directory | `its/ruling/src/test/resources/<projectName>/` | The full set of expected rule baselines for that project |
| Actual dump directory | `its/ruling/target/actual/<projectName>/` | The newly generated rule dumps for that project during a test run |
| One expectation file | `its/ruling/src/test/resources/<projectName>/java-<ruleKey>.json` | The expected baseline for one rule on one project |
| One JSON key | `<sonar.projectKey>` or `<sonar.projectKey>:<relative path>` | One analyzed component: either the whole project or one file |
| One JSON value array | `[line1, line2, ...]` | The complete set of expected issue lines for that component and that rule |

## Repository-State Discrepancies That Affect the Complete Picture

The current checkout contains two relevant inconsistencies that need to be stated explicitly for the report to be complete.

1. `README.md` still refers to expectation files named like `squid-SXXXX.json`, but the repository currently stores expectation files as `java-<ruleKey>.json`.
2. `JavaRulingTest` references source trees for `java-time-example` and `java-time-example-less-threshold`, and the corresponding expectation directories exist, but those source trees are not present in the current workspace and are not declared in the visible `.gitmodules` files.

## Summary

The ruling suite analyzes a fixed set of real projects and compares generated issue dumps with checked-in baselines. Most of those projects come from the `its/sources` submodule, which points to `https://github.com/SonarSource/ruling_java.git`. Three Jetty variants are submodules nested inside that submodule. `vibebot` is local to this repository.

The complete baseline format is:

- project directory: `its/ruling/src/test/resources/<projectName>/`
- one file per rule: `java-<ruleKey>.json`
- one JSON key per analyzed component
- one integer array per component giving the full expected set of issue lines for that rule
