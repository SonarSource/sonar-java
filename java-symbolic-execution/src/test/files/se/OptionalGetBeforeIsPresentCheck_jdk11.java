import java.util.Optional;

abstract class A {

  String isEmpty(Optional<String> s) {
    return s.isEmpty() ? s.get() : null; // Noncompliant
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

}
