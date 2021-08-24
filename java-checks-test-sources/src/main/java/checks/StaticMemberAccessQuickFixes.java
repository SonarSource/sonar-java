package checks;

class StaticMemberAccessQuickFixes {
  public static int counter;
  static void foo() { }

  static class InnerClass extends StaticMemberAccessQuickFixes {
    public static int otherCounter;

    void bar() {
      InnerClass.counter++; // Noncompliant [[sc=18;ec=25;quickfixes=qf1]] {{Use static access with "checks.StaticMemberAccessQuickFixes" for "counter".}}
      // fix@qf1 {{Use "StaticMemberAccessQuickFixes" instead of "InnerClass"}}
      // edit@qf1 [[sc=7;ec=17]] {{StaticMemberAccessQuickFixes}}
    }
  }

  static class SecondInnerClass extends InnerClass {
    void qix() {
      SecondInnerClass.otherCounter++;  // Noncompliant [[sc=24;ec=36;quickfixes=qf2]] {{Use static access with "checks.StaticMemberAccessQuickFixes$InnerClass" for "otherCounter".}}
      // fix@qf2 {{Use "InnerClass" instead of "SecondInnerClass"}}
      // edit@qf2 [[sc=7;ec=23]] {{InnerClass}}
    }
  }
}

class StaticMemberAccessQuickFixesChild extends StaticMemberAccessQuickFixes {
  void bar() {
    StaticMemberAccessQuickFixesChild.counter++;  // Noncompliant [[sc=39;ec=46;quickfixes=qf3]] {{Use static access with "checks.StaticMemberAccessQuickFixes" for "counter".}}
    // fix@qf3 {{Use "StaticMemberAccessQuickFixes" instead of "StaticMemberAccessQuickFixesChild"}}
    // edit@qf3 [[sc=5;ec=38]] {{StaticMemberAccessQuickFixes}}

    StaticMemberAccessQuickFixes.counter++; // Compliant

    StaticMemberAccessQuickFixesChild.foo(); // Noncompliant [[sc=39;ec=42;quickfixes=qf4]] {{Use static access with "checks.StaticMemberAccessQuickFixes" for "foo".}}
    // fix@qf4 {{Use "StaticMemberAccessQuickFixes" instead of "StaticMemberAccessQuickFixesChild"}}
    // edit@qf4 [[sc=5;ec=38]] {{StaticMemberAccessQuickFixes}}

    StaticMemberAccessQuickFixes.foo(); // Compliant
  }
}
