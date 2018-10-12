class Parent {
  static final Class<? extends Parent> IMPL = Child.class; // Compliant
  static int childVersion1 = Child.version; // Noncompliant [[sc=30;ec=35]] {{Remove this reference to "Child".}}
  static int childVersion2 = Child.getVersion(); // Noncompliant
  static Child value;
  static {
    value = Child // Noncompliant {{Remove this reference to "Child".}}
      .singleton; // Noncompliant {{Remove this reference to "MoreChild".}}
  }

  static Parent p = new Parent(); // Compliant - only target subclasses
}

class Child extends Parent {
   static int version = 6;
   static MoreChild singleton = new MoreChild(); // Noncompliant {{Remove this reference to "MoreChild".}}
   static Child child = new Child() { // Compliant
     MoreChild foo() { // Compliant
       return null;
     }
   };

   static int getVersion() {
     return 0;
   }
}

class MoreChild extends Child { }

class A<T extends java.util.Date> {
  static final java.util.Comparator<A<?>> COMPARATOR = (a1, a2) -> a1.value.compareTo(a2.value); // Compliant
  T value;
}

class C {
  static final java.util.function.Function<D, D> FUNC = (D d) -> d; // Compliant
}

class D extends C { }

public class V<T>  {
  private static class VSub extends V {}
  static V V1 = new VSub(); // Noncompliant
}
