import java.io.File;
import java.nio.file.Path;
class A {
  public void cleanUp(Path path) {
    File file = new File(path);
    if (!file.delete()) {  // Noncompliant {{Use "java.nio.Files#delete" here for better messages on error conditions.}}
    }
    java.nio.Files.delete(path); // compliant
  }
}
