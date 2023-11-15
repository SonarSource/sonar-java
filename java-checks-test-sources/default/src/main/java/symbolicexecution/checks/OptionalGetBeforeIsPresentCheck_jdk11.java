package symbolicexecution.checks;

import java.util.Optional;

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
}
