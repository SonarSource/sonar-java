package checks.regex;

import java.util.regex.Matcher;
import java.util.regex.Pattern;
import javax.validation.constraints.Email;
import org.apache.commons.lang3.RegExUtils;

public class ReluctantQuantifierWithEmptyContinuationCheck {

  @Email(regexp = ".*?") // Noncompliant {{Remove the '?' from this unnecessarily reluctant quantifier.}}
  void noncompliant(String str) {
    Pattern.compile(".*?x?").matcher(str).find(); // Noncompliant [[sc=22;ec=25]] {{Fix this reluctant quantifier that will only ever match the empty string.}}
    Pattern.compile(".*?x?$").matcher(str).find(); // Noncompliant {{Remove the '?' from this unnecessarily reluctant quantifier.}}
    Pattern.compile(".*?x?^").matcher(str).find(); // Noncompliant {{Fix this reluctant quantifier that will only ever match the empty string.}}
    Pattern.compile(".*?x?").matcher(str).matches(); // Noncompliant {{Remove the '?' from this unnecessarily reluctant quantifier.}}
    str.matches(".*?x?"); // Noncompliant {{Remove the '?' from this unnecessarily reluctant quantifier.}}
    str.matches(".*?"); // Noncompliant
    str.matches(".*?x*"); // Noncompliant

    Matcher m = Pattern.compile(".*?").matcher(str); // Noncompliant {{Fix this reluctant quantifier that will only ever match the empty string.}}
    m.find();

    RegExUtils.removeAll(str, ".*?"); // Noncompliant {{Fix this reluctant quantifier that will only ever match the empty string.}}
    Pattern p = Pattern.compile(".*?"); // Noncompliant {{Fix this reluctant quantifier that will only ever match the empty string.}}
    RegExUtils.removeAll(str, p);
  }

  Matcher compliant(String str) {
    str.matches(".*?x");
    str.matches(".*x?");
    Matcher m = Pattern.compile(".*?").matcher(str); // Compliant because it's used both for a full match and a partial match
    m.find();
    m.matches();

    Pattern.compile(".*?"); // Compliant because unused

    Matcher m2 = Pattern.compile(".*?").matcher(str); // Compliant because the matcher is returned so we may be using it for a full match later
    m.find();
    return m2;
  }

}
