class A {
  public static int var;

  public int pickNumber() {
    int i = 0;
    int j = 0;

    i = i++; // Noncompliant; i is still zero
    A.var = A.var++;
    return j++; // Noncompliant; 0 returned
  }


  public int pickNumber2() {
    int i = 0;
    int j = 0;
    i++; //Compliant
    A.var = ++var;
    return ++j; //Compliant
  }
}