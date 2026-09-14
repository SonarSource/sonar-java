package checks;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;

class ForLoopStreamSuggestionCheckSample {

  private List<String> items = new ArrayList<>();

  void simpleCollect() {
    List<String> result = new ArrayList<>();
    for (String item : items) { // Noncompliant {{Use a stream instead of this loop.}}
//  ^^^
      result.add(item);
    }
  }

  void filterCollect() {
    List<String> result = new ArrayList<>();
    for (String item : items) { // Noncompliant
//  ^^^
      if (item != null) {
        result.add(item);
      }
    }
  }

  void filterCollectUnbraced() {
    List<String> result = new ArrayList<>();
    for (String item : items) { // Noncompliant
//  ^^^
      if (item.length() > 3)
        result.add(item);
    }
  }

  void simpleCollectUnbraced() {
    List<String> result = new ArrayList<>();
    for (String item : items) // Noncompliant
//  ^^^
      result.add(item);
  }

  void collectToSet() {
    Set<String> result = new HashSet<>();
    for (String item : items) { // Noncompliant
//  ^^^
      result.add(item);
    }
  }

  void collectToLinkedList() {
    LinkedList<String> result = new LinkedList<>();
    for (String item : items) { // Noncompliant
//  ^^^
      result.add(item);
    }
  }

  void addLastMethod() {
    LinkedList<String> result = new LinkedList<>();
    for (String item : items) { // Noncompliant
//  ^^^
      result.addLast(item);
    }
  }

  void offerLastMethod() {
    LinkedList<String> result = new LinkedList<>();
    for (String item : items) { // Noncompliant
//  ^^^
      result.offerLast(item);
    }
  }

  // === Compliant cases ===

  void addFirstMethod() {
    LinkedList<String> result = new LinkedList<>();
    for (String item : items) { // compliant - addFirst reverses order, not equivalent to stream
      result.addFirst(item);
    }
  }

  void offerFirstMethod() {
    LinkedList<String> result = new LinkedList<>();
    for (String item : items) { // compliant - offerFirst reverses order, not equivalent to stream
      result.offerFirst(item);
    }
  }

  void variableWithoutInitializer() {
    List<String> result;
    result = new ArrayList<>();
    for (String item : items) { // compliant - variable has no initializer at declaration
      result.add(item);
    }
  }

  void multipleStatementsInLoop() {
    List<String> result = new ArrayList<>();
    for (String item : items) { // compliant - multiple statements
      System.out.println(item);
      result.add(item);
    }
  }

  void ifElseBranch() {
    List<String> result = new ArrayList<>();
    for (String item : items) { // compliant - else branch present
      if (item != null) {
        result.add(item);
      } else {
        result.add("default");
      }
    }
  }

  void breakInLoop() {
    List<String> result = new ArrayList<>();
    for (String item : items) { // compliant - multi-statement with break
      if (item == null) break;
      result.add(item);
    }
  }

  void continueInLoop() {
    List<String> result = new ArrayList<>();
    for (String item : items) { // compliant - multi-statement with continue
      if (item == null) continue;
      result.add(item);
    }
  }

  void returnInLoop() {
    List<String> result = new ArrayList<>();
    for (String item : items) { // compliant - multi-statement with return
      if (item == null) return;
      result.add(item);
    }
  }

  void nestedLoopBody() {
    List<String> result = new ArrayList<>();
    for (String item : items) { // compliant - nested loop
      for (char c : item.toCharArray()) {
        result.add(String.valueOf(c));
      }
    }
  }

  void localCollectionInLoop() {
    for (String item : items) { // compliant - collection declared inside loop
      List<String> temp = new ArrayList<>();
      temp.add(item);
    }
  }

  void noCollectionDeclared() {
    for (String item : items) { // compliant - no collection variable before loop
      System.out.println(item);
    }
  }

  void emptyLoop() {
    List<String> result = new ArrayList<>();
    for (String item : items) { // compliant - empty body
    }
  }

  void traditionalForLoop() {
    List<String> result = new ArrayList<>();
    for (int i = 0; i < 10; i++) { // compliant - traditional for loop, not for-each
      result.add(String.valueOf(i));
    }
  }

  void nonCollectionTarget() {
    StringBuilder sb = new StringBuilder();
    for (String item : items) { // compliant - StringBuilder is not a Collection
      sb.append(item);
    }
  }

  void methodCallNotAdd() {
    List<String> result = new ArrayList<>();
    for (String item : items) { // compliant - not an add method
      result.remove(item);
    }
  }

  void noCollectionBeforeLoop(List<String> result) {
    for (String item : items) { // compliant - result is a parameter, not declared in the block
      result.add(item);
    }
  }

  void assignmentNotAdd() {
    int[] arr = new int[10];
    int i = 0;
    for (String item : items) { // compliant - array assignment, not collection add
      arr[i++] = item.length();
    }
  }
}
