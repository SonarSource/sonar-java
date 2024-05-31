import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;

class NullDereferenceCheck {

  @SuppressWarnings("null")
  void S2259(String s) {
    if (s == null) {
      s.toString(); // NoIssue
    }
  }

  void S2259NotSuppressed(String s) {
    if (s == null) {
      s.toString(); // WithIssue
    }
  }
}

class UnclosedResources {

  @SuppressWarnings("resource")
  void S2093(String fileName) {
    FileReader fr = null;
    try { // NoIssue
      fr = new FileReader(fileName);
    } catch (Exception e) {
    } finally {
      if (fr != null) {
        try {
          fr.close();
        } catch (IOException e) {
        }
      }
    }
  }

  void S2093_not_suppressed(String fileName) {
    FileReader fr = null;
    try { // WithIssue
      fr = new FileReader(fileName);
    } catch (Exception e) {
    } finally {
      if (fr != null) {
        try {
          fr.close();
        } catch (IOException e) {
        }
      }
    }
  }

  @SuppressWarnings("resource")
  void S2095(String fileName) throws FileNotFoundException {
    FileReader fr = new FileReader(fileName); // NoIssue
    fr.toString();
  }

  void S2095_not_suppressed(String fileName) throws FileNotFoundException {
    FileReader fr = new FileReader(fileName); // WithIssue
    fr.toString();
  }
}
