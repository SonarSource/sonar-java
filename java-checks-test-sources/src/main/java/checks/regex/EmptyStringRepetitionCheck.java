package checks.regex;

class EmptyStringRepetitionCheck {

  private static final String REPLACEMENT = "empty";

  void noncompliant(String input) {
    input.replaceFirst("(?:)*", REPLACEMENT); // Noncompliant {{Remove this part of the regex.}}
    input.replaceFirst("(?:|x)*", REPLACEMENT); // Noncompliant {{Remove this part of the regex.}}
    input.replaceFirst("(?:x|)*", REPLACEMENT); // Noncompliant
    input.replaceFirst("(?:x*|y*)*", REPLACEMENT); // Noncompliant
    input.replaceFirst("(?:x?)*", REPLACEMENT); // Noncompliant
    input.replaceFirst("(?:x?)+", REPLACEMENT); // Noncompliant
  }

  void compliant(String input) {
    input.replaceFirst("x*|", REPLACEMENT);
    input.replaceFirst("x*", REPLACEMENT);
    input.replaceFirst("x?", REPLACEMENT);
    input.replaceFirst("(?:a)*", REPLACEMENT);
    input.replaceFirst("(?:x|y)*", REPLACEMENT);
  }
}
