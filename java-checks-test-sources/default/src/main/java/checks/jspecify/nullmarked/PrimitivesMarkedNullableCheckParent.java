package checks.jspecify.nullmarked;

import org.jspecify.annotations.NullMarked;

abstract class PrimitivesMarkedNullableCheckParent {

  @NullMarked
  abstract int getInt0(); // Noncompliant

}
