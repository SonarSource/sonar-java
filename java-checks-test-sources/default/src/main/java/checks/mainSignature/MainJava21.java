package checks.mainSignature;

public class MainJava21 {
  public static void main(String[] args) { // Compliant
  }

  void main() { // Noncompliant
  }

  void main(int args) { // Noncompliant
  }
}
