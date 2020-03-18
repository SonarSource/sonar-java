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

  static class InnerClass1 extends GenericClass { } // Noncompliant [[sc=36;ec=48]] {{Provide the parametrized type for this generic.}}
  static class InnerClass2 implements GenericInterface { } // Noncompliant [[sc=39;ec=55]] {{Provide the parametrized type for this generic.}}
  enum InnerEnum implements GenericInterface { } // Noncompliant [[sc=29;ec=45]]
}
