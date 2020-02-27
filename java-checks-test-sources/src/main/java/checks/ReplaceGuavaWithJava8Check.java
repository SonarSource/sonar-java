package checks;

class ReplaceGuavaWithJava8Check {
  ReplaceGuavaWithJava8Check(com.google.common.base.Predicate p) {} // Noncompliant [[sc=30;ec=62]] {{Use "java.util.function.Predicate" instead.}}
  ReplaceGuavaWithJava8Check(com.google.common.base.Function f) {} // Noncompliant {{Use "java.util.function.Function" instead.}}
  ReplaceGuavaWithJava8Check(com.google.common.base.Supplier s) {} // Noncompliant {{Use "java.util.function.Supplier" instead.}}
  ReplaceGuavaWithJava8Check(com.google.common.base.Optional o) {} // Noncompliant {{Use "java.util.Optional" instead.}}
  ReplaceGuavaWithJava8Check(java.util.function.Predicate p) {}
  ReplaceGuavaWithJava8Check(java.util.function.Function f) {}
  ReplaceGuavaWithJava8Check(java.util.function.Supplier s) {}
  ReplaceGuavaWithJava8Check(java.util.Optional o) {}
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

    // Joiner can not always be replaced by Java 8 features, see SONARJAVA-3301
    com.google.common.base.Joiner.on(","); //Compliant
    com.google.common.base.Joiner.on(','); // Compliant
  }

  void doWithLambda(B<com.google.common.base.Optional<String>> b) {
    b.foo(o -> o.isPresent()); // Noncompliant [[sc=11;ec=12]] {{Use "java.util.Optional" instead.}}
  }

  static class B<T> {
    void foo(java.util.function.Predicate<T> predicate) {}
  }
}
