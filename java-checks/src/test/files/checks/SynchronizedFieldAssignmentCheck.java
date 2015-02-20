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

    synchronized (b) { // Noncompliant because of line 22
      b.val = 1;
      b = new B();
    }

    synchronized (this.color) { // Noncompliant
      color = "green";
    }

    synchronized (color) { // Noncompliant
      this.color = "green";
    }

    synchronized (this.b.val) { // Compliant
      b = new B();
    }
    
    synchronized (this.thing) { // Compliant
      b = new B();
    }

    synchronized (Integer.valueOf(42)) { // Compliant
      color = "green";
    }
  }

  Integer foo() {
    return Integer.valueOf(0);
  }
}

class B {
  public int val = 0;

  public Integer foo() {
    return Integer.valueOf(0);
  }
}
