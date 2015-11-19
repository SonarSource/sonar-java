import java.io.ByteArrayOutputStream;
import java.io.FileInputStream;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.BufferedInputStream;
import java.io.BufferedWriter;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.io.Writer;
import java.io.FileReader;
import java.io.BufferedWriter;
import java.util.Formatter;

public class A {
  private final static int MAX_LOOP = 42;

  public void justToBeAbleToUseVerify() {
    FileInputStream stream = new FileInputStream("myFile"); // Noncompliant {{Close this "FileInputStream".}}
    stream.read();
   return;
  }

  // Foreign 80
  public void forLoopHandling(int maxLoop) {
    Reader reader = null;
    for (int i = 0; i < MAX_LOOP; i++) {
      reader = new FileReader(""); // Noncompliant {{Close this "FileReader".}}
    }
    reader.close();
  }

  // Foreign 115
  public void forEachLoopHandling(List<Object> objects) {
    Writer writer = null;
    for (Object object : objects) {
      writer = new FileWriter(""); // Noncompliant {{Close this "FileWriter".}}
    }
    writer.close();
  }

  public void methodDispatch(List<Object> objects) {
    FileInputStream stream = new FileInputStream("myFile"); // Compliant since can be closed in method call
    dispatch(stream);
  }

  public InputStream methodReturned(List<Object> objects) {
    FileInputStream stream = new FileInputStream("myFile"); // Compliant since resource is returned (and can be closed elsewhere)
    return stream;
  }

  // Foreign 128
  public void doWhile() {
    j = 0;
    FileInputStream fis = null;
    do {
      fis = new FileInputStream(""); // Noncompliant {{Close this "FileInputStream".}}
      j++;
    } while (j < MAX_LOOP);
    fis.close();
  }
  
  // Foreign 100
  void whileLoopWithCounter() {
    int j = 0;
    InputStream is = null;
    while (j < MAX_LOOP) {
      is = new FileInputStream(""); // Noncompliant {{Close this "FileInputStream".}}
      j++;
    }
    is.close();
  }

  // Foreign 366 & 369 (unexpected)
  void switchMultipleOpen() {
    Writer w7;
    switch (enumValue) {
      case A:
        w7 = new FileWriter("");
        break;
      default:
        w7 = new FileWriter("");
    }
    w7.close();
  }
  
  // Foreign 420
  void russianDollInTryHeader() {
    try (FileWriter fw = new FileWriter("")) { // Compliant - JLS8 - 14.20.3 : try-with-resources
      fw.write("hello");
    } catch (Exception e) {
      // ...
    }
  }
}