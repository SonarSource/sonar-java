package checks.mainSignature;

interface NonInstantiable {
  int multiply(int a, int b);

  default void main() { // Compliant
    System.out.println("default");
  }

  int main(int a); // Noncompliant

  static void main(String[] arg) { // Compliant
    System.out.println("yep, inside an interface");
  }
}
