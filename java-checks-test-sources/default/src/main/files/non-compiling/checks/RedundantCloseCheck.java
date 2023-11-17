package checks;

class RedundantCloseCheck {
  void foo() throws Exception {
    try(unknownVariable) {
      unknownVariable.close(); // Compliant
    }
  }
}
