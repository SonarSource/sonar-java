package org.sonar.java.resolve.targets;

import javax.annotation.Nullable;

class PackageAnnotations {
  void foo(Object input) {
    if (input == null) {} // Noncompliant {{Change this condition so that it does not always evaluate to "false"}}
  }

  private void bar(Object... objects) {
    objects.toString();
  }

  void qix(@Nullable Object singleObject, @Nullable Object[] objects) {
    bar(singleObject);
    if (singleObject != null) {} // Compliant

    bar(objects);
    if (objects != null) {} // Noncompliant {{Change this condition so that it does not always evaluate to "true"}}
  }
}
