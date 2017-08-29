class A {
  private String color = "red";
  private Object lockObj = new Object();
  private A a = new A();
  private B b = new B();


  private void doSomething() {
    synchronized (color) { // Noncompliant [[sc=19;ec=24]] {{"color" is not "private final", and should not be used for synchronization. }}
      color = "green";
    }

    synchronized (lockObj) { // Compliant
      color = "green";
    }

    synchronized (lockObj) { // Compliant
      String[] array = new String[4];
      array[0] = color;
    }

    synchronized (b) { // Noncompliant {{"b" is not "private final", and should not be used for synchronization. }}
      b = new B();
    }

    synchronized (this.color) { // Noncompliant {{"color" is not "private final", and should not be used for synchronization. }}
      color = "green";
    }

    synchronized (color) { // Noncompliant {{"color" is not "private final", and should not be used for synchronization. }}
      this.color = "green";
    }

    synchronized (this.b.val) { // Noncompliant {{"val" is not "private final", and should not be used for synchronization. }}
      b.val = foo();
    }

    synchronized (b.val) { // Noncompliant {{"val" is not "private final", and should not be used for synchronization. }}
      this.b.val = foo();
    }

    synchronized (a.b.val) { // Noncompliant {{"val" is not "private final", and should not be used for synchronization. }}
      this.a.b.val = foo();
    }

    synchronized (this.a.b.val) { // Noncompliant {{"val" is not "private final", and should not be used for synchronization. }}
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
class SyncOnParam {
  void fun(Object param) {
    synchronized (param) { // Noncompliant {{"param" is a method parameter, and should not be used for synchronization.}}
      param = 12;
    }
  }
  void fun1(Object param) {
    Object localVar = null;
    synchronized (localVar) { // compliant local var

    }
    synchronized (unknown) { // compliant : unknown

    }
  }

  void fun2() {
    synchronized (new Object()) { // Noncompliant {{Synchronizing on a new instance is a no-op.}}
      System.out.println("");
    }
  }
}
