package checks.security;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.Writer;
import java.nio.file.Files;
import java.nio.file.Path;

public class AndroidUnencryptedFilesCheck {
  void fileWrite(Path path) throws IOException {
    Files.write(path, "content".getBytes()); // Noncompliant [[sc=11;ec=16]] {{Make sure using unencrypted files is safe here.}}
  }

  void fileOutputStreamWrite(File file) throws IOException {
    FileOutputStream out = new FileOutputStream(file);
    out.write("content".getBytes()); // Noncompliant [[sc=9;ec=14]] {{Make sure using unencrypted files is safe here.}}
  }

  void fileOutputStreamWrite(Writer writer) throws IOException {
    BufferedWriter output = new BufferedWriter(writer);
    output.write("some test content..."); // Noncompliant [[sc=12;ec=17]]
  }

}
