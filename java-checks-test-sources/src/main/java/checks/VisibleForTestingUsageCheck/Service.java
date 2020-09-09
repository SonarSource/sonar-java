package checks.VisibleForTestingUsageCheck;

import com.google.common.annotations.VisibleForTesting;

public class Service {

  public String f(int param) {

    TestOnly testOnly = null; // Compliant, TestOnly class visible if it is private

    MyObject.Nested nested = null; // Noncompliant {{Remove this usage of "Nested", it is annotated with @VisibleForTesting and should not be accessed from production code.}}

    Outer outer = null; // Noncompliant {{Remove this usage of "Outer", it is annotated with @VisibleForTesting and should not be accessed from production code.}}

    String foo = new MyObj().bar; // False negative MyObj and Service are in the same file but if 'bar' is private it wouldn't be visible here)

    MyObject myObject = new MyObject(123); // Noncompliant[[sc=29;ec=37;secondary=17,19]]{{Remove this usage of "MyObject", it is annotated with @VisibleForTesting and should not be accessed from production code.}}

    MyObject myObject2 = new MyObject(456); // Already reported in line 17

    return new MyObject().foo; // Noncompliant {{Remove this usage of "foo", it is annotated with @VisibleForTesting and should not be accessed from production code.}}
  }

  public int g(@Deprecated int param) {
    MyObject myObject = new MyObject();

    myObject.answer(123); // Compliant, no annotation

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


