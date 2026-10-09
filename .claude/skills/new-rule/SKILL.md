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

**Run once** before Step 1. When implementing multiple rules in parallel (subagents), collect answers **here only**, then start each rule with the answers already known — do not re-ask per rule.

Verify / ask as needed:

- [ ] Access to the RSpec repository and a local `rule-api` jar (see [Metadata Files](#metadata-files))
- [ ] Rule id(s) to implement
- [ ] RSpec status per rule: merged on `master` **or** branch name — look up branch/PR **only** in https://github.com/SonarSource/rspec (never in this repo)
- [ ] RSpec Jira ticket → its **epic parent** key (from the RSpec PR title/branch) — used in the PR body as `Part of <EPIC>` (Step 4); **not** reused as the implementation ticket
- [ ] Rule title (for the PR title format in Step 4)
- [ ] Any path/env overrides needed

Stop and resolve gaps before Step 1.

### Commit rules (all steps)

- **Exactly one commit per step** below (after history cleanup: still one commit per step — restructure/rebase, do **not** squash everything into a single commit).
- Message: single line, few words, clear. **No rule id** (it is in the branch). **No "java"** (it is the repo). **No Jira key** (never in commits).
- Examples: `Add rule metadata`, `Add failing reproducer`, `Implement rule`, `Fix review comments`, `Update ruling expectations`

### Step 1 — Metadata → commit

1. Generate metadata with `rule-api` (see [Metadata Files](#metadata-files)). Never hand-write `.html`/`.json`.
2. Stage **only** rule-relevant files: `SXXXX.html`, `SXXXX.json`, and the profile files under `sonar-java-plugin/src/main/resources/profiles/`.
3. Commit.

### Step 2 — Failing reproducer → commit

1. Write the test sample (`java-checks-test-sources/default/src/main/java/checks/{RuleId}CheckSample.java`) covering **every** edge case, noncompliant and compliant, with precise location markers (see the test conventions in `AGENTS.md`).
2. Write `{RuleId}CheckTest.java`, with both a semantic and a `withoutSemantic()` test.
3. Add an **empty** check class (`@Rule(key = "SXXXX")`, minimal `nodesToVisit`/`visitNode`).
4. Run the test — it **must fail**.
5. Commit (reproducer + empty check + test).

### Step 3 — Implementation → commit

1. Implement the check until the unit test **passes**.
2. Commit.

### Step 4 — Push & PR

Push the branch, named with your GitHub username convention (e.g. `<username>/short-kebab-description`), and open the PR.

**Separate Jira tickets (mandatory):** RSpec and rule implementation use **different** SONARJAVA tickets. Do **not** put a Jira key in the initial PR title or in commits.

1. Open the analyzer PR with title exactly: `Implement rule SXXXX - <rule title>` (rule id and title from RSpec; no `SONARJAVA-…` prefix).
2. In the **PR body**, include a line `Part of <EPIC-KEY>`, the **epic parent** of the RSpec ticket. Jira automation creates the implementation ticket in that epic.
3. After the ticket exists: change its issue type from the automation default (**Maintenance**) to **Feature** (new rules are features). Do not invent an epic or ticket key.

Do not stop here.

### Step 5 — Babysit until merge-ready

Loop until **all** are true:

- Every CI check is green
- **No** unresolved or open review-bot comments (address or validly refute each one)
- No merge blockers

While babysitting:

- **Rebase:** automatically rebase on `master` and fix conflicts (rebase, not merge).
- **Comments:** only unresolved threads; read body and location only, not huge JSON dumps. Treat Bugbot carefully — fix only if valid; explain when you disagree.

If **ruling** fails (likely for a new rule), a PR updating the expectations is created automatically on CI failure. Review it, merge it into the branch if the new findings are legitimate, otherwise fix the implementation and the unit tests. Push and re-babysit.

### Step 6 — TP/FP analysis (only when Step 5 is fully green)

1. Collect the **new** findings from the ruling expectation diff: `its/ruling/src/test/resources/expected/<project>/java-<RULE_ID>.json`, where each entry is `"group:artifact:path/to/File.java": [line, …]`. Sources are under `its/sources/<project>/`.
2. **Sample** new findings before classifying (do not analyze unbounded volumes):
   - Let `N` be the sample size (default **100**).
   - If total new findings ≤ `N` → analyze **all**.
   - Else sample up to `N` across projects (water-filling / largest-remainder):
     1. Give every project with findings its fair equal share of the remaining budget among projects that still have unsampled issues.
     2. Projects with fewer findings than their share contribute **all** of theirs; leftover budget is redistributed evenly to projects that still have unsampled issues.
     3. Repeat until the budget is exhausted.
   - Examples: A=1000, B=10_000_000 → about 50 each of 100; A=1, B=100, C=3 → take 1/96/3.
3. For **every** sampled finding: classify **TP** (correct) or **FP** (incorrect).
4. Compute `TP% = 100 * TPs / (TPs + FPs)` on the sample.
5. **Must be > 90%.** If 90% or below: extend the reproducer with cases for those FPs, then **restart from Step 1** and repeat the full workflow (new commits per step; clean history again at the end).

### Step 7 — Clean history

Before considering the PR done: history must show **exactly one commit per workflow step** (metadata, reproducer, implementation, plus any later babysit/ruling/precision fix steps still as clear step commits). Rewrite history as needed (`rebase`, not merge), force-push only if appropriate for the branch, then confirm Steps 5 and 6 still hold.

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

### Tests
Ruling test expectation files, located in `its/ruling/src/test/resources`, are updated by merging an automatically
generated PR into the branch when CI checks fail.

### MethodMatchers

Use `MethodMatchers` to match method calls by type, name, and signature:

```java
private static final MethodMatchers MY_MATCHER = MethodMatchers.create()
  .ofTypes("java.util.List")
  .names("add")
  .withAnyParameters()
  .build();

// In visitNode:
if (MY_MATCHER.matches(methodInvocationTree)) {
  reportIssue(methodInvocationTree, "Message.");
}
```

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
