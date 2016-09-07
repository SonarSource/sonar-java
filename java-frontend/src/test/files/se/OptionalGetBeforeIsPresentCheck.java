import java.util.Optional;

class A {
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
    getOptional().get(); // Noncompliant {{Call "isPresent()" before accessing the value.}}
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
}
