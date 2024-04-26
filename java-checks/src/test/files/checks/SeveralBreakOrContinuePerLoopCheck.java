class A {
  private void f() {
    for (int i = 0; i < 42; i++) { // Compliant
    }

    for (int i = 0; i < 42; i++) { // Compliant
      break;
    }

    for (int i = 0; i < 42; i++) { // Noncompliant {{Reduce the total number of break and continue statements in this loop to use at most one.}}
//  ^^^
      break;
//  ^^^<
      break;
//  ^^^<
    }

    for (int i = 0; i < 42; i++) { // Noncompliant
      break;
      continue;
    }

    while (true) { // Noncompliant
//  ^^^^^
      continue;
//  ^^^<
      continue;
//  ^^^<
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

    do { // Noncompliant
//  ^^
      break;
//  ^^^<
      switch (foo) {
        case 0:
          continue;
//  ^^^<
          continue;
//  ^^^<
        case 0:
          break;
          break;
      }
    } while (false);

    switch (foo) {
      case 0:
        break;
    }

    while(true){//Compliant
      for (int i = 0; i < 42; i++) {
        continue;
      }
      break;
    }

    switch (foo) {
      case 0:
        do{ // Noncompliant
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
