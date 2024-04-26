package checks;

import java.util.Enumeration;

class ForLoopIncrementAndUpdateCheckSample {

  Object foo() {
    int i = 0, j = 0, k = 0, l = 0;
    A a = new A(), p, h = new A();
    int[] m = new int[1000];

    for (i = 0; i< 10; j++, m[0]++) { // Noncompliant {{Move the update of "i" into this loop's update clause.}}
//  ^^^
      i++;
//  ^^^<
    }
    for (i = 0; i< 10; j++) { // Compliant - i is updated multiple times
      if (blah()) {
        i++;
      } else {
        i += 14;
      }
    }

    for (i = 0; i< 10; i++, j++) {} // Compliant
    for (i = 0; i< 10; i++) {} // Compliant
    for (i = 0; k< 10 && l<10 ; l++) {} // Compliant condition is using one of the incrementer
    for (i = 0; k< 10 && l<10 ; i++, j++, l++) {} // Compliant
    for (i = 0; k< 10 ; i++, j++, l++) {} // Compliant
    for (i = 0; k< 10 && l<10; i++, j++) { // Noncompliant {{Move the update of "k" into this loop's update clause.}}
      l -= 42;
      k++;
//  ^^^<
    }
    for (i = 0; k< 10 && l<10; i++, j++) { // Noncompliant {{Move the update of "k","l" into this loop's update clause.}}
      l--;
//  ^^^<
      k++;
//  ^^^<
    }
    for (i = 0; i< 10; i+= 2) {} // Compliant
    for (i = 0; i< 10; a.myField+= 2) {} // Compliant
    for (i = 0; i< 10; a.myField++) {} // Compliant
    for (i = 0; a.myField < 10; a.myField++) {} // Compliant
    for (i = 0; a.myField < 10; j++) { // Noncompliant {{Move the update of "myField" into this loop's update clause.}}
      (a.myField)++;
    }

    for (i = 0; j < 10 && l < 10 && i < 50; k++) { // Compliant
      m[0]++;
      m[l++] = 0;
      if (blah()) {
        i++;
      }
    }

    for(i = 0; i<10; foo()) {}
    for (; !a.foo(); l++) {} // Compliant - no update in body
    for (; blah(); l++) {} // Compliant - no update in body
    for (; (p = h.next()) != null; h = p) {} // Compliant
    for (; new A().foo(); l++) {} // Compliant

    for (Enumeration serverIds = a.next().keys(); serverIds.hasMoreElements();) {} // Compliant
    for (ExecutionState current = state; !current.equals(this); current = current.parentState) {} // Compliant
    for (Integer integer = 0; integer.longValue() < 100L ; integer += 1) {} // Compliant
    for (int inc = 2; ; inc++) { // Compliant
      if (someCondition(inc)) {
        return something;
      }
    }
  }

  private boolean blah() { return true; }
  private boolean someCondition(int i) { return true; }

  Object something;
  ExecutionState state;

  class ExecutionState {
    ExecutionState parentState;
  }

  class A {
    int myField;
    boolean foo() { return true; }
    A next() { return null; }
    Enumeration keys() { return null; }
  }
}
