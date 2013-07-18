class Foo {
  public void myMethod() {
    if(something) {
      executeTask();}                   // Non-Compliant
    else if (somethingElse) {
      doSomethingElse();
    }                                   // Compliant

    if (0) {
    ; } else if (0) {                   // Non-Compliant - just once
    } else {                            // Compliant
    }                                   // Compliant

    try {
      generateOrder();
    }                                   // Compliant
    finally { closeConnection();}       // Non-Compliant
  }
}

@Properties({}) // Compliant
class Exceptions {
  int[] numbers = new int[] { 0, 1 };   // Compliant
}

class Foo {
  {
    switch(x) {
      default:
    }
  }
}
