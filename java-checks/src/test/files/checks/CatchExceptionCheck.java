class A {
  void m1() {
    try {
      m3();
    } catch (MyException e) {
    } catch (MyException1 | MyException2 e) {
    } catch (Exception e) { // Noncompliant [[sc=14;ec=23]] {{Catch a list of specific exception subtypes instead.}}
    }
    try {
    } catch (MyException1 | Exception e) { // Noncompliant
    }
    try {
      m2();
      m2();
    } catch (Exception e) { // compliant, m2 throws explicitly java.lang.Exception
    }
    try {
    } catch (UnsupportedEncodingException | UnsupportedDataTypeException | RuntimeException e) {
    }
  }

  void m2() throws Exception {
  }

  void m3() throws MyException {
  }
}

class B {
  public void foo() {
    try {
      unknownMethod();
    } catch (Exception e) { // Compliant
    }
  }
}
