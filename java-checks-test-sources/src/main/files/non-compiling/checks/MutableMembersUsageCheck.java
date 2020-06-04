package checks;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

class Fields {
  private static final List<String> MODIFIABLE = new ArrayList<>();
  private static final List<Integer> IMMUTABLE_LIST = List.of(1, 2, 3);
  private static final Set<String> IMMUTABLE_SET = Set.of("a");

  public List<Integer> immutableList() {
    return IMMUTABLE_LIST;
  }

  public Set<String> immutableSet() {
    return IMMUTABLE_SET;
  }

  public List<String> modifiable() {
    return MODIFIABLE; // Noncompliant
  }

}

