package checks;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.InputStream;
import java.nio.channels.ReadableByteChannel;
import java.nio.charset.Charset;
import java.nio.file.Path;
import java.util.Scanner;

class StandardCharsetsConstantsCheck_java8 {
  private InputStream inputStream;
  private File file;
  private Path path;
  private ReadableByteChannel readableByteChannel;

  void myMethod() throws Exception {
    Charset.forName("UTF-8"); // Noncompliant {{Replace Charset.forName() call with StandardCharsets.UTF_8}}

    (new ByteArrayOutputStream()).toString("UTF-8");

    new Scanner(inputStream, "UTF-8");
    new Scanner(file, "UTF-8");
    new Scanner(path, "UTF-8");
    new Scanner(readableByteChannel, "UTF-8");
  }
}
