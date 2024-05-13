package checks.regex;

import javax.validation.constraints.Email;

public class AnchorPrecedenceCheckSample {

  @Email(regexp = "^a|b|c$") // Noncompliant
//                 ^^^^^^^
  String email;

  @jakarta.validation.constraints.Email(regexp = "^a|b|c$") // Noncompliant
//                                                ^^^^^^^
  String email2;

  void noncompliant(String str) {
    str.matches("^a|b|c$"); // Noncompliant {{Group parts of the regex together to make the intended operator precedence explicit.}}
//               ^^^^^^^
    str.matches("^a|b|cd"); // Noncompliant
//               ^^^^^^^
    str.matches("(?i)^a|b|cd"); // Noncompliant
//               ^^^^^^^^^^^
    str.matches("(?i:^a|b|cd)"); // Noncompliant
//                   ^^^^^^^
    str.matches("a|b|c$"); // Noncompliant
//               ^^^^^^
    str.matches("\\Aa|b|c\\Z"); // Noncompliant
//               ^^^^^^^^^^^
    str.matches("\\Aa|b|c\\z"); // Noncompliant
//               ^^^^^^^^^^^
  }

  void compliant(String str) {
    str.matches("^(?:a|b|c)$");
    str.matches("(?:^a)|b|(?:c$)");
    str.matches("^abc$");
    str.matches("a|b|c");
    str.matches("^a$|^b$|^c$");
    str.matches("^a$|b|c");
    str.matches("a|b|^c$");
    str.matches("^a|^b$|c$");
    str.matches("^a|^b|c$");
    str.matches("^a|b$|c$");
    // Only beginning and end of line/input boundaries are considered - not word boundaries
    str.matches("\\ba|b|c\\b");
    str.matches("\\ba\\b|\\bb\\b|\\bc\\b");
    // If multiple alternatives are anchored, but not all, that's more likely to be intentional than if only the first
    // one were anchored, so we won't report an issue for the following line:
    str.matches("^a|^b|c");
    str.matches("aa|bb|cc");
    str.matches("^");
    str.matches("^[abc]$");
    str.matches("|");
    str.matches("[");
    str.matches("(?i:^)a|b|c"); // False negative; we don't find the anchor if it's hidden inside a sub-expression
  }
}
