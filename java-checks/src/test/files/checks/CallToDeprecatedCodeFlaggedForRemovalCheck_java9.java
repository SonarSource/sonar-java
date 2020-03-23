class CallToDeprecatedCodeFlaggedForRemovalCheck {

  @Deprecated
  String a;

  @Deprecated(forRemoval = true)
  String b;

  @Deprecated(forRemoval = true)
  void foo() {}

  @Deprecated(forRemoval = false)
  void bar() {}

  @Deprecated
  void qix() {}

  void test() {
    foo(); // Noncompliant {{Remove this call to a deprecated method, it has been marked for removal.}}
    bar();
    qix();

    new DeprecatedForRemoval(); // Noncompliant {{Remove this use of "DeprecatedForRemoval", it has been marked for removal.}
    new DeprecatedDefault();

    String s =  a
      + b; // Noncompliant {{Remove this use of a deprecated field, it has been marked for removal.}
  }

  @Deprecated(forRemoval = true)
  static class DeprecatedForRemoval { }

  @Deprecated
  static class DeprecatedDefault { }

  static class Extending extends CallToDeprecatedCodeFlaggedForRemovalCheck {
    @Override
    void foo() {} // Noncompliant {{Don't override this deprecated method, it has been marked for removal.}}

    @Override
    void bar() {}
  }
}
