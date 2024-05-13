class MyClass {
  MyClass() {
    synchronized (getClass()) { // Noncompliant {{Synchronize on the static class name instead.}}
//                ^^^^^^^^^^
    }
  }
  public void methodInvocationSynchronizedExpr() {
    synchronized (getClass()) { // Noncompliant {{Synchronize on the static class name instead.}}
//                ^^^^^^^^^^
    }
  }

  public void memberSelectSynchronizedExpr() {
    synchronized (this.getClass()) { // Noncompliant {{Synchronize on the static class name instead.}}
//                ^^^^^^^^^^^^^^^
    }
  }
}

class MyClassWithInitializer {
  {
    synchronized (getClass()) { // Noncompliant {{Synchronize on the static class name instead.}}
//                ^^^^^^^^^^
    }
  }
}

enum MyEnumWithInitializer {
  red, white;

  {
    synchronized (getClass()) { // Compliant - enums are implicitly final
      System.out.println();
    }
  }

}

class MyClassWithStaticInitializer {
  static {
    synchronized (getClass()) { // Noncompliant {{Synchronize on the static class name instead.}}
//                ^^^^^^^^^^
    }
  }
}

class MyClassWithLambda {
  java.util.function.Consumer<String> c = s -> {
    synchronized (getClass()) { // Noncompliant {{Synchronize on the static class name instead.}}
//                ^^^^^^^^^^
    }
  };
}

final class MyFinalClassWithLambda {
  java.util.function.Consumer<String> c = s -> {
    synchronized (getClass()) { // Compliant
    }
  };
}

final class FinalClassIsCompliant {

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
