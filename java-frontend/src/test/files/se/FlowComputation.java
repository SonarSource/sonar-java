
class A {

  void symbolSetToNull() {
    Object a = new Object();
    a = null; // flow@npe {{a is assigned null}}
    a.toString(); // Noncompliant [[flows=npe]] {{NullPointerException might be thrown as 'a' is nullable here}}  flow@npe {{a is dereferenced}}
  }


  void combined(Object a) {
    Object b = new Object();
    if (a == null) { // flow@comb {{...}}
      b = a; // flow@comb {{b is assigned null}}
      b.toString(); // Noncompliant [[flows=comb]] flow@comb {{b is dereferenced}}
    }
  }

  void relationship(boolean a, boolean b) {
    if(a < b) { // flow@rel {{...}}
      if(b > a) { // Noncompliant [[flows=rel]] {{Change this condition so that it does not always evaluate to "true"}} flow@rel {{Condition is always true}}
      }
    }
  }
}

