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
 * - 4 Local Variable: when a nonnull local variable is assigned a nullable value.
 * Note: S4449 is targeting point 3 aswell, but only when it is not directly annotated. The current rule will therefore only report an issue if
 * the parameter is directly annotated nonnull.
 */
class NonNullSetToNullCheckSample {

  @Nonnull
  private String primary;
  private String secondary;
  private Object testObject;

  @Nonnull
  private String otherNonnullField;

  @Nullable
  private String nullableField;

  @Nonnull
  private static Integer STATIC_FIELD; // Compliant as static fields are not reported
  private static Integer STATIC_FIELD_NO_ANNOTATION;

  // ============ 1.1 Fields not initialized in constructor ============
  public NonNullSetToNullCheckSample(String color) {
    if (color != null) {
      secondary = null;
    }
    primary = color; // Noncompliant {{"primary" is marked "@Nonnull" but is set to null.}}
//  ^^^^^^^^^^^^^^^
    testObject = new Object();
    otherNonnullField = "";
  }

  public NonNullSetToNullCheckSample(@Nonnull String color, String other) {
    primary = color;
    secondary = other;
    testObject = new Object();
    otherNonnullField = "";
  }

  public NonNullSetToNullCheckSample() { // Noncompliant {{"primary" is marked "@Nonnull" but is not initialized in this constructor.}}
//       ^^^^^^^^^^^^^^^^^^^^^^^^^^^
    return; // Just for coverage
  }

  // ============ 1.2 Testing fields set to nullable ============
  public void reset() {
    primary = null; // Noncompliant {{"primary" is marked "@Nonnull" but is set to null.}}
    secondary = null;
  }

  public String returnColor() {
    if (secondary == null) {
      this.primary = null; // Noncompliant {{"primary" is marked "@Nonnull" but is set to null.}}
      return secondary;
    }
    return primary;
  }

  public void resetConvoluted(String color) {
    if (secondary == null) {
      primary = secondary; // Noncompliant {{"primary" is marked "@Nonnull" but is set to null.}}
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
    primary = color; // Noncompliant {{"primary" is marked "@Nonnull" but is set to null.}}
    secondary = color; // Compliant, secondary is not Nonnull
  }

  public void parameterAssignment() {
    if (secondary == null) {
      otherNonnullField = secondary; // Noncompliant {{"otherNonnullField" is marked "@Nonnull" but is set to null.}}
    }
  }

  public void parameterAssignment2() {
    if (secondary != null) {
      otherNonnullField = secondary; // Compliant: secondary is not null
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
    return null; // Noncompliant {{This method's return value is marked "@Nonnull" but null is returned.}}
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
    return mix; // Noncompliant {{This method's return value is marked "@Nonnull" but null is returned.}}
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
    NonNullSetToNullCheckSample instance =
      new NonNullSetToNullCheckSample(null, "Blue"); // Noncompliant {{Parameter 1 to this constructor is marked "@Nonnull" but null could be passed.}}
//    ^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^
  }

  public static void initialize2() {
    NonNullSetToNullCheckSample instance = new NonNullSetToNullCheckSample("Black", null);
    instance.setColors(null, "Green"); // Noncompliant {{Parameter 1 to this call is marked "@Nonnull" but null could be passed.}}
  }

  public static void initialize3() {
    NonNullSetToNullCheckSample instance = new NonNullSetToNullCheckSample("Red", null);
    instance.setSecondary(null); // Noncompliant {{Parameter 1 to this call is marked "@Nonnull" but null could be passed.}}
  }

  public static void initiliaze4() {
    NonNullSetToNullCheckSample instance = new NonNullSetToNullCheckSample("Red", null);
    instance.setColors("Cyan", "Blue");
    getColorMix();
    doSomething();
    int n = 0;
    n += 2;
  }

  public static void initialize5() {
    NonNullSetToNullCheckSample instance =
      new NonNullSetToNullCheckSample(checkForNull(), "Blue"); // Noncompliant
  }

  // ============ 4. Local variable assigned nullable value ============
  public static void localToNull() {
    @Nonnull
    Object o = new Object();
    o = null; // Noncompliant {{"o" is marked "@Nonnull" but is set to null.}}
  }

  public void localToNull2() {
    @Nonnull
    Object o = new Object();
    if (secondary == null) {
      o = secondary; // Noncompliant {{"o" is marked "@Nonnull" but is set to null.}}
    }
  }

  public void localToNull3() {
    @Nonnull
    Object o = new Object();
    if (secondary != null) {
      o = secondary; // Compliant
    }
  }

  public static void argumentToNull(@Nonnull Object o) {
    o = null; // Compliant: it is fine to assign a Nonnull parameter to null in the body of the method.
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
class NonNullSetToNullCheckSampleFieldWithInitializer {
  @Nonnull
  private static final Integer STATIC_FIELD = null; // Compliant as static fields are not reported
  private static final Integer STATIC_FIELD_NO_ANNOTATION = null;
  @Nonnull
  private final Integer val1 = calculate();
  @Nonnull
  private Integer val2 = calculate();

  public NonNullSetToNullCheckSampleFieldWithInitializer() {
  } // Compliant, the field has an initializer

  @Nonnull
  private Integer calculate() {
    return 42;
  }
}

class NonNullSetToNullCheckSampleCallingOtherConstructor {
  @Nonnull
  private Integer value;

  NonNullSetToNullCheckSampleCallingOtherConstructor(String s) { // Compliant - calls other constructor
    this(Integer.valueOf(s));
  }

  NonNullSetToNullCheckSampleCallingOtherConstructor(Object o) { // Noncompliant
    super();
  }

  NonNullSetToNullCheckSampleCallingOtherConstructor(String s, Object o) { // Noncompliant
    this.foo(o);
  }

  NonNullSetToNullCheckSampleCallingOtherConstructor(Integer i) {
    this.value = i;
  }

  void foo(Object o) {
  }
}

class NonNullSetToNullCheckSampleExitWithException {

  @Nonnull
  private Object self;

  public NonNullSetToNullCheckSampleExitWithException() {
    self = getSelf(); // Compliant
  }

  public NonNullSetToNullCheckSampleExitWithException(int i) throws NonNullSetToNullCheckSampleMyException {
    self = getOtherSelf(); // Compliant
  }

  public NonNullSetToNullCheckSampleExitWithException(@Nonnull String i) {
    if (i == null) {
      throw new RuntimeException(); // method exit on exception, OK
    }
    self = i;
  }

  public NonNullSetToNullCheckSampleExitWithException(String x, String y) { // Compliant
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
  public Object getOtherSelf() throws NonNullSetToNullCheckSampleMyException {
    return self;
  }
}

class NonNullSetToNullCheckSampleMyException extends Exception {
}

abstract class ExcludedMethods {
  @CheckForNull
  abstract Object getMyObject();

  void foo() {
    com.google.common.base.Preconditions.checkNotNull(getMyObject()); // Compliant - will be reported in S4449
    com.google.common.base.Preconditions.checkNotNull(getMyObject(), "yolo"); // Compliant - will be reported in S4449
    com.google.common.base.Preconditions.checkNotNull(getMyObject(), "yolo", new Object(), 2); // Compliant - will be reported in S4449
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

  public JpaEntityInvalidDefault() { // Noncompliant {{"itemName" is marked "@Nonnull" but is not initialized in this constructor.}}
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
    foo(null); // Compliant - will be reported in S4449
    new HandleParametersAreNonnullByDefault(null); // Compliant - will be reported in S4449
  }

  void bar(@Nullable Object o) {
    bar(null); // Compliant
  }

  @Override
  public boolean equals(Object obj) {
    return equals(null); // Compliant - will be reported in S4449
  }
}

@org.eclipse.jdt.annotation.NonNullByDefault
// NonNullByDefault targets parameters, return values and fields
class HandleNonNullByDefault {
  Boolean notInitialized;
  Integer initialized;

  // 1.1: field not assigned in constructor
  public HandleNonNullByDefault() { // Noncompliant {{"notInitialized" is marked "@NonNullByDefault at class level" but is not initialized in this constructor.}}
    initialized = 200;
  }

  // 1.2: field assigned
  public void setInitialized() {
    this.initialized = null; // Noncompliant {{"initialized" is marked "@NonNullByDefault at class level" but is set to null.}}
  }

  // 2. return values
  public String returnNull() {
    return null; // Noncompliant {{This method's return value is marked "@NonNullByDefault at class level" but null is returned.}}
  }

  // 3.
  public void notNullArgument(Object o) {
    notNullArgument(null);  // Compliant - will be reported in S4449
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
    foo(null); // Compliant - will be reported in S4449
  }

  @org.eclipse.jdt.annotation.NonNullByDefault(DefaultLocation.RETURN_TYPE)
  Object bar(Object o) {
    return bar(null); // Compliant
  }

  @org.eclipse.jdt.annotation.NonNullByDefault(DefaultLocation.RETURN_TYPE)
  Object shouldNotReturnNull(Object o) {
    return null; // Noncompliant {{This method's return value is marked "@NonNullByDefault" but null is returned.}}
  }
}

// ============ Via meta annotation ============
@Nonnull
@interface MyNonNull {

}

class HandleAnnotatedViaMetaAnnotation {
  @MyNonNull
  Object shouldNotReturnNull(Object o) {
    return null; // Noncompliant {{This method's return value is marked "@Nonnull via meta-annotation" but null is returned.}}
  }
}

record TestSonar(@Nonnull String arg1, String arg2, String arg3, String arg4, long arg5, String arg6) {
  public TestSonar {}
  public static void f() {
    new TestSonar(null, null, null, null, 0L, null); // Noncompliant
  }
}

// ============ jakarta annotations ============
class JakartaSpringJavaBean {

  @jakarta.validation.constraints.NotNull // This annotation will be used by Bean Validation
  private String field;

  private JakartaSpringJavaBean() { // Compliant
    // Java Bean's fields will be initialized and validated later
  }

  public String getField() {
    return field;
  }

  public void setField(String field) {
    this.field = field;
  }
}
@jakarta.persistence.Entity
class JakartaJpaEntityInvalidDefault {

  @jakarta.annotation.Nonnull
  private String itemName;

  @jakarta.annotation.Nonnull
  private String primary;
  private String secondary;

  private String otherField;

  public JakartaJpaEntityInvalidDefault() { // Noncompliant {{"itemName" is marked "@Nonnull" but is not initialized in this constructor.}}
    otherField = "test";
  }
  public void setColors2(@jakarta.annotation.Nullable String color) {
    primary = color; // Noncompliant {{"primary" is marked "@Nonnull" but is set to null.}}
    secondary = color; // Compliant, secondary is not Nonnull
  }
}

@jakarta.persistence.Embeddable
class JakartaJpaEmbeddable {

  @jakarta.annotation.Nonnull
  private String itemName;

  public JakartaJpaEmbeddable() { // Compliant
    // Default constructor for JPA
  }

  public JakartaJpaEmbeddable(String name) {
    itemName = name;
  }
}

@jakarta.persistence.MappedSuperclass
class JakartaJpaMappedSuperClass {

  @jakarta.annotation.Nonnull
  private String itemName;

  public JakartaJpaMappedSuperClass() { // Compliant
    // Default constructor for JPA
  }

  public JakartaJpaMappedSuperClass(String name) {
    itemName = name;
  }
}

