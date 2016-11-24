class A {
  void null_assigned() {
    Object a = null; // flow@fl1 {{}}
    a.toString(); // Noncompliant [[flows=fl1]]
  }

  void reassignement() {
    Object a = null; // flow@reass {{}}
    Object b = new Object();
    b = a; // flow@reass {{}}
    b.toString(); // Noncompliant [[flows=reass]]
  }

  void relationshipLearning(Object a) {
    if (a == null) { // flow@rela {{}}
      a.toString(); // Noncompliant [[flows=rela]]
    }
  }

  void combined(Object a) {
    Object b = new Object();
    if (a == null) { // flow@comb {{}}
      b = a; // flow@comb {{}}
      b.toString(); // Noncompliant [[flows=comb]]
    }
  }

  void complexRelation(int a, int b, Object c) {
    if (a < b) { // This should be reported as well to highlight context
      c = null; // flow@cplx {{}}
    }
    System.out.println("");
    if (b > a) { // This should be reported as well to highlight context
      c.toString(); // Noncompliant [[flows=cplx]]
    }
  }

  void recursiveRelation(Object a, Object b) {
    if ((a == null) == true) { // flow@rec {{}}
      b = a; // flow@rec {{}}
      b.toString(); // Noncompliant [[flows=rec]]
    }
  }

  void Xproc(boolean a) {
    if (a) {
      Object b = // flow@xproc
        foo(a); // flow@xproc
      b.toString(); // Noncompliant [[flows=xproc]]
    }
  }

  private Object foo(boolean a) {
    if (a) {
      return null;
    }
    return new Object();
  }
}
