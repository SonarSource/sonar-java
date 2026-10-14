---
name: new-rule
description: >-
  Implement Java analyzer rules using the mandatory delivery workflow:
  metadata → failing reproducer → implementation → PR babysit → TP/FP > 90%.
  Use when creating or shipping any new/changed rule.
---

# New Rule Implementation Skill

This skill provides sonar-java-specific guidelines for implementing new rules.

**Non-negotiable:** every rule PR must follow the [Mandatory delivery workflow](#mandatory-delivery-workflow). Do not end before a PR that is not fully green, has open review-bot comments, messy history, or a TP ratio of 90% or below.

## Mandatory delivery workflow

Execute **in order**. Never skip. Never combine steps into one commit. If precision fails, loop again.

### Step 0 — Pre-flight (once, before any rule work)

**Run once** before Step 1. When implementing multiple rules in parallel (subagents), collect answers **here only** once at the beginning, then start each rule with the answers already known — do not re-ask later.

Verify / ask as needed:

- [ ] Access to the RSpec repository and a local `rule-api` jar (see [Metadata Files](#metadata-files))
- [ ] Rule id(s) to implement
- [ ] RSpec status per rule: merged on `master` **or** branch name — look up branch/PR **only** in https://github.com/SonarSource/rspec (never in this repo)
- [ ] RSpec Jira ticket key (from the RSpec PR title/branch) — reused as the implementation ticket and as the PR title prefix (Step 4)
- [ ] Rule title (for the PR title format in Step 4)
- [ ] Any path/env overrides needed

Stop and resolve gaps before Step 1.

### Commit rules (all steps)

- Aim for **one commit per step** below. While working, extra commits are fine: fixes, retries and ruling updates will be folded back into one commit per step during [Step 7](#step-7--clean-history). The rule is an invariant on the **final** history, not a constraint on how you work.
- Never squash everything into a single commit either: the final history keeps one commit per step.
- Message: single line, few words, clear. **No rule id** (it is in the branch). **No "java"** (it is the repo). **No Jira key** (never in commits).
- Examples: `Add rule metadata`, `Add failing reproducer`, `Implement rule`, `Fix review comments`, `Update ruling expectations`

### Step 1 — Metadata → commit

1. Generate metadata with `rule-api` (see [Metadata Files](#metadata-files)). Never hand-write `.html`/`.json`.
2. Stage **only** rule-relevant files: `SXXXX.html`, `SXXXX.json`, and the profile files under `sonar-java-plugin/src/main/resources/profiles/`.
3. Commit.

### Step 2 — Failing reproducer → commit

1. Write the test sample (`java-checks-test-sources/default/src/main/java/checks/{RuleId}CheckSample.java`) covering **every** edge case, noncompliant and compliant, with caret markers on **every** primary and secondary location (see [Test sample location markers](#test-sample-location-markers)).
2. Write `{RuleId}CheckTest.java`, with both a semantic and a `withoutSemantic()` test, free of duplication (see [Test class structure](#test-class-structure)).
3. Add an **empty** check class (`@Rule(key = "SXXXX")`, minimal `nodesToVisit`/`visitNode`).
4. Run the test — it **must fail**.
5. Commit (reproducer + empty check + test).

### Step 3 — Implementation → commit

1. Implement the check until the unit test **passes**. Match methods and types with [matchers](#method-and-type-matching), never with raw strings, and keep the check [stateless](#stateless-checks).
2. Commit.

### Step 4 — Push & PR

Push the branch, named with your GitHub username convention (e.g. `<username>/short-kebab-description`), and open the PR.

**Single Jira ticket (mandatory):** the RSpec and the rule implementation share the **same** SONARJAVA ticket. Do **not** create a second ticket, and do **not** put the Jira key in commits.

1. Open the analyzer PR with title exactly: `SONARJAVA-XXXX Implement rule SXXXX - <rule title>`, reusing the RSpec ticket key as prefix (rule id and title from RSpec).
2. Do not invent a ticket key — take it from the RSpec PR or branch.

Do not stop here.

### Step 5 — Babysit until merge-ready

Loop until **all** are true:

- Every CI check is green
- **No** unresolved or open review-bot comments (address or validly refute each one)
- No merge blockers

While babysitting:

- **Rebase:** automatically rebase on `master` and fix conflicts (rebase, not merge).
- **Comments:** only unresolved threads; read body and location only, not huge JSON dumps. Treat gitar review bot carefully — fix only if valid; explain when you disagree.

If **ruling** fails (likely for a new rule), a PR updating the expectations is created automatically on CI failure. Review it, merge it into the branch if the new findings are legitimate, otherwise fix the implementation and the unit tests. Push and re-babysit. The resulting merge commit is acceptable while babysitting; Step 7 folds the expectation update into a single `Update ruling expectations` commit.

### Step 6 — TP/FP analysis (only when Step 5 is fully green)

1. Collect the **new** findings from the ruling expectation diff: `its/ruling/src/test/resources/expected/<project>/java-<RULE_ID>.json`, where each entry is `"group:artifact:path/to/File.java": [line, …]`. Sources are under `its/sources/<project>/`.
2. **Sample** new findings before classifying (do not analyze unbounded volumes):
   - Let `N` be the sample size (default **100**).
   - If total new findings ≤ `N` → analyze **all**.
   - Else sample up to `N` across projects (water-filling, i.e. equal shares capped by each project's count):
     1. Give every project with findings its fair equal share of the remaining budget among projects that still have unsampled issues.
     2. Projects with fewer findings than their share contribute **all** of theirs; leftover budget is redistributed evenly to projects that still have unsampled issues.
     3. Repeat until the budget is exhausted.
   - Examples: A=1000, B=10_000_000 → about 50 each of 100; A=1, B=100, C=3 → take 1/96/3.
3. For **every** sampled finding: classify **TP** (correct) or **FP** (incorrect).
4. Compute `TP% = 100 * TPs / (TPs + FPs)` on the sample.
5. **Must be > 90%.** If 90% or below: add reproducers for the FPs as new test cases and confirm the test fails (Step 2 commit), then introduce heuristics in the implementation to reduce the noise until it passes (Step 3 commit), and re-run Steps 5 and 6. Do not restart from Step 1: metadata and the check class already exist.

### Step 7 — Clean history

This is where the one-commit-per-step rule is enforced. Whatever the branch looks like after babysitting and precision loops, the final history must show **exactly one commit per workflow step**:

- one `Add rule metadata` commit
- one `Add failing reproducer` commit, including the FP cases added in Step 6
- one `Implement rule` commit, including the heuristics added in Step 6
- one `Update ruling expectations` commit, folding in the auto-generated ruling PR and dropping its merge commit

Rewrite history with interactive `rebase` (never a merge), then force-push and confirm Steps 5 and 6 still hold.

### Definition of done

The PR is done **only** when:

1. History is clean (one commit per step)
2. All checks are green
3. No open review-bot comments
4. TP/FP analysis is done and **TP% > 90%**

## What to Do

### Metadata Files
- **DO** generate rule metadata using rule-api tool from the RSPEC repository:
  - Ensure your local rspec repository is up-to-date with the rule branch
  - Use rule-api jar (check Maven local repository for available versions)
  - Command: `java -jar <rule-api.jar> generate -branch rule/add-RSPEC-S{RULE_ID} -rule S{RULE_ID}`
  - This generates HTML and JSON files and updates the Sonar way profile automatically
- Generated files will be placed in:
  - `sonar-java-plugin/src/main/resources/org/sonar/l10n/java/rules/java/S{RULE_ID}.html`
  - `sonar-java-plugin/src/main/resources/org/sonar/l10n/java/rules/java/S{RULE_ID}.json`
  - `sonar-java-plugin/src/main/resources/profiles/{Sonar_way|Sonar_agentic_AI}/S{RULE_ID}`

### Test sample location markers

Every **primary** and **secondary** location must be asserted with caret markers. Never leave a bare `// Noncompliant` when the rule reports on a sub-expression: a test that does not pin the location does not protect it.

Put the carets in a comment on the line **below** the reported line, aligned under the flagged token:

```java
b = sb1.length() == 0; // Noncompliant {{Use "isEmpty()" to check whether a "StringBuilder" is empty or not.}}
//  ^^^^^^^^^^^^^^^^^
```

Secondary locations use the same carets plus a direction marker: `<` when the primary is **above**, `>` when it is **below**.

```java
new Thread(() -> {
//  ^^^^^^>
  Thread.sleep(1000); // Noncompliant {{Message.}}
//       ^^^^^
});
```

Several locations can be marked on one caret line:

```java
Arrays.fill(textBuffer, 42); // Noncompliant {{Message.}}
//     ^^^^ ^^^^^^^^^^<
```

Use `[[sc=…;ec=…]]` / `[[secondary=…]]` only when carets cannot work: a token at column 1, a multi-line span, or quickfix declarations (`[[quickfixes=qf1]]`).

To build an aligned caret line:

```shell
str="<line including <substring>>"; sub="<substring>"; pre="${str%%"$sub"*}"; echo "${pre//?/ }${sub//?/^}"
```

### Test class structure

Do not repeat the sample path or the check instantiation in each test method. Extract them as constants and keep the verifier chain in the tests:

```java
class FooCheckTest {

  private static final String SAMPLE = "checks/FooCheckSample.java";
  private static final FooCheck CHECK = new FooCheck();

  @Test
  void test() {
    CheckVerifier.newVerifier()
      .onFile(mainCodeSourcesPath(SAMPLE))
      .withCheck(CHECK)
      .verifyIssues();
  }

  @Test
  void test_without_semantic() {
    CheckVerifier.newVerifier()
      .onFile(mainCodeSourcesPath(SAMPLE))
      .withCheck(CHECK)
      .withoutSemantic()
      .verifyNoIssues();
  }
}
```

A single shared check instance is safe because checks are [stateless](#stateless-checks). When a test needs several samples (non-compiling or Java version variants), declare one constant per sample.

### Method and type matching

Match methods with `MethodMatchers` and types with the type API. **Never** compare fully qualified names, method names or type names with raw strings (`equals`, `contains`, `startsWith`): matchers handle overloads, inheritance and unknown symbols correctly.

```java
private static final MethodMatchers CONSTRUCTOR_LOOKUP = MethodMatchers.create()
  .ofTypes("java.lang.Class")
  .names("getConstructor", "getDeclaredConstructor")
  .addParametersMatcher("java.lang.Class[]")
  .build();

// In visitNode:
if (CONSTRUCTOR_LOOKUP.matches(methodInvocationTree)) {
  reportIssue(methodInvocationTree, "Message.");
}
```

Prefer an explicit `addParametersMatcher(...)` over `withAnyParameters()` whenever the rule only targets specific overloads. For types, use `symbolType().isSubtypeOf("java.util.Collection")` rather than string checks on the type name.

### Stateless checks

A check instance is reused across files, so it must carry **no mutable state**. A check class may only declare `private static final` fields (messages, matchers, immutable constants).

Anything accumulated during a scan — sets of visited symbols, maps, counters, lookup tables — must live in a local variable and be passed explicitly through the call graph, never stored in a field.

```java
@Rule(key = "SXXXX")
public class MyCheck extends IssuableSubscriptionVisitor {

  private static final String MESSAGE = "Message.";
  private static final MethodMatchers MATCHER = MethodMatchers.create() /* … */ .build();

  @Override
  public List<Tree.Kind> nodesToVisit() {
    return List.of(Tree.Kind.CLASS);
  }

  @Override
  public void visitNode(Tree tree) {
    Set<Symbol> seen = new HashSet<>();
    collectFields((ClassTree) tree, seen);
    report(seen);
  }
}
```

If a rule genuinely needs cross-node context, compute it from the subscribed node by walking the AST, or subscribe to the enclosing node (class, method, compilation unit) and do the whole analysis there with local state.

The only accepted non-static fields are the ones the API itself requires, all set before the scan and never mutated during it:

- `@RuleProperty` parameter fields, which the platform injects and therefore cannot be `static final`
- the `JavaFileScannerContext` of a `BaseTreeVisitor` rule, assigned at the start of `scanFile`
- the flags a `DependencyVersionAware` rule sets in `isCompatibleWithDependencies`

Because a stateless check has nothing to reset, it does not need `clearState()`. The `clearState()` guidance in `AGENTS.md` applies to existing checks that still hold per-file state, not to new rules.

### External Dependencies in Tests
- **DO NOT** add external library dependencies for test samples
- **DO** create mock-ups or use non-compiling tests when external libraries are needed
- **SEE** Spring examples in `java-checks-test-sources/*/src/main/files/non-compiling/checks/`

## What NOT to Do

### 1. Version Control
- **DO NOT** commit log files (e.g., `*.log`, build logs)
- **DO NOT** commit temporary or generated files

### 2. Build Configuration
- **DO NOT** modify `pom.xml` files
- Build configuration is managed centrally
- Only modify if explicitly required by the rule implementation

### 3. Architecture
- **DO NOT** make architectural changes to the codebase
- New rules should fit within the existing architecture
- Follow the patterns used by similar existing rules

### 4. Test Dependencies
- **DO NOT** add real external dependencies to test projects
- Use mocks, stubs, or non-compiling tests instead
- Keep test projects lightweight and self-contained

## File Structure Reference

### Rule Implementation
- Rule class: `java-checks/src/main/java/org/sonar/java/checks/{RuleId}Check.java`
- Test class: `java-checks/src/test/java/org/sonar/java/checks/{RuleId}CheckTest.java`
- Test samples: `java-checks-test-sources/default/src/main/java/checks/{RuleId}CheckSample.java`

### Metadata
- HTML description: `sonar-java-plugin/src/main/resources/org/sonar/l10n/java/rules/java/{RULE_ID}.html`
- JSON metadata: `sonar-java-plugin/src/main/resources/org/sonar/l10n/java/rules/java/{RULE_ID}.json`
- Profile: `sonar-java-plugin/src/main/resources/profiles/{Sonar_way|Sonar_agentic_AI}/S{RULE_ID}`

### Non-Compiling Tests (when needed)
- Location: `java-checks-test-sources/default/src/main/files/non-compiling/checks/`
- Use for: Code that requires external libraries (Spring, AWS SDK, etc.)
