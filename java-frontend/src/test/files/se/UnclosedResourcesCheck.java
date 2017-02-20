import java.io.ByteArrayOutputStream;
import java.io.FileInputStream;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;

import org.apache.commons.io.IOUtils;

import javax.annotation.Nonnull;

import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.io.Writer;
import java.nio.file.Path;
import java.io.FileReader;
import java.io.BufferedWriter;
import java.util.Formatter;
import java.util.jar.JarFile;
import java.io.DataInputStream;
import java.io.File;
import java.nio.file.FileSystem;
import java.nio.file.FileSystems;

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
    FileInputStream stream = new FileInputStream("myFile"); // Noncompliant [[flows=wrongHandling]] {{Close this "FileInputStream".}} flow@wrongHandling {{FileInputStream is never closed.}}
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
    FileInputStream stream = new FileInputStream("myFile"); // Noncompliant [[flows=while]] {{Close this "FileInputStream".}} flow@while {{FileInputStream is never closed.}}
    while(needsMore()) {
      stream = new FileInputStream("myFile"); // Noncompliant {{Close this "FileInputStream".}}
      stream.read(); // can fail
      stream.close();
    }
   return;
  }

  public void wrongLoopHandling() {
    int i = 0;
    FileInputStream stream = new FileInputStream("WhileFile"); // Noncompliant [[flows=loop1]] {{Close this "FileInputStream".}} flow@loop1 {{FileInputStream is never closed.}}
    while(needsMore()) {
      i += 1;
      stream = new FileInputStream("WhileFile"+i); // Noncompliant [[flows=loop2]] {{Close this "FileInputStream".}} flow@loop2 {{FileInputStream is never closed.}}
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
    BufferedInputStream in = new BufferedInputStream(new FileInputStream("myFile")); // Noncompliant [[flows=dolls]] {{Close this "FileInputStream".}} flow@dolls {{FileInputStream is never closed.}}
    in.read();
  }
  
  public void overwrite() {
    InputStream stream = new FileInputStream("myFile"); // Noncompliant [[flows=overwrite]] {{Close this "FileInputStream".}} flow@overwrite {{FileInputStream is never closed.}}
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
    Reader reader3 = new FileReader(""); // Noncompliant [[flows=creation1]] {{Close this "FileReader".}} flow@creation1 {{FileReader is never closed.}}
    if (test) {
      reader3 = new FileReader(""); // Noncompliant [[flows=creation2]] {{Close this "FileReader".}} flow@creation2 {{FileReader is never closed.}}
    } else {
      reader3 = new FileReader(""); // Noncompliant [[flows=creation3]] {{Close this "FileReader".}} flow@creation3 {{FileReader is never closed.}}
    }
  }
  
  public void closeWithinIFs(boolean test) {
    Reader reader = new FileReader(""); // Noncompliant [[flows=ifs]] {{Close this "FileReader".}} flow@ifs {{FileReader is never closed.}}
    if (test) {
      reader.close();
    }
  }
  
  public void conditionalCreation(boolean test) {
    BufferedWriter bw;
    if (test) {
      bw = new BufferedWriter(new FileWriter("")); // Noncompliant [[flows=cc1]] {{Close this "FileWriter".}} flow@cc1 {{FileWriter is never closed.}}
    } else {
      bw = new BufferedWriter(new FileWriter(""));// Noncompliant [[flows=cc2]] {{Close this "FileWriter".}} flow@cc2 {{FileWriter is never closed.}}
    }
    return;
  }
  
  public void conditionalClose(boolean test) {
    FileInputStream fis; // Not closed in else branch
    if (test) {
      fis = new FileInputStream("");
      fis.close();
    } else {
      fis = new FileInputStream(""); // Noncompliant [[flows=conClose]] {{Close this "FileInputStream".}} flow@conClose {{FileInputStream is never closed.}}
    }
  }
  
  public void arrayOfStreams() {
    InputStream[] streams = new InputStream[2];
    streams[0] = new FileInputStream("MyFile");
    streams[0].close();
  }
  
  public void closePrimary(String fileName) throws IOException {
    InputStream fileIn = new FileInputStream(fileName); // Noncompliant {{Close this "FileInputStream".}}
    BufferedInputStream bufferIn = new BufferedInputStream(fileIn);
    Reader reader = new InputStreamReader(bufferIn, "UTF-16");
    reader.read(); // can fail
    fileIn.close();
  }
  
  public void closeSecondary(String fileName) throws IOException {
    InputStream fileIn = new FileInputStream(fileName); // Noncompliant {{Close this "FileInputStream".}}
    BufferedInputStream bufferIn = new BufferedInputStream(fileIn);
    Reader reader = new InputStreamReader(bufferIn, "UTF-16");
    reader.read(); // can fail
    bufferIn.close();
  }
  
  public void closeTertiary(String fileName) throws IOException {
    InputStream fileIn = new FileInputStream(fileName); // Noncompliant {{Close this "FileInputStream".}}
    BufferedInputStream bufferIn = new BufferedInputStream(fileIn);
    Reader reader = new InputStreamReader(bufferIn, "UTF-16");
    reader.read(); // can fail
    reader.close();
  }

  public void forLoopHandling(int maxLoop) {
    FileInputStream stream = new FileInputStream("myFile"); // Noncompliant [[flows=for]] {{Close this "FileInputStream".}} flow@for {{FileInputStream is never closed.}}
    for(int i = 0; i < maxLoop; i++) {
      stream = new FileInputStream("myFile"); // Noncompliant {{Close this "FileInputStream".}}
      stream.read();  // can fail
      stream.close();
    }
   return;
  }

  public void forEachLoopHandling(List<Object> objects) {
    FileInputStream stream = new FileInputStream("myFile"); // Noncompliant [[flows=for2]] {{Close this "FileInputStream".}} flow@for2 {{FileInputStream is never closed.}}
    for(Object object : objects) {
      stream = new FileInputStream("myFile"); // Noncompliant {{Close this "FileInputStream".}}
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
    PrintStream printStream = new PrintStream(response.outputStream());

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
      reader = new BufferedReader(new FileReader(aFile)); // Noncompliant {{Close this "FileReader".}}
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
    FileInputStream stream = new FileInputStream("myFile"); // Noncompliant {{Close this "FileInputStream".}}
    stream.read();
   return;
  }

  public void forLoopHandling(int maxLoop) {
    Reader reader = null;
    for (int i = 0; i < MAX_LOOP; i++) {
      reader = new FileReader(""); // Noncompliant {{Close this "FileReader".}}
    }
    reader.close();
  }

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

  public void doWhile() {
    j = 0;
    FileInputStream fis = null;
    do {
      fis = new FileInputStream(""); // Noncompliant {{Close this "FileInputStream".}}
      j++;
    } while (j < MAX_LOOP);
    fis.close();
  }
  
  void whileLoopWithCounter() {
    int j = 0;
    InputStream is = null;
    while (j < MAX_LOOP) {
      is = new FileInputStream(""); // Noncompliant {{Close this "FileInputStream".}}
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
