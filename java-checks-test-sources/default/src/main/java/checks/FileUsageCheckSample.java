package checks;

import java.io.File;
import java.nio.file.Path;

public class FileUsageCheckSample {
  public File getAbcFile() {
    return new File("abc.txt"); // Noncompliant
  }

  public Path getAbcPath() {
    return new Path.of("abc.txt");
  }

  public void checkFile() {
    if (new File("/home/bill").exists()) { // Noncompliant
      // no-op
    }
  }
}
