package checks;

import java.util.Objects;

class FlexibleConstructorBodyValidationCheckSample {

  static class Coffee {
    Coffee() {}
    Coffee(int water, int milk) {}
  }

  static class SmallCoffee extends Coffee {
    private String topping;
    private int maxVolume = 100;

    public SmallCoffee(int water, int milk, String topping) {
      super(water, milk);
      int maxCupCoffee = 90;
      int totalVolume = water + milk;
      if (totalVolume > maxCupCoffee) { // Noncompliant {{Move this validation logic before the super() or this() call.}}
        throw new IllegalArgumentException();
      }
      this.topping = topping;
    }

    public SmallCoffee(int water, int milk) {
      int totalVolume = water + milk;
      int maxCupCoffee = 90;
      if (totalVolume > maxCupCoffee) { // Compliant: validation before super()
        throw new IllegalArgumentException();
      }
      super(water, milk);
    }

    public SmallCoffee(int water) {
      super(water, 0);
      if (water > maxVolume) { // Compliant: Uses instance field
        throw new IllegalArgumentException();
      }
    }

    public SmallCoffee() {
      super(0, 0); // Compliant: no validation
    }
  }

  static class MediumCoffee extends Coffee {
    private String flavor;

    public MediumCoffee(int water, int milk) {
      super(water, milk);
      this.flavor = "Vanilla";
    }

    public MediumCoffee(int water) {
      if (water < 0) { // Compliant: throw before this()
        throw new IllegalArgumentException("Water cannot be negative");
      }
      this(water, 0);
    }

    public MediumCoffee(String flavor) {
      this(100, 50);
      if (!isValidFlavor(flavor)) { // Compliant: validation uses instance method, cannot be moved
        throw new IllegalArgumentException();
      }
      this.flavor = flavor;
    }

    private boolean isValidFlavor(String flavor) {
      return flavor != null && !flavor.isEmpty();
    }
  }

  static class LargeCoffee extends Coffee {
    private String name;
    private static final int MAX_SIZE = 500;

    public LargeCoffee(int water, int milk) {
      super(water, milk);
      if (water + milk > MAX_SIZE) { // Noncompliant
        throw new IllegalArgumentException();
      }
    }

    public LargeCoffee(String name) {
      super(100, 100);
      this.name = name;
      if (name == null) { // Compliant: Not reported - after field assignment
        throw new IllegalArgumentException();
      }
    }
  }

  // Test with different validation libraries
  static class GuavaCoffee extends Coffee {
    public GuavaCoffee(String name) {
      super(100, 50);
      com.google.common.base.Preconditions.checkNotNull(name); // Noncompliant {{Move this validation logic before the super() or this() call.}}
    }
  }

  static class SpringCoffee extends Coffee {
    public SpringCoffee(String name) {
      super(100, 50);
      org.springframework.util.Assert.notNull(name, "Name must not be null"); // Noncompliant {{Move this validation logic before the super() or this() call.}}
    }
  }

  // Test with implicit super()
  static class ImplicitSuperCoffee extends Coffee {
    public ImplicitSuperCoffee(int water) {
      // Implicit super() call here
      if (water < 0) { // Noncompliant
        throw new IllegalArgumentException();
      }
    }
  }

  // Test multiple validations
  static class MultiValidationCoffee extends Coffee {
    public MultiValidationCoffee(int water, int milk, String name) {
      super(water, milk);
      if (water < 0) { // Noncompliant
        throw new IllegalArgumentException("Invalid water");
      }
      if (milk < 0) { // Noncompliant
        throw new IllegalArgumentException("Invalid milk");
      }
      Objects.requireNonNull(name); // Noncompliant
    }
  }

  // Test nested if statements
  static class NestedIfCoffee extends Coffee {
    public NestedIfCoffee(int water, int milk) {
      super(water, milk);
      if (water > 0) { // Noncompliant
        if (milk > 0) {
          if (water + milk > 200) {
            throw new IllegalArgumentException();
          }
        }
      }
    }
  }
}
