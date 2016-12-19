
class A {
  void combined(Object a) {
    Object b = new Object();
    if (a == null) { // flow@comb {{...}}
      b = a; // flow@comb {{...}}
      b.toString(); // Noncompliant [[flows=comb]] flow@comb
    }
  }

  void relationship(boolean a, boolean b) {
    if(a < b) { // flow@rel {{...}}
      if(b > a) { // Noncompliant [[flows=rel]] {{Change this condition so that it does not always evaluate to "true"}} flow@rel {{Condition is always true}}
      }
    }
  }
}

