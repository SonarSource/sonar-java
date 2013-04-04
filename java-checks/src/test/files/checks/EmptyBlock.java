class EmptyBlock {
  static { // NOK
  }

  static { // OK
    doSomething();
  }

  { // NOK
  }

  { // OK
    doSomething();
  }

  void method() {
    for (int i = 0; i < 10; i++) { // NOK
    }
    for (int i = 0; i < 10; i++); // OK

    switch (1) {
      case 1: // OK
      case 2:
        break;
    }

    switch (1) { // NOK
    }

    try { // NOK
    } catch (Exception e) { // NOK
    } finally { // NOK
    }

    try { // OK
      doSomething();
    } catch (Exception e) { // OK
      doSomething();
    } finally { // OK
      doSomething();
    }

    synchronized (this) { // NOK
    }

    synchronized (this) { // OK
      doSomething();
    }
  }

  void anotherMethod() { // OK
  }
}
