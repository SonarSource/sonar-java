package checks;

import org.sonar.api.Properties;

class LeftCurlyBraceStartLineCheck {    // Noncompliant [[sc=36;ec=37]] {{Move this left curly brace to the beginning of next line of code.}}

  class Gul
  {              // Compliant
  }

  class
  Doo {          // Noncompliant {{Move this left curly brace to the beginning of next line of code.}}
  }

  class Koo
  {              // Compliant
    void koo() { // Noncompliant {{Move this left curly brace to the beginning of next line of code.}}
    }
  }

  class Bar
  {              // Compliant
    void bar()
    {            // Compliant
    }
  }

  @Properties({ // Compliant
  })
  class Exceptions
  {
    int[] numbers = new int[] { 0, 1 }; // Compliant
  }
  public @interface Resolver
  {
    String codeRuleId();
  }
}

