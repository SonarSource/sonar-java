package checks;

public class OverrideAnnotationCheck_QuickFixes extends OverrideAnnotationCheckSimple {
  class A {
    void foo(){}
  }

  interface I {
    void bar(); // Compliant
  }

  class B extends A implements I {
    void foo() {} // Noncompliant [[sc=10;ec=13;quickfixes=qf1]]
    // fix@qf1 {{Add "@Override" annotation}}
    // edit@qf1 [[sc=5;ec=5]] {{@Override\n    }}

    public void bar() {} // Compliant - from interface

    public void finalize() throws Throwable {
      super.finalize();
    }
  }

  abstract static class AbstractClass {
    abstract boolean foo();
    boolean bar() { return false; }

    static class ExtendsAbstractClass extends AbstractClass {
      boolean foo() { return false; } // Compliant - overridee is abstract

      @Deprecated
      public boolean bar() { return true; } // Noncompliant [[sc=22;ec=25;quickfixes=qf2]]
                                            // fix@qf2 {{Add "@Override" annotation}}
                                            // edit@qf2 [[sl=-1;sc=7;el=-1;ec=7]] {{@Override\n      }}
    }
  }

  abstract class ImplementsFromJDK8 implements java.lang.reflect.AnnotatedElement {

    public <A extends java.lang.annotation.Annotation> A[] getAnnotationsByType(Class<A> annotationClass) { return null; } // Noncompliant [[sc=60;ec=80;quickfixes=qf3]]
                                                                                                                           // fix@qf3 {{Add "@Override" annotation}}
                                                                                                                           // edit@qf3 [[sc=5;ec=5]] {{@Override\n    }}
  }

  @interface Annotation {}
  @interface OtherAnnotation {}

  @OtherAnnotation public @Annotation synchronized void get() { } // Noncompliant [[sc=57;ec=60;quickfixes=qf4]]
                                                                  // fix@qf4 {{Add "@Override" annotation}}
                                                                  // edit@qf4 [[sc=3;ec=3]] {{@Override\n  }}

  static class InnerClassSingleLine extends OverrideAnnotationCheckSimple { public void get() { } } // Noncompliant [[sc=89;ec=92;quickfixes=qf5]]
                                                                                                    // fix@qf5 {{Add "@Override" annotation}}
                                                                                                    // edit@qf5 [[sc=77;ec=77]] {{@Override }}
}

class OverrideAnnotationCheckSimple {
  void get() { }
}

class AbstractOverrideAnnotationSingleLine extends OverrideAnnotationCheckSimple { public void get() { } } // Noncompliant [[sc=96;ec=99;quickfixes=qf6]]
                                                                                                           // fix@qf6 {{Add "@Override" annotation}}
                                                                                                           // edit@qf6 [[sc=84;ec=84]] {{@Override }}
