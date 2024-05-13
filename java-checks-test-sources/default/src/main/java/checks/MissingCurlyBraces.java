package checks;

import java.util.ArrayList;
import java.util.List;

class MissingCurlyBraces {

  void method(boolean condition) {
    if (condition) doSomething(); // Noncompliant {{Missing curly brace.}}
//  ^^

    if (condition) doSomething(); // Noncompliant
    else doSomethingElse(); // Noncompliant
//  ^^^^

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
//  ^^^

    for (int i = 0; i < 10; i++) {
    }

    List<String> list = new ArrayList<>();
    for (String s: list) doSomething(); // Noncompliant
//  ^^^

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

  int exceptions(boolean condition) {
    if (condition) return 1; // Compliant

    for (int i = 0; i < 12; i++) {
      if (condition) break; // Compliant
      if (condition) continue; // Compliant

      if (condition) something();continue; // Noncompliant
    }

    if (condition) something(); // Noncompliant

    if (condition) // Noncompliant
      return 1;

    if (condition) return 1;something(); // Compliant, S2681 raises an issue here

    if (condition) return 1; else { doSomethingElse(); } // Noncompliant
//  ^^
    if (condition) return 1; else if (condition) { doSomethingElse(); } // Noncompliant
//  ^^

    if (condition) return 1; // Noncompliant
    else doSomethingElse(); // Noncompliant

    while(condition) return 1; // Noncompliant
    while(condition) break; // Noncompliant
    while(condition) continue; // Noncompliant

    if (condition) return 1; // Noncompliant
    else return 2; // Noncompliant
  }

  private void something() {
  }

  private void doSomething() {
  }

  private void doSomethingElse() {
  }

}
