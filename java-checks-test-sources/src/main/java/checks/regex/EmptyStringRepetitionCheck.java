package checks.regex;

class EmptyStringRepetitionCheck {

  private static final String REPLACEMENT = "empty";

  void noncompliant(String input) {
    input.replaceFirst("(?:)*", REPLACEMENT); // Noncompliant [[sc=25;ec=30]] {{Rework this part of the regex to not match the empty string.}}
    input.replaceFirst("(?:)?", REPLACEMENT); // Noncompliant
    input.replaceFirst("(?:)+", REPLACEMENT); // Noncompliant
    input.replaceFirst("()*", REPLACEMENT); // Noncompliant
    input.replaceFirst("()?", REPLACEMENT); // Noncompliant
    input.replaceFirst("()+", REPLACEMENT); // Noncompliant
    input.replaceFirst("xyz|(?:)*", REPLACEMENT); // Noncompliant [[sc=29;ec=34]]
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
}
