package checks;

import java.util.List;

public class S8795CheckSample {

  // ===========================
  // return statement
  // ===========================

  int afterReturnWithExpression(int a) {
    int i = 10;
    return i + a;
    i++; // Noncompliant {{Remove this unreachable code.}}
  }

  void afterReturnVoid() {
    System.out.println("before");
    return;
    System.out.println("after"); // Noncompliant {{Remove this unreachable code.}}
  }

  void multipleUnreachableOnlyFirstFlagged() {
    return;
    int x = 1; // Noncompliant {{Remove this unreachable code.}}
    int y = 2; // Compliant - only first unreachable is reported per block
  }

  int compliantReturn(int a) {
    int i = 10;
    i++;
    return i + a; // Compliant
  }

  // ===========================
  // throw statement
  // ===========================

  void afterThrow() {
    throw new IllegalStateException("Error occurred");
    cleanup(); // Noncompliant {{Remove this unreachable code.}}
  }

  void compliantThrow() {
    cleanup();
    throw new IllegalStateException("Error occurred"); // Compliant
  }

  // ===========================
  // break statement
  // ===========================

  void afterBreakInLoop(List<String> items) {
    for (String item : items) {
      break;
      System.out.println(item); // Noncompliant {{Remove this unreachable code.}}
    }
  }

  void compliantBreakInLoop(List<String> items) {
    for (String item : items) {
      if (item.isEmpty()) {
        break; // Compliant
      }
      System.out.println(item); // Compliant
    }
  }

  void afterBreakInWhile() {
    while (true) {
      break;
      System.out.println("unreachable"); // Noncompliant {{Remove this unreachable code.}}
    }
  }

  // ===========================
  // continue statement
  // ===========================

  void afterContinueInLoop(List<String> items) {
    for (String item : items) {
      continue;
      System.out.println(item); // Noncompliant {{Remove this unreachable code.}}
    }
  }

  void afterContinueInsideIf(List<String> items) {
    for (String item : items) {
      if (item.isEmpty()) {
        continue;
        System.out.println("Processing: " + item); // Noncompliant {{Remove this unreachable code.}}
      }
    }
  }

  void compliantContinueInsideIf(List<String> items) {
    for (String item : items) {
      if (item.isEmpty()) {
        continue; // Compliant
      }
      System.out.println("Processing: " + item); // Compliant
    }
  }

  void afterContinueInForEach(List<String> items) {
    for (String item : items) {
      if (item.isEmpty()) {
        continue;
        System.out.println(item); // Noncompliant {{Remove this unreachable code.}}
      }
    }
  }

  // ===========================
  // nested if/else blocks
  // ===========================

  void afterReturnInIfBlock(int x) {
    if (x > 0) {
      return;
      System.out.println("positive"); // Noncompliant {{Remove this unreachable code.}}
    }
    System.out.println("done"); // Compliant
  }

  void afterReturnInElseBlock(int x) {
    if (x > 0) {
      System.out.println("positive"); // Compliant
    } else {
      return;
      System.out.println("non-positive"); // Noncompliant {{Remove this unreachable code.}}
    }
  }

  // ===========================
  // switch statement
  // ===========================

  String afterReturnInSwitchCase(int x) {
    switch (x) {
      case 1:
        return "one";
        System.out.println("unreachable"); // Noncompliant {{Remove this unreachable code.}}
      case 2:
        return "two"; // Compliant - separate case group
      default:
        return "other";
    }
  }

  void afterBreakInSwitchCase(int x) {
    switch (x) {
      case 1:
        System.out.println("one");
        break;
        System.out.println("unreachable"); // Noncompliant {{Remove this unreachable code.}}
      case 2:
        System.out.println("two"); // Compliant - separate case group
        break;
    }
  }

  // ===========================
  // try-catch-finally
  // ===========================

  void afterReturnInTryBlock() {
    try {
      return;
      System.out.println("unreachable in try"); // Noncompliant {{Remove this unreachable code.}}
    } finally {
      cleanup(); // Compliant - finally always executes
    }
  }

  void afterThrowInCatchBlock() {
    try {
      doSomething();
    } catch (Exception e) {
      throw new RuntimeException(e);
      cleanup(); // Noncompliant {{Remove this unreachable code.}}
    }
  }

  void finallyIsAlwaysReachable() {
    try {
      return; // Compliant
    } finally {
      cleanup(); // Compliant - finally blocks always execute
    }
  }

  void independentTryAndCatchAnalysis() {
    try {
      doSomething(); // Compliant
    } catch (Exception e) {
      System.out.println("caught"); // Compliant
    } finally {
      cleanup(); // Compliant
    }
  }

  // ===========================
  // while / do-while loops
  // ===========================

  void afterReturnInWhile() {
    while (true) {
      return;
      System.out.println("unreachable"); // Noncompliant {{Remove this unreachable code.}}
    }
  }

  void afterBreakInDoWhile() {
    do {
      break;
      System.out.println("unreachable"); // Noncompliant {{Remove this unreachable code.}}
    } while (true);
  }

  // ===========================
  // labeled break / continue
  // ===========================

  void afterLabeledBreak() {
    outer:
    for (int i = 0; i < 10; i++) {
      for (int j = 0; j < 10; j++) {
        break outer;
        System.out.println("unreachable"); // Noncompliant {{Remove this unreachable code.}}
      }
    }
  }

  void afterLabeledContinue() {
    outer:
    for (int i = 0; i < 10; i++) {
      for (int j = 0; j < 10; j++) {
        continue outer;
        System.out.println("unreachable"); // Noncompliant {{Remove this unreachable code.}}
      }
    }
  }

  // ===========================
  // single-statement bodies (no issue possible)
  // ===========================

  void singleStatementIfBody(boolean cond) {
    if (cond) return; // Compliant - single statement, no sibling after it in same block
    System.out.println("after if"); // Compliant - not in the if block
  }

  void singleStatementForBody(List<String> items) {
    for (String item : items)
      continue; // Compliant - single statement body
  }

  // ===========================
  // static initializer and instance initializer
  // ===========================

  static {
    if (Math.random() > 0.5) {
      return; // Compliant (not a jump in the static block's top-level sequence)
    }
    System.out.println("static init"); // Compliant
  }

  // ===========================
  // constructor
  // ===========================

  S8795CheckSample(boolean failFast) {
    if (failFast) {
      throw new IllegalArgumentException("fail fast");
      System.out.println("unreachable"); // Noncompliant {{Remove this unreachable code.}}
    }
    System.out.println("constructed"); // Compliant
  }

  S8795CheckSample() {
    // Compliant default constructor
  }

  // ===========================
  // redundant break after return (inside loop)
  // ===========================

  String findFirst(List<String> items, String target) {
    for (String item : items) {
      if (item.equals(target)) {
        return item;
        break; // Noncompliant {{Remove this unreachable code.}}
      }
    }
    return null;
  }

  // ===========================
  // helpers
  // ===========================

  private void cleanup() {
  }

  private void doSomething() throws Exception {
  }
}
