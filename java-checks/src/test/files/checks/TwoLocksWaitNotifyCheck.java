
class Wait {

  Object mon1;
  Object mon2;

  public Wait() {
    synchronized (this.mon1) {
      System.out.println("Hello");
      synchronized (this.mon2) {
        this.mon2.wait(); // Noncompliant [[sc=9;ec=25;secondary=8,10]] {{Don't use "wait()" here; multiple locks are held.}}
      }
    }
  }

  public synchronized void method() {
    synchronized (this.mon2) {
      this.mon2.wait(); // Noncompliant [[sc=7;ec=23;secondary=16,17]] {{Don't use "wait()" here; multiple locks are held.}}
    }
  }

  public synchronized void waitsWithTimeoutAreOK() {
    synchronized (this) {
      wait(10); // Compliant - wait with timeout is fishy, but perhaps OK
      wait(10, 1000); // Compliant - another kind of wait with timeout
    }
  }
}

class Notify {

  Object mon1;
  Object mon2;

  public void method1() {
    synchronized (this.mon1) {
      System.out.println("Hello");
      synchronized (this.mon2) {
        this.mon2.notify(); // Noncompliant [[sc=9;ec=27;secondary=36,38]] {{Use "notifyAll()" here; multiple locks are held.}}
      }
    }
  }

  synchronized void method2() {
    synchronized (this.mon2) {
      this.mon2.notify(); // Noncompliant [[sc=7;ec=25;secondary=44,45]] {{Use "notifyAll()" here; multiple locks are held.}}
    }
  }

  synchronized void method3() {
    synchronized (this.mon2) {
      this.mon2.notifyAll(); // Compliant
    }
  }

  synchronized void method4() {
    class LocalClass {
      void method() {
        synchronized (this) {
          notify(); // Compliant
        }
      }

      void method() {
        Object obj = new Object();
        synchronized (obj) {
          synchronized (this) {
            notify(); // Noncompliant [[sc=13;ec=21;secondary=66,67]] {{Use "notifyAll()" here; multiple locks are held.}}
          }
        }
      }
    }
  }

  void lambdas() {
    synchronized (mon1) {
      synchronized (mon2) {
        new Thread(() -> wait()).run(); // Compliant
      }
    }
  }

  void methodCompliant() {
    synchronized (this) {
      notify(); // Compliant
    }
  }

  synchronized void methodCompliant2() {
    notify(); // Compliant
  }
}
