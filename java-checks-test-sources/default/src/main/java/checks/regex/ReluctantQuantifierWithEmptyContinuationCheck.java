package checks.regex;

import java.util.regex.Matcher;
import java.util.regex.Pattern;
import javax.validation.constraints.Email;
import org.apache.commons.lang3.RegExUtils;

public class ReluctantQuantifierWithEmptyContinuationCheck {

  void noncompliant(String str) {
    // ====== Reluctant quantifier that will only ever match the empty string ======
    // Partial match
    Pattern.compile(".*?").matcher(str).find(); // Noncompliant {{Fix this reluctant quantifier that will only ever match 0 repetitions.}}
    Pattern.compile(".+?").matcher(str).find(); // Noncompliant {{Fix this reluctant quantifier that will only ever match 1 repetition.}}
    Pattern.compile(".{4}?").matcher(str).find(); // Noncompliant {{Fix this reluctant quantifier that will only ever match 4 repetitions.}}
    Pattern.compile(".{2,4}?").matcher(str).find(); // Noncompliant {{Fix this reluctant quantifier that will only ever match 2 repetitions.}}
    Pattern.compile(".*?x?").matcher(str).find(); // Noncompliant [[sc=22;ec=25]] {{Fix this reluctant quantifier that will only ever match 0 repetitions.}}
    Pattern.compile(".*?()").matcher(str).find(); // Noncompliant {{Fix this reluctant quantifier that will only ever match 0 repetitions.}}
    Pattern.compile(".*?x?^").matcher(str).find(); // Noncompliant {{Fix this reluctant quantifier that will only ever match 0 repetitions.}}
    str.split(".*?x?^"); // Noncompliant {{Fix this reluctant quantifier that will only ever match 0 repetitions.}}
    Matcher mPartial = Pattern.compile(".*?").matcher(str); // Noncompliant {{Fix this reluctant quantifier that will only ever match 0 repetitions.}}
    mPartial.find();
    RegExUtils.removeAll(str, ".*?"); // Noncompliant {{Fix this reluctant quantifier that will only ever match 0 repetitions.}}
    Pattern p = Pattern.compile(".*?"); // Noncompliant {{Fix this reluctant quantifier that will only ever match 0 repetitions.}}
    RegExUtils.removeAll(str, p);

    // ====== Unnecessarily reluctant quantifier ======
    // Full match (implicitly end with end anchor "$")
    str.matches(".*?"); // Noncompliant {{Remove the '?' from this unnecessarily reluctant quantifier.}}
    Matcher mFull = Pattern.compile(".*?").matcher(str); // Noncompliant {{Remove the '?' from this unnecessarily reluctant quantifier.}}
    mFull.matches();
    str.matches(".*?()"); // Noncompliant {{Remove the '?' from this unnecessarily reluctant quantifier.}}
    str.matches(".*?()*"); // Noncompliant {{Remove the '?' from this unnecessarily reluctant quantifier.}}
    str.matches(".*?((?=))*"); // Noncompliant {{Remove the '?' from this unnecessarily reluctant quantifier.}}
    str.matches(".*?(?!x)"); // Noncompliant {{Remove the '?' from this unnecessarily reluctant quantifier.}}
    // Followed explicitly by end anchor ("$")
    Pattern.compile(".*?$").matcher(str).matches(); // Noncompliant {{Remove the '?' from this unnecessarily reluctant quantifier.}}
    Pattern.compile(".*?()$").matcher(str).matches(); // Noncompliant {{Remove the '?' from this unnecessarily reluctant quantifier.}}
    // The match type does not change anything
    Pattern.compile(".*?$").matcher(str).find(); // Noncompliant {{Remove the '?' from this unnecessarily reluctant quantifier.}}
    Pattern.compile(".*?()$").matcher(str).find(); // Noncompliant {{Remove the '?' from this unnecessarily reluctant quantifier.}}
    // Even when the match type is both or unknown, the reluctant quantifier is still useless if we have an explicit "$"
    Matcher mBoth = Pattern.compile(".*?$").matcher(str); // Noncompliant {{Remove the '?' from this unnecessarily reluctant quantifier.}}
    mBoth.find();
    mBoth.matches();
    Pattern.compile(".*?$").matcher(str); // Noncompliant {{Remove the '?' from this unnecessarily reluctant quantifier.}}
    Pattern.compile(".*?()$").matcher(str); // Noncompliant {{Remove the '?' from this unnecessarily reluctant quantifier.}}
  }
  // Full match
  @Email(regexp = ".*?") // Noncompliant {{Remove the '?' from this unnecessarily reluctant quantifier.}}
  void fullMatch() { }

  Matcher compliant(String str) {
    str.matches(".*?x");
    str.matches(".*?x?");
    str.split(".*?x");
    str.matches("(.*?)x?"); // Compliant because the last x won't be included in the `.*?` if the string ends with x
    str.matches(".*?x*");
    Pattern.compile("(.*?)x?").matcher(str).matches();
    Matcher m = Pattern.compile(".*?").matcher(str); // Compliant because it's used both for a full match and a partial match
    m.find();
    m.matches();

    Pattern.compile(".*?"); // Compliant because unused

    Matcher m2 = Pattern.compile(".*?").matcher(str); // Compliant because the matcher is returned so we may be using it for a full match later
    return m2;
  }

}
