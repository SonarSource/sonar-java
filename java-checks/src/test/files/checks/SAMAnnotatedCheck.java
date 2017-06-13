interface notAnnotated { // Noncompliant [[sc=11;ec=23]] {{Annotate the "notAnnotated" interface with the @FunctionalInterface annotation}}
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

interface MyFunc { // Noncompliant {{Annotate the "MyFunc" interface with the @FunctionalInterface annotation}}
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

interface Level0 {
  void m1();
  void m2();
}

interface Level1 extends Level0 {}

interface Level2 extends Level1 { // Compliant
  void plop1(int yolo);
}

interface InterfaceWithField { // Compliant
  public static final int MY_CONST = 0;
}

interface InterfaceWithoutField extends InterfaceWithField { // Noncompliant
  void m1();
}

interface InterfaceWithUnknownParent extends UnknownInterface { // Compliant
  void m1();
}

interface InterfaceWithPrivate {
  private int transform(int a) {};
}
