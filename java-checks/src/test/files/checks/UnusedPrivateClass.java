package org.sonar.java.checks.targets;

public class UnusedPrivateClass {

  private class Unused {} // Noncompliant {{Remove this unused private "Unused" class.}} [[sc=17;ec=23]]
  private class Used {} // compliant
  private class Used2 {} // compliant

  void fun() {
    Used used = new Used() {};
    new Used2().toString();
    IUsed a = new IUsed() {};

    @AnnotationUsed Object o = MyUsedEnum.QIX;
  }

  private interface IUnused {} // Noncompliant {{Remove this unused private "IUnused" class.}} [[sc=21;ec=28]]
  private interface IUsed {}

  private enum MyUnusedEnum { // Noncompliant {{Remove this unused private "MyUnusedEnum" class.}} [[sc=16;ec=28]]
    FOO, BAR;}
  private enum MyUsedEnum {QIX, PLOP;}

  private @interface AnnotationUnused {} // Noncompliant {{Remove this unused private "AnnotationUnused" class.}} [[sc=22;ec=38]]
  private @interface AnnotationUsed {}

}
