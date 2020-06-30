package checks.regex;

public class ImpossibleRegexCheck {

  void noncompliantBackReferences(String str) {
    str.matches("\\1"); // Noncompliant [[sc=18;ec=21]] {{Remove this illegal back reference or rewrite the regex.}}
    str.matches("\\1(.)"); // Noncompliant [[sc=18;ec=21]] {{Remove this illegal back reference or rewrite the regex.}}
    str.matches("(?:\\1(.))*"); // Noncompliant [[sc=21;ec=24]] {{Remove this illegal back reference or rewrite the regex.}}
    str.matches("\\1|(.)"); // Noncompliant [[sc=18;ec=21]] {{Remove this illegal back reference or rewrite the regex.}}
    // FP (IntelliJ has this FP too):
    str.matches("(?:\\1|x(.))*"); // Noncompliant [[sc=21;ec=24]] {{Remove this illegal back reference or rewrite the regex.}}
  }

  void compliantBackReferences(String str) {
    str.matches("(.)\\1");
    str.matches("(?:x(.)|\\1)*");
    str.matches("(.)|\\1"); // FN (IntelliJ has this FN too)
    // Illegal named back references are handled by the illegal regex rule because they cause an exception rather than
    // just failing to match anything
    str.matches("\\k<name>(?<name>.)");
  }

}
