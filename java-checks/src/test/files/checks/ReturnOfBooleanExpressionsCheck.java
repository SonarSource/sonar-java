class A {
  void foo() {
    if (something) { // Non-Compliant
      return true;
    } else {
      return false;
    }

    if (something) { // Non-Compliant
      return false;
    } else {
      return true;
    }

    if (something) { // Compliant
      return foo;
    } else {
      return false;
    }

    if (something) { // Compliant
      return true;
    } else {
      return foo;
    }

    if (something) // Non-Compliant
      return true;
    else
      return false;

    if (something) { // Compliant
      System.out.println();
    } else {
      return true;
    }

    if (something) { // Compliant
      System.out.println();
      return true;
    } else {
      return false;
    }

    if (something) { // Compliant
      return;
    } else {
      return true;
    }

    if (something) { // Compliant
      return true;
    }

    if (something) { // Compliant
      return foo(true);
    } else {
      return foo(false);
    }

    if (something) { // Compliant
      int foo;
    } else {
      return false;
    }
  }
}
