package checks;

import java.util.Map;

public class InstanceOfPatternMatching {

  int if1(Object o) {
    if (o instanceof String) {  // Noncompliant {{Replace this instanceof check and cast with 'instanceof String str'}} [[secondary=+1]]
      String str = (String) o;
      return str.length();
    }
    return 0;
  }

  int if1Compliant(Object o) {
    if (o instanceof String string) {  // Compliant
      return string.length();
    }
    return 0;
  }

  int if2(Object o) {
    if (1 > 2 && o instanceof String && 3 > 4) {  // Noncompliant {{Replace this instanceof check and cast with 'instanceof String str'}} [[secondary=+2]]
      System.out.println("holla");
      String str = (String) o;
      return str.length();
    }
    return 0;
  }

  int if3(Object o) {
    if (o instanceof String) {  // Noncompliant {{Replace this instanceof check and cast with 'instanceof String str'}} [[secondary=+2]]
      if (23 < 42) {
        String str = (String) o;
        return str.length();
      }
    }
    return 0;
  }

  Object o;
  int if4() {
    if (o instanceof String) { // Noncompliant {{Replace this instanceof check and cast with 'instanceof String str'}} [[secondary=+1]]
      String str = (String) o;
      return str.length();
    }
    return 0;
  }

  int if4Compliant() {
    if (o instanceof String) {
      Object o = "shadow";
      String str = (String) o; // Compliant because we're casting a different o than the one we instanceofed
      return str.length();
    }
    return 0;
  }

  int if5(Map<String, Object> map) {
    if (map.get("hello") instanceof String) {  // Noncompliant {{Replace this instanceof check and cast with 'instanceof String str'}} [[secondary=+1]]
      String str = (String) map.get("hello");
      return str.length();
    }
    return 0;
  }

  int if5Compliant(Map<String, Object> map) {
    if (map.get("hello") instanceof String) {
      String str = (String) map.get("goodbye"); // Compliant because we're getting a different key than we instanceofed
      return str.length();
    }
    return 0;
  }

  int if16(Object o) {
    // Since the assignment of the cast happens separately from the variable declaration, we don't use the variable name
    // in the error message
    if (o instanceof String) { // Noncompliant {{Replace this instanceof check and cast with 'instanceof String string'}} [[secondary=+2]]
      String str;
      str = (String) o;
      return str.length();
    }
    return 0;
  }

  int if7(Object o) {
    if (o instanceof Integer) {  // Compliant because different types are used for the cast
      String str = (String) o;
      return str.length();
    }
    return 0;
  }

  int if8(Object o1, Object o2) {
    if (o1 instanceof String) {  // Compliant because we're instanceoffing a different object than we cast
      String str = (String) o2;
      return str.length();
    }
    return 0;
  }

  boolean and1(Object o) {
    return (o instanceof String && ((String) o).length() > 0); // Noncompliant {{Replace this instanceof check and cast with 'instanceof String string'}} [[secondary=+0]]
  }

  boolean and1Compliant(Object o) {
    return (o instanceof String s && s.length() > 0);  // Compliant
  }

  boolean and2(Object o) {
    return (1 < 4 && o instanceof String && ((String) o).length() > 0 && 23 < 42); // Noncompliant {{Replace this instanceof check and cast with 'instanceof String string'}} [[secondary=+0]]
  }

  int ternary(Object o) {
    return (o instanceof String) ? ((String) o).length() : 0; // Noncompliant {{Replace this instanceof check and cast with 'instanceof String string'}} [[secondary=+0]]
  }

  int ternaryCompliant(Object o) {
    return (o instanceof String s) ? s.length() : 0; // Compliant
  }

}
