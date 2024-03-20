package checks;

class UselessIncrementCheckSample {
  public static int var;

  public int pickNumber() {
    int i = 0;
    int j = 0;
    if (i == 1) {
      return var++;
    } else if (i == 2) {
      return UselessIncrementCheckSample.var++;
    }
    i = i++; // Noncompliant {{Remove this increment or correct the code not to waste it.}}
    UselessIncrementCheckSample.var = UselessIncrementCheckSample.var++; // Noncompliant [[sc=70;ec=72]] {{Remove this increment or correct the code not to waste it.}}
    UselessIncrementCheckSample.var = i++;
    return j++; // Noncompliant [[sc=13;ec=15]] {{Remove this increment or correct the code not to waste it.}}
  }

  public int pickNumber2() {
    int i = 0;
    int j = 0;
    i++; //Compliant
    UselessIncrementCheckSample.var = ++var;
    return ++j; //Compliant
  }

  public void run() {
    return;
  }
}
