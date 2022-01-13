package checks;

class CallToDeprecatedMethod {
  @Deprecated(forRemoval = false)
  CallToDeprecatedMethod(int a) {}
  CallToDeprecatedMethod(long a) {}

  @Deprecated(forRemoval = false)
  void foo(Object obj) {}
  void foo(long value) {}

  @Deprecated(forRemoval = false)
  void bar(String... labels) {}
  void bar(String label) {}

  void test() {
    new CallToDeprecatedMethod(42); // Noncompliant
    new CallToDeprecatedMethod(42L); // Compliant
    new CallToDeprecatedMethod(unknownNumber()); // Compliant

    foo(new Object()); // Noncompliant
    foo(unknown()); // Compliant because we cannot determine the type of unknown
    foo(42L);

    bar("Hello", " World!"); // Noncompliant
    bar("Hello, World!"); // Compliant
    bar(unknwonCall()); // Compliant because we cannot determine the type of unknownCall
  }
}
