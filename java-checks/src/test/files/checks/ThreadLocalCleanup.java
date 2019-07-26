package test;

public class UserSession {

  private static final ThreadLocal<UserSession> DELEGATE = new ThreadLocal<>(); // Noncompliant {{Call the "remove()" on "DELEGATE" in a "finally" block.}}

  public ThreadLocalCleanup get() {
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

  private static final ThreadLocal<UserSession> DELEGATE = new ThreadLocal<>();

  public ThreadLocalCleanup get() {
    UserSession session = DELEGATE.get();
    if (session != null) {
      return session;
    }
    throw new Exception("User is not authenticated");
  }

  public void set(UserSession session) {
    DELEGATE.set(session);
  }

  public void remove() {
    DELEGATE.remove();
  }
}


class AnonymousSubclass {

  private static final ThreadLocal<char[]> DEST_TL = new ThreadLocal<char[]>() {  // Noncompliant
    @Override
    protected char[] initialValue() {
      return new char[1024];
    }
  };
}
