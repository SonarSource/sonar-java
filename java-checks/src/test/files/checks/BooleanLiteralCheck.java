class A {

  public void f(boolean var, boolean foo) {
    boolean[] tests = {
    var == false,       // Noncompliant [[sc=12;ec=17]] {{Remove the literal "false" boolean value.}}
    var == true,        // Noncompliant {{Remove the literal "true" boolean value.}}
    var != false,       // Noncompliant {{Remove the literal "false" boolean value.}}
    var != true,        // Noncompliant {{Remove the literal "true" boolean value.}}
    false == var,       // Noncompliant {{Remove the literal "false" boolean value.}}
    true == var,        // Noncompliant {{Remove the literal "true" boolean value.}}
    false != var,       // Noncompliant {{Remove the literal "false" boolean value.}}
    true != var,        // Noncompliant {{Remove the literal "true" boolean value.}}
    !true,              // Noncompliant {{Remove the literal "true" boolean value.}}
    !false,             // Noncompliant {{Remove the literal "false" boolean value.}}
    false && foo(),     // Noncompliant {{Remove the literal "false" boolean value.}}
    foo() || true,      // Noncompliant {{Remove the literal "true" boolean value.}}

    var == foo(true),   // Compliant
    !foo,               // Compliant
    foo() && bar()      // Compliant
    };

    boolean exp = foo();
    var = foo() ? true : false; // Noncompliant
    var = foo() ? true : exp;   // Noncompliant
    var = foo() ? false : exp;  // Noncompliant
    var = foo() ? exp : true;   // Noncompliant
    var = foo() ? exp : false;  // Noncompliant

    var = foo();
    var = foo() || exp;
    var = !foo() && exp;
    var = !foo() || exp;
    var = foo() && exp;
  }

  boolean foo()          { return true; }
  boolean foo(boolean b) { return b;    }
  boolean bar()          { return true; }
}
