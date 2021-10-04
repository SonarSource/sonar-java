package checks.security;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.Writer;
import java.nio.file.Files;
import java.nio.file.Path;

public class AndroidUnencryptedFilesCheck {
  void fileWrite(Path path) throws IOException {
    Files.write(path, "content".getBytes()); // Noncompliant [[sc=11;ec=16]] {{Make sure using unencrypted files is safe here.}}
  }

  void fileOutputStreamWrite(File file) throws IOException {
    FileOutputStream out = new FileOutputStream(file); // Noncompliant [[sc=32;ec=48]] {{Make sure using unencrypted files is safe here.}}
    out.write("content".getBytes());
  }

  void fileOutputStreamWrite(Writer writer) throws IOException {
    FileWriter fw = new FileWriter("outfilename", true); // Noncompliant [[sc=25;ec=35]] {{Make sure using unencrypted files is safe here.}}
    BufferedWriter output = new BufferedWriter(fw); // Compliant, reported on
    output.write("some test content...");
  }

}
