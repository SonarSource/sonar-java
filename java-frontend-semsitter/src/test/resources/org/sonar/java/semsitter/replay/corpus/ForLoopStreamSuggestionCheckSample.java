package checks;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Queue;
import java.util.Set;
import java.util.concurrent.LinkedBlockingQueue;

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

  void offerSingleArg() {
    LinkedList<String> result = new LinkedList<>();
    for (String item : items) { // Noncompliant
//  ^^^
      result.offer(item);
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

  void ifWithNonExpressionBody() {
    List<String> result = new ArrayList<>();
    for (String item : items) { // compliant - if body is a return statement, not expression
      if (item != null)
        return;
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

  void filterCollectPositionalAdd() {
    List<String> result = new ArrayList<>();
    for (String item : items) { // compliant - add(int, E) inside filter has no stream equivalent
      if (item != null) {
        result.add(0, item);
      }
    }
  }

  void positionalAdd() {
    List<String> result = new ArrayList<>();
    for (String item : items) { // compliant - add(int, E) is positional insert, no stream equivalent
      result.add(0, item);
    }
  }

  void offerWithTimeout() throws InterruptedException {
    LinkedBlockingQueue<String> result = new LinkedBlockingQueue<>();
    for (String item : items) { // compliant - offer(e, timeout, unit) has 3 args
      result.offer(item, 1, java.util.concurrent.TimeUnit.SECONDS);
    }
  }

  void addWithoutMemberSelect() {
    List<String> result = new ArrayList<>();
    for (String item : items) { // compliant - add is a plain method call, not member select
      add(item);
    }
  }

  private void add(String item) { }

  void assignmentNotAdd() {
    int[] arr = new int[10];
    int i = 0;
    for (String item : items) { // compliant - array assignment, not collection add
      arr[i++] = item.length();
    }
  }

  void collectFromExistingCollection() {
    List<String> result = items;
    for (String item : items) { // compliant - result is not a fresh collection
      result.add(item);
    }
  }

  void collectFromCopiedCollection() {
    List<String> result = new ArrayList<>(items);
    for (String item : items) { // compliant - result is pre-populated, not empty
      result.add(item);
    }
  }

  void selfModifyingLoop() {
    List<String> result = new ArrayList<>();
    result.add("seed");
    for (String item : result) { // compliant - source and target are the same collection
      result.add(item);
    }
  }

  void preLoopMutation() {
    List<String> result = new ArrayList<>();
    result.add("seed");
    for (String item : items) { // compliant - collection is mutated before the loop
      result.add(item);
    }
  }

  void offerToQueueInterface() {
    Queue<String> result = new LinkedList<>();
    for (String item : items) { // Noncompliant
//  ^^^
      result.offer(item);
    }
  }

  void addLastToDequeInterface() {
    Deque<String> result = new ArrayDeque<>();
    for (String item : items) { // Noncompliant
//  ^^^
      result.addLast(item);
    }
  }

  void offerLastToDequeInterface() {
    Deque<String> result = new ArrayDeque<>();
    for (String item : items) { // Noncompliant
//  ^^^
      result.offerLast(item);
    }
  }

  interface CustomCollectionLike {
    void add(int metric);
  }

  void unrelatedAddOverload(CustomCollectionLike tracker) {
    List<String> result = new ArrayList<>();
    for (String item : items) { // compliant - tracker.add(int) is not Collection.add
      tracker.add(item.length());
    }
  }

  static class Metrics extends ArrayList<String> {
    void add(int metric) { }
  }

  void nonCollectionAddOverload() {
    Metrics result = new Metrics();
    for (String item : items) { // compliant - add(int) is Metrics' own overload, not Collection.add(E)
      result.add(item.length());
    }
  }

  void newCollectionWithInitialCapacity() {
    List<String> result = new ArrayList<>(16);
    for (String item : items) { // Noncompliant
//  ^^^
      result.add(item);
    }
  }

  void assignmentBeforeLoop() {
    List<String> result = new ArrayList<>();
    int x = 0;
    x = 42;
    for (String item : items) { // Noncompliant
//  ^^^
      result.add(item);
    }
  }

  void chainedMethodTarget() {
    List<String> result = new ArrayList<>();
    for (String item : items) { // compliant - target is method call return, not a tracked variable
      getList().add(item);
    }
  }

  private List<String> getList() { return items; }

  void staticMethodCallBeforeLoop() {
    List<String> result = new ArrayList<>();
    System.out.println("start");
    for (String item : items) { // Noncompliant
//  ^^^
      result.add(item);
    }
  }

  void fieldTargetNotTracked() {
    for (String item : items) { // compliant - this.items is a field, not a local fresh collection
      this.items.add(item);
    }
  }

}
