package checks;

import java.util.Map;

public class InstanceOfPatternMatching {

  int if1(Object o) {
    if (o instanceof String) { // Noncompliant {{Replace this instanceof check and cast with 'instanceof String str'}}
      String str = (String) o;
//  ^^^<
      return str.length();
    }
    return 0;
  }

  int if2() {
    if (o instanceof String) { // Compliant because we can't access the symbol for o
      String str = (String) o;
      return str.length();
    }
    return 0;
  }

  int if3(Object o) {
    if (o instanceof Blablabla) { // Compliant because we can't access the symbol for Blablabla
      Blablabla str = (Blablabla) o;
      return str.length();
    }
    return 0;
  }
}
