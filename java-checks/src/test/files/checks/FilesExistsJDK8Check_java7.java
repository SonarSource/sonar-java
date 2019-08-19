import java.nio.file.Files;

class A {
  void foo(Path path) {
    if (Files.exists(path)) { // compliant - no alternative with java 7
    }
  }
}
