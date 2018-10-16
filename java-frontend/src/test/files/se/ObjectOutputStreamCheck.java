import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectOutputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.nio.file.OpenOption;

class A {
  void noncompliant_1(String fileName) throws IOException {
    FileOutputStream fos = new FileOutputStream(fileName , true);  // fos opened in append mode
    ObjectOutputStream out = new ObjectOutputStream(fos);  // Noncompliant
  }
  void noncompliant_2(String fileName, boolean appendMode) throws IOException {
    if (!appendMode) return;
    FileOutputStream fos = new FileOutputStream(fileName, appendMode);  // fos opened in append mode
    ObjectOutputStream out = new ObjectOutputStream(fos);  // Noncompliant
  }
  void noncompliant_3(File file) throws IOException {
    FileOutputStream fos = new FileOutputStream(file , true);  // fos opened in append mode
    ObjectOutputStream out = new ObjectOutputStream(fos);  // Noncompliant
  }

  void noncompliant_10() throws IOException {
    FileOutputStream fos = Files.newOutputStream(Paths.get("a"), StandardOpenOption.APPEND);
    ObjectOutputStream out = new ObjectOutputStream(fos); // Noncompliant
  }
  void noncompliant_11() throws IOException {
    FileOutputStream fos = Files.newOutputStream(Paths.get("a"), StandardOpenOption.DELETE_ON_CLOSE, StandardOpenOption.APPEND);
    ObjectOutputStream out = new ObjectOutputStream(fos); // Noncompliant
  }
  void noncompliant_12() throws IOException {
    OpenOption openOption = StandardOpenOption.APPEND;
    FileOutputStream fos = Files.newOutputStream(Paths.get("a"), StandardOpenOption.DELETE_ON_CLOSE, openOption);
    ObjectOutputStream out = new ObjectOutputStream(fos); // Noncompliant
  }

  void compliant_1(String fileName) throws IOException {
    FileOutputStream fos = new FileOutputStream(fileName, false);
    ObjectOutputStream out = new ObjectOutputStream(fos);
  }
  void compliant_2(String fileName) throws IOException {
    FileOutputStream fos = new FileOutputStream(fileName);
    ObjectOutputStream out = new ObjectOutputStream(fos);
  }

  void compliant_10() throws IOException {
    FileOutputStream fos = Files.newOutputStream(Paths.get("a"), StandardOpenOption.TRUNCATE);
    ObjectOutputStream out = new ObjectOutputStream(fos);
  }
}
