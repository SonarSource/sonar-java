package javax.annotation;

@interface Nonnull {}

public class MainClass {

  @Nonnull
  private String primary;
  private String secondary;

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

  public MainClass() { // Noncompliant  {{"primary" is marked "javax.annotation.Nonnull" but is not initialized in this constructor.}}
    return; // Just for coverage
  }

  @Nonnull
  public String colorMix() {
    return null;  // Noncompliant {{This method's return value is marked "javax.annotation.Nonnull" but null is returned.}}
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
      this.primary = null; // FN does not handle fields accessed by this.
      return secondary;
    }
    return primary;
  }

  public static void initialize1() {
    MainClass instance = new MainClass(null, "Blue");  // Noncompliant {{Parameter 1 to this constructor is marked "javax.annotation.Nonnull" but null is passed.}}
  }

  public static void initialize2() {
    MainClass instance = new MainClass("Black", null);
    instance.setColors(null, "Green");  // Noncompliant {{Parameter 1 to this call is marked "javax.annotation.Nonnull" but null is passed.}}
  }

  public static void initialize3() {
    MainClass instance = new MainClass("Red", null);
    instance.setSecondary(null);  // Noncompliant {{Parameter 1 to this call is marked "javax.annotation.Nonnull" but null is passed.}}
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

public class Coverage {

  @Nonnull
  public Object[] notnullableField;

  private void checkColors(Object text, String... texts) {
  }

  public void method1(@Nonnull Object[] a1, @Nonnull Object... variadic) {
  }

  private void callMethods(Supplier<String> supplier) {
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
public class JpaEntityInvalidDefault {

  @Nonnull
  private String itemName;

  private String otherField;

  public JpaEntity() { // Noncompliant  {{"itemName" is marked "javax.annotation.Nonnull" but is not initialized in this constructor.}}
    otherField = "test";
  }
}

@javax.persistence.Embeddable
public class JpaEmbeddable {

  @Nonnull
  private String itemName;

  public JpaEmbeddable() { // Compliant
    // Default constructor for JPA
  }

  public JpaEmbeddable(String name) {
    itemName = name;
  }

}

class FieldWithInitializer {
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
