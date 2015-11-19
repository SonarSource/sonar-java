import java.io.ByteArrayOutputStream;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.BufferedInputStream;
import java.io.BufferedWriter;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.Reader;
import java.io.Writer;
import java.io.FileReader;
import java.io.BufferedWriter;
import java.util.Formatter;

public class A {
  
  // Expected: 32, 81, 42, 94
  // Unexpected: 104, 115, 117
  private final static int MAX_LOOP = 42;

  public void justToBeAbleToUseVerify() {
    FileInputStream stream = new FileInputStream("myFile"); // Noncompliant {{Close this "FileInputStream".}}
    stream.read();
   return;
  }
  
  // Foreign 325
  public void switchUse(int key) {
    Reader reader = new FileReader(""); // Noncompliant {{Close this "FileReader".}}
    switch (key) {
      case 0:
        reader.close();
        break;
    }
  }
  
  // Foreign 384
  void closeable_used_in_try_with_resource() throws Exception {
    InputStream is = new FileInputStream(""); // Noncompliant {{Close this "FileInputStream".}}
    try {
      is.close();
    } catch (IOException e) {

    } catch (Exception e) {
      is.close();
    }
  }
  
  // Foreign 144
  void loopAndTryCatch() {
    OutputStream stream = null;
    try {
      for (String property : propertyList) {
        stream = new FileOutputStream("myfile.txt"); // Noncompliant {{Close this "FileOutputStream".}}
        // ...
      }
    } catch (Exception e) {
      // ...
    } finally {
      stream.close(); // Multiple stream were opened. Only the last is closed.
    }
  }
  
  // Foreign 554
  void if_nested_in_for() {
    for (int i = 0; i < 4; i++) {
      if (true) {
        FileInputStream auxinput = new FileInputStream();
        auxinput.close();
      }
    }

    for (int i = 0; i < 4; i++) {
      if (i > 2) {
        FileInputStream auxinput = new FileInputStream();
        auxinput.close();
      } else {
        FileInputStream auxinput = new FileInputStream(); // Noncompliant
      }
    }
  }
  
  // Foreign 447
  void closeInCatch() {
    InputStream is2 = new FileInputStream("");
    try {
      is2.close();
    } catch (Exception e) {
      is2.close();
    } finally {
      InputStream is2 = new FileInputStream(""); // Noncompliant
      is2.read();
    }
    is2.close();
  }
  
  // Foreign 403
  void openInFinally() {
    Writer writer; // Compliant
    try {
      writer = new FileWriter("");
    } catch (Exception e) {

    } finally {
      writer = new FileWriter("");
    }
    writer.close();
  }
  
  // Foreign 465 & 467
  InputStream closeable_used_in_anonymous_or_inner_classes_should_be_ignored() throws Exception {
    final InputStream is = new FileInputStream(""); // Compliant - Closeable is used in returned anonymous class (simplification: "final"
                                                    // closeable are ignored)
    final OutputStream os = new FileOutputStream(""); // Compliant - Closeable is used in inner class (simplification: "final" closeable are
                                                      // ignored)

    class MyClass {
      void doSomething() throws IOException {
        os.read();
        os.close();
      }
    }

    new MyClass().doSomething();

    return new InputStream() {

      @Override
      public void close() throws IOException {
        is.close();
        super.close();
      }

      @Override
      public int read() throws IOException {
        is.close();
        return 0;
      }
    };
  }
}