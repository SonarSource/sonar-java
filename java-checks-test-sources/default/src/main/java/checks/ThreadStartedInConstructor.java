package checks;

class ThreadStartedInConstructor {
  public static class ExtendsThread extends Thread {
  }

  enum Enum {
    ;
    public static final String STRING = "string".intern();
  }

  interface Interface {
    String STRING = "string".intern();
  }

  abstract static class TestClass1 {

    public static final String STRING = "string".intern(); // Compliant

    public abstract void abstractMethod();

    static {
      new Thread((Runnable) null).start(); // Compliant
      new ExtendsThread().start(); // Compliant
    }

    TestClass1() {
      toString(); // Compliant
      new Thread((Runnable) null).start(); // Noncompliant {{Move this "start" call to another method.}}
//                                ^^^^^
      new ExtendsThread().start(); // Noncompliant {{Move this "start" call to another method.}}
    }

    public void method() {
      new Thread((Runnable) null).start(); // Compliant
    }
  }

  static final class TestClass2 {
    TestClass2() {
      new Thread((Runnable) null).start(); // Compliant
      new ExtendsThread().start(); // Compliant
    }

    public void method() {
      new Thread((Runnable) null).start(); // Compliant
    }
  }

  record MyRecord() {
    MyRecord {
      new Thread((Runnable) null).start(); // Compliant - records can not be extended, they are implicitly final
    }
  }
}

