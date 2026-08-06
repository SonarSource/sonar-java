package checks.regex;

import java.util.regex.Pattern;
import javax.validation.constraints.Email;

public class SuperLinearRegexCheckSampleWithoutSemantic {

  String email;

  @jakarta.validation.constraints.Email(regexp = "(.*-)*@.*") // Noncompliant

  String email2;

  void realWorldExamples(String str) {
    String cloudflareAttack = "(?:(?:\"|'|\\]|\\}|\\\\|\\d|(?:nan|infinity|true|false|null|undefined|symbol|math)|\\`|\\-|\\+)+[)]*;?((?:\\s|-|~|!|\\{\\}|\\|\\||\\+)*.*(?:.*=.*)))";
    String stackOverflowAttack = "^[\\s\\u200c]+|[\\s\\u200c]+$";
    str.matches(cloudflareAttack); // Noncompliant
    str.replaceAll(stackOverflowAttack, ""); // Noncompliant
  }

  void fullAndPartialMatches(String str) {
    Pattern p1 = Pattern.compile("(.*,)*"); // Compliant because it's never used for a full match
    Pattern p2 = Pattern.compile("(.*,)*"); // Noncompliant
    p1.matcher(str).find();
    p2.matcher(str).find();
    p2.matcher(str).matches();
  }

  void alwaysExponential(String str) {
    str.matches("(.*,)*?"); // Compliant - always exponential, reported by S5852
    str.matches("(.?,)*?"); // Compliant - always exponential, reported by S5852
    str.matches("(a|.a)*?"); // Compliant - always exponential, reported by S5852
    str.matches("(?:.*,)*(X)\\1"); // Compliant - QUADRATIC_WHEN_OPTIMIZED + backref on Java 9+, reported by S5852
    str.matches("(.*,)*\\1"); // Compliant - QUADRATIC_WHEN_OPTIMIZED + backref on Java 9+, reported by S5852
  }

  void polynomialInJava9(String str) {
    str.matches("(.*,)*"); // Noncompliant
    str.matches("(.*,)*.*"); // Noncompliant
    str.split("(.*,)*X"); // Noncompliant
    str.matches("(.*,)*X"); // Noncompliant
    str.matches("(.*?,)+"); // Noncompliant
    str.matches("(.*?,){5,}"); // Noncompliant
    str.matches("((.*,)*)*+"); // Noncompliant
    str.matches("((.*,)*)?"); // Noncompliant
    str.matches("(?>(.*,)*)"); // Noncompliant
    str.matches("((?>.*,)*)*"); // Noncompliant
    str.matches("(.*,)* (.*,)*"); // Noncompliant
    str.split("(.*,)*$"); // Noncompliant
    str.matches("(.*,)*$"); // Noncompliant
    str.matches("(.*,)*(..)*"); // Noncompliant
    str.matches("(.*,)*(.{2})*"); // Noncompliant
  }

  void alwaysQuadratic(String str) {
    // Always polynomial when two non-possessive quantifiers overlap in a sequence
    str.matches("x*\\w*"); // Noncompliant
    str.matches(".*.*X"); // Noncompliant
    str.matches("x*a*x*"); // Noncompliant
    str.matches("x*,a*x*"); // Compliant, can fail between the two quantifiers
    str.matches("x*(xy?)*"); // Noncompliant
    str.matches("(ab)*a(ba)*"); // False Negative :-(
    str.matches("x*xx*"); // Noncompliant
    str.matches("x*yx*"); // Compliant
    str.matches("x*a*b*c*d*e*f*g*h*i*x*"); // Noncompliant
    str.matches("x*a*b*c*d*e*f*g*h*i*j*x*x*"); // Noncompliant
    // Non-possessive followed by possessive quantifier is actually polynomial
    str.matches(".*\\s*"); // Noncompliant
    str.matches(".*\\s*+"); // Noncompliant
    str.matches(".*+\\s*"); // Compliant, other way (possessive then non-possessive) is fine
    str.matches(".*+\\s*+"); // Compliant, two possessives is fine
    str.matches(".*,\\s*+,"); // Compliant, can fail between the two quantifiers
    str.matches("\\s*\\s*+,"); // Noncompliant
    str.matches("a*\\s*+,"); // Compliant, no overlap
    str.matches("[a\\s]*\\s*+,"); // Noncompliant
    str.matches("[a\\s]*b*\\s*+,"); // Noncompliant
    str.matches("\\s*+[a\\s]*b*,"); // Compliant, possessive then non-possessive
    str.matches("\\s*+b*[a\\s]*,"); // Compliant, possessive then non-possessive
    // Implicit reluctant quantifier in partial match also leads to polynomial runtime
    str.split("\\s*,"); // Noncompliant
    str.split("\\s*+,"); // Noncompliant
    str.matches("(?s:.*)\\s*,(?s:.*)"); // Noncompliant
    str.matches("(?s:.*)\\s*+,(?s:.*)"); // Noncompliant
    str.split(",\\s*+"); // Compliant
    str.split(",\\s*+,"); // Compliant
    str.split("\\s*+"); // Compliant
  }

  void differentPolynomials(String str) {
    // quadratic (O(n^2))
    str.matches("x*x*"); // Noncompliant
    // cubic (O(n^3))
    str.matches("x*x*x*"); // Noncompliant
    // O(n^4)
    str.matches("x*x*x*x*"); // Noncompliant
    // O(n^5)
    str.matches("x*x*x*x*x*"); // Noncompliant

    str.matches("[^=]*.*.*=.*"); // Noncompliant
  }

  void fixedInJava9(String str) {
    str.matches("(.?,)*X"); // Compliant - linear on Java 9+
  }

  void notFixedInJava9(String str) {
    // The back reference prevents the Java 9+ optimization from being applied
    str.matches("(.?,)*\\1"); // Compliant - LINEAR_WHEN_OPTIMIZED + backref on Java 9+, reported by S5852
  }

  void compliant(String str) {
    str.split("(.*,)*");
    str.matches("(?s)(.*,)*.*");
    str.matches("(.*,)*(?s:.)*");
    str.matches("(?s)(.*,)*(.?)*");
    str.matches("(a|b)*");
    str.matches("(x*,){1,5}X");
    str.matches("((a|.a),)*");
    str.matches("(.*,)*[\\s\\S]*");
    str.matches("(?U)(.*,)*(.|\\s)*");
    str.matches("(x?,)?");
    str.matches("(?>.*,)*");
    str.matches("([^,]*+,)*");
    str.matches("(.*?,){5}");
    str.matches("(.*?,){1,5}");
    str.matches("([^,]*,)*");
    str.matches("(;?,)*");
    str.matches("(;*,)*");
    str.matches("x*|x*");
    str.matches("a*b*");
    str.matches("a*a?b*");
    str.matches("a*(a?b)*");
    str.matches("a*(ab)*");
    str.split("x*x*");
    str.matches("(?s)x*.*");
    str.matches("x*(?s)*"); // Coverage
    str.matches("(.*,)*("); // Rule is not applied to syntactically invalid regular expressions
  }

}
