import java.nio.file.Files;
import java.nio.file.Path;
class A {
  void foo() {
    Path path;
    if(Files.exists(path)) { // Noncompliant {{Replace this with a call to the "toFile().exists()" method}}
    }
    if(Files.notExists(path)) { // Noncompliant {{Replace this with a call to the "toFile().exists()" method}}
    }
    if(Files.isRegularFile(path)) { // Noncompliant {{Replace this with a call to the "toFile().isFile()" method}}
    }
    if(Files.isDirectory(path)) { // Noncompliant {{Replace this with a call to the "toFile().isDirectory()" method}}
    }
  }
}
