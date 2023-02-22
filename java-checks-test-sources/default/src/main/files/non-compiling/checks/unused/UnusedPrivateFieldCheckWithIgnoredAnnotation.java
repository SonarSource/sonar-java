package checks.unused;

import javax.inject.Inject;

class UnusedPrivateFieldCheckWithIgnoredAnnotation {

  @Inject
  private int unusedField; // Noncompliant [[sc=15;ec=26]] {{Remove this unused "unusedField" private field.}}

}
