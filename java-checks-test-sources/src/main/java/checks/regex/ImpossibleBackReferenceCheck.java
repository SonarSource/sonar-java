package checks.regex;

public class ImpossibleBackReferenceCheck {

  void noncompliant(String str) {
    str.matches("\\1" + // Noncompliant [[sc=18;ec=21;secondary=7]] {{Fix this backreference, so that it refers to a group that can be matched before it.}}
      "(.)");
    str.matches("\\k<name>" + // Noncompliant [[sc=18;ec=27;secondary=9]] {{Fix this backreference, so that it refers to a group that can be matched before it.}}
      "(?<name>.)");
    str.matches("(.)|" +
      "\\1"); // Noncompliant [[sc=8;ec=11;secondary=10]]
    str.matches("\\1"); // Noncompliant [[sc=18;ec=21]] {{Fix this backreference - it refers to a capturing group that doesn't exist.}}
    str.matches("\\2(.)"); // Noncompliant [[sc=18;ec=21]] {{Fix this backreference - it refers to a capturing group that doesn't exist.}}
    str.matches("(.)\\2(.)"); // Noncompliant {{Fix this backreference, so that it refers to a group that can be matched before it.}}
    str.matches("(?<x>.)\\k<y>(?<y>.)"); // Noncompliant {{Fix this backreference, so that it refers to a group that can be matched before it.}}
    str.matches("(?<nothername>)\\k<name>"); // Noncompliant [[sc=33;ec=42]] {{Fix this backreference - it refers to a capturing group that doesn't exist.}}
    str.matches("\\k<name>(?<name>.)"); // Noncompliant
    str.matches("(?<name>.)|\\k<name>"); // Noncompliant
    str.matches("(?:\\1(.))*"); // Noncompliant
    str.matches("\\1|(.)"); // Noncompliant
  }

  void compliant(String str) {
    str.matches("(.)\\1");
    str.matches("(?:x(.)|\\1)*");
    str.matches("(?<name>)\\k<name>");
    str.matches("(?<name>)\\1");
    // This produces an FP in IntelliJ
    str.matches("(?:\\1|x(.))*");
  }

}
