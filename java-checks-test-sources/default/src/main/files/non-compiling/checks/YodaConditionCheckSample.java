package checks;

class YodaConditionCheckSample {

  void unknownLiteralType() {
    Object x = new Object();
    if (UNKNOWN_LITERAL == x) { } // Compliant - UNKNOWN_LITERAL is not a valid literal
  }

}
