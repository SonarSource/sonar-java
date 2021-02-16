package org.sonar.trial;

import org.sonar.trial.NoWayOutLoop.TextIterator;

import java.util.Enumeration;
import java.util.Iterator;
import java.util.Map;

public class NoWayOutLoop {

  private boolean loopExit;

  void badForLoop() {
    for (;;) { // Noncompliant {{Add an end condition to this loop.}}
      doSomething();
    }
  }

  void okForLoop() {
    for (;;) { // Compliant: explicit exit
      doSomething();
      if (canExit()) {
        break;
      }
      doSomeOtherThing();
    }
  }

  void badWhileLoop() {
    int j = 0;
    while (true) { // Noncompliant {{Add an end condition to this loop.}}
      j++;
    }
  }

  void badWhileLoopWithVariable() {
    boolean condition = true;
    if (condition) {
    }
    while((condition)) { // Noncompliant
    }
  }

  void okWhileLoop() {
    int j = 0;
    while (true) { // Compliant: explicit exit
      j++;
      if (canExit()) {
        break;
      }
    }
  }

  void okWhileLoopWithVariable() {
    boolean condition = true;
    while(condition) {
      condition = false;
    }
  }

  void returnWhileLoop() {
    int j = 0;
    while (true) { // Compliant: explicit exit
      j++;
      if (canExit()) {
        return;
      }
    }
  }

  void doubleReturnWhileLoop() {
    int j = 0;
    while (true) { // Compliant: explicit exit
      j++;
      if (canExit()) {
        return;
      }
      while (true) {
        j++;
        if (canExit()) {
          return;
        }
      }
    }
  }

  void okVariableWhileLoop() {
    loopExit = true;
    while (loopExit) { // Compliant: loopExit may be changed in doSomething()
      doSomething();
    }
  }

  void unreachableCount() {
    for (int i = 0; i < 10; i--) { // Noncompliant {{Correct this loop's end condition.}}
      doSomething();
    }
  }

  void unreachableCountReversedCondition() {
    for (int i = 0; 10 > i; i--) { // Noncompliant {{Correct this loop's end condition.}}
      doSomething();
    }
  }

  void normalCount() { // Compliant.
    for (int i = 0; i < 10; i++) {
      doSomething();
    }
  }

  void incrementForever() {
    for (int i = 100; i > 10; i++) { // Noncompliant {{Correct this loop's end condition.}}
      doSomething();
    }
  }

  void decrementUntilZero() {
    for (int i = 100; i >= 0; i--) { // Compliant
      doSomething();
    }
  }

  void reachableCountAssigned() {
    for (int i = 0; i < 10; i += 1, normalCount()) { // Compliant
      doSomething();
    }
  }

  void unreachableCountAssigned() {
    for (int i = 0; i < 10; i -= 1, normalCount()) { // Noncompliant {{Correct this loop's end condition.}}
      doSomething();
    }
  }

  void normalCountAssigned() {
    for (int i = 0; i < 10; i+=1) { // Compliant.
      doSomething();
    }
  }

  void notEqualCondition() {
    for (int i = 0; i != 10; i+=1) { // Compliant.
      doSomething();
    }
  }

  void for_loop_condition() {
    a = null;
    for (; a != null;) { // Compliant: break is there
      a.toString();
      break;
    }
  }
  
  void emptyBranch() {
    for (int i = 1;      ; ) {} // Noncompliant {{Add an end condition to this loop.}}
  }
  
  void unsupportedUpdate() {
    for (n = len; n < size - n; n <<= 1) { // Compliant: shift operand not supported
      doSomething();
    }
  }
  
  void fullExpressionUpdate() {
    String a = "aaaaa";
    for(int i = 0; i < a.length; i = i + 3) { // Compliant: loop variable is reassigned
      doSomething();
    }
  }
  
  void indirectUpdate() {
    int[] spine = {1,2,3,4};
    int[] next = {3,4,2,1}; 
    for (int i = spine[index]; i >= 0; i = next[i]) { // Compliant: loop variable is reassigned
      doSomething();
    }
  }
  
  void reassignedValue() {
    for (int count = listeners, i = 0; count > 0; i++) { // Compliant: condition is modified within loop body
      count = next();
    }
  }
  
  private void enumerationLoop() {
    for (Enumeration e = keys() ; e.hasMoreElements() ; ) {
      doSomething(e.nextElement());
    }
  }
  
  private void iteratorLoop() {
    for (Iterator iterator = values(); iterator.hasNext();) {
      doSomething(iterator.next());
    }
  }

  private void doSomething() {}

  private void doSomething(Object obj) {}

  private void doSomeOtherThing() {}

  private boolean canExit() {
    return false;
  }

  private int next() {
    return 0;
  }
}

public class UnsupportedYet {

  void unreachableCount() {
    for (int i = 0; i < 10; i--) { // Noncompliant {{Correct this loop's end condition.}}
      doSomething();
    }
  }
  
}

public class Coverage {

  int n;

  static void otherPlusAssignment() {
    Coverage cov = new Coverage();
    cov.n = 0;
    for (int i = 1; i < 10; i += 1, cov.n += 1) {
    }
  }

  static void otherMinusAssignment() {
    Coverage cov = new Coverage();
    cov.n = 0;
    for (int i = 1; i < 10; i += 1, cov.n -= 1) {
    }
  }

  static void otherIncrement() {
    Coverage cov = new Coverage();
    cov.n = 0;
    for (int i = 1; i < 10; ++i, cov.n++) {
    }
  }

  static void otherDecrement() {
    Coverage cov = new Coverage();
    cov.n = 0;
    for (int i = 1; i < 10; ++i, cov.n--) {
      cov.n = -cov.n;
    }
  }

  static void otherUnary() {
    for (int i = 1; i < 10; ++i) {
      Integer.valueOf(-i);
    }
  }
  
  static void whileFalse() {
    while(false) {
      doSomething();
    }
  }
  
  static void whileVariable() {
    boolean condition = true;
    while(condition) { // Noncompliant
      doSomething();
    }
  }

}

public class MyThread extends Thread {
  @Override
  public void run() {
    NoWayOutLoop loop = new NoWayOutLoop();
    for (;;) {
      loop.doSomething();
    }
  }
}

public class Ruling {
  private final Map<K, Long> map;
  public long put(K key, long newValue) {
    outer: for (;;) {
      AtomicLong atomic = map.get(key);
      if (atomic == null) {
        return 0L;
      }
      for (;;) {
        long oldValue = atomic.get();
        if (oldValue == 0L) {
          if (map.replace(key, atomic, new Long(newValue))) { 
            return 0L;
          }
          continue outer;
        }
      }
    }
  }
  
  public void incrementWithinLoopBody(bolean[] isSet) {
    for (int i = 0; i < isSet.length; ) {
      isSet[i++] = false;
    }
  }
  
  public void autoIncrementInCondition(int n) {
    for (int i = 0; i++ < n;) {
      doSomething();
    }
  }
  
  public void autoDecrementInCondition() {
    for (int i = 10; i-- > 0;) {
      doSomething();
    }
  }
  
  public void autoIncrementInMethod(String text) {
    char c;
    for (int i = 0; i < 10;) {
      c = text.charAt(i++);
    }
  }
  
  public void autoDecrementInMethod(String text) {
    char c;
    for (int i = 10; i > 0;) {
      c = text.charAt(--i);
    }
  }
  
  public void complexCondition(int[] values) {
    for (int i = 0; values[i] < 100; i++) {
      doSomething();
    }
  }
  
  public void mixedVariation(String text) {
    char c;
    for (int i = 10; i > 0;) {
      if(canExit()) {
        c = text.charAt(--i);
      } else {
        c = text.charAt(i++);
      }
    }
  }
  
  private void updateThroughMethod(TextIterator text, int endIndex) {
    for (char c = text.current(); text.getIndex() < endIndex; c = text.next()) {
      doSomething(c);
    }
  }

  private static interface TextIterator {
    char current();
    int getIndex();
    char next();
  }

}
