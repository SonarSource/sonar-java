package checks;

class IncorrectOrderOfMembersCheck {
  class FieldAfterConstructor {
    FieldAfterConstructor() {
    }

    int field; // Noncompliant [[sc=9;ec=14]] {{Move this variable to comply with Java Code Conventions.}}
  }

  class ConstructorAfterMethod {
    void method() {
    }

    ConstructorAfterMethod() { // Noncompliant [[sc=5;ec=27]]
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
    private static String MESSAGE = "Static variables should be declared before instance variables"; // Noncompliant [[sc=27;ec=34]] {{Move this static variable to comply with Java Code Conventions.}}
    public int current;
    protected static int counter = 0;// Noncompliant [[sc=26;ec=33]] {{Move this static variable to comply with Java Code Conventions.}}
  }

  // The visibility of variables is not taken into account
  static class ProperVariablesOrdering {
    private static String MESSAGE = "Static variables should be declared before instance variables";
    protected static int counter = 0;
    boolean locked;
    public int current = counter;
  }
}


