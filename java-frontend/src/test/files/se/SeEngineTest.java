class A0 {
  Object a;
  Object b;

  void simple_condition() {
    if (a == null)
      a.toString(); // Noncompliant
  }

  void switch_expression(int r) {
    r = switch (1) { default -> 42; };
  }

  void complex_condition() {
    if (b == a && a == null)
      a.toString(); // Noncompliant
  }

  void test_complex_condition_inverted() {
    if (null == a && b == a)
      a.toString(); // Noncompliant
  }

  void test_reassignement() {
    if (b == null) {
      a = b;
      a.toString(); // Noncompliant
    }
  }

  void test_null_assignement() {
    a = null;
    a.toString(); // Noncompliant
  }


  void tracking_symbolic_value() {
    Object a = null;
    Object b = new Object();
    b = a;
    if (b == null) { // Noncompliant {{Remove this expression which always evaluates to "true"}}
      a.toString(); // Noncompliant {{A "NullPointerException" could be thrown; "a" is nullable here.}}
    }
  }

  void test_assign_null() {
    if (a != null) {
      a = null;
      a.toString(); // Noncompliant
    }
  }

  void condition_always_true() {
    a = null;
    if (a == null) // Noncompliant
      ;
  }

  void condition_always_false() {
    a = null;
    b = null;
    if (a != null && b != null) // Noncompliant
      ;
  }

  void for_loop_condition() {
    a = null;
    for (; a != null;) {
      a.toString();
      break;
    }
  }

  void test_npe_in_conditional_and(String str) {
    boolean b1 = str == null
        && str.length() == 0; // Noncompliant {{A "NullPointerException" could be thrown; "str" is nullable here.}}
  }

  void instance_of_set_not_null_constraint(Object d) {
    Object c = null;
    if (c instanceof Object) { // Noncompliant {{Change this condition so that it does not always evaluate to "false"}}
      c.toString();
    }
    if (d instanceof Object) {
      d.toString(); //d is not null
    }

  }

  void lambdas() {
    foo(a -> a, x -> y);
  }

  boolean booleanField;
  Object objectField1;
  Object objectField2;
  boolean equals(Object obj) {
    if (obj == this) {
      return true;
    }

    if (obj instanceof A0) {
      A0 other = (A0) obj;
      return (booleanField ? other.booleanField : (other.objectField1 != null) && (objectField1 == other.objectField1)) && objectField2.equals(other.objectField2);
    } else {
      return false;
    }
  }

}

class DefaultValues {

  void localVars() {
    boolean a;  // no SV is created on local variable declaration, value is undefined as per JLS
    Object b;
    manyStatementsLater();
    inTheGalaxyFarFarAway();
    variablesAreInitialized();
    a = true; // flow@vars {{Implies 'a' is true.}}
    if (a) {  // Noncompliant [[flows=vars]] flow@vars

    }
    b = new Object();  // flow@vars2 flow@vars2
    if (b != null) {  // Noncompliant [[flows=vars2]] flow@vars2

    }
    try {
      Thread.sleep(10);
    } catch (Exception ex) {
      if (ex != null) {  // Noncompliant {{Remove this expression which always evaluates to "true"}}

      }
    }
    for (Integer i : Arrays.asList(1,2,null)) {
      if (i == null) { // Compliant

      }
      if (i != null) { // Compliant

      }
    }
  }
}

abstract class TestingOptionals {
  private void usingOfNullable(@javax.annotation.Nullable Object o) {
    // null value used with 'ofNullable' creates an empty optional
    java.util.Optional<Object> op = java.util.Optional.ofNullable(o);
    if (op.isPresent()) {
      o.toString(); // Compliant - 'o' is necessary not null here
    } else {
      o.toString(); // Noncompliant {{A "NullPointerException" could be thrown; "o" is nullable here.}} - 'o' is necessary null if not 'op' is empty
    }
    op.get(); // Compliant - empty case triggered a NPE
  }

  private java.util.Optional<File> usingFilters(java.util.Optional<String> relativePath) { // Compliant - not always same result
    if (relativePath.filter(this::isNotBlank).isPresent()) {
      java.io.File f = getFile(relativePath.get()); // Compliant
      return java.util.Optional.ofNullable(f);
    }
    return java.util.Optional.empty();
  }

  abstract boolean isNotBlank(@javax.annotation.Nullable String s);

  @javax.annotation.CheckForNull
  abstract File getFile(String path);
}

class ResetFieldValue {
  boolean b;
  static boolean b2 = true;
  Object o;

  void bar() {
    if (b) {
      getClass();
      if (b) { } // Noncompliant
    }

    if (o == null) {
      this.getClass();
      o.toString(); // Noncompliant
    }
  }
  void bar2() {
    if (b) {
      foo();
      if (b) { } // Noncompliant
    }
    if (b2) {
      foo();
      if (b2) { } // compliant : static call can update static field
    }

    if (o == null) {
      foo();
      o.toString(); // Noncompliant
    }
  }

  void qix() {
    if (b) {
      fun(this);
      if (b) { } // compliant, fun can modify internal state of this instance.
    }
  }

  public static void foo() {
    b2 = false;
  }
  public static void fun(Object o) {
    // could modify internal state of o
  }

  public static String divisionResultCanBeZero(long millis) {
    if (millis <= 0) {
      return "goodbye";
    }
    long seconds = millis / 1000;
    long minutes = seconds / 60;
    if (minutes == 0) { // minutes can be zero due to 'long' division
      return "zero";
    }
    return "non-zero";
  }

}
