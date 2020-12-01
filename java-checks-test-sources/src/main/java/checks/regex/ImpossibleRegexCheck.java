package checks.regex;

public class ImpossibleRegexCheck {

  void noncompliantBackReferences(String str) {
    str.matches("\\1"); // Noncompliant [[sc=18;ec=21]] {{Remove this illegal back reference that can never match or rewrite the regex.}}
    str.matches("\\1(.)"); // Noncompliant [[sc=18;ec=21]]
    str.matches("(?:\\1(.))*"); // Noncompliant [[sc=21;ec=24]]
    str.matches("\\1|(.)"); // Noncompliant [[sc=18;ec=21]]
    // FP (IntelliJ has this FP too):
    str.matches("(?:\\1|x(.))*"); // Noncompliant [[sc=21;ec=24]]
  }

  void compliantBackReferences(String str) {
    str.matches("(.)\\1");
    str.matches("(?:x(.)|\\1)*");
    str.matches("(.)|\\1"); // FN (IntelliJ has this FN too)
    // Illegal named back references are handled by the illegal regex rule because they cause an exception rather than
    // just failing to match anything
    str.matches("\\k<name>(?<name>.)");
  }

  void nonCompliantBoundaries(String str) {
    str.matches("$[a-z]^"); // Noncompliant [[sc=18;ec=19;secondary=24,24]] {{Remove these subpatterns that can never match or rewrite the regex.}}
    str.matches("$[a-z]"); // Noncompliant [[sc=18;ec=19]] {{Remove this boundary that can never match or rewrite the regex.}}
    str.matches("$(abc)"); // Noncompliant [[sc=18;ec=19]]
    str.matches("[a-z]^"); // Noncompliant [[sc=23;ec=24]]
    str.matches("\\Z[a-z]"); // Noncompliant [[sc=18;ec=21]]
    str.matches("\\z[a-z]"); // Noncompliant [[sc=18;ec=21]]
    str.matches("[a-z]\\A"); // Noncompliant [[sc=23;ec=26]]
    str.matches("($)a"); // Noncompliant [[sc=19;ec=20]]
    str.matches("a$|$a"); // Noncompliant [[sc=21;ec=22]]
    str.matches("^a|a^"); // Noncompliant [[sc=22;ec=23]]
    str.matches("a(b|^)"); // Noncompliant [[sc=22;ec=23]]
    str.matches("(?=abc^)"); // Noncompliant [[sc=24;ec=25]]
  }

  void compliantBoundaries(String str) {
    str.matches("^[a-z]$");
    str.matches("^$");
    str.matches("^(?i)$");
    str.matches("^$(?i)");
    str.matches("^abc$|^def$");
    str.matches("(?i)^abc$");
    str.matches("()^abc$");
    str.matches("^abc$()");
    str.matches("^abc$\\b");
    str.matches("(?=abc)^abc$");
  }

  void nonCompliantWithBoth(String str) {
    str.matches("$\\1^"); // Noncompliant [[sc=18;ec=19;secondary=52,52,52]] {{Remove these subpatterns that can never match or rewrite the regex.}}
  }

  @org.hibernate.validator.constraints.URL(regexp = "a$|$a")  // Noncompliant [[sc=57;ec=58]] {{Remove this boundary that can never match or rewrite the regex.}}
  String url;

}
