public abstract class TestClass {

  abstract void probe();

  abstract Object dumpState(Object... objects);

  abstract Object dumpValue(Object... objects);

  void testLiterals() {
    dumpState(false); // Noncompliant {{FALSE}}
    dumpState(true); // Noncompliant {{TRUE}}
    dumpState(null); // Noncompliant {{UNKNOWN}}
  }

  void testLogicalNot(boolean value) {
    dumpState(!true); // Noncompliant {{FALSE}}
    dumpState(!false); // Noncompliant {{TRUE}}
    dumpState(!value); // Noncompliant {{UNKNOWN}}
  }

  void testLogicalNotFalse() {
    if (!false) {
      probe(); // Noncompliant {{1}}
    } else {
      probe(); // Compliant
    }
  }

  void testLogicalNotTrue() {
    if (!true) {
      probe(); // Compliant
    } else {
      probe(); // Noncompliant {{1}}
    }
  }

  void testLogicalNotUnknown(boolean value) {
    if (!value) {
      probe(); // Noncompliant {{1}}
    } else {
      probe(); // Noncompliant {{1}}
    }
  }

  public void testAssignment(boolean condition) {
    boolean local, local1, local2;

    // unknown state
    dumpState(condition); // Noncompliant {{UNKNOWN}}
    dumpState(local = condition); // Noncompliant {{UNKNOWN}}
    dumpState(local); // Noncompliant {{UNKNOWN}}

    // explicit assignment
    dumpValue(condition = false); // Noncompliant {{FALSE}}
    dumpValue(condition); // Noncompliant {{FALSE}}
    dumpValue(condition = true); // Noncompliant {{TRUE}}
    dumpValue(condition); // Noncompliant {{TRUE}}

    // indirect assignment
    dumpValue(local = condition); // Noncompliant {{TRUE}}
    dumpValue(local); // Noncompliant {{TRUE}}

    // nested assignment in array index
    boolean array[] = new array[1];
    array[(local = false) ? 0 : 0] = false;
    dumpValue(local); // Noncompliant {{FALSE}}

    // nested assignment in function call
    dumpValue(local1 = false, local2 = true); // Noncompliant {{FALSE,TRUE}}
    dumpValue(false, true); // Noncompliant {{FALSE,TRUE}}
  }

  public void testConditional(boolean p1, boolean p2) {
    dumpValue(false && false); // Noncompliant {{FALSE}}
    dumpValue(false && true); // Noncompliant {{FALSE}}
    // dumpValue(false && local2); // Noncompliant {{FALSE}}
    dumpValue(true && false); // Noncompliant {{FALSE}}
    dumpValue(true && true); // Noncompliant {{TRUE}}
    dumpValue(true && local2);
    // dumpValue(local1 && false); // Noncompliant {{FALSE}}
    dumpValue(local1 && true);
    dumpValue(local1 && local2);

    dumpValue(false || false); // Noncompliant {{FALSE}}
    dumpValue(false || true); // Noncompliant {{TRUE}}
    dumpValue(false || local2);
    dumpValue(true || false); // Noncompliant {{TRUE}}
    dumpValue(true || true); // Noncompliant {{TRUE}}
    // dumpValue(true || local2); // Noncompliant {{TRUE}}
    dumpValue(local1 || false);
    // dumpValue(local1 || true); // Noncompliant {{TRUE}}
    dumpValue(local1 || local2);
  }

  public void testConditionalAndFalseAny() {
    if (false && probe()) { // Compliant
      probe(); // Compliant
    } else {
      probe(); // Noncompliant {{1}}
    }
  }

  public void testConditionalAndTrueAny() {
    if (true && probe()) { // Noncompliant {{1}}
      // FIXME: should be 1
      probe(); // Noncompliant {{2}}
    } else {
      probe(); // Noncompliant {{2}}
    }
  }

  public void testConditionalAndTrueFalse() {
    if (true && false) {
      probe(); // Compliant
    } else {
      probe(); // Noncompliant {{1}}
    }
  }

  public void testConditionalAndTrueTrue() {
    if (true && true) {
      probe(); // Noncompliant {{1}}
    } else {
      probe(); // Compliant
    }
  }

  public void testConditionalOrFalseAny() {
    if (false || probe()) { // Noncompliant {{1}}
      // FIXME: should be 1
      probe(); // Noncompliant {{2}}
    } else {
      probe(); // Noncompliant {{2}}
    }
  }

  public void testConditionalOrFalseFalse() {
    if (false || false) {
      probe(); // Compliant
    } else {
      probe(); // Noncompliant {{1}}
    }
  }

  public void testConditionalOrFalseTrue() {
    if (false || true) {
      probe(); // Noncompliant {{1}}
    } else {
      probe(); // Compliant
    }
  }

  public void testConditionalOrTrueAny() {
    if (false && probe()) { // Compliant
      probe(); // Compliant
    } else {
      probe(); // Noncompliant {{1}}
    }
  }

  public void testInstanceOf(Object instance) {
    // unsupported now
    dumpState(instance instanceof Object); // Noncompliant {{UNKNOWN}}
    // unsupported now
    if (instance instanceof Object) {
      probe(); // Noncompliant {{1}}
    } else {
      probe(); // Noncompliant {{1}}
    }
  }

}
