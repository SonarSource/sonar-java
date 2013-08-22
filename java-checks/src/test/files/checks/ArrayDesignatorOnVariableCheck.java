class A {
  int[] a,
        b,
        c[][][][][], // Non-Compliant - just once
        d[], // Non-Compliant
        e;
}

interface A {
  int a[] = null; // Non-Compliant
  int[] b = null; // Compliant
}

class A {
  private void f(
      int[] a,
      int b[]) { // Non-Compliant
    for (String a[]: null) { // Non-Compliant
    }

    for (String[] a: null) { // Compliant
    }
  }

  private int f()[] { // Compliant
    return null;
  }
}