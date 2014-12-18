import java.io.FileReader;
import java.io.FileWriter;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.PrintStream;
import java.io.PrintWriter;
import java.util.Formatter;
import java.util.Scanner;

class A {
  void myMethod(byte[] bytes, java.io.File file, OutputStream outputStream) {
    // Noncompliant
    new String(bytes);
    new String(bytes, 0, 1);
    "".getBytes();
    "".getBytes(0, 0, bytes, 0);
    new java.io.ByteArrayOutputStream().toString();
    new FileReader("fileName");
    new FileReader(file);
    new FileReader(new java.io.FileDescriptor());
    new FileWriter(file);
    new FileWriter(file, true);
    new FileWriter(new java.io.FileDescriptor());
    new FileWriter("fileName");
    new FileWriter("fileName", true);
    new InputStreamReader(new java.io.FileInputStream(""));
    new OutputStreamWriter(outputStream);
    new PrintStream(file);
    new PrintStream(outputStream);
    new PrintStream(outputStream, true);
    new PrintStream("fileName");
    new PrintWriter(file);
    new PrintWriter(outputStream);
    new PrintWriter(outputStream, true);
    new PrintWriter("fileName");
    new Formatter("");
    new Formatter(file);
    new Formatter(outputStream);
    new Scanner(file);
    new java.util.Scanner(new java.io.FileInputStream(""));
    FileReader reader = null;
    FileReader reader = new FileReader(""); // we should not raise 2 issues
    java.io.Reader reader2 = fileReader();
    FileWriter writer = null;
    java.io.Writer writer2 = fileWriter();

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
}
