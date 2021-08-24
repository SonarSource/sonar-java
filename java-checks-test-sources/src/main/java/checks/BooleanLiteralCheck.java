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
    !true,              // Noncompliant [[sc=6;ec=10;quickfixes=qf_negation_true]]
    // fix@qf_negation_true {{Simplify the expression}}
    // edit@qf_negation_true [[sc=5;ec=10]] {{false}}
    !false,             // Noncompliant [[sc=6;ec=11;quickfixes=qf_negation_false]]
    // fix@qf_negation_false {{Simplify the expression}}
    // edit@qf_negation_false [[sc=5;ec=11]] {{true}}
    false && foo(),     // Noncompliant
    foo() || true,      // Noncompliant
    true || false,      // Noncompliant [[sc=5;ec=9;secondary=+0]] {{Remove the unnecessary boolean literals.}}

    var == foo(true),   // Compliant
    !foo,               // Compliant
    foo() && bar()      // Compliant
    };

    boolean exp = foo();
    var = foo() ? true : // Noncompliant [[sc=19;ec=23;secondary=+1;quickfixes=qf_cond1]] {{Remove the unnecessary boolean literals.}}
      false;
    // fix@qf_cond1 {{Simplify the expression}}
    // edit@qf_cond1 [[sc=16;el=+1;ec=12]] {{}}
    var = foo() ? false : true;   // Noncompliant [[sc=19;ec=24;quickfixes=qf_cond2]]
    // fix@qf_cond2 {{Simplify the expression}}
    // edit@qf_cond2 [[sc=16;ec=31]] {{}}
    // edit@qf_cond2 [[sc=11;ec=11]] {{!}}
    var = foo() ? true : exp;   // Noncompliant [[sc=19;ec=23;quickfixes=qf_cond3]]
    // fix@qf_cond3 {{Simplify the expression}}
    // edit@qf_cond3 [[sc=17;ec=25]] {{||}}
    var = foo() ? false : exp;  // Noncompliant [[sc=19;ec=24;quickfixes=qf_cond4]]
    // fix@qf_cond4 {{Simplify the expression}}
    // edit@qf_cond4 [[sc=11;ec=11]] {{!}}
    // edit@qf_cond4 [[sc=17;ec=26]] {{&&}}
    var = foo() ? exp : true;   // Noncompliant [[sc=25;ec=29;quickfixes=qf_cond5]]
    // fix@qf_cond5 {{Simplify the expression}}
    // edit@qf_cond5 [[sc=22;ec=29]] {{}}
    // edit@qf_cond5 [[sc=11;ec=11]] {{!}}
    // edit@qf_cond5 [[sc=17;ec=18]] {{||}}
    var = foo() ? exp : false;  // Noncompliant [[sc=25;ec=30;quickfixes=qf_cond6]]
    // fix@qf_cond6 {{Simplify the expression}}
    // edit@qf_cond6 [[sc=22;ec=30]] {{}}
    // edit@qf_cond6 [[sc=17;ec=18]] {{&&}}

    // The following are reported but we do not suggest a quick fix as it looks more like a bug (see S3923)
    var = foo() ? true : true;   // Noncompliant [[sc=19;ec=23;quickfixes=!]]
    var = foo() ? false : false;   // Noncompliant [[sc=19;ec=24;quickfixes=!]]

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

  void quickFixForEquality(boolean var) {
    boolean b;
    b = var == false;       // Noncompliant [[sc=16;ec=21;quickfixes=qf_equal1]]
    // fix@qf_equal1 {{Simplify the expression}}
    // edit@qf_equal1 [[sc=9;ec=9]] {{!}}
    // edit@qf_equal1 [[sc=12;ec=21]] {{}}
    b = var == true;        // Noncompliant [[sc=16;ec=20;quickfixes=qf_equal2]]
    // fix@qf_equal2 {{Simplify the expression}}
    // edit@qf_equal2 [[sc=12;ec=20]] {{}}
    b = var != false;       // Noncompliant [[sc=16;ec=21;quickfixes=qf_equal3]]
    // fix@qf_equal3 {{Simplify the expression}}
    // edit@qf_equal3 [[sc=12;ec=21]] {{}}
    b = var != true;        // Noncompliant [[sc=16;ec=20;quickfixes=qf_equal4]]
    // fix@qf_equal4 {{Simplify the expression}}
    // edit@qf_equal4 [[sc=9;ec=9]] {{!}}
    // edit@qf_equal4 [[sc=12;ec=20]] {{}}

    b = false == var;       // Noncompliant [[sc=9;ec=14;quickfixes=qf_equal5]]
    // fix@qf_equal5 {{Simplify the expression}}
    // edit@qf_equal5 [[sc=9;ec=18]] {{!}}
    b = true == var;        // Noncompliant [[sc=9;ec=13;quickfixes=qf_equal6]]
    // fix@qf_equal6 {{Simplify the expression}}
    // edit@qf_equal6 [[sc=9;ec=17]] {{}}
    b = false != var;       // Noncompliant [[sc=9;ec=14;quickfixes=qf_equal7]]
    // fix@qf_equal7 {{Simplify the expression}}
    // edit@qf_equal7 [[sc=9;ec=18]] {{}}
    b = true != var;        // Noncompliant [[sc=9;ec=13;quickfixes=qf_equal8]]
    // fix@qf_equal8 {{Simplify the expression}}
    // edit@qf_equal8 [[sc=9;ec=17]] {{!}}

    b = true == true; // Noncompliant [[sc=9;ec=13;quickfixes=qf_equal9]]
    // fix@qf_equal9 {{Simplify the expression}}
    // edit@qf_equal9 [[sc=9;ec=21]] {{true}}
    b = false == false; // Noncompliant [[sc=9;ec=14;quickfixes=qf_equal10]]
    // fix@qf_equal10 {{Simplify the expression}}
    // edit@qf_equal10 [[sc=9;ec=23]] {{true}}
    b = false == true; // Noncompliant [[sc=9;ec=14;quickfixes=qf_equal11]]
    // fix@qf_equal11 {{Simplify the expression}}
    // edit@qf_equal11 [[sc=9;ec=22]] {{false}}
    b = true == false; // Noncompliant [[sc=9;ec=13;quickfixes=qf_equal12]]
    // fix@qf_equal12 {{Simplify the expression}}
    // edit@qf_equal12 [[sc=9;ec=22]] {{false}}

    b = true != true; // Noncompliant [[sc=9;ec=13;quickfixes=qf_equal13]]
    // fix@qf_equal13 {{Simplify the expression}}
    // edit@qf_equal13 [[sc=9;ec=21]] {{false}}
    b = false != false; // Noncompliant [[sc=9;ec=14;quickfixes=qf_equal14]]
    // fix@qf_equal14 {{Simplify the expression}}
    // edit@qf_equal14 [[sc=9;ec=23]] {{false}}
    b = false != true; // Noncompliant [[sc=9;ec=14;quickfixes=qf_equal15]]
    // fix@qf_equal15 {{Simplify the expression}}
    // edit@qf_equal15 [[sc=9;ec=22]] {{true}}
    b = true != false; // Noncompliant [[sc=9;ec=13;quickfixes=qf_equal16]]
    // fix@qf_equal16 {{Simplify the expression}}
    // edit@qf_equal16 [[sc=9;ec=22]] {{true}}
  }

  void quickFixForAndAndOr(boolean var, boolean var2) {
    boolean b;
    b = f() || false; // Noncompliant [[sc=16;ec=21;quickfixes=qf_and_or1]]
    // fix@qf_and_or1 {{Simplify the expression}}
    // edit@qf_and_or1 [[sc=12;ec=21]] {{}}
    b = f() && true; // Noncompliant [[sc=16;ec=20;quickfixes=qf_and_or2]]
    // fix@qf_and_or2 {{Simplify the expression}}
    // edit@qf_and_or2 [[sc=12;ec=20]] {{}}

    // When the expression can have side effects, it is not always possible to simply fix the issue.
    // We are not suggesting a quick fix when it would require to extract the side effects.
    b = f() || true; // Noncompliant [[sc=16;ec=20;quickfixes=!]]
    b = f() && false; // Noncompliant [[sc=16;ec=21;quickfixes=!]]
    // However, the same expression without side effect can be replaced safely
    b = var || true; // Noncompliant [[sc=16;ec=20;quickfixes=qf_and_or3]]
    // fix@qf_and_or3 {{Simplify the expression}}
    // edit@qf_and_or3 [[sc=9;ec=16]] {{}}
    b = var && false; // Noncompliant [[sc=16;ec=21;quickfixes=qf_and_or4]]
    // fix@qf_and_or4 {{Simplify the expression}}
    // edit@qf_and_or4 [[sc=9;ec=16]] {{}}

    b = true || f(); // Noncompliant [[sc=9;ec=13;quickfixes=qf_and_or5]]
    // fix@qf_and_or5 {{Simplify the expression}}
    // edit@qf_and_or5 [[sc=13;ec=20]] {{}}
    b = false || f(); // Noncompliant [[sc=9;ec=14;quickfixes=qf_and_or6]]
    // fix@qf_and_or6 {{Simplify the expression}}
    // edit@qf_and_or6 [[sc=9;ec=18]] {{}}
    b = true && f(); // Noncompliant [[sc=9;ec=13;quickfixes=qf_and_or7]]
    // fix@qf_and_or7 {{Simplify the expression}}
    // edit@qf_and_or7 [[sc=9;ec=17]] {{}}
    b = false && f(); // Noncompliant [[sc=9;ec=14;quickfixes=qf_and_or8]]
    // fix@qf_and_or8 {{Simplify the expression}}
    // edit@qf_and_or8 [[sc=14;ec=21]] {{}}

    b = true || true; // Noncompliant [[sc=9;ec=13;quickfixes=qf_and_or9]]
    // fix@qf_and_or9 {{Simplify the expression}}
    // edit@qf_and_or9 [[sc=9;ec=21]] {{true}}
    b = false || true; // Noncompliant [[sc=9;ec=14;quickfixes=qf_and_or10]]
    // fix@qf_and_or10 {{Simplify the expression}}
    // edit@qf_and_or10 [[sc=9;ec=22]] {{true}}
    b = true || false; // Noncompliant [[sc=9;ec=13;quickfixes=qf_and_or11]]
    // fix@qf_and_or11 {{Simplify the expression}}
    // edit@qf_and_or11 [[sc=9;ec=22]] {{true}}
    b = false || false; // Noncompliant [[sc=9;ec=14;quickfixes=qf_and_or12]]
    // fix@qf_and_or12 {{Simplify the expression}}
    // edit@qf_and_or12 [[sc=9;ec=23]] {{false}}

    b = true && true; // Noncompliant [[sc=9;ec=13;quickfixes=qf_and_or13]]
    // fix@qf_and_or13 {{Simplify the expression}}
    // edit@qf_and_or13 [[sc=9;ec=21]] {{true}}
    b = false && true; // Noncompliant [[sc=9;ec=14;quickfixes=qf_and_or14]]
    // fix@qf_and_or14 {{Simplify the expression}}
    // edit@qf_and_or14 [[sc=9;ec=22]] {{false}}
    b = true && false; // Noncompliant [[sc=9;ec=13;quickfixes=qf_and_or15]]
    // fix@qf_and_or15 {{Simplify the expression}}
    // edit@qf_and_or15 [[sc=9;ec=22]] {{false}}
    b = false && false; // Noncompliant [[sc=9;ec=14;quickfixes=qf_and_or16]]
    // fix@qf_and_or16 {{Simplify the expression}}
    // edit@qf_and_or16 [[sc=9;ec=23]] {{false}}

    // Side effect can be nested into a more complex expression
    b = (b ? var : bar()) || true;  // Noncompliant [[sc=30;ec=34;quickfixes=!]]
    b = (b ? var : var2) || true;  // Noncompliant [[sc=29;ec=33;quickfixes=qf_side_effect]]
    // fix@qf_side_effect {{Simplify the expression}}
    // edit@qf_side_effect [[sc=9;ec=29]] {{}}

  }

  boolean foo()          { return true; }
  boolean foo(boolean b) { return b;    }
  boolean bar()          { return true; }
  boolean f()          { return true; }
}
