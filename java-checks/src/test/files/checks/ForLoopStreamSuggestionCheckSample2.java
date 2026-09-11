import java.util.ArrayList;
import java.util.List;

class ForLoopStreamSuggestionCheckSample {

  void simpleFilter() {
    List<String> result = new ArrayList<>();
    for (String item : items) { // Noncompliant
      if (item != null) {
        result.add(item);
      }
    }
  }

  private List<String> items = new ArrayList<>();
}
