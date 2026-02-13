package checks;

import java.io.BufferedReader;
import java.io.IO;
import java.io.InputStreamReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.Reader;

class BufferedReaderBoilerplateCheckSample {
  void noncompliantBasic() throws IOException {
    BufferedReader reader = new BufferedReader(new InputStreamReader(System.in)); // Noncompliant {{Use "IO.readln()" instead of this "BufferedReader" boilerplate.}}
    String line = reader.readLine();
  }

  void compliantBasic() throws IOException {
    String line = IO.readln();
  }

  void noncompliantWithPrompt() throws IOException {
    System.out.print("Enter text: ");
    BufferedReader reader = new BufferedReader(new InputStreamReader(System.in)); // Noncompliant
    String line = reader.readLine();
  }

  void noncompliantInTryCatch() {
    try {
      BufferedReader reader = new BufferedReader(new InputStreamReader(System.in)); // Noncompliant
      String line = reader.readLine();
    } catch (IOException ioe) {
      ioe.printStackTrace();
    }
  }

  void noncompliantInTryWithResources() throws IOException {
    try (BufferedReader reader = new BufferedReader(new InputStreamReader(System.in))) { // Noncompliant
      String line = reader.readLine();
    }
  }

  void noncompliantInlineUsage() throws IOException {
    String line = new BufferedReader(new InputStreamReader(System.in)).readLine(); // Noncompliant
  }

  void compliantWithFileStream(InputStream fileStream) throws IOException {
    BufferedReader reader = new BufferedReader(new InputStreamReader(fileStream)); // Compliant - not wrapping System.in
    String line = reader.readLine();
  }

  void compliantWithExistingReader(Reader existingReader) throws IOException {
    BufferedReader reader = new BufferedReader(existingReader); // Compliant - not wrapping System.in via InputStreamReader
    String line = reader.readLine();
  }

  void compliantNoSystemIn() throws IOException {
    InputStream other = new java.io.ByteArrayInputStream(new byte[0]);
    BufferedReader reader = new BufferedReader(new InputStreamReader(other)); // Compliant - not System.in
    String line = reader.readLine();
  }
}
