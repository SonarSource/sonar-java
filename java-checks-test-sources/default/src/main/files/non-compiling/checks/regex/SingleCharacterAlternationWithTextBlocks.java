package checks.regex;

public class SingleCharacterAlternationWithTextBlocks {

  void nonCompliant(String str) {
    str.matches("""
      a|b|c"""); // Noncompliant {{Replace this alternation with a character class.}}
//    ^^^^^
    // Matches [ab\n]
    str.matches("""
      a|b|
      """); // Noncompliant@-1
    // Matches [ab\nc]
    str.matches("""
      a|b| // Noncompliant@-1
//    ^^^^
      |c""");
//  ^^^<
    str.matches("""
      (?x)
      (a|b) // Noncompliant@-1
//     ^^^
      """);
    str.matches("""
      (?x)(
      a|b // Noncompliant@-1
//    ^^^
      )""");
  }

  void compliant(String str) {
    str.matches("""
      ab|cd""");
    // Equivalent to "a|b\n"
    str.matches("""
      a|b
      """);
    // Equivalent to "a|b|\n " - note the space
    str.matches("""
      a|b|
       """);
  }

}
