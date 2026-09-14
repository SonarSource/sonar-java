package checks;

import java.util.Objects;

class ObjectsEqualsCheckNoSemanticSample {
  Object a;
  Object b;
  Object c;
  Object other;

  void testBasicPatterns() {
    if (a != null ? a.equals(b) : b == null) { // Noncompliant {{Replace this manual null check with "Objects.equals()".}}
      //
    }
    if (a != null ? a.equals(b) : null == b) { // Noncompliant
      //
    }
    Object result = a != null ? a.equals(b) : b == null; // Noncompliant
    boolean flag = a != null ? a.equals(b) : b == null; // Noncompliant

    if (Objects.equals(a, b)) { // Compliant
      //
    }
    if (a.equals(b)) { // Compliant
      //
    }
    if (a == b) { // Compliant
      //
    }
  }

  void testReversedConditions() {
    if (null == a ? b == null : a.equals(b)) { // Compliant
      //
    }
  }

  void testWrongFalseBranch() {
    if (a != null ? a.equals(b) : b != null) { // Compliant
      //
    }
    if (a != null ? a.equals(b) : true) { // Compliant
      //
    }
  }

  void testDifferentVariables() {
    if (a != null ? a.equals(b) : c == null) { // Compliant
      //
    }
    if (a != null ? a.equals(b) : a == null) { // Compliant
      //
    }
  }

  void testWrongReceiver() {
    if (a != null ? b.equals(a) : a == null) { // Compliant
      //
    }
  }

  void testParenthesizedExpressions() {
    if ((a != null) ? a.equals(b) : b == null) { // Noncompliant
      //
    }
    if (a != null ? (a.equals(b)) : (b == null)) { // Noncompliant
      //
    }
  }

  void testQualifiedIdentifiers() {
    if (this.a != null ? this.a.equals(b) : b == null) { // Noncompliant
      //
    }
  }

  void testFieldAccess() {
    MyClass obj = new MyClass();
    if (obj.field != null ? obj.field.equals(other) : other == null) { // Noncompliant
      //
    }
  }

  void testStaticFieldAccess() {
    if (MyClass.staticField != null ? MyClass.staticField.equals(other) : other == null) { // Noncompliant
      //
    }
  }

  void testNestedTernary() {
    if (a != null ? (b != null ? b.equals(c) : c == null) : c == null) { // Noncompliant
      //
    }
  }

  void testNullOnLeftSide() {
    if (null != a ? a.equals(b) : b == null) { // Noncompliant
      //
    }
  }

  void testNonEqualsMethod() {
    String s = "hello";
    if (s != null ? s.contains("x") : b == null) { // Compliant - not equals method
      //
    }
  }

  void testReceiverIsMethodInvocation() {
    if (a != null ? getA().equals(b) : b == null) { // Compliant - receiver is a method call
      //
    }
  }

  void testFalseBranchEqualToNonNull() {
    if (a != null ? a.equals(b) : b == c) { // Compliant - false branch compares two non-null values
      //
    }
  }

  void testImplicitThisEquals() {
    if (this != null ? equals(b) : b == null) { // Compliant - implicit this
      //
    }
  }

  Object getA() { return a; }

  static class MyClass {
    Object field;
    static Object staticField;
  }
}
