package org.foo.bar;

import javax.annotation.CheckForNull;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

abstract class JetBrains {
  void foo(@NotNull Object input) { // flow@cof [[sc=28;ec=33]] {{Implies 'input' can not be null.}}
    // Noncompliant@+1 [[flows=cof]] {{Change this condition so that it does not always evaluate to "false"}}
    if (input == null) {} // flow@cof {{Expression is always false.}}
  }

  void kom(@Nullable Object input) { // flow@npe [[sc=29;ec=34]] {{Implies 'input' can be null.}}
    // Noncompliant@+1 [[flows=npe]] {{A "NullPointerException" could be thrown; "input" is nullable here.}}
    input.toString(); // flow@npe {{'input' is dereferenced.}}
  }

  private Integer bar2(@Nullable Integer i) { return i; }
  private void qix2(Integer i) {
    bar2(i).intValue(); // Noncompliant {{A "NullPointerException" could be thrown; "bar2()" can return null.}}
  }

  private void gul(@NotNull Object o) {
    o.toString(); // Compliant
  }

  @Nullable
  abstract Object tip();
  @CheckForNull
  abstract Object tip2();

  private void top() {
    tip().toString(); // Compliant - only @CheckForNull is handled 
    tip2().toString(); // Noncompliant {{A "NullPointerException" could be thrown; "tip2()" can return null.}}
  }

}
