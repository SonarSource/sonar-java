package checks.mainSignature;

public class Varargs {
  void main(String... args) {
  }

  void main(int i, String ... args) { // Noncompliant

  }
}
