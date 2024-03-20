package checks;

class ParentS2390 {
  static final Class<? extends ParentS2390> IMPL = ChildS2390.class; // Compliant
  static int childVersion1 = ChildS2390.version; // Noncompliant [[sc=30;ec=40]] {{Remove this reference to "ChildS2390".}}
  static int childVersion2 = ChildS2390.getVersion(); // Noncompliant
  static ChildS2390 value;
  static {
    value = ChildS2390 // Noncompliant {{Remove this reference to "ChildS2390".}}
      .singleton; // Noncompliant {{Remove this reference to "MoreChildS2390".}}
  }

  static ParentS2390 p = new ParentS2390(); // Compliant - only target subclasses
}

class ChildS2390 extends ParentS2390 {
   static int version = 6;
   static MoreChildS2390 singleton = new MoreChildS2390(); // Noncompliant {{Remove this reference to "MoreChildS2390".}}
   static ChildS2390 child = new ChildS2390() { // Compliant
     MoreChildS2390 foo() { // Compliant
       return null;
     }
   };

   static int getVersion() {
     return 0;
   }
}

class MoreChildS2390 extends ChildS2390 { }

class S2390A<T extends java.util.Date> {
  static final java.util.Comparator<S2390A<?>> COMPARATOR = (a1, a2) -> a1.value.compareTo(a2.value); // Compliant
  T value;
}

class S2390C {
  static final java.util.function.Function<S2390D, S2390D> FUNC = (S2390D d) -> d; // Compliant
}

class S2390D extends S2390C { }

class S2390V<T>  {
  private static class VSub extends S2390V {}
  static S2390V V1 = new VSub(); // Compliant
}

class ParentNested {

  private static class ChildNested extends ParentNested {
    static int version = 6;
    static int getVersion() {
      return 0;
    }
  }

  public static final ParentNested INSTANCE1 = new ChildNested(); // Compliant
  static int childVersion1 = ChildNested.version; // Compliant
  static int childVersion2 = ChildNested.getVersion(); // Compliant

  static int otherChildVersion1 = ChildS2390.version; // Compliant, not a child of ParentNested
  static int otherChildVersion2 = ChildS2390.getVersion(); // Compliant, not a child of ParentNested
}

class ParentNotNested {
  static int childVersion1 = UnrelatedNestingClass.ChildNested.version; // Noncompliant
}

class UnrelatedNestingClass {
  static class ChildNested extends ParentNotNested {
    static int version = 6;
  }
}
