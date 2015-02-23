class A {
  private String color = "red";
  private Object lockObj = new Object();
  private B b = new B();

  private void doSomething() {
    synchronized (color) { // Noncompliant
      color = "green";
    }

    synchronized (lockObj) { // Compliant
      color = "green";
    }

    synchronized (lockObj) { // Compliant
      String[] array = new String[4];
      array[0] = color;
    }

    synchronized (b) { // Noncompliant for object field reassigned (l.21) and whole object reassigned (l.23)
      b.val = 1;
      b.foo();
      b = new B();
    }

    synchronized (this.color) { // Noncompliant
      color = "green";
    }

    synchronized (color) { // Noncompliant
      this.color = "green";
    }

    synchronized (this.b.val) { // Noncompliant because of whole object b reassigned
      b = new B();
    }

    synchronized (Integer.valueOf(42)) { // Compliant
      color = "green";
    }

    synchronized (this.thing) { // Compliant - thing is unknown
      b = new B();
    }
  }

  Integer foo() {
    return Integer.valueOf(0);
  }
}

class B {
  public Integer val = 0;

  public Integer foo() {
    return Integer.valueOf(0);
  }
}