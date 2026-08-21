package checks;

class FinalizerAttackCheckSample {

  // --- Noncompliant: non-final class with throwing constructor ---

  class SecurityService { // Noncompliant {{Make this class "final" or make the throwing constructors "private".}}
    private final String token;

    public SecurityService(String token) throws IllegalArgumentException {
      if (token == null) {
        throw new IllegalArgumentException("Invalid token");
      }
      this.token = token;
    }
  }

  class AuthProvider { // Noncompliant
    public AuthProvider(String credentials) throws Exception {
      if (credentials.isEmpty()) {
        throw new Exception("Bad credentials");
      }
    }
  }

  class ResourceLoader { // Noncompliant
    ResourceLoader(String path) {
      if (path == null) {
        throw new NullPointerException();
      }
    }
  }

  class MultiConstructorService { // Noncompliant
    MultiConstructorService(int id) throws Exception {
      if (id < 0) {
        throw new Exception("Negative id");
      }
    }

    MultiConstructorService(String name) {
    }
  }

  class ProtectedConstructorService { // Noncompliant
    protected ProtectedConstructorService(String data) throws Exception {
      if (data == null) {
        throw new Exception("Null data");
      }
    }
  }

  class ThrowsClauseOnly { // Noncompliant
    public ThrowsClauseOnly() throws Exception {
    }
  }

  // --- Compliant: final class ---

  final class SecureService {
    public SecureService(String token) throws IllegalArgumentException {
      if (token == null) {
        throw new IllegalArgumentException("Invalid token");
      }
    }
  }

  // --- Compliant: all constructors private (factory pattern) ---

  class FactoryService {
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

  class SafeService {
    public SafeService(String data) {
      // no throw
    }
  }

  class NoConstructor {
    void doSomething() {
    }
  }

  // --- Compliant: abstract class ---

  abstract class AbstractService {
    public AbstractService(String data) throws Exception {
      if (data == null) {
        throw new Exception("Null");
      }
    }
  }

  // --- Compliant: private throwing constructor, public non-throwing constructor ---

  class MixedConstructors {
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

  class ConfigLoader { // Noncompliant
    public ConfigLoader(String config) {
      if (config == null) {
        throw new IllegalStateException("Missing config");
      }
    }
  }

  // --- Compliant: throw in a method, not in constructor ---

  class Processor {
    public Processor() {
    }

    public void process() {
      throw new UnsupportedOperationException();
    }
  }

  // --- Noncompliant: nested throw in try block within constructor ---

  class DatabaseConnection { // Noncompliant
    public DatabaseConnection(String url) {
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

  class PrivateOnlyThrowers {
    private PrivateOnlyThrowers(String s) throws Exception {
      throw new Exception();
    }

    private PrivateOnlyThrowers(int i) {
      throw new IllegalArgumentException();
    }
  }
}
