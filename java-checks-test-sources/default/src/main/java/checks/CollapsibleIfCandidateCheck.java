package checks;

import java.util.logging.Level;
import java.util.logging.Logger;

public class CollapsibleIfCandidateCheck {
  private static final Logger LOGGER = Logger.getLogger(CollapsibleIfCandidateCheck.class.getCanonicalName());

  void testMyFile(File file) {
    // fix@qf1 {{Merge this if statement with the enclosing one}}
    // edit@qf1 [[sl=-1;el=+4;sc=5;ec=6]] {{if (file != null && (file.isFile() || file.isDirectory())) {\n      LOGGER.log(Level.INFO, file.getAbsolutePath());\n    }}}
    if (file != null) {
      if (file.isFile() || file.isDirectory()) { // Noncompliant [[quickfixes=qf1]]
//    ^^
        LOGGER.log(Level.INFO, file.getAbsolutePath());
      }
    }
  }

  void noBraceOnOuter(File file) {
    // fix@qf2 {{Merge this if statement with the enclosing one}}
    // edit@qf2 [[sl=-1;el=+3;sc=5;ec=8]] {{if (file != null && (file.isFile() || file.isDirectory())) {\n      LOGGER.log(Level.INFO, file.getAbsolutePath());\n    }}}
    if (file != null)
      if (file.isFile() || file.isDirectory()) { // Noncompliant [[quickfixes=qf2]]
//    ^^
        LOGGER.log(Level.INFO, file.getAbsolutePath());
      }
  }

  void noBraceOnInner(File file) {
    // fix@qf3 {{Merge this if statement with the enclosing one}}
    // edit@qf3 [[sl=-1;el=+2;sc=5;ec=6]] {{if (file != null && (file.isFile() || file.isDirectory())) \n      LOGGER.log(Level.INFO, file.getAbsolutePath());}}
    if (file != null) {
      if (file.isFile() || file.isDirectory()) LOGGER.log(Level.INFO, file.getAbsolutePath()); // Noncompliant [[quickfixes=qf3]]
//    ^^
    }
  }

  void leftConditionNeedsParenthesis(boolean a, boolean b, boolean c) {
    // fix@qf4 {{Merge this if statement with the enclosing one}}
    // edit@qf4 [[sl=-1;el=+3;sc=5;ec=6]] {{if ((a || b) && c) {\n      \n    }}}
    if (a || b) {
      if (c) { // Noncompliant [[quickfixes=qf4]]
//    ^^
      }
    }
  }

  void rightConditionNeedsParenthesis(boolean a, boolean c, boolean d) {
    // fix@qf5 {{Merge this if statement with the enclosing one}}
    // edit@qf5 [[sl=-1;el=+3;sc=5;ec=6]] {{if (a && (c || d)) {\n      \n    }}}
    if (a) {
      if (c || d) { // Noncompliant [[quickfixes=qf5]]
//    ^^
      }
    }
  }

  void bothConditionsNeedParenthesis(boolean a, boolean b, boolean c, boolean d) {
    // fix@qf6 {{Merge this if statement with the enclosing one}}
    // edit@qf6 [[sl=-1;el=+3;sc=5;ec=6]] {{if ((a || b) && (c || d)) {\n      \n    }}}
    if (a || b) {
      if (c || d) { // Noncompliant [[quickfixes=qf6]]
//    ^^
      }
    }
  }

  void noConditionNeedsParenthesis(boolean a, boolean c) {
    // fix@qf7 {{Merge this if statement with the enclosing one}}
    // edit@qf7 [[sl=-1;el=+3;sc=5;ec=6]] {{if (a && c) {\n      \n    }}}
    if (a) {
      if (c) { // Noncompliant [[quickfixes=qf7]]
//    ^^
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
    if (a) if (c); // Noncompliant [[quickfixes=qf10]]
//         ^^
    // fix@qf10 {{Merge this if statement with the enclosing one}}
    // edit@qf10 [[sc=5;ec=19]] {{if (a && c) \n      ;}}
  }

  void operatorsWithLowerPrecedenceCoverage(boolean b, boolean c) {
    boolean a;
    // fix@qf11 {{Merge this if statement with the enclosing one}}
    // edit@qf11 [[sl=-1;el=+3;sc=5;ec=6]] {{if ((a = b) && c) {\n      \n    }}}
    if (a = b) {
      if (c) { // Noncompliant [[quickfixes=qf11]]
//    ^^
      }
    }
    // fix@qf12 {{Merge this if statement with the enclosing one}}
    // edit@qf12 [[sl=-1;el=+3;sc=5;ec=6]] {{if ((a ? b : c) && c) {\n      \n    }}}
    if (a? b: c) {
      if (c) { // Noncompliant [[quickfixes=qf12]]
//    ^^
      }
    }
  }

  void operatorsWithHigherPrecedenceCoverage(boolean a, boolean b, boolean c) {
    // fix@qf13 {{Merge this if statement with the enclosing one}}
    // edit@qf13 [[sl=-1;el=+3;sc=5;ec=6]] {{if (a | b && c) {\n      \n    }}}
    if (a | b) {
      if (c) { // Noncompliant [[quickfixes=qf13]]
//    ^^
      }
    }
  }
}
