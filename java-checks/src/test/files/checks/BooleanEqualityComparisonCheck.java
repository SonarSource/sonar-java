class A {
  public void f() {
    var == false;       // Non-Compliant
    var == true;        // Non-Compliant
    var != false;       // Non-Compliant
    var != true;        // Non-Compliant
    false == var;       // Non-Compliant
    true == var;        // Non-Compliant
    false != var;       // Non-Compliant
    true != var;        // Non-Compliant

    var == foo(true);   // Compliant
  }
}
