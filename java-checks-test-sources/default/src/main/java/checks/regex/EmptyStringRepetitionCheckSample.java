package checks.regex;

import org.hibernate.validator.constraints.URL;

class EmptyStringRepetitionCheckSample {

  private static final String REPLACEMENT = "empty";

  @URL(regexp = "(?:)*") // Noncompliant [[sc=18;ec=22]] {{Rework this part of the regex to not match the empty string.}}
  String url;

  void noncompliant(String input) {
    input.replaceFirst("(?:)*", REPLACEMENT); // Noncompliant [[sc=25;ec=29]] {{Rework this part of the regex to not match the empty string.}}
    input.replaceFirst("(?:)?", REPLACEMENT); // Noncompliant
    input.replaceFirst("(?:)+", REPLACEMENT); // Noncompliant
    input.replaceFirst("()*", REPLACEMENT); // Noncompliant
    input.replaceFirst("()?", REPLACEMENT); // Noncompliant
    input.replaceFirst("()+", REPLACEMENT); // Noncompliant
    input.replaceFirst("xyz|(?:)*", REPLACEMENT); // Noncompliant [[sc=29;ec=33]]
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
