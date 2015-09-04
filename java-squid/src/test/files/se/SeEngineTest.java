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

  void local_variable() {
    Object a;
    a.toString(); // Noncompliant
  }

  void tracking_symbolic_value() {
    Object a = null;
    Object b = new Object();
    b = a;
    if (b == null) { // Noncompliant {{Change this condition so that it does not always evaluate to true}}
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
    }
  }

  void test_npe_in_conditional_and() {
    boolean b1 = str == null
        && str.length() == 0; // Noncompliant
  }

  void instance_of_set_not_null_constraint(Object d) {
    Object c;
    if (c instanceof Object) { // Noncompliant {{Change this condition so that it does not always evaluate to false}}
      c.toString();
    }
    if (d instanceof Object) {
      d.toString(); //d is not null
    }

  }

}
