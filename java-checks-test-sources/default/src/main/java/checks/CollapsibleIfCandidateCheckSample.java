package checks;

import java.util.logging.Level;
import java.util.logging.Logger;

public class CollapsibleIfCandidateCheckSample {
  private static final Logger LOGGER = Logger.getLogger(CollapsibleIfCandidateCheckSample.class.getCanonicalName());
  void testMyFile(File file) {
    if (file != null) {
      if (file.isFile() || file.isDirectory()) { // Noncompliant [[sc=7;ec=9;quickfixes=qf1]]
        LOGGER.log(Level.INFO, file.getAbsolutePath());
      }
      // fix@qf1 {{Merge this if statement with the enclosing one}}
      // edit@qf1 [[sl=+2;el=+2;sc=7;ec=8]] {{}}
      // edit@qf1 [[sc=47;ec=47]] {{)}}
      // edit@qf1 [[sl=-1;el=+0;sc=21;ec=9]] {{ && }}
    }
  }

  void noBraceOnOuter(File file) {
    if (file != null)
      if (file.isFile() || file.isDirectory()) { // Noncompliant [[sc=7;ec=9;quickfixes=qf2]]
        LOGGER.log(Level.INFO, file.getAbsolutePath());
      }
      // fix@qf2 {{Merge this if statement with the enclosing one}}
      // edit@qf2 [[sc=47;ec=47]] {{)}}
      // edit@qf2 [[sl=-1;el=+0;sc=21;ec=9]] {{ && }}
  }

  void noBraceOnInner(File file) {
    if (file != null) {
      if (file.isFile() || file.isDirectory()) LOGGER.log(Level.INFO, file.getAbsolutePath()); // Noncompliant [[sc=7;ec=9;quickfixes=qf3]]
      // fix@qf3 {{Merge this if statement with the enclosing one}}
      // edit@qf3 [[sc=48;ec=48]] {{{}}
      // edit@qf3 [[sc=47;ec=47]] {{)}}
      // edit@qf3 [[sl=-1;el=+0;sc=21;ec=9]] {{ && }}
    }
  }
}
