package checks;

public class ForLoopFalseConditionCheck {
  void myMethod(int x, int y, int z) {

    for(int i = 0; i < ZERO; i++) {}  // Noncompliant
    for(int i = 1; i < 0 + 1; i++) {}  // Noncompliant
    for(int i = 10; i < 0 + 1 + 1; i++) {}  // Noncompliant
    for(int i = 0; i < 0 + 1 - 1; i++) {}  // Noncompliant
    for(int i = 12; i > 1 + 2 * 5; i++) {}  // Compliant
    for(int i = 12; i > (1 + 2) * 5; i++) {}  // Noncompliant
    for(int i = 2; i > (1 + 5) / 5; i++) {}  // Compliant
    for(int i = 2; i > 1 + 5 / 5; i++) {}  // Noncompliant


    int j = 0, k = 0;
    for (int i = x; true; ) {break;}

    for (int i = x; true; ) {break;}


    for (int i = 1; i < 5; ) {}
    for (int i = 9; i < 5; ) {} // Noncompliant
    for (int i = 9; i > 5; ) {}
    for (int i = 1; i > 5; ) {} // Noncompliant
    for (int i = 1; i <=5; ) {}
    for (int i = 9; i <=5; ) {} // Noncompliant
    for (int i = 9; i >=5; ) {}
    for (int i = 1; i >=5; ) {} // Noncompliant
    for (int i = x; i < 5; ) {}
    for (int i = 1; i < x; ) {}
    for (int i = 1; i <-x; ) {}
    for (         ; j < 5; ) {}
    for (    j = 9; j < 5; ) {} // Noncompliant
    for (   x += 1; j < 5; ) {}

    for (int i = 1;      ; ) {break;}
    for (int i = 0; i < 0x10; ) {}
    for (int i = 0; i < 0b10; ) {}
    for (int i = 1; i <= 0Xffff; i++) {}
  }

  static final int ZERO = 0;
  void foo() {
    for(int i = 0; i < 0; i++) {}  // Noncompliant

  }
}
