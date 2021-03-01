package checks;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

class Fields {
  private static final List<String> MODIFIABLE = new ArrayList<>();
  private static final List<Integer> IMMUTABLE_LIST = List.of(1, 2, 3);
  private static final List<Integer> IMMUTABLE_COPY_LIST = List.copyOf(new ArrayList<>());
  private static final Set<String> IMMUTABLE_SET = Set.of("a");
  private static final Set<String> IMMUTABLE_COPY_SET = Set.copyOf(new HashSet<>());
  private static final Set<String> UNKNOWN_SET = unknownMethod(new HashSet<>());

  public List<Integer> immutableList() {
    return IMMUTABLE_LIST; // Compliant
  }

  public List<Integer> immutableListCopy() {
    return IMMUTABLE_COPY_LIST; // Compliant
  }

  public Set<String> immutableSet() {
    return IMMUTABLE_SET; // Compliant
  }

  public Set<String> immutableSetCopy() {
    return IMMUTABLE_COPY_SET; // Compliant
  }

  public List<String> modifiable() {
    return MODIFIABLE; // Noncompliant
  }

  public Set<String> unknownSet() {
    return UNKNOWN_SET; // Compliant
  }

}
