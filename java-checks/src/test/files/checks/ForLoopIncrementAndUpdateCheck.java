class A {

  void foo() {
    int i,j,k,l = 0; A a = new A(); int[] m;
    for (i = 0; i< 10; j++, m[0]++) { // Noncompliant [[sc=5;ec=8;secondary=6]] {{Move the update of "i" into this loop's update clause.}}
      i++;
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
    for (i = 0; k< 10 && l<10; i++, j++) { // Noncompliant [[secondary=23]] {{Move the update of "k" into this loop's update clause.}}
      l -= 42;
      k++;
    }
    for (i = 0; k< 10 && l<10; i++, j++) { // Noncompliant [[secondary=26,27]] {{Move the update of "k","l" into this loop's update clause.}}
      l--;
      k++;
    }
    for (i = 0; i< 10; i+= 2) {} // Compliant
    for (i = 0; i< 10; a.myField+= 2) {} // Compliant
    for (i = 0; i< 10; a.myField++) {} // Compliant
    for (i = 0; a.myField < 10; a.myField++) {} // Compliant
    for (i = 0; a.myField < 10; j++) { // Noncompliant  {{Move the update of "myField" into this loop's update clause.}}
      (a.myField)++;
    }

    for (i = 0; j < 10 && l < 10 && i < 50; k++) { // Compliant
      m[0]++;
      m[l++] = 0;
      if (fooBar()) {
        i++;
      }
      unknown++;
    }

    for(i = 0; i<10; foo()) {}

    for (; !a.foo(); l++) {} // Compliant - no update in body
    for (; foo(); l++) {} // Compliant - no update in body
    // bar is unknown
    for (; bar(); l++) {} // Compliant
    for (; (p=h.next) !=null; h = p) {} // Compliant 
    for (; new A().foo(); l++) {} // Compliant

    for (Enumeration serverIds = db.serverTable.keys(); serverIds.hasMoreElements();){} // Compliant
    for (ExecutionState current = state; !current.equals(this); current = current.parentState) {} // Compliant
    for (Integer integer = 0; integer.longValue() < 100L ; integer += 1) {} // Compliant
    for (int i = 2; ; i++) { // Compliant
      if (someCondition(i)) {
        return something;
      }
    }
  }
  
  int myField;
  
  class ExecutionState {
    ExecutionState parentState;
  }
}

