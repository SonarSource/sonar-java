package checks;

class ImmediatelyReturnedVariableCheck {

  Object emptyInitializer() {
    // Does not compile (variable o not initialized), but the rule should not raise an Exception anyway.
    Object o;
    return o;
  }

}
