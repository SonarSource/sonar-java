package checks.unused;

import javax.inject.Inject;
import javax.annotation.Nullable;
import javax.annotation.CheckForNull;

class UnusedPrivateFieldCheckWithIgnoredAnnotation {

  @Inject
  private int unusedFieldAnnotationIsIgnored; // Noncompliant [[sc=15;ec=45]] {{Remove this unused "unusedFieldAnnotationIsIgnored" private field.}}

  @Nullable
  private Object unusedFieldAnnotationIsAlsoIgnored; // Noncompliant [[sc=18;ec=52]] {{Remove this unused "unusedFieldAnnotationIsAlsoIgnored" private field.}}

  @CheckForNull
  private Object unusedFieldAnnotationIsNotIgnored; // Compliant

  @Inject
  @CheckForNull
  private Object unusedFieldOneAnnotationIsNotIgnored; // Compliant
}
