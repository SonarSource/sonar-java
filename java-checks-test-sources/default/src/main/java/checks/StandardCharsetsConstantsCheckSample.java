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

class StandardCharsetsConstantsCheckSample {
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
    charset = com.google.common.base.Charsets.ISO_8859_1; // Noncompliant {{Replace "com.google.common.base.Charsets.ISO_8859_1" with "StandardCharsets.ISO_8859_1".}}
//                                            ^^^^^^^^^^
    charset = com.google.common.base.Charsets.US_ASCII; // Noncompliant {{Replace "com.google.common.base.Charsets.US_ASCII" with "StandardCharsets.US_ASCII".}}
    charset = com.google.common.base.Charsets.UTF_16; // Noncompliant {{Replace "com.google.common.base.Charsets.UTF_16" with "StandardCharsets.UTF_16".}}
    charset = com.google.common.base.Charsets.UTF_16BE; // Noncompliant {{Replace "com.google.common.base.Charsets.UTF_16BE" with "StandardCharsets.UTF_16BE".}}
    charset = com.google.common.base.Charsets.UTF_16LE; // Noncompliant {{Replace "com.google.common.base.Charsets.UTF_16LE" with "StandardCharsets.UTF_16LE".}}
    charset = com.google.common.base.Charsets.UTF_8; // Noncompliant {{Replace "com.google.common.base.Charsets.UTF_8" with "StandardCharsets.UTF_8".}}

    // Canonical names of java.nio API and java.io/java.lang API
    Charset.forName("ISO-8859-1"); // Noncompliant {{Replace Charset.forName() call with StandardCharsets.ISO_8859_1}}
//  ^^^^^^^^^^^^^^^^^^^^^^^^^^^^^
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

    org.apache.commons.codec.Charsets.toCharset("UTF-8"); // Noncompliant {{Replace Charsets.toCharset() call with StandardCharsets.UTF_8}}
//  ^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^

    org.apache.commons.io.Charsets.toCharset("UTF-8"); // Noncompliant {{Replace Charsets.toCharset() call with StandardCharsets.UTF_8}}
//  ^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^

    org.apache.commons.io.IOUtils.toString(bytes, "UTF-8"); // Noncompliant {{Replace IOUtils.toString() call with new String(..., StandardCharsets.UTF_8);}}
//  ^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^
    org.apache.commons.io.IOUtils.toString(inputStream, "UTF-8"); // Noncompliant {{Replace charset name argument with StandardCharsets.UTF_8}}
//                                                      ^^^^^^^
    org.apache.commons.io.IOUtils.toString(uri, "UTF-8"); // Noncompliant {{Replace charset name argument with StandardCharsets.UTF_8}}
//                                              ^^^^^^^
    org.apache.commons.io.IOUtils.toString(url, "UTF-8"); // Noncompliant {{Replace charset name argument with StandardCharsets.UTF_8}}
//                                              ^^^^^^^

    "".getBytes("UTF-8"); // Noncompliant {{Replace charset name argument with StandardCharsets.UTF_8}}

    new String(bytes, org.apache.commons.lang.CharEncoding.UTF_8); // Noncompliant
    new String(bytes, offset, length, org.apache.commons.lang.CharEncoding.UTF_8); // Noncompliant

    new InputStreamReader(inputStream, org.apache.commons.lang.CharEncoding.UTF_8); // Noncompliant
    new OutputStreamWriter(outputStream, org.apache.commons.lang.CharEncoding.UTF_8); // Noncompliant

    new org.apache.commons.codec.binary.Hex("UTF-8"); // Noncompliant
    new org.apache.commons.codec.net.QuotedPrintableCodec("UTF-8"); // Noncompliant

    org.apache.commons.io.FileUtils.readFileToString(file, "UTF-8"); // Noncompliant
    org.apache.commons.io.FileUtils.readLines(file, "UTF-8"); // Noncompliant
    org.apache.commons.io.FileUtils.write(file, charSequence, "UTF-8"); // Noncompliant
    org.apache.commons.io.FileUtils.write(file, charSequence, "UTF-8", append); // Noncompliant
    org.apache.commons.io.FileUtils.writeStringToFile(file, dataString, "UTF-8"); // Noncompliant
//                                                                      ^^^^^^^
    org.apache.commons.io.FileUtils.writeStringToFile(file, dataString, "UTF-8", append); // Noncompliant
//                                                                      ^^^^^^^
    org.apache.commons.io.IOUtils.copy(inputStream, writer, "UTF-8"); // Noncompliant
    org.apache.commons.io.IOUtils.copy(reader, outputStream, "UTF-8"); // Noncompliant
    org.apache.commons.io.IOUtils.lineIterator(inputStream, "UTF-8"); // Noncompliant
    org.apache.commons.io.IOUtils.readLines(inputStream, "UTF-8"); // Noncompliant
    org.apache.commons.io.IOUtils.toByteArray(reader, "UTF-8"); // Noncompliant
    org.apache.commons.io.IOUtils.toCharArray(inputStream, "UTF-8"); // Noncompliant
    org.apache.commons.io.IOUtils.toInputStream(charSequence, "UTF-8"); // Noncompliant
    org.apache.commons.io.IOUtils.toInputStream(inputString, "UTF-8"); // Noncompliant
//                                                           ^^^^^^^
    org.apache.commons.io.IOUtils.write(bytes, writer, "UTF-8"); // Noncompliant
    org.apache.commons.io.IOUtils.write(chars, outputStream, "UTF-8"); // Noncompliant
    org.apache.commons.io.IOUtils.write(charSequence, outputStream, "UTF-8"); // Noncompliant
    org.apache.commons.io.IOUtils.write(dataString, outputStream, "UTF-8"); // Noncompliant
//                                                                ^^^^^^^
    org.apache.commons.io.IOUtils.write(stringBuffer, outputStream, "UTF-8"); // Noncompliant
    org.apache.commons.io.IOUtils.writeLines(collection, lineEndingString, outputStream, "UTF-8"); // Noncompliant
//                                                                                       ^^^^^^^
    new org.apache.commons.io.input.CharSequenceInputStream(charSequence, "UTF-8"); // Noncompliant
    new org.apache.commons.io.input.CharSequenceInputStream(charSequence, "UTF-8", bufferSize); // Noncompliant
    new org.apache.commons.io.input.ReaderInputStream(reader, "UTF-8"); // Noncompliant
    new org.apache.commons.io.input.ReaderInputStream(reader, "UTF-8", bufferSize); // Noncompliant
    new org.apache.commons.io.input.ReversedLinesFileReader(file, blockSize, "UTF-8"); // Noncompliant
    new org.apache.commons.io.output.LockableFileWriter(file, "UTF-8"); // Noncompliant
    new org.apache.commons.io.output.LockableFileWriter(file, "UTF-8", append, lockDirString); // Noncompliant
//                                                            ^^^^^^^
    new org.apache.commons.io.output.WriterOutputStream(writer, "UTF-8"); // Noncompliant
    new org.apache.commons.io.output.WriterOutputStream(writer, "UTF-8", bufferSize, writeImmediately); // Noncompliant

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
    charset = com.google.common.base.Charsets.ISO_8859_1; // Noncompliant [[quickfixes=qf1]]
//                                            ^^^^^^^^^^
    // fix@qf1 {{Replace with "StandardCharsets.ISO_8859_1"}}
    // edit@qf1 [[sc=15;ec=46]] {{StandardCharsets}}
    // edit@qf1 [[sl=13;sc=33;el=13;ec=33]] {{\nimport java.nio.charset.StandardCharsets;}}
    charset = com.google.common.base.Charsets.US_ASCII; // Noncompliant [[quickfixes=qf2]]
//                                            ^^^^^^^^
    // fix@qf2 {{Replace with "StandardCharsets.US_ASCII"}}
    // edit@qf2 [[sc=15;ec=46]] {{StandardCharsets}}
    // edit@qf2 [[sl=13;sc=33;el=13;ec=33]] {{\nimport java.nio.charset.StandardCharsets;}}
    charset = com.google.common.base.Charsets.UTF_16; // Noncompliant [[quickfixes=qf3]]
//                                            ^^^^^^
    // fix@qf3 {{Replace with "StandardCharsets.UTF_16"}}
    // edit@qf3 [[sc=15;ec=46]] {{StandardCharsets}}
    // edit@qf3 [[sl=13;sc=33;el=13;ec=33]] {{\nimport java.nio.charset.StandardCharsets;}}
    charset = com.google.common.base.Charsets.UTF_16BE; // Noncompliant [[quickfixes=qf4]]
//                                            ^^^^^^^^
    // fix@qf4 {{Replace with "StandardCharsets.UTF_16BE"}}
    // edit@qf4 [[sc=15;ec=46]] {{StandardCharsets}}
    // edit@qf4 [[sl=13;sc=33;el=13;ec=33]] {{\nimport java.nio.charset.StandardCharsets;}}
    charset = com.google.common.base.Charsets.UTF_16LE; // Noncompliant [[quickfixes=qf5]]
//                                            ^^^^^^^^
    // fix@qf5 {{Replace with "StandardCharsets.UTF_16LE"}}
    // edit@qf5 [[sc=15;ec=46]] {{StandardCharsets}}
    // edit@qf5 [[sl=13;sc=33;el=13;ec=33]] {{\nimport java.nio.charset.StandardCharsets;}}
    charset = com.google.common.base.Charsets.UTF_8; // Noncompliant [[quickfixes=qf6]]
//                                            ^^^^^
    // fix@qf6 {{Replace with "StandardCharsets.UTF_8"}}
    // edit@qf6 [[sc=15;ec=46]] {{StandardCharsets}}
    // edit@qf6 [[sl=13;sc=33;el=13;ec=33]] {{\nimport java.nio.charset.StandardCharsets;}}

    Charset.forName("ISO-8859-1"); // Noncompliant [[quickfixes=qf7]]
//  ^^^^^^^^^^^^^^^^^^^^^^^^^^^^^
    // fix@qf7 {{Replace with "StandardCharsets.ISO_8859_1"}}
    // edit@qf7 [[sc=5;ec=34]] {{StandardCharsets.ISO_8859_1}}
    // edit@qf7 [[sl=13;sc=33;el=13;ec=33]] {{\nimport java.nio.charset.StandardCharsets;}}
    Charset.forName("ISO_8859_1"); // Noncompliant [[quickfixes=qf8]]
//  ^^^^^^^^^^^^^^^^^^^^^^^^^^^^^
    // fix@qf8 {{Replace with "StandardCharsets.ISO_8859_1"}}
    // edit@qf8 [[sc=5;ec=34]] {{StandardCharsets.ISO_8859_1}}
    // edit@qf8 [[sl=13;sc=33;el=13;ec=33]] {{\nimport java.nio.charset.StandardCharsets;}}
    Charset.forName("UTF8"); // Noncompliant [[quickfixes=qf9]]
//  ^^^^^^^^^^^^^^^^^^^^^^^
    // fix@qf9 {{Replace with "StandardCharsets.UTF_8"}}
    // edit@qf9 [[sc=5;ec=28]] {{StandardCharsets.UTF_8}}
    // edit@qf9 [[sl=13;sc=33;el=13;ec=33]] {{\nimport java.nio.charset.StandardCharsets;}}
    Charset.forName("utf-8"); // Noncompliant [[quickfixes=qf10]]
//  ^^^^^^^^^^^^^^^^^^^^^^^^
    // fix@qf10 {{Replace with "StandardCharsets.UTF_8"}}
    // edit@qf10 [[sc=5;ec=29]] {{StandardCharsets.UTF_8}}
    // edit@qf10 [[sl=13;sc=33;el=13;ec=33]] {{\nimport java.nio.charset.StandardCharsets;}}
    Charset.forName("UTF-16LE"); // Noncompliant [[quickfixes=qf11]]
//  ^^^^^^^^^^^^^^^^^^^^^^^^^^^
    // fix@qf11 {{Replace with "StandardCharsets.UTF_16LE"}}
    // edit@qf11 [[sc=5;ec=32]] {{StandardCharsets.UTF_16LE}}
    // edit@qf11 [[sl=13;sc=33;el=13;ec=33]] {{\nimport java.nio.charset.StandardCharsets;}}
    Charset.forName("UnicodeLittleUnmarked"); // Noncompliant [[quickfixes=qf12]]
//  ^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^
    // fix@qf12 {{Replace with "StandardCharsets.UTF_16LE"}}
    // edit@qf12 [[sc=5;ec=45]] {{StandardCharsets.UTF_16LE}}
    // edit@qf12 [[sl=13;sc=33;el=13;ec=33]] {{\nimport java.nio.charset.StandardCharsets;}}
    org.apache.commons.codec.Charsets.toCharset("UTF-8"); // Noncompliant [[quickfixes=qf13]]
//  ^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^
    // fix@qf13 {{Replace with "StandardCharsets.UTF_8"}}
    // edit@qf13 [[sc=5;ec=57]] {{StandardCharsets.UTF_8}}
    // edit@qf13 [[sl=13;sc=33;el=13;ec=33]] {{\nimport java.nio.charset.StandardCharsets;}}

    org.apache.commons.io.IOUtils.toString(inputStream, "UTF-8"); // Noncompliant [[quickfixes=qf14]]
//                                                      ^^^^^^^
    // fix@qf14 {{Replace with "StandardCharsets.UTF_8"}}
    // edit@qf14 [[sc=57;ec=64]] {{StandardCharsets.UTF_8}}
    // edit@qf14 [[sl=13;sc=33;el=13;ec=33]] {{\nimport java.nio.charset.StandardCharsets;}}
    "".getBytes("UTF-8"); // Noncompliant [[quickfixes=qf15]]
//              ^^^^^^^
    // fix@qf15 {{Replace with "StandardCharsets.UTF_8"}}
    // edit@qf15 [[sc=17;ec=24]] {{StandardCharsets.UTF_8}}
    // edit@qf15 [[sl=13;sc=33;el=13;ec=33]] {{\nimport java.nio.charset.StandardCharsets;}}
    new String(bytes, offset, length, org.apache.commons.lang.CharEncoding.UTF_8); // Noncompliant [[quickfixes=qf16]]
//                                    ^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^
    // fix@qf16 {{Replace with "StandardCharsets.UTF_8"}}
    // edit@qf16 [[sc=39;ec=81]] {{StandardCharsets.UTF_8}}
    // edit@qf16 [[sl=13;sc=33;el=13;ec=33]] {{\nimport java.nio.charset.StandardCharsets;}}
    org.apache.commons.io.FileUtils.write(file, charSequence, "UTF-8"); // Noncompliant [[quickfixes=qf17]]
//                                                            ^^^^^^^
    // fix@qf17 {{Replace with "StandardCharsets.UTF_8"}}
    // edit@qf17 [[sc=63;ec=70]] {{StandardCharsets.UTF_8}}
    // edit@qf17 [[sl=13;sc=33;el=13;ec=33]] {{\nimport java.nio.charset.StandardCharsets;}}
     org.apache.commons.io.IOUtils.toCharArray(inputStream, "UTF-8"); // Noncompliant [[quickfixes=qf18]]
//                                                          ^^^^^^^
    // fix@qf18 {{Replace with "StandardCharsets.UTF_8"}}
    // edit@qf18 [[sc=61;ec=68]] {{StandardCharsets.UTF_8}}
    // edit@qf18 [[sl=13;sc=33;el=13;ec=33]] {{\nimport java.nio.charset.StandardCharsets;}}
    new org.apache.commons.io.input.ReaderInputStream(reader, "ISO-8859-1", bufferSize); // Noncompliant [[quickfixes=qf19]]
//                                                            ^^^^^^^^^^^^
    // fix@qf19 {{Replace with "StandardCharsets.ISO_8859_1"}}
    // edit@qf19 [[sc=63;ec=75]] {{StandardCharsets.ISO_8859_1}}
    // edit@qf19 [[sl=13;sc=33;el=13;ec=33]] {{\nimport java.nio.charset.StandardCharsets;}}
  }
}
