package checks;

public class CollapsibleIfCandidateCheck {
  void testMyFile(File file) {
    if (file != null) {
      if (file.isFile() || file.isDirectory()) { // Noncompliant [[sc=7;ec=9;quickfixes=qf1]]
        /* ... */
      }
      // fix@qf1 {{Merge this if statement with the (enclosing|nested) one}}
      // edit@qf1 [[sl=+2;el=+2;sc=7;ec=8]] {{}}
      // edit@qf1 [[sl=-1;el=+0;sc=21;ec=9]] {{ && }}
    }
  }

  void noBraceOnOuter(File file) {
    if (file != null)
      if (file.isFile() || file.isDirectory()) { // Noncompliant [[sc=7;ec=9;quickfixes=qf2]]
        /* ... */
      }
      // fix@qf2 {{Merge this if statement with the (enclosing|nested) one}}
      // edit@qf2 [[sl=-1;el=+0;sc=21;ec=9]] {{ && }}
  }

  void noBraceOnInner(File file) {
    if (file != null) {
      if (file.isFile() || file.isDirectory()) System.out.println("Good enough"); // Noncompliant [[sc=7;ec=9;quickfixes=qf3]]
      // fix@qf3 {{Merge this if statement with the (enclosing|nested) one}}
      // edit@qf3 [[sc=48;ec=48]] {{{}}
      // edit@qf3 [[sl=-1;el=+0;sc=21;ec=9]] {{ && }}
    }
  }
}
