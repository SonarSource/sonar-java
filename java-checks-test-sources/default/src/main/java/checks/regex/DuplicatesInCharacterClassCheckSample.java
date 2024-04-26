package checks.regex;

public class DuplicatesInCharacterClassCheckSample {

  void nonCompliant() {
    String str = "123";
    str.matches("[0-99]"); // Noncompliant {{Remove duplicates in this character class.}}
//                ^^^
//  ^^^<
    str.matches("[90-9]"); // Noncompliant
//                ^
//  ^^^<
    str.matches("[0-73-9]"); // Noncompliant
//                ^^^
    str.matches("[0-93-57]"); // Noncompliant
//                ^^^
//  ^^^<
    str.matches("[4-92-68]"); // Noncompliant
//                ^^^
//  ^^^<
    str.matches("[0-33-9]"); // Noncompliant
//                ^^^
    str.matches("[0-70-9]"); // Noncompliant
//                ^^^
    str.matches("[3-90-7]"); // Noncompliant
//                ^^^
    str.matches("[3-50-9]"); // Noncompliant
//                ^^^
    str.matches("[xxx]"); // Noncompliant
//                ^
//  ^^^<
    str.matches("[A-z_]"); // Noncompliant
//                ^^^
    str.matches("(?i)[A-Za-z]"); // Noncompliant
//                    ^^^
    str.matches("(?i)[A-_d]"); // Noncompliant
//                    ^^^
    str.matches("(?iu)[Ä-Üä]"); // Noncompliant
//                     ^^^
    str.matches("(?iu)[a-Öö]"); // Noncompliant
//                     ^^^
    str.matches("[  ]"); // Noncompliant
//                ^
    str.matches("(?i)[  ]"); // Noncompliant
//                    ^
    str.matches("(?iu)[  ]"); // Noncompliant
//                     ^
    str.matches("(?i)[A-_D]"); // Noncompliant
//                    ^^^
    str.matches("(?iu)[A-_D]"); // Noncompliant
//                     ^^^
    str.matches("(?i)[xX]"); // Noncompliant
//                    ^
    str.matches("(?iu)[äÄ]"); // Noncompliant
//                     ^
    str.matches("(?iU)[äÄ]"); // Noncompliant
//                     ^
    str.matches("(?iu)[xX]"); // Noncompliant
//                     ^
    str.matches("[\\\"\\\".]"); // Noncompliant
//                ^^^^
    str.matches("[\\x{1F600}-\\x{1F637}\\x{1F608}]"); // Noncompliant
//                ^^^^^^^^^^^^^^^^^^^^^
    str.matches("[\\Qxx\\E]"); // Noncompliant
//                   ^
    str.matches("[[a][a]]"); // Noncompliant
//                 ^
    str.matches("[[abc][b]]"); // Noncompliant
//                 ^
    str.matches("[[^a]b]"); // Noncompliant
//                ^^^^
    str.matches("[[^a]z]"); // Noncompliant
//                ^^^^
    str.matches("[a[^z]]"); // Noncompliant
//                ^
    str.matches("[z[^a]]"); // Noncompliant
//                ^
    str.matches("[\\s\\Sx]"); // Noncompliant
//                   ^^^
    str.matches("(?U)[\\s\\Sx]"); // Noncompliant
//                       ^^^
    str.matches("[\\w\\d]"); // Noncompliant
//                ^^^
    str.matches("[\\wa]"); // Noncompliant
//                ^^^
    str.matches("[\\d1]"); // Noncompliant
//                ^^^
    str.matches("[\\d1-3]"); // Noncompliant
//                ^^^
    str.matches("(?U)[\\wa]"); // Noncompliant
//                    ^^^
    str.matches("(?U)[\\s\\u0085" + // Noncompliant
      "\\u2028" +
      "\\u2029]");
    str.matches("[0-9" + // Noncompliant
      "9]");

    str.matches("[a-b" +
      "0-9" + // Noncompliant
//     ^^^
      "d-e" +
      "9]");
//  ^^^<
    str.matches("[a-z" + // Noncompliant
//                ^^^
      "0-9" +
//  ^^^<
      "b" +
//  ^^^<
      "9]");
//  ^^^<
    str.matches("[a-z" + // Noncompliant
//                ^^^
      "0-9" +
//  ^^^<
      "b" +
//  ^^^<
      "c" +
//  ^^^<
      "9]");
//  ^^^<
    str.matches("[ba-zc]"); // Noncompliant
//                ^
//  ^^^<
    // Miss "b" in secondary locations
    str.matches("[aba-z]"); // Noncompliant
//                ^
//  ^^^<
    str.matches("[aba-zc]"); // Noncompliant
//                ^
//  ^^^<
    str.matches("[a-c" + // Noncompliant
//                ^^^
      "b" +
//  ^^^<
      "a-z" +
//  ^^^<
      "d]");
//  ^^^<
    str.matches("[0-54-6]"); // Noncompliant
//                ^^^
//  ^^^<
    str.matches("[0-352-6]"); // Noncompliant
//                ^^^
//  ^^^<
    str.matches("[0-392-43-54-65-76-87-9]"); // Noncompliant
//                ^^^
//  ^^^<
    str.matches("[0-397-96-85-72-44-63-5]"); // Noncompliant
//                   ^
//  ^^^<
    str.matches("[0-397-96-8" + // Noncompliant
//                   ^
//  ^^^<
      "a" + // not included
      "5-72-44-63-5]");
//  ^^^<
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

  @javax.validation.constraints.Email(regexp = "[0-99]") // Noncompliant {{Remove duplicates in this character class.}}
//                                               ^^^
  String email;

  void emoji(String str) {
    str.matches("[😂😊]"); // Compliant
    str.matches("[^\ud800\udc00-\udbff\udfff]"); // Compliant
  }
  
}
