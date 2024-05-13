package checks;

import java.util.List;

class CastArithmeticOperandCheck {

  CastArithmeticOperandCheck(int a, long l) {}

  void foo() {
    CastArithmeticOperandCheck a = new CastArithmeticOperandCheck(1 + 2, 1 + 2); // Noncompliant {{Cast one of the operands of this addition operation to a "long".}}
    long l1 = 1000 * 3600 * 24 * 365; // Noncompliant {{Cast one of the operands of this multiplication operation to a "long".}}
//                             ^
    l1 += 10 + 2;
    long l2 = 1000L * 3600 * 24 * 365;
    float f = 4L / 3; // Noncompliant {{Cast one of the operands of this division operation to a "float".}}
    float f1 = 2 / 3; // Noncompliant {{Cast one of the operands of this division operation to a "float".}}
    float f2 = 2f / 3;
    float f3 = 1 / 2 / 2 * 0.5f; // Noncompliant
    l2 = 1000 * 3600 * 24 * 365; // Noncompliant {{Cast one of the operands of this multiplication operation to a "long".}}
    l2 = 1000L * 3600 * 24 * 365; // compliant
    double d = 2 / 3; // Noncompliant {{Cast one of the operands of this division operation to a "double".}}
    long l3 = 2 + Integer.MAX_VALUE; // Noncompliant {{Cast one of the operands of this addition operation to a "long".}}
//              ^
    l3 = 2 - Integer.MIN_VALUE; // Noncompliant {{Cast one of the operands of this subtraction operation to a "long".}}
    longMethod(1 + 2, 1 + 2); // Noncompliant {{Cast one of the operands of this addition operation to a "long".}}
    longMethod(1 + 2, 1 + 2l);  // Compliant
    doubleMethod(1 + 2, 1 + 2); // Noncompliant {{Cast one of the operands of this addition operation to a "double".}}
    doubleMethod(1 + 2, 1 + 2d);// Compliant
    floatMethod(1 + 2, 1 + 2); // Noncompliant {{Cast one of the operands of this addition operation to a "float".}}
    floatMethod(1 + 2, 1 + 2f); // Compliant
    foo(); //Compliant
    double tst = 1 | 2; // Compliant
    longMethod(12 / 7, 12 / 7);   // Compliant dividing two ints into and widening into a long can't cause any harm
    double d2  = 1 / 2 / 2 * 0.5; // Noncompliant
  }


  void longMethod(int a, long l) {}
  void doubleMethod(int a, double d) {}
  void floatMethod(int a, float f) {}

  long l() {
    if (true) {
      return 1 + 2l; // compliant
    } else {
      return 1 + 2; // Noncompliant {{Cast one of the operands of this addition operation to a "long".}}
    }
  }

  double d() {
    if (true) {
      return 1 + 2d; // compliant
    } else {
      return 1 + 2; // Noncompliant {{Cast one of the operands of this addition operation to a "double".}}
    }
  }

  float f() {
    if (true) {
      return 1 + 2f; // compliant
    } else {
      return 1 + 2; // Noncompliant {{Cast one of the operands of this addition operation to a "float".}}
    }
  }

  void test_int_division() {
    double a = 1 / 2 * 5.0; // Noncompliant {{Cast one of the operands of this integer division to a "double".}}
//             ^^^^^
    double b = (1+3) / 5.0; // Compliant
    double c = ((1 + 0) / 2 + 0) / 5.0; // Noncompliant {{Cast one of the operands of this integer division to a "double".}}
//              ^^^^^^^^^^^
  }

  void test_constructors() {
    java.util.Date date1 = new java.util.Date(2 + 1); // Noncompliant {{Cast one of the operands of this addition operation to a "long".}}
    java.util.Date date2 = new java.util.Date("today"); // Compliant
  }

  double test_nested_class(List<Integer> list) {
    long sum = list.stream().mapToLong(i -> {
      return i * i; // Compliant, not a return of the "test_nested_class" scope
    }).sum();
    return sum;
  }

  double test_nested_method() {
    Nested n = new Nested() {
      float f() {
        // Only one issue, no issue for "test_nested_method" scope.
        return 1 + 2; // Noncompliant {{Cast one of the operands of this addition operation to a "float".}}
      }
      int i() {
        return 1 + 2; // Compliant
      }
    };
    return 1.2;
  }

  abstract class Nested { }
}

