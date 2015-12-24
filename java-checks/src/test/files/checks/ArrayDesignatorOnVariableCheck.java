class A {
  int[] a,
        b,
        c[][][][][], // Noncompliant {{Move the array designator from the variable to the type.}}
        d[], // Noncompliant {{Move the array designator from the variable to the type.}}
        e,
        f
        []; // Noncompliant [[sc=9;ec=10]] {{Move the array designator from the variable to the type.}}
}

interface B {
  int a[] = null; // Noncompliant {{Move the array designator from the variable to the type.}}
  int[] b = null; // Compliant
}

class C {
  private void foo(
      int[] a,
      int b[]) { // Noncompliant {{Move the array designator from the variable to the type.}}
    for (String a[]: null) { // Noncompliant {{Move the array designator from the variable to the type.}}
    }

    for (String[] a: null) { // Compliant
    }
  }

  private int bar()[] { // Compliant
    return 0;
  }
  
  private int lum(int ... a) {// Compliant
    return 0;
  }
}
