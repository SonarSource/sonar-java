package checks;

interface InnerClassInInterfaceCheckSample {

  void doSomething();

  class DefaultImpl implements InnerClassInInterfaceCheckSample { // Noncompliant {{Move this class out of this interface.}}
//      ^^^^^^^^^^^
    @Override
    public void doSomething() {
    }
  }

  class Empty {} // Noncompliant
//      ^^^^^

  static class ExplicitlyStatic {} // Noncompliant
//             ^^^^^^^^^^^^^^^^

  abstract class AbstractHelper {} // Compliant - abstract classes are contract-like

  interface NestedContract { // Compliant - nested interface
    void process();
  }

  enum Status { // Compliant - enums for contract constants
    ACTIVE, INACTIVE
  }

  record Payload(String data, int size) {} // Compliant - record as data carrier

  @interface Marker {} // Compliant - annotation type

  interface InnerInterface {
    class InnerInNested {} // Noncompliant
//        ^^^^^^^^^^^^^
  }
}

interface ExtendedInterface extends Comparable<ExtendedInterface> {
  class Helper { // Noncompliant
//      ^^^^^^
    public int compare(ExtendedInterface a, ExtendedInterface b) {
      return 0;
    }
  }

  class First {} // Noncompliant
//      ^^^^^
  class Second {} // Noncompliant
//      ^^^^^^
}

class TopLevelClass { // Compliant - not inside an interface
  class InnerClass {} // Compliant - class inside class

  interface InnerInterface {} // Compliant - interface inside class
}

enum TopLevelEnum {
  VALUE;
  class Helper {} // Compliant - class inside enum
}

interface WithDefaultMethod {
  default Runnable createAction() {
    return new Runnable() { // Compliant - anonymous class
      @Override
      public void run() {
      }
    };
  }
}

interface WithNestedClassHierarchy {
  class Outer { // Noncompliant
//      ^^^^^
    class Inner {} // Compliant - parent is a class, not an interface
  }
}
