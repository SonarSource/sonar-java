class A {
  protected static Object instance = null;

  public static Object getInstance() {
    if (instance != null) {
      return instance;
    }

    instance = new Object();  // Noncompliant {{Synchronize this lazy initialization of 'instance'}}
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


}