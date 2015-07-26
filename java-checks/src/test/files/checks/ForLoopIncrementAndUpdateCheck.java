class A {
  
  void foo() {
    int i,j,k,l = 0; A a = new A();
    for (i = 0; i< 10; j++) { // Noncompliant {{This loop's stop condition tests "i" but the incrementer updates "j".}}
    }

    for (i = 0; i< 10; i++) { // Compliant
    }

    for (i = 0; k< 10 && l<10 ; l++) { // Compliant condition is using one of the incrementer
    }
    for (i = 0; k< 10 && l<10 ; i++, j++, l++) { // Compliant
    }
    for (i = 0; k< 10 ; i++, j++, l++) { // Noncompliant {{This loop's stop condition tests "k" but the incrementer updates "i, j, l".}}
    }
    for (i = 0; k< 10 && l<10; i++, j++) { // Noncompliant {{This loop's stop condition tests "k, l" but the incrementer updates "i, j".}}
    }
    for (i = 0; k< 10; i+= 2) { // Noncompliant {{This loop's stop condition tests "k" but the incrementer updates "i".}}
    }
    for (i = 0; i< 10; i+= 2) { // Compliant
    }
    for (i = 0; i< 10; a.myField+= 2) { // Noncompliant {{This loop's stop condition tests "i" but the incrementer updates "myField".}}
    }
    for (i = 0; i< 10; a.myField++) { // Noncompliant {{This loop's stop condition tests "i" but the incrementer updates "myField".}}
    }

    for(i = 0; i<10; foo()){}

    for (; !a.foo(); l++) {} // Noncompliant {{This loop's stop condition tests "a" but the incrementer updates "l".}}
    for (; foo(); l++) {} // Noncompliant {{This loop's stop condition tests "foo()" but the incrementer updates "l".}}
    // bar is unknown
    for (; bar(); l++) {} // Noncompliant {{This loop's stop condition tests "bar" but the incrementer updates "l".}}
    for (; (p=h.next) !=null; h = p) {} // Compliant 
    for (; new A().foo(); l++) {} // Noncompliant {{This loop's stop condition tests "foo()" but the incrementer updates "l".}}
    
    for (Enumeration serverIds = db.serverTable.keys(); serverIds.hasMoreElements();){} // Compliant
    for (ExecutionState current = state; !current.equals(this); current = current.parentState) {} // Compliant
    for (Integer integer = 0; integer.longValue() < 100L ; integer += 1) {} // Compliant
  }
  
  int myField;
  
  class ExecutionState {
    ExecutionState parentState;
  }
}

