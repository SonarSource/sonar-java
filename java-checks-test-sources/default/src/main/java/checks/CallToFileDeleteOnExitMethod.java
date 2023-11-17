package checks;

import java.io.File;

public class CallToFileDeleteOnExitMethod {

  public void doWork(File file) {
    file.deleteOnExit(); // Noncompliant [[sc=10;ec=22]] {{Remove this call to "deleteOnExit".}}
    file.delete();
  }

}
