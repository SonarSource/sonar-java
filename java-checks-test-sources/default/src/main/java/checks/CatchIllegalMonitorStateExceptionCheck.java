package checks;

class CatchIllegalMonitorStateExceptionCheck {
  void myMethod(Object anObject) {
    try {
      anObject.notify();
    } catch(IllegalMonitorStateException e) { // Noncompliant [[sc=13;ec=41]] {{Refactor this piece of code to not catch IllegalMonitorStateException}}
      
    } catch(IllegalStateException e) {
      
    }
    try {
      anObject.notify();
    } catch(IllegalStateException|IllegalMonitorStateException e) { // Noncompliant {{Refactor this piece of code to not catch IllegalMonitorStateException}}
      
    }
  }
}
