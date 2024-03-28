package checks.regex;

import java.util.regex.Pattern;
import javax.validation.constraints.Email;

public class RedosCheckSample {

  @Email(regexp = "(.*-)*@.*") // Noncompliant [[sc=4;ec=9]] {{Make sure the regex used here, which is vulnerable to polynomial runtime due to backtracking, cannot lead to denial of service.}}
  String email;

  @jakarta.validation.constraints.Email(regexp = "(.*-)*@.*") // Noncompliant [[sc=4;ec=40]] {{Make sure the regex used here, which is vulnerable to polynomial runtime due to backtracking, cannot lead to denial of service.}}
  String email2;

  void realWorldExamples(String str) {
    String cloudflareAttack = "(?:(?:\"|'|\\]|\\}|\\\\|\\d|(?:nan|infinity|true|false|null|undefined|symbol|math)|\\`|\\-|\\+)+[)]*;?((?:\\s|-|~|!|\\{\\}|\\|\\||\\+)*.*(?:.*=.*)))";
    String stackOverflowAttack = "^[\\s\\u200c]+|[\\s\\u200c]+$";
    str.matches(cloudflareAttack); // Noncompliant {{Make sure the regex used here, which is vulnerable to polynomial runtime due to backtracking, cannot lead to denial of service.}}
    str.replaceAll(stackOverflowAttack, ""); // Noncompliant {{Make sure the regex used here, which is vulnerable to polynomial runtime due to backtracking, cannot lead to denial of service.}}
  }

  void fullAndPartialMatches(String str) {
    Pattern p1 = Pattern.compile("(.*,)*"); // Compliant because it's never used for a full match
    Pattern p2 = Pattern.compile("(.*,)*"); // Noncompliant {{Make sure the regex used here, which is vulnerable to polynomial runtime due to backtracking, cannot lead to denial of service.}}
    p1.matcher(str).find();
    p2.matcher(str).find();
    p2.matcher(str).matches();
  }

  void alwaysExponential(String str) {
    str.matches("(.*,)*?"); // Noncompliant [[sc=9;ec=16]] {{Make sure the regex used here, which is vulnerable to exponential runtime due to backtracking, cannot lead to denial of service.}}
    str.matches("(.?,)*?"); // Noncompliant {{Make sure the regex used here, which is vulnerable to exponential runtime due to backtracking, cannot lead to denial of service.}}
    str.matches("(a|.a)*?"); // Noncompliant {{Make sure the regex used here, which is vulnerable to exponential runtime due to backtracking, cannot lead to denial of service.}}
    str.matches("(?:.*,)*(X)\\1"); // Noncompliant {{Make sure the regex used here, which is vulnerable to exponential runtime due to backtracking, cannot lead to denial of service.}}
    str.matches("(.*,)*\\1"); // Noncompliant {{Make sure the regex used here, which is vulnerable to exponential runtime due to backtracking, cannot lead to denial of service.}}
  }

  void polynomialInJava9(String str) {
    str.matches("(.*,)*"); // Noncompliant {{Make sure the regex used here, which is vulnerable to polynomial runtime due to backtracking, cannot lead to denial of service.}}
    str.matches("(.*,)*.*"); // Noncompliant {{Make sure the regex used here, which is vulnerable to polynomial runtime due to backtracking, cannot lead to denial of service.}}
    str.split("(.*,)*X"); // Noncompliant {{Make sure the regex used here, which is vulnerable to polynomial runtime due to backtracking, cannot lead to denial of service.}}
    str.matches("(.*,)*X"); // Noncompliant {{Make sure the regex used here, which is vulnerable to polynomial runtime due to backtracking, cannot lead to denial of service.}}
    str.matches("(.*?,)+"); // Noncompliant {{Make sure the regex used here, which is vulnerable to polynomial runtime due to backtracking, cannot lead to denial of service.}}
    str.matches("(.*?,){5,}"); // Noncompliant {{Make sure the regex used here, which is vulnerable to polynomial runtime due to backtracking, cannot lead to denial of service.}}
    str.matches("((.*,)*)*+"); // Noncompliant {{Make sure the regex used here, which is vulnerable to polynomial runtime due to backtracking, cannot lead to denial of service.}}
    str.matches("((.*,)*)?"); // Noncompliant {{Make sure the regex used here, which is vulnerable to polynomial runtime due to backtracking, cannot lead to denial of service.}}
    str.matches("(?>(.*,)*)"); // Noncompliant {{Make sure the regex used here, which is vulnerable to polynomial runtime due to backtracking, cannot lead to denial of service.}}
    str.matches("((?>.*,)*)*"); // Noncompliant {{Make sure the regex used here, which is vulnerable to polynomial runtime due to backtracking, cannot lead to denial of service.}}
    str.matches("(.*,)* (.*,)*"); // Noncompliant {{Make sure the regex used here, which is vulnerable to polynomial runtime due to backtracking, cannot lead to denial of service.}}
    str.split("(.*,)*$"); // Noncompliant {{Make sure the regex used here, which is vulnerable to polynomial runtime due to backtracking, cannot lead to denial of service.}}
    str.matches("(.*,)*$"); // Noncompliant {{Make sure the regex used here, which is vulnerable to polynomial runtime due to backtracking, cannot lead to denial of service.}}
    str.matches("(.*,)*(..)*"); // Noncompliant {{Make sure the regex used here, which is vulnerable to polynomial runtime due to backtracking, cannot lead to denial of service.}}
    str.matches("(.*,)*(.{2})*"); // Noncompliant {{Make sure the regex used here, which is vulnerable to polynomial runtime due to backtracking, cannot lead to denial of service.}}
  }

  void alwaysQuadratic(String str) {
    // Always polynomial when two non-possessive quantifiers overlap in a sequence
    str.matches("x*\\w*"); // Noncompliant {{Make sure the regex used here, which is vulnerable to polynomial runtime due to backtracking, cannot lead to denial of service.}}
    str.matches(".*.*X"); // Noncompliant {{Make sure the regex used here, which is vulnerable to polynomial runtime due to backtracking, cannot lead to denial of service.}}
    str.matches("x*a*x*"); // Noncompliant {{Make sure the regex used here, which is vulnerable to polynomial runtime due to backtracking, cannot lead to denial of service.}}
    str.matches("x*,a*x*"); // Compliant, can fail between the two quantifiers
    str.matches("x*(xy?)*"); // Noncompliant {{Make sure the regex used here, which is vulnerable to polynomial runtime due to backtracking, cannot lead to denial of service.}}
    str.matches("(ab)*a(ba)*"); // False Negative :-(
    str.matches("x*xx*"); // Noncompliant {{Make sure the regex used here, which is vulnerable to polynomial runtime due to backtracking, cannot lead to denial of service.}}
    str.matches("x*yx*"); // Compliant
    str.matches("x*a*b*c*d*e*f*g*h*i*x*"); // Noncompliant {{Make sure the regex used here, which is vulnerable to polynomial runtime due to backtracking, cannot lead to denial of service.}}
    str.matches("x*a*b*c*d*e*f*g*h*i*j*x*"); // FN because we forget about the first x* when the maximum number of tracked repetitions is exceeded
    str.matches("x*a*b*c*d*e*f*g*h*i*j*x*x*"); // Noncompliant {{Make sure the regex used here, which is vulnerable to polynomial runtime due to backtracking, cannot lead to denial of service.}}
    // Non-possessive followed by possessive quantifier is actually polynomial
    str.matches(".*\\s*"); // Noncompliant {{Make sure the regex used here, which is vulnerable to polynomial runtime due to backtracking, cannot lead to denial of service.}}
    str.matches(".*\\s*+"); // Noncompliant {{Make sure the regex used here, which is vulnerable to polynomial runtime due to backtracking, cannot lead to denial of service.}}
    str.matches(".*+\\s*"); // Compliant, other way (possessive then non-possessive) is fine
    str.matches(".*+\\s*+"); // Compliant, two possessives is fine
    str.matches(".*,\\s*+,"); // Compliant, can fail between the two quantifiers
    str.matches("\\s*\\s*+,"); // Noncompliant {{Make sure the regex used here, which is vulnerable to polynomial runtime due to backtracking, cannot lead to denial of service.}}
    str.matches("a*\\s*+,"); // Compliant, no overlap
    str.matches("[a\\s]*\\s*+,"); // Noncompliant {{Make sure the regex used here, which is vulnerable to polynomial runtime due to backtracking, cannot lead to denial of service.}}
    str.matches("[a\\s]*b*\\s*+,"); // Noncompliant {{Make sure the regex used here, which is vulnerable to polynomial runtime due to backtracking, cannot lead to denial of service.}}
    str.matches("\\s*+[a\\s]*b*,"); // Compliant, possessive then non-possessive
    str.matches("\\s*+b*[a\\s]*,"); // Compliant, possessive then non-possessive
    // Implicit reluctant quantifier in partial match also leads to polynomial runtime
    str.split("\\s*,"); // Noncompliant {{Make sure the regex used here, which is vulnerable to polynomial runtime due to backtracking, cannot lead to denial of service.}}
    str.split("\\s*+,"); // Noncompliant {{Make sure the regex used here, which is vulnerable to polynomial runtime due to backtracking, cannot lead to denial of service.}}
    str.matches("(?s:.*)\\s*,(?s:.*)"); // Noncompliant {{Make sure the regex used here, which is vulnerable to polynomial runtime due to backtracking, cannot lead to denial of service.}}
    str.matches("(?s:.*)\\s*+,(?s:.*)"); // Noncompliant {{Make sure the regex used here, which is vulnerable to polynomial runtime due to backtracking, cannot lead to denial of service.}}
    str.split(",\\s*+"); // Compliant
    str.split(",\\s*+,"); // Compliant
    str.split("\\s*+"); // Compliant
  }

  void differentPolynomials(String str) {
    // quadratic (O(n^2))
    str.matches("x*x*"); // Noncompliant {{Make sure the regex used here, which is vulnerable to polynomial runtime due to backtracking, cannot lead to denial of service.}}
    // cubic (O(n^3))
    str.matches("x*x*x*"); // Noncompliant {{Make sure the regex used here, which is vulnerable to polynomial runtime due to backtracking, cannot lead to denial of service.}}
    // O(n^4)
    str.matches("x*x*x*x*"); // Noncompliant {{Make sure the regex used here, which is vulnerable to polynomial runtime due to backtracking, cannot lead to denial of service.}}
    // O(n^5)
    str.matches("x*x*x*x*x*"); // Noncompliant {{Make sure the regex used here, which is vulnerable to polynomial runtime due to backtracking, cannot lead to denial of service.}}
    // cubic
    str.matches("[^=]*.*.*=.*"); // Noncompliant {{Make sure the regex used here, which is vulnerable to polynomial runtime due to backtracking, cannot lead to denial of service.}}
  }

  void fixedInJava9(String str) {
    str.matches("(.?,)*X");
  }

  void notFixedInJava9(String str) {
    // The back reference prevents the Java 9+ optimization from being applied
    str.matches("(.?,)*\\1"); // Noncompliant {{Make sure the regex used here, which is vulnerable to exponential runtime due to backtracking, cannot lead to denial of service.}}
    str.matches("(?:(.?)\\1,)*"); // FN because RegexTreeHelpers.intersects can't currently handle backreferences inside the repetition
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
