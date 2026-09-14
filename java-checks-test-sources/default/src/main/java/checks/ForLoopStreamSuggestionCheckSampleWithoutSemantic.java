package checks;

import java.util.ArrayList;
import java.util.List;

class ForLoopStreamSuggestionCheckSampleWithoutSemantic {

  private List<String> items = new ArrayList<>();

  void simpleCollect() {
    List<String> result = new ArrayList<>();
    for (String item : items) { // Noncompliant
      result.add(item);
    }
  }

  void filterCollect() {
    List<String> result = new ArrayList<>();
    for (String item : items) { // Noncompliant
      if (item != null) {
        result.add(item);
      }
    }
  }
}
