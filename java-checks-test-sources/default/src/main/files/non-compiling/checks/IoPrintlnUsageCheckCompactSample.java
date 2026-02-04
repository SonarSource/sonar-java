import java.io.IO;

void main() {
  IO.println(""); // Compliant
  IO.print(""); // Compliant
  f();
  new A().f();
}

void f() {
  IO.println(""); // Compliant
  IO.print(""); // Compliant
}

class A {
  void f() {
    IO.println(""); // Compliant
    IO.print(""); // Compliant
  }
}
