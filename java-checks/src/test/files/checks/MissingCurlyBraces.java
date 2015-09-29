class MissingCurlyBraces {

  void method() {
    if (condition) doSomething(); // Noncompliant {{Missing curly brace.}}

    if (condition) doSomething(); // Noncompliant
    else doSomethingElse(); // Noncompliant

    if (condition) {
    } else doSomething(); // Noncompliant

    if (condition) {
    } else {
    }

    if (condition) {
    }

    if (condition) {
    } else if (condition) {
    }

    for (int i = 0; i < 10; i++) doSomething(); // Noncompliant

    for (int i = 0; i < 10; i++) {
    }

    while (condition) doSomething(); // Noncompliant

    while (condition) {
    }

    do something(); while (condition); // Noncompliant

    do {
      something();
    } while (condition);
    if (condition) { doSomething(); }
    else // Noncompliant
      doSomethingElse();
  }

}
