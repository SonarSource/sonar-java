class A {

  void foo() {
    int i,j,k,l = 0; A a = new A();
    for (i = 0; i< 10; j++) { //Noncompliant
    }

    for (i = 0; i< 10; i++) { //Compliant
    }

    for (i = 0; k< 10 && l<10 ; l++) { //compliant condition is using one of the incrementer
    }
    for (i = 0; k< 10 && l<10 ; i++, j++, l++) { //compliant
    }
    for (i = 0; k< 10 ; i++, j++, l++) { //Noncompliant
    }
    for (i = 0; k< 10 && l<10; i++, j++) { //Noncompliant
    }
    for (i = 0; k< 10; i+= 2) { //Noncompliant
    }
    for (i = 0; i< 10; i+= 2) { //Compliant
    }
    for(Enumeration serverIds = db.serverTable.keys(); serverIds.hasMoreElements();){}

    for(i = 0; i<10; foo()){}

    for (; !a.foo(); l++) {}
    for (; foo(); l++) {}
    for (; bar(); l++) {}
    for (; (p=h.next) !=null; h = p) {}
  }

}