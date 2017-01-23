
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

  void lambdas() {
    synchronized (mon1) {
      synchronized (mon2) {
        new Thread(() -> wait()).run(); // Compliant
      }
    }
  }
}
