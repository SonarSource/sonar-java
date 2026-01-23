void main() {
  String name = IO.readln("Enter your name: ");
  IO.println("Hello " + name + "!");
  System.out.println("Goodbye " + name + "!"); // Noncompliant

  System.out.println(f(6, 7)); // Noncompliant

  RegularClass rc = new RegularClass();
  rc.log("This is a log message.");
}

int f(int x, int y) {
  System.err.println("Error: x=" + x + ", y=" + y); // Noncompliant
  return x + y;
}

class RegularClass {
  void log(String message) {
    System.out.println(message); // Noncompliant
  }
}
