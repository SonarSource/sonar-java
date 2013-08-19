class A {
  public int getVector()[] { /* ... */ }    // Non-Compliant

  public int[] getVector() { /* ... */ }    // Compliant

  public int[] getMatrix()[] { /* ... */ }  // Non-Compliant

  public int[][] getMatrix() { /* ... */ }  // Compliant

  public int f()[][][][] { /* ... */ }      // Non-Compliant
}

interface A {
  int f()[];                                // Non-Compliant
  int[] f();                                // Compliant
}
