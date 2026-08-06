package checks.regex;

import java.util.regex.Pattern;
import javax.validation.constraints.Email;
import javax.validation.constraints.Pattern.Flag;

public class UnicodeCaseCheckWithoutSemantic {

  @Email(
    regexp = "söme pättern"
  )
  String email1;

  @Email(
    regexp = "söme pättern",
    flags = { Flag.CASE_INSENSITIVE, Flag.UNICODE_CASE } // Compliant
  )
  String email2;

  void noncompliant(String str) {
    Pattern.compile("söme pättern", Pattern.CASE_INSENSITIVE); // Noncompliant

    Pattern.compile("s\u00F6me", Pattern.CASE_INSENSITIVE); // Noncompliant
    Pattern.compile("s\\u00F6me", Pattern.CASE_INSENSITIVE); // Noncompliant
    Pattern.compile("s\\xF6me", Pattern.CASE_INSENSITIVE); // Noncompliant

    Pattern.compile("söme pättern", Pattern.CASE_INSENSITIVE | Pattern.MULTILINE); // Noncompliant

    str.matches("(?i)söme pättern"); // Noncompliant

    str.matches("(?i:söme) pättern"); // Noncompliant

    String regexPart1 = "(?i:söme)"; // Noncompliant

    String regexPart2 = "(?i:pättern)"; // Noncompliant

    str.matches(regexPart1 + regexPart2);

    // In these cases the location of the issue is a bit confusing, but code like this will probably not occur in the wild
    str.matches("(?iu)söme (?-u)pättern"); // Noncompliant

    str.matches("(?iu)söme (?-U)pättern"); // Noncompliant

  }

  void compliant(String str) {
    Pattern.compile("söme pättern", Pattern.CASE_INSENSITIVE | Pattern.UNICODE_CASE);
    str.matches("(?iu)söme pättern");
    str.matches("(?iu:söme) pättern");
    str.matches("(?i)\uD83D\uDCA9"); // compliant because the string doesn't contain any letters
    str.matches("(?i:öäü"); // rule is not applied to syntactically invalid regexen

    // UNICODE_CHARACTER_CLASS implies UNICODE_CASE
    Pattern.compile("söme pättern", Pattern.CASE_INSENSITIVE | Pattern.UNICODE_CHARACTER_CLASS);
    str.matches("(?iU)söme pättern");
    str.matches("(?iU:söme) pättern");
  }

  @jakarta.validation.constraints.Email(
    regexp = "söme pättern"
  )
  String jakartaEmail1;

  @jakarta.validation.constraints.Email(
    regexp = "söme pättern",
    flags = { jakarta.validation.constraints.Pattern.Flag.CASE_INSENSITIVE, jakarta.validation.constraints.Pattern.Flag.UNICODE_CASE } // Compliant
  )
  String jakartaEmail2;
}
