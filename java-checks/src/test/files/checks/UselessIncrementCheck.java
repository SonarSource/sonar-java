class A {
  public static int var;

  public int pickNumber() {
    int i = 0;
    int j = 0;
    if (i == 1) {
      return var++;
    } else if (i == 2) {
      return A.var++;
    }
    i = i++; // Noncompliant {{Remove this increment or correct the code not to waste it.}}
    A.var = A.var++; // Noncompliant [[sc=18;ec=20]] {{Remove this increment or correct the code not to waste it.}}
    A.var = i++;
    return j++; // Noncompliant [[sc=13;ec=15]] {{Remove this increment or correct the code not to waste it.}}
  }

  public int pickNumber2() {
    int i = 0;
    int j = 0;
    i++; //Compliant
    A.var = ++var;
    return ++j; //Compliant
  }

  public void foo() {
    return;
  }
}
