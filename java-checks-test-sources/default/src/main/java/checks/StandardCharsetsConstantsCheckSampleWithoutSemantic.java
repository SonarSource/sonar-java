package checks;

import java.io.File;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.Reader;
import java.io.Writer;
import java.net.URI;
import java.net.URL;
import java.nio.channels.ReadableByteChannel;
import java.nio.charset.Charset;
import java.nio.file.Path;
import java.util.Collection;

class StandardCharsetsConstantsCheckSampleWithoutSemantic {
  private Charset charset;
  private byte[] bytes;
  private char[] chars;
  private int offset;
  private int length;
  private int bufferSize;
  private int blockSize;
  private boolean append;
  private boolean writeImmediately;
  private String dataString;
  private String inputString;
  private String lineEndingString;
  private String lockDirString;
  private String charsetName;
  private InputStream inputStream;
  private OutputStream outputStream;
  private Reader reader;
  private Writer writer;
  private StringBuffer stringBuffer;
  private CharSequence charSequence;
  private Collection<?> collection;
  private File file;
  private Path path;
  private URI uri;
  private URL url;
  private ReadableByteChannel readableByteChannel;

  void myMethod() throws Exception {
    charset = com.google.common.base.Charsets.ISO_8859_1; // FN

    charset = com.google.common.base.Charsets.US_ASCII; // FN
    charset = com.google.common.base.Charsets.UTF_16; // FN
    charset = com.google.common.base.Charsets.UTF_16BE; // FN
    charset = com.google.common.base.Charsets.UTF_16LE; // FN
    charset = com.google.common.base.Charsets.UTF_8; // FN

    // Canonical names of java.nio API and java.io/java.lang API
    Charset.forName("ISO-8859-1"); // Noncompliant

    Charset.forName("ISO_8859_1"); // Noncompliant
    Charset.forName("US-ASCII"); // Noncompliant
    Charset.forName("ASCII"); // Noncompliant
    Charset.forName("UTF-16"); // Noncompliant
    Charset.forName("UTF-16BE"); // Noncompliant
    Charset.forName("UnicodeBigUnmarked"); // Noncompliant
    Charset.forName("UTF-16LE"); // Noncompliant
    Charset.forName("UnicodeLittleUnmarked"); // Noncompliant
    Charset.forName("UTF-8"); // Noncompliant
    Charset.forName("UTF8"); // Noncompliant
    Charset.forName("utf-8"); // Noncompliant
    Charset.forName("Utf-8"); // Noncompliant

    org.apache.commons.codec.Charsets.toCharset("UTF-8"); // FN


    org.apache.commons.io.Charsets.toCharset("UTF-8"); // FN


    org.apache.commons.io.IOUtils.toString(bytes, "UTF-8"); // FN

    org.apache.commons.io.IOUtils.toString(inputStream, "UTF-8"); // FN

    org.apache.commons.io.IOUtils.toString(uri, "UTF-8"); // FN

    org.apache.commons.io.IOUtils.toString(url, "UTF-8"); // FN


    "".getBytes("UTF-8"); // Noncompliant

    new String(bytes, org.apache.commons.lang.CharEncoding.UTF_8); // FN
    new String(bytes, offset, length, org.apache.commons.lang.CharEncoding.UTF_8); // FN

    new InputStreamReader(inputStream, org.apache.commons.lang.CharEncoding.UTF_8); // FN
    new OutputStreamWriter(outputStream, org.apache.commons.lang.CharEncoding.UTF_8); // FN

    new org.apache.commons.codec.binary.Hex("UTF-8"); // FN
    new org.apache.commons.codec.net.QuotedPrintableCodec("UTF-8"); // FN

    org.apache.commons.io.FileUtils.readFileToString(file, "UTF-8"); // FN
    org.apache.commons.io.FileUtils.readLines(file, "UTF-8"); // FN
    org.apache.commons.io.FileUtils.write(file, charSequence, "UTF-8"); // FN
    org.apache.commons.io.FileUtils.write(file, charSequence, "UTF-8", append); // FN
    org.apache.commons.io.FileUtils.writeStringToFile(file, dataString, "UTF-8"); // FN

    org.apache.commons.io.FileUtils.writeStringToFile(file, dataString, "UTF-8", append); // FN

    org.apache.commons.io.IOUtils.copy(inputStream, writer, "UTF-8"); // FN
    org.apache.commons.io.IOUtils.copy(reader, outputStream, "UTF-8"); // FN
    org.apache.commons.io.IOUtils.lineIterator(inputStream, "UTF-8"); // FN
    org.apache.commons.io.IOUtils.readLines(inputStream, "UTF-8"); // FN
    org.apache.commons.io.IOUtils.toByteArray(reader, "UTF-8"); // FN
    org.apache.commons.io.IOUtils.toCharArray(inputStream, "UTF-8"); // FN
    org.apache.commons.io.IOUtils.toInputStream(charSequence, "UTF-8"); // FN
    org.apache.commons.io.IOUtils.toInputStream(inputString, "UTF-8"); // FN

    org.apache.commons.io.IOUtils.write(bytes, writer, "UTF-8"); // FN
    org.apache.commons.io.IOUtils.write(chars, outputStream, "UTF-8"); // FN
    org.apache.commons.io.IOUtils.write(charSequence, outputStream, "UTF-8"); // FN
    org.apache.commons.io.IOUtils.write(dataString, outputStream, "UTF-8"); // FN

    org.apache.commons.io.IOUtils.write(stringBuffer, outputStream, "UTF-8"); // FN
    org.apache.commons.io.IOUtils.writeLines(collection, lineEndingString, outputStream, "UTF-8"); // FN

    new org.apache.commons.io.input.CharSequenceInputStream(charSequence, "UTF-8"); // FN
    new org.apache.commons.io.input.CharSequenceInputStream(charSequence, "UTF-8", bufferSize); // FN
    new org.apache.commons.io.input.ReaderInputStream(reader, "UTF-8"); // FN
    new org.apache.commons.io.input.ReaderInputStream(reader, "UTF-8", bufferSize); // FN
    new org.apache.commons.io.input.ReversedLinesFileReader(file, blockSize, "UTF-8"); // FN
    new org.apache.commons.io.output.LockableFileWriter(file, "UTF-8"); // FN
    new org.apache.commons.io.output.LockableFileWriter(file, "UTF-8", append, lockDirString); // FN

    new org.apache.commons.io.output.WriterOutputStream(writer, "UTF-8"); // FN
    new org.apache.commons.io.output.WriterOutputStream(writer, "UTF-8", bufferSize, writeImmediately); // FN

    // Compliant
    charset = java.nio.charset.StandardCharsets.ISO_8859_1;
    charset = java.nio.charset.StandardCharsets.US_ASCII;
    charset = java.nio.charset.StandardCharsets.UTF_16;
    charset = java.nio.charset.StandardCharsets.UTF_16BE;
    charset = java.nio.charset.StandardCharsets.UTF_16LE;
    charset = java.nio.charset.StandardCharsets.UTF_8;

    "".getBytes(charsetName);
    "".getBytes("Windows-1252");

    Charset charset = Charset.forName(charsetName);
    "".getBytes(charset);

    new String(bytes, charsetName);
    new String(bytes, offset, length, charsetName);

    new InputStreamReader(inputStream, charsetName);
    new OutputStreamWriter(outputStream, charsetName);
  }

  void quickfixes() throws Exception {
    charset = com.google.common.base.Charsets.ISO_8859_1; // FN




    charset = com.google.common.base.Charsets.US_ASCII; // FN




    charset = com.google.common.base.Charsets.UTF_16; // FN




    charset = com.google.common.base.Charsets.UTF_16BE; // FN




    charset = com.google.common.base.Charsets.UTF_16LE; // FN




    charset = com.google.common.base.Charsets.UTF_8; // FN





    Charset.forName("ISO-8859-1"); // Noncompliant




    Charset.forName("ISO_8859_1"); // Noncompliant




    Charset.forName("UTF8"); // Noncompliant




    Charset.forName("utf-8"); // Noncompliant




    Charset.forName("UTF-16LE"); // Noncompliant




    Charset.forName("UnicodeLittleUnmarked"); // Noncompliant




    org.apache.commons.codec.Charsets.toCharset("UTF-8"); // FN





    org.apache.commons.io.IOUtils.toString(inputStream, "UTF-8"); // FN




    "".getBytes("UTF-8"); // Noncompliant




    new String(bytes, offset, length, org.apache.commons.lang.CharEncoding.UTF_8); // FN




    org.apache.commons.io.FileUtils.write(file, charSequence, "UTF-8"); // FN




     org.apache.commons.io.IOUtils.toCharArray(inputStream, "UTF-8"); // FN




    new org.apache.commons.io.input.ReaderInputStream(reader, "ISO-8859-1", bufferSize); // FN




  }
}
