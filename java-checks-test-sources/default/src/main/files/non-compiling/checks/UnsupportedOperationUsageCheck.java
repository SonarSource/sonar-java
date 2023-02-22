package checks;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

class Fields {

  public void add() {
    List<String> strings;
    strings = Arrays.asList("test1", "test2");
    strings.get(0); // Compliant
  }
}
