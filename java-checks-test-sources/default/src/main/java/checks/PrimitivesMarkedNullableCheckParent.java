package checks;

import javax.annotation.CheckForNull;

abstract class PrimitivesMarkedNullableCheckParent {

  @CheckForNull
  abstract int getInt0(); // Noncompliant

}
