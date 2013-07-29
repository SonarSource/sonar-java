class A {
  private void f() {
    try {
      return; // Compliant
    } catch (Exception e) {
      return; // Compliant
    } finally {
      return; // Non-Compliant
    }

    try {
    } finally {
      new Foo() {
        public void foo() {
          return; // Compliant
        }
      };
    }

    try {
      return; // Compliant
    } finally {

    }
  }
}

enum A {
  A;

  {
    return;
  }

}
