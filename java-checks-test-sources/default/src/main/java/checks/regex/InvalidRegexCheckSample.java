package checks.regex;

import java.util.regex.Pattern;

public class InvalidRegexCheckSample {

  void noncompliant(String str) {
    Pattern.compile("("); // Noncompliant {{Fix the syntax error inside this regex.}}
//                    ^
//  ^^^<

    str.matches("("); // Noncompliant
    str.replaceAll("(", "{"); // Noncompliant

    str.replaceAll("x{1,2,3}|(", "x"); // Noncompliant {{Fix the syntax errors inside this regex.}}
//                       ^
//  ^^^<

    str.matches("(\\w+-(\\d+)"); // Noncompliant {{Fix the syntax error inside this regex.}}
//                           ^
//  ^^^<
  }

  void compliant(String str) {
    Pattern.compile("\\(\\[");
    Pattern.compile("([", Pattern.LITERAL);
    str.equals("([");
    str.replace("([", "{");

    str.matches("");
    str.matches("a|b");

    Pattern.compile("()");

    str.replaceAll("abc", "x");
    str.replaceAll("x{42}", "x");

    str.matches("(\\w+)-(\\d+)");
    // Errors in backreferences are handled by rule S6001
    str.matches("(\\w+)-\\2");
    str.matches("(?<name>\\w+)-\\k<name>");
  }

  @javax.validation.constraints.Pattern(regexp = "(") // Noncompliant {{Fix the syntax error inside this regex.}}
//                                                 ^
//  ^^^<
  String pattern;

  void unicode16(String str) {
    str.matches("[😂😊]"); // Compliant
    str.matches("[^\ud800\udc00-\udbff\udfff]"); // Compliant
    str.matches("[^\\ud800\\udc00-\\udbff\\udfff]"); // Compliant
  }

}
