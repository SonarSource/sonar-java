package symbolicexecution.checks.NonNullSetToNullCheck.noDefault;

import javax.annotation.CheckForNull;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.annotation.ParametersAreNonnullByDefault;
import javax.annotation.meta.When;
import org.eclipse.jdt.annotation.DefaultLocation;

/**
 * The rule reports an issue in many situations/locations:
 * - 1 Fields:
 * - 1.1 at constructor level, when a nonnull field is not assigned to anything.
 * - 1.2 when a nonnull field is assigned a nullable value (directly or indirectly).
 * - 2 Return values:
 * - When a nullable value is returned from a method annotated nonnull.
 * - 3 Arguments:
 * - When a nullable value is passed to a parameter annotated nonnull.
 */
class NonNullSetToNullCheck {

  @Nonnull
  private String primary;
  private String secondary;
  private Object testObject;

  @Nullable
  private String nullableField;

  @Nonnull
  private static Integer STATIC_FIELD; // Compliant as static fields are not reported
  private static Integer STATIC_FIELD_NO_ANNOTATION;

  // ============ 1.1 Fields not initialized in constructor ============
  public NonNullSetToNullCheck(String color) {
    if (color != null) {
      secondary = null;
    }
    primary = color;  // Noncompliant [[sc=5;ec=20]] {{"primary" is marked "javax.annotation.Nonnull" but is set to null.}}
    testObject = new Object();
  }

  public NonNullSetToNullCheck(@Nonnull String color, String other) {
    primary = color;
    secondary = other;
    testObject = new Object();
  }

  public NonNullSetToNullCheck() { // Noncompliant [[sc=10;ec=31]] {{"primary" is marked "javax.annotation.Nonnull" but is not initialized in this constructor.}}
    return; // Just for coverage
  }

  // ============ 1.2 Testing fields set to nullable ============
  public void reset() {
    primary = null; // Noncompliant {{"primary" is marked "javax.annotation.Nonnull" but is set to null.}}
    secondary = null;
  }

  public String returnColor() {
    if (secondary == null) {
      this.primary = null; // Noncompliant {{"primary" is marked "javax.annotation.Nonnull" but is set to null.}}
      return secondary;
    }
    return primary;
  }

  public void resetConvoluted(String color) {
    if (secondary == null) {
      primary = secondary; // Noncompliant {{"primary" is marked "javax.annotation.Nonnull" but is set to null.}}
    }
    secondary = color;
  }

  public void setSecondary(@Nonnull String color) {
    secondary = color; // Compliant: Nonnull to Nonnull
  }

  public void setColors(@Nonnull String color, String other) {
    primary = other;
    secondary = other;
  }

  public void setColors2(@Nullable String color) {
    primary = color; // Noncompliant {{"primary" is marked "javax.annotation.Nonnull" but is set to null.}}
    secondary = color; // Compliant, secondary is not Nonnull
  }

  public void parameterAssignment(@Nonnull String color) {
    if (secondary == null) {
      color = secondary; // Noncompliant {{"color" is marked "javax.annotation.Nonnull" but is set to null.}}
    }
  }

  public void parameterAssignment2(@Nonnull String color) {
    if (secondary != null) {
      color = secondary; // Compliant: secondary is not null
    }
  }

  public void setColors(@Nonnull String... colors) {
    if (colors.length > 1) {
      primary = colors[0];  // Compliant since the contents of the array cannot be inferred
      secondary = colors[1];
    }
  }

  // ============ 2. Testing Return values ============
  @Nonnull
  public String colorMix() {
    return null;  // Noncompliant {{This method's return value is marked "javax.annotation.Nonnull" but null is returned.}}
  }

  @Nonnull(when = When.MAYBE)
  public String maybeNull() {
    return null; // Compliant, When.MAYBE is equivalent to Nullable
  }

  @Nonnull(when = When.UNKNOWN)
  public String unknownNull() {
    return null; // Compliant, When.UNKNOWN is equivalent to Nullable
  }

  @Nonnull(when = When.ALWAYS)
  public String neverNull() {
    return null; // Noncompliant
  }

  @Nonnull
  public String indirectMix() {
    String mix = null;
    return mix;  // Noncompliant {{This method's return value is marked "javax.annotation.Nonnull" but null is returned.}}
  }

  @Nonnull
  public String getPrimary() {
    return nullableField;  // FN: nullable field returned by nonnull method
  }

  @Nonnull
  public String getSecondary() {
    return secondary; // Compliant: secondary is not annotated
  }

  @Nonnull
  public void noReturnMethod() {
    if (secondary == null) {
      return;
    }
  }

  @Nonnull
  public String getSecondaryOrDefault(String color) {
    if (color.length() == 0 && secondary != null) {
      return secondary;
    }
    return color;
  }

  // ============ 3. Testing nullable value passed as argument ============
  public static void initialize1() {
    NonNullSetToNullCheck instance =
      new NonNullSetToNullCheck(null, "Blue");  // Noncompliant [[sc=7;ec=46]] {{Parameter 1 to this constructor is marked "javax.annotation.Nonnull" but null could be passed.}}
  }

  public static void initialize2() {
    NonNullSetToNullCheck instance = new NonNullSetToNullCheck("Black", null);
    instance.setColors(null, "Green");  // Noncompliant {{Parameter 1 to this call is marked "javax.annotation.Nonnull" but null could be passed.}}
  }

  public static void initialize3() {
    NonNullSetToNullCheck instance = new NonNullSetToNullCheck("Red", null);
    instance.setSecondary(null);  // Noncompliant {{Parameter 1 to this call is marked "javax.annotation.Nonnull" but null could be passed.}}
  }

  public static void initiliaze4() {
    NonNullSetToNullCheck instance = new NonNullSetToNullCheck("Red", null);
    instance.setColors("Cyan", "Blue");
    getColorMix();
    doSomething();
    int n = 0;
    n += 2;
  }

  public static void initialize5() {
    NonNullSetToNullCheck instance =
      new NonNullSetToNullCheck(checkForNull(), "Blue");  // Noncompliant
  }

  private static void doSomething() {
  }

  private static void getColorMix() {
  }

  @CheckForNull
  private static String checkForNull() {
    return null;
  }
}

class Coverage {

  @Nonnull
  public Object[] notnullableField;

  private void checkColors(Object text, String... texts) {
  }

  public void method1(@Nonnull Object[] a1, @Nonnull Object... variadic) {
  }

  private void callMethods(java.util.function.Supplier<String> supplier) {
    String[] texts = new String[2];
    checkColors("a");
    checkColors("a", "b");
    checkColors("a", "b", "c");
    texts[0] = "z";
    Coverage coverage = new Coverage();
    method1(texts, notnullableField);
    throw new NullPointerException(supplier.get());
  }
}

// ============ Other test cases ============
class NonNullSetToNullCheckFieldWithInitializer {
  @Nonnull
  private static final Integer STATIC_FIELD = null; // Compliant as static fields are not reported
  private static final Integer STATIC_FIELD_NO_ANNOTATION = null;
  @Nonnull
  private final Integer val1 = calculate();
  @Nonnull
  private Integer val2 = calculate();

  public NonNullSetToNullCheckFieldWithInitializer() {
  } // Compliant, the field has an initializer

  @Nonnull
  private Integer calculate() {
    return 42;
  }
}

class NonNullSetToNullCheckCallingOtherConstructor {
  @Nonnull
  private Integer value;

  NonNullSetToNullCheckCallingOtherConstructor(String s) { // Compliant - calls other constructor
    this(Integer.valueOf(s));
  }

  NonNullSetToNullCheckCallingOtherConstructor(Object o) { // Noncompliant - calls super constructor but do not initialize value
    super();
  }

  NonNullSetToNullCheckCallingOtherConstructor(String s, Object o) { // Noncompliant - calls other method
    this.foo(o);
  }

  NonNullSetToNullCheckCallingOtherConstructor(Integer i) {
    this.value = i;
  }

  void foo(Object o) {
  }
}

class NonNullSetToNullCheckExitWithException {

  @Nonnull
  private Object self;

  public NonNullSetToNullCheckExitWithException() {
    self = getSelf(); // Compliant
  }

  public NonNullSetToNullCheckExitWithException(int i) throws NonNullSetToNullCheckMyException {
    self = getOtherSelf(); // Compliant
  }

  public NonNullSetToNullCheckExitWithException(@Nonnull String i) {
    if (i == null) {
      throw new RuntimeException(); // method exit on exception, OK
    }
    self = i;
  }

  public NonNullSetToNullCheckExitWithException(String x, String y) { // Compliant
    try {
      Integer.valueOf(x);
    } catch (NumberFormatException e) {
      throw new RuntimeException(e);
    }
    this.self = x;
  }

  @Nonnull
  public Object getSelf() throws IllegalArgumentException {
    return self;
  }

  @Nonnull
  public Object getOtherSelf() throws NonNullSetToNullCheckMyException {
    return self;
  }
}

class NonNullSetToNullCheckMyException extends Exception {
}

abstract class ExcludedMethods {
  @CheckForNull
  abstract Object getMyObject();

  void foo() {
    com.google.common.base.Preconditions.checkNotNull(getMyObject()); // Compliant
    com.google.common.base.Preconditions.checkNotNull(getMyObject(), "yolo"); // Compliant
    com.google.common.base.Preconditions.checkNotNull(getMyObject(), "yolo", new Object(), 2); // Compliant
  }
}

class MultipleConstructors {

  @Nonnull
  private final String a;

  MultipleConstructors() {
    this("test");
  }

  private MultipleConstructors(@Nonnull String b) {
    this.a = b;
  }

}

class SpringJavaBean {

  @javax.validation.constraints.NotNull // This annotation will be used by Bean Validation
  private String field;

  private SpringJavaBean() { // Compliant
    // Java Bean's fields will be initialized and validated later
  }

  public String getField() {
    return field;
  }

  public void setField(String field) {
    this.field = field;
  }

}

class DifferentAnnotations {
  @org.springframework.lang.NonNull
  private String nonNullSpring;

  @reactor.util.annotation.NonNull
  private String nonNullReactor;

  DifferentAnnotations() {
    nonNullSpring = null; // Noncompliant
    nonNullReactor = null; // Noncompliant
  }
}

// ============ javax.persistence special cases ============
@javax.persistence.Entity
class JpaEntityInvalidDefault {

  @Nonnull
  private String itemName;

  private String otherField;

  public JpaEntityInvalidDefault() { // Noncompliant  {{"itemName" is marked "javax.annotation.Nonnull" but is not initialized in this constructor.}}
    otherField = "test";
  }
}

@javax.persistence.Embeddable
class JpaEmbeddable {

  @Nonnull
  private String itemName;

  public JpaEmbeddable() { // Compliant
    // Default constructor for JPA
  }

  public JpaEmbeddable(String name) {
    itemName = name;
  }
}

@javax.persistence.MappedSuperclass
class JpaMappedSuperClass {

  @Nonnull
  private String itemName;

  public JpaMappedSuperClass() { // Compliant
    // Default constructor for JPA
  }

  public JpaMappedSuperClass(String name) {
    itemName = name;
  }
}

// ============ Class level annotations ============
@ParametersAreNonnullByDefault
class HandleParametersAreNonnullByDefault {
  HandleParametersAreNonnullByDefault(Object o) {
  }

  void foo(Object o) {
    foo(null); // Compliant- FN - @ParametersAreNonnullByDefault not handled
    new HandleParametersAreNonnullByDefault(null); // Compliant - FN - @ParametersAreNonnullByDefault not handled
  }

  void bar(@Nullable Object o) {
    bar(null); // Compliant
  }

  @Override
  public boolean equals(Object obj) {
    return equals(null); // Compliant
  }
}

@org.eclipse.jdt.annotation.NonNullByDefault
// NonNullByDefault targets parameters, return values and fields
class HandleNonNullByDefault {
  Boolean notInitialized;
  Integer initialized;

  // 1.1: field not assigned in constructor
  public HandleNonNullByDefault() { // Noncompliant {{"notInitialized" is marked "org.eclipse.jdt.annotation.NonNullByDefault" but is not initialized in this constructor.}}
    initialized = 200;
  }

  // 1.2: field assigned
  public void setInitialized() {
    this.initialized = null; // Noncompliant {{"initialized" is marked "org.eclipse.jdt.annotation.NonNullByDefault" but is set to null.}}
  }

  // 2. return values
  public String returnNull() {
    return null;  // Noncompliant {{This method's return value is marked "org.eclipse.jdt.annotation.NonNullByDefault" but null is returned.}}
  }

  // 3.
  public void notNullArgument(Object o) {
    notNullArgument(null);  // Compliant - FN
  }
}

@org.eclipse.jdt.annotation.NonNullByDefault
class HandleNonNullByDefaultPrimitives {
  boolean falseByDefault;

  public HandleNonNullByDefaultPrimitives() { // Compliant, primitive are not reported
  }
}

// ============ Method level annotations ============
class HandleEclipseParametersNonNull {
  @org.eclipse.jdt.annotation.NonNullByDefault(DefaultLocation.PARAMETER)
  void foo(Object o) {
    foo(null); // Compliant - FN - @NonNullByDefault not handled
  }

  @org.eclipse.jdt.annotation.NonNullByDefault(DefaultLocation.RETURN_TYPE)
  Object bar(Object o) {
    return bar(null); // Compliant
  }

  @org.eclipse.jdt.annotation.NonNullByDefault(DefaultLocation.RETURN_TYPE)
  Object shouldNotReturnNull(Object o) {
    return null; // Noncompliant {{This method's return value is marked "org.eclipse.jdt.annotation.NonNullByDefault" but null is returned.}}
  }
}

// ============ Via meta annotation ============
@Nonnull
@interface MyNonNull {

}

class HandleAnnotatedViaMetaAnnotation {
  @MyNonNull
  Object shouldNotReturnNull(Object o) {
    return null; // Compliant - FN
  }
}

