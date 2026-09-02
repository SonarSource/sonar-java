package checks;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.BufferedOutputStream;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.jar.JarEntry;
import java.util.jar.JarOutputStream;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

class EmptyArchiveEntryCheckSample {

  void emptyZipEntry() throws IOException {
    ZipOutputStream zos = new ZipOutputStream(new FileOutputStream("archive.zip"));
    zos.putNextEntry(new ZipEntry("empty.txt"));
//      ^^^^^^^^^^^^>
    zos.closeEntry(); // Noncompliant {{Write content to this archive entry; it is empty.}}
//      ^^^^^^^^^^
    zos.close();
  }

  void multipleEntriesOneEmpty() throws IOException {
    ZipOutputStream zos = new ZipOutputStream(new FileOutputStream("archive.zip"));
    zos.putNextEntry(new ZipEntry("file.txt"));
    zos.write("content".getBytes());
    zos.closeEntry(); // Compliant - content written

    zos.putNextEntry(new ZipEntry("empty.txt"));
//      ^^^^^^^^^^^^>
    zos.closeEntry(); // Noncompliant
//      ^^^^^^^^^^
    zos.close();
  }

  void emptyEntryInLoop(String[] names) throws IOException {
    ZipOutputStream zos = new ZipOutputStream(new FileOutputStream("archive.zip"));
    for (String name : names) {
      zos.putNextEntry(new ZipEntry(name));
//        ^^^^^^^^^^^^>
      zos.closeEntry(); // Noncompliant
//        ^^^^^^^^^^
    }
    zos.close();
  }

  void emptyJarEntry() throws IOException {
    JarOutputStream jos = new JarOutputStream(new FileOutputStream("app.jar"));
    jos.putNextEntry(new JarEntry("empty.class"));
//      ^^^^^^^^^^^^>
    jos.closeEntry(); // Noncompliant
//      ^^^^^^^^^^
    jos.close();
  }

  void jarWithZipEntry() throws IOException {
    JarOutputStream jos = new JarOutputStream(new FileOutputStream("app.jar"));
    jos.putNextEntry(new ZipEntry("empty.txt"));
//      ^^^^^^^^^^^^>
    jos.closeEntry(); // Noncompliant
//      ^^^^^^^^^^
    jos.close();
  }

  void zipWithContent() throws IOException {
    ZipOutputStream zos = new ZipOutputStream(new FileOutputStream("archive.zip"));
    zos.putNextEntry(new ZipEntry("file.txt"));
    zos.write("Hello, World!".getBytes());
    zos.closeEntry(); // Compliant
    zos.close();
  }

  void writeFromFile() throws IOException {
    ZipOutputStream zos = new ZipOutputStream(new FileOutputStream("archive.zip"));
    zos.putNextEntry(new ZipEntry("data.bin"));
    FileInputStream fis = new FileInputStream("data.bin");
    byte[] buffer = new byte[1024];
    int len;
    while ((len = fis.read(buffer)) > 0) {
      zos.write(buffer, 0, len);
    }
    fis.close();
    zos.closeEntry(); // Compliant
    zos.close();
  }

  void writeByteArray() throws IOException {
    ZipOutputStream zos = new ZipOutputStream(new FileOutputStream("archive.zip"));
    zos.putNextEntry(new ZipEntry("data.txt"));
    byte[] data = "content".getBytes();
    zos.write(data, 0, data.length);
    zos.closeEntry(); // Compliant
    zos.close();
  }

  void jarWithContent() throws IOException {
    JarOutputStream jos = new JarOutputStream(new FileOutputStream("app.jar"));
    jos.putNextEntry(new JarEntry("META-INF/MANIFEST.MF"));
    jos.write("Manifest-Version: 1.0\n".getBytes());
    jos.closeEntry(); // Compliant
    jos.close();
  }

  void multipleJarEntriesAllWithContent() throws IOException {
    JarOutputStream jos = new JarOutputStream(new FileOutputStream("app.jar"));
    jos.putNextEntry(new JarEntry("a.class"));
    jos.write(new byte[]{1, 2, 3});
    jos.closeEntry(); // Compliant

    jos.putNextEntry(new JarEntry("b.class"));
    jos.write(new byte[]{4, 5, 6});
    jos.closeEntry(); // Compliant
    jos.close();
  }

  void writeSingleByte() throws IOException {
    ZipOutputStream zos = new ZipOutputStream(new FileOutputStream("archive.zip"));
    zos.putNextEntry(new ZipEntry("byte.txt"));
    zos.write(65);
    zos.closeEntry(); // Compliant
    zos.close();
  }

  void writeInIfBlock() throws IOException {
    ZipOutputStream zos = new ZipOutputStream(new FileOutputStream("archive.zip"));
    zos.putNextEntry(new ZipEntry("conditional.txt"));
    if (System.currentTimeMillis() > 0) {
      zos.write("data".getBytes());
    }
    zos.closeEntry(); // Compliant - write is in a conditional block
    zos.close();
  }

  void helperMethodWithStream() throws IOException {
    ZipOutputStream zos = new ZipOutputStream(new FileOutputStream("archive.zip"));
    zos.putNextEntry(new ZipEntry("helper.txt"));
    writeContent(zos);
    zos.closeEntry(); // Compliant - stream passed to helper method
    zos.close();
  }

  private void writeContent(ZipOutputStream zos) throws IOException {
    zos.write("content".getBytes());
  }

  void twoStreamsIndependent() throws IOException {
    ZipOutputStream zos1 = new ZipOutputStream(new FileOutputStream("a.zip"));
    ZipOutputStream zos2 = new ZipOutputStream(new FileOutputStream("b.zip"));

    zos1.putNextEntry(new ZipEntry("file.txt"));
    zos2.putNextEntry(new ZipEntry("file.txt"));
    zos1.write("content".getBytes());
    zos1.closeEntry(); // Compliant - zos1 has content

    zos2.putNextEntry(new ZipEntry("empty.txt"));
//       ^^^^^^^^^^^^>
    zos2.closeEntry(); // Noncompliant
//       ^^^^^^^^^^

    zos1.close();
    zos2.close();
  }

  void noCloseEntry() throws IOException {
    ZipOutputStream zos = new ZipOutputStream(new FileOutputStream("archive.zip"));
    zos.putNextEntry(new ZipEntry("file.txt"));
    // No closeEntry - no issue raised (putNextEntry without closeEntry is not flagged)
    zos.close();
  }

  void closeEntryWithoutPutNextEntry() throws IOException {
    ZipOutputStream zos = new ZipOutputStream(new FileOutputStream("archive.zip"));
    zos.closeEntry(); // Compliant - no preceding putNextEntry tracked
    zos.close();
  }

  void zipDirectoryEntry() throws IOException {
    ZipOutputStream zos = new ZipOutputStream(new FileOutputStream("archive.zip"));
    zos.putNextEntry(new ZipEntry("dir/"));
    zos.closeEntry(); // Compliant - directory entry
    zos.close();
  }

  void writeViaWrapper() throws IOException {
    ZipOutputStream zos = new ZipOutputStream(new FileOutputStream("archive.zip"));
    zos.putNextEntry(new ZipEntry("wrapped.txt"));
    java.io.PrintWriter pw = new java.io.PrintWriter(zos);
    pw.println("content via wrapper");
    pw.flush();
    zos.closeEntry(); // Compliant - written through wrapper
    zos.close();
  }

  void qualifiedHelperMethodCall() throws IOException {
    ZipOutputStream zos = new ZipOutputStream(new FileOutputStream("archive.zip"));
    zos.putNextEntry(new ZipEntry("helper.txt"));
    this.writeContent(zos);
    zos.closeEntry(); // Compliant - stream passed to qualified helper method
    zos.close();
  }

  void directoryEntryViaVariable() throws IOException {
    ZipOutputStream zos = new ZipOutputStream(new FileOutputStream("archive.zip"));
    ZipEntry directory = new ZipEntry("dir/");
    zos.putNextEntry(directory);
    zos.closeEntry(); // Compliant - directory entry via variable
    zos.close();
  }

  void writeViaBufferedOutputStream() throws IOException {
    ZipOutputStream zos = new ZipOutputStream(new FileOutputStream("archive.zip"));
    zos.putNextEntry(new ZipEntry("file.txt"));
    OutputStream writer = new BufferedOutputStream(zos);
    writer.write("data".getBytes());
    zos.closeEntry(); // Compliant - written through BufferedOutputStream wrapper
    zos.close();
  }

  void writeViaObjectMapperOnCustomOutputStream() throws IOException {
    FileOutputStream fos = new FileOutputStream("archive.zip");
    ZipOutputStream zos = new ZipOutputStream(fos);
    zos.putNextEntry(new ZipEntry("data.json"));
    ObjectMapper objectMapper = new ObjectMapper();
    objectMapper.writeValue(new NonClosingOutputStream(zos), new Object());
    zos.closeEntry(); // Compliant - zos passed to custom OutputStream wrapper used by ObjectMapper
    zos.close();
  }

  void writeViaObjectMapperWithCastArgument() throws IOException {
    FileOutputStream fos = new FileOutputStream("archive.zip");
    ZipOutputStream zos = new ZipOutputStream(fos);
    zos.putNextEntry(new ZipEntry("data.json"));
    ObjectMapper objectMapper = new ObjectMapper();
    objectMapper.writeValue((OutputStream) zos, new Object());
    zos.closeEntry(); // Compliant - zos passed via cast expression
    zos.close();
  }

  void writeViaCustomOutputStreamWithCastInNestedStatement() throws IOException {
    FileOutputStream fos = new FileOutputStream("archive.zip");
    ZipOutputStream zos = new ZipOutputStream(fos);
    zos.putNextEntry(new ZipEntry("data.json"));
    if (System.currentTimeMillis() > 0) {
      OutputStream out = new NonClosingOutputStream((OutputStream) zos);
      out.write("data".getBytes());
    }
    zos.closeEntry(); // Compliant - zos passed via cast to constructor in nested statement
    zos.close();
  }

  void writeViaMethodCallWithCastInNestedStatement() throws IOException {
    FileOutputStream fos = new FileOutputStream("archive.zip");
    ZipOutputStream zos = new ZipOutputStream(fos);
    zos.putNextEntry(new ZipEntry("data.json"));
    if (System.currentTimeMillis() > 0) {
      writeContent((ZipOutputStream) (zos));
    }
    zos.closeEntry(); // Compliant - zos passed via cast and parentheses in nested statement
    zos.close();
  }

  void writeViaCastReceiver() throws IOException {
    ZipOutputStream zos = new ZipOutputStream(new FileOutputStream("archive.zip"));
    zos.putNextEntry(new ZipEntry("cast.txt"));
    ((OutputStream) zos).write("data".getBytes());
    zos.closeEntry(); // Compliant - written through cast receiver
    zos.close();
  }

  void writeViaParenthesizedReceiver() throws IOException {
    ZipOutputStream zos = new ZipOutputStream(new FileOutputStream("archive.zip"));
    zos.putNextEntry(new ZipEntry("paren.txt"));
    (zos).write("data".getBytes());
    zos.closeEntry(); // Compliant - written through parenthesized receiver
    zos.close();
  }

  void writeViaCastReceiverInNestedStatement() throws IOException {
    ZipOutputStream zos = new ZipOutputStream(new FileOutputStream("archive.zip"));
    zos.putNextEntry(new ZipEntry("nested-cast.txt"));
    if (System.currentTimeMillis() > 0) {
      ((OutputStream) zos).write("data".getBytes());
    }
    zos.closeEntry(); // Compliant - written through cast receiver in nested statement
    zos.close();
  }

  void writeViaObjectMapperWithParenthesizedArgument() throws IOException {
    FileOutputStream fos = new FileOutputStream("archive.zip");
    ZipOutputStream zos = new ZipOutputStream(fos);
    zos.putNextEntry(new ZipEntry("data.json"));
    ObjectMapper objectMapper = new ObjectMapper();
    objectMapper.writeValue((zos), new Object());
    zos.closeEntry(); // Compliant - zos passed via parenthesized expression
    zos.close();
  }

  static class NonClosingOutputStream extends OutputStream {
    private final OutputStream delegate;

    NonClosingOutputStream(OutputStream delegate) {
      this.delegate = delegate;
    }

    @Override
    public void write(int b) throws IOException {
      delegate.write(b);
    }

    @Override
    public void write(byte[] b, int off, int len) throws IOException {
      delegate.write(b, off, len);
    }

    @Override
    public void close() {
      // Do not close the delegate
    }
  }
}
