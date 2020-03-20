//faking the fact to be part of java.lang in order to redefine the Deprecated annotation.
//depending of the JDK used for run tests (java 8 vs 9+), the annotation in bytecode might not be present
package java.lang;

class CallToDeprecatedMethod {

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
    foo();
    bar(); // Noncompliant {{Remove this use of "bar"; it is deprecated.}}
    qix(); // Noncompliant {{Remove this use of "qix"; it is deprecated.}}

    new DeprecatedForRemoval();
    new DeprecatedDefault(); // Noncompliant {{Remove this use of "DeprecatedDefault", it has been marked for removal.}

    String s =
      a // Noncompliant {{Remove this use of a "a"; it is deprecated.}
      + b;
  }

  @Deprecated(forRemoval = true)
  static class DeprecatedForRemoval {}

  @Deprecated
  static class DeprecatedDefault {}

  static class Extending extends CallToDeprecatedMethod {
    @Override
    void foo() {}

    @Override
    void bar() {}  // Noncompliant {{Don't override a deprecated method or explicitly mark it as "@Deprecated".}}
  }
}

@interface Deprecated {
  boolean forRemoval() default false;
}
