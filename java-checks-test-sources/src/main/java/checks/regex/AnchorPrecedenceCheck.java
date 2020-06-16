package checks.regex;

public class AnchorPrecedenceCheck {

  void noncompliant(String str) {
    str.matches("^a|b|c$"); // Noncompliant [[sc=18;ec=25]] {{Group the alternatives together to get the intended precedence.}}
    str.matches("^a|b|cd"); // Noncompliant [[sc=18;ec=25]]
    str.matches("(?i)^a|b|cd"); // Noncompliant [[sc=18;ec=29]]
    str.matches("(?i:^a|b|cd)"); // Noncompliant [[sc=22;ec=29]]
    str.matches("a|b|c$"); // Noncompliant [[sc=18;ec=24]]
    str.matches("\\Aa|b|c\\Z"); // Noncompliant [[sc=18;ec=29]]
    str.matches("\\Aa|b|c\\z"); // Noncompliant [[sc=18;ec=29]]
    str.matches("\\ba|b|c\\b"); // Noncompliant [[sc=18;ec=29]]
  }

  void compliant(String str) {
    str.matches("^(?:a|b|c)$");
    str.matches("(?:^a)|b|(?:c$)");
    str.matches("^abc$");
    str.matches("a|b|c");
    str.matches("aa|bb|cc");
    str.matches("^");
    str.matches("^[abc]$");
    str.matches("|");
    str.matches("[");
    str.matches("(?i:^)a|b|c"); // False negative; we don't find the anchor if it's hidden inside a sub-expression
  }
}
