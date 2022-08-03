package symbolicexecution.checks;


class NullFromBooleanValue {
  private static void f(String nullableValue) {
    Boolean isValid = true;
    if (nullableValue == null) {
      isValid = false;
    }
    if (isValid.booleanValue()) {
      System.out.println(nullableValue.toLowerCase()); // FP: NPE is not possible
    }
  }
}
