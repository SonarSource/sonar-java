package checks;

import java.util.ArrayList;
import java.util.List;

class ForLoopStreamSuggestionCheckSampleWithoutSemantic {

  private List<String> items = new ArrayList<>();

  void noCollectionDeclared() {
    for (String item : items) { // compliant - no collection variable before loop
      System.out.println(item);
    }
  }

  void multipleStatementsInLoop() {
    List<String> result = new ArrayList<>();
    for (String item : items) { // compliant - multiple statements
      System.out.println(item);
      result.add(item);
    }
  }
}
