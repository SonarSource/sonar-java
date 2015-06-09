interface notAnnotated { // Noncompliant {{Annotate the "notAnnotated" interface with the @FunctionInterface annotation}}
  public int transform(int a);
}
interface notAnnotatedWithTwoMethods {
  public int transform(int a);
  public int transformInto(int a);
}

interface notAnnotatedWithDefaultMethod {
  default public int transform(int a) {
    return a+1;
  }
}
interface notAnnotatedWithStatic {
  public static int transform(int a) {
    return a+1;
  }
}
@FunctionalInterface
interface Annotated {
  public int transform(int a);
}

interface MyFunc { // Noncompliant {{Annotate the "MyFunc" interface with the @FunctionInterface annotation}}
  int func(Object b);
  String toString();
}

interface extendsOther extends notAnnotated {
  public int transform2(int a);
}

interface extendsOther2 extends notAnnotated, Annotated { //False negative, this one should raise an issue.
}

interface NonFunc {
  boolean equals(Object obj);
}