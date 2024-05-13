package checks;

import java.util.Optional;
import javax.annotation.CheckForNull;
import javax.annotation.Nonnull;
import javax.validation.constraints.NotNull;

class BoxedBooleanExpressionsCheckSample {

  String foo(Object value) {
    if (value == null) return "";
    return (Boolean) value ? "1" : "0"; // Compliant
  }

  String foo(String value) {
    if (value == null) return "";
    return Boolean.parseBoolean(value) ? "1" : "0"; // Compliant
  }

  String foo2(Object value) {
    return (Boolean) value ? "1" : "0"; // Noncompliant
  }

  void emptyFor(Boolean B) {
    for (;;) {
      foo();
    }
  }

  void boxedFor1(Boolean B) {
    for (;B;) { // Noncompliant {{Use a primitive boolean expression here.}}
      foo();
    }
  }

  void boxedFor2(Boolean B) {
    for (;Boolean.TRUE.equals(B);) { // Compliant
      foo();
    }
  }

  void primitiveFor(boolean b) {
    for (;b;) {
      foo();
    }
  }

  void nullCheckFor(Boolean B) {
    for (;B != null;) {
      foo();
    }
  }

  void boxedWhile1(Boolean B) {
    while (B) { // Noncompliant
      foo();
    }
  }

  void boxedWhil2(Boolean B) {
    while (Boolean.TRUE.equals(B)) { // Compliant
      foo();
    }
  }

  void primitiveWhile(boolean b) {
    while (b) {
      foo();
    }
  }

  void nullCheckWhile(Boolean B) {
    while (B != null) {
      foo();
    }
  }

  void boxedDoWhile1(Boolean B) {
    do {
      foo();
    } while (B); // Noncompliant
  }

  void boxedDoWhile2(Boolean B) {
    do {
      foo();
    } while (Boolean.TRUE.equals(B)); // Compliant
  }

  void primitiveDoWhile(boolean b) {
    do {
      foo();
    } while (b);
  }

  void nullCheckDoWhile(Boolean B) {
    do {
      foo();
    } while (B != null);
  }

  void boxedIf1(Boolean B) {
    if (B) { // Noncompliant
      foo();
    } else {
      bar();
    }
  }

  void boxedIf2(Boolean B) {
    if (Boolean.TRUE.equals(B)) { // Compliant
      foo();
    } else {
      bar();
    }
  }

  void primitiveIf(boolean b) {
    if (b) {
      foo();
    } else {
      bar();
    }
  }

  void nullCheckIf(Boolean B) {
    if (B != null) {
      foo();
    } else {
      bar();
    }
  }

  void boxedNotEqual1(Boolean B, boolean b) {
    if (B != b) { // Noncompliant
      foo();
    } else {
      bar();
    }
  }

  void boxedNotEqual2(Boolean B, boolean b) {
    if (b != B) { // Noncompliant
      foo();
    } else {
      bar();
    }
  }

  void boxedNotEqual3(Boolean B, Boolean V) {
    if (B != V) {
      foo();
    } else {
      bar();
    }
  }

  void primitiveNotEqual(boolean b, boolean v) {
    if (b != v) {
      foo();
    } else {
      bar();
    }
  }

  void boxedEqual1(Boolean B, boolean b) {
    if (B == b) { // Noncompliant
      foo();
    } else {
      bar();
    }
  }

  void boxedEqual2(Boolean B, boolean b) {
    if (b == B) { // Noncompliant
      foo();
    } else {
      bar();
    }
  }

  void boxedEqual3(Boolean B, Boolean V) {
    if (B == V) {
      foo();
    } else {
      bar();
    }
  }

  void nullCheck1(Boolean B) {
    if (B == null) {
      foo();
    } else {
      bar();
    }
  }

  void nullCheck2(Boolean B) {
    if (B != null) {
      foo();
    } else {
      bar();
    }
  }

  void nullCheck3(Boolean B) {
    if (B != null && B) {
      foo();
    } else {
      bar();
    }
  }

  void nullCheck4(Boolean B) {
    if (null != B && B) {
      foo();
    } else {
      bar();
    }
  }

  void compoundCheck(Boolean B, boolean b, boolean v) {
    if (b != v && B) { // Noncompliant
      foo();
    } else {
      bar();
    }
  }

  void primitiveEqual(boolean b, boolean v) {
    if (b == v) {
      foo();
    } else {
      bar();
    }
  }

  void boxedComplement(Boolean B) {
    if (!B) { // Noncompliant
      foo();
    } else {
      bar();
    }
  }

  void primitiveComplement(boolean b) {
    if (!b) {
      foo();
    } else {
      bar();
    }
  }

  void boxedFunction1() {
    if (True()) { // Noncompliant
      foo();
    } else {
      bar();
    }
  }

  void boxedFunction2(Boolean B) {
    if (True() && B) {
      foo();
    } else {
      bar();
    }
  }

  void boxedFunction3(boolean b) {
    if (True() || b) { // Noncompliant
      foo();
    } else {
      bar();
    }
  }

  void boxedFunction4() {
    if (True() == False()) {
      foo();
    } else {
      bar();
    }
  }

  void boxedOptional() {
    if (Optional.of(True()).orElse(False())) {
      foo();
    } else {
      bar();
    }
  }

  void boxedOptional2() {
    if (Optional.of(True()).orElse(null)) { // Noncompliant
      foo();
    } else {
      bar();
    }
  }

  void nonNullStuff() {
    if (getNotNull()) { // Compliant
      foo();
    }

    if (getNonnull()) { // Compliant
      foo();
    }

    if (getCustomNonNull()) { // Compliant
      foo();
    }

    if (getNullable()) { // Noncompliant
      foo();
    }
  }

  Object boxedConditional1(Boolean B) {
    return B ? foo() : bar(); // Noncompliant
  }

  Object boxedConditional2(Boolean B) {
    return Boolean.TRUE.equals(B) ? foo() : bar(); // Compliant
  }

  Object boxedConditional3(Boolean B, boolean b) {
    return B != b ? foo() : bar(); // Noncompliant
  }

  Object nullCheckConditional(Boolean B) {
    return B != null ? foo() : bar();
  }

  Object primitiveConditional(boolean b) {
    return b ? foo() : bar();
  }

  Boolean True() {
    return Boolean.TRUE;
  }

  Boolean False() {
    return Boolean.FALSE;
  }

  public @interface NonNull {}
  // no need to be a well-known annotation, the semantic of the name is enough
  @NonNull Boolean getCustomNonNull() { return Math.random() > 0.5 ? Boolean.FALSE : Boolean.TRUE; }

  @Nonnull Boolean getNonnull() { return getCustomNonNull(); }
  @NotNull Boolean getNotNull() { return getCustomNonNull(); }
  @CheckForNull Boolean getNullable() { return null; }

  Object foo() { return new Object(); }
  Object bar() { return new Object(); }

  Boolean getSurprizeBoxedBoolean() {
    double random = Math.random();
    if (random < 0.34) {
      return Boolean.FALSE;
    }
    if (random < 0.67) {
      return Boolean.TRUE;
    }
    return null;
  }

  void forStatementCheckedBeforeUse() {
    final Boolean alwaysTrue = Boolean.TRUE;
    Boolean checkedBeforeUse = getSurprizeBoxedBoolean();
    if (checkedBeforeUse != null) {
      for (; checkedBeforeUse; )  {} // Compliant because the variable is checked before use
    }
    Boolean checkedBeforeUse2 = getSurprizeBoxedBoolean();
    if (checkedBeforeUse2 == null) {
      return;
    }
    for (; checkedBeforeUse2; ) {} // Compliant because the variable is checked before use
    Boolean checkedBeforeUse3 = getSurprizeBoxedBoolean();
    if (checkedBeforeUse3 == null) {
      for (; checkedBeforeUse3; ) {} // Compliant FN because the value is checked but still used
    }
    Boolean checkedBeforeUse4 = getSurprizeBoxedBoolean();
    if (checkedBeforeUse4 == alwaysTrue) {
      for (; checkedBeforeUse4; ) {} // Noncompliant
    }
    Boolean checkedBeforeUse5 = getSurprizeBoxedBoolean();
    if (checkedBeforeUse5 == Boolean.TRUE) {
      for (; checkedBeforeUse5; ) {} // Noncompliant
    }
    Boolean checkedBeforeUse6 = getSurprizeBoxedBoolean();
    if (alwaysTrue.equals(checkedBeforeUse6)) {
      for (; checkedBeforeUse6; ) {} // Noncompliant
    }
    Boolean checkedBeforeUse7 = getSurprizeBoxedBoolean();
    if (Boolean.FALSE.equals(checkedBeforeUse7)) {
      for (; checkedBeforeUse7; ) {} // Noncompliant
    }

    Boolean checkedBeforeUse8 = getSurprizeBoxedBoolean();
    Boolean checkedBeforeUse9 = getSurprizeBoxedBoolean();
    Boolean checkedBeforeUse10 = getSurprizeBoxedBoolean();
    Boolean checkedBeforeUse11 = getSurprizeBoxedBoolean();
    Boolean checkedBeforeUse12 = getSurprizeBoxedBoolean();
    Boolean checkedBeforeUse13 = getSurprizeBoxedBoolean();
    if (1 == 2) {
    } else if (checkedBeforeUse8 != null) {
      for (; checkedBeforeUse8; ) {} // Compliant because the variable is checked before use
    } else if (checkedBeforeUse9 == null) {
      for (; checkedBeforeUse9; ) {} // Compliant FN because the value is checked but sill used
    } else if (checkedBeforeUse10 == alwaysTrue) {
      for (; checkedBeforeUse10; ) {} // Noncompliant
    } else if (checkedBeforeUse11 == Boolean.FALSE) {
      for (; checkedBeforeUse11; ) {} // Noncompliant
    } else if (alwaysTrue.equals(checkedBeforeUse12)) {
      for (; checkedBeforeUse12; ) {} // Noncompliant
    } else if (Boolean.FALSE.equals(checkedBeforeUse13)) {
      for (; checkedBeforeUse13; ) {} // Noncompliant
    }
}

  void whileStatementCheckedBeforeUse() {
    final Boolean alwaysTrue = Boolean.TRUE;
    Boolean checkedBeforeUse = getSurprizeBoxedBoolean();
    if (checkedBeforeUse != null) {
      while (checkedBeforeUse)  {} // Compliant because the variable is checked before use
    }
    Boolean checkedBeforeUse2 = getSurprizeBoxedBoolean();
    if (checkedBeforeUse2 == null) {
      return;
    }
    while (checkedBeforeUse2)  {} // Compliant because the variable is checked before use
    Boolean checkedBeforeUse3 = getSurprizeBoxedBoolean();
    if (checkedBeforeUse3 == null) {
      while (checkedBeforeUse3) {} // Compliant FN because the value is checked but still used
    }
    Boolean checkedBeforeUse4 = getSurprizeBoxedBoolean();
    if (checkedBeforeUse4 == alwaysTrue) {
      while (checkedBeforeUse4)  {} // Noncompliant
    }
    Boolean checkedBeforeUse5 = getSurprizeBoxedBoolean();
    if (checkedBeforeUse5 == Boolean.TRUE) {
      while (checkedBeforeUse5) {} // Noncompliant
    }
    Boolean checkedBeforeUse6 = getSurprizeBoxedBoolean();
    if (alwaysTrue.equals(checkedBeforeUse6)) {
      while (checkedBeforeUse6) {} // Noncompliant
    }
    Boolean checkedBeforeUse7 = getSurprizeBoxedBoolean();
    if (Boolean.FALSE.equals(checkedBeforeUse7)) {
      while (checkedBeforeUse7) {} // Noncompliant
    }

    Boolean checkedBeforeUse8 = getSurprizeBoxedBoolean();
    Boolean checkedBeforeUse9 = getSurprizeBoxedBoolean();
    Boolean checkedBeforeUse10 = getSurprizeBoxedBoolean();
    Boolean checkedBeforeUse11 = getSurprizeBoxedBoolean();
    Boolean checkedBeforeUse12 = getSurprizeBoxedBoolean();
    Boolean checkedBeforeUse13 = getSurprizeBoxedBoolean();
    if (1 == 2) {
    } else if (checkedBeforeUse8 != null) {
      while (checkedBeforeUse8) {} // Compliant because the variable is checked before use
    } else if (checkedBeforeUse9 == null) {
      while (checkedBeforeUse9) {} // Compliant FN because the value is checked but sill used
    } else if (checkedBeforeUse10 == alwaysTrue) {
      while (checkedBeforeUse10) {} // Noncompliant
    } else if (checkedBeforeUse11 == Boolean.FALSE) {
      while (checkedBeforeUse11) {} // Noncompliant
    } else if (alwaysTrue.equals(checkedBeforeUse12)) {
      while (checkedBeforeUse12) {} // Noncompliant
    } else if (Boolean.FALSE.equals(checkedBeforeUse13)) {
      while (checkedBeforeUse13) {} // Noncompliant
    }
  }

  void doWhileStatementCheckedBeforeUse() {
    final Boolean alwaysTrue = Boolean.TRUE;
    Boolean checkedBeforeUse = getSurprizeBoxedBoolean();
    if (checkedBeforeUse != null) {
      do {}
      while (checkedBeforeUse); // Compliant because the variable is checked before use
    }
    Boolean checkedBeforeUse2 = getSurprizeBoxedBoolean();
    if (checkedBeforeUse2 == null) {
      return;
    }
    do {} while (checkedBeforeUse2); // Compliant because the variable is checked before use
    Boolean checkedBeforeUse3 = getSurprizeBoxedBoolean();
    if (checkedBeforeUse3 == null) {
      do {} while (checkedBeforeUse3); // Compliant FN because the value is checked but still used
    }
    Boolean checkedBeforeUse4 = getSurprizeBoxedBoolean();
    if (checkedBeforeUse4 == alwaysTrue) {
      do {} while (checkedBeforeUse4); // Noncompliant
    }
    Boolean checkedBeforeUse5 = getSurprizeBoxedBoolean();
    if (checkedBeforeUse5 == Boolean.TRUE) {
      do {} while (checkedBeforeUse5); // Noncompliant
    }
    Boolean checkedBeforeUse6 = getSurprizeBoxedBoolean();
    if (alwaysTrue.equals(checkedBeforeUse6)) {
      do {} while (checkedBeforeUse6); // Noncompliant
    }
    Boolean checkedBeforeUse7 = getSurprizeBoxedBoolean();
    if (Boolean.FALSE.equals(checkedBeforeUse7)) {
      do {} while (checkedBeforeUse7); // Noncompliant
    }

    Boolean checkedBeforeUse8 = getSurprizeBoxedBoolean();
    Boolean checkedBeforeUse9 = getSurprizeBoxedBoolean();
    Boolean checkedBeforeUse10 = getSurprizeBoxedBoolean();
    Boolean checkedBeforeUse11 = getSurprizeBoxedBoolean();
    Boolean checkedBeforeUse12 = getSurprizeBoxedBoolean();
    Boolean checkedBeforeUse13 = getSurprizeBoxedBoolean();
    if (1 == 2) {
    } else if (checkedBeforeUse8 != null) {
      do {} while (checkedBeforeUse8); // Compliant because the variable is checked before use
    } else if (checkedBeforeUse9 == null) {
      do {} while (checkedBeforeUse9); // Compliant FN because the value is checked but sill used
    } else if (checkedBeforeUse10 == alwaysTrue) {
      do {} while (checkedBeforeUse10); // Noncompliant
    } else if (checkedBeforeUse11 == Boolean.FALSE) {
      do {} while (checkedBeforeUse11); // Noncompliant
    } else if (alwaysTrue.equals(checkedBeforeUse12)) {
      do {} while (checkedBeforeUse12); // Noncompliant
    } else if (Boolean.FALSE.equals(checkedBeforeUse13)) {
      do {} while (checkedBeforeUse13); // Noncompliant
    }
  }

  void ifStatementCheckedBeforeUse() {
    final Boolean alwaysTrue = Boolean.TRUE;
    Boolean checkedBeforeUse = getSurprizeBoxedBoolean();
    if (checkedBeforeUse != null) {
      if (checkedBeforeUse) { // Compliant because the variable is checked before use
      } else {}
    }
    Boolean checkedBeforeUse2 = getSurprizeBoxedBoolean();
    if (checkedBeforeUse2 == null) {
      return;
    }
    if (checkedBeforeUse2) { // Compliant because the variable is checked before use
    } else {}
    Boolean checkedBeforeUse3 = getSurprizeBoxedBoolean();
    if (checkedBeforeUse3 == null) {
      if (checkedBeforeUse3) {} // Compliant FN because the value is checked but still used
    }
    Boolean checkedBeforeUse4 = getSurprizeBoxedBoolean();
    if (checkedBeforeUse4 == alwaysTrue) {
      if (checkedBeforeUse4) {} // Noncompliant
    }
    Boolean checkedBeforeUse5 = getSurprizeBoxedBoolean();
    if (checkedBeforeUse5 == Boolean.TRUE) {
      if (checkedBeforeUse5) {} // Noncompliant
    }
    Boolean checkedBeforeUse6 = getSurprizeBoxedBoolean();
    if (alwaysTrue.equals(checkedBeforeUse6)) {
      if (checkedBeforeUse6) {} // Noncompliant
    }
    Boolean checkedBeforeUse7 = getSurprizeBoxedBoolean();
    if (Boolean.FALSE.equals(checkedBeforeUse7)) {
      if (checkedBeforeUse7) {} // Noncompliant
    }

    Boolean checkedBeforeUse8 = getSurprizeBoxedBoolean();
    Boolean checkedBeforeUse9 = getSurprizeBoxedBoolean();
    Boolean checkedBeforeUse10 = getSurprizeBoxedBoolean();
    Boolean checkedBeforeUse11 = getSurprizeBoxedBoolean();
    Boolean checkedBeforeUse12 = getSurprizeBoxedBoolean();
    Boolean checkedBeforeUse13 = getSurprizeBoxedBoolean();
    if (1 == 2) {
    } else if (checkedBeforeUse8 != null) {
      if (checkedBeforeUse8) {} // Compliant because the variable is checked before use
    } else if (checkedBeforeUse9 == null) {
      if (checkedBeforeUse9) {} // Compliant FN because the value is checked but sill used
    } else if (checkedBeforeUse10 == alwaysTrue) {
      if (checkedBeforeUse10) {} // Noncompliant
    } else if (checkedBeforeUse11 == Boolean.FALSE) {
      if (checkedBeforeUse11) {} // Noncompliant
    } else if (alwaysTrue.equals(checkedBeforeUse12)) {
      if (checkedBeforeUse12) {} // Noncompliant
    } else if (Boolean.FALSE.equals(checkedBeforeUse13)) {
      if (checkedBeforeUse13) {} // Noncompliant
    }
  }

  void conditionalCheckedBeforeUse() {
    String ignored;
    final Boolean alwaysTrue = Boolean.TRUE;
    Boolean checkedBeforeUse = getSurprizeBoxedBoolean();
    if (checkedBeforeUse != null) {
      ignored = (checkedBeforeUse ? "a" : "b"); // Compliant because the variable is checked before use
    }
    Boolean checkedBeforeUse2 = getSurprizeBoxedBoolean();
    if (checkedBeforeUse2 == null) {
      return;
    }
    ignored = (checkedBeforeUse2 ? "a" : "b"); // Compliant because the variable is checked before use
    Boolean checkedBeforeUse3 = getSurprizeBoxedBoolean();
    if (checkedBeforeUse3 == null) {
      ignored = (checkedBeforeUse3 ? "a" : "b"); // Compliant FN because the value is checked but still used
    }
    Boolean checkedBeforeUse4 = getSurprizeBoxedBoolean();
    if (checkedBeforeUse4 == alwaysTrue) {
      ignored = (checkedBeforeUse4 ? "a" : "b"); // Noncompliant
    }
    Boolean checkedBeforeUse5 = getSurprizeBoxedBoolean();
    if (checkedBeforeUse5 == Boolean.TRUE) {
      ignored = (checkedBeforeUse5 ? "a" : "b"); // Noncompliant
    }
    Boolean checkedBeforeUse6 = getSurprizeBoxedBoolean();
    if (alwaysTrue.equals(checkedBeforeUse6)) {
      ignored = (checkedBeforeUse6 ? "a" : "b"); // Noncompliant
    }
    Boolean checkedBeforeUse7 = getSurprizeBoxedBoolean();
    if (Boolean.FALSE.equals(checkedBeforeUse7)) {
      ignored = (checkedBeforeUse7 ? "a" : "b"); // Noncompliant
    }

    Boolean checkedBeforeUse8 = getSurprizeBoxedBoolean();
    Boolean checkedBeforeUse9 = getSurprizeBoxedBoolean();
    Boolean checkedBeforeUse10 = getSurprizeBoxedBoolean();
    Boolean checkedBeforeUse11 = getSurprizeBoxedBoolean();
    Boolean checkedBeforeUse12 = getSurprizeBoxedBoolean();
    Boolean checkedBeforeUse13 = getSurprizeBoxedBoolean();
    if (1 == 2) {
    } else if (checkedBeforeUse8 != null) {
      ignored = (checkedBeforeUse8 ? "a" : "b"); // Compliant because the variable is checked before use
    } else if (checkedBeforeUse9 == null) {
      ignored = (checkedBeforeUse9 ? "a" : "b"); // Compliant FN because the value is checked but sill used
    } else if (checkedBeforeUse10 == alwaysTrue) {
      ignored = (checkedBeforeUse10 ? "a" : "b"); // Noncompliant
    } else if (checkedBeforeUse11 == Boolean.FALSE) {
      ignored = (checkedBeforeUse11 ? "a" : "b"); // Noncompliant
    } else if (alwaysTrue.equals(checkedBeforeUse12)) {
      ignored = (checkedBeforeUse12 ? "a" : "b"); // Noncompliant
    } else if (Boolean.FALSE.equals(checkedBeforeUse13)) {
      ignored = (checkedBeforeUse13 ? "a" : "b"); // Noncompliant
    }
  }

  void maskedInConditional() {
    Boolean effectivelyChecked = getSurprizeBoxedBoolean();
    String ignored = null;
    if (effectivelyChecked == null) {
      // ... Do something
    } else {
      ignored = (effectivelyChecked ? "a" : "b");
    }
    Boolean actuallyChecked = getSurprizeBoxedBoolean();
    if (false) {
      // ... Do something
    } else if (actuallyChecked == null) {
      ignored = (actuallyChecked ? "a" : "b");
    } else {
      ignored = (actuallyChecked ? "a" : "b");
    }

    Boolean irrelevant = getSurprizeBoxedBoolean();
    Boolean ifDoesNotCoverUse = getSurprizeBoxedBoolean();
    if (irrelevant == null) {
      if (ifDoesNotCoverUse == null) {
        // ... Do something
      }
    } else if (irrelevant != null) {
      // ... Do something
    } else {
      ignored = (ifDoesNotCoverUse ? "a" : "b"); // false-negative, first usage is a null check
    }

    Boolean conditionalDoesNotCoverUse = getSurprizeBoxedBoolean();
    if (irrelevant == null) {
      ignored = (conditionalDoesNotCoverUse == null) ? "null" : "not null";
    } else if (irrelevant != null) {
      // ... Do something
    } else {
      ignored = (conditionalDoesNotCoverUse ? "a" : "b"); // false-negative, first usage is a null check
    }

    Boolean whileDoesNotCoverUse = getSurprizeBoxedBoolean();
    if (irrelevant == null) {
      while (whileDoesNotCoverUse == null) {
        // ... Do something
      }
    } else if (irrelevant != null) {
      // ... Do something
    } else {
      ignored = (whileDoesNotCoverUse ? "a" : "b"); // // false-negative, first usage is a null check
    }

    Boolean forDoesNotCoverUse = getSurprizeBoxedBoolean();
    if (irrelevant == null) {
      for (; forDoesNotCoverUse == null; ) {
        // ... Do something
      }
    } else if (irrelevant != null) {
      // ... Do something
    } else {
      ignored = (forDoesNotCoverUse ? "a" : "b"); // false-negative, first usage is a null check
    }
    Boolean conditonalUsedBeforeCheck = getSurprizeBoxedBoolean();
    ignored = conditonalUsedBeforeCheck ? "a" : " b"; // Noncompliant
    if (conditonalUsedBeforeCheck == null) {
      // ... Do something
    }
  }

  void multipleUsageButNoCheck() {
    Boolean flag = getSurprizeBoxedBoolean();
    String ignored = null;
    if (flag) { // Noncompliant
      ignored = "a";
    } else if (flag) { // Noncompliant
      ignored = "b";
    }
  }

  void testBoxedValue(Boolean value) {
    if (value == null) {
      if (value) {
      }
    } else if (value) {
    }
  }

}
