package org.foo.bar;

import javax.annotation.Nonnull;
import javax.annotation.ParametersAreNullableByDefault;

@ParametersAreNullableByDefault
class ClassAnnotation {
  void foo(Object input) { // flow@npe [[sc=19;ec=24]] {{Implies 'input' can be null.}}
    // Noncompliant@+1 [[flows=npe]] {{A "NullPointerException" could be thrown; "input" is nullable here.}}
    input.toString(); // flow@npe {{'input' is dereferenced.}}
  }

  private Integer bar2(Integer i) { return i; }
  private void qix2(Integer i) {
    bar2(i).intValue(); // Noncompliant {{A "NullPointerException" could be thrown; "bar2()" can return null.}}
  }

  private void gul(@Nonnull Object o) {
    o.toString(); // Compliant -  @Nonnull annotation discard @ParametersAreNullableByDefault annotation
  }
}
