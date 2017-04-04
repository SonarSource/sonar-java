class A {
  void foo() {
    if (something) { // Noncompliant [[sc=5;ec=7]] {{Replace this if-then-else statement by a single return statement.}}
      return true;
    } else {
      return false;
    }

    if (something) { // Noncompliant
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

    if (something) // Noncompliant
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
    if (something) // Noncompliant
      return true;
    else
      return false;
  }

  boolean bar() {
    if(something) // Noncompliant
      return true;
    return false;

    if(something) { // Noncompliant
      return true;
    }
    return false;

    if(something) // compliant
      return true;
    System.out.println("");
    return false;

    if(something)
      return true;
  }
}
