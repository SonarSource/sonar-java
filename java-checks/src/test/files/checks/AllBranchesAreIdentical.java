class A {

  void conditionalExpression () {
    true ? 1 : // Noncompliant [[sc=5;ec=11]] {{This conditional operation returns the same value whether the condition is "true" or "false".}}
      (1);
    true ? 1 * 5 : 1 * 5; // Noncompliant {{This conditional operation returns the same value whether the condition is "true" or "false".}}
    true ? 1 : 2;
    true ? true ? 1 : 1 : 1; // Noncompliant
  }

  void switchStatement() {
    switch (1) { // Noncompliant [[sc=5;ec=11]] {{Remove this conditional structure or edit its code blocks so that they're not all the same.}}
      case 1:
        break;
      case 2:
        break;
    }

    switch (1) { // Compliant
      case 1:
        break;
      case 2:
        break;
      case 3:
        f();
        break;
    }
  }

  void ifStatement() {
    if (b == 0) {  // Noncompliant [[sc=5;ec=7]] {{Remove this conditional structure or edit its code blocks so that they're not all the same.}}
      doOneMoreThing();
    }
    else {
      doOneMoreThing();
    }

    if (true) { // Noncompliant

    } else if (true) {

    } else {

    }

    if (true) f(); // Noncompliant
    else f();


  }


}
