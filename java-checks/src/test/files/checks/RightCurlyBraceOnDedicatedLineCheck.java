class Foo {
  public void myMethod() {
    if(something) {
      executeTask();}                   // Non-Compliant
    else if (somethingElse) {
      doSomethingElse();
    }                                   // Compliant

    try {
      generateOrder();
    }                                   // Compliant
    finally { closeConnection();}       // Non-Compliant
  }
}
