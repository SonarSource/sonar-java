package checks;

class UserSession {
  private static final ThreadLocal<UnknownSession> DELEGATE = new ThreadLocal<>(); // Compliant, ECJ can not get resolve the call to method "remove"

  public void unload() {
    DELEGATE.remove(); // For some reasons, the type of DELEGATE is known, but the type of remove is unknown.
  }

  private static final ThreadLocalUnknown DELEGATE_UNKNOWN = new ThreadLocal<>(); // Compliant, not "java.lang.ThreadLocal"
}

class ThreadLocalCleanupExtends extends UnknownA {
  private static final ThreadLocal<UserSession> DELEGATE = new ThreadLocal<>(); // Compliant, can not get the type of the parent class, can be a subtype of ThreadLocal
}

