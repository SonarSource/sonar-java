import javax.annotation.Nullable;

import java.util.Optional;
import java.util.stream.Stream;

abstract class A {
  Optional<String> getOptional() { return Optional.of(""); }
  Optional<String> optional;

  A() {
    this(Optional.empty());
  }

  A(Optional<String> s) {
    s.get();  // Noncompliant {{Call "s.isPresent()" before accessing the value.}}
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
      op.get(); // Noncompliant - original optional may or may not be present, cannot know here by only testing the filtered optional
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

  abstract boolean testSomething(String s);

  abstract boolean testSomethingElse(String s);

}

class Location {

  void test() {
    Stream.of(1,2,3).findFirst().get(); // Noncompliant [[sc=5;ec=33]]
  }

  void test2() {
    Optional<String> op = Optional.empty();
    op.get(); // Noncompliant [[sc=5;ec=7]]
  }

}

