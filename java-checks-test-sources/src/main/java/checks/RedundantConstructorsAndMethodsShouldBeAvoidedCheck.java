package checks;

import java.util.Locale;

public class RedundantConstructorsAndMethodsShouldBeAvoidedCheck {
  record RedundantConstructorAndGetter(String name, int age) {
    RedundantConstructorAndGetter(String name, int age) { // Noncompliant, already autogenerated
      System.out.println("Just printing something...");
      this.name = name;
      int x = 42;
      this.age = age;
    }

    public String name() { // Noncompliant
      return this.name;
    }

    public int age() { // Noncompliant
      return age;
    }
  }

  record CompliantConstructorIgnoringParameter(String name, int age) {
    CompliantConstructorIgnoringParameter(String name, int age) { // Compliant
      this.name = name;
      this.age = 42;
    }
  }

  record CompliantConstructorNotAssigningToComponent(String name, int age) {
    CompliantConstructorNotAssigningToComponent(String name, int age) { // Compliant
      this.name = "A";
      this.age = age;
    }

    CompliantConstructorNotAssigningToComponent(int age) {
      this("ignored", 42);
    }
  }

  record EmptyConstructorAndRedundantGetter(String name, int age) {
    EmptyConstructorAndRedundantGetter { // Noncompliant, no need for empty compact constructor
    }
    public String name() { // Noncompliant, already autogenerated
      return name;
    }
  }

  record Compliant(String name, int age) { } // Compliant

  record CompliantConstructorWithAddedValue(String name, int age) {
    CompliantConstructorWithAddedValue(String name, int age) { // Compliant
      this.name = name.toLowerCase(Locale.ROOT);
      this.age = age;
    }
  }

  record CompliantConstructorComplementAndTransformativeGetter(String name, int age) {
    CompliantConstructorComplementAndTransformativeGetter { // Compliant
      if (age < 0) {
        throw new IllegalArgumentException("Negative age");
      }
    }

    public String name() { // Compliant
      return name.toUpperCase(Locale.ROOT);
    }
  }
}
