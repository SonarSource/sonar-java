package checks;

import java.util.Optional;

public class RawTypeCheck {

  void foo() {
    GenericClass v; // Noncompliant [[sc=5;ec=17]] {{Provide the parametrized type for this generic.}}
    v = new GenericClass(); // Noncompliant [[sc=13;ec=25]] {{Provide the parametrized type for this generic.}}
    v = new RawTypeCheck.GenericClass(); // Noncompliant [[sc=26;ec=38]]

    v = new GenericClass<>(); // Compliant
    v = new GenericClass<String>(); // Compliant

    RawTypeCheck t;

    @SuppressWarnings("rawtypes")
    Optional o2; // Noncompliant - should be handled by SONARJAVA-2410 and filtered out
    Optional<String> o1 = Optional.empty(); // Compliant
  }

  GenericClass bar() { return null; } // Noncompliant

  static class GenericClass<T> { }
  interface GenericInterface<T> { }

  abstract static class InnerClass1 extends GenericClass { // Noncompliant [[sc=45;ec=57]] {{Provide the parametrized type for this generic.}}
    abstract GenericClass bar(); // Noncompliant
    abstract void qix(GenericClass gc); // Noncompliant
  }

  static class InnerClass2 extends InnerClass1 {
    @Override GenericClass bar() { return null; } // Compliant - override
    @Override void qix(GenericClass gc) { } // Compliant - override
  }

  static class InnerClass3 implements GenericInterface { } // Noncompliant [[sc=39;ec=55]] {{Provide the parametrized type for this generic.}}
  static class InnerClass4 implements GenericInterface<GenericClass> {} // Noncompliant [[sc=56;ec=68]]
  enum InnerEnum implements GenericInterface { } // Noncompliant [[sc=29;ec=45]]
}
