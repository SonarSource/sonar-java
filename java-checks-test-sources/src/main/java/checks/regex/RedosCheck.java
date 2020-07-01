package checks.regex;

import org.junit.jupiter.api.Test;

public class RedosCheck {

  @Test
  void noncompliant(String str) {
    str.matches("(.*,)*"); // Noncompliant [[sc=9;ec=16]] {{Make sure the regex used in this method call cannot lead to denial of service.}}
    str.matches("(.*?,)+"); // Noncompliant
    str.matches("(.*?,){5,}"); // Noncompliant
    str.matches("((.*,)*)*+"); // Noncompliant
    str.matches("((.*,)*)?"); // Noncompliant
    str.matches("(?>(.*,)*)"); // Noncompliant
    str.matches("((?>.*,)*)*"); // Noncompliant
    str.matches("(.*,)* (.*,)*"); // Noncompliant

    // This one should be unproblematic, but we currently warn about it. If this causes a lot of FPs in real code,
    // we might want to add some exceptions to the rule. That said, making the inner * possessive is better anyway.
    str.matches("([^,]*,)*"); // Noncompliant
  }

  void compliant(String str) {
    str.matches("(x?,)?");
    str.matches("(?>.*,)*");
    str.matches("([^,]*+,)*");
    str.matches("(.*?,){5}");
    str.matches("(.*?,){1,5}");
    str.matches("(.*,)*("); // Rule is not applied to syntactically invalid regular expressions
  }

}
