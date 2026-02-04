package checks;


import java.io.IO;
import java.io.PrintStream;

class IoPrintlnUsageCheckSample {

  void f() {
    IO.println(""); // Noncompliant {{Replace this use of IO.println by a logger.}}
    IO.print(""); // Noncompliant {{Replace this use of IO.print by a logger.}}
  }

}
