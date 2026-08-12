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

    "".getBytes("UTF-8"); // Noncompliant

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

    Charset.forName("ISO-8859-1"); // Noncompliant

    Charset.forName("ISO_8859_1"); // Noncompliant

    Charset.forName("UTF8"); // Noncompliant

    Charset.forName("utf-8"); // Noncompliant

    Charset.forName("UTF-16LE"); // Noncompliant

    Charset.forName("UnicodeLittleUnmarked"); // Noncompliant

    "".getBytes("UTF-8"); // Noncompliant

  }
}
