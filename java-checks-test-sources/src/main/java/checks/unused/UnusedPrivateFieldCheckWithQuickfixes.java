package checks.unused;

public class UnusedPrivateFieldCheckWithQuickfixes {
  private class QuickFixAndRemoveAssignment {
    // fix@qf1 {{Remove this unused private field}}
    // edit@qf1 [[sl=+3;el=+3;sc=7;ec=16]] {{var valueFormerlyAssignedToX1 = }}
    // edit@qf1 [[sc=5;ec=19]] {{}}
    private int x; // Noncompliant [[sc=17;ec=18;quickfixes=qf1]]

    QuickFixAndRemoveAssignment(int x, int y) {
      this.x = x + y;
    }
  }

  private class QuickfixAndRemoveNestedAssignment {
    // fix@qf2 {{Remove this unused private field}}
    // edit@qf2 [[sl=+6;el=+6;sc=9;ec=18]] {{var valueFormerlyAssignedToX2 = }}
    // edit@qf2 [[sl=+4;el=+4;sc=9;ec=18]] {{var valueFormerlyAssignedToX1 = }}
    // edit@qf2 [[sc=5;ec=19]] {{}}
    private int x; // Noncompliant [[sc=17;ec=18;quickfixes=qf2]]

    QuickfixAndRemoveNestedAssignment(int x, int y) {
      if (x > y) {
        this.x = x + y;
      } else {
        this.x = x;
      }
    }
  }

  private class QuickFixesRemovesAsssignmentFromLoopInitializer {
    // fix@qf3 {{Remove this unused private field}}
    // edit@qf3 [[sl=+3;el=+3;sc=12;ec=21]] {{var valueFormerlyAssignedToX1 = }}
    // edit@qf3 [[sc=5;ec=19]] {{}}
    private int x; // Noncompliant [[sc=17;ec=18;quickfixes=qf3]]

    QuickFixesRemovesAsssignmentFromLoopInitializer(int x, int y) {
      for (this.x = x + y; y < 0; y++) {
        System.out.println("Hello y = " + y);
      }
    }
  }
}
