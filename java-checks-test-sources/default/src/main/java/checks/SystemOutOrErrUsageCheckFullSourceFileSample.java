package checks;

import java.io.IO;

public class SystemOutOrErrUsageCheckFullSourceFileSample {
  void main() {
    String name = IO.readln("Enter your name: ");

    System.out.println("Goodbye " + name + "!"); // Noncompliant

    IO.println("Hello " + name + "!");
  }
}
