class A {
  void null_assigned() {
    Object a = null;
    a.toString(); // Noncompliant
  }

  void reassignement() {
    Object a = null;
    Object b = new Object();
    b = a;
    b.toString(); // Noncompliant
  }

  void relationshipLearning(Object a) {
    if(a == null) {
      a.toString(); // Noncompliant
    }
  }
}