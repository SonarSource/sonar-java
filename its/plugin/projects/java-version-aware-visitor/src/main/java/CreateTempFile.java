import java.io.File;
import java.io.IOException;

public class CreateTempFile {
  private void foo() throws IOException {
    File tempDir;
    tempDir = File.createTempFile("", ".");
    tempDir.delete();
    tempDir.mkdir(); // squid:S2976 when sonar.java.source is set to java7+
  }
}

