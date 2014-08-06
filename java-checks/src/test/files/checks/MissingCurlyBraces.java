class MissingCurlyBraces {

  void method() {
    if (condition) doSomething(); // NOK

    if (condition) doSomething(); // NOK
    else doSomethingElse(); // NOK

    if (condition) { // OK
    } else doSomething(); // NOK

    if (condition) { // OK
    } else { // OK
    }

    if (condition) { // OK
    }

    if (condition) { // OK
    } else if (condition) { // OK
    }

    for (int i = 0; i < 10; i++) doSomething(); // NOK

    for (int i = 0; i < 10; i++) { // OK
    }

    while (condition) doSomething(); // NOK

    while (condition) { // OK
    }

    do something(); while (condition); // NOK

    do { // OK
      something();
    } while (condition);
    if (condition) { doSomething(); } // OK
    else
      doSomethingElse(); // NOK
  }

}
