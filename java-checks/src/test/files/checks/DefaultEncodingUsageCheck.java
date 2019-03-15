import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.Reader;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.PrintStream;
import java.io.PrintWriter;
import java.util.Formatter;
import java.util.Scanner;
import java.util.Collection;


import org.apache.commons.io.IOUtils;
import org.apache.commons.io.FileUtils;

class A {
  void myMethod(byte[] bytes, java.io.File file, OutputStream outputStream) {
    new String(bytes); // Noncompliant {{Remove this use of constructor "String(byte[])"}}
    new String(bytes, 0, 1); // Noncompliant {{Remove this use of constructor "String(byte[],int,int)"}}
    "".getBytes(); // Noncompliant {{Remove this use of "getBytes"}}
    "".getBytes(0, 0, bytes, 0); // Noncompliant {{Remove this use of "getBytes"}}
    new java.io.ByteArrayOutputStream().toString(); // Noncompliant
    new FileReader("fileName"); // Noncompliant
    new FileReader(file); // Noncompliant [[sc=9;ec=19]]
    new FileReader(new java.io.FileDescriptor()); // Noncompliant
    new FileWriter(file); // Noncompliant
    new FileWriter(file, true); // Noncompliant
    new FileWriter(new java.io.FileDescriptor()); // Noncompliant
    new FileWriter("fileName"); // Noncompliant
    new FileWriter("fileName", true); // Noncompliant
    new FileWriter("fileName", StandardCharsets.UTF_8, true); // java 11 - not resolved if not build with java 11, valid
    new InputStreamReader(new java.io.FileInputStream("")); // Noncompliant
    new OutputStreamWriter(outputStream); // Noncompliant
    new PrintStream(file); // Noncompliant
    new PrintStream(outputStream); // Noncompliant
    new PrintStream(outputStream, true); // Noncompliant
    new PrintStream("fileName"); // Noncompliant
    new PrintWriter(file); // Noncompliant
    new PrintWriter(outputStream); // Noncompliant
    new PrintWriter(outputStream, true); // Noncompliant
    new PrintWriter("fileName"); // Noncompliant
    new Formatter(""); // Noncompliant
    new Formatter(file); // Noncompliant
    new Formatter(outputStream); // Noncompliant
    new Scanner(file); // Noncompliant
    new java.util.Scanner(new java.io.FileInputStream("")); // Noncompliant
    FileReader reader = null; // Noncompliant
    FileReader reader3 = new FileReader(""); // Noncompliant
    java.io.Reader reader2 = fileReader(); // Noncompliant [[sc=30;ec=40]]
    FileWriter writer = null; // Noncompliant
    java.io.Writer writer2 = fileWriter(); // Noncompliant

    // Compliant
    new String("");
    "".length();
    new java.io.ByteArrayOutputStream().toString("UTF-8");
    UnknownClass unknown;
  }
  
  FileReader fileReader() {
    return null;
  }
  
  FileWriter fileWriter() {
    return null;
  }

  void commons_io(Reader reader, Writer writer, InputStream input, OutputStream output, String s, CharSequence charSequence, byte[] bytes,
                  java.net.URI uri, java.net.URL url, char[] chars, StringBuffer buffer, Collection<?> lines) {
    IOUtils.copy(input, writer); // Noncompliant
    IOUtils.copy(reader, output); // Noncompliant
    IOUtils.readLines(input); // Noncompliant
    IOUtils.toByteArray(reader); // Noncompliant
    IOUtils.toCharArray(input); // Noncompliant
    IOUtils.toInputStream(charSequence); // Noncompliant
    IOUtils.toInputStream(s); // Noncompliant
    IOUtils.toString(bytes); // Noncompliant
    IOUtils.toString(uri); // Noncompliant
    IOUtils.toString(url); // Noncompliant
    IOUtils.write(chars, output); // Noncompliant
    IOUtils.write(charSequence, output); // Noncompliant
    IOUtils.write(buffer, output); // Noncompliant
    IOUtils.write(s, output); // Noncompliant
    IOUtils.writeLines(lines, "\n", output); // Noncompliant
  }

  void commons_fileutils(File file, CharSequence charSequence) {
    FileUtils.readFileToString(file); // Noncompliant
    FileUtils.readLines(file); // Noncompliant
    FileUtils.write(file, charSequence); // Noncompliant
    FileUtils.write(file, charSequence, false); // Noncompliant
    FileUtils.writeStringToFile(file, "data"); // Noncompliant
  }

  void commons_io_with_null(Reader reader, Writer writer, InputStream input, OutputStream output, String s, CharSequence charSequence, byte[] bytes,
                  java.net.URI uri, java.net.URL url, char[] chars, StringBuffer buffer, Collection<?> lines) {
    IOUtils.copy(input, writer, (String) null); // Noncompliant
    IOUtils.copy(input, writer, ((String) (((null))))); // Noncompliant
    IOUtils.copy(reader, output, (String) null); // Noncompliant
    IOUtils.readLines(input, (String) null); // Noncompliant
    IOUtils.toByteArray(reader, (String) null); // Noncompliant
    IOUtils.toCharArray(input, (String) null); // Noncompliant
    IOUtils.toInputStream(charSequence, (String) null); // Noncompliant
    IOUtils.toInputStream(s, (String) null); // Noncompliant
    IOUtils.toString(bytes, (String) null); // Noncompliant
    IOUtils.toString(uri, (String) null); // Noncompliant
    IOUtils.toString(url, (String) null); // Noncompliant
    IOUtils.write(chars, output, (String) null); // Noncompliant
    IOUtils.write(charSequence, output, (String) null); // Noncompliant
    IOUtils.write(buffer, output, (String) null); // Noncompliant
    IOUtils.write(s, output, (String) null); // Noncompliant
    IOUtils.writeLines(lines, "\n", output, (String) null); // Noncompliant
  }

  void commons_fileutils_with_null(File file, CharSequence charSequence) {
    FileUtils.readFileToString(file, (java.nio.charset.Charset) null); // Noncompliant
    FileUtils.readLines(file, (java.nio.charset.Charset) null); // Noncompliant
    FileUtils.write(file, charSequence, (java.nio.charset.Charset) null); // Noncompliant
    FileUtils.write(file, charSequence, (java.nio.charset.Charset) null, false); // Noncompliant
    FileUtils.writeStringToFile(file, "data", (java.nio.charset.Charset) null); // Noncompliant
  }


}
