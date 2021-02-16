package checks;

class BooleanLiteralCheck {

  public void f(boolean var, boolean foo) {
    boolean[] tests = {
    var == false,       // Noncompliant [[sc=12;ec=17]] {{Remove the unnecessary boolean literal.}}
    var == true,        // Noncompliant {{Remove the unnecessary boolean literal.}}
    var != false,       // Noncompliant
    var != true,        // Noncompliant
    false == var,       // Noncompliant
    true == var,        // Noncompliant
    false != var,       // Noncompliant
    true != var,        // Noncompliant
    !true,              // Noncompliant
    !false,             // Noncompliant
    false && foo(),     // Noncompliant
    foo() || true,      // Noncompliant
    true || false,      // Noncompliant [[sc=5;ec=9;secondary=+0]] {{Remove the unnecessary boolean literals.}}

    var == foo(true),   // Compliant
    !foo,               // Compliant
    foo() && bar()      // Compliant
    };

    boolean exp = foo();
    var = foo() ? true : // Noncompliant [[sc=19;ec=23;secondary=+1]] {{Remove the unnecessary boolean literals.}}
      false;
    var = foo() ? true : exp;   // Noncompliant
    var = foo() ? false : exp;  // Noncompliant
    var = foo() ? exp : true;   // Noncompliant
    var = foo() ? exp : false;  // Noncompliant

    Boolean b1 = foo() ? true : null;  // Compliant
    Boolean b2 = foo() ? exp : null;   // Compliant
    Boolean b3 = foo() ? null : false; // Compliant
    Boolean b4 = foo() ? null : exp;   // Compliant

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
