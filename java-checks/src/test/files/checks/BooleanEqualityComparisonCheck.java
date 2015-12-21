class A {
  public void f() {
    var == false;       // Noncompliant [[sc=12;ec=17]] {{Remove the literal "false" boolean value.}}
    var == true;        // Noncompliant {{Remove the literal "true" boolean value.}}
    var != false;       // Noncompliant {{Remove the literal "false" boolean value.}}
    var != true;        // Noncompliant {{Remove the literal "true" boolean value.}}
    false == var;       // Noncompliant {{Remove the literal "false" boolean value.}}
    true == var;        // Noncompliant {{Remove the literal "true" boolean value.}}
    false != var;       // Noncompliant {{Remove the literal "false" boolean value.}}
    true != var;        // Noncompliant {{Remove the literal "true" boolean value.}}
    !true;              // Noncompliant {{Remove the literal "true" boolean value.}}
    !false;             // Noncompliant {{Remove the literal "false" boolean value.}}
    false && foo();     // Noncompliant {{Remove the literal "false" boolean value.}}
    foo() || true;      // Noncompliant {{Remove the literal "true" boolean value.}}

    var == foo(true);   // Compliant
    true < 0;           // Compliant
    ~true;              // Compliant
    ++ true;            // Compliant
    !foo;               // Compliant
    foo() && bar();     // Compliant
  }
}
