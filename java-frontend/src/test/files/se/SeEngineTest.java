class A0 {
  Object a;
  Object b;

  void simple_condition() {
    if (a == null)
      a.toString(); // Noncompliant
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
    if (b == null) { // Noncompliant {{Change this condition so that it does not always evaluate to "true"}}
      a.toString(); // Noncompliant {{NullPointerException might be thrown as 'a' is nullable here}}
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
        && str.length() == 0; // Noncompliant {{NullPointerException might be thrown as 'str' is nullable here}}
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
    a = true; // flow@vars {{'a' is assigned true.}} flow@vars {{'a' is assigned non-null.}}
    if (a) {  // Noncompliant [[flows=vars]] flow@vars

    }
    b = new Object();  // flow@vars2
    if (b != null) {  // Noncompliant [[flows=vars2]] flow@vars2

    }
    try {
      Thread.sleep(10);
    } catch (Exception ex) {
      if (ex != null) {  // Noncompliant {{Change this condition so that it does not always evaluate to "true"}}

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
