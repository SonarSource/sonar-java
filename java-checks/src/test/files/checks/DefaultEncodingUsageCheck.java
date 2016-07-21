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
    new InputStreamReader(new java.io.FileInputStream("")); // Noncompliant
    new OutputStreamWriter(outputStream); // Noncompliant
    new PrintStream(file); // Noncompliant
    new PrintStream(outputStream); // Noncompliant
    new PrintStream(outputStream, true); // Noncompliant
    new PrintStream("fileName"); // Noncompliant
    new PrintWriter(file); // Noncompliant
    new PrintWriter(outputStream); // Compliant - Responsability of the underlying stream to define the correct encoding
    new PrintWriter(outputStream, true); // Compliant - Responsability of the underlying stream to define the correct encoding
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
}
