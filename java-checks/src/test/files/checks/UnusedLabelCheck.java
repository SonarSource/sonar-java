class A {
  void foo() {
    outer:
    for (int i = 0; i < 10; i++) {
      continue outer;
    }
    for (int i = 0; i < 10; i++) {
      break outer;
    }
    label2:
    for (int i = 0; i < 10; i++) {
      break;
    }
  }
}