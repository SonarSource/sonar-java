package checks;

class UselessIncrementCheck {
  public static int var;

  public int pickNumber() {
    int i = 0;
    int j = 0;
    if (i == 1) {
      return var++;
    } else if (i == 2) {
      return UselessIncrementCheck.var++;
    }
    i = i++; // Noncompliant {{Remove this increment or correct the code not to waste it.}}
    UselessIncrementCheck.var = UselessIncrementCheck.var++; // Noncompliant [[sc=58;ec=60]] {{Remove this increment or correct the code not to waste it.}}
    UselessIncrementCheck.var = i++;
    return j++; // Noncompliant [[sc=13;ec=15]] {{Remove this increment or correct the code not to waste it.}}
  }

  public int pickNumber2() {
    int i = 0;
    int j = 0;
    i++; //Compliant
    UselessIncrementCheck.var = ++var;
    return ++j; //Compliant
  }

  public void run() {
    return;
  }
}
