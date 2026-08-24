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

  // --- Noncompliant: abstract class with throwing constructor (attacker can subclass) ---

  static abstract class AbstractService { // Secondary {{Non-final class}}
    public AbstractService(String data) throws Exception { // Noncompliant
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

  // --- Compliant: sealed class whose permitted subclasses are all final/sealed ---

  static sealed class SealedServiceAllFinal permits AllowedSubclass {
    public SealedServiceAllFinal(String data) throws Exception {
      if (data == null) {
        throw new Exception("Null");
      }
    }
  }

  static final class AllowedSubclass extends SealedServiceAllFinal {
    public AllowedSubclass(String data) throws Exception {
      super(data);
    }
  }

  // --- Noncompliant: sealed class with a non-sealed permitted subclass ---

  static sealed class SealedServiceWithNonSealed permits OpenSubclass { // Secondary {{Non-final class}}
    public SealedServiceWithNonSealed(String data) throws Exception { // Noncompliant
      if (data == null) {
        throw new Exception("Null");
      }
    }
  }

  static non-sealed class OpenSubclass extends SealedServiceWithNonSealed { // Secondary {{Non-final class}}
    public OpenSubclass(String data) throws Exception { // Noncompliant
      super(data);
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

  // --- Compliant: class declares final finalize() method ---

  static class ProtectedByFinalizer {
    public ProtectedByFinalizer(String data) throws Exception {
      if (data == null) {
        throw new Exception("Null");
      }
    }

    @Override
    protected final void finalize() {
      // prevents finalizer attack
    }
  }

  // --- Noncompliant: instance initializer throws, no explicit constructor ---

  static class InitializerThrower { // Noncompliant {{Make this class "final" or add a private constructor, because initializers can throw.}}
    { // Secondary {{Throwing initializer}}
      if (System.currentTimeMillis() == 0) {
        throw new RuntimeException("init");
      }
    }
  }

  // --- Compliant: field initializer calls a method that may throw, but no direct throw statement ---

  static class FieldInitializerMethodCall {
    private final Object value = computeValue();

    private static Object computeValue() {
      throw new UnsupportedOperationException();
    }
  }

  // --- Compliant: instance initializer throws but has explicit private constructor ---

  static class InitializerWithPrivateConstructor {
    {
      if (System.currentTimeMillis() == 0) {
        throw new RuntimeException("init");
      }
    }

    private InitializerWithPrivateConstructor() {
    }
  }

  // --- Compliant: local class (cannot be subclassed from outside) ---

  void someMethod() {
    class LocalClass {
      LocalClass() throws Exception {
        throw new Exception("local");
      }
    }
  }

  // --- Compliant: field initializer calls a method, no direct throw in initializer expression ---

  static class FieldInitializerIndirectThrow {
    private final Object data = check(null);

    private static Object check(Object o) {
      if (o == null) {
        throw new IllegalArgumentException();
      }
      return o;
    }
  }

  // --- Noncompliant: non-final finalize() does not protect ---

  static class NonFinalFinalize { // Secondary {{Non-final class}}
    public NonFinalFinalize(String data) throws Exception { // Noncompliant
      if (data == null) {
        throw new Exception("Null");
      }
    }

    @Override
    protected void finalize() {
      // non-final finalize does NOT protect
    }
  }

  // --- Compliant: throw only inside lambda in constructor ---

  static class ThrowInLambda {
    public ThrowInLambda() {
      Runnable r = () -> {
        throw new RuntimeException("in lambda");
      };
    }
  }

  // --- Compliant: throw only inside anonymous class in constructor ---

  static class ThrowInAnonymousClass {
    public ThrowInAnonymousClass() {
      Runnable r = new Runnable() {
        @Override
        public void run() {
          throw new RuntimeException("in anon");
        }
      };
    }
  }

  // --- Noncompliant: multiple throwing instance initializers, no explicit constructor ---

  static class MultipleThrowingInitializers { // Noncompliant {{Make this class "final" or add a private constructor, because initializers can throw.}}
    { // Secondary {{Throwing initializer}}
      if (System.currentTimeMillis() == 0) {
        throw new RuntimeException("init block 1");
      }
    }
    { // Secondary {{Throwing initializer}}
      if (System.currentTimeMillis() == 1) {
        throw new RuntimeException("init block 2");
      }
    }
  }

  // --- Compliant: local class inside a constructor ---

  static class EnclosingWithLocalInConstructor {
    EnclosingWithLocalInConstructor() {
      class InnerLocal {
        InnerLocal() throws Exception {
          throw new Exception("local in ctor");
        }
      }
    }
  }

  // --- Compliant: field initializer without throw ---

  static class FieldInitializerNoThrow {
    private final String data = "hello";
  }

  // --- Compliant: field without initializer ---

  static class FieldNoInitializer {
    private String data;
  }
}
