package checks.jspecify.nullmarked;

import org.jspecify.annotations.NullUnmarked;

// NullMarked at the package level
class EqualsParametersMarkedNonNullCheckSampleA {

  // @NullUnmarked not applicable to parameter
  public boolean equals(Object obj) { // Compliant
    return true;
  }
}

// NullMarked at the package level
class EqualsParametersMarkedNonNullCheckSampleB {

  @NullUnmarked
  // @NullUnmarked not applicable to parameter
  public boolean equals(Object obj) { // Compliant
    return true;
  }
}
