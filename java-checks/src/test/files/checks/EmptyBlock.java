class EmptyBlock {
  static { // NOK
  }

  { // NOK
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
    }
    catch (Exception e) { // NOK
    } finally { // NOK
    }

    synchronized (this) { // NOK
    }
  }
}
