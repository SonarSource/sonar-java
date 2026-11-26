---
title: Testing Java quick fixes
description: Pin quick-fix descriptions, replacement text, edit spans and edit ordering in the verifier sample.
generated:
  by: tirion
---

# Testing Java quick fixes

Declare expected fixes in the same sample as the issue. This fragment comes from `java-checks-test-sources/default/src/main/java/checks/DurationTimeUnitAgreementCheckSample.java`; the [rule test guide](rule-tests.md) explains the underlying issue and caret markers.

```java
    f.get(d.toMillis(), TimeUnit.SECONDS); // Noncompliant {{Change this TimeUnit to "MILLISECONDS" or convert the duration to seconds.}} [[quickfixes=qf1]]
//                      ^^^^^^^^^^^^^^^^
    // fix@qf1 {{Change TimeUnit to "MILLISECONDS"}}
    // edit@qf1 [[sc=34;ec=41]] {{MILLISECONDS}}
```

## Declare each fix and edit

- Put `[[quickfixes=qf1]]` after the message on the `Noncompliant` line, not on the underline. Use a distinct ID for each fix in the sample, a comma-separated list for alternatives, or `[[quickfixes=!]]` to assert no fix. Omitting the attribute does not assert absence.
- The `fix@qf1` text must exactly match the description shown in the IDE.
- Each `edit@qf1` describes one text edit. Put the replacement in `{{...}}`; use `{{}}` for deletion.
- Columns (`sc`, `ec`) are one-based and the end is exclusive. In the example, `sc=34` points to the `S` in `SECONDS` and `ec=41` points to the closing parenthesis. The edit can be narrower than the primary span.
- Omit start/end lines if both are on the issue line. Otherwise give `sl` and `el`, preferably relative to the issue line: `sl=+2;sc=3;el=+2;ec=9` starts two lines below the issue, regardless of where the annotation is written.
- List edits in the order the `JavaQuickFix` supplies them. If the builder calls `reverseSortEdits()`, match the resulting order rather than the source order.

## Report the fix and verify it

`java-checks/src/main/java/org/sonar/java/checks/DurationTimeUnitAgreementCheck.java` demonstrates `QuickFixHelper.newIssue(context)`. After setting the issue location and message, use `withQuickFix` for one fix or `withQuickFixes` for a list of alternatives, then call `report()`. A single fix can contain multiple edits. Prefer `JavaTextEdit` tree-based operations over manual offsets when the edit aligns with the syntax tree.

Run the rule test with `CheckVerifier.verifyIssues()`. If a fix is unmatched, first check the description, exact replacement text, one-based columns and exclusive end, and the edit order. If the annotations are right, verify that the issue is actually reported through the quick-fix builder for that case. Include a case that asserts no fix when the rule should report an issue but cannot safely offer an automatic edit.
