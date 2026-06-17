package checks.regex;

import javax.validation.constraints.Email;

public class SuperLinearRegexCheckJava8 {

  @Email(regexp = "(.*-)*@.*") // Compliant - QUADRATIC_WHEN_OPTIMIZED on Java 8, reported by S5852
  String email;

  @jakarta.validation.constraints.Email(regexp = "(.*-)*@.*") // Compliant - QUADRATIC_WHEN_OPTIMIZED on Java 8, reported by S5852
  String email2;

  void alwaysExponential(String str) {
    str.matches("(.*,)*?"); // Compliant - always exponential, reported by S5852
    str.matches("(.?,)*?"); // Compliant - always exponential, reported by S5852
    str.matches("(a|.a)*?"); // Compliant - always exponential, reported by S5852
    str.matches("(.*,)*X\\1"); // Compliant - QUADRATIC_WHEN_OPTIMIZED on Java 8, reported by S5852
    str.matches("(.*,)*\\1"); // Compliant - QUADRATIC_WHEN_OPTIMIZED on Java 8, reported by S5852
  }

  void quadraticInJava9(String str) {
    str.matches("(.*,)*"); // Compliant - QUADRATIC_WHEN_OPTIMIZED on Java 8, reported by S5852
    str.matches("(.*,)*.*"); // Compliant - QUADRATIC_WHEN_OPTIMIZED on Java 8, reported by S5852
    str.split("(.*,)*X"); // Compliant - QUADRATIC_WHEN_OPTIMIZED on Java 8, reported by S5852
    str.matches("(.*,)*X"); // Compliant - QUADRATIC_WHEN_OPTIMIZED on Java 8, reported by S5852
    str.matches("(.*?,)+"); // Compliant - QUADRATIC_WHEN_OPTIMIZED on Java 8, reported by S5852
    str.matches("(.*?,){5,}"); // Compliant - QUADRATIC_WHEN_OPTIMIZED on Java 8, reported by S5852
    str.matches("((.*,)*)*+"); // Compliant - QUADRATIC_WHEN_OPTIMIZED on Java 8, reported by S5852
    str.matches("((.*,)*)?"); // Compliant - QUADRATIC_WHEN_OPTIMIZED on Java 8, reported by S5852
    str.matches("(?>(.*,)*)"); // Compliant - QUADRATIC_WHEN_OPTIMIZED on Java 8, reported by S5852
    str.matches("((?>.*,)*)*"); // Compliant - QUADRATIC_WHEN_OPTIMIZED on Java 8, reported by S5852
    str.matches("(.*,)* (.*,)*"); // Compliant - QUADRATIC_WHEN_OPTIMIZED on Java 8, reported by S5852
    str.split("(.*,)*$"); // Compliant - QUADRATIC_WHEN_OPTIMIZED on Java 8, reported by S5852
    str.matches("(.*,)*$"); // Compliant - QUADRATIC_WHEN_OPTIMIZED on Java 8, reported by S5852
  }

  void alwaysQuadratic(String str) {
    str.matches("x*\\w*"); // Noncompliant {{Simplify this regular expression to reduce its runtime, as it has super-linear performance due to backtracking.}}
    str.matches(".*.*X"); // Noncompliant {{Simplify this regular expression to reduce its runtime, as it has super-linear performance due to backtracking.}}
  }

  void fixedInJava9(String str) {
    str.matches("(.?,)*X"); // Compliant - LINEAR_WHEN_OPTIMIZED on Java 8, reported by S5852
  }

  void notFixedInJava9(String str) {
    // The back reference prevents the Java 9+ optimization from being applied
    str.matches("(.?,)*\\1"); // Compliant - LINEAR_WHEN_OPTIMIZED + backref on Java 8, reported by S5852
  }

  void compliant(String str) {
    str.split("(.*,)*");
    str.matches("(?s)(.*,)*.*");
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
    str.matches("(.*,)*("); // Rule is not applied to syntactically invalid regular expressions
  }

}
