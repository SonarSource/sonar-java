public class Class {

  public void method(int[] array, int value) {
    int a;
    ;
    int b = 1 + 2 - 3; // Noncompliant {{Add parentheses to make the operator precedence explicit.}}
    b = +(1 + 2 / 3); // Noncompliant {{Add parentheses to make the operator precedence explicit.}}
    b = array[1 * 2 / 3]; // Noncompliant {{Add parentheses to make the operator precedence explicit.}}
    b = method(array, 1 + 2 << 3); // Noncompliant {{Add parentheses to make the operator precedence explicit.}}
    b = 1 == 2 ? 1 + 1 : 1 - 1; // Noncompliant {{Add parentheses to make the operator precedence explicit.}}
    b = b = 1; // Noncompliant {{Add parentheses to make the operator precedence explicit.}}

    do {
      a = a = 1; // Noncompliant {{Add parentheses to make the operator precedence explicit.}}
    } while (a = a == 1); // Noncompliant {{Add parentheses to make the operator precedence explicit.}}

    for (a = a = 1;;) { // Noncompliant {{Add parentheses to make the operator precedence explicit.}}
    }
    for (; a = a == 1;) { // Noncompliant {{Add parentheses to make the operator precedence explicit.}}
    }
    for (;; a = a = 1) { // Noncompliant {{Add parentheses to make the operator precedence explicit.}}
    }
    for (;;) {
      a = a = 1; // Noncompliant {{Add parentheses to make the operator precedence explicit.}}
    }

    if (a = f(b, c) == 1) { // Noncompliant {{Add parentheses to make the operator precedence explicit.}}
      a = a = 1; // Noncompliant {{Add parentheses to make the operator precedence explicit.}}
    } else {
      a = a = 1; // Noncompliant {{Add parentheses to make the operator precedence explicit.}}
    }

    switch (a = a == 1) { // Noncompliant {{Add parentheses to make the operator precedence explicit.}}
      case 0:
        a = a = 1; // Noncompliant {{Add parentheses to make the operator precedence explicit.}}
    }

    while (a = a == 1) { // Noncompliant {{Add parentheses to make the operator precedence explicit.}}
      a = a = 1; // Noncompliant {{Add parentheses to make the operator precedence explicit.}}
    }

    int b = +1 + +2; // Compliant
    b = 1 + 2 + 3; // Compliant
    b = array[1 + 2] - 3; // Compliant
    b = method(array, 1 + 2, 1 * 2) - 3;
    a = (1 == 2) ? (1 + 1) : (1 - 1); // Compliant
    a = (a = 1); // Compliant
    b = (1 + 2) - 3 - 4; // Compliant
    b = new int[] {1 + 2}[0] - 3; // Compliant
    int[] c = {1 + 2, 2 - 3, 3}; // Compliant
    b = 1 + new Object() {
      @Override
      public int method() {
        return 1 - 2; // Compliant
      }
    }.hashCode();

    if ((a = f(b, c)) == 1) { // Compliant
    }
  }
}
