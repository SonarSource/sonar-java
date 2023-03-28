package checks.design;

import java.util.ArrayList;
import java.util.List;
import org.checkerframework.checker.units.qual.A;
import org.junit.jupiter.api.Assertions;
import org.sonar.api.SonarProduct;

class ClassImportCouplingCheck { // Noncompliant

  List<A> a = new ArrayList<>();
  SonarProduct sonarProduct;

  TestClass test;

  List<TestClass> someList;

  public TestClass2 method(Math math, TestClass testClass, TestClass2 testClass2) {
    TestClass2 t2 = new TestClass2();
    if (t2.equals(new Object())) {
    }
    Assertions.assertTrue(true);
    return null;
  }

  TestAbstract testAbstract = new TestAbstract() {
    @Override
    void t() {

    }
  };

  enum Qax {
    ;
    T1 foo() {
      return null;
    }
  }

  private class InnerClass {
  }

  class Tmp<T1> {
    void m() {
      try {
      } catch (Exception e) {
      }
      Object o;
    }

    Object o;
    checks.design.T1 t1 = (checks.design.T1) o;
  }

}
