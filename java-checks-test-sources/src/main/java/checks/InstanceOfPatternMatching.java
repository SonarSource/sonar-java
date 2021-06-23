package checks;

import java.util.Map;

public abstract class InstanceOfPatternMatching {

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

  int if9(Object o) {
    if (1 > 2 || o instanceof String || 3 > 4) {  // Compliant because the instanceof doesn't dominate the cast (would be nice to have an invalid cast SE rule to catch this as a bug)
      System.out.println("holla");
      String str = (String) o;
      return str.length();
    }
    return 0;
  }

  int if10(Object o) {
    if (!(o instanceof String)) {  // Compliant because the instanceof doesn't dominate the cast (would be nice to have an invalid cast SE rule to catch this as a bug)
      System.out.println("holla");
      String str = (String) o;
      return str.length();
    }
    return 0;
  }


  int ifElse1(Object o) {
    if (!(o instanceof String)) {  // Noncompliant {{Replace this instanceof check and cast with 'instanceof String str'}} [[secondary=+2]]
    } else {
      String str = (String) o;
      return str.length();
    }
    return 0;
  }

  int ifElse2(Object o) {
    if (o instanceof String) {  // Compliant because the condition isn't active in the else clause (would be nice to have an invalid cast SE rule to catch this as a bug)
    } else {
      String str = (String) o;
      return str.length();
    }
    return 0;
  }

  int ifElse3(Object o) {
    if (!(o instanceof String || 1 < 2)) {  // Noncompliant {{Replace this instanceof check and cast with 'instanceof String str'}} [[secondary=+2]]
    } else {
      String str = (String) o;
      return str.length();
    }
    return 0;
  }

  int ifElse4(Object o) {
    if (!(o instanceof String && 1 < 2)) {  // Compliant because the instanceof doesn't dominate the else
    } else {
      String str = (String) o;
      return str.length();
    }
    return 0;
  }

  int ifElse5(Object o) {
    if (!!(o instanceof String)) {  // Compliant because the condition isn't active in the else clause (would be nice to have an invalid cast SE rule to catch this as a bug)
    } else {
      String str = (String) o;
      return str.length();
    }
    return 0;
  }

  int ifElse6(Object o) {
    if (!!!(o instanceof String)) {  // Noncompliant {{Replace this instanceof check and cast with 'instanceof String str'}} [[secondary=+2]]
    } else {
      String str = (String) o;
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

  boolean and3(Object o) {
    return (!(o instanceof String) && ((String) o).length() > 0); // Compliant because the instanceof isn't in effect for the right operand
  }

  boolean or1(Object o) {
    return (!(o instanceof String) || ((String) o).length() > 0); // Noncompliant {{Replace this instanceof check and cast with 'instanceof String string'}} [[secondary=+0]]
  }

  boolean or2(Object o) {
    return (o instanceof String || ((String) o).length() > 0); // Compliant because the instanceof isn't in effect for the right operand
  }


  int ternary(Object o) {
    return (o instanceof String) ? ((String) o).length() : 0; // Noncompliant {{Replace this instanceof check and cast with 'instanceof String string'}} [[secondary=+0]]
  }

  int ternaryCompliant(Object o) {
    return (o instanceof String s) ? s.length() : 0; // Compliant
  }

  abstract Object getNext();

  void whileLoop1() {
    Object o = getNext();
    while (o instanceof String) { // Noncompliant {{Replace this instanceof check and cast with 'instanceof String str'}} [[secondary=+1]]
      String str = (String) o;
      o = getNext();
    }
  }

  void whileLoop2() {
    Object o = getNext();
    while (!(o instanceof String)) { // Compliant because instanceof not active inside loop
      String str = (String) o;
      o = getNext();
    }
  }

  void whileLoop3() {
    Object o = getNext();
    while (!(o instanceof String)) { // FN because we don't detect casts that come after the loop
      o = getNext();
    }
    String str = (String) o;
  }

  void forLoop1() {
    for (Object o = getNext(); o instanceof String; o = getNext()) { // Noncompliant {{Replace this instanceof check and cast with 'instanceof String str'}} [[secondary=+1]]
      String str = (String) o;
    }
  }

  void forLoop2() {
    for (Object o = getNext(); !(o instanceof String); o = getNext()) { // Compliant because instanceof not active inside loop
      String str = (String) getNext();
    }
  }

  void forLoop3() {
    for (Object o = getNext(); ; o = getNext()) { // Test that we don't throw for empty conditions
      String str = (String) getNext();
    }
  }


}
