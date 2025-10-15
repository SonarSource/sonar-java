package checks;

class CallToDeprecatedCodeFlaggedForRemovalCheckSample {

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

    new DeprecatedForRemoval(); // Noncompliant
    new DeprecatedDefault();

    String s =  a
      + b; // Noncompliant
  }

  @Deprecated(forRemoval = true)
  static class DeprecatedForRemoval { }

  @Deprecated
  static class DeprecatedDefault { }

  static class Extending extends CallToDeprecatedCodeFlaggedForRemovalCheckSample {
    @Override
    void foo() {} // Noncompliant {{Don't override this deprecated method, it has been marked for removal.}}

    @Override
    void bar() {}
  }

  interface WithDeprecatedMethod {
    @Deprecated(forRemoval = true)
    void interfaceMethod();

    @Deprecated(forRemoval = true)
    default void interfaceDefault() {}
  }

  static abstract class WithAbstractMethod {
    @Deprecated(forRemoval = true)
    public abstract void forOverriding();

    @Deprecated(forRemoval = true)
    public void bad() {}

    public void good() {}
  }

  class Implementing extends WithAbstractMethod implements WithDeprecatedMethod {
    // Abstract, so overriding is unavoidable.
    @Override
    public void interfaceMethod() {} // Compliant

    @Override
    public void interfaceDefault() {} // Noncompliant

    // Abstract, so overriding is unavoidable.
    @Override
    public void forOverriding() {} // Compliant

    @Override
    public void bad() {} // Noncompliant

    @Override
    public void good() {}
  }
}
