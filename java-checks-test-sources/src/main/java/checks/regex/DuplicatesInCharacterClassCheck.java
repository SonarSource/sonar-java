package checks.regex;

public class DuplicatesInCharacterClassCheck {

  void nonCompliant() {
    String str = "123";
    str.matches("[0-99]"); // Noncompliant [[sc=22;ec=23]] {{Remove duplicates in this character class.}}
    str.matches("[0-73-9]"); // Noncompliant [[sc=22;ec=25]]
    str.matches("[0-33-9]"); // Noncompliant [[sc=22;ec=25]]
    str.matches("[0-70-9]"); // Noncompliant [[sc=22;ec=25]]
    str.matches("[3-90-7]"); // Noncompliant [[sc=22;ec=25]]
    str.matches("[3-50-9]"); // Noncompliant [[sc=22;ec=25]]
    str.matches("[xxx]"); // Noncompliant [[sc=20;ec=21;secondary=13]]
    str.matches("[A-z_]"); // Noncompliant [[sc=22;ec=23]]
    str.matches("(?i)[A-Za-z]"); // Noncompliant [[sc=26;ec=29]]
    str.matches("(?i)[A-_d]"); // Noncompliant [[sc=26;ec=27]]
    str.matches("(?iu)[Ä-Üä]"); // Noncompliant [[sc=27;ec=28]]
    str.matches("(?iu)[a-Öö]");// Noncompliant [[sc=27;ec=28]]
    str.matches("[  ]"); // Noncompliant [[sc=20;ec=21]]
    str.matches("(?i)[  ]"); // Noncompliant [[sc=24;ec=25]]
    str.matches("(?iu)[  ]"); // Noncompliant [[sc=25;ec=26]]
    str.matches("(?i)[A-_D]"); // Noncompliant [[sc=26;ec=27]]
    str.matches("(?iu)[A-_D]"); // Noncompliant [[sc=27;ec=28]]
    str.matches("(?i)[xX]"); // Noncompliant [[sc=24;ec=25]]
    str.matches("(?iu)[äÄ]"); // Noncompliant [[sc=25;ec=26]]
    str.matches("(?iU)[äÄ]"); // Noncompliant [[sc=25;ec=26]]
    str.matches("(?iu)[xX]"); // Noncompliant [[sc=25;ec=26]]
    str.matches("[\\\"\\\".]"); // Noncompliant [[sc=23;ec=27]]
    str.matches("[\\x{1F600}-\\x{1F637}\\x{1F608}]"); // Noncompliant [[sc=40;ec=50]]
    str.matches("[\\Qxx\\E]"); // Noncompliant [[sc=23;ec=24]]
    str.matches("[[a][a]]"); // Noncompliant [[sc=22;ec=25]]
    str.matches("[[abc][b]]"); // Noncompliant [[sc=24;ec=27]]
    str.matches("[[^a]b]"); // Noncompliant [[sc=23;ec=24]]
    str.matches("[[^a]z]"); // Noncompliant [[sc=23;ec=24]]
    str.matches("[\\s\\Sx]"); // Noncompliant [[sc=25;ec=26]]
    str.matches("(?U)[\\s\\Sx]"); // Noncompliant [[sc=29;ec=30]]
    str.matches("[\\w\\d]"); // Noncompliant [[sc=22;ec=25]]
    str.matches("[\\wa]"); // Noncompliant [[sc=22;ec=23]]
    str.matches("[\\d1]"); // Noncompliant [[sc=22;ec=23]]
    str.matches("[\\d1-3]"); // Noncompliant [[sc=22;ec=25]]
    str.matches("(?U)[\\wa]"); // Noncompliant [[sc=26;ec=27]]
  }

  void compliant() {
    String str = "123";
    str.matches("a-z\\d");
    str.matches("[0-9][0-9]?");
    str.matches("[xX]");
    str.matches("[\\s\\S]");
    str.matches("[[^\\s\\S]x]");
    str.matches("(?U)[\\s\\S]");
    str.matches("[\\d\\D]");
    str.matches("(?U)[\\d\\D]");
    str.matches("[\\w\\W]");
    str.matches("(?U)[\\w\\W]");
    str.matches("[\\wä]");
    str.matches("(?i)[äÄ]");
    str.matches("(?i)[Ä-Üä]");
    str.matches("(?u)[äÄ]");
    str.matches("(?u)[xX]");
    str.matches("[ab-z]");
    str.matches("[[a][b]]");
    str.matches("[[^a]a]");
    str.matches("(?i)[a-Öö]");
    str.matches("[0-9\\Q.-_\\E]"); // This used to falsely interpret .-_ as a range and complain that it overlaps with 0-9
    str.matches("[A-Z\\Q-_.\\E]");
    str.matches("[\\x00\\x01]]"); // This used to falsely complain about x and 0 being duplicates
    str.matches("[\\x00-\\x01\\x02-\\x03]]");
    str.matches("[z-a9-0]"); // Illegal character class should not make the check explode
    str.matches("[aa"); // Check should not run on syntactically invalid regexen
    str.matches("(?U)[\\wä]"); // False negative because we don't support Unicode characters in \\w and \\W
    str.matches("(?U)[[^\\W]a]"); // False negative because once we negate a character class whose contents we don't
                                  // fully understand, we ignore it to avoid false positives
    str.matches("[\\N{slightly smiling face}\\N{slightly smiling face}]"); // FN because we don't support \\N
    str.matches("[[a-z&&b-e]c]"); // FN because we don't support intersections
    str.matches("(?i)[A-_d-{]"); // FN because we ignore case insensitivity unless both ends of the ranges are letters
    str.matches("(?i)[A-z_]"); // FN because A-z gets misinterpreted as A-Za-z due to the way we handle case insensitivity
    str.matches("[\\p{IsLatin}x]"); // FN because we don't support \p at the moment
  }
}
