package checks;

import java.util.Objects;

class ObjectsEqualsCheckNoSemanticSample {
  Object a;
  Object b;
  Object c;
  Object other;

  void testBasicPatterns() {
    if (a != null ? a.equals(b) : b == null) { // Noncompliant {{Replace this manual null check with "Objects.equals()".}} [[sc=8;ec=48]]
      //
    }
    if (a != null ? a.equals(b) : null == b) { // Noncompliant [[sc=8;ec=48]]
      //
    }
    Object result = a != null ? a.equals(b) : b == null; // Noncompliant [[sc=19;ec=54]]
    boolean flag = a != null ? a.equals(b) : b == null; // Noncompliant [[sc=16;ec=51]]

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
    if ((a != null) ? a.equals(b) : b == null) { // Noncompliant [[sc=8;ec=49]]
      //
    }
    if (a != null ? (a.equals(b)) : (b == null)) { // Noncompliant [[sc=8;ec=51]]
      //
    }
  }

  void testQualifiedIdentifiers() {
    if (this.a != null ? this.a.equals(b) : b == null) { // Noncompliant [[sc=8;ec=54]]
      //
    }
  }

  void testFieldAccess() {
    MyClass obj = new MyClass();
    if (obj.field != null ? obj.field.equals(other) : other == null) { // Noncompliant [[sc=8;ec=68]]
      //
    }
  }

  void testStaticFieldAccess() {
    if (MyClass.staticField != null ? MyClass.staticField.equals(other) : other == null) { // Noncompliant [[sc=8;ec=88]]
      //
    }
  }

  void testNestedTernary() {
    if (a != null ? (b != null ? b.equals(c) : c == null) : c == null) { // Noncompliant [[sc=8;ec=66]]
      //
    }
  }

  void testNullOnLeftSide() {
    if (null != a ? a.equals(b) : b == null) { // Noncompliant [[sc=8;ec=48]]
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
