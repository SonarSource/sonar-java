package checks;

class IdenticalCasesInSwitchCheckSample {
  void foo(){
    switch (1) {
      case 1:
//    ^[el=+4;ec=14]>
        System.out.println("plop");
        System.out.println("plop");
        break;
      case 2:
        System.out.println("bar"); //Compliant
        break;
      case 3: // Noncompliant {{This case's code block is the same as the block for the case on line 6.}}
//    ^[el=+5;ec=14] 1
      case 4:
        System.out.println("plop");
        System.out.println("plop");
        break;
      case 5: // Noncompliant {{This case's code block is the same as the block for the case on line 6.}}
//    ^[el=+4;ec=14]
        System.out.println("plop");
        System.out.println("plop");
        break;
    }

    switch (1) {
      case 1:
        f(1);
        break;
      case 2:
        f(2);
        break;
    }

    switch (1) {
      case 1:
//    ^[el=+2;ec=18]>
        trivial();
      case 2: // Noncompliant
    //^[el=+2;ec=18] 1
        trivial();
    }

    switch (1) {
      case 1:
//    ^[el=+3;ec=14]>
        trivial();
        break;
      case 2: // Noncompliant
    //^[el=+3;ec=14] 1
        trivial();
        break;
      case 3: // Noncompliant
        trivial();
        break;
    }

    switch (1) {
      case 1:
        trivial();
      case 2:
        trivial();
      case 3:
      default:
    }

    switch (1) {
      case 1:
        trivial();
        break;
      case 2:
        trivial();
        break;
      case 3:
      default:
    }

    switch (1) {
      case 1:
        f();
        nonTrivial();
      case 2: // Noncompliant
        f();
        nonTrivial();
      case 3:
    }

    switch (1) {
      case 1:
        f(1);
        break;
    }

    switch (1) {
      case 1:
        f(1);
        System.out.println(1);
        break;
      case 2: // Noncompliant
        f(1);
        System.out.println(1);
        break;
    }

    switch (1) {
      case 1:
        f(1);
        System.out.println(1);
        break;
      case 2: // Noncompliant
        f(1);
        System.out.println(1);
        break;
      case 3:
        break;
    }

    switch (1) {
      case 1:
        trivial();
        break;
      case 2: // Compliant - this case is covered by RSPEC-3923
        trivial();
        break;
      default: // Compliant - this case is covered by RSPEC-3923
        trivial();
        break;
    }
  }

  void ifStatement() {
    if (true) {
      System.out.println("foo");
    } else if (true) {
      // skip empty blocks
    } else if (true) {
      // skip empty blocks
    } else if (true) {
      System.out.println("bar");
    } else if (true) { // Compliant - trivial
      System.out.println("foo");
    } else { // Compliant - trivial
      System.out.println("foo");
    }

    if (true) {
            //^[el=+4;ec=5]> {{Original}}
      System.out.println("foo");
      System.out.println("foo");
    } else if (true) {
      // skip empty blocks
    } else if (true) {
      // skip empty blocks
    } else if (true) {
      System.out.println("bar");
    } else if (true) { // Noncompliant {{This branch's code block is the same as the block for the branch on line 147.}}
//                   ^[el=+4;ec=5] 1
      System.out.println("foo");
      System.out.println("foo");
    } else { // Noncompliant {{This branch's code block is the same as the block for the branch on line 147.}}
//         ^[ec=5;el=+4]
      System.out.println("foo");
      System.out.println("foo");
    }
    if (true) {
      f();
    }

    if (true) f();
    else if (true) f();
    else g();

    if (true) f();
    else f();

    if (true) f();
//            ^^^^>
    else if (true) f(); // Noncompliant
//                 ^^^^
    if (true) {
//            ^[el=+4;ec=5]>
      f();
      f();
    }
    else if (true) { // Noncompliant
    //             ^[el=+4;ec=5] 1
      f();
      f();
    }
    else if (true) {
//                 ^[el=+4;ec=5]>
      g();
      g();
    }
    else if (true) { // Noncompliant
  //               ^[el=+4;ec=5]
      g();
      g();
    }
    else ;
  }

  private void g() {
  }

  private void nonTrivial() {
  }

  private void trivial() {
  }

  private void f() {
  }

  private void f(int i) {
  }

}
