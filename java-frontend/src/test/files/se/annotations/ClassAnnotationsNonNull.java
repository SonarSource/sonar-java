package org.foo.bar;

import javax.annotation.Nullable;
import javax.annotation.ParametersAreNonnullByDefault;

@ParametersAreNonnullByDefault
class ClassAnnotation {
  void foo(Object input) { // flow@cof [[sc=19;ec=24]] {{Implies 'input' can not be null.}}
    // Noncompliant@+1 [[flows=cof]] {{Change this condition so that it does not always evaluate to "false"}}
    if (input == null) {} // flow@cof {{Expression is always false.}}
  }

  private void bar(Object... objects) {
    objects.toString();
  }

  void qix(@Nullable Object singleObject, @Nullable Object[] objects) {
    bar(singleObject);
    if (singleObject != null) {} // Compliant

    bar(objects);
    if (objects != null) {} // Noncompliant {{Remove this expression which always evaluates to "true"}}
  }
}
