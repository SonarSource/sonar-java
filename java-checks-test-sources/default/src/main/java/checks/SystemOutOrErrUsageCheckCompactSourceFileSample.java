void main() {
  String name = IO.readln("Enter your name: ");

  System.out.println("Goodbye " + name + "!");

  IO.println("Hello " + name + "!"); // Noncompliant
}
