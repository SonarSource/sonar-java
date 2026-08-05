package checks;

import java.io.BufferedInputStream;
import java.io.FileInputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.Reader;

class StreamReadResultCastCheckSample {

  void byteCastInWhileLoop(FileInputStream fis) throws IOException {
    byte b;
    while ((b = (byte) fis.read()) != -1) { // Noncompliant {{Store the return value of "read()" in an "int" variable and check for -1 before casting.}}
      process(b);
    }
  }

  void byteCastAssignment(InputStream is) throws IOException {
    byte value = (byte) is.read(); // Noncompliant
  }

  void charCastFromInputStream(InputStream is) throws IOException {
    char c;
    while ((c = (char) is.read()) != -1) { // Noncompliant
      process(c);
    }
  }

  void byteCastInDoWhile(FileInputStream fis) throws IOException {
    byte b;
    do {
      b = (byte) fis.read(); // Noncompliant
    } while (b != -1);
  }

  void charCastFromReader(FileReader reader) throws IOException {
    char c;
    while ((c = (char) reader.read()) != -1) { // Noncompliant
      process(c);
    }
  }

  void charCastAssignmentFromReader(Reader reader) throws IOException {
    char c = (char) reader.read(); // Noncompliant
  }

  void byteCastWithParentheses(InputStream is) throws IOException {
    byte b = (byte) (is.read()); // Noncompliant
  }

  void byteCastFromSubtype(BufferedInputStream bis) throws IOException {
    byte b = (byte) bis.read(); // Noncompliant
  }

  // Compliant cases

  void correctPatternWithIntVariable(FileInputStream fis) throws IOException {
    int data;
    while ((data = fis.read()) != -1) {
      byte b = (byte) data;
      process(b);
    }
  }

  void multiArgReadByteArray(InputStream is) throws IOException {
    byte[] buffer = new byte[1024];
    int bytesRead = is.read(buffer);
  }

  void multiArgReadByteArrayWithOffset(InputStream is) throws IOException {
    byte[] buffer = new byte[1024];
    int bytesRead = is.read(buffer, 0, 1024);
  }

  void noCastAtAll(InputStream is) throws IOException {
    int value = is.read();
  }

  void multiArgReadCharArray(Reader reader) throws IOException {
    char[] buffer = new char[1024];
    int charsRead = reader.read(buffer);
  }

  void correctPatternWithReader(Reader reader) throws IOException {
    int data;
    while ((data = reader.read()) != -1) {
      char c = (char) data;
      process(c);
    }
  }

  void wideningCast(InputStream is) throws IOException {
    long value = (long) is.read();
  }

  void customReadMethod() {
    CustomReader custom = new CustomReader();
    byte b = (byte) custom.read();
  }

  private void process(byte b) {}
  private void process(char c) {}

  static class CustomReader {
    int read() {
      return 0;
    }
  }
}
