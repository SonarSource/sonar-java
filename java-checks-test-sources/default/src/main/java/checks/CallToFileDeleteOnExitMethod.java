package checks;

import java.io.File;

public class CallToFileDeleteOnExitMethod {

  public void doWork(File file) {
    file.deleteOnExit(); // Noncompliant {{Remove this call to "deleteOnExit".}}
//       ^^^^^^^^^^^^
    file.delete();
  }

}
