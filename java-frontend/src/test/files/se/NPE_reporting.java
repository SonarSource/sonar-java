class A {
  void null_assigned() {
    Object a = null;
    a.toString(); // Noncompliant [[secondary=3]]
  }

  void reassignement() {
    Object a = null;
    Object b = new Object();
    b = a;
    b.toString(); // Noncompliant [[secondary=10]]
  }

  void relationshipLearning(Object a) {
    if(a == null) {
      a.toString(); // Noncompliant [[secondary=15]]
    }
  }

  void combined(Object a) {
    Object b = new Object();
    if(a == null) {
      b = a;
      b.toString(); // Noncompliant
    }
  }
}