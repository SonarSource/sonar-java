import java.util.Optional;
class A {

  void emptyFor(Boolean B) {
    for (;;) {
      foo();
    }
  }

  void boxedFor1(Boolean B) {
    for (;B;) { // Noncompliant {{Use the primitive boolean expression here.}}
      foo();
    }
  }

  void boxedFor2(Boolean B) {
    for (;Boolean.TRUE.equals(B);) { // Compliant
      foo();
    }
  }

  void primitiveFor(boolean b) {
    for (;b;) {
      foo();
    }
  }

  void nullCheckFor(Boolean B) {
    for (;B != null;) {
      foo();
    }
  }

  void boxedWhile1(Boolean B) {
    while (B) { // Noncompliant
      foo();
    }
  }

  void boxedWhil2(Boolean B) {
    while (Boolean.TRUE.equals(B)) { // Compliant
      foo();
    }
  }

  void primitiveWhile(boolean b) {
    while (b) {
      foo();
    }
  }

  void nullCheckWhile(Boolean B) {
    while (B != null) {
      foo();
    }
  }

  void boxedDoWhile1(Boolean B) {
    do {
      foo();
    } while (B); // Noncompliant
  }

  void boxedDoWhile2(Boolean B) {
    do {
      foo();
    } while (Boolean.TRUE.equals(B)); // Compliant
  }

  void primitiveDoWhile(boolean b) {
    do {
      foo();
    } while (b);
  }

  void nullCheckDoWhile(Boolean B) {
    do {
      foo();
    } while (B != null);
  }

  void boxedIf1(Boolean B) {
    if (B) { // Noncompliant
      foo();
    } else {
      bar();
    }
  }

  void boxedIf2(Boolean B) {
    if (Boolean.TRUE.equals(B)) { // Compliant
      foo();
    } else {
      bar();
    }
  }

  void primitiveIf(boolean b) {
    if (b) {
      foo();
    } else {
      bar();
    }
  }

  void nullCheckIf(Boolean B) {
    if (B != null) {
      foo();
    } else {
      bar();
    }
  }

  void boxedNotEqual1(Boolean B, boolean b) {
    if (B != b) { // Noncompliant
      foo();
    } else {
      bar();
    }
  }

  void boxedNotEqual2(Boolean B, boolean b) {
    if (b != B) { // Noncompliant
      foo();
    } else {
      bar();
    }
  }

  void boxedNotEqual3(Boolean B, Boolean V) {
    if (B != V) {
      foo();
    } else {
      bar();
    }
  }

  void primitiveNotEqual(boolean b, boolean v) {
    if (b != v) {
      foo();
    } else {
      bar();
    }
  }

  void boxedEqual1(Boolean B, boolean b) {
    if (B == b) { // Noncompliant
      foo();
    } else {
      bar();
    }
  }

  void boxedEqual2(Boolean B, boolean b) {
    if (b == B) { // Noncompliant
      foo();
    } else {
      bar();
    }
  }

  void boxedEqual3(Boolean B, Boolean V) {
    if (B == V) {
      foo();
    } else {
      bar();
    }
  }

  void nullCheck1(Boolean B) {
    if (B == null) {
      foo();
    } else {
      bar();
    }
  }

  void nullCheck2(Boolean B) {
    if (B != null) {
      foo();
    } else {
      bar();
    }
  }

  void nullCheck3(Boolean B) {
    if (B != null && B) {
      foo();
    } else {
      bar();
    }
  }

  void nullCheck4(Boolean B) {
    if (null != B && B) {
      foo();
    } else {
      bar();
    }
  }

  void compoundCheck(Boolean B, boolean b, boolean v) {
    if (b != v && B) { // Noncompliant
      foo();
    } else {
      bar();
    }
  }

  void primitiveEqual(boolean b, boolean v) {
    if (b == v) {
      foo();
    } else {
      bar();
    }
  }

  void boxedComplement(Boolean B) {
    if (!B) { // Noncompliant
      foo();
    } else {
      bar();
    }
  }

  void primitiveComplement(boolean b) {
    if (!b) {
      foo();
    } else {
      bar();
    }
  }

  void boxedFunction1() {
    if (True()) { // Noncompliant
      foo();
    } else {
      bar();
    }
  }

  void boxedFunction2(Boolean B) {
    if (True() && B) {
      foo();
    } else {
      bar();
    }
  }

  void boxedFunction3(boolean b) {
    if (True() || b) { // Noncompliant
      foo();
    } else {
      bar();
    }
  }

  void boxedFunction4() {
    if (True() == False()) {
      foo();
    } else {
      bar();
    }
  }

  void boxedOptional() {
    if (Optional.of(True()).orElse(False())) {
      foo();
    } else {
      bar();
    }
  }

  void boxedOptional2() {
    if (Optional.of(True()).orElse(null)) { // Noncompliant
      foo();
    } else {
      bar();
    }
  }

  Object boxedConditional1(Boolean B) {
    return B ? foo() : bar(); // Noncompliant
  }

  Object boxedConditional2(Boolean B) {
    return Boolean.TRUE.equals(B) ? foo() : bar(); // Compliant
  }

  Object boxedConditional3(Boolean B, boolean b) {
    return B != b ? foo() : bar(); // Noncompliant
  }

  Object nullCheckConditional(Boolean B) {
    return B != null ? foo() : bar();
  }

  Object primitiveConditional(boolean b) {
    return b ? foo() : bar();
  }

  Boolean True() {
    return Boolean.TRUE;
  }

  Boolean False() {
    return Boolean.FALSE;
  }
}
