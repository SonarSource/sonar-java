class Foo {
  public void myMethod() {
    if(something) {
      executeTask();
    } else if (somethingElse) {          // Non-Compliant
      doSomethingElse();
    }
    else {                               // Compliant
       generateError();
    }

    try {
      generateOrder();
    } catch (Exception e) {              // Non-Compliant
      log(e);
    }
    finally {                            // Compliant
      closeConnection();
    }

    if (0) {
    } int a;                             // Compliant
  }
}
