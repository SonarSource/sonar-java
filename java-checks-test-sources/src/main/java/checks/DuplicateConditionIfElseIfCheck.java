package checks;

class DuplicateConditionIfElseIfCheck {
  void example(boolean condition1, boolean condition2, int i1, int i2) {
    if (condition1) {
    } else if (condition1) { // Noncompliant [[sc=16;ec=26;secondary=-1]] {{This branch can not be reached because the condition duplicates a previous condition in the same sequence of "if/else if" statements}}
    }

    if (condition2) {
    } else if (condition1) {
    } else if (condition1) { // Noncompliant [[secondary=-1]] {{This branch can not be reached because the condition duplicates a previous condition in the same sequence of "if/else if" statements}}
    }

    if (condition1) {
    } else if (condition2) {
    } else if (condition1) { // Noncompliant [[secondary=-2]] {{This branch can not be reached because the condition duplicates a previous condition in the same sequence of "if/else if" statements}}
    }
  }
}
