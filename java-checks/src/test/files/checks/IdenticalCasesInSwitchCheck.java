class A {
  void foo(){
    switch (1) {
      case 1:
        System.out.println("plop");
        break;
      case 2:
        System.out.println("bar"); //Compliant
        break;
      case 3:
      case 4: // Noncompliant {{This case's code block is the same as the block for the case on line 4.}}
        System.out.println("plop");
        break;
      case 5: // Noncompliant {{This case's code block is the same as the block for the case on line 4.}}
        System.out.println("plop");
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
    } else if (true) { // Noncompliant {{This branch's code block is the same as the block for the branch on line 21.}}
      System.out.println("foo");
    } else { // Noncompliant {{This branch's code block is the same as the block for the branch on line 29.}}
      System.out.println("foo");
    }
    if (true) {
      1;
    }
  }

  void conditionalExpression () {
    true ? 1 : (1); // Noncompliant {{This conditional operation returns the same value whether the condition is "true" or "false".}}
    true ? 1 * 5 : 1 * 5; // Noncompliant {{This conditional operation returns the same value whether the condition is "true" or "false".}}
    true ? 1 : 2;
  }


}
