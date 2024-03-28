package checks;

import javax.annotation.CheckForNull;

abstract class PrimitivesMarkedNullableCheckSampleParent {

  @CheckForNull
  abstract int getInt0(); // Noncompliant

}
