
class A {

  private void noncompliant() {
    int i = 0;
    for (; i < 5;) { // Noncompliant
//  ^^^
      i++;
    }
  }

  private void compliant() {
    int i = 0;
    for (int j; j < 5;) {
      j++;
    }
    for (int j; j < 5; j++) { }
    for (;; i++) { }
    for (;;) { }
  }

}
