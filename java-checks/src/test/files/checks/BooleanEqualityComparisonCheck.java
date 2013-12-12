class A {
  public void f() {
    var == false;       // Noncompliant
    var == true;        // Noncompliant
    var != false;       // Noncompliant
    var != true;        // Noncompliant
    false == var;       // Noncompliant
    true == var;        // Noncompliant
    false != var;       // Noncompliant
    true != var;        // Noncompliant
    !true;              // Noncompliant
    !false;             // Noncompliant
    false && foo();     // Noncompliant
    foo() || true;      // Noncompliant

    var == foo(true);   // Compliant
    true < 0;           // Compliant
    ~true;              // Compliant
    ++ true;            // Compliant
    !foo;               // Compliant
    foo() && bar();     // Compliant
  }
}
