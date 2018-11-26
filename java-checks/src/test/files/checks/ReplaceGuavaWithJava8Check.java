class A {
  A(com.google.common.base.Predicate p) {} // Noncompliant [[sc=5;ec=37]] {{Use "java.util.function.Predicate" instead.}}
  A(com.google.common.base.Function f) {} // Noncompliant {{Use "java.util.function.Function" instead.}}
  A(com.google.common.base.Supplier s) {} // Noncompliant {{Use "java.util.function.Supplier" instead.}}
  A(com.google.common.base.Optional o) {} // Noncompliant {{Use "java.util.Optional" instead.}}
  A(java.util.function.Predicate p) {}
  A(java.util.function.Function f) {}
  A(java.util.function.Supplier s) {}
  A(java.util.Optional o) {}
  void doX() {
    com.google.common.base.Predicate p; // Noncompliant {{Use "java.util.function.Predicate" instead.}}
    com.google.common.base.Function f; // Noncompliant {{Use "java.util.function.Function" instead.}}
    com.google.common.base.Supplier s; // Noncompliant {{Use "java.util.function.Supplier" instead.}}
  }
  void doY(com.google.common.base.Predicate p) {} // Noncompliant {{Use "java.util.function.Predicate" instead.}}
  void doY(com.google.common.base.Function f) {} // Noncompliant {{Use "java.util.function.Function" instead.}}
  void doY(com.google.common.base.Supplier s) {} // Noncompliant {{Use "java.util.function.Supplier" instead.}}

  void doZ() {
    com.google.common.io.BaseEncoding.base32();
    com.google.common.io.BaseEncoding.base64(); // Noncompliant {{Use "java.util.Base64" instead.}}
    com.google.common.io.BaseEncoding.base64Url(); // Noncompliant {{Use "java.util.Base64" instead.}}

    com.google.common.base.Optional.fromJavaUtil(java.util.Optional.empty());
    com.google.common.base.Optional.absent(); // Noncompliant {{Use "java.util.Optional.empty" instead.}}
    com.google.common.base.Optional.of(new Object()); // Noncompliant {{Use "java.util.Optional.of" instead.}}
    com.google.common.base.Optional.fromNullable(null); // Noncompliant {{Use "java.util.Optional.ofNullable" instead.}}

    com.google.common.base.Joiner.on(","); // Noncompliant {{Use "String.join" or "java.util.stream.Collectors.joining" instead.}}
    com.google.common.base.Joiner.on(','); // Noncompliant {{Use "String.join" or "java.util.stream.Collectors.joining" instead.}}
  }

  void doWithLambda(B<com.google.common.base.Optional<String>> b) {
    b.foo(o -> System.out.println(o)); // Noncompliant [[sc=11;ec=12]] {{Use "java.util.Optional" instead.}}
  }

  static class B<T> {
    void foo(java.util.function.Predicate<T> predicate) {}
  }
}
