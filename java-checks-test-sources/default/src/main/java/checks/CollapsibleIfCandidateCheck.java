package checks;

import java.util.logging.Level;
import java.util.logging.Logger;

public class CollapsibleIfCandidateCheck {
  private static final Logger LOGGER = Logger.getLogger(CollapsibleIfCandidateCheck.class.getCanonicalName());

  // fix@qf1 {{Merge this if statement with the enclosing one}}
  // edit@qf1 [[sl=-1;sc=5;el=+3;ec=6]] {{if (file != null && (file.isFile() || file.isDirectory())) {\n      LOGGER.log(Level.INFO, file.getAbsolutePath());\n    }}}
  void testMyFile(File file) {
    // Noncompliant@+2 [[sc=7;ec=9;quickfixes=qf1]]
    if (file != null) {
      if (file.isFile() || file.isDirectory()) {
        LOGGER.log(Level.INFO, file.getAbsolutePath());
      }
    }
  }

  // fix@qf2 {{Merge this if statement with the enclosing one}}
  // edit@qf2 [[sl=-1;sc=5;el=+2;ec=8]] {{if (file != null && (file.isFile() || file.isDirectory())) {\n      LOGGER.log(Level.INFO, file.getAbsolutePath());\n    }}}
  void noBraceOnOuter(File file) {
    // Noncompliant@+2 [[sc=7;ec=9;quickfixes=qf2]]
    if (file != null)
      if (file.isFile() || file.isDirectory()) {
        LOGGER.log(Level.INFO, file.getAbsolutePath());
      }
  }

  // TODO update other quickfixes

  void noBraceOnInner(File file) {
    if (file != null) {
      if (file.isFile() || file.isDirectory()) LOGGER.log(Level.INFO, file.getAbsolutePath()); // Noncompliant [[sc=7;ec=9;quickfixes=qf3]]
      // fix@qf3 {{Merge this if statement with the enclosing one}}
      // edit@qf3 [[sl=-1;el=+0;sc=21;ec=11]] {{ && }}
      // edit@qf3 [[sc=11;ec=11]] {{(}}
      // edit@qf3 [[sc=46;ec=46]] {{)}}
      // edit@qf3 [[sl=+6;el=+6;sc=5;ec=6]] {{}}
    }
  }

  void leftConditionNeedsParenthesis(boolean a, boolean b, boolean c) {
    if (a || b) {
      if (c) { // Noncompliant [[sc=7;ec=9;quickfixes=qf4]]
        // fix@qf4 {{Merge this if statement with the enclosing one}}
        // edit@qf4 [[sl=-1;el=+0;sc=15;ec=11]] {{ && }}
        // edit@qf4 [[sl=-1;el=-1;sc=9;ec=9]] {{(}}
        // edit@qf4 [[sl=-1;el=-1;sc=15;ec=15]] {{)}}
        // edit@qf4 [[sl=+7;el=+7;sc=5;ec=6]] {{}}
      }
    }
  }

  void rightConditionNeedsParenthesis(boolean a, boolean c, boolean d) {
    if (a) {
      if (c || d) { // Noncompliant [[sc=7;ec=9;quickfixes=qf5]]
        // fix@qf5 {{Merge this if statement with the enclosing one}}
        // edit@qf5 [[sl=-1;el=+0;sc=10;ec=11]] {{ && }}
        // edit@qf5 [[sc=11;ec=11]] {{(}}
        // edit@qf5 [[sc=17;ec=17]] {{)}}
        // edit@qf5 [[sl=+7;el=+7;sc=5;ec=6]] {{}}
      }
    }
  }

  void bothConditionsNeedParenthesis(boolean a, boolean b, boolean c, boolean d) {
    if (a || b) {
      if (c || d) { // Noncompliant [[sc=7;ec=9;quickfixes=qf6]]
        // fix@qf6 {{Merge this if statement with the enclosing one}}
        // edit@qf6 [[sl=-1;el=+0;sc=15;ec=11]] {{ && }}
        // edit@qf6 [[sl=-1;el=-1;sc=9;ec=9]] {{(}}
        // edit@qf6 [[sl=-1;el=-1;sc=15;ec=15]] {{)}}
        // edit@qf6 [[sc=11;ec=11]] {{(}}
        // edit@qf6 [[sc=17;ec=17]] {{)}}
        // edit@qf6 [[sl=+9;el=+9;sc=5;ec=6]] {{}}
      }
    }
  }

  void noConditionNeedsParenthesis(boolean a, boolean c) {
    if (a) {
      if (c) { // Noncompliant [[sc=7;ec=9;quickfixes=qf7]]
        // fix@qf7 {{Merge this if statement with the enclosing one}}
        // edit@qf7 [[sl=-1;el=+0;sc=10;ec=11]] {{ && }}
        // edit@qf7 [[sl=+5;el=+5;sc=5;ec=6]] {{}}
      }
    }
  }

  void noInnerBlockImpliesSingleStatement(boolean a, boolean c) {
    if (a) {
      if (c); // Compliant
      System.out.println();
    }
  }

  void noBraceOnAny(boolean a, boolean c) {
    if (a) if (c); // Noncompliant [[sc=12;ec=14;quickfixes=qf10]]
    // fix@qf10 {{Merge this if statement with the enclosing one}}
    // edit@qf10 [[sc=10;ec=16]] {{ && }}
  }

  void operatorsWithLowerPrecedenceCoverage(boolean b, boolean c) {
    boolean a;
    if (a = b) {
      if (c) { // Noncompliant [[sc=7;ec=9;quickfixes=qf11]]
        // fix@qf11 {{Merge this if statement with the enclosing one}}
        // edit@qf11 [[sl=-1;el=+0;sc=14;ec=11]] {{ && }}
        // edit@qf11 [[sl=-1;el=-1;sc=9;ec=9]] {{(}}
        // edit@qf11 [[sl=-1;el=-1;sc=14;ec=14]] {{)}}
        // edit@qf11 [[sl=+7;el=+7;sc=5;ec=6]] {{}}
      }
    }
    if (a? b: c) {
      if (c) { // Noncompliant [[sc=7;ec=9;quickfixes=qf12]]
        // fix@qf12 {{Merge this if statement with the enclosing one}}
        // edit@qf12 [[sl=-1;el=+0;sc=16;ec=11]] {{ && }}
        // edit@qf12 [[sl=-1;el=-1;sc=9;ec=9]] {{(}}
        // edit@qf12 [[sl=-1;el=-1;sc=16;ec=16]] {{)}}
        // edit@qf12 [[sl=+7;el=+7;sc=5;ec=6]] {{}}
      }
    }
  }

  void operatorsWithHigherPrecedenceCoverage(boolean a, boolean b, boolean c) {
    if (a | b) {
      if (c) { // Noncompliant [[sc=7;ec=9;quickfixes=qf13]]
        // fix@qf13 {{Merge this if statement with the enclosing one}}
        // edit@qf13 [[sl=-1;el=+0;sc=14;ec=11]] {{ && }}
        // edit@qf13 [[sl=+5;el=+5;sc=5;ec=6]] {{}}
      }
    }
  }
}
