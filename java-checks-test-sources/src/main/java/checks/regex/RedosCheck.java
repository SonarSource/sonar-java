package checks.regex;

import org.junit.jupiter.api.Test;

public class RedosCheck {

  @Test
  void noncompliant(String str) {
    str.matches("(.*,)*"); // Noncompliant [[sc=18;ec=24;secondary=9]] {{Make sure this regex cannot lead to denial of service here.}}
    str.matches("(.*?,)+"); // Noncompliant [[sc=18;ec=25;secondary=10]] {{Make sure this regex cannot lead to denial of service here.}}
    str.matches("(.*?,){5,}"); // Noncompliant [[sc=18;ec=28;secondary=11]] {{Make sure this regex cannot lead to denial of service here.}}
    str.matches("((.*,)*)*+"); // Noncompliant [[sc=18;ec=28;secondary=12]] {{Make sure this regex cannot lead to denial of service here.}}
    str.matches("((.*,)*)?"); // Noncompliant [[sc=18;ec=27;secondary=13]] {{Make sure this regex cannot lead to denial of service here.}}
    str.matches("(?>(.*,)*)"); // Noncompliant [[sc=18;ec=28;secondary=14]] {{Make sure this regex cannot lead to denial of service here.}}
    str.matches("((?>.*,)*)*"); // Noncompliant [[sc=18;ec=29;secondary=15]] {{Make sure this regex cannot lead to denial of service here.}}
    str.matches("(.*,)* (.*,)*"); // Noncompliant [[sc=18;ec=31;secondary=16,16]] {{Make sure this regex cannot lead to denial of service here.}}

    // This one should be unproblematic, but we currently warn about it. If this causes a lot of FPs in real code,
    // we might want to add some exceptions to the rule. That said, making the inner * possessive is better anyway.
    str.matches("([^,]*,)*"); // Noncompliant [[sc=18;ec=27;secondary=20]] {{Make sure this regex cannot lead to denial of service here.}}
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
