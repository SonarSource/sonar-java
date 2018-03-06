import com.google.common.base.Preconditions;
import javax.annotation.Nullable;
import javax.annotation.ParametersAreNonnullByDefault;

@ParametersAreNonnullByDefault
abstract class A {

  A(String s) { }
  A(Object o1, Object o2) { } // flow@A [[order=2]] {{Constructor declaration.}}

  Object field;

  void foo(Object o) { // flow@foo [[order=2]] {{Method 'foo' declaration.}}
    foo( // Noncompliant [[sc=5;ec=8;flows=foo]] {{Annotate the parameter with @javax.annotation.Nullable in method 'foo' declaration, or make sure that null can not be passed as argument.}}
      null); // flow@foo [[order=1]] {{Argument can be null.}}
    bar(o, null);
    bar(null, o); // Noncompliant [[sc=5;ec=8]]

    equals(null);
    unknownMethod(null);
    B.foo(null);
    if (o != null) {
      foo(o);
    }
    foo(field);
    qix();

    gul(null, o, null, o); // Compliant - ignore variadic argument
    gul2(null, o, null, o); // Noncompliant [[sc=5;ec=9]] - first parameter is not variadic

    A a1 = new A(null); // Noncompliant
    A a2 = new A(o, // Noncompliant [[sc=16;ec=17;flows=A]] {{Annotate the parameter with @javax.annotation.Nullable in constructor declaration, or make sure that null can not be passed as argument.}}
      null); // flow@A [[order=1]] {{Argument can be null.}}
    B b = new B();

    Preconditions.checkNotNull( // Noncompliant [[sc=19;ec=31;flows=checkNotNull]] 
      null); // flow@checkNotNull [[order=1]] {{Argument can be null.}}
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
