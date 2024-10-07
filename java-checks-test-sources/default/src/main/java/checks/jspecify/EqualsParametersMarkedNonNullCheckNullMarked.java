package checks.jspecify;

import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.NullUnmarked;

@NullMarked
class EqualsParametersMarkedNonNullCheckSampleA {

  // @NullUnmarked not applicable to parameter
  public boolean equals(Object obj) { // Compliant
    return true;
  }
}

@NullMarked
class EqualsParametersMarkedNonNullCheckSampleB {

  @NullUnmarked
  // @NullUnmarked not applicable to parameter
  public boolean equals(Object obj) { // Compliant
    return true;
  }
}
