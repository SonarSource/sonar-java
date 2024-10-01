package checks.jspecify;

import org.jspecify.annotations.NullMarked;

abstract class PrimitivesMarkedNullableCheckNullMarkedParent {

  @NullMarked
  abstract int getInt0(); // Noncompliant

}
