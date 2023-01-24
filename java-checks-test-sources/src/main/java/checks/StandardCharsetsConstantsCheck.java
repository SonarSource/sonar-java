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
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.Collection;

class StandardCharsetsConstantsCheck {
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
    charset = com.google.common.base.Charsets.ISO_8859_1; // Noncompliant {{Replace "com.google.common.base.Charsets.ISO_8859_1" with "StandardCharsets.ISO_8859_1".}} [[sc=47;ec=57]]
    charset = com.google.common.base.Charsets.US_ASCII; // Noncompliant {{Replace "com.google.common.base.Charsets.US_ASCII" with "StandardCharsets.US_ASCII".}}
    charset = com.google.common.base.Charsets.UTF_16; // Noncompliant {{Replace "com.google.common.base.Charsets.UTF_16" with "StandardCharsets.UTF_16".}}
    charset = com.google.common.base.Charsets.UTF_16BE; // Noncompliant {{Replace "com.google.common.base.Charsets.UTF_16BE" with "StandardCharsets.UTF_16BE".}}
    charset = com.google.common.base.Charsets.UTF_16LE; // Noncompliant {{Replace "com.google.common.base.Charsets.UTF_16LE" with "StandardCharsets.UTF_16LE".}}
    charset = com.google.common.base.Charsets.UTF_8; // Noncompliant {{Replace "com.google.common.base.Charsets.UTF_8" with "StandardCharsets.UTF_8".}}

    // Canonical names of java.nio API and java.io/java.lang API
    Charset.forName("ISO-8859-1"); // Noncompliant {{Replace Charset.forName() call with StandardCharsets.ISO_8859_1}} [[sc=5;ec=34]]
    Charset.forName("ISO_8859_1"); // Noncompliant {{Replace Charset.forName() call with StandardCharsets.ISO_8859_1}}
    Charset.forName("US-ASCII"); // Noncompliant {{Replace Charset.forName() call with StandardCharsets.US_ASCII}}
    Charset.forName("ASCII"); // Noncompliant {{Replace Charset.forName() call with StandardCharsets.US_ASCII}}
    Charset.forName("UTF-16"); // Noncompliant {{Replace Charset.forName() call with StandardCharsets.UTF_16}}
    Charset.forName("UTF-16BE"); // Noncompliant {{Replace Charset.forName() call with StandardCharsets.UTF_16BE}}
    Charset.forName("UnicodeBigUnmarked"); // Noncompliant {{Replace Charset.forName() call with StandardCharsets.UTF_16BE}}
    Charset.forName("UTF-16LE"); // Noncompliant {{Replace Charset.forName() call with StandardCharsets.UTF_16LE}}
    Charset.forName("UnicodeLittleUnmarked"); // Noncompliant {{Replace Charset.forName() call with StandardCharsets.UTF_16LE}}
    Charset.forName("UTF-8"); // Noncompliant {{Replace Charset.forName() call with StandardCharsets.UTF_8}}
    Charset.forName("UTF8"); // Noncompliant {{Replace Charset.forName() call with StandardCharsets.UTF_8}}
    Charset.forName("utf-8"); // Noncompliant {{Replace Charset.forName() call with StandardCharsets.UTF_8}}
    Charset.forName("Utf-8"); // Noncompliant {{Replace Charset.forName() call with StandardCharsets.UTF_8}}

    org.apache.commons.codec.Charsets.toCharset("UTF-8"); // Noncompliant {{Replace Charsets.toCharset() call with StandardCharsets.UTF_8}} [[sc=5;ec=57]]

    org.apache.commons.io.Charsets.toCharset("UTF-8"); // Noncompliant {{Replace Charsets.toCharset() call with StandardCharsets.UTF_8}} [[sc=5;ec=54]]

    org.apache.commons.io.IOUtils.toString(bytes, "UTF-8"); // Noncompliant {{Replace IOUtils.toString() call with new String(..., StandardCharsets.UTF_8);}} [[sc=5;ec=59]]
    org.apache.commons.io.IOUtils.toString(inputStream, "UTF-8"); // Noncompliant {{Replace charset name argument with StandardCharsets.UTF_8}} [[sc=57;ec=64]]
    org.apache.commons.io.IOUtils.toString(uri, "UTF-8"); // Noncompliant {{Replace charset name argument with StandardCharsets.UTF_8}} [[sc=49;ec=56]]
    org.apache.commons.io.IOUtils.toString(url, "UTF-8"); // Noncompliant {{Replace charset name argument with StandardCharsets.UTF_8}} [[sc=49;ec=56]]

    "".getBytes("UTF-8"); // Noncompliant {{Replace charset name argument with StandardCharsets.UTF_8}}

    new String(bytes, org.apache.commons.lang.CharEncoding.UTF_8); // Noncompliant
    new String(bytes, offset, length, org.apache.commons.lang.CharEncoding.UTF_8); // Noncompliant

    new InputStreamReader(inputStream, org.apache.commons.lang.CharEncoding.UTF_8); // Noncompliant
    new OutputStreamWriter(outputStream, org.apache.commons.lang.CharEncoding.UTF_8); // Noncompliant

    new org.apache.commons.codec.binary.Hex("UTF-8"); // Noncompliant
    new org.apache.commons.codec.net.QuotedPrintableCodec("UTF-8"); // Noncompliant

    org.apache.commons.io.FileUtils.readFileToString(file, "UTF-8"); // Noncompliant
    org.apache.commons.io.FileUtils.readLines(file, "UTF-8");  // Noncompliant
    org.apache.commons.io.FileUtils.write(file, charSequence, "UTF-8"); // Noncompliant
    org.apache.commons.io.FileUtils.write(file, charSequence, "UTF-8", append); // Noncompliant
    org.apache.commons.io.FileUtils.writeStringToFile(file, dataString, "UTF-8"); // Noncompliant [[sc=73;ec=80]]
    org.apache.commons.io.FileUtils.writeStringToFile(file, dataString, "UTF-8", append); // Noncompliant [[sc=73;ec=80]]
    org.apache.commons.io.IOUtils.copy(inputStream, writer, "UTF-8"); // Noncompliant
    org.apache.commons.io.IOUtils.copy(reader, outputStream, "UTF-8"); // Noncompliant
    org.apache.commons.io.IOUtils.lineIterator(inputStream, "UTF-8"); // Noncompliant
    org.apache.commons.io.IOUtils.readLines(inputStream, "UTF-8"); // Noncompliant
    org.apache.commons.io.IOUtils.toByteArray(reader, "UTF-8"); // Noncompliant
    org.apache.commons.io.IOUtils.toCharArray(inputStream, "UTF-8"); // Noncompliant
    org.apache.commons.io.IOUtils.toInputStream(charSequence, "UTF-8"); // Noncompliant
    org.apache.commons.io.IOUtils.toInputStream(inputString, "UTF-8"); // Noncompliant [[sc=62;ec=69]]
    org.apache.commons.io.IOUtils.write(bytes, writer, "UTF-8"); // Noncompliant
    org.apache.commons.io.IOUtils.write(chars, outputStream, "UTF-8"); // Noncompliant
    org.apache.commons.io.IOUtils.write(charSequence, outputStream, "UTF-8"); // Noncompliant
    org.apache.commons.io.IOUtils.write(dataString, outputStream, "UTF-8"); // Noncompliant [[sc=67;ec=74]]
    org.apache.commons.io.IOUtils.write(stringBuffer, outputStream, "UTF-8"); // Noncompliant
    org.apache.commons.io.IOUtils.writeLines(collection, lineEndingString, outputStream, "UTF-8"); // Noncompliant [[sc=90;ec=97]]
    new org.apache.commons.io.input.CharSequenceInputStream(charSequence, "UTF-8"); // Noncompliant
    new org.apache.commons.io.input.CharSequenceInputStream(charSequence, "UTF-8", bufferSize); // Noncompliant
    new org.apache.commons.io.input.ReaderInputStream(reader, "UTF-8"); // Noncompliant
    new org.apache.commons.io.input.ReaderInputStream(reader, "UTF-8", bufferSize); // Noncompliant
    new org.apache.commons.io.input.ReversedLinesFileReader(file, blockSize, "UTF-8"); // Noncompliant
    new org.apache.commons.io.output.LockableFileWriter(file, "UTF-8"); // Noncompliant
    new org.apache.commons.io.output.LockableFileWriter(file, "UTF-8", append, lockDirString); // Noncompliant [[sc=63;ec=70]]
    new org.apache.commons.io.output.WriterOutputStream(writer, "UTF-8"); // Noncompliant
    new org.apache.commons.io.output.WriterOutputStream(writer, "UTF-8", bufferSize, writeImmediately); // Noncompliant

    // Compliant
    charset = StandardCharsets.ISO_8859_1;
    charset = StandardCharsets.US_ASCII;
    charset = StandardCharsets.UTF_16;
    charset = StandardCharsets.UTF_16BE;
    charset = StandardCharsets.UTF_16LE;
    charset = StandardCharsets.UTF_8;

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
    charset = com.google.common.base.Charsets.ISO_8859_1; // Noncompliant [[sc=47;ec=57;quickfixes=qf1]]
    // fix@qf1 {{Replace with "StandardCharsets.ISO_8859_1".}}
    // edit@qf1 [[sc=15;ec=46]] {{java.nio.charset.StandardCharsets}}
    charset = com.google.common.base.Charsets.US_ASCII; // Noncompliant [[sc=47;ec=55;quickfixes=qf2]]
    // fix@qf2 {{Replace with "StandardCharsets.US_ASCII".}}
    // edit@qf2 [[sc=15;ec=46]] {{java.nio.charset.StandardCharsets}}
    charset = com.google.common.base.Charsets.UTF_16; // Noncompliant [[sc=47;ec=53;quickfixes=qf3]]
    // fix@qf3 {{Replace with "StandardCharsets.UTF_16".}}
    // edit@qf3 [[sc=15;ec=46]] {{java.nio.charset.StandardCharsets}}
    charset = com.google.common.base.Charsets.UTF_16BE; // Noncompliant [[sc=47;ec=55;quickfixes=qf4]]
    // fix@qf4 {{Replace with "StandardCharsets.UTF_16BE".}}
    // edit@qf4 [[sc=15;ec=46]] {{java.nio.charset.StandardCharsets}}
    charset = com.google.common.base.Charsets.UTF_16LE; // Noncompliant [[sc=47;ec=55;quickfixes=qf5]]
    // fix@qf5 {{Replace with "StandardCharsets.UTF_16LE".}}
    // edit@qf5 [[sc=15;ec=46]] {{java.nio.charset.StandardCharsets}}
    charset = com.google.common.base.Charsets.UTF_8; // Noncompliant [[sc=47;ec=52;quickfixes=qf6]]
    // fix@qf6 {{Replace with "StandardCharsets.UTF_8".}}
    // edit@qf6 [[sc=15;ec=46]] {{java.nio.charset.StandardCharsets}}

    Charset.forName("ISO-8859-1"); // Noncompliant [[sc=5;ec=34;quickfixes=qf7]]
    // fix@qf7 {{Replace with "StandardCharsets.ISO_8859_1".}}
    // edit@qf7 [[sc=5;ec=34]] {{java.nio.charset.StandardCharsets.ISO_8859_1}}
    Charset.forName("ISO_8859_1"); // Noncompliant [[sc=5;ec=34;quickfixes=qf8]]
    // fix@qf8 {{Replace with "StandardCharsets.ISO_8859_1".}}
    // edit@qf8 [[sc=5;ec=34]] {{java.nio.charset.StandardCharsets.ISO_8859_1}}
    Charset.forName("UTF8"); // Noncompliant [[sc=5;ec=28;quickfixes=qf9]]
    // fix@qf9 {{Replace with "StandardCharsets.UTF_8".}}
    // edit@qf9 [[sc=5;ec=28]] {{java.nio.charset.StandardCharsets.UTF_8}}
    Charset.forName("utf-8"); // Noncompliant [[sc=5;ec=29;quickfixes=qf10]]
    // fix@qf10 {{Replace with "StandardCharsets.UTF_8".}}
    // edit@qf10 [[sc=5;ec=29]] {{java.nio.charset.StandardCharsets.UTF_8}}
    Charset.forName("UTF-16LE"); // Noncompliant [[sc=5;ec=32;quickfixes=qf11]]
    // fix@qf11 {{Replace with "StandardCharsets.UTF_16LE".}}
    // edit@qf11 [[sc=5;ec=32]] {{java.nio.charset.StandardCharsets.UTF_16LE}}
    Charset.forName("UnicodeLittleUnmarked"); // Noncompliant [[sc=5;ec=45;quickfixes=qf12]]
    // fix@qf12 {{Replace with "StandardCharsets.UTF_16LE".}}
    // edit@qf12 [[sc=5;ec=45]] {{java.nio.charset.StandardCharsets.UTF_16LE}}
    org.apache.commons.codec.Charsets.toCharset("UTF-8"); // Noncompliant [[sc=5;ec=57;quickfixes=qf13]]
    // fix@qf13 {{Replace with "StandardCharsets.UTF_8".}}
    // edit@qf13 [[sc=5;ec=57]] {{java.nio.charset.StandardCharsets.UTF_8}}

    org.apache.commons.io.IOUtils.toString(inputStream, "UTF-8"); // Noncompliant [[sc=57;ec=64;quickfixes=qf14]]
    // fix@qf14 {{Replace with "StandardCharsets.UTF_8".}}
    // edit@qf14 [[sc=57;ec=64]] {{java.nio.charset.StandardCharsets.UTF_8}}
    "".getBytes("UTF-8"); // Noncompliant [[sc=17;ec=24;quickfixes=qf15]]
    // fix@qf15 {{Replace with "StandardCharsets.UTF_8".}}
    // edit@qf15 [[sc=17;ec=24]] {{java.nio.charset.StandardCharsets.UTF_8}}
    new String(bytes, offset, length, org.apache.commons.lang.CharEncoding.UTF_8); // Noncompliant [[sc=39;ec=81;quickfixes=qf16]]
    // fix@qf16 {{Replace with "StandardCharsets.UTF_8".}}
    // edit@qf16 [[sc=39;ec=81]] {{java.nio.charset.StandardCharsets.UTF_8}}
    org.apache.commons.io.FileUtils.write(file, charSequence, "UTF-8"); // Noncompliant [[sc=63;ec=70;quickfixes=qf17]]
    // fix@qf17 {{Replace with "StandardCharsets.UTF_8".}}
    // edit@qf17 [[sc=63;ec=70]] {{java.nio.charset.StandardCharsets.UTF_8}}
     org.apache.commons.io.IOUtils.toCharArray(inputStream, "UTF-8"); // Noncompliant [[sc=61;ec=68;quickfixes=qf18]]
    // fix@qf18 {{Replace with "StandardCharsets.UTF_8".}}
    // edit@qf18 [[sc=61;ec=68]] {{java.nio.charset.StandardCharsets.UTF_8}}
    new org.apache.commons.io.input.ReaderInputStream(reader, "ISO-8859-1", bufferSize); // Noncompliant [[sc=63;ec=75;quickfixes=qf19]]
    // fix@qf19 {{Replace with "StandardCharsets.ISO_8859_1".}}
    // edit@qf19 [[sc=63;ec=75]] {{java.nio.charset.StandardCharsets.ISO_8859_1}}
  }
}
