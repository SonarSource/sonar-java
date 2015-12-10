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
import java.nio.file.Path;
import java.io.FileReader;
import java.io.BufferedWriter;
import java.util.Formatter;
import java.util.jar.JarFile;
import java.io.DataInputStream;
import java.io.File;

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
    FileInputStream stream = new FileInputStream("myFile"); // Noncompliant {{Close this "FileInputStream".}}
    stream.read();
   return;
  }

  public void toleratedHandling() {
    ByteArrayOutputStream stream = new ByteArrayOutputStream();
    stream.write('c');
   return;
  }

  public void whileLoopHandling() {
    FileInputStream stream = new FileInputStream("myFile"); // Noncompliant {{Close this "FileInputStream".}}
    while(needsMore()) {
      stream = new FileInputStream("myFile");
      stream.read();
      stream.close();
    }
   return;
  }

  public void wrongLoopHandling() {
    int i = 0;
    FileInputStream stream = new FileInputStream("WhileFile"); // Noncompliant {{Close this "FileInputStream".}}
    while(needsMore()) {
      i += 1;
      stream = new FileInputStream("WhileFile"+i); // Noncompliant {{Close this "FileInputStream".}}
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
    BufferedInputStream in = new BufferedInputStream(new FileInputStream("myFile")); // Noncompliant {{Close this "FileInputStream".}}
    in.read();
  }
  
  public void overwrite() {
    InputStream stream = new FileInputStream("myFile"); // Noncompliant {{Close this "FileInputStream".}}
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
    Reader reader3 = new FileReader(""); // Noncompliant {{Close this "FileReader".}}
    if (test) {
      reader3 = new FileReader(""); // Noncompliant {{Close this "FileReader".}}
    } else {
      reader3 = new FileReader(""); // Noncompliant {{Close this "FileReader".}}
    }
  }
  
  public void closeWithinIFs(boolean test) {
    Reader reader = new FileReader(""); // Noncompliant {{Close this "FileReader".}}
    if (test) {
      reader.close();
    }
  }
  
  public void conditionalCreation(boolean test) {
    BufferedWriter bw;
    if (test) {
      bw = new BufferedWriter(new FileWriter("")); // Noncompliant {{Close this "FileWriter".}}
    } else {
      bw = new BufferedWriter(new FileWriter(""));// Noncompliant {{Close this "FileWriter".}}
    }
    return;
  }
  
  public void conditionalClose(boolean test) {
    FileInputStream fis; // Not closed in else branch
    if (test) {
      fis = new FileInputStream("");
      fis.close();
    } else {
      fis = new FileInputStream(""); // Noncompliant {{Close this "FileInputStream".}}
    }
  }
  
  public void arrayOfStreams() {
    InputStream[] streams = new InputStream[2];
    streams[0] = new FileInputStream("MyFile");
    streams[0].close();
  }
  
  public void closePrimary(String fileName) throws IOException {
    InputStream fileIn = new FileInputStream(fileName);
    BufferedInputStream bufferIn = new BufferedInputStream(fileIn);
    Reader reader = new InputStreamReader(bufferIn, "UTF-16");
    reader.read();
    fileIn.close();
  }
  
  public void closeSecondary(String fileName) throws IOException {
    InputStream fileIn = new FileInputStream(fileName);
    BufferedInputStream bufferIn = new BufferedInputStream(fileIn);
    Reader reader = new InputStreamReader(bufferIn, "UTF-16");
    reader.read();
    bufferIn.close();
  }
  
  public void closeTertiary(String fileName) throws IOException {
    InputStream fileIn = new FileInputStream(fileName);
    BufferedInputStream bufferIn = new BufferedInputStream(fileIn);
    Reader reader = new InputStreamReader(bufferIn, "UTF-16");
    reader.read();
    reader.close();
  }

  public void forLoopHandling(int maxLoop) {
    FileInputStream stream = new FileInputStream("myFile"); // Noncompliant {{Close this "FileInputStream".}}
    for(int i = 0; i < maxLoop; i++) {
      stream = new FileInputStream("myFile");
      stream.read();
      stream.close();
    }
   return;
  }

  public void forEachLoopHandling(List<Object> objects) {
    FileInputStream stream = new FileInputStream("myFile"); // Noncompliant {{Close this "FileInputStream".}}
    for(Object object : objects) {
      stream = new FileInputStream("myFile");
      stream.read();
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
      reader = new BufferedReader(new FileReader(aFile));
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
}
