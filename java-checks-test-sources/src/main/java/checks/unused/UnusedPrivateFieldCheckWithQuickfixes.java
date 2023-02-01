package checks.unused;

public class UnusedPrivateFieldCheckWithQuickfixes {
  private class IssueWithoutQuickFix {
    // fix@qf1 {{Remove this unused private field}}
    // edit@qf1 [[sl=+3;el=+3;sc=7;ec=21]] {{}}
    // edit@qf1 [[sc=5;ec=19]] {{}}
    private int x; // Noncompliant [[sc=17;ec=18;quickfixes=qf1]]

    IssueWithoutQuickFix(int x, int y) {
      this.x = x + y;
    }
  }
}
