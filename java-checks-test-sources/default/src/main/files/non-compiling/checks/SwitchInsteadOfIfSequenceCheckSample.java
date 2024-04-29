package checks;

public class SwitchInsteadOfIfSequenceCheckSample {
  String choice;
  String choice2;
  Object choice3;
  String myStringMethod(){
    return "";
  }
  void foo() {
    if (""" // Noncompliant@+1 {{Convert this "if/else if" structure into a "switch". (sonar.java.source not set. Assuming 7 or greater.)}}
//      ^^^^^^^^^^^^^^^^
      red
      """.equals(choice)) {  
      dispenseRed();
    } else if ("""
      blue
      """.equals(choice)) {
      dispenseBlue();
    } else if ("""
      yellow
      """.equals(choice)) {
      dispenseYellow();
    } else {
      promptUser();
    }
  }

  private void promptUser() {
  }

  private void dispenseYellow() {
  }

  private void dispenseBlue() {
  }

  private void dispenseRed() {
  }

  void foo2() {
    if ("""
      red
      """.equals(choice)) {  // compliant, not enough choices
      dispenseRed();
    } else if ("""
      blue
      """.equals(choice)) {
      dispenseBlue();
    } else {
      promptUser();
    }
  }
  
  void foo22() {
    if ("""
      red
      """.equals(choice)) {  // compliant, not enough choices
      dispenseRed();
    } else if ("""
      blue
      """.equals("\nblue\n")) {
      dispenseBlue();
    } else {
      promptUser();
    }
  }

  void foo3() { // Noncompliant@+1 {{Convert this "if/else if" structure into a "switch". (sonar.java.source not set. Assuming 7 or greater.)}}
    if ("""
      red
      """.equals(choice)) {
      dispenseRed();
    } else if (choice.equals("""
      blue
      """)) {
      dispenseBlue();
    } else if (choice.equals("""
      yellow
      """)) {
      dispenseYellow();
    } else if ("""
      brown
      """.equals(choice)) {
      dispenseBrown();
    } else {
      promptUser();
    }
  }

  private void dispenseBrown() {
  }

}
