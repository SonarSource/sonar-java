void main() {
  System.out.println("Just right.");
  if (true) {
        System.out.println("Too much."); // Noncompliant
  }
}

class MyClass {
  void myMethod() {
    System.err.println("Error message");
  }

  static private class MyNestedClass {
    private final static int VALUE = 42;
    public float pi = 3.14f;
      public String badlyIndentedField = "Oops"; // Noncompliant
      public String secondBadlyIndentedField = "Oops x 2"; // Compliant, raised on previous line


    void anotherMethod() {
      System.out.println("Nested class output");
    }
  }
}

int i = 0;
  public String badlyIndentedField = "Oops"; // Noncompliant
  public String secondBadlyIndentedField = "Oops x 2"; // Compliant, raised on previous line
int j = 0;
