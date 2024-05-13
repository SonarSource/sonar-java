package checks;

class StaticFieldUpdateInConstructorCheckSample {
  String field;
  static String staticField;
//^^^^^^^^^^^^^^^^^^^^^^^^^^>
  static String[] words = {"yolo", "fun"};
  static int value = 14;

  StaticFieldUpdateInConstructorCheckSample() {
    field = "world"; // Compliant
    staticField = "hello"; // Noncompliant {{Remove this assignment of "staticField".}}
//  ^^^^^^^^^^^
    StaticFieldUpdateInConstructorCheckSample.staticField = "again"; // Noncompliant {{Remove this assignment of "staticField".}}
//                                            ^^^^^^^^^^^
    words[0] = "noFun"; // Noncompliant {{Remove this assignment of "words".}}
//  ^^^^^
//^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^@-10<
    value = 42; // Noncompliant {{Remove this assignment of "value".}}
//  ^^^^^
//^^^^^^^^^^^^^^^^^^^^^^@-12<
    value += 1; // Noncompliant {{Remove this assignment of "value".}}
//  ^^^^^

    String var = "boom";
    field = staticField = var = "why so mean, java"; // Noncompliant {{Remove this assignment of "staticField".}}
//          ^^^^^^^^^^^

    value++; // Compliant - postfix/prefix increment/decrement not taken into account

    getA().field = "beer"; // Compliant
    getA().staticField = "garden"; // Noncompliant

    (StaticFieldUpdateInConstructorCheckSample.staticField) = "hello"; // Noncompliant
    (words)[1] = "polo"; // Noncompliant

    StaticFieldUpdateInConstructorCheckSample.values()[1] = 14; // Compliant

    synchronized (new Object()) {
      field = "reworld"; // Compliant - synchronized block
    }
  }

  static StaticFieldUpdateInConstructorCheckSample getA() {
    return null;
  }

  static int[] values() {
    return null;
  }

}
