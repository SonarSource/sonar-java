package checks.regex;

import java.util.regex.Pattern;

public class UnquantifiedNonCapturingGroupCheck {

  void nonCompliant() {
    Pattern.compile("(?:number)"); // Noncompliant {{Unwrap this unnecessarily grouped subpattern.}}
//                   ^^^^^^^^^^
    Pattern.compile("(?:number)\\d{2}"); // Noncompliant
    Pattern.compile("(?:number(?:two){2})"); // Noncompliant
//                   ^^^^^^^^^^^^^^^^^^^^
    Pattern.compile("(?:number(?:two)){2}"); // Noncompliant
//                            ^^^^^^^
    Pattern.compile("foo(?:number)bar"); // Noncompliant
    Pattern.compile("(?:)"); // Noncompliant
  }

  void compliant() {
    Pattern.compile("(?:number)?+");
    Pattern.compile("number\\d{2}");
    Pattern.compile("(?:number)?\\d{2}");
    Pattern.compile("(?:number|string)"); // Compliant, exception
  }
}
