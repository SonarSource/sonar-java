package symbolicexecution.checks;

class BooleanWrapper {

  boolean b, field;

  public void foo1(Boolean b) {
    if (b == Boolean.TRUE)
      doSomething();
    else if (Boolean.FALSE.equals(b))
      doSomethingElse();
  }

  public void foo2(Boolean b) {
    if (Boolean.TRUE == b)
      doSomething();
    else if (Boolean.FALSE.equals(b))
      doSomethingElse();
  }

  public void foo3(Boolean b) {
    if (Boolean.TRUE == b)
      doSomething();
    else if (Boolean.FALSE == b)
      doSomethingElse();
  }

  public void foo4(Boolean b) {
    if (b.equals(Boolean.TRUE))
      doSomething();
    else if (b.equals(Boolean.FALSE))
      doSomethingElse();
  }

  public void foo5(boolean a) {
    boolean b = a == false;
    if (Boolean.TRUE == b)
      doSomething();
    else if (Boolean.FALSE == b) // Noncompliant
      doSomethingElse();
  }

  public void foo6(boolean a) {
    b = a == false;
    if (Boolean.TRUE == b)
      doSomething();
    else if (Boolean.FALSE == b) // Noncompliant
      doSomethingElse();
  }

  public void foo7(Boolean b) {
    if (Boolean.TRUE == b)
      doSomething();
    else if (Boolean.FALSE == b)
      doSomethingElse();
  }

  void foo8(boolean a) {
    boolean b = a == false;
    if (b == false) {
      if (b) { // Noncompliant
      }
    }
  }

  public void foo9() {
    if (field == false && field == true) { // Noncompliant {{Remove this expression which always evaluates to "false"}}
    }
    if (field == false || field == true) { // Noncompliant {{Remove this expression which always evaluates to "true"}}
    }
  }

  public void foo10(boolean b) {
    if (Boolean.TRUE == b)
      doSomething();
    else if (Boolean.FALSE == b) // Noncompliant
      doSomethingElse();
  }

  void test3(Boolean condition) {
    if (condition == null) {
    } else if (Boolean.FALSE.equals(condition)) {
    } else if (Boolean.TRUE.equals(condition)) { // Noncompliant
    }
  }

  private void doSomething() {
  }

  private void doSomethingElse() {
  }
}
