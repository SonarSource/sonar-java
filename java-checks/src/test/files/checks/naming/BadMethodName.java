import org.junit.Test;

class BadMethodName {
  public BadMethodName() {
  }

  void Bad() { // Noncompliant [[sc=8;ec=11]] {{Rename this method name to match the regular expression '^[a-z][a-zA-Z0-9]*$'.}}
  }

  void good() {
  }

  @Override
  void BadButOverrides(){
  }

  @Deprecated
  void Bad2() { // Noncompliant
  }

  public String toString() { //Overrides from object
    return "...";
  }

  @Test
  public void my_test_1() { } // Compliant

  @org.junit.jupiter.api.Test
  public void my_test_2() { } // Compliant

  @org.testng.annotations.Test
  public void my_test_3() { } // Compliant

  @org.foo.bar.Test
  public void not_a_recognized_test() { } // Compliant

  @org.foo.bar.Test2
  public void not_a_recognized_test() { } // Noncompliant

}
