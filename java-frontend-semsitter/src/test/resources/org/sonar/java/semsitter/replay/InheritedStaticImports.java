package inherited;

import static inherited.Child.*;

class Parent {
  static int twice(int value) {
    return value * 2;
  }
}

class Child extends Parent {
}

class InheritedStaticImports {
  int four() {
    return twice(2);
  }
}
