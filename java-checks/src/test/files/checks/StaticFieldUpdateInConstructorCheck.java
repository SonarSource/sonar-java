class A {
  String field;
  static String staticField;
  static String[] words = {"yolo", "fun"};
  static int value = 14;

  A() {
    field = "world"; // Compliant
    staticField = "hello"; // Noncompliant [[sc=5;ec=16;secondary=3]] {{Remove this assignment of "staticField".}}
    A.staticField = "again"; // Noncompliant [[sc=7;ec=18;secondary=3]] {{Remove this assignment of "staticField".}}
    words[0] = "noFun"; // Noncompliant [[sc=5;ec=10;secondary=4]] {{Remove this assignment of "words".}}
    value = 42; // Noncompliant [[sc=5;ec=10;secondary=5]] {{Remove this assignment of "value".}}
    value += 1; // Noncompliant [[sc=5;ec=10;secondary=5]] {{Remove this assignment of "value".}}

    String var = "boom";
    field = staticField = var = "why so mean, java"; // Noncompliant [[sc=13;ec=24;secondary=3]] {{Remove this assignment of "staticField".}}

    value++; // Compliant - postfix/prefix increment/decrement not taken into account

    getA().field = "beer"; // Compliant
    getA().staticField = "garden"; // Noncompliant

    (A.staticField) = "hello"; // Noncompliant
    (words)[1] = "polo"; // Noncompliant

    A.values()[1] = 14; // Compliant

    synchronized (new Object()) {
      field = "reworld"; // Compliant - synchronized block
    }
  }

  static A getA() {
    return null;
  }

  static int[] values() {
    return null;
  }

}
