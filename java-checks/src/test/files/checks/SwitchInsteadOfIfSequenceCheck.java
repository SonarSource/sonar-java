class A {
  String choice;
  String choice2;
  Object choice3;
  String myStringMethod(){
    return "";
  }
  void foo() {
    if ("red".equals(choice)) {  // Noncompliant [[sc=9;ec=29]] {{Convert this "if/else if" structure into a "switch". (sonar.java.source not set. Assuming 7 or greater.)}}
      dispenseRed();
    } else if ("blue".equals(choice)) {
      dispenseBlue();
    } else if ("yellow".equals(choice)) {
      dispenseYellow();
    } else {
      promptUser();
    }
  }
  void foobis() {
    if ("red" == choice) {  // Noncompliant [[sc=9;ec=24]] {{Convert this "if/else if" structure into a "switch". (sonar.java.source not set. Assuming 7 or greater.)}}
      dispenseRed();
    } else if ("blue" == choice) {
      dispenseBlue();
    } else if ("yellow" == choice) {
      dispenseYellow();
    } else {
      promptUser();
    }
  }


  void foo2() {
    if ("red".equals(choice)) {  // compliant, not enough choices
      dispenseRed();
    } else if ("blue".equals(choice)) {
      dispenseBlue();
    } else {
      promptUser();
    }
  }

  void foo3() {
    if ("red".equals(choice)) {  // Noncompliant {{Convert this "if/else if" structure into a "switch". (sonar.java.source not set. Assuming 7 or greater.)}}
      dispenseRed();
    } else if (choice.equals("blue")) {
      dispenseBlue();
    } else if (choice.equals("yellow")) {
      dispenseYellow();
    } else if ("brown".equals(choice)) {
      dispenseBrown();
    } else {
      promptUser();
    }
  }

  void foo4() {
    if ("red".equals(this.choice)) {  // compliant : operand is not and identifier
      dispenseRed();
    } else if ("blue".equals(choice)) {
      dispenseBlue();
    } else if ("yellow".equals(choice)) {
      dispenseYellow();
    } else {
      promptUser();
    }
  }

  void foo5() {
    if ("red".equals(choice)) {  // compliant, last case is not changeable to switch
      dispenseRed();
    } else if ("blue".equals(choice)) {
      dispenseBlue();
    } else if ("yellow".equals(choice)) {
      dispenseYellow();
    } else if (myStringMethod().equals(choice)) {
      dispenseBrown();
    } else {
      promptUser();
    }
  }

  void foo5() {
    if ("red".equals(choice)) {  // compliant, last case is not comparing same symbol
      dispenseRed();
    } else if ("blue".equals(choice)) {
      dispenseBlue();
    } else if ("yellow".equals(choice2)) {
      dispenseYellow();
    } else {
      promptUser();
    }
  }

  void foo6() {
    if (true) {  // compliant, not using equals.
      dispenseRed();
    } else if ("blue".equals(choice)) {
      dispenseBlue();
    } else if ("yellow".equals(choice2)) {
      dispenseYellow();
    } else {
      promptUser();
    }
  }

  void no_else_statement() {
    if ("red".equals(choice)) {  // Noncompliant
      dispenseRed();
    } else if ("blue".equals(choice)) {
      dispenseBlue();
    } else if ("yellow".equals(choice)) {
      dispenseYellow();
    }
  }

  void not_member_select_equals() {
    if (equals(choice)) {  // compliant, not a member select
      dispenseRed();
    } else if ("blue".equals(choice)) {
      dispenseBlue();
    } else if ("yellow".equals(choice2)) {
      dispenseYellow();
    }
  }

  boolean two_param_method(String a, String b) {
    if (this.two_param_method(choice, choice2)) {  // compliant, not equal method
      dispenseRed();
    } else if ("blue".equals(choice)) {
      dispenseBlue();
    } else if ("yellow".equals(choice2)) {
      dispenseYellow();
    }
  }

  boolean not_equal_method(String b) {
    if (this.not_equal_method(choice)) {  // compliant, not equal method
      dispenseRed();
    } else if ("blue".equals(choice)) {
      dispenseBlue();
    } else if ("yellow".equals(choice2)) {
      dispenseYellow();
    }
  }

  void not_string_arg() {
    if ("red".equal(choice3)) {  // compliant, not comparing string
      dispenseRed();
    } else if ("blue".equals(choice3)) {
      dispenseBlue();
    } else if ("yellow".equals(choice3)) {
      dispenseYellow();
    }
  }
  void nestedIFs() {
    if (choice.equals(choice))   // compliant, don't count nesting
      if ("blue".equals(choice)) { // Noncompliant
        dispenseBlue();
      } else if ("yellow".equals(choice)) {
        dispenseYellow();
      } else if ("blue".equals(choice)) {
        dispenseBlue();
      }
    else if ("blue".equals(choice)) {
      dispenseBlue();
    }
  }

}
