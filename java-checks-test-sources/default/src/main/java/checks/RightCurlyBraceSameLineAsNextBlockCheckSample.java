package checks;

class RightCurlyBraceSameLineAsNextBlockCheckSample {
  public void myMethod(boolean something, boolean somethingElse) {
    if(something) {
      executeTask();
    } else if (somethingElse) {          // Compliant
      doSomethingElse();
    }
    else { // Noncompliant [[quickfixes=qf1]] {{Move this "else" on the same line that the previous closing curly brace.}}
//  ^^^^
                                         // fix@qf1 {{Move to the same line as the closing curly brace}}
                                         // edit@qf1 [[sl=-1;sc=6;el=+0;ec=5]] {{ }}
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
    {
      try {
        generateOrder();
      }
      catch (RuntimeException e) { // Noncompliant [[quickfixes=qf2]] {{Move this "catch" on the same line that the previous closing curly brace.}}
//    ^^^^^
                                           // fix@qf2 {{Move to the same line as the closing curly brace}}
                                           // edit@qf2 [[sl=-1;sc=8;el=+0;ec=7]] {{ }}
        log(e);
      } catch (Exception e) {              // Compliant
        log(e);
      }


    finally { // Noncompliant [[quickfixes=qf3]] {{Move this "finally" on the same line that the previous closing curly brace.}}
//  ^^^^^^^
                                         // fix@qf3 {{Move to the same line as the closing curly brace}}
                                         // edit@qf3 [[sl=-3;sc=8;el=+0;ec=5]] {{ }}
      closeConnection();
    }

    }
    
    try {
      generateOrder();
    }
    catch (Exception e) { // Noncompliant {{Move this "catch" on the same line that the previous closing curly brace.}}
      log(e);
    }
  }

  private void closeConnection() {
  }

  private void generateError() {
  }

  private void doSomethingElse() {
  }

  private void generateOrder() {
  }

  private void executeTask() {
  }
  void log(Exception e) {
  }
}
