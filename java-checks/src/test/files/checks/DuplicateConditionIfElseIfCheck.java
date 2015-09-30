class DuplicateConditionIfElseIf {
  void example() {
    if (condition1) {
    } else if (condition1) { // Noncompliant [[sc=16;ec=26;secondary=3]] {{This branch can not be reached because the condition duplicates a previous condition in the same sequence of "if/else if" statements}}
    }

    if (condition2) {
    } else if (condition1) {
    } else if (condition1) { // Noncompliant [[secondary=8]] {{This branch can not be reached because the condition duplicates a previous condition in the same sequence of "if/else if" statements}}
    }

    if (condition1) {
    } else if (condition2) {
    } else if (condition1) { // Noncompliant [[secondary=12]] {{This branch can not be reached because the condition duplicates a previous condition in the same sequence of "if/else if" statements}}
    }
  }
}
