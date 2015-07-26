import org.springframework.context.support.AbstractApplicationContext;

import com.sun.org.apache.bcel.internal.util.ByteSequence;
import com.sun.org.apache.xml.internal.security.utils.UnsyncByteArrayOutputStream;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.Closeable;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.RandomAccessFile;
import java.io.Reader;
import java.io.Writer;
import java.lang.Exception;
import java.util.Collection;
import java.util.Formatter;

class A {
  private enum MyEnum {
    A, B, C;
  }

  private final static int MAX_LOOP = 42;
  private BufferedReader br;

  void closeable_never_closed() throws Exception {

    // Noncompliant@+1 {{Close this "FileReader".}}
    Reader reader = new FileReader(""); // Reader is not used and not closed

    // Noncompliant@+1 {{Close this "FileWriter".}}
    Writer writer = new FileWriter(""); // Writer is not closed
    writer.write(10);

    InputStream is;
    is = new FileInputStream(""); // Compliant
    is.close();
    // Noncompliant@+1 {{Close this "FileInputStream".}}
    is = new FileInputStream(""); // InputStream reinitialized and not closed

    // Noncompliant@+1 {{Close this "RandomAccessFile".}}
    RandomAccessFile raf = new RandomAccessFile("", "r"); // RandomAccessFile reinitialized before closing
    raf.length();
    raf = new RandomAccessFile("", "r"); // Compliant
    raf.close();
  }

  Closeable closeable_never_closed_but_state_is_lost(Closeable closeable) throws Exception {
    Formatter formatter = new Formatter(); // Compliant - (unknown) as Closeable instance is later used as method parameter
    myMethod(formatter);

    FileInputStream fis = getFileInputStream(); // Compliant - Closeable is retrieved but then used as argument
    fis.available();
    myMethod(fis);

    br = new BufferedReader(new FileReader("")); // Compliant - only look at local variables

    BufferedWriter bw = new BufferedWriter(new FileWriter("")); // Compliant - closeable state is lost
    Object o = getObject(bw);

    closeable = new FileReader(""); // Compliant - responsability of closing the variable is not in the method

    OutputStream ubaos = new UnsyncByteArrayOutputStream(); // Compliant - UnsyncByteArrayOutputStream does not implements close()
    ubaos.write(0);

    Reader reader = new BufferedReader(br); // Compliant - uses a field

    RandomAccessFile raf = new RandomAccessFile("", "r"); // Compliant - Closeable is returned so its state is unknown
    return (Closeable) raf;
  }

  void closeable_not_closed_in_loops(Collection<Object> objects, Collection<String> propertyList) throws Exception {
    Reader reader = null;
    for (int i = 0; i < MAX_LOOP; i++) {
      reader = new FileReader(""); // Noncompliant {{Close this "FileReader".}}
    }
    reader.close();

    Reader reader2 = null;
    for (int i = 0; i < MAX_LOOP; i++) {
      reader2 = new FileReader(""); // Compliant
      reader2.close();
    }

    Reader reader3 = null;
    for (;;) {
      reader3 = new FileReader(""); // Compliant
      reader3.close();
    }

    int j = 0;

    InputStream is = null;
    while (j < MAX_LOOP) {
      is = new FileInputStream(""); // Noncompliant {{Close this "FileInputStream".}}
      j++;
    }
    is.close();

    j = 0;
    InputStream is2 = null;
    while (j < MAX_LOOP) {
      is2 = new FileInputStream(""); // Compliant
      is2.close();
      j++;
    }

    Writer writer = null;
    for (Object object : objects) {
      writer = new FileWriter(""); // Noncompliant {{Close this "FileWriter".}}
    }
    writer.close();

    Writer writer2 = null;
    for (Object object : objects) {
      writer2 = new FileWriter(""); // Compliant
      writer2.close();
    }

    j = 0;
    FileInputStream fis = null;
    do {
      fis = new FileInputStream(""); // Noncompliant {{Close this "FileInputStream".}}
      j++;
    } while (j < MAX_LOOP);
    fis.close();

    j = 0;
    FileInputStream fis2 = null;
    do {
      fis2 = new FileInputStream(""); // Compliant
      fis2.close();
      j++;
    } while (j < MAX_LOOP);

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

    OutputStream stream2 = null;
    try {
      stream2 = new FileOutputStream("myfile.txt"); // Compliant
      for (String property : propertyList) {
        // ...
      }
    } catch (Exception e) {
      // ...
    } finally {
      stream2.close();
    }
  }

  void closeable_not_closed_in_every_paths_when_using_if(boolean test, int x) throws Exception {
    Reader reader = new FileReader(""); // Noncompliant {{Close this "FileReader".}}
    if (test) {
      reader.close();
    }

    Reader reader2 = new FileReader(""); // Noncompliant {{Close this "FileReader".}}
    if (test) {
    } else {
      reader2.close();
    }

    Reader reader3 = new FileReader(""); // Noncompliant {{Close this "FileReader".}}
    if (test) {
      reader3 = new FileReader(""); // Noncompliant {{Close this "FileReader".}}
    } else {
      reader3 = new FileReader(""); // Noncompliant {{Close this "FileReader".}}
    }
    // Noncompliant@+1
    Writer writer = new FileWriter(""); // One branch is missing
    if (test) {
      writer.close();
    } else {
      if (x > 0) {
        // Noncompliant@+1
        Formatter formatter = new Formatter(); // Only used in the local then clause and not closed
        formatter.out();
        writer.close();
      }
    }

    BufferedWriter bw;
    if (test) {
      bw = new BufferedWriter(new FileWriter("")); // Noncompliant {{Close this "BufferedWriter".}}
    } else {
      bw = new BufferedWriter(new FileWriter(""));// Noncompliant {{Close this "BufferedWriter".}}
    }

    FileInputStream fis; // Not closed in else branch
    if (test) {
      fis = new FileInputStream("");
      fis.close();
    } else {
      fis = new FileInputStream(""); // Noncompliant {{Close this "FileInputStream".}}
    }

    FileInputStream fis1; // Compliant - has an unknown status
    if (test) {
      fis1 = new FileInputStream(""); // OPEN
      myMethod(fis1); // UNKNOWN
    } else {
      fis1 = getFileInputStream(); // UNKNOWN
    }

    FileInputStream fis2; // Compliant - has an unknown status
    if (test) {
      fis2 = getFileInputStream(); // UNKNOWN
      myMethod(fis2);
    } else {
      fis2 = new FileInputStream(""); // Noncompliant {{Close this "FileInputStream".}}
    }

    FileInputStream fis3; // Not closed after the if
    if (test) {
      fis3 = new FileInputStream(""); // Noncompliant {{Close this "FileInputStream".}}
    } else {
    }

    FileInputStream fis4; // Not closed after the if
    if (test) {
    } else {
      fis4 = new FileInputStream(""); // Noncompliant {{Close this "FileInputStream".}}
    }

    InputStream is = new FileInputStream(""); // Compliant
    if (test) {
      is.close();
    } else {
      is.close();
      if (x > 0) {
        Formatter formatter = new Formatter(); // Compliant
        formatter.close();
      }
    }
  }

  void closeable_not_closed_in_every_paths_when_using_switch(MyEnum enumValue) throws Exception {
    FileWriter fw = new FileWriter(""); // False-negative : NonCompliant
    switch (enumValue) {
      case A:
        // known engine limitation: break statements are ignored when within other statements
        if ("UTF-8".equals(fw.getEncoding())) {
          fw.close();
          break; // exit switch case with fw = CLOSED
        } else {
          fw.write("");
          break; // exit switch case with fw = OPEN --> should be non compliant
        }
      default:
        fw.close();
        break;
    }

    Reader reader = new FileReader(""); // Compliant
    switch (enumValue) {
      case A:
        reader.close();
        break;
      case B:
        reader.close();
        break;
      default:
        reader.close();
        break;
    }

    Formatter formatter = new Formatter(); // Noncompliant {{Close this "Formatter".}}
    switch (enumValue) {
      case A:
        formatter.close();
        break;
      case B:
        formatter.flush();
        break;
      default:
        formatter.close();
        break;
    }

    OutputStream os = new FileOutputStream(""); // Compliant - (unknown) as Closeable instance is later used as method parameter
    switch (enumValue) {
      case A:
        myMethod(os);
        break;
      default:
        os.close();
        break;
    }

    Writer w1 = new FileWriter(""); // Compliant
    switch (enumValue) {
      case A:
        w1.write("A");
      default:
        w1.close();
        break;
    }

    Writer w2 = new FileWriter(""); // Compliant
    switch (enumValue) {
      case A:
        w2.write("A");
        break;
      default:
        w2.flush();
        break;
    }
    w2.close();

    Writer w3 = new FileWriter(""); // Noncompliant {{Close this "FileWriter".}}
    switch (enumValue) {
    // as there is no "default", we can not guarantee that the closeable is closed
      case A:
        w3.close();
        break;
    }

    Writer w4 = new FileWriter(""); // Compliant
    switch (enumValue) {
      case A:
        w4.write("A");
      default:
        w4.close();
        break;
    }

    Writer w5;
    switch (enumValue) {
      case A:
        w5 = new FileWriter(""); // Noncompliant {{Close this "FileWriter".}}
      default:
        w5 = new FileWriter("");
        break;
    }
    w5.close();

    Writer w6; // Compliant
    switch (enumValue) {
      case A:
        w6 = new FileWriter("");
        break;
      default:
        w6 = new FileWriter("");
        break;
    }
    w6.close();

    Writer w7; // Compliant (last statement does not contain a break)
    switch (enumValue) {
      case A:
        w7 = new FileWriter("");
        break;
      default:
        w7 = new FileWriter("");
    }
    w7.close();

    Writer w8; // Compliant (empty switch)
    switch (enumValue) {
    }

    Writer w9; // Compliant (no statements)
    switch (enumValue) {
      case A:
    }
  }

  void closeable_used_in_try_with_resource() throws Exception {
    InputStream is = new FileInputStream(""); // Noncompliant
    try {
      is.close();
    } catch (IOException e) {

    } catch (Exception e) {
      is.close();
    }

    Reader reader; // Compliant
    try {
      reader = new FileReader("");
    } catch (Exception e) {
      reader = new FileReader("");
    }
    reader.close();

    Writer writer; // Compliant
    try {
      writer = new FileWriter("");
    } catch (Exception e) {

    } finally {
      writer = new FileWriter("");
    }
    writer.close();

    FileWriter fileWriter = new FileWriter(""); // Compliant
    try {
      fileWriter.flush();
    } catch (Exception e) {

    } finally {
      fileWriter.close();
    }

    try (BufferedReader br = new BufferedReader(new FileReader(""))) { // Compliant - JLS8 - 14.20.3 : try-with-resources
      // ...
    } catch (Exception e) {
      // ...
    }

    try (FileWriter fw = new FileWriter("")) { // Compliant - JLS8 - 14.20.3 : try-with-resources
      fw.write("hello");
    } catch (Exception e) {
      // ...
    }

    try {
      FileInputStream fis = new FileInputStream(""); // Compliant
      try {
      } finally {
        fis.close();
      }
    } catch (Exception e) {
    }

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

    try {
      InputStream is3 = new FileInputStream("");
      try {
        myMethod(is3);
      } finally {
        is3.read();
      }
    } catch (Exception e) {

    }
  }

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

  void myMethod(Closeable closeable) {
  }

  FileInputStream getFileInputStream() throws Exception {
    return new FileInputStream("");
  }

  Object getObject(Closeable closeable) {
    return new Object();
  }
}

abstract class B {
  static final int MAGIC_NUMBER = 42;

  abstract void foo();

  void bar(Closeable closeable) throws Exception {
    int a = MAGIC_NUMBER;
    int b;
    b = MAGIC_NUMBER;
    InputStream[] streams = new InputStream[5];
    streams[0] = new FileInputStream("");
    streams[0].close();
    getInputStream().close();
    closeable.close();
    bar(closeable);
    gul(MAGIC_NUMBER);
  }

  void gul(int number) {
  }

  InputStream getInputStream() {
    return null;
  }
}

class MyCloseable implements Closeable {
  void foo() {
    close();
  }

  public void close() {
  }

  void if_nested_in_for() {
    for (;;) {
      if (true) {
        FileInputStream auxinput = new FileInputStream();
        auxinput.close();
      }
    }

    for (;;) {
      if (true) {
        FileInputStream auxinput = new FileInputStream();
        auxinput.close();
      } else {
        FileInputStream auxinput = new FileInputStream(); // Noncompliant
      }
    }
  }

  void closeableSymbol() {
    ByteSequence stream = new ByteSequence(code);
      try {
        for(;;) {
          codeToString(stream, constant_pool, verbose);
        }
      }catch (Exception e) {

      }
  }
}

class Spring {
  void foo() {
    AbstractApplicationContext myAppContext = new MyContext(); // Compliant
    myAppContext.registerShutdownHook(); // will be closed on JVM shutdown unless it has already been closed at that time.
  }
  
  class MyContext extends AbstractApplicationContext {
  }
}
