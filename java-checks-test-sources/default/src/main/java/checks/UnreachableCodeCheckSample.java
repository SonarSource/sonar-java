package checks;

class UnreachableCodeCheckSample {

  int codeAfterReturn(int a) {
    int i = 10;
    return i + a;
    i++; // Noncompliant {{Remove this unreachable code.}}
  }

  int multipleStatementsAfterReturn() {
    return 1;
    int x = 2; // Noncompliant
    int y = 3;
  }

  void voidReturn() {
    System.out.println("Start");
    return;
    System.out.println("End"); // Noncompliant
  }

  void codeAfterThrow(String input) {
    if (input == null) {
      throw new IllegalArgumentException();
      System.out.println("Valid"); // Noncompliant
    }
  }

  void throwInMethod() {
    throw new RuntimeException();
    System.out.println("After throw"); // Noncompliant
  }

  void breakInLoop() {
    while (true) {
      System.out.println("Loop");
      break;
      System.out.println("After break"); // Noncompliant
    }
  }

  void breakInSwitch(int val) {
    switch (val) {
      case 1:
        System.out.println("Case 1");
        break;
        System.out.println("After break"); // Noncompliant
      case 2:
        break;
    }
  }

  void labeledBreak() {
    outer: for (int i = 0; i < 10; i++) {
      break outer;
      System.out.println(i); // Noncompliant
    }
  }

  void codeAfterContinue() {
    for (int i = 0; i < 10; i++) {
      if (i % 2 == 0) {
        continue;
        System.out.println(i); // Noncompliant
      }
    }
  }

  void labeledContinue() {
    outer: for (int i = 0; i < 10; i++) {
      for (int j = 0; j < 10; j++) {
        continue outer;
        System.out.println(j); // Noncompliant
      }
    }
  }

  public UnreachableCodeCheckSample(boolean flag) {
    if (!flag) {
      return;
      System.out.println("Init"); // Noncompliant
    }
  }

  int lastStatement() {
    int x = 10;
    return x;
  }

  int conditionalReturn(boolean flag) {
    if (flag) {
      return 1;
    }
    return 0;
  }

  void breakInConditional() {
    for (int i = 0; i < 10; i++) {
      if (i == 5) {
        break;
      }
      System.out.println(i);
    }
  }

  void continueInConditional() {
    for (int i = 0; i < 10; i++) {
      if (i % 2 == 0) {
        continue;
      }
      System.out.println(i);
    }
  }

  void emptyAfterReturn() {
    return;
  }

  void nestedBlocks() {
    {
      System.out.println("Block 1");
    }
    System.out.println("Block 2");
  }

  void switchCases(int val) {
    switch (val) {
      case 1:
        System.out.println("Case 1");
        break;
      case 2:
        System.out.println("Case 2");
        break;
      default:
        System.out.println("Default");
    }
  }

  void tryCatch() {
    try {
      System.out.println("Try");
      return;
    } catch (Exception e) {
      System.out.println("Catch");
    } finally {
      System.out.println("Finally");
    }
  }

  int ifElseReturns(int x) {
    if (x > 0) {
      return 1;
    } else {
      return -1;
    }
  }

  void forLoop() {
    for (int i = 0; i < 10; i++) {
      if (i == 5) {
        continue;
      }
      System.out.println(i);
    }
  }

  void whileLoop(boolean cond) {
    while (cond) {
      System.out.println("Loop");
      if (cond) {
        break;
      }
      System.out.println("After check");
    }
  }
}
