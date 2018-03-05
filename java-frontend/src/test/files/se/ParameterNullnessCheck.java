import javax.annotation.Nullable;
import javax.annotation.ParametersAreNonnullByDefault;

@ParametersAreNonnullByDefault
abstract class A {

  A(String s) { }
  A(Object o1, Object o2) { }

  Object field;

  void foo(Object o) {
    foo(null); // Noncompliant [[sc=9;ec=13]] {{Annotate the parameter with @javax.annotation.Nullable in method declaration, or make sure that null can not be passed as argument.}}
    bar(o, null);
    bar(null, o); // Noncompliant [[sc=9;ec=13]]

    equals(null);
    unknownMethod(null);
    B.foo(null);
    if (o != null) {
      foo(o);
    }
    foo(field);
    qix();

    gul(null, o, null, o); // Compliant - ignore variadic argument
    gul2(null, o, null, o); // Noncompliant [[sc=10;ec=14]] - first parameter is not variadic

    A a1 = new A(null); // Noncompliant
    A a2 = new A(o, null); // Noncompliant [[sc=21;ec=25]]
    B b = new B();
  }

  @Override
  public boolean equals(Object obj) {
    return super.equals(obj);
  }

  abstract void bar(Object o1, @Nullable Object o2);
  abstract void qix();
  abstract void gul(Object ... objects);
  abstract void gul2(String s, Object ... objects);
}

class B {
  static void foo(Object o) { }
}
