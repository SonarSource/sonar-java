class A {
  void foo() {
    outer:
    for (int i = 0; i < 10; i++) {
      continue outer;
    }
    for (int i = 0; i < 10; i++) {
      break outer;
    }
    label2: // Noncompliant [[sc=5;ec=11]] {{Remove this unused label.}}
    for (int i = 0; i < 10; i++) {
      break;
    }
  }
}
