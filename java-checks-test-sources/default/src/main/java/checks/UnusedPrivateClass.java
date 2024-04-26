package checks;

public class UnusedPrivateClass {

  private class Unused {} // Noncompliant {{Remove this unused private "Unused" class.}}
//              ^^^^^^
  private class Used {} // compliant
  private class Used2 {} // compliant

  void fun() {
    Used used = new Used() {};
    new Used2().toString();
    IUsed a = new IUsed() {};

    @AnnotationUsed Object o = MyUsedEnum.QIX;
  }

  private interface IUnused {} // Noncompliant {{Remove this unused private "IUnused" class.}}
//                  ^^^^^^^
  private interface IUsed {}

  private enum MyUnusedEnum { // Noncompliant {{Remove this unused private "MyUnusedEnum" class.}}
//             ^^^^^^^^^^^^
    FOO, BAR;}
  private enum MyUsedEnum {QIX, PLOP;}

  private @interface AnnotationUnused {} // Noncompliant {{Remove this unused private "AnnotationUnused" class.}}
//                   ^^^^^^^^^^^^^^^^
  private @interface AnnotationUsed {}

}
