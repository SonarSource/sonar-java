package checks.unused;

import javax.inject.Inject;
import javax.annotation.Nullable;
import javax.annotation.CheckForNull;

class UnusedPrivateFieldCheckWithIgnoredAnnotation {

  @Inject
  private int unusedFieldAnnotationIsIgnored; // Noncompliant {{Remove this unused "unusedFieldAnnotationIsIgnored" private field.}}
//            ^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^

  @Nullable
  private Object unusedFieldAnnotationIsAlsoIgnored; // Noncompliant {{Remove this unused "unusedFieldAnnotationIsAlsoIgnored" private field.}}
//               ^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^

  @CheckForNull
  private Object unusedFieldAnnotationIsNotIgnored; // Compliant

  @Inject
  @CheckForNull
  private Object unusedFieldOneAnnotationIsNotIgnored; // Compliant
}
