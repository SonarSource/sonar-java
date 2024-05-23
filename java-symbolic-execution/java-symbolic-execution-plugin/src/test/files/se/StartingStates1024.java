package org.foo;

import javax.annotation.Nullable;

public class A {
  /**
   * There is 10 parameters annotated with @Nullable,
   * which would generate 1,024 (2^10) starting states
   */
  public String methodWithLotsOfParameters(
    @Nullable String one,
    @Nullable String two,
    @Nullable String three,
    @Nullable String four,
    @Nullable String five,
    @Nullable String six,
    @Nullable String seven,
    @Nullable String eight,
    @Nullable String nine,
    @Nullable String ten) {
    return "Hello world.";
  }
}
