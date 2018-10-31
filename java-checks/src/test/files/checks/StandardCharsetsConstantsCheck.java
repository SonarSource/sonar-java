import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;

import com.google.common.base.Charsets;

import org.apache.commons.io.IOUtils;
import org.apache.commons.io.FileUtils;

class A {

  void myMethod(byte[] bytes, int offset, int length, InputStream inputStream, OutputStream outputStream, String charsetName) {
    Charset c;
    c = Charsets.ISO_8859_1; // Noncompliant
    c = Charsets.US_ASCII; // Noncompliant
    c = Charsets.UTF_16; // Noncompliant
    c = Charsets.UTF_16BE; // Noncompliant
    c = Charsets.UTF_16LE; // Noncompliant
    c = Charsets.UTF_8; // Noncompliant

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

    "".getBytes("UTF-8"); // Noncompliant

    new String(bytes, org.apache.commons.lang.CharEncoding.UTF_8); // Noncompliant
    new String(bytes, offset, length, org.apache.commons.lang.CharEncoding.UTF_8); // Noncompliant

    new InputStreamReader(inputStream, org.apache.commons.lang.CharEncoding.UTF_8); // Noncompliant
    new OutputStreamWriter(outputStream, org.apache.commons.lang.CharEncoding.UTF_8); // Noncompliant

    // Compliant
    c = StandardCharsets.ISO_8859_1;
    c = StandardCharsets.US_ASCII;
    c = StandardCharsets.UTF_16;
    c = StandardCharsets.UTF_16BE;
    c = StandardCharsets.UTF_16LE;
    c = StandardCharsets.UTF_8;

    "".getBytes(charsetName);
    "".getBytes("Windows-1252");

    Charset charset = Charset.forName(charsetName);
    "".getBytes(charset);

    new String(bytes, charsetName);
    new String(bytes, offset, length, charsetName);

    new InputStreamReader(inputStream, charsetName);
    new OutputStreamWriter(outputStream, charsetName);
  }
}
