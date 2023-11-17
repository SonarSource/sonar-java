package checks;

import java.util.ArrayList;

class UnusedLocalVariableCheck {

  public void f(int unusedParameter) {
    int unusedLocalVariable; // Noncompliant [[sc=9;=ec=28]] {{Remove this unused "unusedLocalVariable" local variable.}}
    unknown++;
    this.unknown++;
  }

  void lambdas_not_resolved(UnknnownFunction lambda) {
    int a = 42; // Compliant
    lambdas_not_resolved(y -> a + y);
    lambdas_not_resolved(y -> {
      int x = 1; // Compliant
      return y + x;
    });
    lambdas_not_resolved(y -> {
      int sum = 0;
      for (Integer in: new ArrayList<Integer>()) {
        sum += in;
      }
      return sum;
    });
    int b = 42; // Noncompliant
    lambdas_not_resolved(y -> b() + y);
    int c = 42; // Noncompliant
    lambdas_not_resolved(y -> UnusedLocalVariableCheck.c + y);
  }
  int b() { return 0; }
  int c;

}
