import java.io.File;
import java.nio.file.attribute.FileAttribute;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.FileSystem;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.RandomAccessFile;
import org.apache.commons.io.FileUtils;
import java.nio.charset.Charset;

import com.google.common.io.FileBackedOutputStream;
import com.google.common.io.MoreFiles;
import com.google.common.io.Resources;
import com.google.common.io.LineProcessor;


class javaIOFile {
  void fun(String strPath, String StrParent, String StrChild, String prefix, String suffix, java.net.URI uri) throws Exception {

    // Questionable: check what is done with this file
    new File(strPath); // Noncompliant
    new File(StrParent, StrChild); // Noncompliant {{Make sure this file handling is safe here.}}
    File parent = new File(uri); // Noncompliant
    new File(parent, StrChild); // compliant we rely on the fact that parent should be highlighted


    File.createTempFile(prefix, suffix); // Noncompliant
    File.createTempFile(prefix, suffix, parent); // compliant
  }

  void mymethod(File file) {
    // ...
  }
}
class NioFile {
  void fun(FileSystem fileSystem, java.net.URI uri, String part1, String part2, String prefix, FileAttribute<?> attrs, String suffix) throws Exception {
    Path path = Paths.get(part1, part2); // Noncompliant
    Path path2 = Paths.get(uri); // Noncompliant

    Iterable<Path> paths = fileSystem.getRootDirectories(); // Noncompliant {{Make sure this file handling is safe here.}}
    Path path3 = fileSystem.getPath(part1, part2); // Noncompliant

    Path path4 = Files.createTempDirectory(prefix, attrs); // Noncompliant
    Path path5 = Files.createTempFile(prefix, suffix, attrs); // Noncompliant
  }

  void mymethod(Path path) {
    // ...
  }
}

class Fis {
  void fun(String mode) throws Exception {
    FileReader reader = new FileReader("test.txt"); // Noncompliant
    FileReader reader2 = new FileReader("test.txt") { // Noncompliant [[sc=30;ec=40]]
    };
    FileInputStream instream = new FileInputStream("test.txt"); // Noncompliant
    FileWriter writer = new FileWriter("out.txt"); // Noncompliant
    FileOutputStream outstream = new FileOutputStream("out2.txt"); // Noncompliant
    FileOutputStream outstream2 = new FileOutputStream("out2.txt", true); // Noncompliant
    RandomAccessFile file = new RandomAccessFile("test.txt", mode); // Noncompliant
    FileOutputStream outstream3 = new FileOutputStream(file, true); // compliant
  }
}
class ApacheFileUtils {
  void fun() {
    FileUtils.getFile("test.txt"); // Noncompliant
    FileUtils.getTempDirectory(); // Noncompliant
    FileUtils.getUserDirectory(); // Noncompliant [[sc=15;ec=31]] {{Make sure this file handling is safe here.}}
  }
}

class Guava {
  void fun(java.net.URL url, Charset charset, java.io.OutputStream stream, String resourceName, Class<?> contextClass,
           LineProcessor<Object> callback, int fileThreshold, boolean resetOnFinalize) throws Exception {

    com.google.common.io.Files.createTempDir(); // Noncompliant
    com.google.common.io.Files.fileTreeTraverser(); // not testable : method is package protected before being removed from guava.
    com.google.common.io.Files.fileTraverser(); // Noncompliant
    com.google.common.io.MoreFiles.directoryTreeTraverser(); // not testable : method has been removed from guava.
    com.google.common.io.MoreFiles.fileTraverser(); // Noncompliant
    Resources.asByteSource(url); // Noncompliant
    Resources.asCharSource(url, charset); // Noncompliant
    Resources.copy(url, stream); // Noncompliant
    Resources.getResource(contextClass, resourceName); // Noncompliant
    Resources.getResource(resourceName); // Noncompliant
    Resources.readLines(url, charset); // Noncompliant
    Resources.readLines(url, charset, callback); // Noncompliant
    Resources.toByteArray(url); // Noncompliant
    Resources.toString(url, charset); // Noncompliant

    // these OutputStreams creates files
    new FileBackedOutputStream(fileThreshold); // Noncompliant
    new FileBackedOutputStream(fileThreshold, resetOnFinalize); // Noncompliant
  }
}

