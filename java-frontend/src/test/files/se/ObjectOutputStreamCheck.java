import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectOutputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.nio.file.OpenOption;

import static java.nio.file.StandardOpenOption.APPEND;

class A {
  void noncompliant_1(String fileName) throws IOException {
    FileOutputStream fos = new FileOutputStream(fileName , true);  // fos opened in append mode
    ObjectOutputStream out = new ObjectOutputStream(fos);  // Noncompliant {{Do not use a FileOutputStream in append mode.}}
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
    FileOutputStream fos = Files.newOutputStream(Paths.get("a"), StandardOpenOption.APPEND); // flow@f1 {{FileOutputStream created here.}}
    ObjectOutputStream out = new ObjectOutputStream(fos); // Noncompliant [[flows=f1]]
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
  void noncompliant_13() throws IOException {
    FileOutputStream fos = Files.newOutputStream(Paths.get("a"), APPEND);
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
    FileOutputStream fos = Files.newOutputStream(Paths.get("a"), StandardOpenOption.TRUNCATE_EXISTING);
    ObjectOutputStream out = new ObjectOutputStream(fos);
  }

  void coverage() throws IOException {
    ObjectOutputStream out = new ObjectOutputStream();
  }
}
