package checks;

class ImmediatelyReturnedVariableCheck {

  public long computeDurationInMilliseconds(long hours, long minutes, long seconds) {
    long duration = (((hours * 60) + minutes) * 60 + seconds ) * 1000 ; // Noncompliant {{Immediately return this expression instead of assigning it to the temporary variable "duration".}}
    return duration;
  }

  public void doSomething() {
    RuntimeException myException = new RuntimeException(); // Noncompliant {{Immediately throw this expression instead of assigning it to the temporary variable "myException".}}
    throw myException;
  }

  public long computeDurationInMilliseconds2(long hours, long minutes, long seconds) {
    return (((hours * 60) + minutes) * 60 + seconds ) * 1000 ;
  }

  public void doSomething2() {
    throw new RuntimeException();
  }

  public long computeDurationInMilliseconds3(long hours, long minutes, long seconds) {
    long duration = (((hours * 60) + minutes) * 60 + seconds ) * 1000 ;
    duration = duration - 12;
    return duration;
  }

  public void doSomething3() {
    RuntimeException myException = new RuntimeException();
    System.out.println(myException.getMessage());
    throw myException;
  }

  private String toto;
  public String getToto(){
    return toto;
  }

  long foo() {
    long duration = computeDurationInMilliseconds(1,2,3);
    return computeDurationInMilliseconds(1,2,3);
  }

  long bar() {
    long start = computeDurationInMilliseconds(1,2,3);
    long duration = computeDurationInMilliseconds(1,2,3);
    return start;
  }

  void voidMethod() {
    long duration = computeDurationInMilliseconds(1,2,3);
    return;
  }

  void voidMethod2() {
    long duration = computeDurationInMilliseconds(1,2,3);
    long duration2 = computeDurationInMilliseconds(1,2,3);
  }

  Object SuppressWarnings() {
    @SuppressWarnings("unchecked")
    Object a = myMethod(); // compliant, the variable is annotated.
    return a;
  }

  private Object myMethod() {
    return null;
  }

}
