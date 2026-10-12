# Quick Fix Test Annotation Pattern

## Overview

Quick fix annotations go in the sample file alongside `// Noncompliant` comments.

---

## Annotation Format

### 1. Declare an expected quick fix on the Noncompliant line

```java
someCode(); // Noncompliant {{message}} [[quickfixes=qf1]]
```

`[[quickfixes=qfN]]` must come **after** `{{message}}`.

If the issue has a precise underline annotation on the next line, the `[[quickfixes=qfN]]`
attribute goes on the `// Noncompliant` line (not on the `//^^^^` line).

### 2. Declare fix description

```java
// fix@qf1 {{Description shown in IDE}}
```

### 3. Declare each text edit

```java
// edit@qf1 [[sl=+2;sc=3;el=+2;ec=9]] {{replacement}}
// edit@qf1 [[sl=+5;sc=3;el=+5;ec=13]] {{}}   // empty = delete
```

**Attributes:**
- `sl` / `el` — start/end line, relative (`+N`/`-N`) to the Noncompliant line, or absolute
- `sc` / `ec` — start/end column (1-indexed, end is exclusive — i.e. points to the first character NOT removed)
- `{{replacement}}` — replacement text; `{{}}` means delete

---

## Column Indexing

Columns are **1-indexed** and the end column is **exclusive** (points past the last removed character).

For a modifier like `public` at the start of `  public static void foo()`:
- `public` occupies columns 3–8 (1-indexed, inclusive)
- To remove `public ` (including trailing space, up to `static`): `sc=3;ec=10`
  - `sc=3` = column of `p`
  - `ec=10` = column of `s` in `static` (the first character NOT removed)

For `protected` followed by a space and `static` (13 chars total indented by 2):
- `protected` at columns 3–11; `static` at column 13
- To remove `protected `: `sc=3;ec=13`

General rule using `AnalyzerMessage.textSpanBetween(modifier, true, nextToken, false)`:
- `sc` = column of modifier's first character (1-indexed)
- `ec` = column of next token's first character (1-indexed)

---

## Edit Order

Edits must be listed **in the same order** they appear in the `JavaQuickFix`.
The verifier uses `List.equals()` — order matters.

**Do NOT call `.reverseSortEdits()` on `JavaQuickFix.Builder`** unless you also list
the annotations in the resulting (reversed) order. The natural collection order (ascending
by source position) is the standard — matches how annotations appear in the sample file.

---

## Example: Single modifier (one edit)

```java
protected static class Foo { // Noncompliant {{Remove this 'protected' modifier.}} [[quickfixes=qf1]]
//^^^^^^^^^
// fix@qf1 {{Remove "protected" modifier}}
// edit@qf1 [[sc=3;ec=13]] {{}}
```

No `sl`/`el` needed when the edit is on the **same line** as the issue.

## Example: Multiple modifiers across methods (multiple edits)

```java
class MyTest { // Noncompliant {{Remove the visibility modifiers from this test class and its methods.}} [[quickfixes=qf1]]
//    ^^^^^^
  // fix@qf1 {{Remove all visibility modifiers}}
  // edit@qf1 [[sl=+5;sc=3;el=+5;ec=10]] {{}}   // public on line+5
  // edit@qf1 [[sl=+9;sc=3;el=+9;ec=13]] {{}}   // protected on line+9
```

---

## Check implementation

```java
private static JavaQuickFix buildQuickFix(List<ModifierKeywordTree> modifiers) {
  List<JavaTextEdit> edits = modifiers.stream()
    .map(m -> JavaTextEdit.removeTextSpan(
      AnalyzerMessage.textSpanBetween(m, true, QuickFixHelper.nextToken(m), false)))
    .toList();
  String description = modifiers.size() == 1
    ? String.format("Remove \"%s\" modifier", modifiers.get(0).keyword().text())
    : "Remove all visibility modifiers";
  return JavaQuickFix.newQuickFix(description)
    .addTextEdits(edits)
    .build(); // do NOT call reverseSortEdits() unless annotations also reversed
}
```

Report the issue with `withQuickFixes` (plural), not `withQuickFix`:

```java
QuickFixHelper.newIssue(context)
  .forRule(this)
  .onTree(someTree)
  .withMessage("...")
  .withQuickFixes(() -> List.of(buildQuickFix(modifiers)))
  .report();
```

---

## Debugging

If the test fails with:
```
Expected quickfix qfN at line X was not matched by any provided quickfixes
```

Check these in order:
1. **Order of edits** — are the `edit@` annotations in the same order as the `JavaQuickFix` text edits?
2. **Column values** — verify `sc`/`ec` using the column formula above
3. **Description** — must match the `fix@` annotation exactly
4. **`withQuickFixes` vs `withQuickFix`** — use the plural form
5. **`reverseSortEdits()`** — remove it unless annotations are also in reversed order

The verifier prints only the EXPECTED side on failure; it does not print the actual.
To see the actual, temporarily add a print in `buildQuickFix` or check column math manually.
