package checks;

class FinalizerAttackCheckSample {

  // --- Noncompliant: non-final class with throwing constructor ---

  static class SecurityService { // Secondary {{Non-final class}}
    private final String token;

    public SecurityService(String token) throws IllegalArgumentException { // Noncompliant {{Make this class "final" or make this throwing constructor "private".}}
      if (token == null) {
        throw new IllegalArgumentException("Invalid token");
      }
      this.token = token;
    }
  }

  static class AuthProvider { // Secondary {{Non-final class}}
    public AuthProvider(String credentials) throws Exception { // Noncompliant
      if (credentials.isEmpty()) {
        throw new Exception("Bad credentials");
      }
    }
  }

  static class ResourceLoader { // Secondary {{Non-final class}}
    ResourceLoader(String path) { // Noncompliant
      if (path == null) {
        throw new NullPointerException();
      }
    }
  }

  static class MultiConstructorService { // Secondary {{Non-final class}}
    MultiConstructorService(int id) throws Exception { // Noncompliant
      if (id < 0) {
        throw new Exception("Negative id");
      }
    }

    MultiConstructorService(String name) {
    }
  }

  static class ProtectedConstructorService { // Secondary {{Non-final class}}
    protected ProtectedConstructorService(String data) throws Exception { // Noncompliant
      if (data == null) {
        throw new Exception("Null data");
      }
    }
  }

  static class ThrowsClauseOnly { // Secondary {{Non-final class}}
    public ThrowsClauseOnly() throws Exception { // Noncompliant
    }
  }

  // --- Compliant: final class ---

  static final class SecureService {
    public SecureService(String token) throws IllegalArgumentException {
      if (token == null) {
        throw new IllegalArgumentException("Invalid token");
      }
    }
  }

  // --- Compliant: all constructors private (factory pattern) ---

  static class FactoryService {
    private FactoryService(String data) {
    }

    public static FactoryService create(String data) throws Exception {
      if (data == null) {
        throw new Exception("Null");
      }
      return new FactoryService(data);
    }
  }

  // --- Compliant: no throwing constructor ---

  static class SafeService {
    public SafeService(String data) {
      // no throw
    }
  }

  static class NoConstructor {
    void doSomething() {
    }
  }

  // --- Compliant: abstract class ---

  static abstract class AbstractService {
    public AbstractService(String data) throws Exception {
      if (data == null) {
        throw new Exception("Null");
      }
    }
  }

  // --- Compliant: private throwing constructor, public non-throwing constructor ---

  static class MixedConstructors {
    private MixedConstructors(String data) throws Exception {
      if (data == null) {
        throw new Exception("Null");
      }
    }

    public MixedConstructors(int id) {
    }
  }

  // --- Compliant: enum (implicitly final) ---

  enum Status {
    ACTIVE, INACTIVE;

    Status() {
    }
  }

  // --- Compliant: record (implicitly final) ---

  record Credential(String value) {
    Credential {
      if (value == null) {
        throw new IllegalArgumentException("Null value");
      }
    }
  }

  // --- Compliant: inner interface (no constructors) ---

  interface Service {
    void execute();
  }

  // --- Noncompliant: throw in constructor body without throws clause ---

  static class ConfigLoader { // Secondary {{Non-final class}}
    public ConfigLoader(String config) { // Noncompliant
      if (config == null) {
        throw new IllegalStateException("Missing config");
      }
    }
  }

  // --- Compliant: throw in a method, not in constructor ---

  static class Processor {
    public Processor() {
    }

    public void process() {
      throw new UnsupportedOperationException();
    }
  }

  // --- Noncompliant: nested throw in try block within constructor ---

  static class DatabaseConnection { // Secondary {{Non-final class}}
    public DatabaseConnection(String url) { // Noncompliant
      try {
        if (url == null) {
          throw new RuntimeException("Null URL");
        }
      } catch (Exception e) {
        throw new RuntimeException("Connection failed", e);
      }
    }
  }

  // --- Compliant: all throwing constructors are private ---

  static class PrivateOnlyThrowers {
    private PrivateOnlyThrowers(String s) throws Exception {
      throw new Exception();
    }

    private PrivateOnlyThrowers(int i) {
      throw new IllegalArgumentException();
    }
  }
}
