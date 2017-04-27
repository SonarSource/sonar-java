class A {
  void foo(){
    switch (1) {
      case 1:
        System.out.println("plop");
        break;
      case 2:
        System.out.println("bar"); //Compliant
        break;
      case 3: // Noncompliant [[sc=7;el=+3;ec=15;secondary=4]] {{This case's code block is the same as the block for the case on line 4.}}
      case 4:
        System.out.println("plop");
        break;
      case 5: // Noncompliant [[sc=7;el=+2;ec=15;secondary=4]] {{This case's code block is the same as the block for the case on line 4.}}
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
    } else if (true) { // Noncompliant [[sc=22;el=+2;ec=6;secondary=21]] {{This branch's code block is the same as the block for the branch on line 21.}}
      System.out.println("foo");
    } else { // Noncompliant [[sc=12;el=+2;ec=6;secondary=29]] {{This branch's code block is the same as the block for the branch on line 29.}}
      System.out.println("foo");
    }
    if (true) {
      1;
    }
  }

}
