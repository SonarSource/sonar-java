package symbolicexecution.checks;

import java.util.Optional;
import java.util.stream.Stream;
import javax.annotation.Nullable;

abstract class OptionalGetBeforeIsPresentCheckSample {
  Optional<String> getOptional() { return Optional.of(""); }
  Optional<String> optional;

  OptionalGetBeforeIsPresentCheckSample() {
    this(Optional.empty());
  }

  OptionalGetBeforeIsPresentCheckSample(Optional<String> s) {
    s.get(); // Noncompliant {{Call "s.isPresent()" before accessing the value.}}
    if (s.isPresent()) {
      s.get(); // Compliant
    }
  }

  void foo() {
    getOptional().get(); // Noncompliant {{Call "Optional#isPresent()" before accessing the value.}}
  }

  void bar() {
    Optional<String> s = getOptional();
    if (s.isPresent()) {
      s.get(); // Compliant
      if (!s.isPresent()) { // condition always false
        s.get(); // Compliant - dead code
      }
    }
    s.get(); // Noncompliant
  }

  void dul() {
    Optional<String> s = getOptional();
    if (!s.isPresent()) {
      if (s.isPresent()) { // condition always false
        s.get(); // Compliant - dead code
      }
      s.get(); // Noncompliant
    }
    s.get(); // Compliant
  }

  void qix() {
    Optional<String> s = optional;
    if (s.isPresent()) {
      s.get(); // Compliant
    }
    s.get(); // Noncompliant
  }

  String mug(Optional<String> s) {
    return s.isPresent() ? null : s.get(); // Noncompliant
  }

  private void usingEmpty() {
    Optional<String> op = Optional.empty();
    op.get(); // Noncompliant
  }

  private void usingOf() {
    String s = "helloWorld";
    Optional<String> op = Optional.of(s);
    op.get(); // Compliant - will always be present
  }

  private void usingOfNullable(@Nullable Object o1, Object o2) {
    Optional<Object> op1 = Optional.ofNullable(o1);
    op1.get(); // Noncompliant

    Optional<Object> op2 = Optional.ofNullable(o2);
    op2.get(); // Noncompliant
  }

  private void usingOfNullableWithTest(@Nullable Object o) {
    Optional<Object> op = Optional.ofNullable(o);
    if (o != null) {
      op.get(); // Compliant - if o is not null, then the optional is necessarily present
    }
  }

  private void usingFilter1(Optional<String> op) {
    if (op.filter(this::testSomething).isPresent()) {
      op.get(); // Compliant - filter return the same optional if 'isPresent()' is true on the filtered value
    }
    op.get(); // Noncompliant
  }

  private void usingFilter2(Optional<String> op) {
    if (op.filter(this::testSomething).filter(this::testSomethingElse).isPresent()) {
      op.get(); // Compliant
    }
    op.get(); // Noncompliant
  }

  private void usingFilter3(Optional<String> op) {
    if (!op.filter(this::testSomething).isPresent()) {
      op.get(); // Noncompliant
      return;
    }
    op.get(); // Compliant - if op is not present, then filtered value will always be non present ->  unreachable when op not present
  }

  private void usingFilter4() {
    Optional<String> op = Optional.empty();
    if (!op.filter(this::testSomething).isPresent()) {
      return;
    }
    op.get(); // Compliant - dead code
  }

  private void fromLocalField() {
    if (optional.isPresent() && optional.get() instanceof String) { // Compliant
      // do something
    }
  }

  private void fromField1() {
    OptionalField optionalField = new OptionalField();
    if (optionalField.op.isPresent() && optionalField.op.get() instanceof String) { // Compliant, from field
      // do something
    }
  }

  private void fromField2() {
    OptionalField optionalField = new OptionalField();
    if (optionalField.op.isPresent()) {
      testSomething(optionalField.op.get()); // Compliant, from field
    }
  }

  private void fromField3(OptionalField optionalField) {
    if (optionalField.op.isPresent()) {
      testSomething(optionalField.op.get()); // Compliant, from field
    }
  }

  private void fromField4() {
    Optional<String> op = new OptionalField().op;
    if (op.isPresent() && op.get() instanceof String) { // Compliant
      //do something
    }
  }

  private void fromField6() {
    OptionalField optionalField = new OptionalField();
    if (optionalField.op.get() instanceof String) { // Compliant, FN, we can't know if the field can be empty
      //do something
    }
  }

  private void fromField7() {
    Optional<String> op = new OptionalField().op;
    if (op.get() instanceof String) { // Noncompliant
      //do something
    }
  }

  abstract boolean testSomething(String s);

  abstract boolean testSomethingElse(String s);

}

class Location {

  void test() {
    Stream.of(1,2,3).findFirst().get(); // Noncompliant
//  ^^^^^^^^^^^^^^^^^^^^^^^^^^^^
  }

  void test2() {
    Optional<String> op = Optional.empty();
    op.get(); // Noncompliant
//  ^^
  }

}

class OptionalField {
  Optional<String> op = Optional.of("hello");
}

class OptionalField2 {
  // Optional as field is a commonly agreed bad pattern that should be avoided, it is therefore acceptable to not be super precise for fields,
  // and to focus mainly in avoiding FP, by reporting only if we are sure that the value is not present.
  private Optional<String> state = Optional.of("hello");

  public String useOptionalWithCallToMemberFunction() {
    if (state.isPresent()) {
      functionThatDoesNotChangeState();
      return state.get(); // Compliant
    }
    return "";
  }

  public String useOptionalWithCallToMemberFunction2() {
    if (state.isPresent()) {
      changeState();
      return state.get(); // FN
    }
    return "";
  }

  public String issueOnField() {
    if (!state.isPresent()) {
      return state.get(); // Noncompliant
    }
    return "";
  }

  public String issueOnField2() {
    return state.get(); // We can not know the exact state of the field, we don't report anything
  }

  public void functionThatDoesNotChangeState() {
    // DO NOTHING
  }

  public void changeState() {
    state = Optional.empty();
  }

}
