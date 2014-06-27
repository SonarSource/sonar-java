class A {
  private void f() {
    for (int i = 0; i < 42; i++) { // Compliant
    }

    for (int i = 0; i < 42; i++) { // Compliant
      break;
    }

    for (int i = 0; i < 42; i++) { // Non-Compliant
      break;
      break;
    }

    for (int i = 0; i < 42; i++) { // Non-Compliant
      break;
      continue;
    }

    while (true) { // Non-Compliant
      continue;
      continue;
    }

    for (Integer a: a) { // Compliant
      switch (foo) {
        case 0:
          break;
        default:
          break;
      }

      break;
    }

    break;
    break;
    continue;
    continue;

    do { // Non-Compliant
      break;
      switch (foo) {
        case 0:
          continue;
          continue;
        case 0:
          break;
          break;
      }
    } while (false);

    switch (foo) {
      case 0:
        break;
    }

    while(true){// Non-Compliant
      for (int i = 0; i < 42; i++) {
        continue;
      }
      break;
    }

    switch (foo) {
      case 0:
        do{ //non-compliant
          if(false){
            continue;
          }
          break;
        }while(true);
      case 1:
        break;
    }
  }
}