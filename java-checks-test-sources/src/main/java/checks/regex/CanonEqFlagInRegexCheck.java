package checks.regex;

import java.util.regex.Pattern;
import javax.validation.constraints.Email;
import javax.validation.constraints.Pattern.Flag;

public class CanonEqFlagInRegexCheck {

  @Email(regexp = "éeéc") // Noncompliant [[sc=20;ec=24;secondary=9,9]] {{Use the CANON_EQ flag with this pattern.}}
  String email1;

  @Email(regexp = "éeéc", flags = Flag.CANON_EQ) // Compliant
  String email2;

  @jakarta.validation.constraints.Email(regexp = "éeéc") // Noncompliant [[sc=51;ec=55;secondary=15,15]] {{Use the CANON_EQ flag with this pattern.}}
  String email3;

  @jakarta.validation.constraints.Email(regexp = "éeéc", flags = jakarta.validation.constraints.Pattern.Flag.CANON_EQ) // Compliant
  String email4;

  void noncompliant(String str) {
    Pattern.compile("éeéc"); // Noncompliant [[sc=22;ec=26;secondary=22,22]] {{Use the CANON_EQ flag with this pattern.}}
    Pattern.compile("é"); // Noncompliant
    Pattern.compile("é|è"); // Noncompliant
    Pattern.compile("à"); // Noncompliant
    Pattern.compile("à"); // Noncompliant
    Pattern.compile("À"); // Noncompliant

    // Letter followed by a "̀ "
    Pattern.compile("è"); // Noncompliant
    Pattern.compile("é̀"); // Noncompliant
    Pattern.compile("aèa"); // Noncompliant
    Pattern.compile("aè"); // Noncompliant
    Pattern.compile("èa"); // Noncompliant
    Pattern.compile("e⃝"); // Noncompliant
    Pattern.compile("a|è|a"); // Noncompliant
    Pattern.compile("\\dè"); // Noncompliant
    // Letter "e" followed by 4 marks
    Pattern.compile("è̀̀̀"); // Noncompliant
  }

  void can_not_set_flag_directly() {
    "éeé".replaceAll("é" , "e"); // Noncompliant {{Use the CANON_EQ flag with "Pattern.compile(pattern, CANON_EQ).matcher(input).replaceAll(replacement)".}}
    "éeé".replaceFirst("é" , "e"); // Noncompliant {{Use the CANON_EQ flag with "Pattern.compile(pattern, CANON_EQ).matcher(input).replaceFirst(replacement)".}}
    "éeé".matches("é"); // Noncompliant {{Use the CANON_EQ flag with "Pattern.compile(regex, CANON_EQ).matcher(input).matches()".}}
    Pattern.matches("é", "input"); // Noncompliant {{Use the CANON_EQ flag with "Pattern.compile(regex, CANON_EQ).matcher(input).matches()".}}
  }

  void compliant(String str) {
    Pattern.compile("e"); // Compliant, does not contains anything subject to normalization
    Pattern.compile("e|a"); // Compliant, does not contains anything subject to normalization
    Pattern.compile("̀"); // Compliant, target the mark alone
    // With escaped unicode, one know exactly what to match
    Pattern.compile("c\\u0308"); // Compliant
    Pattern.compile("e\u0300"); // Compliant
    Pattern.compile("c\\x{0308}"); // Compliant
    Pattern.compile("\\u0308"); // Compliant
    Pattern.compile("c\u0308̀"); // Compliant (letter, escaped mark, non-escaped mark)

    Pattern.compile("a|e|̀|a"); // Compliant, mark alone (look closely...)
    Pattern.compile("\\d̀"); // Compliant

    Pattern.compile("e\u20DD̀"); // Compliant


    Pattern.compile("[ée]"); // Compliant, CANON_EQ will not solve the problem, S5868 will target it.
    Pattern.compile("é", Pattern.CANON_EQ); // Compliant, flag is set
    Pattern.compile("é", Pattern.CANON_EQ | Pattern.MULTILINE); // Compliant
  }

}
