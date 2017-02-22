public class MyClass {
  MyClass() {
    synchronized (getClass()) { // Noncompliant [[sc=19;ec=29]] {{Synchronize on the static class name instead.}}
    }
  }
  public void methodInvocationSynchronizedExpr() {
    synchronized (getClass()) { // Noncompliant [[sc=19;ec=29]] {{Synchronize on the static class name instead.}}
    }
  }

  public void memberSelectSynchronizedExpr() {
    synchronized (this.getClass()) { // Noncompliant [[sc=19;ec=34]] {{Synchronize on the static class name instead.}}
    }
  }
}


public final class FinalClassIsCompliant {

  FinalClassIsCompliant() {
    synchronized (getClass()) { // Compliant
    }
  }

  public void doSomethingSynchronized() {
    synchronized (this.getClass()) { // Compliant
    }
  }

  class MemberSelect {
    public void memberSelectOnUnknownSymbol() {
      A a = new A();
      synchronized (a.getClass()) { // Compliant
      }
    }
  }

  class Coverage {
    public void unrelatedSynchronizedExpr() {
      Object monitor;
      synchronized (monitor) { // Compliant

      }
    }
  }
}
