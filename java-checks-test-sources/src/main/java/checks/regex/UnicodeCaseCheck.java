package checks.regex;

import java.util.regex.Pattern;

public class UnicodeCaseCheck {

  void noncompliant(String str) {
    Pattern.compile("söme pättern", Pattern.CASE_INSENSITIVE); // Noncompliant [[sc=37;ec=61]] {{Also use "Pattern.UNICODE_CASE" to correctly handle non-ASCII letters.}}
    str.matches("(?i)söme pättern"); // Noncompliant [[sc=20;ec=21]] {{Also use the "u" flag to correctly handle non-ASCII letters.}}
    str.matches("(?i:söme) pättern"); // Noncompliant [[sc=20;ec=21]] {{Also use the "u" flag to correctly handle non-ASCII letters.}}

    String regexPart1 = "(?i:söme)"; // Noncompliant [[sc=28;ec=29]] {{Also use the "u" flag to correctly handle non-ASCII letters.}}
    String regexPart2 = "(?i:pättern)"; // Noncompliant [[sc=28;ec=29]] {{Also use the "u" flag to correctly handle non-ASCII letters.}}
    str.matches(regexPart1 + regexPart2);

    // In these cases the location of the issue is a bit confusing, but code like this will probably not occur in the wild
    str.matches("(?iu)söme (?-u)pättern"); // Noncompliant [[sc=20;ec=21]] {{Also use the "u" flag to correctly handle non-ASCII letters.}}
    str.matches("(?iu)söme (?-U)pättern"); // Noncompliant [[sc=20;ec=21]] {{Also use the "u" flag to correctly handle non-ASCII letters.}}
  }

  void complaint(String str) {
    Pattern.compile("söme pättern", Pattern.CASE_INSENSITIVE | Pattern.UNICODE_CASE);
    str.matches("(?iu)söme pättern");
    str.matches("(?iu:söme) pättern");
    str.matches("(?i)\uD83D\uDCA9"); // compliant because the string doesn't contain any letters
    str.matches("(?i:öäü"); // rule is not applied to syntactically invalid regexen

    // UNICODE_CHARACTER_CLASS implies UNICODE_CASE
    Pattern.compile("söme pättern", Pattern.CASE_INSENSITIVE | Pattern.UNICODE_CHARACTER_CLASS);
    str.matches("(?iU)söme pättern");
    str.matches("(?iU:söme) pättern");
  }
}
