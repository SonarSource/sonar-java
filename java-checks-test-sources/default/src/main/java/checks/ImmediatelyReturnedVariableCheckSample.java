package checks;

class ImmediatelyReturnedVariableCheckSample {

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

  public long testQuickFixes(long hours, long minutes, long seconds) {
    long duration = (((hours * 60) + minutes) * 60 + seconds ) * 1000; // Noncompliant [[sc=21;ec=70;quickfixes=qf1]]
    return duration;
    // fix@qf1 {{Inline expression}}
    // edit@qf1 [[sc=5;ec=21]] {{return }}
    // edit@qf1 [[sc=71;el=+1;ec=21]] {{}}
  }

  public long testQuickFixesWithSpaces(long hours, long minutes, long seconds) {
    long duration = (((hours * 60) + minutes) * 60 + seconds ) * 1000; // Noncompliant [[sc=21;ec=70;quickfixes=qf2]]


    return duration;
    // fix@qf2 {{Inline expression}}
    // edit@qf2 [[sc=5;ec=21]] {{return }}
    // edit@qf2 [[sc=71;el=+3;ec=21]] {{}}
  }

  public long testQuickFixesThrow() {
    RuntimeException myException = new RuntimeException(); // Noncompliant [[sc=36;ec=58;quickfixes=qf3]]
    throw myException;
    // fix@qf3 {{Inline expression}}
    // edit@qf3 [[sc=5;ec=36]] {{throw }}
    // edit@qf3 [[sc=59;el=+1;ec=23]] {{}}
  }

  public long testQuickFixesWithFinalVariable(long hours) {
    final long duration = hours * 60; // Noncompliant [[sc=27;ec=37;quickfixes=qf4]]
    return duration;
    // fix@qf4 {{Inline expression}}
    // edit@qf4 [[sc=5;ec=27]] {{return }}
    // edit@qf4 [[sc=38;el=+1;ec=21]] {{}}
  }

  public long testQuickFixesInitOnNextLine(long hours) {
    final long duration =
      hours * 60; // Noncompliant [[sc=7;ec=17;quickfixes=qf5]]
    return duration;
    // fix@qf5 {{Inline expression}}
    // edit@qf5 [[sl=-1;sc=5;ec=7]] {{return }}
    // edit@qf5 [[sc=18;el=+1;ec=21]] {{}}
  }

  private Object myMethod() {
    return null;
  }

}
