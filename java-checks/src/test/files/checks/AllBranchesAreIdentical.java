abstract class A {

  int x;

  void conditionalExpression () {
    x = true ? 1 : // Noncompliant [[sc=9;ec=15]] {{This conditional operation returns the same value whether the condition is "true" or "false".}}
      (1);
    x = true ? 1 * 5 : 1 * 5; // Noncompliant {{This conditional operation returns the same value whether the condition is "true" or "false".}}
    x = true ? 1 : 2;
    x = true ? true ? 1 : 1 : 1; // Noncompliant
  }

  void switchStatement() {
    switch (1) {  // Noncompliant [[sc=5;ec=11]] {{Remove this conditional structure or edit its code blocks so that they're not all the same.}}
      case 1:
        doSomething();
        break;
      case 2:
        doSomething();
        break;
      case 3:
        doSomething();
        break;
      default:
        doSomething();
        break;
    }

    switch (1) {  // Compliant as there is no "default" clause in this "switch" statement, this precise case is handled by RSPEC-1871
      case 1:
        doSomething();
        break;
      case 2:
        doSomething();
        break;
      case 3:
        doSomething();
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

    switch (1) { // Compliant
      case 1:
        break;
      case 2:
        break;
      default:
        f();
        break;
    }
  }


  void ifStatement(int b) {
    if (b == 0) {  // Noncompliant [[sc=5;ec=7]] {{Remove this conditional structure or edit its code blocks so that they're not all the same.}}
      doOneMoreThing();
    } else {
      doOneMoreThing();
    }

    if (b == 0) {  // Compliant
      doSomething();
    } else {
      doOneMoreThing();
    }

    if (true) { // Noncompliant
    } else if (true) {
    } else {
    }

    if (true) f(); // Noncompliant
    else f();

    if(b == 0) { // Compliant as there is no "else" clause in this "if" statement, this precise case is handled by RSPEC-1871
      doSomething();
    } else if(b == 1) {
      doSomething();
    }

  }

  abstract void f();
  abstract void doSomething();
  abstract void doOneMoreThing();

}
