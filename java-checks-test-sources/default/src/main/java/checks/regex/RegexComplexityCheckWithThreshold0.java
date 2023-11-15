package checks.regex;

public class RegexComplexityCheckWithThreshold0 {

  // Since this check is sensitive to comments, all comments that mark a regex as non-compliant or that explain
  // why it is (non-)compliant should not be on the same line as the regex or on the line before it.

  void compliant(String str) {
    str.matches("");
    str.matches("abc");
    str.matches("(?:abc)");
    str.matches("(?>abc)");
    str.matches("(abc)");
    str.matches("\\w.c");
  }

  void nonCompliant(String str) {
    // Noncompliant@+2 {{Simplify this regular expression to reduce its complexity from 1 to the 0 allowed.}}
    str.matches(
      "(?=abc)"
    );
    // Noncompliant@+2 {{Simplify this regular expression to reduce its complexity from 1 to the 0 allowed.}}
    str.matches(
      "(?i:abc)"
    );
    // Noncompliant@+2 {{Simplify this regular expression to reduce its complexity from 3 to the 0 allowed.}}
    str.matches(
      "(?i:a(?-i:bc))"
    );
    // Noncompliant@+2 {{Simplify this regular expression to reduce its complexity from 1 to the 0 allowed.}}
    str.matches(
      "(?i)abc"
    );
    // Noncompliant@+2 {{Simplify this regular expression to reduce its complexity from 1 to the 0 allowed.}}
    str.matches(
      "[a-z0-9]]"
    );
    // Noncompliant@+2 {{Simplify this regular expression to reduce its complexity from 2 to the 0 allowed.}}
    str.matches(
      "[a-z&&0-9]]"
    );
    // Noncompliant@+2 {{Simplify this regular expression to reduce its complexity from 3 to the 0 allowed.}}
    str.matches(
      "[a-z&&0-9&&b-d]]"
    );

    // Noncompliant@+2 {{Simplify this regular expression to reduce its complexity from 1 to the 0 allowed.}}
    str.matches(
      "x*"
    );
    // Noncompliant@+2 {{Simplify this regular expression to reduce its complexity from 1 to the 0 allowed.}}
    str.matches(
      "(?:abc)*"
    );
    // Noncompliant@+2 {{Simplify this regular expression to reduce its complexity from 1 to the 0 allowed.}}
    str.matches(
      "((?:abc)*)"
    );
    // Noncompliant@+2 {{Simplify this regular expression to reduce its complexity from 3 to the 0 allowed.}}
    str.matches(
      "((?:abc)*)?"
    );

    // Noncompliant@+2 {{Simplify this regular expression to reduce its complexity from 1 to the 0 allowed.}}
    str.matches(
      "a|b"
    );
    // Noncompliant@+2 {{Simplify this regular expression to reduce its complexity from 2 to the 0 allowed.}}
    str.matches(
      "a|b|c"
    );
    // Noncompliant@+2 {{Simplify this regular expression to reduce its complexity from 3 to the 0 allowed.}}
    str.matches(
      "(?:a|b)*"
    );
    // Noncompliant@+2 {{Simplify this regular expression to reduce its complexity from 4 to the 0 allowed.}}
    str.matches(
      "(?:a|b|c)*"
    );
  }


  void partiallyKnown(String str, String pat1, String pat2) {
    str.matches(pat1 + pat2);
    str.matches("" + (2 * 21));
    // Compliant because we ignore all parts of the regex if any part is unknown
    String known =
      "x*";
    str.matches(pat1 + pat2 + known);
  }

}
