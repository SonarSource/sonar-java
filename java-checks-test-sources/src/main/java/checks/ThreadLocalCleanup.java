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
    DELEGATE.set(null); // Noncompliant {{Use "remove()" instead of "set(null)".}}
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

  private static ThreadLocal<char[]> DEST_TL = new ThreadLocal<char[]>() {  // Noncompliant
    @Override
    protected char[] initialValue() {
      return new char[1024];
    }

    void foo() {
      set(new char[] {'1'});
    }
  };

  void bar() {
    DEST_TL = null;
    this.DEST_TL = null;
  }
}

