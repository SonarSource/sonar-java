package org.foo;

import javax.annotation.Nullable;

public class A {
  /**
   * There is 24 parameters annotated with @Nullable,
   * which would generate 16,777,216 (2^24) starting states
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
    @Nullable String ten,
    @Nullable String eleven,
    @Nullable String twelve,
    @Nullable String thirteen,
    @Nullable String fourteen,
    @Nullable String fifteen,
    @Nullable String sixteen,
    @Nullable String seventeen,
    @Nullable String eighteen,
    @Nullable String nineteen,
    @Nullable String twenty,
    @Nullable String twentyone,
    @Nullable String twentytwo,
    @Nullable String twentythree,
    @Nullable String twentyfour) {
    return "Hello world.";
  }
}
