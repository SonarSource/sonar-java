class ExtendsThread extends Thread {
}

enum Enum {
  ;
  public static final String STRING = "string".intern(); // Compliant, should not fail with NPE
}

interface Interface {
  public static final String STRING = "string".intern(); // Compliant, should not fail with NPE
}

class TestClass1 {

  public static final String STRING = "string".intern(); // Compliant

  public abstract void abstractMethod();

  static {
    new Thread(null).start(); // Compliant
    new ExtendsThread(null).start(); // Compliant
  }

  TestClass1() {
    toString(); // Compliant
    new Thread(null).start(); // Noncompliant [[sc=22;ec=27]] {{Move this "start" call to another method.}}
    new ExtendsThread(null).start(); // Noncompliant {{Move this "start" call to another method.}}
  }

  public void method() {
    new Thread(null).start(); // Compliant
  }
}

final class TestClass2 {
  TestClass2() {
    new Thread(null).start(); // Compliant
    new ExtendsThread(null).start(); // Compliant
  }

  public void method() {
    new Thread(null).start(); // Compliant
  }
}
