class A {
  void myMethod(Object anObject) {
    try {
      anObject.notify();
    } catch(IllegalMonitorStateException e) { // Noncompliant
      
    } catch(IllegalStateException e) {
    
    } catch(CompletelyUnknownException e) {
      
    } catch(IllegalStateException|IllegalMonitorStateException e) { // Noncompliant
      
    }
  }
}
