package checks;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

class Fields {
  private static final Set<String> UNKNOWN_SET = unknownMethod(new HashSet<>());

  public Set<String> unknownSet() {
    return UNKNOWN_SET; // Compliant
  }

}
