import java.nio.file.Files;
import java.nio.file.Path;
class A {
  void foo() {
    Path path;
    if(Files.exists(path)) {
    }
    if(Files.notExists(path)) {
    }
    if(Files.isRegularFile(path)) {
    }
    if(Files.isDirectory(path)) {
    }
  }
}