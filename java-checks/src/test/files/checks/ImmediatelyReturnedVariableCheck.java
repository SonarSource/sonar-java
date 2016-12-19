class A{

  public long computeDurationInMilliseconds() {
    long duration = (((hours * 60) + minutes) * 60 + seconds ) * 1000 ; // Noncompliant {{Immediately return this expression instead of assigning it to the temporary variable "duration".}}
    return duration;
  }

  public void doSomething() {
    RuntimeException myException = new RuntimeException(); // Noncompliant {{Immediately throw this expression instead of assigning it to the temporary variable "myException".}}
    throw myException;
  }

  public long computeDurationInMilliseconds() {
    return (((hours * 60) + minutes) * 60 + seconds ) * 1000 ;
  }

  public void doSomething() {
    throw new RuntimeException();
  }

  public long computeDurationInMilliseconds() {
    long duration = (((hours * 60) + minutes) * 60 + seconds ) * 1000 ;
    duration = duration - 12;
    return duration;
  }

  public void doSomething() {
    RuntimeException myException = new RuntimeException();
    System.out.println(myException.getMessage());
    throw myException;
  }

  private String toto;
  public String getToto(){
    return toto;
  }

  long foo() {
    long duration = computeDurationInMilliseconds();
    return computeDurationInMilliseconds();
  }

  long bar() {
    long start = computeDurationInMilliseconds();
    long duration = computeDurationInMilliseconds();
    return start;
  }

  void voidMethod() {
    long duration = computeDurationInMilliseconds();
    return;
  }

  void voidMethod() {
    long duration = computeDurationInMilliseconds();
    long duration2 = computeDurationInMilliseconds();
  }

  Object SuppressWarnings() {
    @SuppressWarnings("unchecked")
    Object a = myMethod(); // compliant, the variable is annotated.
    return a;
  }

}
