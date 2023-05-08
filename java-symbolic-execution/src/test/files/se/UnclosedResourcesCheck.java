import org.apache.commons.io.IOUtils;

import java.io.*;
import java.nio.file.*;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.Formatter;
import java.util.List;
import java.util.Properties;
import java.util.function.Consumer;
import java.util.jar.JarFile;
import java.util.stream.Stream;
import javax.annotation.Nonnull;
import javax.jms.ConnectionFactory;
import javax.jms.JMSException;
import javax.jms.MessageConsumer;
import javax.jms.MessageProducer;
import javax.jms.ObjectMessage;
import javax.jms.Session;

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

  public void apacheCommonsIgnoredType() {
    org.apache.commons.io.output.ByteArrayOutputStream s1 = new org.apache.commons.io.output.ByteArrayOutputStream(); // Compliant
    s1.write('c');

    org.apache.commons.io.output.UnsynchronizedByteArrayOutputStream s2 = new org.apache.commons.io.output.UnsynchronizedByteArrayOutputStream(); // Compliant
    s2.write('c');
  }

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

  boolean needsMore() {
    return Math.random() < 0.5d;
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
    BufferedInputStream bufferIn = new BufferedInputStream(fileIn); // Noncompliant
    // InputStreamReader can throw an exeption, if it is the case, there is no way to close bufferIn
    Reader reader = new InputStreamReader(bufferIn, "UTF-16");  // Noncompliant {{Use try-with-resources or close this "InputStreamReader" in a "finally" clause.}}
    reader.read(); // can fail
    fileIn.close();
  }
  
  public void closeSecondary(String fileName) throws IOException {
    InputStream fileIn = new FileInputStream(fileName);
    BufferedInputStream bufferIn = new BufferedInputStream(fileIn); // Noncompliant
    Reader reader = new InputStreamReader(bufferIn, "UTF-16"); // Noncompliant {{Use try-with-resources or close this "InputStreamReader" in a "finally" clause.}}
    reader.read(); // can fail
    bufferIn.close();
  }
  
  public void closeTertiary(String fileName) throws IOException {
    InputStream fileIn = new FileInputStream(fileName);
    BufferedInputStream bufferIn = new BufferedInputStream(fileIn); // Noncompliant
    Reader reader = new InputStreamReader(bufferIn, "UTF-16"); // Noncompliant {{Use try-with-resources or close this "InputStreamReader" in a "finally" clause.}}
    reader.read(); // can fail
    reader.close();
  }

  public void closeTertiary2(String fileName) throws IOException {
    InputStream fileIn = new FileInputStream(fileName); // false-negative
    BufferedInputStream bufferIn = getBufferedInputStream(fileIn);
    Reader reader = new InputStreamReader(bufferIn, "UTF-16");
    reader.read(); // can fail
    reader.close();
  }

  InputStream getBufferedInputStream(InputStream fileIn) {
    return new BufferedInputStream(fileIn);
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

  void dispatch(FileInputStream stream) {
  }

  public InputStream methodReturned(List<Object> objects) {
    FileInputStream stream = new FileInputStream("myFile"); // Compliant since resource is returned (and can be closed elsewhere)
    return stream;
  }
  
  public int delegateCreatedStream(String fileName) {
    return process(new JarFile(fileName));    // Compliant since JAR file could be closed in method process
  }

  int process(JarFile file) {
    return file.hashCode();
  }
  
  private InputStream instantiatedStream;
  
  public void initializeStreamToField(String fileName) {
    instantiatedStream = new FileInputStream(fileName);    // Compliant since instantiatedStream can be closed elsewhere
  }
  
  public void wrappedAccess(InputStream stream) {
    Reader reader = new InputStreamReader(stream);  // Compliant since stream can be closed elsewhere, and thus reader
    reader.read();
  }
  
  public String readUTF() throws IOException {
    return new DataInputStream(instantiatedStream).readUTF();  // Compliant since instantiatedStream can be closed elsewhere
  }

  public void readDelegate(Delegate delegate) {
    Reader reader = new InputStreamReader(delegate.stream());  // Compliant since obtained stream can be closed elsewhere, and thus reader
    reader.read();
  }

  class Delegate {
    InputStream stream() {
      return new FileInputStream("foo.txt");
    }
    OutputStream outputStream() {
      return new FileOutputStream("foo.txt");
    }
  }
  class Payload {
    Stream<Object> rawContent() {
      return Stream.of(new Object());
    }
  }

  private Delegate response;
  protected void writeEventStream(Payload payload) throws IOException {
    PrintStream printStream = new PrintStream(response.outputStream()); // false-negative

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

  void closeJar(JarFile jar) {
    jar.close();
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
    String key = getKey(obj);
    try {
      lock();
      Path path = getCacheCopy(key);
      if (path == null) {
        return null;
      }
      return new DeleteFileOnCloseInputStream(new FileInputStream(path.toFile()), path);
    } finally {
      unlock();
    }
  }

  void lock() {
  }

  String getKey(String obj) {
    return obj.toString();
  }

  Path getCacheCopy(String key) {
    return Paths.get(key);
  }

  class DeleteFileOnCloseInputStream extends ObjectInputStream {
    Path path;
    DeleteFileOnCloseInputStream(InputStream in, Path path) {
      super(in);
      this.path = path;
    }
    public void close() {
      Files.delete(path);
    }
  }

  void unlock() {
  }

  public void checkPath(String fileName) throws IOException {
    new FileInputStream(fileName).close(); // Compliant, but unlikely; check correct retrieval of value passed to method invocation.
  }

  public void justToBeAbleToUseVerify() {
    FileInputStream stream = new FileInputStream("myFile"); // Noncompliant {{Use try-with-resources or close this "FileInputStream" in a "finally" clause.}}
    stream.read();
   return;
  }

  public void forLoopHandling2(int maxLoop) {
    Reader reader = null;
    for (int i = 0; i < MAX_LOOP; i++) {
      reader = new FileReader(""); // Noncompliant {{Use try-with-resources or close this "FileReader" in a "finally" clause.}}
    }
    reader.close();
  }

  public void forEachLoopHandling2(List<Object> objects) {
    Writer writer = null;
    for (Object object : objects) {
      writer = new FileWriter(""); // Noncompliant {{Use try-with-resources or close this "FileWriter" in a "finally" clause.}}
    }
    writer.close();
  }

  public void methodDispatch2(List<Object> objects) {
    FileInputStream stream = new FileInputStream("myFile"); // Compliant since can be closed in method call
    dispatch(stream);
  }

  public InputStream methodReturned2(List<Object> objects) {
    FileInputStream stream = new FileInputStream("myFile"); // Compliant since resource is returned (and can be closed elsewhere)
    return stream;
  }

  public void doWhile() {
    int j = 0;
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

  enum ABC { A, B, C }
  ABC enumValue;

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
  void  foo(String fileName) {
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
  class Foo {
    static void close(InputStream is) {
      is.close();
    }
    static void closeQuietly(InputStream is) {
      is.close();
    }
  }

  public void methodNamedCloseQuietly() throws FileNotFoundException {
    FileInputStream is = new FileInputStream("/tmp/foo"); // Noncompliant
    try {
    } finally {
      // new FileInputStream can fail, preventing to clsoe the is stream.
      closeQuietly(new FileInputStream("/tmp/foo"), is);
    }
  }

  public void methodNamedCloseQuietly2() throws FileNotFoundException {
    FileInputStream is = new FileInputStream("/tmp/foo"); // Compliant - used as parameter of closeQuietly method
    try {
    } finally {
      closeQuietly(is);
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

  void switchTest(int x) {
    MyCloseable a = new MyCloseable(); // Compliant because all paths close a
    (switch (x) { case 0 -> a; default -> a;}).close();
  }

  void switchTest2(int x) {
    MyCloseable a = new MyCloseable(); // Noncompliant
    MyCloseable b = new MyCloseable(); // Noncompliant
    (switch (x) { case 0 -> a; default -> b;}).close();
  }

}

class MyCloseable implements Closeable {
  @Override
  void close() {}
}

class B {
  static <T> T getNullableResource(T param){
    return (T) param.clone();
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
      fis = new FileInputStream("/path");
      fos = new FileOutputStream("/path");
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
  class Logger {
    void error(String message, Object... args) {
    }
  }
  static Logger LOG = new Logger();

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

  public void loadPropertiesWrongHandeling() throws IOException {
    Properties properties = new Properties();
    InputStream in = new FileInputStream("file.properties"); // Noncompliant {{Use try-with-resources or close this "FileInputStream" in a "finally" clause.}}
    OutputStream out = new FileOutputStream("file.properties"); // Noncompliant {{Use try-with-resources or close this "FileOutputStream" in a "finally" clause.}}
    properties.load(in);
    properties.store(out, "");
    return properties;
  }

  public void loadPropertiesGoodHandeling() throws IOException {
    Properties properties = new Properties();
    FileInputStream in = new FileInputStream("file.properties");
    try (OutputStream out = new FileOutputStream("file.properties")) {
      properties.load(in);
      properties.store(out, "");
    } finally {
      in.close();
    }
    return properties;
  }

}

class ApacheIOUtilsDoesNotAlwaysClose {

  void copyNotClosing() {
    FileInputStream input = new FileInputStream("myFile"); // Noncompliant
    ByteArrayOutputStream buf = new ByteArrayOutputStream();

    IOUtils.copyLarge(input, buf);
    IOUtils.copy(input, buf);
  }

  void skipNotClosing() {
    FileInputStream input = new FileInputStream("myFile"); // Noncompliant
    IOUtils.skip(input, 1);
    IOUtils.skipFully(input, 1);
  }

  void consumeNotClosing() {
    FileInputStream input = new FileInputStream("myFile"); // Noncompliant
    IOUtils.consume(input);
  }

  void contentEqualsNotClosing() {
    FileInputStream input = new FileInputStream("myFile"); // Noncompliant
    FileInputStream input2 = new FileInputStream("myFile"); // Noncompliant
    IOUtils.contentEquals(input, input2);
  }

  void readNotClosing() {
    FileInputStream input = new FileInputStream("myFile"); // Noncompliant

    IOUtils.read(input, new byte[4]);
    IOUtils.readFully(input, new byte[4]);
    IOUtils.readLines(input);
    IOUtils.readLines(input, "UTF-8");
  }

  void closeMethodClosing() {
    FileInputStream input = new FileInputStream("C:\\JAVA_TEST_PROJECTS\\test_j11\\src\\main\\resources\\myFile"); // Compliant
    IOUtils.close(input);
    IOUtils.closeQuietly(input);
  }
}

// There is no need to close the sessions, producers, and consumers of a closed javax.jms.Connection.
// Similarly, there is no need to close the producers and consumers of a closed javax.jms.Session.
class JavaxJms {
  void closeInTryWithResources(ConnectionFactory connFactory) throws JMSException {
    try (javax.jms.Connection connection = connFactory.createConnection()){
      Session sess = connection.createSession(true, 1); // Compliant
      MessageProducer producer = sess.createProducer(null); // Compliant
      ObjectMessage msg = sess.createObjectMessage();
      msg.setObject(null);
      producer.send(msg);
    }
  }

  void closeInFinally(javax.jms.Connection connection) throws JMSException {
    try {
      Session sess = connection.createSession(1);
      MessageProducer producer = sess.createProducer(null); // Compliant
      MessageConsumer consumer = sess.createConsumer(null); // Compliant
      MessageConsumer durableConsumer = sess.createDurableConsumer(null, ""); // Compliant
      MessageConsumer sharedConsumer = sess.createSharedConsumer(null, ""); // Compliant
      MessageConsumer sharedDurableConsumer = sess.createSharedDurableConsumer(null, ""); // Compliant
    } finally {
      connection.close();
    }
  }
}

class UnknownExceptions {

  public void wrongHandling() throws IOException {
    FileInputStream stream = new FileInputStream("myFile"); // Noncompliant
    try {
      stream.read();
    } finally {
      // if cleanCanThrow throws an exception, stream will not be closed
      cleanCanThrow();
      stream.close();
    }
  }

  public void goodHandling() throws IOException {
    FileInputStream stream = new FileInputStream("myFile"); // Compliant
    try {
      stream.read();
    } finally {
      // Clean can throw a runtime, but we ignore them
      clean();
      stream.close();
    }
  }

  public void goodHandling2() throws IOException {
    FileInputStream stream = new FileInputStream("myFile"); // Compliant
    try {
      stream.read();
    } finally {
      // The engine defines that this method can possibly throw an unknown exeption, but it probably does not prevent the close to happen.
      canThrowUnknown();
      stream.close();
    }
  }

  public static void cleanCanThrow() throws IOException {
    throw new IOException();
  }

  public static void clean() {
    throw new ArrayIndexOutOfBoundsException();
  }

  public static void canThrowUnknown() {
    Object o = new Object();
    o.toString();
  }
}

