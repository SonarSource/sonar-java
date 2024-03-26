package checks;

class DuplicateConditionIfElseIfCheckSample {
  void example(boolean condition1, boolean condition2, boolean condition3, int i1, int i2) {
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

    if (condition1) {
    } else if ((condition1)) { // Noncompliant
    }

    if (i1 == i2) {
    } else if (i1 == i2) { // Noncompliant
    }

    if ((i1 == (i2))) {
    } else if ((i1) == i2) { // Noncompliant
    }

    if (i2 == i1) {
    } else if (i1 == i2) { // Noncompliant
    }

    if (i1 == i1) {
    } else if (i1 == i2) { // Compliant
    }

    if (i1 == i2) {
    } else if (i2 == i2) { // Compliant
    }

    if ((i1 == i2) == condition1) {
    } else if (condition1 == (i2 == i1)) { // Noncompliant
    }

    if (i1 == i2 == condition1) {
    } else if (condition1 == (i2 == i1)) { // Noncompliant
    }

    if (condition1 == condition2 == condition3) {
    } else if (condition2 == condition1 == condition3) { // Noncompliant
    }

    if (condition1 == condition2 == condition3) {
    } else if (condition3 == condition1 == condition2) { // Compliant
    }

    if (condition1 == condition2 == condition3) {
    } else if (condition3 == (condition1 == condition2)) { // Noncompliant
    }
  }
}
