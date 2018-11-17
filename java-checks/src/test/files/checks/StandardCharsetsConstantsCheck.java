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
    c = Charsets.ISO_8859_1; // Noncompliant {{Replace "com.google.common.base.Charsets.ISO_8859_1" with "StandardCharsets.ISO_8859_1".}} [[sc=18;ec=28]]
    c = Charsets.US_ASCII; // Noncompliant {{Replace "com.google.common.base.Charsets.US_ASCII" with "StandardCharsets.US_ASCII".}}
    c = Charsets.UTF_16; // Noncompliant {{Replace "com.google.common.base.Charsets.UTF_16" with "StandardCharsets.UTF_16".}}
    c = Charsets.UTF_16BE; // Noncompliant {{Replace "com.google.common.base.Charsets.UTF_16BE" with "StandardCharsets.UTF_16BE".}}
    c = Charsets.UTF_16LE; // Noncompliant {{Replace "com.google.common.base.Charsets.UTF_16LE" with "StandardCharsets.UTF_16LE".}}
    c = Charsets.UTF_8; // Noncompliant {{Replace "com.google.common.base.Charsets.UTF_8" with "StandardCharsets.UTF_8".}}

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

    "".getBytes("UTF-8"); // Noncompliant {{Replace charset name argument with StandardCharsets.UTF_8}}

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
