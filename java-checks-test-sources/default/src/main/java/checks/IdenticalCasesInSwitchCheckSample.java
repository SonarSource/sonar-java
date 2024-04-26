package checks;

class IdenticalCasesInSwitchCheckSample {
  void foo(){
    switch (1) {
      case 1:
//  ^^^<
        System.out.println("plop");
        System.out.println("plop");
        break;
      case 2:
        System.out.println("bar"); //Compliant
        break;
      case 3: // Noncompliant {{This case's code block is the same as the block for the case on line 6.}}
//^[sc=7;ec=15;sl=13;el=17]
      case 4:
        System.out.println("plop");
        System.out.println("plop");
        break;
      case 5: // Noncompliant {{This case's code block is the same as the block for the case on line 6.}}
//^[sc=7;ec=15;sl=18;el=21]
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
//  ^^^<
        trivial();
      case 2: // Noncompliant
        trivial();
    }

    switch (1) {
      case 1:
//  ^^^<
        trivial();
        break;
      case 2: // Noncompliant
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
//  ^^^<
      System.out.println("foo");
      System.out.println("foo");
    } else if (true) {
      // skip empty blocks
    } else if (true) {
      // skip empty blocks
    } else if (true) {
      System.out.println("bar");
    } else if (true) { // Noncompliant {{This branch's code block is the same as the block for the branch on line 140.}}
//^[sc=22;ec=6;sl=149;el=152]
      System.out.println("foo");
      System.out.println("foo");
    } else { // Noncompliant {{This branch's code block is the same as the block for the branch on line 140.}}
//^[sc=12;ec=6;sl=152;el=155]
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
//  ^^^<
    else if (true) f(); // Noncompliant

    if (true) {
//  ^^^<
      f();
      f();
    }
    else if (true) { // Noncompliant
      f();
      f();
    }
    else if (true) {
//  ^^^<
      g();
      g();
    }
    else if (true) { // Noncompliant
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
