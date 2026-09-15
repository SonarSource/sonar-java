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

  void simpleCollectWithoutSemantic() {
    List<String> result = new ArrayList<>();
    for (String item : items) { // Noncompliant {{Use a stream instead of this loop.}}
//  ^^^
      result.add(item);
    }
  }

  void filterCollectWithoutSemantic() {
    List<String> result = new ArrayList<>();
    for (String item : items) { // Noncompliant {{Use a stream instead of this loop.}}
//  ^^^
      if (item != null) {
        result.add(item);
      }
    }
  }
}
