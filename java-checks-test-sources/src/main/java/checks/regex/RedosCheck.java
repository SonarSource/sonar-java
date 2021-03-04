package checks.regex;

import java.util.regex.Pattern;
import javax.validation.constraints.Email;

public class RedosCheck {

  @Email(regexp = "(.*-)*@.*") // Noncompliant [[sc=4;ec=9]] {{Make sure the regex used here, which is vulnerable to quadratic runtime due to backtracking, cannot lead to denial of service.}}
  String email;

  void realWorldExamples(String str) {
    String cloudflareAttack = "(?:(?:\"|'|\\]|\\}|\\\\|\\d|(?:nan|infinity|true|false|null|undefined|symbol|math)|\\`|\\-|\\+)+[)]*;?((?:\\s|-|~|!|\\{\\}|\\|\\||\\+)*.*(?:.*=.*)))";
    String stackOverflowAttack = "^[\\s\\u200c]+|[\\s\\u200c]+$";
    str.replaceAll(cloudflareAttack, ""); // Noncompliant {{Make sure the regex used here, which is vulnerable to quadratic runtime due to backtracking, cannot lead to denial of service.}}
    str.replaceAll(stackOverflowAttack, ""); // Noncompliant {{Make sure the regex used here, which is vulnerable to quadratic runtime due to backtracking, cannot lead to denial of service.}}
  }

  void fullAndPartialMatches(String str) {
    Pattern p1 = Pattern.compile("(.*,)*"); // Compliant because it's never used for a full match
    Pattern p2 = Pattern.compile("(.*,)*"); // Noncompliant {{Make sure the regex used here, which is vulnerable to quadratic runtime due to backtracking, cannot lead to denial of service.}}
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

  void quadraticInJava9(String str) {
    str.matches("(.*,)*"); // Noncompliant {{Make sure the regex used here, which is vulnerable to quadratic runtime due to backtracking, cannot lead to denial of service.}}
    str.matches("(.*,)*.*"); // Noncompliant {{Make sure the regex used here, which is vulnerable to quadratic runtime due to backtracking, cannot lead to denial of service.}}
    str.split("(.*,)*X"); // Noncompliant {{Make sure the regex used here, which is vulnerable to quadratic runtime due to backtracking, cannot lead to denial of service.}}
    str.matches("(.*,)*X"); // Noncompliant {{Make sure the regex used here, which is vulnerable to quadratic runtime due to backtracking, cannot lead to denial of service.}}
    str.matches("(.*?,)+"); // Noncompliant {{Make sure the regex used here, which is vulnerable to quadratic runtime due to backtracking, cannot lead to denial of service.}}
    str.matches("(.*?,){5,}"); // Noncompliant {{Make sure the regex used here, which is vulnerable to quadratic runtime due to backtracking, cannot lead to denial of service.}}
    str.matches("((.*,)*)*+"); // Noncompliant {{Make sure the regex used here, which is vulnerable to quadratic runtime due to backtracking, cannot lead to denial of service.}}
    str.matches("((.*,)*)?"); // Noncompliant {{Make sure the regex used here, which is vulnerable to quadratic runtime due to backtracking, cannot lead to denial of service.}}
    str.matches("(?>(.*,)*)"); // Noncompliant {{Make sure the regex used here, which is vulnerable to quadratic runtime due to backtracking, cannot lead to denial of service.}}
    str.matches("((?>.*,)*)*"); // Noncompliant {{Make sure the regex used here, which is vulnerable to quadratic runtime due to backtracking, cannot lead to denial of service.}}
    str.matches("(.*,)* (.*,)*"); // Noncompliant {{Make sure the regex used here, which is vulnerable to quadratic runtime due to backtracking, cannot lead to denial of service.}}
    str.split("(.*,)*$"); // Noncompliant {{Make sure the regex used here, which is vulnerable to quadratic runtime due to backtracking, cannot lead to denial of service.}}
    str.matches("(.*,)*$"); // Noncompliant {{Make sure the regex used here, which is vulnerable to quadratic runtime due to backtracking, cannot lead to denial of service.}}
    str.matches("(.*,)*(..)*"); // Noncompliant {{Make sure the regex used here, which is vulnerable to quadratic runtime due to backtracking, cannot lead to denial of service.}}
    str.matches("(.*,)*(.{2})*"); // Noncompliant {{Make sure the regex used here, which is vulnerable to quadratic runtime due to backtracking, cannot lead to denial of service.}}
  }

  void alwaysQuadratic(String str) {
    str.matches("x*\\w*"); // Noncompliant {{Make sure the regex used here, which is vulnerable to quadratic runtime due to backtracking, cannot lead to denial of service.}}
    str.matches(".*.*X"); // Noncompliant {{Make sure the regex used here, which is vulnerable to quadratic runtime due to backtracking, cannot lead to denial of service.}}
    str.matches("x*a*x*"); // Noncompliant {{Make sure the regex used here, which is vulnerable to quadratic runtime due to backtracking, cannot lead to denial of service.}}
    str.matches("x*a*b*c*d*e*f*g*h*i*x*"); // Noncompliant {{Make sure the regex used here, which is vulnerable to quadratic runtime due to backtracking, cannot lead to denial of service.}}
    str.matches("x*a*b*c*d*e*f*g*h*i*j*x*"); // FN because we forget about the first x* when the maximum number of tracked repetitions is exceeded
    str.matches("x*a*b*c*d*e*f*g*h*i*j*x*x*"); // Noncompliant {{Make sure the regex used here, which is vulnerable to quadratic runtime due to backtracking, cannot lead to denial of service.}}
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
    str.split("x*x*");
    str.matches("(?s)x*.*");
    str.matches("x*(?s)*"); // Coverage
    str.matches("(.*,)*("); // Rule is not applied to syntactically invalid regular expressions
  }

}
