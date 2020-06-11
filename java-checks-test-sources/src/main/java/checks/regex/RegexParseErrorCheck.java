package checks.regex;

import java.util.regex.Pattern;

public class RegexParseErrorCheck {

  void noncompliant(String str) {
    Pattern.compile("("); // Noncompliant [[sc=23;ec=24;secondary=8]] {{Fix the syntax error inside this regex.}}

    str.matches("("); // Noncompliant
    str.replaceAll("(", "{"); // Noncompliant

    str.replaceAll("x{1,2,3}|(", "x"); // Noncompliant [[sc=26;ec=27;secondary=13,13]] {{Fix the syntax errors inside this regex.}}

    str.matches("(\\w+-(\\d+)"); // Noncompliant [[sc=30;ec=31;secondary=15]] {{Fix the syntax error inside this regex.}}

    str.matches("(\\w+)-\\2"); // False Negative - group numbers not handled in rule

    str.matches("(?<name>\\w+)-\\k<nae>"); // False Negative - group names not handled in rule
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
    str.matches("(\\w+)-\\1");
    str.matches("(?<name>\\w+)-\\k<name>");
  }
}
