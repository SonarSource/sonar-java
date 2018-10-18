public class Class {

  @Annotation1(1 + 2 + 3) // Compliant
  @Annotation2(1 & 2 | 3) // Noncompliant [[sc=16;ec=21]] {{Add parentheses to make the operator precedence explicit.}}
  @Annotation3(key = 1 + 2 + 3) // Compliant
  @Annotation4(key = 1 & 2 | 3) // Noncompliant [[sc=22;ec=27]] {{Add parentheses to make the operator precedence explicit.}}
  public void method(int[] array, int value) {
    int a;
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
      @Override
      public int method() {
        return 1 & 2; // Compliant
      }
    }.hashCode();

    b = 1 == 2 ? 1 + 1 : 1 - 1; // Noncompliant
    b = 1 == 2 ? (1 + 1) : (1 - 1); // compliant
    a ? a ? b : c : c; // Noncompliant
    a ? foo(): bar(); // Compliant
    a ? foo(): new bar(); // Compliant
    a ? foo(): (Type) casted; // Compliant
    a ? foo[1]: b; // Compliant
    a ? -1: b; // Compliant
    a ? +1: b; // Compliant
    a ? ++i: b; // Compliant
    a ? i: i++; // Compliant

    b = b = 1; // Compliant

    int d = b = a; // Compliant

    do {
      1 & 2 | 3; // Noncompliant {{Add parentheses to make the operator precedence explicit.}}
    } while (1 & 2 | 3); // Noncompliant {{Add parentheses to make the operator precedence explicit.}}

    for (1 & 2 | 3;;) { // Noncompliant {{Add parentheses to make the operator precedence explicit.}}
    }
    for (; 1 & 2 | 3;) { // Noncompliant {{Add parentheses to make the operator precedence explicit.}}
    }
    for (;; 1 & 2 | 3) { // Noncompliant {{Add parentheses to make the operator precedence explicit.}}
    }
    for (;;) {
      1 & 2 | 3; // Noncompliant {{Add parentheses to make the operator precedence explicit.}}
    }

    if (1 & 2 | 3) { // Noncompliant {{Add parentheses to make the operator precedence explicit.}}
      1 & 2 | 3; // Noncompliant {{Add parentheses to make the operator precedence explicit.}}
    } else {
      1 & 2 | 3; // Noncompliant {{Add parentheses to make the operator precedence explicit.}}
    }

    return 1 & 2 | 3; // Noncompliant {{Add parentheses to make the operator precedence explicit.}}
    return a; // Compliant

    switch (1 & 2 | 3) { // Noncompliant {{Add parentheses to make the operator precedence explicit.}}
      case 0:
        1 & 2 | 3; // Noncompliant {{Add parentheses to make the operator precedence explicit.}}
    }

    throw 1 & 2 | 3; // Noncompliant {{Add parentheses to make the operator precedence explicit.}}
    throw new RuntimeException(); // Compliant

    while (1 & 2 | 3) { // Noncompliant {{Add parentheses to make the operator precedence explicit.}}
      1 & 2 | 3; // Noncompliant {{Add parentheses to make the operator precedence explicit.}}
    }

    1 + 2 << 3; // Noncompliant {{Add parentheses to make the operator precedence explicit.}}
    1 + 2 & 3; // Noncompliant {{Add parentheses to make the operator precedence explicit.}}
    1 + 2 ^ 3; // Noncompliant {{Add parentheses to make the operator precedence explicit.}}
    1 + 2 | 3; // Noncompliant {{Add parentheses to make the operator precedence explicit.}}

    1 << 2 & 3; // Noncompliant {{Add parentheses to make the operator precedence explicit.}}
    1 << 2 ^ 3; // Noncompliant {{Add parentheses to make the operator precedence explicit.}}
    1 << 2 | 3; // Noncompliant {{Add parentheses to make the operator precedence explicit.}}

    1 & 2 ^ 3; // Noncompliant {{Add parentheses to make the operator precedence explicit.}}
    1 & 2 | 3; // Noncompliant {{Add parentheses to make the operator precedence explicit.}}

    1 ^ 2 | 3; // Noncompliant {{Add parentheses to make the operator precedence explicit.}}

    1 && 2 || 3; // Noncompliant {{Add parentheses to make the operator precedence explicit.}}

    if ( a = b = c) { // Compliant
    }
    if ( a = b == c) { // Noncompliant {{Add parentheses to make the operator precedence explicit.}}
    }
    String[] array = (value > 0) ? getValue() : new String[0]; // Compliant
    java.util.concurrent.Executor executor = (exParm == null) ? Runnable::run : exParm; // compliant
  }
}
