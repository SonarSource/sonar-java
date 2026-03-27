package checks.tests;

public class SwitchAtLeastThreeCasesCheckSample {
  record MyRecord(int x, int y) {}

  static void recordSwitch1(Object object) {
    switch (object) { // Compliant
      case String x when x.length() > 42 -> { }
      default -> { }
    }
  }

  static void recordSwitch2(Object object) {
    switch (object) { // Compliant
      case MyRecord(int x, int y) -> { }
      default -> { }
    }
  }

  public interface SealedClass {
    sealed interface Shape permits Box, Circle {}
    record Box() implements Shape { }
    record Circle() implements Shape {}

    default void foo(Shape shape) {
      switch (shape) { // Compliant because of type pattern matching
        case Box ignored -> { }
        case Circle ignored -> System.out.println();
      }
    }

    default void goo(Shape shape) {
      switch (shape) { // Compliant because of type pattern matching
        case Box ignored -> { }
        default -> System.out.println();
      }
    }
  }

  private static void doSomething() {}
  private static void doSomethingElse() {}

  public void f(int variable) {
    switch (variable) { // Noncompliant {{Replace this "switch" statement by "if" statements to increase readability.}}
//  ^^^^^^
      case 0:
        doSomething();
        break;
      default:
        doSomethingElse();
        break;
    }

    switch (variable) {
      case 0:
      case 1:
        doSomething();
        break;
      default:
        doSomethingElse();
        break;
    }

    switch (variable) {
      case 0, 1:
        doSomething();
        break;
      default:
        doSomethingElse();
        break;
    }

    switch (variable) { // Noncompliant
    }

    if (variable == 0) {
      doSomething();
    } else {
      doSomethingElse();
    }
  }

  public enum SmallEnum {
    ONE,
    TWO
  }

  public void switchOverSmallEnum1(SmallEnum smallEnum) {
    switch (smallEnum) {
      case ONE -> {
        System.out.println("1");
      }
      case TWO -> {
        System.out.println("2");
      }
    }
  }

  public int switchOverSmallEnum2(SmallEnum smallEnum) {
    int ret = -1;
    switch (smallEnum) {
      case ONE:
        ret = 1;
        break;
      case TWO:
        ret = 2;
        break;
    }
    return ret;
  }

  public int switchOverSmallEnum3(SmallEnum smallEnum) {
    int ret = -1;
    // This example is not compliant due to the presence of a default case.
    switch (smallEnum) { // Noncompliant
      case ONE:
        ret = 1;
        break;
      default:
        ret = 2;
        break;
    }
    return ret;
  }

  public enum LargeEnum {
    MONDAY, TUESDAY, WEDNESDAY, THURSDAY, FRIDAY, SATURDAY, SUNDAY
  };

  public String switchOverLargeEnum(LargeEnum largeEnum) {
    String mood = "excited";
    switch (largeEnum) { // Noncompliant
      case FRIDAY:
        mood = "love";
        break;
    }
    return mood;
  }

  public enum SmallEnumWithMethods {
    PUBLIC("pub"),
    PRIVATE("prv");

    enum Weird {A, B, C}

    private final String label;

    SmallEnumWithMethods(String label) {
      this.label = label;
    }

    public String getLabel() {
      return label;
    }

    public int spaceRequired() {
      return label.length();
    }
  }

  public int switchSmallEnumWithMethods1(SmallEnumWithMethods largeEnum) {
    int ret = -1;
    switch (largeEnum) { // Compliant
      case PRIVATE:
        ret = 1;
        break;
      case PUBLIC:
        ret = 2;
        break;
    }
    return ret;
  }

  public void switchSmallEnumWithMethods2(SmallEnumWithMethods largeEnum) {
    switch (largeEnum) { // Compliant
      case PRIVATE, PUBLIC:
        System.out.println("Same, Same");
    }
  }
}
