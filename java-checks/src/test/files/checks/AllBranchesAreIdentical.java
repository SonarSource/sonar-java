class A {

  void conditionalExpression () {
    true ? 1 : // Noncompliant [[sc=5;ec=11]] {{This conditional operation returns the same value whether the condition is "true" or "false".}}
      (1);
    true ? 1 * 5 : 1 * 5; // Noncompliant {{This conditional operation returns the same value whether the condition is "true" or "false".}}
    true ? 1 : 2;
    true ? true ? 1 : 1 : 1; // Noncompliant
  }


}
