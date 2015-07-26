class A {
  public static int var;

  public int pickNumber() {
    int i = 0;
    int j = 0;

    i = i++; // Noncompliant {{Remove this increment or correct the code not to waste it.}}
    A.var = A.var++; // Noncompliant {{Remove this increment or correct the code not to waste it.}}
    return j++; // Noncompliant {{Remove this increment or correct the code not to waste it.}}
  }


  public int pickNumber2() {
    int i = 0;
    int j = 0;
    i++; //Compliant
    A.var = ++var;
    return ++j; //Compliant
  }
}