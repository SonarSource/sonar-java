class A {
  private String color = "red";
  private Object lockObj = new Object();
  private A a = new A();
  private B b = new B();


  private void doSomething() {
    synchronized (color) { // Noncompliant {{Don't synchronize on "color" or remove its reassignment on line 10.}}
      color = "green";
    }

    synchronized (lockObj) { // Compliant
      color = "green";
    }

    synchronized (lockObj) { // Compliant
      String[] array = new String[4];
      array[0] = color;
    }

    synchronized (b) { // Noncompliant {{Don't synchronize on "b" or remove its reassignment on line 23.}}
      b = new B();
    }

    synchronized (this.color) { // Noncompliant {{Don't synchronize on "color" or remove its reassignment on line 27.}}
      color = "green";
    }

    synchronized (color) { // Noncompliant {{Don't synchronize on "color" or remove its reassignment on line 31.}}
      this.color = "green";
    }

    synchronized (this.b.val) { // Noncompliant {{Don't synchronize on "val" or remove its reassignment on line 35.}}
      b.val = foo();
    }

    synchronized (b.val) { // Noncompliant {{Don't synchronize on "val" or remove its reassignment on line 39.}}
      this.b.val = foo();
    }

    synchronized (a.b.val) { // Noncompliant {{Don't synchronize on "val" or remove its reassignment on line 43.}}
      this.a.b.val = foo();
    }

    synchronized (this.a.b.val) { // Noncompliant {{Don't synchronize on "val" or remove its reassignment on line 47.}}
      a.b.val = foo();
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

    synchronized (a.getB().val) { // Compliant
      a.getB().val = foo();
    }

    synchronized (this.thing) { // Compliant - thing is unknown
      b = new B();
    }

    synchronized (a.b.thing) { // Compliant - thing is unknown
      a.b.thing = "test";
    }
  }

  Integer foo() {
    return Integer.valueOf(0);
  }

  B getB() {
    return b;
  }
}

class B {
  public Integer val = 0;

  public Integer foo() {
    return Integer.valueOf(0);
  }
}