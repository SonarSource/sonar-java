package checks;

// TODO: Remove before merging. This is only for comparing ASTs in debugger.

public class SystemOutOrErrUsageCheckFullSourceFileSample {
  void main() {
    String name = IO.readln("Enter your name: ");
    IO.println("Hello " + name + "!");
    System.out.println("Goodbye " + name + "!"); // Noncompliant
  }
}
