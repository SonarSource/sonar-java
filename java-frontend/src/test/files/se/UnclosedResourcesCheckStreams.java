import org.apache.commons.io.IOUtils;

import javax.annotation.Nonnull;

import java.io.*;
import java.nio.file.*;
import java.util.Formatter;
import java.util.jar.JarFile;
import java.util.stream.*;

public class A {

  void test(Path path) {
    List<String> lines = Files.lines(path).collect(Collectors.toList()); // Noncompliant
    Stream<Path> walk = Files.walk(path); // Noncompliant
    DirectoryStream<Path> newDirectoryStream = Files.newDirectoryStream(path); // Noncompliant
    Stream<Path> find = Files.find(Paths.get("."), Integer.MAX_VALUE, (path, basicFileAttributes) -> true); // Noncompliant
    Stream<Path> list = Files.list(Paths.get(".")); // Noncompliant
  }

  void compliant(Path path) {
    try (Stream<String> lines = Files.lines(path)) {
      List<String> collect = lines.collect(Collectors.toList());
    }

    Stream<Path> walk = null;
    try {
      walk = Files.walk(path);
    } finally {
      if (walk != null) {
        walk.close();
      }
    }
  }

}
