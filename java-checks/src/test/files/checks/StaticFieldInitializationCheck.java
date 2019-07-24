class A {
  protected static Object instance = null;

  public static Object getInstance() {
    if (instance != null) {
      return instance;
    }

    instance = new Object();  // Noncompliant [[sc=5;ec=13]] {{Synchronize this lazy initialization of 'instance'}}
    return instance;
  }

  protected static volatile Object instance2 = null;

  public static Object getInstance2() {
    if (instance2 != null) {
      return instance2;
    }

    instance2 = new Object();
    return instance2;
  }

  protected static Object instance3 = null;

  public static synchronized Object getInstance3() {
    if (instance3 != null) {
      return instance3;
    }

    instance3 = new Object();
    return instance3;
  }
  public static void foo() {
    synchronized(instance) {
      instance3 = new Object();
    }
  }

  protected volatile static Object instance4 = null;

  public static Object getInstance() {
    if (instance4 != null) {
      return instance4;
    }
    foo();
    instance4 = new Object();  // compliant instance 4 is volatile
    unresolvedIdentifier = new Object();
    return instance4;
  }

  protected static int instance5;

  public static int getInstance5() {
    instance5 = 12;  // compliant - primitive type
    return instance5;
  }

  protected static Object instance6 = null;
  public static Object getInstance6() {
    if (instance6 != null) {
      return instance6;
    }

    A.instance6 = new Object();  // FN - not using identifier directly
    return instance6;
  }

  private static final URI FAKE_URI;
  static {
    try {
      FAKE_URI = new URI("tests://unittest");
    } catch (URISyntaxException e) {
      // Can't happen
      throw new IllegalStateException(e);
    }
  }

  private static URI FAKE_URI2;
  static {
    try {
      FAKE_URI2 = new URI("tests://unittest");
    } catch (URISyntaxException e) {
      // Can't happen
      throw new IllegalStateException(e);
    }
  }
  class B {
    static Config CONFIG = null;
    private static java.util.concurrent.locks.Lock LOCK = new java.util.concurrent.locks.ReentrantLock();

    public static void resetConfiguration() {
      try {
        LOCK.lock();
        CONFIG = null; // Compliant
      } finally {
        LOCK.unlock();
      }
    }

    synchronized void foo() {
    }
  }
  class C {
    static Config CONFIG = null;
    private static java.util.concurrent.locks.Lock LOCK = new java.util.concurrent.locks.ReentrantLock();

    {
     CONFIG = null; // Noncompliant
    }

    {
      LOCK.lock();
    }

    public static void resetConfiguration2() {
      try {
        CONFIG = null; // Noncompliant : before the lock
        LOCK.tryLock();
      } finally {
        LOCK.unlock();
      }
    }

    synchronized void foo() {
    }
  }

  class D { // same as C without 'foo()'
    static Config CONFIG = null;
    private static java.util.concurrent.locks.Lock LOCK = new java.util.concurrent.locks.ReentrantLock();

    {
     CONFIG = null; // Compliant - there is no sycnhronized methods in this class
    }

    {
      LOCK.lock();
    }

    public static void resetConfiguration2() {
      try {
        CONFIG = null; // Compliant - there is no sycnhronized methods in this class
        LOCK.tryLock();
      } finally {
        LOCK.unlock();
      }
    }

  }

  static class E {
    static Config CONFIG;

    synchronized void foo() {
      CONFIG = null; // Compliant
    }

    synchronized Consumer<String> bar() {
      return s -> {
        CONFIG = null; // Noncompliant
      };
    }
  }
}
