package checks.regex;

public class DuplicatesInCharacterClassCheck {

  void nonCompliant() {
    String str = "123";
    str.matches("[0-99]"); // Noncompliant [[sc=19;ec=22;secondary=7]] {{Remove duplicates in this character class.}}
    str.matches("[90-9]"); // Noncompliant [[sc=19;ec=20;secondary=8]]
    str.matches("[0-73-9]"); // Noncompliant [[sc=19;ec=22]]
    str.matches("[0-93-57]"); // Noncompliant [[sc=19;ec=22;secondary=10,10]]
    str.matches("[4-92-68]"); // Noncompliant [[sc=19;ec=22;secondary=11,11]]
    str.matches("[0-33-9]"); // Noncompliant [[sc=19;ec=22]]
    str.matches("[0-70-9]"); // Noncompliant [[sc=19;ec=22]]
    str.matches("[3-90-7]"); // Noncompliant [[sc=19;ec=22]]
    str.matches("[3-50-9]"); // Noncompliant [[sc=19;ec=22]]
    str.matches("[xxx]"); // Noncompliant [[sc=19;ec=20;secondary=16,16]]
    str.matches("[A-z_]"); // Noncompliant [[sc=19;ec=22]]
    str.matches("(?i)[A-Za-z]"); // Noncompliant [[sc=23;ec=26]]
    str.matches("(?i)[A-_d]"); // Noncompliant [[sc=23;ec=26]]
    str.matches("(?iu)[Ä-Üä]"); // Noncompliant [[sc=24;ec=27]]
    str.matches("(?iu)[a-Öö]");// Noncompliant [[sc=24;ec=27]]
    str.matches("[  ]"); // Noncompliant [[sc=19;ec=20]]
    str.matches("(?i)[  ]"); // Noncompliant [[sc=23;ec=24]]
    str.matches("(?iu)[  ]"); // Noncompliant [[sc=24;ec=25]]
    str.matches("(?i)[A-_D]"); // Noncompliant [[sc=23;ec=26]]
    str.matches("(?iu)[A-_D]"); // Noncompliant [[sc=24;ec=27]]
    str.matches("(?i)[xX]"); // Noncompliant [[sc=23;ec=24]]
    str.matches("(?iu)[äÄ]"); // Noncompliant [[sc=24;ec=25]]
    str.matches("(?iU)[äÄ]"); // Noncompliant [[sc=24;ec=25]]
    str.matches("(?iu)[xX]"); // Noncompliant [[sc=24;ec=25]]
    str.matches("[\\\"\\\".]"); // Noncompliant [[sc=19;ec=23]]
    str.matches("[\\x{1F600}-\\x{1F637}\\x{1F608}]"); // Noncompliant [[sc=19;ec=40]]
    str.matches("[\\Qxx\\E]"); // Noncompliant [[sc=22;ec=23]]
    str.matches("[[a][a]]"); // Noncompliant [[sc=20;ec=21]]
    str.matches("[[abc][b]]"); // Noncompliant [[sc=20;ec=21]]
    str.matches("[[^a]b]"); // Noncompliant [[sc=19;ec=23]]
    str.matches("[[^a]z]"); // Noncompliant [[sc=19;ec=23]]
    str.matches("[a[^z]]"); // Noncompliant [[sc=19;ec=20]]
    str.matches("[z[^a]]"); // Noncompliant [[sc=19;ec=20]]
    str.matches("[\\s\\Sx]"); // Noncompliant [[sc=22;ec=25]]
    str.matches("(?U)[\\s\\Sx]"); // Noncompliant [[sc=26;ec=29]]
    str.matches("[\\w\\d]"); // Noncompliant [[sc=19;ec=22]]
    str.matches("[\\wa]"); // Noncompliant [[sc=19;ec=22]]
    str.matches("[\\d1]"); // Noncompliant [[sc=19;ec=22]]
    str.matches("[\\d1-3]"); // Noncompliant [[sc=19;ec=22]]
    str.matches("(?U)[\\wa]"); // Noncompliant [[sc=23;ec=26]]
    str.matches("(?U)[\\s\\u0085" + // Noncompliant
      "\\u2028" +
      "\\u2029]");
    str.matches("[0-9" + // Noncompliant
      "9]");

    str.matches("[a-b" +
      "0-9" + // Noncompliant [[sc=8;ec=11;secondary=56]]
      "d-e" +
      "9]");
    str.matches("[a-z" + // Noncompliant [[sc=19;ec=22;secondary=58,59,60]]
      "0-9" +
      "b" +
      "9]");
    str.matches("[a-z" +  // Noncompliant [[sc=19;ec=22;secondary=62,63,64,65]]
      "0-9" +
      "b" +
      "c" +
      "9]");
    str.matches("[ba-zc]"); // Noncompliant [[sc=19;ec=20;secondary=66,66]]
    // Miss "b" in secondary locations
    str.matches("[aba-z]"); // Noncompliant [[sc=19;ec=20;secondary=68,68]]
    str.matches("[aba-zc]"); // Noncompliant [[sc=19;ec=20;secondary=69,69,69]]
    str.matches("[a-c" + // Noncompliant [[sc=19;ec=22;secondary=71,72,73]]
      "b" +
      "a-z" +
      "d]");
    str.matches("[0-54-6]"); // Noncompliant [[sc=19;ec=22;secondary=74]]
    str.matches("[0-352-6]"); // Noncompliant [[sc=19;ec=22;secondary=75,75]]
    str.matches("[0-392-43-54-65-76-87-9]"); // Noncompliant [[sc=19;ec=22;secondary=76,76,76,76,76,76,76]]
    str.matches("[0-397-96-85-72-44-63-5]"); // Noncompliant [[sc=22;ec=23;secondary=77,77,77,77,77,77,77]]
    str.matches("[0-397-96-8" + // Noncompliant [[sc=22;ec=23;secondary=78,78,78,80,80,80,80]]
      "a" + // not included
      "5-72-44-63-5]");
  }

  void compliant() {
    String str = "123";
    str.matches("a-z\\d");
    str.matches("[0-9][0-9]?");
    str.matches("[xX]");
    str.matches("[\\s\\S]");
    str.matches("[[^\\s\\S]x]");
    str.matches("(?U)[\\s\\S]");
    str.matches("(?U)[\\S\\u0085\\u2028\\u2029]");
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
    str.matches("(?i)[A-_d-{]"); // Noncompliant
    str.matches("(?i)[A-z_]"); // Noncompliant
    str.matches("(?i)[A-z]");
    str.matches("(?i)[Z-a_]"); // Noncompliant
    str.matches("(?i)[Z-a]");
    str.matches("[\\p{IsLatin}x]"); // FN because we don't support \p at the moment
  }

  @javax.validation.constraints.Email(regexp = "[0-99]") // Noncompliant [[sc=50;ec=53]] {{Remove duplicates in this character class.}}
  String email;

  void emoji(String str) {
    str.matches("[😂😊]"); // Compliant
    str.matches("[^\ud800\udc00-\udbff\udfff]"); // Compliant
  }
  
}
