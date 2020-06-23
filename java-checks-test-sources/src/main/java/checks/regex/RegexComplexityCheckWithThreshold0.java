package checks.regex;

public class RegexComplexityCheckWithThreshold0 {

  void compliant(String str) {
    str.matches("");
    str.matches("abc");
    str.matches("(?:abc)");
    str.matches("(abc)");
    str.matches("\\w.c");
  }

  void nonCompliant(String str) {
    str.matches("(?=abc)"); // Noncompliant {{Simplify this regular expression to reduce its complexity from 1 to the 0 allowed.}}
    str.matches("(?>abc)"); // Noncompliant {{Simplify this regular expression to reduce its complexity from 1 to the 0 allowed.}}
    str.matches("(?i:abc)"); // Noncompliant {{Simplify this regular expression to reduce its complexity from 1 to the 0 allowed.}}
    str.matches("(?i:a(?-i:bc))"); // Noncompliant {{Simplify this regular expression to reduce its complexity from 3 to the 0 allowed.}}
    str.matches("(?i)abc"); // Noncompliant {{Simplify this regular expression to reduce its complexity from 1 to the 0 allowed.}}
    str.matches("[a-z0-9]]"); // Noncompliant {{Simplify this regular expression to reduce its complexity from 1 to the 0 allowed.}}
    str.matches("[a-z&&0-9]]"); // Noncompliant {{Simplify this regular expression to reduce its complexity from 2 to the 0 allowed.}}
    str.matches("[a-z&&0-9&&b-d]]"); // Noncompliant {{Simplify this regular expression to reduce its complexity from 3 to the 0 allowed.}}

    str.matches("x*"); // Noncompliant {{Simplify this regular expression to reduce its complexity from 1 to the 0 allowed.}}
    str.matches("(?:abc)*"); // Noncompliant {{Simplify this regular expression to reduce its complexity from 1 to the 0 allowed.}}
    str.matches("((?:abc)*)"); // Noncompliant {{Simplify this regular expression to reduce its complexity from 1 to the 0 allowed.}}
    str.matches("((?:abc)*)?"); // Noncompliant {{Simplify this regular expression to reduce its complexity from 3 to the 0 allowed.}}

    str.matches("a|b"); // Noncompliant {{Simplify this regular expression to reduce its complexity from 1 to the 0 allowed.}}
    str.matches("a|b|c"); // Noncompliant {{Simplify this regular expression to reduce its complexity from 2 to the 0 allowed.}}
    str.matches("(?:a|b)*"); // Noncompliant {{Simplify this regular expression to reduce its complexity from 3 to the 0 allowed.}}
    str.matches("(?:a|b|c)*"); // Noncompliant {{Simplify this regular expression to reduce its complexity from 4 to the 0 allowed.}}
  }


  void partiallyKnown(String str, String pat1, String pat2) {
    str.matches(pat1 + pat2);
    str.matches("" + (2 * 21));
    str.matches(pat1 + pat2 + "x*"); // Noncompliant {{Simplify this regular expression to reduce its complexity from 1 to the 0 allowed.}}
  }

}
