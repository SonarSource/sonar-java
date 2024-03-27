package checks.design;

import java.util.ArrayList;
import java.util.List;
import org.checkerframework.checker.units.qual.A;
import org.junit.jupiter.api.Assertions;
import org.sonar.api.SonarProduct;
;

class ClassImportCouplingCheckSample { // Noncompliant [[sc=7;ec=37;secondary=15, 17, 19, 20, 21, 22, 23, 24, 25, 26, 27, 28, 29, 30, 31, 32, 33, 34, 35, 36, 37, 38, 40, 40, 40, 41, 48, 57, 74, 74, 79, 81, 87]] {{Split this “Monster Class” into smaller and more specialized ones to reduce its dependencies on other classes from 23 to the maximum authorized 20 or less.}}

  List<A> a = new ArrayList<>();
  SonarProduct sonarProduct;

  TestClass test;

  List<TestClass> someList;

  T1 t1;
  T2 t2;
  T3 t3;
  T4 t4;
  T5 t5;
  T6 t6;
  T7 t7;
  T8 t8;
  T9 t9;
  T10 t10;
  T11 t11;
  T12 t12;
  T13 t13;
  T14 t14;
  T15 t15;
  T16 t16;
  T17 t17;
  T18 t18;
  T19 t19;
  T20 t20;

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

    java.util.Collections collections;
  }

  String producerExtends(List<? extends T1> elements) {
    StringBuilder builder = new StringBuilder();
    for (T1 element : elements) {
      builder.append(element.toString());
    }
    return builder.toString();
  }

  String consumerSupers(List<? super T1> elements) {
    StringBuilder builder = new StringBuilder();
    for (var element : elements) {
      builder.append(element.toString());
    }
    return builder.toString();
  }

}

enum Qax2 { // Compliant - should not fail
  ;

  T1 foo() {
    return null;
  }
}
