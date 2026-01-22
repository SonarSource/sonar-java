void main() {
  String name = IO.readln("Enter your name: ");
  IO.println("Hello " + name + "!");
  System.out.println("Goodbye " + name + "!");

  System.out.println(f(6, 7));

  RegularClass rc = new RegularClass();
  rc.log("This is a log message.");
}

int f(int x, int y) {
  System.err.println("Error: x=" + x + ", y=" + y);
  return x + y;
}

class RegularClass {
  void log(String message) {
    System.out.println(message);
  }
}
