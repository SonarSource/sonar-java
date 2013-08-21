class Foo {
  void foo() {

    int a;
    for (a = 0; a < 42; a++) {
      a = 0;                                // Compliant - limitation
    }

    for (int b = 0; b < 42; b++) {
      b = 0;                                //  Non-Compliant
    }

    for (String c: "aaaa".split("a")) {
      c = "";                               // Compliant
    }

    for (int d = 0, e = 0; d < 42; d++) {
      d = 0;                                // Non-Compliant
      e = 0;                                // Non-Compliant
    }

    int g;
    for (int f = 0; f < 42; f++) {
      f = 0;                                // Non-Compliant
      g = 0;                                // Compliant
      for (int g = 0; g < 42; g++) {
        g = 0;                              // Non-Compliant
        f = 0;                              // Non-Compliant
      }
      f = 0;                                // Non-Compliant
      g = 0;                                // Compliant
    }

    g = 0;                                  // Compliant

    for (int h = 0; h < 42; h++) {
      h =                                   // Non-Compliant
          h =                               // Non-Compliant
              0;
    }

    g++;                                    // Compliant
    ++g;                                    // Compliant

    for (int i = 0; 0 < 42; i++) {
      i++;                                  // Non-Compliant
      ++i;                                  // Non-Compliant
      --i;                                  // Non-Compliant
      i--;                                  // Non-Compliant
    }

    for (int j = 0; j < 42; j++) {          // Compliant
      for (int k = 0; j++ < 42; k++) {      // Non-Compliant
      }
    }

    for (int i = 0; i < 42; i++) {
      (int)i;                               // Compliant
    }

    for (int i = 0; i < 10; i++) {
      for (int k = 0; k < 20; i++) {       // Non-Compliant
        System.out.println("Hello");
      }
    }

  }
}

class MyGenerics {
  private T foo;

  private <T> MyGenerics() {
    T foo;                                  // Compliant
  }

  public <T> T setFoo(T foo) {              // Compliant
    this.foo = foo;
  }
}

