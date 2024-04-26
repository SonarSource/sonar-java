package checks;

import java.time.DayOfWeek;

class SwitchLastCaseIsDefaultCheckSample {
  void foo(MyEnum myEnum, DayOfWeek dow) {
    
    switch (myEnum) { // Compliant
      case A, B:
        break;
      case C:
        break;
    }
    
    switch (0) { // Noncompliant {{Add a default case to this switch.}}
//  ^^^^^^
    }

    switch (0) { // Noncompliant
      case 0:
    }

    switch (0) { // Compliant
      default:
    }

    switch (myEnum) { // Noncompliant {{Complete cases by adding the missing enum constants or add a default case to this switch.}}
      case A:
        break;
      case B:
        break;
    }

    switch (myEnum) { // Compliant
      case A:
      case B:
      case C:
        break;
    }

    switch (myEnum) { // Compliant
      case A:
      case B:
        break;
      default:
        break;
    }

    switch (dow) { // Noncompliant {{Complete cases by adding the missing enum constants or add a default case to this switch.}}
      case FRIDAY:
        break;
      case MONDAY:
        break;
    }
  }

  sealed interface Animal permits Cat, Dog, Phoenix {}
  record Cat() implements Animal { }
  record Dog() implements Animal { }
  record Phoenix() implements Animal { }

  static void sealedClass(Animal animal) {
    switch (animal) { // Compliant
      case Cat ignored -> { }
      case Dog ignored -> { }
      case Phoenix ignored -> { }
    }
  }

  static void incompleteSealedClass(Animal animal) {
    switch (animal) { // Compliant
      case Cat ignored -> { }
      default -> { }
    }
  }

  static void typePattern(Object object) {
    switch (object) { // Compliant
      case String s -> { }
      case Number n -> { }
      case Object o -> { } // alternative to default
    }
  }

  static void typePatternWithDefault(CharSequence cs) {
    switch (cs) { // Compliant
      case String s -> { }
      case CharSequence c -> { } // type of the expression
    }
  }

  static void typePatternWithDefault(Object object) {
    switch (object) { // Compliant
      case String s -> { }
      case Number n -> { }
      default -> { }
    }
  }

  record MyRecord(int x, int y) { }

  static void recordSwitch1(MyRecord object) {
    switch (object) { // Compliant
      case MyRecord(int x, int y) when x > 42 -> { }
      case MyRecord(int x, int y) -> { }
    }
  }
}

enum MyEnum {
  A, B, C;

  MyEnum field;
}
