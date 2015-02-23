class A {
  private String color = "red";
  private Object lockObj = new Object();
  private A a = new A();
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

    synchronized (b) { // Noncompliant
      b = new B();
    }

    synchronized (this.color) { // Noncompliant
      color = "green";
    }

    synchronized (color) { // Noncompliant
      this.color = "green";
    }

    synchronized (this.b.val) { // Noncompliant
      b.val = foo();
    }

    synchronized (b.val) { // Noncompliant
      this.b.val = foo();
    }

    synchronized (a.b.val) { // Noncompliant
      this.a.b.val = foo();
    }

    synchronized (this.a.b.val) { // Noncompliant
      a.b.val = foo();
    }

    synchronized (a.b.thing) { // Compliant - thing is unknown
      a.b.thing = "test";
    }

    synchronized (Integer.valueOf(42)) { // Compliant
      color = "green";
    }

    String tmp = "tst";
    synchronized (tmp) { // Compliant
      tmp = "tmp";
    }

    B b2 = new B();
    synchronized (b2.val) { // Compliant
      b2.val = foo();
    }

    synchronized (new A().color) { // Compliant
      color = "yellow";
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