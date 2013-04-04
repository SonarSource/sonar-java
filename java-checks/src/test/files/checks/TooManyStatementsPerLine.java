class TooManyStatementsPerLine {
  void method() {
    doSomething(); doSomethingElse(); // NOK

    if (a) {} // OK

    if (a) {} if (b) {} // NOK

    while (condition); // OK

    label: while (condition) { // OK
      break label; // OK
    }
  }
}
