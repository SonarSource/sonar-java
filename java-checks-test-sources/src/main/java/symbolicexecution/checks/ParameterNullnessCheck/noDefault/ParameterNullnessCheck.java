package symbolicexecution.checks.ParameterNullnessCheck.noDefault;

import com.google.common.base.Preconditions;
import javax.annotation.Nullable;
import javax.annotation.ParametersAreNonnullByDefault;
import javax.annotation.ParametersAreNullableByDefault;

@ParametersAreNonnullByDefault
abstract class ParameterNullnessCheck {

  Object field;

  void foo(Object o) { // flow@foo [[order=2]] {{Method 'foo' declaration.}}
    foo( // Noncompliant [[sc=5;ec=8;flows=foo]] {{Annotate the parameter with @javax.annotation.Nullable in method 'foo' declaration, or make sure that null can not be passed as argument.}}
      null); // flow@foo [[order=1]] {{Argument can be null.}}
    bar(o, null); // Compliant, annotated Nullable
    bar(null, o); // Noncompliant [[sc=5;ec=8]]

    equals(null);
    B.foo(null); // Noncompliant
    if (o != null) {
      foo(o);
    }
    foo(field);
    qix();

    checkerFrameworkNullableAnnotations(null, null, null, null, null); // Compliant - reported by S2637
  }

  void testVarargs(Object o) {
    gul(null, o, null, o); // Compliant - ignore variadic argument
    gul2(null, o, null, o); // Noncompliant [[sc=5;ec=9]] - first parameter is not variadic
  }

  void testConstructors(Object o) {
    C c1 = new C(null); // Noncompliant
    C c2 = new C(o, // Noncompliant [[sc=16;ec=17;flows=A]] {{Annotate the parameter with @javax.annotation.Nullable in constructor declaration, or make sure that null can not be passed as argument.}}
      null); // flow@A [[order=1]] {{Argument can be null.}}
    B b = new B();

    checkerFrameworkNullableAnnotations(null, null, null, null, null); // Compliant - reported by S2637
  }

  void qix(@Nullable Object o) {
    Preconditions.checkNotNull(o); // Compliant - a way to be sure it will be not null
  }

  boolean checkerFrameworkNullableAnnotations(
    @Nullable String javaxNullable,
    @org.checkerframework.checker.nullness.qual.NonNull Object qualNonNull,
    @org.checkerframework.checker.nullness.qual.Nullable Object qualNullable,
    @org.checkerframework.checker.nullness.compatqual.NonNullDecl Object compatQualNonNull,
    @org.checkerframework.checker.nullness.compatqual.NullableDecl Object compatQualNullable
  ) {
    checkerFrameworkQualNonNull(javaxNullable); // Compliant - reported by S2637
    checkerFrameworkQualNullable(javaxNullable);
    checkerFrameworkCompatQualNonNull(javaxNullable); // Compliant - reported by S2637
    checkerFrameworkCompatQualNullable(javaxNullable);

    foo(qualNonNull);
    foo(qualNullable); // Noncompliant
    foo(compatQualNonNull);
    foo(compatQualNullable); // Noncompliant

    // "Strings.isNullOrEmpty" parameter is annotated by @org.checkerframework.checker.nullness.qual.Nullable
    return com.google.common.base.Strings.isNullOrEmpty(javaxNullable);
  }

  abstract void checkerFrameworkQualNonNull(@org.checkerframework.checker.nullness.qual.NonNull Object param);
  abstract void checkerFrameworkQualNullable(@org.checkerframework.checker.nullness.qual.Nullable Object param);
  abstract void checkerFrameworkCompatQualNonNull(@org.checkerframework.checker.nullness.compatqual.NonNullDecl Object param);
  abstract void checkerFrameworkCompatQualNullable(@org.checkerframework.checker.nullness.compatqual.NullableDecl Object param);

  @Override
  public boolean equals(Object obj) {
    return super.equals(obj);
  }

  abstract void bar(Object o1, @Nullable Object o2);
  abstract void qix();
  abstract void gul(Object ... objects);
  abstract void gul2(String s, Object ... objects);

  static class B {
    // Nested class of class annotated are also impacted.
    static void foo(Object o) { }
  }

  @ParametersAreNonnullByDefault
  static class C {
    C(String s) { }
    C(Object o1, Object o2) { } // flow@A [[order=2]] {{Constructor declaration.}}
  }

  @ParametersAreNullableByDefault
  class ParametersAreNullByDefault {
    void callNonNullWithNullable(@Nullable Object o) {
      ParametersAreNonNullByDefault.nonNullArg(o); // Noncompliant
    }

    void callNonNullWithNullable2(Object o) {
      ParametersAreNonNullByDefault.nonNullArg(o); // Noncompliant
    }

  }

  @ParametersAreNonnullByDefault
  static class ParametersAreNonNullByDefault {
    static void nonNullArg(Object o){
    }
  }
}
