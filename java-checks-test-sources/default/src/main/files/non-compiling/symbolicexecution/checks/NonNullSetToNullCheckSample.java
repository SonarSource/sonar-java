package symbolicexecution.checks;

import javax.annotation.Nonnull;

class NonNullSetToNullCheckSample {

  @Nonnull
  private String primary;
  private String secondary;

  @Nonnull
  private static final Integer STATIC_FIELD; // Compliant as static fields are not reported
  private static final Integer STATIC_FIELD_NO_ANNOTATION;

  // ============ 1.1 Fields not initialized in constructor ============
  public NonNullSetToNullCheckSample(@Nonnull String color, String other) {
    primary = color;
    secondary = other;
    unknownTestObject = new Object();
  }

  // ============ 2. Testing Return values ============
  @Nonnull
  public String colorMix() {
    return null; // Noncompliant
  }

  @Nonnull
  public String unknownMix() {
    String mix = getUnknownColor();
    return mix;  // Compliant, coming from unknown method.
  }

}
