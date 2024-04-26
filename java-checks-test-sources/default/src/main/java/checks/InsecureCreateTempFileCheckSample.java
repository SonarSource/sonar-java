package checks;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

class InsecureCreateTempFileCheckSample {

  private static class A {}

  static File b;

  static {
    try {
      b = File.createTempFile("", "");
    } catch (IOException e) {
      e.printStackTrace();
    }
  }

  boolean mkdir = b.mkdir();

  static {
    try {
      b = File.createTempFile("", "");
    } catch (IOException e) {
      e.printStackTrace();
    }
  }

  private static File b() {
    return b;
  }

  private void noncompliant() throws IOException {
    File tempDir;
    tempDir = (File.createTempFile("", "."));
    tempDir.delete();
    tempDir.mkdir(); // Noncompliant {{Use "Files.createTempDirectory" to create this directory instead.}}
    File tempDir2 = File.createTempFile("", ".");
    tempDir2.delete();
    tempDir2.mkdir(); // Noncompliant
    tempDir2.mkdir(); // issue already raised

    b = File.createTempFile("", ".");
    b.delete();

    A a = new A() {
      private void noncompliant() throws IOException {
        b = File.createTempFile("", ".");
        b.delete();
        b.mkdir(); // Noncompliant
      }
    };

    b.mkdir(); // Noncompliant
    b.mkdir(); // issue already raised
  }

  private void compliant() throws IOException {
    Path tempPath = Files.createTempDirectory("");
    File tempDir = tempPath.toFile();
    File file = new File("name");
    file.mkdir();
    int a = 5;
    a = 6;
    InsecureCreateTempFileCheckSample.b = File.createTempFile("", ".");
    b().mkdir();
  }
}
