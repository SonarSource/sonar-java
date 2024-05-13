package checks;

class IncorrectOrderOfMembersCheckSample {
  class FieldAfterConstructor {
    FieldAfterConstructor() {
    }

    int field; // Noncompliant {{Move this variable to comply with Java Code Conventions.}}
//      ^^^^^
  }

  class ConstructorAfterMethod {
    void method() {
    }

    ConstructorAfterMethod() { // Noncompliant
//  ^^^^^^^^^^^^^^^^^^^^^^
    }
  }

  class Ok {
    int field;

    Ok() {
    }

    void method() {
    }
  }

  static class WrongVariablesOrdering {
    boolean locked;
    private static String MESSAGE = "Static variables should be declared before instance variables"; // Noncompliant {{Move this static variable to comply with Java Code Conventions.}}
//                        ^^^^^^^
    public int current;
    protected static int counter = 0; // Noncompliant {{Move this static variable to comply with Java Code Conventions.}}
//                       ^^^^^^^
  }

  // The visibility of variables is not taken into account
  static class ProperVariablesOrdering {
    private static String MESSAGE = "Static variables should be declared before instance variables";
    protected static int counter = 0;
    boolean locked;
    public int current = counter;
  }

  class OneLiner { void foo() {System.out.println("");} }

  record Output(String title, String summary, String text) {
    public static final String TRUNCATE_MESSAGE = "abc"; // compliant
    static final int GREAT_VALUE = 42; // compliant
    private static final String DEFAULT = "xyz"; // compliant

    public boolean isTooLong() {
      return title.length() + summary.length() + text.length() > 1000;
    }

    public static final int DEFAULT_NUMBER = 0; // Noncompliant

    Output(String s) { // Noncompliant
      this(s, s, s);
    }
  }
}


