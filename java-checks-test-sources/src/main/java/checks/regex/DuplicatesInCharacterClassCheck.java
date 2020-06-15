package checks.regex;

public class DuplicatesInCharacterClassCheck {

  void nonCompliant() {
    String str = "123";
    str.matches("[0-99]"); // Noncompliant [[sc=22;ec=23]] {{Remove duplicates in this character class.}}
    str.matches("[0-73-9]"); // Noncompliant [[sc=22;ec=25]]
    str.matches("[0-70-9]"); // Noncompliant [[sc=22;ec=25]]
    str.matches("[3-90-7]"); // Noncompliant [[sc=22;ec=25]]
    str.matches("[3-50-9]"); // Noncompliant [[sc=22;ec=25]]
    str.matches("[xxx]"); // Noncompliant [[sc=20;ec=21;secondary=12]]
    str.matches("[  ]"); // Noncompliant [[sc=20;ec=21]]
    str.matches("(?i)[  ]"); // Noncompliant [[sc=24;ec=25]]
    str.matches("(?i)[xX]"); // Noncompliant [[sc=24;ec=25]]
    str.matches("(?iu)[äÄ]"); // Noncompliant [[sc=25;ec=26]]
    str.matches("(?iU)[äÄ]"); // Noncompliant [[sc=25;ec=26]]
    str.matches("(?iu)[xX]"); // Noncompliant [[sc=25;ec=26]]
  }

  void compliant() {
    String str = "123";
    str.matches("[0-9][0-9]?");
    str.matches("[xX]");
    str.matches("(?i)[äÄ]");
    str.matches("(?u)[äÄ]");
    str.matches("(?u)[xX]");
    str.matches("[ab-z]");
    str.matches("[z-a9-0]"); // Illegal character class should not make the check explode
    str.matches("[aa"); // Check should not run on syntactically invalid regexen
    str.matches("[[a][a]]"); // Rule doesn't take into account nested character classes
  }
}
