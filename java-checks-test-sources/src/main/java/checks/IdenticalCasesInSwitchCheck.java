package checks;

class IdenticalCasesInSwitchCheck {
  void foo(){
    switch (1) {
      case 1:
        System.out.println("plop");
        System.out.println("plop");
        break;
      case 2:
        System.out.println("bar"); //Compliant
        break;
      case 3: // Noncompliant [[sc=7;el=+4;ec=15;secondary=-7]] {{This case's code block is the same as the block for the case on line 6.}}
      case 4:
        System.out.println("plop");
        System.out.println("plop");
        break;
      case 5: // Noncompliant [[sc=7;el=+3;ec=15;secondary=-12]] {{This case's code block is the same as the block for the case on line 6.}}
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
        trivial();
      case 2: // Noncompliant [[secondary=-2]]
        trivial();
    }

    switch (1) {
      case 1:
        trivial();
        break;
      case 2: // Noncompliant [[secondary=-3]]
        trivial();
        break;
      case 3: // Noncompliant [[secondary=-6]]
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
      System.out.println("foo");
      System.out.println("foo");
    } else if (true) {
      // skip empty blocks
    } else if (true) {
      // skip empty blocks
    } else if (true) {
      System.out.println("bar");
    } else if (true) { // Noncompliant [[sc=22;el=+3;ec=6;secondary=-9]] {{This branch's code block is the same as the block for the branch on line 140.}}
      System.out.println("foo");
      System.out.println("foo");
    } else { // Noncompliant [[sc=12;el=+3;ec=6;secondary=-12]] {{This branch's code block is the same as the block for the branch on line 140.}}
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
    else if (true) f(); // Noncompliant [[secondary=-1]]

    if (true) {
      f();
      f();
    }
    else if (true) { // Noncompliant [[secondary=-4]]
      f();
      f();
    }
    else if (true) {
      g();
      g();
    }
    else if (true) {  // Noncompliant [[secondary=-4]]
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
