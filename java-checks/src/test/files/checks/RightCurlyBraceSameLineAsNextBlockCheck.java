class Foo {
  public void myMethod() {
    if(something) {
      executeTask();
    } else if (somethingElse) {          // Compliant
      doSomethingElse();
    }
    else {                               // Non-Compliant
       generateError();
    }

    try {
      generateOrder();
    }
    catch (RuntimeException e) {         // Non-Compliant
      log(e);
    } catch (Exception e) {              // Compliant
      log(e);
    }
    finally {                            // Non-Compliant
      closeConnection();
    }
  }
}
