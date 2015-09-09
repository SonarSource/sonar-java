
class A {
  void m1() {
    try {
      m3();
      notfound();
    } catch (MyException e) {
    } catch (MyException1|MyException2 e) {
    } catch (Exception e) { // Noncompliant {{Catch a list of specific exception subtypes instead.}}
    }
    try {
    } catch (MyException1|Exception e) { // Noncompliant
    }
    try {
      m2();
      m2();
    } catch (Exception e) { // compliant, m1 throws explicitly java.lang.Exception
    }
    try {
    } catch (UnsupportedEncodingException|UnsupportedDataTypeException|RuntimeException e) {
    }
  }

  void m2() throws Exception {}
  void m3() throws MyException {}
}
