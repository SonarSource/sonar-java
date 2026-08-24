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

  // --- Noncompliant: explicit non-private constructor AND throwing initializer ---

  static class ConstructorAndThrowingInitializer { // Secondary {{Non-final class}}
    {
      if (System.currentTimeMillis() == 0) {
        throw new RuntimeException("init");
      }
    }

    public ConstructorAndThrowingInitializer(String data) throws Exception { // Noncompliant
      if (data == null) {
        throw new Exception("Null");
      }
    }
  }

  // --- Noncompliant: explicit non-throwing constructor AND throwing initializer (constructor is still a vector) ---

  static class NonThrowingConstructorWithThrowingInit { // Secondary {{Non-final class}}
    {
      if (System.currentTimeMillis() == 0) {
        throw new RuntimeException("init");
      }
    }

    public NonThrowingConstructorWithThrowingInit() { // Noncompliant
      // non-throwing, but the initializer block throws during construction
    }
  }

  // --- Noncompliant: abstract class with throwing initializer, no constructor ---

  static abstract class AbstractWithThrowingInitializer { // Noncompliant {{Make this class "final" or add a private constructor, because initializers can throw.}}
    { // Secondary {{Throwing initializer}}
      if (System.currentTimeMillis() == 0) {
        throw new RuntimeException("abstract init");
      }
    }
  }

  // --- Compliant: sealed class with all final + sealed subclasses (deep hierarchy) ---

  static sealed class DeepSealedParent permits DeepSealedChild {
    public DeepSealedParent(String data) throws Exception {
      if (data == null) {
        throw new Exception("Null");
      }
    }
  }

  static sealed class DeepSealedChild extends DeepSealedParent permits DeepSealedGrandchild {
    public DeepSealedChild(String data) throws Exception {
      super(data);
    }
  }

  static final class DeepSealedGrandchild extends DeepSealedChild {
    public DeepSealedGrandchild(String data) throws Exception {
      super(data);
    }
  }

  // --- Compliant: class with final finalize() and throwing initializer ---

  static class FinalFinalizerWithThrowingInit {
    {
      if (System.currentTimeMillis() == 0) {
        throw new RuntimeException("init");
      }
    }

    @Override
    protected final void finalize() {
      // prevents finalizer attack
    }
  }

  // --- Compliant: class with only static initializer that throws ---

  static class StaticInitializerThrower {
    static {
      if (System.getenv("MISSING") == null) {
        throw new RuntimeException("static init");
      }
    }
  }

  // --- Compliant: field initializer calls method (no direct throw in initializer) ---

  static class FieldInitializerIndirectThrow2 {
    private final Object value = throwingInit();

    private static Object throwingInit() {
      throw new UnsupportedOperationException();
    }
  }

  // --- Compliant: field initializer is a method call (no direct throw in initializer expression) ---

  static class FieldInitWithExplicitConstructor {
    private final Object data = initField();

    public FieldInitWithExplicitConstructor() {
    }

    private Object initField() {
      throw new UnsupportedOperationException();
    }
  }

  // --- Compliant: abstract class with non-throwing constructor and no initializers ---

  static abstract class AbstractNoThrow {
    public AbstractNoThrow() {
    }
  }

  // --- Compliant: sealed class with only sealed/final subclasses (resolved via symbolType) ---

  static sealed class SealedResolved permits ResolvedFinalChild {
    public SealedResolved(String s) throws Exception {
      if (s == null) throw new Exception();
    }
  }

  static final class ResolvedFinalChild extends SealedResolved {
    ResolvedFinalChild(String s) throws Exception {
      super(s);
    }
  }

  // --- Noncompliant: class with multiple constructors, some throwing ---

  static class PartiallyVulnerable { // Secondary {{Non-final class}}
    PartiallyVulnerable(int x) throws Exception { // Noncompliant
      if (x < 0) throw new Exception();
    }

    private PartiallyVulnerable(String s) throws Exception {
      if (s == null) throw new Exception();
    }

    PartiallyVulnerable(double d) {
      // compliant: non-throwing
    }
  }

  // --- Compliant: abstract class with no throwing constructor and no throwing initializer ---

  static abstract class AbstractSafeClass {
    public AbstractSafeClass(String data) {
      // no throw
    }

    abstract void doWork();
  }

  // --- Noncompliant: abstract class with constructor that has throws clause only ---

  static abstract class AbstractThrowsClause { // Secondary {{Non-final class}}
    protected AbstractThrowsClause() throws Exception { // Noncompliant
    }
  }

  // --- Compliant: sealed class with sealed child (not non-sealed) ---

  static sealed class SealedWithSealedChild permits SealedChild {
    public SealedWithSealedChild(String s) throws Exception {
      if (s == null) throw new Exception();
    }
  }

  static sealed class SealedChild extends SealedWithSealedChild permits FinalGrandchild {
    public SealedChild(String s) throws Exception {
      super(s);
    }
  }

  static final class FinalGrandchild extends SealedChild {
    public FinalGrandchild(String s) throws Exception {
      super(s);
    }
  }

  // --- Compliant: abstract class with only abstract methods ---

  static abstract class AbstractMethodOnly {
    abstract void compute();
  }

  // --- Compliant: field initializer without direct throw ---

  static class FieldInitializerSafe {
    private final String value = String.valueOf(42);

    public FieldInitializerSafe() {
    }
  }

  // --- Compliant: field initializer is anonymous class with throw (skipped by visitor) ---

  static class FieldInitAnonymousThrow {
    private final Runnable action = new Runnable() {
      @Override
      public void run() {
        throw new RuntimeException("in anon in field init");
      }
    };
  }

  // --- Noncompliant: non-final, non-private constructors where one has throw and one has throws clause ---

  static class BothThrowAndThrowsClause { // Secondary {{Non-final class}}
    public BothThrowAndThrowsClause(int x) { // Noncompliant
      if (x < 0) {
        throw new IllegalArgumentException();
      }
    }

    protected BothThrowAndThrowsClause(String s) throws Exception { // Noncompliant
    }
  }

  // --- Noncompliant: finalize(Object) is not zero-arg finalize, does not protect ---

  static class WrongFinalizeSignature { // Secondary {{Non-final class}}
    public WrongFinalizeSignature(String s) throws Exception { // Noncompliant
      if (s == null) throw new Exception();
    }

    protected final void finalize(Object obj) {
      // wrong signature, does not protect
    }
  }

  // --- Noncompliant: class with field without initializer but throwing constructor ---

  static class FieldWithoutInitializer { // Secondary {{Non-final class}}
    private Object data;

    public FieldWithoutInitializer() throws Exception { // Noncompliant
      throw new Exception();
    }
  }
}
