package checks;

class ModifiersOrderCheckWithQuickFixes {

  private final static int A = 0, B = 1; // Noncompliant [[sc=17;ec=23;quickfixes=qf1]]
                                         // fix@qf1 {{Reorder modifiers}}
                                         // edit@qf1 [[sc=3;ec=23]] {{}}
                                         // edit@qf1 [[sc=3;ec=3]] {{private static final}}

  static public void otherMain(String[] args) { } // Noncompliant [[sc=10;ec=16;quickfixes=qf2]]
                                                  // fix@qf2 {{Reorder modifiers}}
                                                  // edit@qf2 [[sc=3;ec=16]] {{}}
                                                  // edit@qf2 [[sc=3;ec=3]] {{public static}}

  // Noncompliant@+3 [[sc=3;ec=15;quickfixes=qf3]]
  @Annotation0(p1 = "foo", p2 = false)
  final
  @Annotation2
  static
  void foo() {}
  // fix@qf3 {{Reorder modifiers}}
  // edit@qf3 [[sl=17;sc=3;el=18;ec=3]] {{}} - "final\n  "
  // edit@qf3 [[sl=19;sc=3;el=20;ec=3]] {{}} - "static\n  "
  // edit@qf3 [[sl=20;sc=3;el=20;ec=3]] {{static final }}

  // Noncompliant@+1 [[sc=9;ec=15;quickfixes=qf4]]
  final public native @Annotation2 synchronized static void bar();
  // fix@qf4 {{Reorder modifiers}}
  // edit@qf4 [[sc=3;ec=9]]   {{}} - final
  // edit@qf4 [[sc=9;ec=16]]  {{}} - public
  // edit@qf4 [[sc=16;ec=23]] {{}} - native
  // edit@qf4 [[sc=36;ec=49]] {{}} - synchronized
  // edit@qf4 [[sc=49;ec=56]] {{}} - static
  // edit@qf4 [[sc=56;ec=56]] {{public static final synchronized native }}

  static @Annotation2 void qix() { } // Compliant - Annotation is not counted as part of the modifier, but attached to the type "void"
  public @Annotation2 ModifiersOrderCheckWithQuickFixes() { } // Compliant - Annotation is not counted as part of the modifier, even for constructors

  @interface Annotation0 { String p1(); boolean p2(); }
  @interface Annotation1 { String value(); }
  @interface Annotation2 {}
  @interface Annotation3 {}

  final static @Annotation2 class Inner {} // Noncompliant [[sc=9;ec=15;quickfixes=qf5]]
  // fix@qf5 {{Reorder modifiers}}
  // edit@qf5 [[sc=3;ec=9]]  {{}} - final
  // edit@qf5 [[sc=9;ec=16]] {{}} - static
  // edit@qf5 [[sc=29;ec=29]] {{static final }}

  @Annotation2
  final static class Inner2 {} // Noncompliant [[sc=9;ec=15;quickfixes=qf6]]
  // fix@qf6 {{Reorder modifiers}}
  // edit@qf6 [[sc=3;ec=9]]  {{}} - final
  // edit@qf6 [[sc=9;ec=16]] {{}} - static
  // edit@qf6 [[sc=16;ec=16]] {{static final }}
}
