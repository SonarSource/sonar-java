package symbolicexecution.checks;

import java.util.Optional;
import org.junit.Assert;
import org.junit.jupiter.api.Assertions;

abstract class OptionalGetBeforeIsPresentCheck_jdk11 {

  Optional<String> optional;

  String isEmpty(Optional<String> s) {
    return s.isEmpty() ? s.get() : null; // Noncompliant {{Call "s.isPresent()" or "!s.isEmpty()" before accessing the value.}}
  }

  private void usingIsEmpty() {
    Optional<Object> op = Optional.empty();
    if (!op.isEmpty()) {
      op.get(); // Compliant - dead code
    }
  }

  private void usingIsEmpty2() {
    Optional<Object> op = Optional.empty();
    if (op.isEmpty()) {
      // Noop
    } else {
      op.get(); // Compliant - dead code
    }
  }

  void usingIsEmpty3() {
    Optional<String> s = optional;
    if (!s.isEmpty()) {
      s.get(); // Compliant
    }
    s.get(); // Noncompliant
  }

  private void usingIsEmpty4() {
    Optional<Object> op = Optional.empty();
    if (op.isEmpty()) {
      return;
    }
    op.get();
  }

  private void usingIsEmpty5() {
    Optional<Object> op = Optional.empty();
    if (op.hashCode() == 0) {
      return;
    }
    op.get(); // Noncompliant {{Call "op.isPresent()" or "!op.isEmpty()" before accessing the value.}}
  }

  void test_junit5_1(Optional<String> op){
    Assertions.assertTrue(op.isPresent(), "Hello");
    op.get();  // Compliant
  }

  void test_junit5_2(Optional<String> op){
    Assertions.assertTrue(op.isEmpty(), "Hello");
    op.get();  // Noncompliant {{Call "op.isPresent()" or "!op.isEmpty()" before accessing the value.}}
  }

  void test_junit5_3(Optional<String> op){
    Assertions.assertFalse(op.isPresent());
    op.get();   // Noncompliant {{Call "op.isPresent()" or "!op.isEmpty()" before accessing the value.}}
  }

  void test_junit5_4(Optional<String> op){
    Assertions.assertFalse(op.isEmpty());
    op.get(); // Compliant
  }

  void test_junit5_5(Optional<String> op, int i){
    Assertions.assertFalse(i < 0);
    op.get();   // Noncompliant {{Call "op.isPresent()" or "!op.isEmpty()" before accessing the value.}}
  }

  void test_junit5_6(Optional<String> op){
    Assertions.assertTrue(op.isPresent(), () -> "Hello");
    op.get();  // Compliant
  }

  void test_junit5_7(Optional<String> op){
    Assertions.assertFalse(op.isEmpty(), () -> "Hello");
    op.get(); // Compliant
  }

  void test_junit5_8(Optional<String> op, int i){
    Assertions.assertFalse(i < 0, () -> "Hello");
    op.get();   // Noncompliant {{Call "op.isPresent()" or "!op.isEmpty()" before accessing the value.}}
  }

  void test_junit5_9(Optional<String> op){
    Assertions.fail();
    op.get(); // Compliant
  }

  void test_junit5_10(Optional<String> op){
    Assertions.fail("Hello");
    op.get(); // Compliant
  }

  void test_junit5_11(Optional<String> op){
    Assertions.fail("Hello", new Exception());
    op.get(); // Compliant
  }

  void test_junit5_12(Optional<String> op){
    Assertions.fail(new Exception());
    op.get(); // Compliant
  }

  void test_junit5_13(Optional<String> op){
    Assertions.fail(() -> "Hello");
    op.get(); // Compliant
  }

  void test_junit4_1(Optional<String> op){
    Assert.assertTrue("Hello", op.isPresent());
    op.get();  // Compliant
  }

  void test_junit4_2(Optional<String> op){
    Assert.assertTrue("Hello", op.isEmpty());
    op.get();  // Noncompliant {{Call "op.isPresent()" or "!op.isEmpty()" before accessing the value.}}
  }

  void test_junit4_3(Optional<String> op){
    Assert.assertFalse(op.isPresent());
    op.get();   // Noncompliant {{Call "op.isPresent()" or "!op.isEmpty()" before accessing the value.}}
  }

  void test_junit4_4(Optional<String> op){
    Assert.assertFalse(op.isEmpty());
    op.get(); // Compliant
  }

  void test_junit4_5(Optional<String> op, int i){
    Assert.assertFalse(i < 0);
    op.get();   // Noncompliant {{Call "op.isPresent()" or "!op.isEmpty()" before accessing the value.}}
  }

  void test_junit4_6(Optional<String> op){
    Assert.fail();
    op.get(); // Compliant
  }

  void test_junit4_7(Optional<String> op){
    Assert.fail("Hello");
    op.get(); // Compliant
  }

}
