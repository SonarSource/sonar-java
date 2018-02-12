import org.apache.commons.io.IOUtils;

import javax.annotation.Nonnull;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

import java.io.*;
import java.nio.file.*;
import java.util.Formatter;
import java.util.jar.JarFile;

public class A {
  private final static int MAX_LOOP = 42;
  public void fairHandling() {
    FileInputStream stream = new FileInputStream("myFile");
    try {
      stream.read();
    } finally {
      stream.close();
    }
  }

  public void wrongHandling() {
    FileInputStream stream = new FileInputStream("myFile"); // Noncompliant {{Use try-with-resources or close this "FileInputStream" in a "finally" clause.}}
    stream.read();
  }

  public void toleratedHandling() {
    ByteArrayOutputStream stream = new ByteArrayOutputStream();
    stream.write('c');
  }

  public void extendedIgnoredType() {
    FastByteArrayOutputStream stream = new FastByteArrayOutputStream(); // Compliant
    stream.write('c');
  }

  private static class FastByteArrayOutputStream extends ByteArrayOutputStream { }

  public void whileLoopHandling() {
    FileInputStream stream = new FileInputStream("myFile"); // Noncompliant {{Use try-with-resources or close this "FileInputStream" in a "finally" clause.}}
    while(needsMore()) {
      stream = new FileInputStream("myFile"); // Noncompliant {{Use try-with-resources or close this "FileInputStream" in a "finally" clause.}}
      stream.read(); // can fail
      stream.close();
    }
   return;
  }

  public void wrongLoopHandling() {
    int i = 0;
    FileInputStream stream = new FileInputStream("WhileFile"); // Noncompliant {{Use try-with-resources or close this "FileInputStream" in a "finally" clause.}}
    while(needsMore()) {
      i += 1;
      stream = new FileInputStream("WhileFile"+i); // Noncompliant {{Use try-with-resources or close this "FileInputStream" in a "finally" clause.}}
    }
    stream.close();
  }

  public void correctHandling() {
    try (FileInputStream stream = new FileInputStream("myFile");) {
      stream.read();
    }
  }
  
  public void russianDollsVariable() {
    InputStream stream = new FileInputStream("myFile");
    BufferedInputStream in = new BufferedInputStream(stream);
    in.close();
  }
  
  public void russianDolls() {
    BufferedInputStream in = new BufferedInputStream(new FileInputStream("myFile"));
    in.close();
  }
  
  public void openedRussianDolls() {
    BufferedInputStream in = new BufferedInputStream(new FileInputStream("myFile")); // Noncompliant {{Use try-with-resources or close this "BufferedInputStream" in a "finally" clause.}}
    in.read();
  }
  
  public void overwrite() {
    InputStream stream = new FileInputStream("myFile"); // Noncompliant {{Use try-with-resources or close this "FileInputStream" in a "finally" clause.}}
    stream = new FileInputStream("otherFile");
    stream.close();
  }
  
  public Closeable getStream() {
    InputStream stream = new FileInputStream("myFile");
    return stream;
  }
  
  public Closeable getStreamDirect(String myFile) {
    return new FileInputStream(myFile);
  }
  
  public void creationWithinIFs(boolean test) {
    Reader reader3 = new FileReader(""); // Noncompliant {{Use try-with-resources or close this "FileReader" in a "finally" clause.}}
    if (test) {
      reader3 = new FileReader(""); // Noncompliant {{Use try-with-resources or close this "FileReader" in a "finally" clause.}}
    } else {
      reader3 = new FileReader(""); // Noncompliant {{Use try-with-resources or close this "FileReader" in a "finally" clause.}}
    }
  }
  
  public void closeWithinIFs(boolean test) {
    Reader reader = new FileReader(""); // Noncompliant {{Use try-with-resources or close this "FileReader" in a "finally" clause.}}
    if (test) {
      reader.close();
    }
  }
  
  public void conditionalCreation(boolean test) {
    BufferedWriter bw;
    if (test) {
      bw = new BufferedWriter(new FileWriter("")); // Noncompliant {{Use try-with-resources or close this "BufferedWriter" in a "finally" clause.}}
    } else {
      bw = new BufferedWriter(new FileWriter(""));// Noncompliant {{Use try-with-resources or close this "BufferedWriter" in a "finally" clause.}}
    }
    return;
  }
  
  public void conditionalClose(boolean test) {
    FileInputStream fis; // Not closed in else branch
    if (test) {
      fis = new FileInputStream("");
      fis.close();
    } else {
      fis = new FileInputStream(""); // Noncompliant {{Use try-with-resources or close this "FileInputStream" in a "finally" clause.}}
    }
  }
  
  public void arrayOfStreams() {
    InputStream[] streams = new InputStream[2];
    streams[0] = new FileInputStream("MyFile");
    streams[0].close();
  }

  public void param(InputStream fileIn) throws IOException {
    BufferedInputStream bufferIn = new BufferedInputStream(fileIn); // Compliant
    bufferIn.read();
  }

  public void nestedParam(InputStream fileIn) throws IOException {
    Reader reader = new InputStreamReader(new BufferedInputStream(fileIn)); // Compliant
    reader.read();
  }

  public void closePrimary(String fileName) throws IOException {
    InputStream fileIn = new FileInputStream(fileName);
    BufferedInputStream bufferIn = new BufferedInputStream(fileIn);
    Reader reader = new InputStreamReader(bufferIn, "UTF-16");  // Noncompliant {{Use try-with-resources or close this "InputStreamReader" in a "finally" clause.}}
    reader.read(); // can fail
    fileIn.close();
  }
  
  public void closeSecondary(String fileName) throws IOException {
    InputStream fileIn = new FileInputStream(fileName);
    BufferedInputStream bufferIn = new BufferedInputStream(fileIn);
    Reader reader = new InputStreamReader(bufferIn, "UTF-16"); // Noncompliant {{Use try-with-resources or close this "InputStreamReader" in a "finally" clause.}}
    reader.read(); // can fail
    bufferIn.close();
  }
  
  public void closeTertiary(String fileName) throws IOException {
    InputStream fileIn = new FileInputStream(fileName);
    BufferedInputStream bufferIn = new BufferedInputStream(fileIn);
    Reader reader = new InputStreamReader(bufferIn, "UTF-16"); // Noncompliant {{Use try-with-resources or close this "InputStreamReader" in a "finally" clause.}}
    reader.read(); // can fail
    reader.close();
  }

  public void forLoopHandling(int maxLoop) {
    FileInputStream stream = new FileInputStream("myFile"); // Noncompliant {{Use try-with-resources or close this "FileInputStream" in a "finally" clause.}}
    for(int i = 0; i < maxLoop; i++) {
      stream = new FileInputStream("myFile"); // Noncompliant {{Use try-with-resources or close this "FileInputStream" in a "finally" clause.}}
      stream.read();  // can fail
      stream.close();
    }
   return;
  }

  public void forEachLoopHandling(List<Object> objects) {
    FileInputStream stream = new FileInputStream("myFile"); // Noncompliant {{Use try-with-resources or close this "FileInputStream" in a "finally" clause.}}
    for(Object object : objects) {
      stream = new FileInputStream("myFile"); // Noncompliant {{Use try-with-resources or close this "FileInputStream" in a "finally" clause.}}
      stream.read(); // can fail
      stream.close();
    }
   return;
  }

  public void methodDispatch(List<Object> objects) {
    FileInputStream stream = new FileInputStream("myFile"); // Compliant since can be closed in method call
    dispatch(stream);
  }

  public InputStream methodReturned(List<Object> objects) {
    FileInputStream stream = new FileInputStream("myFile"); // Compliant since resource is returned (and can be closed elsewhere)
    return stream;
  }
  
  public int delegateCreatedStream(String fileName) {
    return process(new JarFile(fileName));    // Compliant since JAR file could be closed in method process
  }
  
  private InputStream instantiatedStream;
  
  public void initializeStreamToField(String fileName) {
    instantiatedStream = new FileInputStream(fileName);    // Compliant since instantiatedStream can be closed elsewhere
  }
  
  public void wrappedAccess(InputStream stream) {
    Reader reader = InputStreamReader(stream);  // Compliant since stream can be closed elsewhere, and thus reader
    reader.read();
  }
  
  public String readUTF() throws IOException {
    return new DataInputStream(instantiatedStream).readUTF();  // Compliant since instantiatedStream can be closed elsewhere
  }

  public void readDelegate(Delegate delegate) {
    Reader reader = InputStreamReader(delegate.stream());  // Compliant since obtained stream can be closed elsewhere, and thus reader
    reader.read();
  }

  private Delegate response;
  protected void writeEventStream(Payload payload) throws IOException {
    PrintStream printStream = new PrintStream(response.outputStream()); // Noncompliant

    try (Stream<?> stream = (Stream<?>) payload.rawContent()) {
      stream.forEach(item -> {
        String jsonOrPlainString = (item instanceof String) ? (String) item : TypeConvert.toJson(item);

        printStream
            .append("data: ")
            .append(jsonOrPlainString.replaceAll("[\n]", "\ndata: "))
            .append("\n\n")
            .flush();
      });
    }
  }
  
  public void correctHandlingOfJarFile(String fileName) {
    JarFile jar = null;
    try {
      jar = new JarFile(fileName);
      jar.entries();
    } finally {
      closeJar(jar);
    }
  }

  public void getDirectivesFromFile(File aFile) {
    BufferedReader reader = null;
    try {
      reader = new BufferedReader(new FileReader(aFile)); // Compliant
      reader.read();
    } finally {
      IOUtils.closeQuietly(reader);
    }
  }
  
  public InputStream getStreamAsNewInstanceArgument(@Nonnull String obj) throws IOException {
    String key = getKey(Obj);
    try {
      lock();
      Path path = getCacheCopy(key);
      if (path == null) {
        return ull;
      }
      return new DeleteFileOnCloseInputStream(new FileInputStream(path.toFile()), path);
    } finally {
      unlock();
    }
  }

  public void checkPath(String fileName) throws IOException {
    new FileInputStream(fileName).close(); // Compliant, but unlikely; check correct retrieval of value passed to method invocation.
  }

  public void justToBeAbleToUseVerify() {
    FileInputStream stream = new FileInputStream("myFile"); // Noncompliant {{Use try-with-resources or close this "FileInputStream" in a "finally" clause.}}
    stream.read();
   return;
  }

  public void forLoopHandling(int maxLoop) {
    Reader reader = null;
    for (int i = 0; i < MAX_LOOP; i++) {
      reader = new FileReader(""); // Noncompliant {{Use try-with-resources or close this "FileReader" in a "finally" clause.}}
    }
    reader.close();
  }

  public void forEachLoopHandling(List<Object> objects) {
    Writer writer = null;
    for (Object object : objects) {
      writer = new FileWriter(""); // Noncompliant {{Use try-with-resources or close this "FileWriter" in a "finally" clause.}}
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

  public void doWhile() {
    j = 0;
    FileInputStream fis = null;
    do {
      fis = new FileInputStream(""); // Noncompliant {{Use try-with-resources or close this "FileInputStream" in a "finally" clause.}}
      j++;
    } while (j < MAX_LOOP);
    fis.close();
  }
  
  void whileLoopWithCounter() {
    int j = 0;
    InputStream is = null;
    while (j < MAX_LOOP) {
      is = new FileInputStream(""); // Noncompliant {{Use try-with-resources or close this "FileInputStream" in a "finally" clause.}}
      j++;
    }
    is.close();
  }

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
  
  void russianDollInTryHeader() {
    try (FileWriter fw = new FileWriter("")) { // Compliant - JLS8 - 14.20.3 : try-with-resources
      fw.write("hello");
    } catch (Exception e) {
      // ...
    }
  }
  void  foo() {
    try {
      java.util.zip.ZipFile file = new java.util.zip.ZipFile(fileName);
      try {
        // do something with the file...
      } finally {
        file.close();
      }
    } catch (Exception e) {
      // Handle exception
    }
  }
  
  public void useFileSystem() {
    final FileSystem defSystem = FileSystems.getDefault(); // Compliant - default file system cannot be closed
    defSystem.getRootDirectories();
  }

  public void methodNamedClose() throws FileNotFoundException {
    FileInputStream is = new FileInputStream("/tmp/foo"); // Compliant - used as parameter of close method
    try {
    } finally {
      close(is);
    }
  }

  public void unknownMethodNamedClose() throws FileNotFoundException {
    FileInputStream is = new FileInputStream("/tmp/foo"); // Compliant - used as parameter of close method
    try {
    } finally {
      Foo.close(is);
    }
  }

  public void methodNamedCloseQuietly() throws FileNotFoundException {
    FileInputStream is = new FileInputStream("/tmp/foo"); // Compliant - used as parameter of closeQuietly method
    try {
    } finally {
      closeQuietly(new FileInputStream("/tmp/foo"), is);
    }
  }

  public void unknownMethodCloseQuietly() throws FileNotFoundException  {
    FileInputStream is = new FileInputStream("/tmp/foo"); // Compliant - used as parameter of closeQuietly method
    try {
    } finally {
      Foo.closeQuietly(is);
    }
  }

  private static void closeQuietly(FileInputStream ... streams) {
    // supposedly close the input streams
  }

  private static void close(FileInputStream fis) {
    // supposedly close the input stream
  }

  void activateDeferredProfile(File file, B b) throws Exception {
    FileInputStream fis1, fis2;

    if ((fis1 = B.getNullableResource(fis1)) == null) { // Compliant - fis is closed...
      throw new Exception();
    }
    fis1.close(); // close the file

    if ((fis2 = new FileInputStream(file)) == null) { // compliant, fis2 is closed, no exceptions in between open and close
      throw new Exception();
    }
    try {
      fis2.close();  // try to close the file
    } catch (IOException e) {
      // fis2 is not closed
      throw new Exception();
    }
  }

}

class B {
  static <T> T getNullableResource(T param){
    return unknownMethod();
  }
}


class Trans {
  void transitive(FileInputStream param) {
    FileInputStream local = new FileInputStream("foo");
    try {
      if (param == local) {

      }
    } finally {
      local.close();
    }
  }
  void empty_catch_block() {
    FileInputStream fis = null;
    FileOutputStream fos = null;

    try {
      fis = new FileInputStream();
      fos = new FileOutputStream();
    } catch (IOException e) {
      // empty catch block
    } finally {
      if (fis != null) {
        try {
          fis.close();
        } catch (IOException e){
          
        }
      }
      fos.close();
    }
  }
}


class MethodHeuristics {
  Utils utils;

  void test() {
    OutputStream os = utils.newOutputStream(); // Noncompliant
    InputStream is = Files.newInputStream(Paths.get("test")); // Noncompliant
  }

  InputStream testChained() {
    try {
      // this is technically an FP, since if constructor fails stream can be still open, but it will cause too many FPs
      return new BufferedInputStream(Files.newInputStream(Paths.get("test"))); // Compliant
    } catch (Exception e) {
    }
  }

  InputStream testChained2() {
    try {
      InputStream is = Files.newInputStream(Paths.get("test")); // Noncompliant
      return new BufferedInputStream(is);
    } catch (Exception e) {

    }
  }
}

abstract class Utils {
  abstract OutputStream newOutputStream();
}
class nestedFinally {
  void foo() {
    try {
      FileInputStream fis = null;
      try {
        fis = new FileInputStream("foobar.txt"); // compliant because of the finally.
        fis.read();
      }
      catch (IOException e) {
        LOG.error("Exception reading from stream:", e);
      }
      finally {
        if (fis != null) {
          try {
            fis.close();
          }
          catch (IOException e) {
            LOG.error("Exception closing stream:", e);
          }
        }
      }
    }
    catch (Exception e) {
      LOG.error("Exception reading:", e);
    }
  }
}

class NPEUnknownSymbol implements Closeable {

  NPEUnknownSymbol() {
    // this method invocation's symbol is unresolved, which triggered NPE in check
    this(NPEUnknownSymbol::consumer);
  }

  NPEUnknownSymbol(Consumer<?> consumer) {

  }

  static void consumer(Object o) {}

  public void close()  {}
}

public class App
{
  public static Connection getJDBCConnectionWithFinally(String driver, String url, String user, String pwd) {

    Connection con = null;

    try
    {
      Class.forName(driver);
      con = DriverManager.getConnection(url, user, pwd); // compliant connection is returned

    }
    catch(ClassNotFoundException cnfe){}
    catch(SQLException se){}
    finally {}
    return con;
  }

  public static Connection getJDBCConnectionOrigWithoutFinally(String driver, String url, String user, String pwd) {

    Connection con = null;

    try
    {
      Class.forName(driver);
      con = DriverManager.getConnection(url, user, pwd); // compliant connection is returned

    }
    catch(ClassNotFoundException cnfe){}
    catch(SQLException se){}
    return con;
  }
}
