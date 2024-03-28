package checks;

class RedundantCloseCheckSample {
  void foo() throws Exception {
    try(unknownVariable) {
      unknownVariable.close(); // Compliant
    }
  }
}
