import java.util.Optional;

class A {
  Optional<String> getOptional() { return Optional.of(""); }
  Optional<String> optional;

  A() {
    this(Optional.empty());
  }

  A(Optional<String> s) {
    s.get();  // Noncompliant [[flows=A]] {{Call "s.isPresent()" before accessing the value.}} flow@A {{Optional s is accessed}}
    if (s.isPresent()) {
      s.get(); // Compliant
    }
  }

  void foo() {
    getOptional().get(); // Noncompliant [[flows=foo]] {{Call "Optional#isPresent()" before accessing the value.}} flow@foo {{Optional is accessed}}
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
      s.get(); // Noncompliant [[flows=dul]] flow@dul
    }
    s.get(); // Compliant
  }

  void qix() {
    Optional<String> s = optional;
    if (s.isPresent()) {
      s.get(); // Compliant
    }
    s.get(); // Noncompliant [[flows=qix]] flow@qix
  }

  String mug(Optional<String> s) {
    return s.isPresent() ? null : s.get(); // Noncompliant [[flows=mug]] flow@mug
  }
}
