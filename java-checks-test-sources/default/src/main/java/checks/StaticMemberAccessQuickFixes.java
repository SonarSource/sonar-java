package checks;

class StaticMemberAccessQuickFixes {
  public static int counter;
  static void foo() { }

  static class InnerClass extends StaticMemberAccessQuickFixes {
    public static int otherCounter;

    void bar() {
      InnerClass.counter++; // Noncompliant {{Use static access with "checks.StaticMemberAccessQuickFixes" for "counter".}} [[quickfixes=qf1]]
//               ^^^^^^^
      // fix@qf1 {{Use "StaticMemberAccessQuickFixes" instead of "InnerClass"}}
      // edit@qf1 [[sc=7;ec=17]] {{StaticMemberAccessQuickFixes}}
    }
  }

  static class SecondInnerClass extends InnerClass {
    void qix() {
      SecondInnerClass.otherCounter++; // Noncompliant {{Use static access with "checks.StaticMemberAccessQuickFixes$InnerClass" for "otherCounter".}} [[quickfixes=qf2]]
//                     ^^^^^^^^^^^^
      // fix@qf2 {{Use "InnerClass" instead of "SecondInnerClass"}}
      // edit@qf2 [[sc=7;ec=23]] {{InnerClass}}
    }
  }
}

class StaticMemberAccessQuickFixesChild extends StaticMemberAccessQuickFixes {
  void bar() {
    StaticMemberAccessQuickFixesChild.counter++; // Noncompliant {{Use static access with "checks.StaticMemberAccessQuickFixes" for "counter".}} [[quickfixes=qf3]]
//                                    ^^^^^^^
    // fix@qf3 {{Use "StaticMemberAccessQuickFixes" instead of "StaticMemberAccessQuickFixesChild"}}
    // edit@qf3 [[sc=5;ec=38]] {{StaticMemberAccessQuickFixes}}

    StaticMemberAccessQuickFixes.counter++; // Compliant

    StaticMemberAccessQuickFixesChild.foo(); // Noncompliant {{Use static access with "checks.StaticMemberAccessQuickFixes" for "foo".}} [[quickfixes=qf4]]
//                                    ^^^
    // fix@qf4 {{Use "StaticMemberAccessQuickFixes" instead of "StaticMemberAccessQuickFixesChild"}}
    // edit@qf4 [[sc=5;ec=38]] {{StaticMemberAccessQuickFixes}}

    StaticMemberAccessQuickFixes.foo(); // Compliant
  }
}
