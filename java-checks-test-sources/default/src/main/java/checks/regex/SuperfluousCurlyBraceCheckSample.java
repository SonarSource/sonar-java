package checks.regex;

import static java.util.regex.Pattern.compile;

public class SuperfluousCurlyBraceCheckSample {

  void noncompliant() {
    compile("ab{1,1}c"); // Noncompliant {{Remove this unnecessary quantifier.}}
//             ^^^^^
    compile("ab{1}c"); // Noncompliant {{Remove this unnecessary quantifier.}}
//             ^^^
    compile("ab{0,0}c"); // Noncompliant {{Remove this unnecessarily quantified expression.}}
//            ^^^^^^
    compile("ab{0}c"); // Noncompliant {{Remove this unnecessarily quantified expression.}}
//            ^^^^
  }

  void compliant() {
    compile("abc");
    compile("ac");
  }

}
