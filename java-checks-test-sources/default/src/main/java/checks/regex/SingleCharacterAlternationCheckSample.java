package checks.regex;

import javax.validation.constraints.Pattern;

public class SingleCharacterAlternationCheckSample {

  @Pattern(regexp = "x|y|z") // Noncompliant {{Replace this alternation with a character class.}}
//                   ^^^^^
  String pattern;

  @jakarta.validation.constraints.Pattern(regexp = "x|y|z") // Noncompliant {{Replace this alternation with a character class.}}
//                                                  ^^^^^
  String jakartaPattern;

  void nonCompliant() {
    String str = "abc123";
    str.matches("a|b|c"); // Noncompliant {{Replace this alternation with a character class.}}
//               ^^^^^
    str.matches("a|(b|c)"); // Noncompliant
//                  ^^^
    str.matches("abcd|(e|f)gh"); // Noncompliant
//                     ^^^
    str.matches("(a|b|c)*"); // Noncompliant
//                ^^^^^
    str.matches("\\d|x"); // Noncompliant
    str.matches("\\P{L}|\\d"); // Noncompliant
    str.matches("\\u1234|\\x{12345}"); // Noncompliant
    str.matches("😂|😊"); // Noncompliant
    str.matches("\ud800\udc00|\udbff\udfff"); // Noncompliant
    str.matches("[😂😊]"); // Compliant
    str.matches("[\ud800\udc00\udbff\udfff]"); // Compliant
  }

  void compliant() {
    String str = "abc123";
    str.matches("ab|cd");
    str.matches("a|\\b|c");
    str.matches("^|$");
    str.matches("|");
  }

}
