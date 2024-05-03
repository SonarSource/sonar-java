package checks;

class UserSession {

  private static final ThreadLocal<UserSession> DELEGATE = new ThreadLocal<>(); // Noncompliant {{Call "remove()" on "DELEGATE".}}
  static final ThreadLocal<UserSession> NOT_PRIVATE = new ThreadLocal<>(); // Compliant, because not private

  public UserSession get() throws Exception {
    UserSession session = DELEGATE.get();
    if (session != null) {
      return session;
    }
    throw new Exception("User is not authenticated");
  }

  public void set(UserSession session) {
    DELEGATE.set(session);
  }

  public void incorrectCleanup() {
    DELEGATE.set(null); // Noncompliant {{Use "remove()" instead of "set(null)".}} [[quickfixes=qf1]]
//  ^^^^^^^^^^^^^^^^^^
    // fix@qf1 {{Replace with "remove()"}}
    // edit@qf1 [[sc=14;ec=23]] {{remove()}}

    this.DELEGATE.set(null); // Noncompliant [[quickfixes=qf2]]
//  ^^^^^^^^^^^^^^^^^^^^^^^
    // fix@qf2 {{Replace with "remove()"}}
    // edit@qf2 [[sc=19;ec=28]] {{remove()}}
  }

}


class UserSessionOk {

  private static final ThreadLocal<UserSessionOk> DELEGATE = new ThreadLocal<>();

  public UserSessionOk get() throws Exception {
    UserSessionOk session = DELEGATE.get();
    if (session != null) {
      return session;
    }
    throw new Exception("User is not authenticated");
  }

  public void set(UserSessionOk session) {
    DELEGATE.set(session);
  }

  public void remove() {
    DELEGATE.remove();
  }
}

class UserSessionClosed {
  private static final ThreadLocal<String> DELEGATE = new ThreadLocal<>(); // Compliant

  void clean() {
    UserSessionClosed.DELEGATE.remove();
  }
}

class UserSessionClosed2 {
  private static final ThreadLocal<String> DELEGATE = new ThreadLocal<>(); // Compliant

  void clean() {
    this.DELEGATE.remove();
  }
}

class AnonymousSubclass {

  private static ThreadLocal<char[]> DEST_TL = new ThreadLocal<char[]>() { // Noncompliant
    private final ThreadLocal<String> delegate = new ThreadLocal<>(); // Compliant, inside a class subtype of ThreadLocal
    @Override
    protected char[] initialValue() {
      return new char[1024];
    }

    void foo() {
      delegate.set("1");
      set(new char[] {'1'});
    }
  };

  void bar() {
    DEST_TL = null;
    this.DEST_TL = null;
  }
}

class ThreadLocalCleanupExtends extends ThreadLocal {
  private static final ThreadLocal<UserSession> DELEGATE = new ThreadLocal<>(); // Compliant, extends ThreadLocal

  public void quickFixInThreadLocal() {
    set(null); // Noncompliant [[quickfixes=qf3]]
//  ^^^^^^^^^
    // fix@qf3 {{Replace with "remove()"}}
    // edit@qf3 [[sc=5;ec=14]] {{remove()}}
  }

}

