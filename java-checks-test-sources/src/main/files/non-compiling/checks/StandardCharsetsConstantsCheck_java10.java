import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.InputStream;
import java.nio.channels.ReadableByteChannel;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.Scanner;

class A {
  private InputStream inputStream;
  private File file;
  private Path path;
  private ReadableByteChannel readableByteChannel;

  void myMethod() throws Exception {
    Charset.forName("UTF-8"); // Noncompliant {{Replace Charset.forName() call with StandardCharsets.UTF_8}}

    (new ByteArrayOutputStream()).toString("UTF-8"); // Noncompliant {{Replace charset name argument with StandardCharsets.UTF_8}} [[sc=44;ec=51]]
    (new ByteArrayOutputStream()).toString(StandardCharsets.UTF_8);

    new Scanner(inputStream, "UTF-8"); // Noncompliant {{Replace charset name argument with StandardCharsets.UTF_8}} [[sc=30;ec=37]]
    new Scanner(inputStream, StandardCharsets.UTF_8);
    new Scanner(file, "UTF-8"); // Noncompliant {{Replace charset name argument with StandardCharsets.UTF_8}} [[sc=23;ec=30]]
    new Scanner(file, StandardCharsets.UTF_8);
    new Scanner(path, "UTF-8"); // Noncompliant {{Replace charset name argument with StandardCharsets.UTF_8}} [[sc=23;ec=30]]
    new Scanner(path, StandardCharsets.UTF_8);
    new Scanner(readableByteChannel, "UTF-8"); // Noncompliant {{Replace charset name argument with StandardCharsets.UTF_8}} [[sc=38;ec=45]]
    new Scanner(readableByteChannel, StandardCharsets.UTF_8);
  }
}
