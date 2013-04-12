class TooManyStatementsPerLine {
  int a; int b; // OK - not a statement

  void method() {
    doSomething(); doSomethingElse(); // NOK

    if (a) {} // OK

    if (a) {} if (b) {} // NOK

    while (condition); // OK

    label: while (condition) { // OK
      break label; // OK
    }

    int a = 0; a++; // NOK
  }
}
