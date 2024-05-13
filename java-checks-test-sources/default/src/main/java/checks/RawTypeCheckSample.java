package checks;

import java.util.Optional;

public class RawTypeCheckSample {

  void foo() {
    GenericClass v; // Noncompliant {{Provide the parametrized type for this generic.}}
//  ^^^^^^^^^^^^
    v = new GenericClass(); // Noncompliant {{Provide the parametrized type for this generic.}}
//          ^^^^^^^^^^^^
    v = new RawTypeCheckSample.GenericClass(); // Noncompliant
//                             ^^^^^^^^^^^^

    v = new GenericClass<>(); // Compliant
    v = new GenericClass<String>(); // Compliant

    RawTypeCheckSample t;

    @SuppressWarnings("rawtypes")
    Optional o2; // Noncompliant
    Optional<String> o1 = Optional.empty(); // Compliant
  }

  GenericClass bar() { return null; } // Noncompliant

  static class GenericClass<T> { }
  interface GenericInterface<T> { }

  abstract static class InnerClass1 extends GenericClass { // Noncompliant {{Provide the parametrized type for this generic.}}
//                                          ^^^^^^^^^^^^
    abstract GenericClass bar(); // Noncompliant
    abstract void qix(GenericClass gc); // Noncompliant
  }

  static class InnerClass2 extends InnerClass1 {
    @Override GenericClass bar() { return null; } // Compliant - override
    @Override void qix(GenericClass gc) { } // Compliant - override
  }

  static class InnerClass3 implements GenericInterface { } // Noncompliant {{Provide the parametrized type for this generic.}}
//                                    ^^^^^^^^^^^^^^^^
  static class InnerClass4 implements GenericInterface<GenericClass> {} // Noncompliant
//                                                     ^^^^^^^^^^^^
  enum InnerEnum implements GenericInterface { } // Noncompliant
//                          ^^^^^^^^^^^^^^^^
}
