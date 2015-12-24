class A {
  public int getVector()[] { /* ... */ }    // Noncompliant [[sc=25;ec=26]] {{Move the array designators "[]" to the end of the return type.}}

  public int[] getVector() { /* ... */ }    // Compliant

  public int[] getMatrix()[] { /* ... */ }  // Noncompliant {{Move the array designators "[]" to the end of the return type.}}

  public int[][] getMatrix() { /* ... */ }  // Compliant

  public int foo()[][][][] { /* ... */ }    // Noncompliant {{Move the array designators "[]" to the end of the return type.}}
  
  public int 
    bar()[] { /* ... */ }                   // Noncompliant {{Move the array designators "[]" to the end of the return type.}}
  
  public int 
    qix()
    [] { /* ... */ }                        // Noncompliant {{Move the array designators "[]" to the end of the return type.}}
  
  public int[] 
    goo() { /* ... */ }                     // Compliant
}

interface B {
  int foo()[];                              // Noncompliant {{Move the array designators "[]" to the end of the return type.}}
  int[] bar();                              // Compliant
}
