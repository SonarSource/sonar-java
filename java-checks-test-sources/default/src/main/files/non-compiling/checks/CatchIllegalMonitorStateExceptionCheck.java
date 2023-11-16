package checks;

import from.some.unknown.source.CompletelyUnknownException;

class CatchIllegalMonitorStateExceptionCheck {
  void myMethod(Object anObject) {
    try {
      anObject.notify();
    } catch(IllegalStateException e) {

    } catch(CompletelyUnknownException e) {
      
    }
  }
}
