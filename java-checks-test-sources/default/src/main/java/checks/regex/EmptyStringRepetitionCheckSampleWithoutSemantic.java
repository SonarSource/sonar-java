package checks.regex;

class EmptyStringRepetitionCheckSampleWithoutSemantic {

  private static final String REPLACEMENT = "empty";

  String url;

  void noncompliant(String input) {
    input.replaceFirst("(?:)*", REPLACEMENT); // Noncompliant

    input.replaceFirst("(?:)?", REPLACEMENT); // Noncompliant
    input.replaceFirst("(?:)+", REPLACEMENT); // Noncompliant
    input.replaceFirst("()*", REPLACEMENT); // Noncompliant
    input.replaceFirst("()?", REPLACEMENT); // Noncompliant
    input.replaceFirst("()+", REPLACEMENT); // Noncompliant
    input.replaceFirst("xyz|(?:)*", REPLACEMENT); // Noncompliant

    input.replaceFirst("(?:|x)*", REPLACEMENT); // Noncompliant
    input.replaceFirst("(?:x|)*", REPLACEMENT); // Noncompliant
    input.replaceFirst("(?:x|y*)*", REPLACEMENT); // Noncompliant
    input.replaceFirst("(?:x*|y*)*", REPLACEMENT); // Noncompliant
    input.replaceFirst("(?:x?|y*)*", REPLACEMENT); // Noncompliant
    input.replaceFirst("(?:x*)*", REPLACEMENT); // Noncompliant
    input.replaceFirst("(?:x?)*", REPLACEMENT); // Noncompliant
    input.replaceFirst("(?:x*)?", REPLACEMENT); // Noncompliant
    input.replaceFirst("(?:x?)?", REPLACEMENT); // Noncompliant
    input.replaceFirst("(?:x*)+", REPLACEMENT); // Noncompliant
    input.replaceFirst("(?:x?)+", REPLACEMENT); // Noncompliant
    input.replaceFirst("(x*)*", REPLACEMENT); // Noncompliant
    input.replaceFirst("((x*))*", REPLACEMENT); // Noncompliant
    input.replaceFirst("(?:x*y*)*", REPLACEMENT); // Noncompliant
    input.replaceFirst("(?:())*", REPLACEMENT); // Noncompliant
    input.replaceFirst("(?:(?:))*", REPLACEMENT); // Noncompliant
    input.replaceFirst("((?i))*", REPLACEMENT); // Noncompliant
    input.replaceFirst("(())*", REPLACEMENT); // Noncompliant
    input.replaceFirst("(()x*)*", REPLACEMENT); // Noncompliant
    input.replaceFirst("(()|x)*", REPLACEMENT); // Noncompliant
    input.replaceFirst("($)*", REPLACEMENT); // Noncompliant
    input.replaceFirst("(\\b)*", REPLACEMENT); // Noncompliant
    input.replaceFirst("((?!x))*", REPLACEMENT); // Noncompliant
  }

  void compliant(String input) {
    input.replaceFirst("x*|", REPLACEMENT);
    input.replaceFirst("x*|", REPLACEMENT);
    input.replaceFirst("x*", REPLACEMENT);
    input.replaceFirst("x?", REPLACEMENT);
    input.replaceFirst("(?:x|y)*", REPLACEMENT);
    input.replaceFirst("(?:x+)+", REPLACEMENT);
    input.replaceFirst("(?:x+)*", REPLACEMENT);
    input.replaceFirst("(?:x+)?", REPLACEMENT);
    input.replaceFirst("((x+))*", REPLACEMENT);
  }

  void no_duplications(String input) {
    String regex = "(?:)*"; // Noncompliant

    input.replaceFirst(regex, REPLACEMENT);
    input.replaceFirst(regex, REPLACEMENT);

    String regex2_1 = "(?:"; // Noncompliant
    String regex2_2 = ")*";

    input.replaceFirst(regex2_1 + regex2_2, REPLACEMENT);
    input.replaceFirst(regex2_1 + regex2_2, REPLACEMENT);

    String regex3_1 = "(?:"; // Compliant
    String regex3_2 = ")*";

    input.replaceFirst(regex3_1 + "x|y" +  regex3_2, REPLACEMENT);
  }
}
