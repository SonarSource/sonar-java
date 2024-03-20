package checks.regex;

import java.util.regex.Pattern;

public class UnquantifiedNonCapturingGroupCheckSample {

  void nonCompliant() {
    Pattern.compile("(?:number)");               // Noncompliant [[sc=22;ec=32]] {{Unwrap this unnecessarily grouped subpattern.}}
    Pattern.compile("(?:number)\\d{2}");         // Noncompliant
    Pattern.compile("(?:number(?:two){2})");     // Noncompliant [[sc=22;ec=42]]
    Pattern.compile("(?:number(?:two)){2}");     // Noncompliant [[sc=31;ec=38]]
    Pattern.compile("foo(?:number)bar");         // Noncompliant
    Pattern.compile("(?:)");                     // Noncompliant
  }

  void compliant() {
    Pattern.compile("(?:number)?+");
    Pattern.compile("number\\d{2}");
    Pattern.compile("(?:number)?\\d{2}");
    Pattern.compile("(?:number|string)"); // Compliant, exception
  }
}
