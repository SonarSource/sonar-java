public abstract class TestClass extends BaseClass {

  abstract void probe();

  abstract Object dumpState(Object... objects);

  abstract Object dumpRelation(Object a, Object b);

  abstract Object dumpValue(Object... objects);

  void a(boolean p1, boolean p2) {
    dumpValue(p1 && false); // Noncompliant {{false/false}}
    probe(); // Noncompliant {{2}}
  }

  void testAssignment(boolean condition) {
    boolean local, local1, local2;

    // unknown state
    dumpState(condition); // Noncompliant {{UNKNOWN}}
    dumpState(local = condition); // Noncompliant {{UNKNOWN}}
    dumpState(local); // Noncompliant {{UNKNOWN}}

    // explicit assignment
    probe(); // Noncompliant {{1}}
    dumpValue(condition = false); // Noncompliant {{false}}
    dumpValue(condition); // Noncompliant {{false}}
    dumpValue(condition = true); // Noncompliant {{true}}
    dumpValue(condition); // Noncompliant {{true}}

    // indirect assignment
    dumpValue(local = condition); // Noncompliant {{true}}
    dumpValue(local); // Noncompliant {{true}}

    // nested assignment in array index
    boolean array[] = new array[1];
    dumpValue((local1 = false)[local2 = true]);
    dumpValue(local1, local2); // Noncompliant {{false,true}}

    // nested assignment in function call
    dumpValue(local1 = false, local2 = true); // Noncompliant {{false,true}}
    dumpValue(local1, local2); // Noncompliant {{false,true}}
  }

  void testBinary(boolean a, boolean b) {
    // invalid in java
    boolean c = (a = true) + (b = false);
    dumpValue(a, b); // Noncompliant {{true,false}}
  }

  void testBreakBreak() {
    while (true) {
      break;
      break;
    }
    probe(); // Noncompliant {{1}}
  }

  void testBreakContinue() {
    while (true) {
      break;
      continue;
    }
    probe(); // Noncompliant {{1}}
  }

  void testBreakDoWhile() {
    while (true) {
      break;
      do {
      } while (true);
    }
    probe(); // Noncompliant {{1}}
  }

  void testBreakFor() {
    while (true) {
      break;
      for (;;) {
      }
    }
    probe(); // Noncompliant {{1}}
  }

  void testBreakForEach(String[] strings) {
    while (true) {
      break;
      for (String string : strings) {
      }
    }
    probe(); // Noncompliant {{1}}
  }

  void testBreakIf() {
    while (true) {
      break;
      if (true) {
      }
    }
    probe(); // Noncompliant {{1}}
  }

  void testBreakReturn() {
    while (true) {
      break;
      return;
    }
    probe(); // Noncompliant {{1}}
  }

  void testBreakSwitch(int condition) {
    while (true) {
      break;
      switch (condition) {
      }
    }
    probe(); // Noncompliant {{1}}
  }

  void testBreakSynchronized() {
    while (true) {
      break;
      synchronized (new Object()) {
      }
    }
    probe(); // Noncompliant {{1}}
  }

  void testBreakThrow() {
    while (true) {
      break;
      throw new Object();
    }
    probe(); // Noncompliant {{1}}
  }

  void testBreakTry() {
    while (true) {
      break;
      try {
      } catch (Exception e) {
      } finally {
      }
    }
    probe(); // Noncompliant {{1}}
  }

  void testBreakVariable() {
    while (true) {
      break;
      int i = 0;
    }
    probe(); // Noncompliant {{1}}
  }

  void testBreakWhile() {
    while (true) {
      break;
      while (true) {
      }
    }
    probe(); // Noncompliant {{1}}
  }

  void testCastAndParentesized(boolean a, boolean b, boolean c, boolean d) {
    if ((boolean) ((boolean) false)) {
      probe(); // Compliant, unreachable
    }
    if ((boolean) !(boolean) (true)) {
      probe(); // Compliant, unreachable
    }
    if (((boolean) (a) == ((boolean) b)) && ((boolean) (c) == ((boolean) d))) {
      dumpRelation(a, b); // Noncompliant {{EQUAL_TO}}
      dumpRelation(c, d); // Noncompliant {{EQUAL_TO}}
    }
  }

  void testConditional(boolean p1, boolean p2) {
    dumpValue(false && false); // Noncompliant {{false}}
    probe(); // Noncompliant {{1}}
    dumpValue(false && true); // Noncompliant {{false}}
    probe(); // Noncompliant {{1}}
    dumpValue(false && p2); // Noncompliant {{false}}
    probe(); // Noncompliant {{1}}
    dumpValue(true && false); // Noncompliant {{false}}
    probe(); // Noncompliant {{1}}
    dumpValue(true && true); // Noncompliant {{true}}
    probe(); // Noncompliant {{1}}
    dumpState(true && p2); // Noncompliant {{UNKNOWN}}
    probe(); // Noncompliant {{1}}

    dumpValue(false || false); // Noncompliant {{false}}
    probe(); // Noncompliant {{1}}
    dumpValue(false || true); // Noncompliant {{true}}
    probe(); // Noncompliant {{1}}
    dumpState(false || p2); // Noncompliant {{UNKNOWN}}
    probe(); // Noncompliant {{1}}
    dumpValue(true || false); // Noncompliant {{true}}
    probe(); // Noncompliant {{1}}
    dumpValue(true || true); // Noncompliant {{true}}
    probe(); // Noncompliant {{1}}
    dumpValue(true || p2); // Noncompliant {{true}}
    probe(); // Noncompliant {{1}}
  }

  private void testConditionalAnd(boolean parameter) {
    if (parameter && dumpState(parameter)) { // Noncompliant {{TRUE}}
      dumpState(parameter); // Noncompliant {{TRUE}}
      probe(); // Noncompliant {{1}}
    } else {
      dumpState(parameter); // Noncompliant {{FALSE/TRUE}}
      probe(); // Noncompliant {{2}}
    }
  }

  void testConditionalAndFalseAny() {
    if (false && probe()) { // Compliant
      probe(); // Compliant
    } else {
      probe(); // Noncompliant {{1}}
    }
    probe(); // Noncompliant {{1}}
  }

  void testConditionalAndNested(int i, int j, int k) {
    if (i < j && j < k) {
      dumpRelation(i, j); // Noncompliant {{LESS_THAN}}
      dumpRelation(j, k); // Noncompliant {{LESS_THAN}}
    } else {
      dumpRelation(i, j); // Noncompliant {{GREATER_EQUAL/LESS_THAN}}
      dumpRelation(j, k); // Noncompliant {{UNKNOWN/GREATER_EQUAL}}
    }
  }

  void testConditionalAndTrueAny() {
    if (true && probe()) { // Noncompliant {{1}}
      probe(); // Noncompliant {{1}}
    } else {
      probe(); // Noncompliant {{1}}
    }
  }

  void testConditionalAndTrueFalse() {
    if (true && false) {
      probe(); // Compliant
    } else {
      probe(); // Noncompliant {{1}}
    }
  }

  void testConditionalAndTrueTrue() {
    if (true && true) {
      probe(); // Noncompliant {{1}}
    } else {
      probe(); // Compliant
    }
  }

  void testConditionalAndUnknownFalse(boolean p1) {
    dumpValue(p1 && false); // Noncompliant {{false/false}}
  }

  void testConditionalAndUnknownTrue(boolean p1) {
    dumpState(p1 && true); // Noncompliant {{FALSE/TRUE}}
  }

  void testConditionalAndUnknownUnknown(boolean p1, boolean p2) {
    dumpState(p1 && p2); // Noncompliant {{FALSE/UNKNOWN}}
  }

  private void testConditionalOr(boolean parameter) {
    if (parameter || dumpState(parameter)) { // Noncompliant {{FALSE}}
      dumpState(parameter); // Noncompliant {{FALSE/TRUE}}
      probe(); // Noncompliant {{2}}
    } else {
      dumpState(parameter); // Noncompliant {{FALSE}}
      probe(); // Noncompliant {{1}}
    }
  }

  void testConditionalOrFalseAny() {
    if (false || probe()) { // Noncompliant {{1}}
      probe(); // Noncompliant {{1}}
    } else {
      probe(); // Noncompliant {{1}}
    }
  }

  void testConditionalOrFalseFalse() {
    if (false || false) {
      probe(); // Compliant
    } else {
      probe(); // Noncompliant {{1}}
    }
  }

  void testConditionalOrFalseTrue() {
    if (false || true) {
      probe(); // Noncompliant {{1}}
    } else {
      probe(); // Compliant
    }
  }

  void testConditionalOrNested(int i, int j, int k) {
    if (i < j || j < k) {
      dumpRelation(i, j); // Noncompliant {{GREATER_EQUAL/LESS_THAN}}
      dumpRelation(j, k); // Noncompliant {{LESS_THAN/UNKNOWN}}
    } else {
      dumpRelation(i, j); // Noncompliant {{GREATER_EQUAL}}
      dumpRelation(j, k); // Noncompliant {{GREATER_EQUAL}}
    }
  }

  void testConditionalOrTrueAny() {
    if (false || probe()) { // Noncompliant {{1}}
      probe(); // Noncompliant {{1}}
    } else {
      probe(); // Noncompliant {{1}}
    }
  }

  void testConditionalOrUnknownFalse(boolean p1) {
    dumpState(p1 || false); // Noncompliant {{FALSE/TRUE}}
  }

  void testConditionalOrUnknownTrue(boolean p1) {
    dumpValue(p1 || true); // Noncompliant {{true/true}}
  }

  void testConditionalOrUnknownUnknown(boolean p1, boolean p2) {
    dumpState(p1 || p2); // Noncompliant {{UNKNOWN/TRUE}}
  }

  void testContinue(boolean condition) {
    while (true) {
      probe(); // Noncompliant {{1}}
      continue;
      probe(); // Compliant, unreachable
    }
    probe(); // Compliant, unreachable
  }

  void testDoWhileBreak(boolean a, boolean b) {
    a = true;
    b = false;
    do {
      a = false;
      break;
      probe(); // Compliant
    } while (probe()); // Compliant
    probe(); // Noncompliant {{1}}
    dumpState(a); // Noncompliant {{UNKNOWN}}
    dumpValue(b); // Noncompliant {{false}}
  }

  void testDoWhileFalse() {
    do {
      probe(); // Noncompliant {{1}}
    } while (false);
    probe(); // Noncompliant {{1}}
  }

  void testDoWhileInvalidate(boolean b, boolean c, boolean condition) {
    // variables assigned in the loop are invalidated.
    b = false;
    c = true;
    do {
      dumpValue(b); // Noncompliant {{false}}
      dumpState(c); // Noncompliant {{UNKNOWN}}
      c = false;
      dumpValue(c); // Noncompliant {{false}}
    } while (condition);
    dumpValue(b); // Noncompliant {{false}}
    dumpState(c); // Noncompliant {{UNKNOWN}}
  }

  void testDoWhileTrue() {
    do {
      probe(); // Noncompliant {{1}}
    } while (true);
    probe(); // Compliant
  }

  void testDoWhileUnknown(boolean condition) {
    do {
      probe(); // Noncompliant {{1}}
    } while (condition);
    probe(); // Noncompliant {{1}}
  }

  void testFor() {
    boolean a = true, b = true, c = true;
    for (dumpValue(a = false, b, c) // Noncompliant {{false,true,true}}
    , dumpValue(a, b = a, c) // Noncompliant {{false,false,true}}
    , dumpValue(a, b, c = b); // Noncompliant {{false,false,false}}
    probe(); // Noncompliant {{1}}
    dumpState(a = true, b, c) // Noncompliant {{TRUE,UNKNOWN,UNKNOWN}}
    , dumpState(a, b = a, c) // Noncompliant {{TRUE,TRUE,UNKNOWN}}
    , dumpState(a, b, c = b)) { // Noncompliant {{TRUE,TRUE,TRUE}}
      probe(); // Noncompliant {{1}}
    }
    probe(); // Noncompliant {{1}}
  }

  void testForBreak(boolean a, boolean b) {
    a = true;
    b = false;
    for (; true;) {
      a = false;
      break;
      probe(); // Compliant
    }
    probe(); // Noncompliant {{1}}
    dumpState(a); // Noncompliant {{UNKNOWN}}
    dumpValue(b); // Noncompliant {{false}}
  }

  void testForFalse() {
    for (; false; probe()) { // Compliant, unreachable
      probe(); // Compliant, unreachable
    }
    probe(); // Noncompliant {{1}}
  }

  void testForEach(boolean a, boolean b, boolean c) {
    a = true;
    b = true;
    for (String string : probe()) { // Noncompliant {{1}}
      probe(); // Noncompliant {{1}}
      dumpValue(a); // Noncompliant {{true}}
      dumpState(b); // Noncompliant {{UNKNOWN}}
      b = false;
      dumpValue(b); // Noncompliant {{false}}
      c = a;
    }
    dumpValue(a); // Noncompliant {{true/true}}
    dumpState(b); // Noncompliant {{UNKNOWN/UNKNOWN}}
    dumpState(c); // Noncompliant {{UNKNOWN/UNKNOWN}}
  }

  void testForEachExpression(boolean a, String[] b, String[] c) {
    for (String string : a ? b : c) {
      probe(); // Noncompliant {{2}}
    }
    probe(); // Noncompliant {{4}}
  }

  void testForInvalidate(boolean a, boolean b, boolean c, boolean d, boolean condition) {
    a = true;
    b = true;
    for (c = true; condition; d = true) {
      dumpValue(a); // Noncompliant {{true}}
      dumpState(b); // Noncompliant {{UNKNOWN}}
      // FIXME: initializer is currently invalidated
      dumpState(c); // Noncompliant {{UNKNOWN}}
      dumpState(d); // Noncompliant {{UNKNOWN}}
      b = false;
      dumpValue(b); // Noncompliant {{false}}
    }
    dumpValue(a); // Noncompliant {{true}}
    dumpState(b, c, d); // Noncompliant {{UNKNOWN,UNKNOWN,UNKNOWN}}
  }

  void testForTrue() {
    for (; true; probe()) { // Noncompliant {{1}}
      probe(); // Noncompliant {{1}}
    }
  }

  void testForUnknown(boolean condition) {
    for (; condition; probe()) { // Noncompliant {{1}}
      probe(); // Noncompliant {{1}}
    }
    probe(); // Noncompliant {{1}}
  }

  void testForWithoutCondition() {
    for (;; probe()) { // Noncompliant {{1}}
      probe(); // Noncompliant {{1}}
    }
    probe(); // Compliant
  }

  void testForWithoutConditionWithBreak() {
    for (;; probe()) { // Compliant
      probe(); // Noncompliant {{1}}
      break;
      probe(); // Compliant
    }
    probe(); // Noncompliant {{1}}
  }

  void testIdentifierIfConditionFalse(boolean b) {
    if (b == false) {
      dumpState(b); // Noncompliant {{FALSE}}
      probe(); // Noncompliant {{1}}
    }
    probe(); // Noncompliant {{2}}
    dumpState(b); // Noncompliant {{TRUE/FALSE}}
  }

  void testIdentifierIfConditionFalseWithElse(boolean b) {
    if (b == false) {
      dumpState(b); // Noncompliant {{FALSE}}
    } else {
      dumpState(b); // Noncompliant {{TRUE}}
    }
    dumpState(b); // Noncompliant {{TRUE/FALSE}}
  }

  void testIdentifierIfConditionImplicit(boolean b) {
    if (b) {
      dumpState(b); // Noncompliant {{TRUE}}
    }
    dumpState(b); // Noncompliant {{FALSE/TRUE}}
  }

  void testIdentifierIfConditionImplicitWithElse(boolean b) {
    if (b) {
      dumpState(b); // Noncompliant {{TRUE}}
    } else {
      dumpState(b); // Noncompliant {{FALSE}}
    }
    dumpState(b); // Noncompliant {{FALSE/TRUE}}
  }

  void testIdentifierIfConditionTrue(boolean b) {
    if (b == true) {
      dumpState(b); // Noncompliant {{TRUE}}
    }
    dumpState(b); // Noncompliant {{FALSE/TRUE}}
  }

  void testIdentifierIfConditionTrueWithElse(boolean b) {
    if (b == true) {
      dumpState(b); // Noncompliant {{TRUE}}
    } else {
      dumpState(b); // Noncompliant {{FALSE}}
    }
    dumpState(b); // Noncompliant {{FALSE/TRUE}}
  }

  void testIfIdentifierFalse(boolean b) {
    b = false;
    if (b) {
      probe(); // compliant, unreachable
    } else {
      probe(); // Noncompliant {{1}}
    }
  }

  void testIdentifierIfIdentifierTrue(boolean b) {
    b = true;
    if (b) {
      probe(); // Noncompliant {{1}}
    } else {
      probe(); // compliant, unreachable
    }
  }

  void testIfMerge(boolean a, boolean b, boolean c, boolean d, boolean condition) {
    a = true; // assigned to true in true branch -> true
    b = true; // assigned to true in false branch -> true
    c = true; // assigned to false in both branches -> false
    if (condition) {
      a = true;
      c = false;
      d = false;
    } else {
      b = true;
      c = false;
      d = true;
    }
    dumpValue(a, b, c, d); // Noncompliant {{true,true,false,true/true,true,false,false}}
  }

  void testInstanceOf(Object instance) {
    // unsupported now
    dumpState(instance instanceof Object); // Noncompliant {{UNKNOWN}}
    // unsupported now
    if (instance instanceof Object) {
      probe(); // Noncompliant {{1}}
    } else {
      probe(); // Noncompliant {{1}}
    }
  }

  void testLiterals() {
    dumpState(false); // Noncompliant {{FALSE}}
    dumpValue(false); // Noncompliant {{false}}
    dumpState(true); // Noncompliant {{TRUE}}
    dumpValue(true); // Noncompliant {{true}}
    dumpState(null); // Noncompliant {{UNKNOWN}}
    dumpValue(1); // Noncompliant {{1}}
    dumpValue(1L); // Noncompliant {{1}}
  }

  private boolean field, field1, field2;

  void testMemberSelect(boolean b) {
    super.field = false;
    field = true;
    dumpValue(super.field, field, this.field); // Noncompliant {{false,true,true}}

    super.field = false;
    this.field = true;
    dumpValue(super.field, field, this.field); // Noncompliant {{false,true,true}}

    BaseClass other = new BaseClass();
    other.field = true;
    dumpState(other.field); // Noncompliant {{UNKNOWN}}

    new Object[3][b = true].hashCode();
    dumpValue(b); // Noncompliant {{true}}
  }

  void testMemberSelectCondition(boolean b) {
    if (this.field1 == this.field2) {
      dumpRelation(this.field1, this.field2); // Noncompliant {{EQUAL_TO}}
    }
  }

  void testMemberSelectCondition(boolean b) {
    if (this.field1) {
      dumpState(this.field1); // Noncompliant {{TRUE}}
    }
  }

  void testMethodInvocation() {
    boolean local1 = true, local2 = true;
    field1 = true;
    field2 = true;
    if (field1 == field2 && field1 == local1) {
      testMethodInvocation();
      dumpState(field1, field2); // Noncompliant {{UNKNOWN,UNKNOWN}}
      dumpRelation(field1, field2); // Noncompliant {{UNKNOWN}}
      dumpRelation(field1, local2); // Noncompliant {{UNKNOWN}}
    }
  }

  void testNewArrayClass() {
   new Object[][] {{1}, {1}};
   // checking emptiness of stack.
   new Object[1][1];
   // checking emptiness of stack.
   new RuntimeException("");
   // checking emptiness of stack.
   }

  void testLogicalNot(boolean value) {
    dumpState(!true); // Noncompliant {{FALSE}}
    dumpValue(!true); // Noncompliant {{false}}
    dumpState(!false); // Noncompliant {{TRUE}}
    dumpValue(!false); // Noncompliant {{true}}
    dumpState(!value); // Noncompliant {{UNKNOWN}}
    value = true;
    dumpValue(!value); // Noncompliant {{false}}
    value = false;
    dumpValue(!value); // Noncompliant {{true}}
  }

  void testLogicalNotConstraintFalse(boolean value) {
    if (value == false) {
      dumpValue(!value); // Noncompliant {{true}}
    }
  }

  void testLogicalNotConstraintTrue(boolean value) {
    if (value == true) {
      dumpValue(!value); // Noncompliant {{false}}
    }
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

  void testRelationEqual(int a, int b) {
    if (a == b) {
      dumpRelation(a, b); // Noncompliant {{EQUAL_TO}}
      dumpValue(a == b); // Noncompliant {{true}}
      dumpValue(a >= b); // Noncompliant {{true}}
      dumpValue(a > b); // Noncompliant {{false}}
      dumpValue(a <= b); // Noncompliant {{true}}
      dumpValue(a < b); // Noncompliant {{false}}
      dumpValue(a != b); // Noncompliant {{false}}
    } else {
      dumpRelation(a, b); // Noncompliant {{NOT_EQUAL}}
    }
  }

  void testRelationGreaterEqual(int a, int b) {
    if (a >= b) {
      dumpRelation(a, b); // Noncompliant {{GREATER_EQUAL}}
      dumpState(a == b); // Noncompliant {{UNKNOWN}}
      dumpValue(a >= b); // Noncompliant {{true}}
      dumpState(a > b); // Noncompliant {{UNKNOWN}}
      dumpState(a <= b); // Noncompliant {{UNKNOWN}}
      dumpValue(a < b); // Noncompliant {{false}}
      dumpState(a != b); // Noncompliant {{UNKNOWN}}
    } else {
      dumpRelation(a, b); // Noncompliant {{LESS_THAN}}
    }
  }

  void testRelationGreaterThan(int a, int b) {
    if (a > b) {
      dumpRelation(a, b); // Noncompliant {{GREATER_THAN}}
      dumpValue(a == b); // Noncompliant {{false}}
      dumpValue(a >= b); // Noncompliant {{true}}
      dumpValue(a > b); // Noncompliant {{true}}
      dumpValue(a <= b); // Noncompliant {{false}}
      dumpValue(a < b); // Noncompliant {{false}}
      dumpValue(a != b); // Noncompliant {{true}}
    } else {
      dumpRelation(a, b); // Noncompliant {{LESS_EQUAL}}
    }
  }

  void testRelationLessEqual(int a, int b) {
    if (a <= b) {
      dumpRelation(a, b); // Noncompliant {{LESS_EQUAL}}
      dumpState(a == b); // Noncompliant {{UNKNOWN}}
      dumpState(a >= b); // Noncompliant {{UNKNOWN}}
      dumpValue(a > b); // Noncompliant {{false}}
      dumpValue(a <= b); // Noncompliant {{true}}
      dumpState(a < b); // Noncompliant {{UNKNOWN}}
      dumpState(a != b); // Noncompliant {{UNKNOWN}}
    } else {
      dumpRelation(a, b); // Noncompliant {{GREATER_THAN}}
    }
  }

  void testRelationLessThan(int a, int b) {
    if (a < b) {
      dumpRelation(a, b); // Noncompliant {{LESS_THAN}}
      dumpValue(a == b); // Noncompliant {{false}}
      dumpValue(a >= b); // Noncompliant {{false}}
      dumpValue(a > b); // Noncompliant {{false}}
      dumpValue(a <= b); // Noncompliant {{true}}
      dumpValue(a < b); // Noncompliant {{true}}
      dumpValue(a != b); // Noncompliant {{true}}
    } else {
      dumpRelation(a, b); // Noncompliant {{GREATER_EQUAL}}
    }
  }

  void testRelationNested(int a, int b) {
    if (a == b) {
      if (a == b) {
        probe(); // Noncompliant {{1}}
      } else {
        probe(); // Compliant
      }
      if (a != b) {
        probe(); // Compliant
      } else {
        probe(); // Noncompliant {{1}}
      }
    }
  }

  void testRelationNotEqual(int a, int b) {
    if (a != b) {
      dumpRelation(a, b); // Noncompliant {{NOT_EQUAL}}
      dumpValue(a == b); // Noncompliant {{false}}
      dumpState(a >= b); // Noncompliant {{UNKNOWN}}
      dumpState(a > b); // Noncompliant {{UNKNOWN}}
      dumpState(a <= b); // Noncompliant {{UNKNOWN}}
      dumpState(a < b); // Noncompliant {{UNKNOWN}}
      dumpValue(a != b); // Noncompliant {{true}}
    } else {
      dumpRelation(a, b); // Noncompliant {{EQUAL_TO}}
    }
  }

  void testReturn(boolean b, boolean condition) {
    b = false;
    if (condition) {
      b = true;
      return probe(); // Noncompliant {{1}}
      probe(); // Compliant, unreachable
    }
    probe(); // Noncompliant {{1}}
    dumpValue(b); // Noncompliant {{false}}
  }

  void testSynchronized(boolean a, boolean b, boolean c, Object object) {
    a = true;
    c = true;
    synchronized (object) {
      b = false;
      c = false;
      probe(); // Noncompliant {{1}}
    }
    probe(); // Noncompliant {{1}}
    dumpValue(a, b, c); // Noncompliant {{true,false,false}}
  }

  void testSwitch(boolean a) {
    a = true;
    switch (condition) {
      case 0:
        probe(); // Noncompliant {{1}}
        a = false;
      case 1:
        probe(); // Noncompliant {{2}}
        a = false;
    }
    probe(); // Noncompliant {{3}}
    dumpState(a); // Noncompliant {{FALSE/FALSE/TRUE}}
    dumpValue(a); // Noncompliant {{false/false/true}}
  }

  void testSwitchBreak(boolean a, boolean condition) {
    switch (condition) {
      default:
        a = true;
        if (condition) {
          break;
        } else {
          break;
        }
        probe(); // Compliant, unreachable
    }
    dumpValue(a); // Noncompliant {{true/true}}
  }

  void testSwitchDefault(boolean a) {
    a = true;
    switch (condition) {
      case 0:
        probe(); // Noncompliant {{1}}
        a = false;
        break;
      case 1:
        probe(); // Noncompliant {{1}}
      default:
        probe(); // Noncompliant {{2}}
        a = false;
    }
    probe(); // Noncompliant {{3}}
    dumpState(a); // Noncompliant {{FALSE/FALSE/FALSE}}
    dumpValue(a); // Noncompliant {{false/false/false}}
  }

  void testSwitchFallthrough(boolean a, boolean b, boolean c, int condition) {
    a = true;
    switch (condition) {
      case 0:
        probe(); // Noncompliant {{1}}
        a = false;
      case 1:
      case 2:
        probe(); // Noncompliant {{2}}
        break;
      case 3:
        probe(); // Noncompliant {{1}}
        dumpValue(a); // Noncompliant {{true}}
        a = false;
    }
    // FIXME: 1 and 2 are evaluated once, whereas two paths are feasible. this is fine as long as we don't retrieve any information from the
    // condition.
    probe(); // Noncompliant {{4}}
    dumpValue(a); // Noncompliant {{false/true/false/true}}
  }

  void testSwitchUnconditionalReturn(int condition) {
    switch (condition) {
      default:
        return;
    }
    probe(); // Compliant, unreachable
  }

  void testSynchronized(boolean condition) {
    synchronized (condition = true) {
      dumpValue(condition); // Noncompliant {{true}}
    }
  }

  void testTernary(boolean a) {
    a
      ? dumpState(a) // Noncompliant {{TRUE}}
      : dumpState(a); // Noncompliant {{FALSE}}
  }

  void testThrow(boolean b, boolean condition) {
    b = false;
    if (condition) {
      b = true;
      throw probe(); // Noncompliant {{1}}
      probe(); // Compliant, unreachable
    }
    probe(); // Noncompliant {{1}}
    dumpValue(b); // Noncompliant {{false}}
  }

  void testTry(boolean a, boolean b, boolean c) {
    a = true;
    b = true;
    c = true;
    try {
      probe(); // Noncompliant {{1}}
      dumpValue(a, b, c); // Noncompliant {{true,true,true}}
      a = false;
      b = false;
    } catch (Exception e) {
      probe(); // Noncompliant {{1}}
      // FIXME: all is invalidated here, but it is probably enough to invalidate only the variables modified in the try block.
      dumpState(a, b, c); // Noncompliant {{UNKNOWN,UNKNOWN,UNKNOWN}}
      a = false;
      b = false;
      c = false;
    } catch (Exception e) {
      probe(); // Noncompliant {{1}}
      // FIXME: all is invalidated here, but it is probably enough to invalidate only the variables modified in the try block.
      dumpState(a, b, c); // Noncompliant {{UNKNOWN,UNKNOWN,UNKNOWN}}
      a = false;
      c = false;
    } finally {
      probe(); // Noncompliant {{3}}
      // FIXME: multiple values here. how should this be dumped?
      // dumpState(a, b, c); // Noncompliant {{FALSE,UNKNOWN,FALSE}}
      a = false;
      b = false;
      c = false;
    }
    probe(); // Noncompliant {{3}}
    dumpValue(a, b, c); // Noncompliant {{false,false,false/false,false,false/false,false,false}}
  }

  void testUnarySideEffect(boolean value) {
   // the following is illegal in java
   value = true;
   +value;
   dumpValue(value); // Noncompliant {{true}}
   }

  void testUnarySideEffect(boolean value) {
    // the following is illegal in java
    value = true;
    value++;
    dumpState(value); // Noncompliant {{UNKNOWN}}
  }

  void testWhileBreak(boolean a, boolean b) {
    a = true;
    b = false;
    while (true) {
      a = false;
      break;
      probe(); // Compliant
    }
    dumpState(a); // Noncompliant {{UNKNOWN}}
    dumpValue(b); // Noncompliant {{false}}
  }

  void testWhileFalse() {
    while (false) {
      probe(); // Compliant, unreachable
    }
    probe(); // Noncompliant {{1}}
  }

  void testWhileTrue() {
    while (true) {
      probe(); // Noncompliant {{1}}
    }
    probe(); // Compliant, unreachable
  }

  void testWhileInvalidate(boolean a, boolean b, boolean c, boolean d, boolean condition) {
    a = true;
    b = true;
    while (condition) {
      dumpValue(a); // Noncompliant {{true}}
      dumpState(b); // Noncompliant {{UNKNOWN}}
      b = false;
      dumpValue(b); // Noncompliant {{false}}
    }
    dumpValue(a); // Noncompliant {{true}}
    dumpState(b); // Noncompliant {{UNKNOWN}}
  }

  void testWhileUnknown(boolean condition) {
    while (condition) {
      probe(); // Noncompliant {{1}}
    }
    probe(); // Noncompliant {{1}}
  }

}

class BaseClass {
  boolean field;
}
