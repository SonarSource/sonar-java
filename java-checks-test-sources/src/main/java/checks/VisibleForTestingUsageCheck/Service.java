package checks.VisibleForTestingUsageCheck;

public class Service {

  public String f(int param) {

    String foo = new MyObj().bar; // False negative MyObj and Service are in the same file but if 'bar' is private it wouldn't be visible here)

    MyObject myObject = new MyObject(123); // Noncompliant {{Remove this usage of "MyObject", it is annotated with @VisibleForTesting and should not be accessed from production code.}}

    return new MyObject().foo; // Noncompliant {{Remove this usage of "foo", it is annotated with @VisibleForTesting and should not be accessed from production code.}}
  }

  public int g(int param) {
    MyObject myObject = new MyObject();

    myObject.answer(123); // Compliant, no annotation

    return myObject.answer(); // Noncompliant {{Remove this usage of "answer", it is annotated with @VisibleForTesting and should not be accessed from production code.}}
  }

}

class MyObj {
  @com.google.common.annotations.VisibleForTesting
  String bar;
}


