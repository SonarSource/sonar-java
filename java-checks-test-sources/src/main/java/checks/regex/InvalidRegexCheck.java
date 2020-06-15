package checks.regex;

import java.util.regex.Pattern;

public class InvalidRegexCheck {

  void noncompliant_syntax(String str) {
    Pattern.compile("("); // Noncompliant [[sc=23;ec=24;secondary=8]] {{Fix the syntax error inside this regex.}}

    str.matches("("); // Noncompliant
    str.replaceAll("(", "{"); // Noncompliant

    str.replaceAll("x{1,2,3}|(", "x"); // Noncompliant [[sc=26;ec=27;secondary=13,13]] {{Fix the syntax errors inside this regex.}}

    str.matches("(\\w+-(\\d+)"); // Noncompliant [[sc=30;ec=31;secondary=15]] {{Fix the syntax error inside this regex.}}
  }

  void noncompliant_backreference(String str) {
    str.matches("(?<name>\\w+)-\\k<nae>"); // Noncompliant {{Fix the back reference error inside this regex.}}

    str.matches(
      "(?<g1>ab)"
        + "\\k<g2>" // Noncompliant [[sc=12;ec=19;secondary=23,24]] {{Fix the back reference errors inside this regex.}}
        + "\\k<g3>"
        + "(?<g2>cd)"
        + "(?<g3>ed)");
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
    str.matches("(\\w+)-\\2"); // does not make compilation of the regex fail, even if there is no 2nd group at this point
    str.matches("(?<name>\\w+)-\\k<name>");
  }
}
