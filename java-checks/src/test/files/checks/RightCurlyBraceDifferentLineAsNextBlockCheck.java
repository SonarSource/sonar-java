class Foo {
  public void myMethod() {
    if(something) {
      executeTask();
    } else if (somethingElse) { // Noncompliant {{Move this "else" keyword to a new dedicated line.}}
//    ^^^^
      doSomethingElse();
    }
    else {                               // Compliant
       generateError();
    }
    
    if (somethingElse)
      doSomethingElse();
    else {                               // Compliant
       generateError();
    }

    try {
      generateOrder();
    } catch (Exception e) { // Noncompliant {{Move this "catch" keyword to a new dedicated line.}}
      log(e);
    }
    finally {                            // Compliant
      closeConnection();
    }
    
    try {
      generateOrder();
    } 
    catch (Exception e) {                // Compliant
      log(e);
    }

    if (0) {
    } int a;                             // Compliant
  }
}
