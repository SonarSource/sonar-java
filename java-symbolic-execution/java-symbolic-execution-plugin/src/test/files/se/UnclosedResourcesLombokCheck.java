import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import lombok.Cleanup;

class UnclosedResourcesLombokCheck {
  public void fullyQualified(String fileName) throws IOException {
    @lombok.Cleanup
    InputStream in = new FileInputStream(fileName);
    in.read();
  }

  public void sameButAnnotated(String fileName) throws IOException {
    @Cleanup
    InputStream in = new FileInputStream(fileName);
    in.read();
  }
}

