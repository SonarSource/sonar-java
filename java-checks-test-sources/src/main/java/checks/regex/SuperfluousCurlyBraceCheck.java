package checks.regex;

import static java.util.regex.Pattern.compile;

public class SuperfluousCurlyBraceCheck {

  void noncompliant() {
    compile("ab{1,1}c"); // Noncompliant [[sc=16;ec=21]] {{Remove this unnecessary quantifier.}}
    compile("ab{1}c");   // Noncompliant [[sc=16;ec=19]] {{Remove this unnecessary quantifier.}}
    compile("ab{0,0}c"); // Noncompliant [[sc=15;ec=21]] {{Remove this unnecessarily quantified expression.}}
    compile("ab{0}c");   // Noncompliant [[sc=15;ec=19]] {{Remove this unnecessarily quantified expression.}}
  }

  void compliant() {
    compile("abc");
    compile("ac");
  }

}
