package checks;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import com.google.common.io.Files;
import java.io.IOException;

class ReplaceGuavaWithJavaCheckSample {
  ReplaceGuavaWithJavaCheckSample(com.google.common.base.Predicate p) {} // Noncompliant [[sc=35;ec=67]] {{Use "java.util.function.Predicate" instead.}}
  ReplaceGuavaWithJavaCheckSample(com.google.common.base.Function f) {} // Noncompliant {{Use "java.util.function.Function" instead.}}
  ReplaceGuavaWithJavaCheckSample(com.google.common.base.Supplier s) {} // Noncompliant {{Use "java.util.function.Supplier" instead.}}
  ReplaceGuavaWithJavaCheckSample(com.google.common.base.Optional o) {} // Noncompliant {{Use "java.util.Optional" instead.}}
  ReplaceGuavaWithJavaCheckSample(java.util.function.Predicate p) {}
  ReplaceGuavaWithJavaCheckSample(java.util.function.Function f) {}
  ReplaceGuavaWithJavaCheckSample(java.util.function.Supplier s) {}
  ReplaceGuavaWithJavaCheckSample(java.util.Optional o) {}
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
  
  void tempDir() throws IOException {
    com.google.common.io.Files.createTempDir(); // Noncompliant [[sc=5;ec=47]] {{Use "java.nio.file.Files.createTempDirectory" instead.}}
    Files.createTempDir(); // Noncompliant [[sc=5;ec=26]] {{Use "java.nio.file.Files.createTempDirectory" instead.}}

    java.nio.file.Files.createTempDirectory(""); // Compliant
  }

  void immutableCollections() {
    ImmutableSet.of("A", "B", "C"); // Compliant, it's Java 8 check
    ImmutableList.of("A", "B", "C"); // Compliant, it's Java 8 check
    ImmutableMap.of("A", "B", "C", "D"); // Compliant, it's Java 8 check
  }
}
