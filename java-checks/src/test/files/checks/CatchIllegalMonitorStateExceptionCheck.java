class A {
  void myMethod(Object anObject) {
    try {
      anObject.notify();
    } catch(IllegalMonitorStateException e) { // Noncompliant {{Refactor this piece of code to not catch IllegalMonitorStateException}}
      
    } catch(IllegalStateException e) {
    
    } catch(CompletelyUnknownException e) {
      
    } catch(IllegalStateException|IllegalMonitorStateException e) { // Noncompliant {{Refactor this piece of code to not catch IllegalMonitorStateException}}
      
    }
  }
}
