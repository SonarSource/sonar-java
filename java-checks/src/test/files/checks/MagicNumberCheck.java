@Annotation(title = "plop", value = 51)
class A {
  // All compliant
  int a = 0;
  int b = 1;
  int c = -1;

  int d = 42; // Noncompliant {{Assign this magic number 42 to a well-named constant, and use the constant instead.}}
  long aLong = 12L; // Noncompliant {{Assign this magic number 12L to a well-named constant, and use the constant instead.}}
  double aDouble = 12.3d; // Noncompliant {{Assign this magic number 12.3d to a well-named constant, and use the constant instead.}}
  float aFloat = 12.3F; // Noncompliant {{Assign this magic number 12.3F to a well-named constant, and use the constant instead.}}
  String string = "string";
  String strDouble = "123.3d";
  boolean bool = true;

  long a = 0;
  long b = 1;
  long c = -1;

  double a = 0.0d;
  double b = 1.0d;
  double c = -1.0d;

  float a = 0.0f;
  float b = 1.0f;
  float c = -1.0f;

  private static final int CONSTANT = 42;

  private static final MyType MY_TYPE = new MyType() {
    int magic = 42;
  };

  public enum MyEnum {
    INSTANCE1(100), // Compliant
    INSTANCE2 { // Compliant
      void method() {
        System.out.println(42); // Noncompliant {{Assign this magic number 42 to a well-named constant, and use the constant instead.}}
      }
    };

    MyEnum(int value) {
    }
  }

}
