class DuplicateConditionIfElseIf {
  void example() {
    if (condition1) {
    } else if (condition1) { // NOK
    }

    if (condition2) {
    } else if (condition1) {
    } else if (condition1) { // NOK
    }

    if (condition1) {
    } else if (condition2) {
    } else if (condition1) { // NOK
    }
  }
}
