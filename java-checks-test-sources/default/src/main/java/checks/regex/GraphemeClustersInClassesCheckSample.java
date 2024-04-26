package checks.regex;

import java.util.regex.Pattern;

public class GraphemeClustersInClassesCheckSample {

  void noncompliant(String str) {
    Pattern.compile("[aaaèaaa]"); // Noncompliant {{Extract 1 Grapheme Cluster(s) from this character class.}}
//                   ^^^^^^^^^^
//  ^^^<
    Pattern.compile("[0Ṩ0]"); // Noncompliant {{Extract 1 Grapheme Cluster(s) from this character class.}}
//                   ^^^^^^^
//  ^^^<
    Pattern.compile("aaa[è]aaa"); // Noncompliant
    // two secondary per line: one for the regex location, and one for the cluster location
    Pattern.compile("[èaèaè]"); // Noncompliant {{Extract 3 Grapheme Cluster(s) from this character class.}}
//                   ^^^^^^^^^^
//  ^^^<
    Pattern.compile("[èa-dä]"); // Noncompliant
    Pattern.compile("[èa]" + // Noncompliant
      "aaa" +
      "[dè]"); // Noncompliant

    "abc".replaceFirst("[ä]", "A"); // Noncompliant
    Pattern.compile("[c̈]"); // Noncompliant
    Pattern.compile("[e⃝]"); // Noncompliant
  }

  void compliant(String str) {
    Pattern.compile("[é]"); // Compliant, a single char
    Pattern.compile("[e\u0300]"); // Compliant, escaped unicode
    Pattern.compile("[e\\u0300]"); // Compliant, escaped unicode
    Pattern.compile("[e\\x{0300}]"); // Compliant, escaped unicode
    Pattern.compile("[e\u20DD̀]"); // Compliant, (letter, escaped unicode, mark) can not be combined
    Pattern.compile("[\u0300e]"); // Compliant, escaped unicode, letter
    Pattern.compile("[̀̀]"); // Compliant, two marks
    Pattern.compile("[̀̀]"); // Compliant, one mark

    Pattern.compile("ä"); // Compliant, not in a class
  }

  @org.hibernate.validator.constraints.URL(regexp = "[èaèaè]") // Noncompliant {{Extract 3 Grapheme Cluster(s) from this character class.}}
//                                                   ^^^^^^^^^^
  String url;


}
