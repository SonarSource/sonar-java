package checks.regex;

import java.util.regex.Matcher;
import java.util.regex.Pattern;
import javax.validation.constraints.Email;
import org.apache.commons.lang3.RegExUtils;

public class ReluctantQuantifierWithEmptyContinuationCheckSampleWithoutSemantic {

  void noncompliant(String str) {
    // ====== Reluctant quantifier that will only ever match the empty string ======
    // Partial match
    Pattern.compile(".*?").matcher(str).find(); // Noncompliant
    Pattern.compile(".+?").matcher(str).find(); // Noncompliant
    Pattern.compile(".{4}?").matcher(str).find(); // Noncompliant
    Pattern.compile(".{2,4}?").matcher(str).find(); // Noncompliant
    Pattern.compile(".*?x?").matcher(str).find(); // Noncompliant

    Pattern.compile(".*?()").matcher(str).find(); // Noncompliant
    Pattern.compile(".*?x?^").matcher(str).find(); // Noncompliant
    str.split(".*?x?^"); // Noncompliant
    Matcher mPartial = Pattern.compile(".*?").matcher(str); // Noncompliant
    mPartial.find();
    RegExUtils.removeAll(str, ".*?"); // FN
    Pattern p = Pattern.compile(".*?"); // FN
    RegExUtils.removeAll(str, p);

    // ====== Unnecessarily reluctant quantifier ======
    // Full match (implicitly end with end anchor "$")
    str.matches(".*?"); // Noncompliant
    Matcher mFull = Pattern.compile(".*?").matcher(str); // Noncompliant
    mFull.matches();
    str.matches(".*?()"); // Noncompliant
    str.matches(".*?()*"); // Noncompliant
    str.matches(".*?((?=))*"); // Noncompliant
    str.matches(".*?(?!x)"); // Noncompliant
    // Followed explicitly by end anchor ("$")
    Pattern.compile(".*?$").matcher(str).matches(); // Noncompliant
    Pattern.compile(".*?()$").matcher(str).matches(); // Noncompliant
    // The match type does not change anything
    Pattern.compile(".*?$").matcher(str).find(); // Noncompliant
    Pattern.compile(".*?()$").matcher(str).find(); // Noncompliant
    // Even when the match type is both or unknown, the reluctant quantifier is still useless if we have an explicit "$"
    Matcher mBoth = Pattern.compile(".*?$").matcher(str); // Noncompliant
    mBoth.find();
    mBoth.matches();
    Pattern.compile(".*?$").matcher(str); // Noncompliant
    Pattern.compile(".*?()$").matcher(str); // Noncompliant
  }
  // Full match
  @Email(regexp = ".*?") // FN
  void fullMatch() { }

  @jakarta.validation.constraints.Email(regexp = ".*?") // Noncompliant
  void fullMatchJakarta() { }

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
