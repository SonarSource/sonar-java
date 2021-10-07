import javax.annotation.CheckForNull;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.annotation.meta.When;
import javax.annotation.ParametersAreNonnullByDefault;
import org.eclipse.jdt.annotation.DefaultLocation;
import org.eclipse.jdt.annotation.NonNullByDefault;

class MainClass {

  @Nonnull
  private String primary;
  private String secondary;

  @Nonnull
  private static final Integer STATIC_FIELD; // Compliant as static fields are not reported
  private static final Integer STATIC_FIELD_NO_ANNOTATION;

  public MainClass(String color) {
    if (color != null) {
      secondary = null;
    }
    primary = color;  // Noncompliant {{"primary" is marked "javax.annotation.Nonnull" but is set to null.}}
    testObject = new Object();
  }

  public MainClass(@Nonnull String color, String other) {
    primary = color;
    secondary = other;
    testObject = new Object();
  }

  public MainClass() { // Noncompliant [[sc=10;ec=19]] {{"primary" is marked "javax.annotation.Nonnull" but is not initialized in this constructor.}}
    return; // Just for coverage
  }

  @Nonnull
  public String colorMix() {
    return null;  // Noncompliant {{This method's return value is marked "javax.annotation.Nonnull" but null is returned.}}
  }

  @Nonnull(when = When.MAYBE)
  public String maybeNull() {
    return null; // Compliant
  }

  @Nonnull(when = When.UNKNOWN)
  public String unknownNull() {
    return null; // Compliant
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
  public String unknownMix() {
    String mix = getUnknownColor();
    return mix;  // Compliant, otherwise FP!
  }

  @Nonnull
  public String getSecondary() {
    return secondary;
  }

  @Nonnull
  public String getSecondaryOrDefault(String color) {
    if (color.length() == 0 && secondary != null) {
      return secondary;
    }
    return color;
  }

  public void setSecondary(@Nonnull String color) {
    secondary = color;
  }

  public void setColors(@Nonnull String color, String other) {
    primary = other;
    secondary = other;
  }

  public void reset() {
    primary = null; // Noncompliant {{"primary" is marked "javax.annotation.Nonnull" but is set to null.}}
    secondary = null;
  }

  public void resetConvoluted(String color) {
    if (secondary == null) {
      primary = secondary; // Noncompliant {{"primary" is marked "javax.annotation.Nonnull" but is set to null.}}
    }
    secondary = color;
  }

  public void parameterAssignment(@Nonnull String color) {
    if (secondary == null) {
      color = secondary; // Noncompliant {{"color" is marked "javax.annotation.Nonnull" but is set to null.}}
    }
    if (secondary != null) {
      color = secondary; // Compliant: secondary is not null
    }
  }

  public String returnColor() {
    if (secondary == null) {
      this.primary = null; // Noncompliant {{"primary" is marked "javax.annotation.Nonnull" but is set to null.}}
      return secondary;
    }
    return primary;
  }

  public static void initialize1() {
    MainClass instance = new MainClass(null, "Blue");  // Noncompliant {{Parameter 1 to this constructor is marked "javax.annotation.Nonnull" but null could be passed.}}
  }

  public static void initialize2() {
    MainClass instance = new MainClass("Black", null);
    instance.setColors(null, "Green");  // Noncompliant {{Parameter 1 to this call is marked "javax.annotation.Nonnull" but null could be passed.}}
  }

  public static void initialize3() {
    MainClass instance = new MainClass("Red", null);
    instance.setSecondary(null);  // Noncompliant {{Parameter 1 to this call is marked "javax.annotation.Nonnull" but null could be passed.}}
  }

  public static void initiliaze4() {
    MainClass instance = new MainClass("Red", null);
    instance.setColors("Cyan", "Blue");
    getColorMix();
    doSomething();
    int n = 0;
    n += 2;
  }

  public void setColors(@Nonnull String... colors) {
    if (colors.length > 1) {
      primary = colors[0];  // Compliant since the contents of the array cannot be inferred
      secondary = colors[1];
    }
  }

  @Nonnull
  public void noReturnMethod() {
    if (secondary == null) {
      return;
    }
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

class FieldWithInitializer {
  @Nonnull
  private static final Integer STATIC_FIELD = null; // Compliant as static fields are not reported
  private static final Integer STATIC_FIELD_NO_ANNOTATION = null;
  @Nonnull
  private final Integer val1 = calculate();
  @Nonnull
  private Integer val2 = calculate();

  public FieldWithInitializer() {} // Compliant, the field has an initializer

  @Nonnull
  private Integer calculate() {
    return 42;
  }
}

class CallingOtherConstructor {
  @Nonnull
  private final Integer value;

  CallingOtherConstructor(String s) { // Compliant - calls other constructor
    this(Integer.valueOf(s));
  }

  CallingOtherConstructor(Object o) { // Noncompliant - calls super constructor but do not initialize value
    super();
  }

  CallingOtherConstructor(String s, Object o) { // Noncompliant - calls other method
    this.foo(o);
  }

  CallingOtherConstructor(Integer i) {
    this.value = i;
  }

  void foo(Object o) {}
}

class ExitWithException {

  @Nonnull
  private Object self;

  public ExitWithException() {
    self = getSelf(); // Compliant
  }

  public ExitWithException(int i) throws MyException {
    self = getOtherSelf(); // Compliant
  }

  public ExitWithException(@Nonnull String i) {
    if (i == null) {
      throw new RuntimeException(); // method exit on exception, OK
    }
    self = i;
  }

  public ExitWithException(String x, String y) { // Compliant
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
  public Object getOtherSelf() throws MyException {
    return self;
  }
}

class MyException extends Exception { }

@ParametersAreNonnullByDefault
class HandleParametersAreNonnullByDefault {
  HandleParametersAreNonnullByDefault(Object o) {
  }

  void foo(Object o) {
    foo(null); // Compliant - @ParametersAreNonnullByDefault not handled
    new HandleParametersAreNonnullByDefault(null); // Compliant - @ParametersAreNonnullByDefault not handled
  }

  void bar(@Nullable Object o) {
    bar(null); // Compliant
  }

  @Override
  public boolean equals(Object obj) {
    return equals(null); // Compliant
  }
}

class HandleEclipseParametersNonNull {
  @NonNullByDefault(DefaultLocation.PARAMETER)
  void foo(Object o) {
    foo(null); // Compliant - @NonNullByDefault not handled
  }

  @NonNullByDefault(DefaultLocation.RETURN_TYPE)
  Object bar(Object o) {
    return bar(null); // Compliant
  }
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

@NonNullByDefault
class NoIssueOnPrimitive {
  boolean falseByDefault;
  Boolean nullByDefault;

  public Example() {
    nullByDefault = false;
    nullByDefault2 = 200;
  }
}
