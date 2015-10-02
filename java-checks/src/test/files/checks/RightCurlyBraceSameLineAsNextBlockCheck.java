class Foo {
  public void myMethod() {
    if(something) {
      executeTask();
    } else if (somethingElse) {          // Compliant
      doSomethingElse();
    }
    else {                               // Noncompliant [[sc=5;ec=9]] {{Move this "else" on the same line that the previous closing curly brace.}}
       generateError();
    }
    
    if (something) {
      executeTask();
    }                                   // Compliant
    
    if (something)
      executeTask();
    else {                              // Compliant
      generateError();
    }

    try {
      generateOrder();
    }
    catch (RuntimeException e) {         // Noncompliant {{Move this "catch" on the same line that the previous closing curly brace.}}
      log(e);
    } catch (Exception e) {              // Compliant
      log(e);
    }
    finally {                            // Noncompliant {{Move this "finally" on the same line that the previous closing curly brace.}}
      closeConnection();
    }
    
    try {
      generateOrder();
    }
    catch (Exception e) {              // Noncompliant {{Move this "catch" on the same line that the previous closing curly brace.}}
      log(e);
    }
  }
}
