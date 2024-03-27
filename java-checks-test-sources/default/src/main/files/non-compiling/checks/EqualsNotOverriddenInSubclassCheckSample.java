package checks;

class EqualsNotOverriddenInSubclassCheckSample {

  class A {
    public boolean equals(Object obj) {
      return true;
    }
  }

  class B extends A { // Noncompliant
    String s2;
  }


  class K extends com.tst.UnknownClass {
    String s;
  }

  class L<T> extends com.tst.MyList<T> {
    int s;
  }

}


