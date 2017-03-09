class Parent {
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

class MoreChild extends Child {
}
