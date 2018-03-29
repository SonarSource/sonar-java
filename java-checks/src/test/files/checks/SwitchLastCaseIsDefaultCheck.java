import java.time.DayOfWeek;

class Foo {
  void foo(MyEnum myEnum, DayOfWeek dow) {
    switch (0) { // Noncompliant [[sc=5;ec=11]] {{Add a default case to this switch.}}
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
}

enum MyEnum {
  A, B, C;

  MyEnum field;
}
