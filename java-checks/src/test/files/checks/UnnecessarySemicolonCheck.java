import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Paths;

public class UnnecessarySemicolonCheck {

  private static InputStream input(String path) throws IOException {
    return Files.newInputStream(Paths.get(path));
  }

  public static void main(String[] args) throws IOException {

    try(InputStream i1 = input("i1");) { // Noncompliant [[sc=37;ec=38]] {{Remove this extraneous semicolon.}}
    }

    try(InputStream i1 = input("i1");InputStream i2 = input("i2");) { // Noncompliant [[sc=66;ec=67]]
    }

    try(InputStream i1 = input("i1")) { // Compliant
    }

    try(InputStream i1 = input("i1"); InputStream i2 = input("i2")) { // Compliant
    }

    final InputStream fi1 = input("fi1");
    final InputStream fi2 = input("fi1");
    try (fi1;) { // Noncompliant

    }

    try (fi1;fi2) { // Compliant

    }

    try (fi1;fi2;) { // Noncompliant

    }


  }

}
