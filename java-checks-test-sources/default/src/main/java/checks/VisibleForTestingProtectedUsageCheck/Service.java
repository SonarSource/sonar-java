package checks.VisibleForTestingProtectedUsageCheck;

import com.google.common.annotations.VisibleForTesting;

public class Service {

  public String f(int param) {

    TestOnly testOnly = null; // Compliant, TestOnly class visible if it is private

    MyObject.Nested nested = null; // Noncompliant {{Remove this usage of "Nested", it is annotated with @VisibleForTesting and should not be accessed from production code.}}

    Outer outer = null; // Noncompliant {{Remove this usage of "Outer", it is annotated with @VisibleForTesting and should not be accessed from production code.}}

    String foo = new MyObj().bar; // False negative MyObj and Service are in the same file but if 'bar' is private it wouldn't be visible here)

    String bar = new MyObj().bar;

    return new MyObject().foo; // Already reported in line 21

  }

  public int g(@Deprecated int param) {
    MyObject myObject = new MyObject();

    myObject.answer(123); // Compliant, no annotation
    myObject.answer(param); // Compliant, no annotation

    return myObject.answer(); // Noncompliant {{Remove this usage of "answer", it is annotated with @VisibleForTesting and should not be accessed from production code.}}
  }

}

class MyObj {
  @VisibleForTesting
  String bar;
}


@VisibleForTesting
class TestOnly {

}


