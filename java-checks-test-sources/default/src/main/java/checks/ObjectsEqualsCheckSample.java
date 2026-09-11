package checks;

import java.util.Objects;

class ObjectsEqualsCheckSample {
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
    if (null == a ? b == null : a.equals(b)) { // Compliant - different structure
      //
    }
    if (b == null ? a == null : b.equals(a)) { // Compliant - different structure
      //
    }
  }

  void testWrongFalseBranch() {
    if (a != null ? a.equals(b) : b != null) { // Compliant - wrong false branch
      //
    }
    if (a != null ? a.equals(b) : true) { // Compliant - wrong false branch
      //
    }
    if (a != null ? a.equals(b) : false) { // Compliant - wrong false branch
      //
    }
  }

  void testDifferentVariables() {
    if (a != null ? a.equals(b) : c == null) { // Compliant - different variable in false branch
      //
    }
    if (a != null ? a.equals(b) : a == null) { // Compliant - wrong variable in false branch
      //
    }
  }

  void testWrongReceiver() {
    if (a != null ? b.equals(a) : a == null) { // Compliant - wrong receiver
      //
    }
    if (b != null ? a.equals(b) : b == null) { // Compliant - different receiver
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
    if ((a != null) ? (a.equals(b)) : (b == null)) { // Noncompliant
      //
    }
  }

  void testQualifiedIdentifiers() {
    if (this.a != null ? this.a.equals(b) : b == null) { // Noncompliant
      //
    }
    if (this.a != null ? this.a.equals(this.b) : this.b == null) { // Noncompliant
      //
    }
  }

  void testFieldAccess() {
    MyClass obj = new MyClass();
    if (obj.field != null ? obj.field.equals(other) : other == null) { // Noncompliant
      //
    }
    if (obj.field != null ? obj.field.equals(obj.other) : obj.other == null) { // Noncompliant
      //
    }
  }

  void testStaticFieldAccess() {
    if (MyClass.staticField != null ? MyClass.staticField.equals(other) : other == null) { // Noncompliant
      //
    }
    if (MyClass.staticField != null ? MyClass.staticField.equals(MyClass.staticOther) : MyClass.staticOther == null) { // Noncompliant
      //
    }
  }

  void testNestedTernary() {
    if (a != null ? (b != null ? b.equals(c) : c == null) : c == null) { // Noncompliant
      //
    }
    if (a != null ? a.equals(b) : (c != null ? c.equals(d) : d == null)) { // Noncompliant
      //
    }
  }

  void testMultipleEqualsCalls() {
    // Method calls are not matched (accepted false negative)
    if (getA() != null ? getA().equals(getB()) : getB() == null) { // Compliant
      //
    }
  }

  void testEqualsWithDifferentArgs() {
    if (a != null ? a.equals(b) : b == null) { // Noncompliant
      //
    }
    // Accepted false negative: complex expression with && changes AST structure
    if (a != null ? a.equals(b) : b == null && c != null ? c.equals(d) : d == null) { // Compliant
      //
    }
  }

  void testNullOnLeftSide() {
    if (null != a ? a.equals(b) : b == null) { // Noncompliant
      //
    }
  }

  void testWithMethodChaining() {
    if (a != null ? a.toString().equals(b.toString()) : b == null) { // Compliant - not a direct equals on the variable
      //
    }
  }

  void testCompliantVariations() {
    if (a != null && a.equals(b)) { // Compliant - not a ternary
      //
    }
    if (a == null || !a.equals(b)) { // Compliant - different structure
      //
    }
    if (a != null ? b.equals(a) : a == null) { // Compliant - receiver doesn't match
      //
    }
  }

  static class MyClass {
    Object field;
    Object other;
    static Object staticField;
    static Object staticOther;
  }

  Object getA() { return a; }
  Object getB() { return b; }
}
