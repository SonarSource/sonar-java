class A {
  void plop(boolean bool) {
    Object a = null; // flow@npe1 [[sc=12;ec=20]] {{a is assigned to null here}}
    Object b = new Object();

    if (bool) {
      b = null; // flow@npe2 [[sc=7;ec=15;el=7]] {{b is assigned to null here}}
    } else {
      b = a; // flow@npe1 [[sc=7;ec=12]] {{a is assigned to b here}}
    }
    b.toString(); // Noncompliant [[sc=5;ec=15;flows=npe1,npe2]] {{A "NullPointerException" could be thrown; "b" is nullable here}}
  }

  // failing tests blowup above, so they do not validate below this line

  void reassignement() {
    Object a = null; // flow@reass {{msg}}
    Object b = new Object();
    b = a; // flow@reass
    b.toString(); // Noncompliant [[flows=reass]]
  }

  void sameLineTwoFlows() {
    Object a = null; // flow@id1,id2 {{common}}
    Object b = new Object(); // flow@id1 {{msg1}}
    b = a; // flow@id2 {{msg2}}
    b.toString(); // Noncompliant [[flows=id1,id2]]
  }

  void complexRelation(int a, int b, Object c) {
    if (a < b) { // flow@asd {{When}}
      c = null; // flow@asd {{Given}}
    }
    System.out.println("");
    if (b > a) {
      c.toString(); // Noncompliant [[flows=asd]]
    }
  }

  void complexRelation(int a, int b, Object c) {
    if (a < b) { // flow@qwe {{When}}
      c = null; // flow@qwe
    }
    System.out.println("");
    if (b > a) {
      c.toString(); // Noncompliant [[flows=qwe]]
    }
  }
}
