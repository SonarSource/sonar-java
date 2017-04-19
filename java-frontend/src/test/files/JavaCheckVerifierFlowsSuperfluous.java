class A {
  void plop(boolean bool) {
    Object a = null; // flow@npe1 [[sc=12;ec=20]] {{a is assigned to null here}}
    Object b = new Object(); // flow@superfluous

    if (bool) { // flow@superfluous
      b = null; // flow@npe2 [[sc=7;ec=15;el=7]] {{b is assigned to null here}}
    } else { // flow@superfluous
      b = a; // flow@npe1 [[sc=7;ec=12]] {{a is assigned to b here}}
    }
    b.toString(); // Noncompliant [[sc=5;ec=15;flows=npe1]] {{A "NullPointerException" could be thrown; "b" is nullable here}}
  }

}
