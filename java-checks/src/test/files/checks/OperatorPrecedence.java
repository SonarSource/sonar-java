abstract class Class {

  int a, i;
  boolean t;

  @Annotation1(1 + 2 + 3) // Compliant
  @Annotation2(1 & 2 | 3) // Noncompliant [[sc=16;ec=21]] {{Add parentheses to make the operator precedence explicit.}}
  @Annotation3(key = 1 + 2 + 3) // Compliant
  @Annotation4(key = 1 & 2 | 3) // Noncompliant [[sc=22;ec=27]] {{Add parentheses to make the operator precedence explicit.}}
  public int method(int[] array, int value) {
    ;
    // should raise an issue in initializer
    int b = 1 & 2 | 3 & 4; // Noncompliant {{Add parentheses to make the operator precedence explicit.}}

    // should raise an issue in operand of unary / parenthesized expression
    // nested kind should not leak outside
    b = +(1 & 2 | 3) + 2; // Noncompliant {{Add parentheses to make the operator precedence explicit.}}

    // should raise an issue in operand of array acces expression
    // nested kind should not leak outside
    b = array[1 & 2 | 3] + 2; // Noncompliant {{Add parentheses to make the operator precedence explicit.}}

    // should raise an issue in operand of method invocation expression
    // nested kind should not leak outside
    b = method(array, 1 & 2 | 3) + 1; // Noncompliant {{Add parentheses to make the operator precedence explicit.}}

    int[][] c = {{1 & 2 | 3, 2 | 3, 3}, { 1 << 2, 1 + 2 }}; // Noncompliant {{Add parentheses to make the operator precedence explicit.}}

    b = 1 | new Object() {
      public int method() {
        return 1 & 2; // Compliant
      }
    }.hashCode();

    Object o;
    o = 1 == 2 ? 1 + 1 : 1 - 1; // Noncompliant
    o = 1 == 2 ? (1 + 1) : (1 - 1); // compliant
    o = t ? t ? b : c : c; // Noncompliant
    o = t ? foo() : bar(); // Compliant
    o = t ? foo() : new Object(); // Compliant
    o = t ? foo() : (A) o; // Compliant
    o = t ? array[1] : b; // Compliant
    o = t ? -1 : b; // Compliant
    o = t ? +1 : b; // Compliant
    o = t ? ++i : b; // Compliant
    o = t ? i : i++; // Compliant

    b = b = 1; // Compliant

    int d = b = a; // Compliant

    do {
      b = 1 & 2 | 3; // Noncompliant {{Add parentheses to make the operator precedence explicit.}}
    } while ((1 & 2 | 3) > 42); // Noncompliant {{Add parentheses to make the operator precedence explicit.}}

    for (int j = 1 & 2 | 3;;) { // Noncompliant {{Add parentheses to make the operator precedence explicit.}}
    }
    for (; (1 & 2 | 3) > 42;) { // Noncompliant {{Add parentheses to make the operator precedence explicit.}}
    }
    for (int j = 0;; j = 1 & 2 | 3) { // Noncompliant {{Add parentheses to make the operator precedence explicit.}}
    }
    for (;;) {
      o = 1 & 2 | 3; // Noncompliant {{Add parentheses to make the operator precedence explicit.}}
    }

    if ((1 & 2 | 3) > 42) { // Noncompliant {{Add parentheses to make the operator precedence explicit.}}
      o = 1 & 2 | 3; // Noncompliant {{Add parentheses to make the operator precedence explicit.}}
    } else {
      o = 1 & 2 | 3; // Noncompliant {{Add parentheses to make the operator precedence explicit.}}
    }

    return 1 & 2 | 3; // Noncompliant {{Add parentheses to make the operator precedence explicit.}}
    return a; // Compliant

    switch (1 & 2 | 3) { // Noncompliant {{Add parentheses to make the operator precedence explicit.}}
      case 0:
        o = 1 & 2 | 3; // Noncompliant {{Add parentheses to make the operator precedence explicit.}}
    }

    throw new RuntimeException(); // Compliant

    while ((1 & 2 | 3) > 42) { // Noncompliant {{Add parentheses to make the operator precedence explicit.}}
      o = 1 & 2 | 3; // Noncompliant {{Add parentheses to make the operator precedence explicit.}}
    }

    o = 1 + 2 << 3; // Noncompliant {{Add parentheses to make the operator precedence explicit.}}
    o = 1 + 2 & 3; // Noncompliant {{Add parentheses to make the operator precedence explicit.}}
    o = 1 + 2 ^ 3; // Noncompliant {{Add parentheses to make the operator precedence explicit.}}
    o = 1 + 2 | 3; // Noncompliant {{Add parentheses to make the operator precedence explicit.}}

    o = 1 << 2 & 3; // Noncompliant {{Add parentheses to make the operator precedence explicit.}}
    o = 1 << 2 ^ 3; // Noncompliant {{Add parentheses to make the operator precedence explicit.}}
    o = 1 << 2 | 3; // Noncompliant {{Add parentheses to make the operator precedence explicit.}}

    o = 1 & 2 ^ 3; // Noncompliant {{Add parentheses to make the operator precedence explicit.}}
    o = 1 & 2 | 3; // Noncompliant {{Add parentheses to make the operator precedence explicit.}}

    o = 1 ^ 2 | 3; // Noncompliant {{Add parentheses to make the operator precedence explicit.}}

    o = t && t || t; // Noncompliant {{Add parentheses to make the operator precedence explicit.}}

    if (t = t = true) { // Compliant
    }
    if (t = a == i) { // Noncompliant {{Add parentheses to make the operator precedence explicit.}}
    }
    String[] strings = (value > 0) ? getValue() : new String[0]; // Compliant
    java.util.concurrent.Executor executor = (ex() == null) ? Runnable::run : ex(); // compliant
  }

  @interface Annotation1 {
    int value();
  }
  @interface Annotation2 {
    int value();
  }
  @interface Annotation3 {
    int key();
  }
  @interface Annotation4 {
    int key();
  }

  abstract int foo();

  abstract int bar();

  abstract String[] getValue();

  protected abstract java.util.concurrent.Executor ex();
}
