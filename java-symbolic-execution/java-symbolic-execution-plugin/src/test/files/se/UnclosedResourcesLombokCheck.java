import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import lombok.Cleanup;

class UnclosedResourcesLombokCheck {
  public void wrongHandling() throws IOException {
    FileInputStream stream = new FileInputStream("myFile"); // Noncompliant
    stream.read();
  }

  void withLombokCleanup(String fileName) throws IOException {
    @Cleanup
    InputStream in = new FileInputStream(fileName);
    in.read();
  }
}

