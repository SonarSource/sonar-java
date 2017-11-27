import java.io.FileInputStream;

class A {
  void plop() { // Noncompliant {{SE Interrupted: reached limit of 20 steps for method plop#4 in class A}}
    Object o = new Object();

    boolean a = true;
    a &= (b() == C);
    a &= (b() == C);
    a &= (b() == C);
    a &= (b() == C);
    a &= (b() == C);
    a &= (b() == C);
    a &= (b() == C);
    a &= (b() == C);
    a &= (b() == C);
    a &= (b() == C);
    a &= (b() == C);
    a &= (b() == C);
    a &= (b() == C);
    a &= (b() == C);

    if (a) { //BOOM : 2^n -1 states are generated (where n is the number of lines of &= assignements in the above code) -> fail fast by not even enqueuing nodes
      o = null;
    }
  }

  Object bar(boolean a) { // Compliant
    if (a) {
      return null;
    }
    return new Object();
  }
}
